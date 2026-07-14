package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Traversing;
import com.flechazo.hkt.functions.CompositePointFreeOptic;
import com.flechazo.hkt.functions.MapOpticElement;
import com.flechazo.hkt.functions.TypedOptic;
import com.flechazo.hkt.tuple.Tuple2;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface TypeRewriteRule {
    Maybe<TypeRewriteResult<?, ?>> rewrite(Type<?> type);

    default Type<?> rewriteOrSame(Type<?> type) {
        Objects.requireNonNull(type, "type");
        Maybe<TypeRewriteResult<?, ?>> result = rewrite(type);
        return result.isDefined() ? result.get().targetType() : type;
    }

    static TypeRewriteRule typeOnly(Function<Type<?>, Maybe<Type<?>>> rewrite) {
        Objects.requireNonNull(rewrite, "rewrite");
        return type -> rewrite.apply(type).map(target -> TypeRewriteResult.typeOnly(cast(type), cast(target)));
    }

    static TypeRewriteRule result(Function<Type<?>, Maybe<TypeRewriteResult<?, ?>>> rewrite) {
        Objects.requireNonNull(rewrite, "rewrite");
        return rewrite::apply;
    }

    static TypeRewriteRule choice(TypeRewriteRule first, TypeRewriteRule second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return type -> first.rewrite(type).or(() -> second.rewrite(type));
    }

    static TypeRewriteRule seq(TypeRewriteRule first, TypeRewriteRule second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return type -> {
            Maybe<TypeRewriteResult<?, ?>> firstResult = first.rewrite(type);
            if (firstResult.isEmpty()) {
                return Maybe.none();
            }
            return second.rewrite(firstResult.get().targetType())
                    .map(secondResult -> composeCaptured(firstResult.get(), secondResult));
        };
    }

    static TypeRewriteRule all(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return type -> rewriteChildren(type, rule, true);
    }

    static TypeRewriteRule one(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return type -> rewriteChildren(type, rule, false);
    }

    static TypeRewriteRule topDown(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return type -> rule.rewrite(type).or(() -> rewriteChildren(type, topDown(rule), true));
    }

    static TypeRewriteRule bottomUp(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return type -> {
            Maybe<TypeRewriteResult<?, ?>> rewrittenChildren = rewriteChildren(type, bottomUp(rule), true);
            Type<?> nextType = targetOr(rewrittenChildren, type);
            Maybe<TypeRewriteResult<?, ?>> local = rule.rewrite(nextType);
            if (local.isDefined()) {
                if (rewrittenChildren.isDefined()) {
                    return Maybe.some(composeCaptured(rewrittenChildren.get(), local.get()));
                }
                return local;
            }
            return rewrittenChildren;
        };
    }

    static TypeRewriteRule once(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return type -> rule.rewrite(type).or(() -> rewriteChildren(type, once(rule), false));
    }

    static TypeRewriteRule many(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return type -> {
            TypeRewriteResult<?, ?> accumulated = TypeRewriteResult.nop(cast(type));
            boolean changed = false;
            while (true) {
                Maybe<TypeRewriteResult<?, ?>> next = rule.rewrite(accumulated.targetType());
                if (next.isEmpty()) {
                    return changed ? Maybe.some(accumulated) : Maybe.none();
                }
                accumulated = composeCaptured(accumulated, next.get());
                changed = true;
            }
        };
    }

    private static Maybe<TypeRewriteResult<?, ?>> rewriteChildren(Type<?> type, TypeRewriteRule rule, boolean allChildren) {
        if (type instanceof Product.ProductType<?, ?> product) {
            return rewriteProduct(product, rule, allChildren);
        }
        if (type instanceof Sum.SumType<?, ?> sum) {
            return rewriteSum(sum, rule, allChildren);
        }
        if (type instanceof ListTemplate.ListType<?> list) {
            return rule.rewrite(list.element())
                    .map(child -> liftList(list, child));
        }
        if (type instanceof CompoundList.CompoundListType<?, ?> compound) {
            return rewriteBinary(compound.key(), compound.element(), rule, allChildren, Types::compoundList)
                    .map(target -> typeOnly(type, target));
        }
        if (type instanceof Types.MapType<?, ?> map) {
            return rewriteMap(map, rule, allChildren);
        }
        if (type instanceof Types.MaybeType<?> maybe) {
            return rule.rewrite(maybe.value())
                    .map(child -> liftMaybe(maybe, child));
        }
        if (type instanceof Types.ValidatedType<?, ?> validated) {
            return rewriteValidated(validated, rule, allChildren);
        }
        if (type instanceof Func<?, ?> function) {
            return rewriteBinary(function.input(), function.output(), rule, allChildren, Types::function)
                    .map(target -> typeOnly(type, target));
        }
        if (type instanceof Tag.TagType<?> tag) {
            return rule.rewrite(tag.element())
                    .map(child -> liftTag(tag, child));
        }
        if (type instanceof Named.NamedType<?> named) {
            return rule.rewrite(named.element())
                    .map(child -> liftNamed(named, child));
        }
        if (type instanceof Check.CheckType<?> check) {
            if (!check.matchesIndex()) {
                return Maybe.none();
            }
            return rule.rewrite(check.element())
                    .map(child -> liftCheck(check, child));
        }
        if (type instanceof TaggedChoice.TaggedChoiceType<?> choice) {
            return rewriteTaggedChoiceCaptured(choice, rule, allChildren);
        }
        if (type instanceof RecursivePoint.RecursivePointType<?> recursivePoint) {
            return rewriteRecursivePoint(recursivePoint, rule, allChildren);
        }
        if (type instanceof Variant.VariantType variant) {
            return rewriteVariant(variant, rule, allChildren)
                    .map(target -> typeOnly(type, target));
        }
        return Maybe.none();
    }

    private static Maybe<TypeRewriteResult<?, ?>> rewriteProduct(
            Product.ProductType<?, ?> product, TypeRewriteRule rule, boolean allChildren) {
        TypeRewriteResult<?, ?> result = TypeRewriteResult.nop(cast(product));
        Type<?> currentFirst = product.first();
        boolean changed = false;

        Maybe<TypeRewriteResult<?, ?>> first = rule.rewrite(product.first());
        if (first.isDefined()) {
            TypeRewriteResult<?, ?> lifted = liftProductFirst(product.first(), product.second(), first.get());
            result = composeCaptured(result, lifted);
            currentFirst = lifted.targetType() instanceof Product.ProductType<?, ?> target
                    ? target.first()
                    : first.get().targetType();
            changed = true;
            if (!allChildren) {
                return Maybe.some(lifted);
            }
        }

        Maybe<TypeRewriteResult<?, ?>> second = rule.rewrite(product.second());
        if (second.isDefined()) {
            TypeRewriteResult<?, ?> lifted = liftProductSecond(currentFirst, product.second(), second.get());
            result = composeCaptured(result, lifted);
            changed = true;
        }
        return changed ? Maybe.some(result) : Maybe.none();
    }

    private static Maybe<TypeRewriteResult<?, ?>> rewriteSum(
            Sum.SumType<?, ?> sum, TypeRewriteRule rule, boolean allChildren) {
        TypeRewriteResult<?, ?> result = TypeRewriteResult.nop(cast(sum));
        Type<?> currentLeft = sum.left();
        boolean changed = false;

        Maybe<TypeRewriteResult<?, ?>> left = rule.rewrite(sum.left());
        if (left.isDefined()) {
            TypeRewriteResult<?, ?> lifted = liftSumLeft(sum.left(), sum.right(), left.get());
            result = composeCaptured(result, lifted);
            currentLeft = lifted.targetType() instanceof Sum.SumType<?, ?> target
                    ? target.left()
                    : left.get().targetType();
            changed = true;
            if (!allChildren) {
                return Maybe.some(lifted);
            }
        }

        Maybe<TypeRewriteResult<?, ?>> right = rule.rewrite(sum.right());
        if (right.isDefined()) {
            TypeRewriteResult<?, ?> lifted = liftSumRight(currentLeft, sum.right(), right.get());
            result = composeCaptured(result, lifted);
            changed = true;
        }
        return changed ? Maybe.some(result) : Maybe.none();
    }

    private static Maybe<TypeRewriteResult<?, ?>> rewriteValidated(
            Types.ValidatedType<?, ?> validated, TypeRewriteRule rule, boolean allChildren) {
        TypeRewriteResult<?, ?> result = TypeRewriteResult.nop(cast(validated));
        Type<?> currentError = validated.error();
        boolean changed = false;

        Maybe<TypeRewriteResult<?, ?>> error = rule.rewrite(validated.error());
        if (error.isDefined()) {
            TypeRewriteResult<?, ?> lifted = liftValidatedInvalid(validated.error(), validated.value(), error.get());
            result = composeCaptured(result, lifted);
            currentError = lifted.targetType() instanceof Types.ValidatedType<?, ?> target
                    ? target.error()
                    : error.get().targetType();
            changed = true;
            if (!allChildren) {
                return Maybe.some(lifted);
            }
        }

        Maybe<TypeRewriteResult<?, ?>> value = rule.rewrite(validated.value());
        if (value.isDefined()) {
            TypeRewriteResult<?, ?> lifted = liftValidatedValid(currentError, validated.value(), value.get());
            result = composeCaptured(result, lifted);
            changed = true;
        }
        return changed ? Maybe.some(result) : Maybe.none();
    }

    private static Maybe<TypeRewriteResult<?, ?>> rewriteMap(
            Types.MapType<?, ?> map, TypeRewriteRule rule, boolean allChildren) {
        TypeRewriteResult<?, ?> result = TypeRewriteResult.nop(cast(map));
        Type<?> currentKey = map.key();
        boolean changed = false;

        Maybe<TypeRewriteResult<?, ?>> key = rule.rewrite(map.key());
        if (key.isDefined()) {
            TypeRewriteResult<?, ?> lifted = liftMapKey(map.key(), map.value(), key.get());
            result = composeCaptured(result, lifted);
            currentKey = lifted.targetType() instanceof Types.MapType<?, ?> target
                    ? target.key()
                    : key.get().targetType();
            changed = true;
            if (!allChildren) {
                return Maybe.some(lifted);
            }
        }

        Maybe<TypeRewriteResult<?, ?>> value = rule.rewrite(map.value());
        if (value.isDefined()) {
            TypeRewriteResult<?, ?> lifted = liftMapValue(currentKey, map.value(), value.get());
            result = composeCaptured(result, lifted);
            changed = true;
        }
        return changed ? Maybe.some(result) : Maybe.none();
    }

    private static TypeRewriteResult<?, ?> liftProductFirst(
            Type<?> first, Type<?> second, TypeRewriteResult<?, ?> child) {
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult.cast(child);
        return typed.throughOptic(TypedOptic.proj1(cast(first), cast(second), cast(child.targetType())));
    }

    private static TypeRewriteResult<?, ?> liftProductSecond(
            Type<?> first, Type<?> second, TypeRewriteResult<?, ?> child) {
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult.cast(child);
        return typed.throughOptic(TypedOptic.proj2(cast(first), cast(second), cast(child.targetType())));
    }

    private static TypeRewriteResult<?, ?> liftSumLeft(
            Type<?> left, Type<?> right, TypeRewriteResult<?, ?> child) {
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult.cast(child);
        return typed.throughOptic(TypedOptic.inj1(cast(left), cast(right), cast(child.targetType())));
    }

    private static TypeRewriteResult<?, ?> liftSumRight(
            Type<?> left, Type<?> right, TypeRewriteResult<?, ?> child) {
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult.cast(child);
        return typed.throughOptic(TypedOptic.inj2(cast(left), cast(right), cast(child.targetType())));
    }

    private static TypeRewriteResult<?, ?> liftList(
            ListTemplate.ListType<?> list, TypeRewriteResult<?, ?> child) {
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult.cast(child);
        return typed.throughOptic(TypedOptic.list(cast(list.element()), cast(child.targetType())));
    }

    private static TypeRewriteResult<?, ?> liftMaybe(
            Types.MaybeType<?> maybe, TypeRewriteResult<?, ?> child) {
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult.cast(child);
        return typed.throughOptic(TypedOptic.maybe(cast(maybe.value()), cast(child.targetType())));
    }

    private static TypeRewriteResult<?, ?> liftValidatedValid(
            Type<?> error, Type<?> value, TypeRewriteResult<?, ?> child) {
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult.cast(child);
        return typed.throughOptic(TypedOptic.validatedValid(cast(error), cast(value), cast(child.targetType())));
    }

    private static TypeRewriteResult<?, ?> liftValidatedInvalid(
            Type<?> error, Type<?> value, TypeRewriteResult<?, ?> child) {
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult.cast(child);
        return typed.throughOptic(TypedOptic.validatedInvalid(cast(error), cast(child.targetType()), cast(value)));
    }

    private static TypeRewriteResult<?, ?> liftMapKey(
            Type<?> key, Type<?> value, TypeRewriteResult<?, ?> child) {
        Type<?> targetKey = child.targetType();
        Type<Tuple2<Object, Object>> sourceEntry = cast(Types.and(cast(key), cast(value)));
        Type<Tuple2<Object, Object>> targetEntry = cast(Types.and(cast(targetKey), cast(value)));
        TypedOptic<Map<Object, Object>, Map<Object, Object>, Tuple2<Object, Object>, Tuple2<Object, Object>> entries =
                new TypedOptic<>(
                        Traversing.Mu.TYPE_TOKEN,
                        cast(Types.map(cast(key), cast(value))),
                        cast(Types.map(cast(targetKey), cast(value))),
                        sourceEntry,
                        targetEntry,
                        MapOpticElement.entries());
        TypedOptic<Tuple2<Object, Object>, Tuple2<Object, Object>, Object, Object> first =
                TypedOptic.proj1(cast(key), cast(value), cast(targetKey));
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult.cast(child);
        return typed.throughOptic(entries.compose(first));
    }

    private static TypeRewriteResult<?, ?> liftMapValue(
            Type<?> key, Type<?> value, TypeRewriteResult<?, ?> child) {
        Type<?> targetValue = child.targetType();
        Type<Tuple2<Object, Object>> sourceEntry = cast(Types.and(cast(key), cast(value)));
        Type<Tuple2<Object, Object>> targetEntry = cast(Types.and(cast(key), cast(targetValue)));
        TypedOptic<Map<Object, Object>, Map<Object, Object>, Tuple2<Object, Object>, Tuple2<Object, Object>> entries =
                new TypedOptic<>(
                        Traversing.Mu.TYPE_TOKEN,
                        cast(Types.map(cast(key), cast(value))),
                        cast(Types.map(cast(key), cast(targetValue))),
                        sourceEntry,
                        targetEntry,
                        MapOpticElement.entries());
        TypedOptic<Tuple2<Object, Object>, Tuple2<Object, Object>, Object, Object> second =
                TypedOptic.proj2(cast(key), cast(value), cast(targetValue));
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult.cast(child);
        return typed.throughOptic(entries.compose(second));
    }

    private static TypeRewriteResult<?, ?> liftTag(Tag.TagType<?> tag, TypeRewriteResult<?, ?> child) {
        return TypeRewriteResult.cast(child)
                .retag(cast(tag), cast(Types.field(tag.name(), child.targetType())));
    }

    private static TypeRewriteResult<?, ?> liftNamed(Named.NamedType<?> named, TypeRewriteResult<?, ?> child) {
        return TypeRewriteResult.cast(child)
                .retag(cast(named), cast(Types.named(named.name(), child.targetType())));
    }

    private static TypeRewriteResult<?, ?> liftCheck(Check.CheckType<?> check, TypeRewriteResult<?, ?> child) {
        return TypeRewriteResult.cast(child)
                .retag(cast(check), cast(Types.checkedType(check.name(), check.index(), check.expectedIndex(), child.targetType())));
    }

    private static <K> Maybe<TypeRewriteResult<?, ?>> rewriteTaggedChoiceCaptured(
            TaggedChoice.TaggedChoiceType<K> choice, TypeRewriteRule rule, boolean allChildren) {
        TypeRewriteResult<?, ?> result = TypeRewriteResult.nop(cast(choice));
        TaggedChoice.TaggedChoiceType<K> current = choice;
        boolean changed = false;
        for (Map.Entry<K, Type<?>> entry : Object2ObjectMaps.fastIterable(choice.types())) {
            Maybe<Type<?>> currentBranch = current.choiceType(entry.getKey());
            if (currentBranch.isEmpty()) {
                return Maybe.none();
            }
            Maybe<TypeRewriteResult<?, ?>> next = rule.rewrite(currentBranch.get());
            if (next.isDefined()) {
                Maybe<TypeRewriteResult<?, ?>> lifted = liftTagged(current, entry.getKey(), next.get());
                if (lifted.isEmpty()) {
                    return Maybe.none();
                }
                result = composeCaptured(result, lifted.get());
                if (!(lifted.get().targetType() instanceof TaggedChoice.TaggedChoiceType<?> target)) {
                    return Maybe.none();
                }
                current = castTagged(target);
                changed = true;
                if (!allChildren) {
                    return Maybe.some(lifted.get());
                }
            }
        }
        return changed ? Maybe.some(result) : Maybe.none();
    }

    private static <K> Maybe<TypeRewriteResult<?, ?>> liftTagged(
            TaggedChoice.TaggedChoiceType<K> choice,
            K key,
            TypeRewriteResult<?, ?> child) {
        Maybe<TypedOptic<Tuple2<K, ?>, Tuple2<K, ?>, Object, Object>> optic =
                choice.branchOptic(key, cast(child.sourceType()), cast(child.targetType()));
        if (optic.isEmpty()) {
            return Maybe.none();
        }
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult.cast(child);
        return Maybe.some(typed.throughOptic(new CompositePointFreeOptic<>(optic.get())));
    }

    private static Maybe<TypeRewriteResult<?, ?>> rewriteRecursivePoint(
            RecursivePoint.RecursivePointType<?> point,
            TypeRewriteRule rule,
            boolean allChildren) {
        Type<?> unfolded = point.unfold();
        Maybe<TypeRewriteResult<?, ?>> rewritten = rewriteTemplateChildren(unfolded, rule, allChildren);
        if (rewritten.isEmpty()) {
            return Maybe.none();
        }
        RecursivePoint.RecursivePointType<?> targetPoint =
                point.family().buildMuType(point.index(), rewritten.get().targetType());
        TypeRewriteResult<Object, Object> typed = TypeRewriteResult
                .cast(rewritten.get())
                .retag(cast(unfolded), cast(targetPoint.unfold()));
        TypedOptic<Object, Object, Object, Object> optic = TypedOptic.retag(
                cast(point),
                cast(targetPoint),
                cast(unfolded),
                cast(targetPoint.unfold()));
        return Maybe.some(typed.throughOptic(new CompositePointFreeOptic<>(optic))
                .plusRecursiveDependencies(point.index()));
    }

    private static Maybe<TypeRewriteResult<?, ?>> rewriteTemplateChildren(
            Type<?> type,
            TypeRewriteRule rule,
            boolean allChildren) {
        if (type instanceof RecursivePoint.RecursivePointType<?>) {
            return Maybe.none();
        }
        return rewriteChildren(type, templateChildRule(rule, allChildren), allChildren);
    }

    private static TypeRewriteRule templateChildRule(TypeRewriteRule rule, boolean allChildren) {
        return type -> {
            Maybe<TypeRewriteResult<?, ?>> direct = rule.rewrite(type);
            if (direct.isDefined()) {
                return direct;
            }
            if (type instanceof RecursivePoint.RecursivePointType<?>) {
                return Maybe.none();
            }
            return rewriteTemplateChildren(type, rule, allChildren);
        };
    }

    private static Maybe<Type<?>> rewriteVariant(
            Variant.VariantType variant, TypeRewriteRule rule, boolean allChildren) {
        Object2ObjectOpenHashMap<String, Type<?>> rewritten = new Object2ObjectOpenHashMap<>(variant.cases().size());
        boolean changed = false;
        for (Map.Entry<String, Type<?>> entry : Object2ObjectMaps.fastIterable(variant.cases())) {
            Maybe<TypeRewriteResult<?, ?>> next = rule.rewrite(entry.getValue());
            if (next.isDefined()) {
                rewritten.put(entry.getKey(), next.get().targetType());
                changed = true;
                if (!allChildren) {
                    putRemainingVariant(variant.cases(), rewritten, entry.getKey());
                    return Maybe.some(Types.variantType(variant.name(), rewritten));
                }
            } else {
                rewritten.put(entry.getKey(), entry.getValue());
            }
        }
        return changed ? Maybe.some(Types.variantType(variant.name(), rewritten)) : Maybe.none();
    }

    private static Maybe<Type<?>> rewriteUnary(
            Type<?> child, TypeRewriteRule rule, Function<Type<?>, Type<?>> rebuild) {
        Maybe<TypeRewriteResult<?, ?>> rewritten = rule.rewrite(child);
        return rewritten.map(result -> rebuild.apply(result.targetType()));
    }

    private static Maybe<Type<?>> rewriteBinary(
            Type<?> first,
            Type<?> second,
            TypeRewriteRule rule,
            boolean allChildren,
            BiFunction<Type<?>, Type<?>, Type<?>> rebuild) {
        Maybe<TypeRewriteResult<?, ?>> firstRewritten = rule.rewrite(first);
        Maybe<TypeRewriteResult<?, ?>> secondRewritten =
                allChildren || firstRewritten.isEmpty() ? rule.rewrite(second) : Maybe.none();
        if (firstRewritten.isEmpty() && secondRewritten.isEmpty()) {
            return Maybe.none();
        }
        return Maybe.some(rebuild.apply(
                targetOr(firstRewritten, first),
                targetOr(secondRewritten, second)));
    }

    private static TypeRewriteResult<?, ?> typeOnly(Type<?> source, Type<?> target) {
        return TypeRewriteResult.typeOnly(cast(source), cast(target));
    }

    private static Type<?> targetOr(Maybe<TypeRewriteResult<?, ?>> result, Type<?> fallback) {
        return result.isDefined() ? result.get().targetType() : fallback;
    }

    private static TypeRewriteResult<?, ?> composeCaptured(
            TypeRewriteResult<?, ?> first,
            TypeRewriteResult<?, ?> second) {
        TypeRewriteResult<Object, Object> typedFirst = TypeRewriteResult.cast(first);
        TypeRewriteResult<Object, Object> typedSecond = TypeRewriteResult.cast(second);
        return typedFirst.compose(typedSecond);
    }

    private static <K> void putRemaining(
            Object2ObjectMap<K, Type<?>> source, Object2ObjectOpenHashMap<K, Type<?>> target, K currentKey) {
        boolean afterCurrent = false;
        for (Map.Entry<K, Type<?>> entry : Object2ObjectMaps.fastIterable(source)) {
            if (afterCurrent) {
                target.put(entry.getKey(), entry.getValue());
            } else if (Objects.equals(entry.getKey(), currentKey)) {
                afterCurrent = true;
            }
        }
    }

    private static void putRemainingVariant(
            Object2ObjectMap<String, Type<?>> source, Object2ObjectOpenHashMap<String, Type<?>> target, String currentKey) {
        boolean afterCurrent = false;
        for (Map.Entry<String, Type<?>> entry : Object2ObjectMaps.fastIterable(source)) {
            if (afterCurrent) {
                target.put(entry.getKey(), entry.getValue());
            } else if (Objects.equals(entry.getKey(), currentKey)) {
                afterCurrent = true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <A> Type<A> cast(Type<?> type) {
        return (Type<A>) type;
    }

    @SuppressWarnings("unchecked")
    private static <K> TaggedChoice.TaggedChoiceType<K> castTagged(TaggedChoice.TaggedChoiceType<?> type) {
        return (TaggedChoice.TaggedChoiceType<K>) type;
    }
}
