package com.flechazo.hkt.type;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Objects;

public final class TypeSubstitution {
    private final Object2ObjectOpenHashMap<String, Type<?>> variables;
    private final Object2ObjectOpenHashMap<RecursivePoint.RecursivePointType<?>, Type<?>> recursivePoints;

    private TypeSubstitution(
            Object2ObjectOpenHashMap<String, Type<?>> variables,
            Object2ObjectOpenHashMap<RecursivePoint.RecursivePointType<?>, Type<?>> recursivePoints) {
        this.variables = variables;
        this.recursivePoints = recursivePoints;
    }

    public static TypeSubstitution empty() {
        return new TypeSubstitution(new Object2ObjectOpenHashMap<>(), new Object2ObjectOpenHashMap<>());
    }

    public static TypeSubstitution variable(String name, Type<?> replacement) {
        TypeSubstitution substitution = empty();
        substitution.variables.put(Type.requireName(name, "name"), Objects.requireNonNull(replacement, "replacement"));
        return substitution;
    }

    public TypeSubstitution plusVariable(String name, Type<?> replacement) {
        Object2ObjectOpenHashMap<String, Type<?>> nextVariables = new Object2ObjectOpenHashMap<>(variables);
        nextVariables.put(Type.requireName(name, "name"), Objects.requireNonNull(replacement, "replacement"));
        return new TypeSubstitution(nextVariables, new Object2ObjectOpenHashMap<>(recursivePoints));
    }

    public TypeSubstitution plusRecursivePoint(RecursivePoint.RecursivePointType<?> point, Type<?> replacement) {
        Object2ObjectOpenHashMap<RecursivePoint.RecursivePointType<?>, Type<?>> nextPoints =
                new Object2ObjectOpenHashMap<>(recursivePoints);
        nextPoints.put(Objects.requireNonNull(point, "point"), Objects.requireNonNull(replacement, "replacement"));
        return new TypeSubstitution(new Object2ObjectOpenHashMap<>(variables), nextPoints);
    }

    public Type<?> apply(Type<?> type) {
        Objects.requireNonNull(type, "type");
        switch (type) {
            case Type.VariableType<?> variable -> {
                return variables.getOrDefault(variable.name(), variable);
            }
            case RecursivePoint.RecursivePointType<?> point -> {
                return recursivePoints.getOrDefault(point, point);
            }
            case Product.ProductType<?, ?> product -> {
                return Types.and(apply(product.first()), apply(product.second()));
            }
            case Sum.SumType<?, ?> sum -> {
                return Types.or(apply(sum.left()), apply(sum.right()));
            }
            case ListTemplate.ListType<?> list -> {
                return Types.list(apply(list.element()));
            }
            case CompoundList.CompoundListType<?, ?> compoundList -> {
                return Types.compoundList(apply(compoundList.key()), apply(compoundList.element()));
            }
            case Types.MapType<?, ?> map -> {
                return Types.map(apply(map.key()), apply(map.value()));
            }
            case Types.MaybeType<?> maybe -> {
                return Types.maybe(apply(maybe.value()));
            }
            case Types.ValidatedType<?, ?> validated -> {
                return Types.validated(apply(validated.error()), apply(validated.value()));
            }
            case Func<?, ?> function -> {
                return Types.function(apply(function.input()), apply(function.output()));
            }
            case Tag.TagType<?> tag -> {
                return Types.field(tag.name(), apply(tag.element()));
            }
            case Named.NamedType<?> named -> {
                return Types.named(named.name(), apply(named.element()));
            }
            case Check.CheckType<?> check -> {
                return Types.checkedType(check.name(), check.index(), check.expectedIndex(), apply(check.element()));
            }
            case TaggedChoice.TaggedChoiceType<?> choice -> {
                return applyTaggedChoice(choice);
            }
            case Variant.VariantType variant -> {
                Object2ObjectOpenHashMap<String, Type<?>> next = new Object2ObjectOpenHashMap<>(variant.cases().size());
                for (Map.Entry<String, Type<?>> entry : variant.cases().entrySet()) {
                    next.put(entry.getKey(), apply(entry.getValue()));
                }
                return Types.variantType(variant.name(), next);
            }
            default -> {
            }
        }
        return type;
    }

    public TypeSubstitution normalized() {
        TypeSubstitution result = empty();
        for (Map.Entry<String, Type<?>> entry : variables.object2ObjectEntrySet()) {
            result = result.plusVariable(entry.getKey(), entry.getValue().substitute(withoutVariable(entry.getKey())));
        }
        for (Map.Entry<RecursivePoint.RecursivePointType<?>, Type<?>> entry : recursivePoints.object2ObjectEntrySet()) {
            result = result.plusRecursivePoint(entry.getKey(), entry.getValue().substitute(this));
        }
        return result;
    }

    private TypeSubstitution withoutVariable(String name) {
        Object2ObjectOpenHashMap<String, Type<?>> nextVariables = new Object2ObjectOpenHashMap<>(variables);
        nextVariables.remove(name);
        return new TypeSubstitution(nextVariables, new Object2ObjectOpenHashMap<>(recursivePoints));
    }

    private <K> Type<?> applyTaggedChoice(TaggedChoice.TaggedChoiceType<K> choice) {
        Object2ObjectOpenHashMap<K, Type<?>> next = new Object2ObjectOpenHashMap<>(choice.types().size());
        for (Map.Entry<K, Type<?>> entry : choice.types().entrySet()) {
            next.put(entry.getKey(), apply(entry.getValue()));
        }
        return Types.taggedChoiceType(choice.name(), choice.keyType(), next);
    }
}
