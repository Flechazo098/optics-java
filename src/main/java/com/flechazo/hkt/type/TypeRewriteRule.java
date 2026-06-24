package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface TypeRewriteRule {
    Maybe<Type<?>> rewrite(Type<?> type);

    default Type<?> rewriteOrSame(Type<?> type) {
        Objects.requireNonNull(type, "type");
        return rewrite(type).orElse(type);
    }

    static TypeRewriteRule choice(TypeRewriteRule first, TypeRewriteRule second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return type -> first.rewrite(type).or(() -> second.rewrite(type));
    }

    static TypeRewriteRule seq(TypeRewriteRule first, TypeRewriteRule second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return type -> first.rewrite(type).flatMap(second::rewrite);
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
            Type<?> rewrittenChildren = rewriteChildren(type, bottomUp(rule), true).orElse(type);
            return rule.rewrite(rewrittenChildren)
                    .or(() -> rewrittenChildren == type || rewrittenChildren.equals(type)
                            ? Maybe.none()
                            : Maybe.some(rewrittenChildren));
        };
    }

    static TypeRewriteRule once(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return type -> rule.rewrite(type).or(() -> rewriteChildren(type, once(rule), false));
    }

    static TypeRewriteRule many(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return type -> {
            Type<?> current = type;
            boolean changed = false;
            while (true) {
                Maybe<Type<?>> next = rule.rewrite(current);
                if (next.isEmpty()) {
                    return changed ? Maybe.some(current) : Maybe.none();
                }
                current = next.get();
                changed = true;
            }
        };
    }

    private static Maybe<Type<?>> rewriteChildren(Type<?> type, TypeRewriteRule rule, boolean allChildren) {
        if (type instanceof Product.ProductType<?, ?> product) {
            return rewriteBinary(product.first(), product.second(), rule, allChildren, Types::and);
        }
        if (type instanceof Sum.SumType<?, ?> sum) {
            return rewriteBinary(sum.left(), sum.right(), rule, allChildren, Types::or);
        }
        if (type instanceof ListTemplate.ListType<?> list) {
            return rewriteUnary(list.element(), rule, Types::list);
        }
        if (type instanceof CompoundList.CompoundListType<?, ?> compound) {
            return rewriteBinary(compound.key(), compound.element(), rule, allChildren, Types::compoundList);
        }
        if (type instanceof Types.MapType<?, ?> map) {
            return rewriteBinary(map.key(), map.value(), rule, allChildren, Types::map);
        }
        if (type instanceof Types.MaybeType<?> maybe) {
            return rewriteUnary(maybe.value(), rule, Types::maybe);
        }
        if (type instanceof Types.ValidatedType<?, ?> validated) {
            return rewriteBinary(validated.error(), validated.value(), rule, allChildren, Types::validated);
        }
        if (type instanceof Func<?, ?> function) {
            return rewriteBinary(function.input(), function.output(), rule, allChildren, Types::function);
        }
        if (type instanceof Tag.TagType<?> tag) {
            return rewriteUnary(tag.element(), rule, child -> Types.field(tag.name(), child));
        }
        if (type instanceof Named.NamedType<?> named) {
            return rewriteUnary(named.element(), rule, child -> Types.named(named.name(), child));
        }
        if (type instanceof Check.CheckType<?> check) {
            return rewriteUnary(check.element(), rule, child -> Types.checkedType(check.name(), check.index(), child));
        }
        if (type instanceof TaggedChoice.TaggedChoiceType<?> choice) {
            return rewriteTaggedChoiceCaptured(choice, rule, allChildren);
        }
        if (type instanceof Variant.VariantType variant) {
            return rewriteVariant(variant, rule, allChildren);
        }
        return Maybe.none();
    }

    private static <K> Maybe<Type<?>> rewriteTaggedChoiceCaptured(
            TaggedChoice.TaggedChoiceType<K> choice, TypeRewriteRule rule, boolean allChildren) {
        Object2ObjectOpenHashMap<K, Type<?>> rewritten = new Object2ObjectOpenHashMap<>(choice.types().size());
        boolean changed = false;
        for (Map.Entry<K, Type<?>> entry : Object2ObjectMaps.fastIterable(choice.types())) {
            Maybe<Type<?>> next = rule.rewrite(entry.getValue());
            if (next.isDefined()) {
                rewritten.put(entry.getKey(), next.get());
                changed = true;
                if (!allChildren) {
                    putRemaining(choice.types(), rewritten, entry.getKey());
                    return Maybe.some(Types.taggedChoiceType(choice.name(), choice.keyType(), rewritten));
                }
            } else {
                rewritten.put(entry.getKey(), entry.getValue());
            }
        }
        return changed ? Maybe.some(Types.taggedChoiceType(choice.name(), choice.keyType(), rewritten)) : Maybe.none();
    }

    private static Maybe<Type<?>> rewriteVariant(
            Variant.VariantType variant, TypeRewriteRule rule, boolean allChildren) {
        Object2ObjectOpenHashMap<String, Type<?>> rewritten = new Object2ObjectOpenHashMap<>(variant.cases().size());
        boolean changed = false;
        for (Map.Entry<String, Type<?>> entry : Object2ObjectMaps.fastIterable(variant.cases())) {
            Maybe<Type<?>> next = rule.rewrite(entry.getValue());
            if (next.isDefined()) {
                rewritten.put(entry.getKey(), next.get());
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
        Maybe<Type<?>> rewritten = rule.rewrite(child);
        return rewritten.map(rebuild);
    }

    private static Maybe<Type<?>> rewriteBinary(
            Type<?> first,
            Type<?> second,
            TypeRewriteRule rule,
            boolean allChildren,
            BiFunction<Type<?>, Type<?>, Type<?>> rebuild) {
        Maybe<Type<?>> firstRewritten = rule.rewrite(first);
        Maybe<Type<?>> secondRewritten = allChildren || firstRewritten.isEmpty() ? rule.rewrite(second) : Maybe.none();
        if (firstRewritten.isEmpty() && secondRewritten.isEmpty()) {
            return Maybe.none();
        }
        return Maybe.some(rebuild.apply(firstRewritten.orElse(first), secondRewritten.orElse(second)));
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

}
