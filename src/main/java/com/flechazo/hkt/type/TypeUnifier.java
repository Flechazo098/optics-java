package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.Map;
import java.util.Objects;

public final class TypeUnifier {
    private TypeUnifier() {
    }

    public static Maybe<TypeSubstitution> unify(Type<?> left, Type<?> right) {
        return unify(left, right, TypeSubstitution.empty());
    }

    public static Maybe<TypeSubstitution> unify(Type<?> left, Type<?> right, TypeSubstitution seed) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(seed, "seed");
        return unifyInternal(left.substitute(seed), right.substitute(seed), seed);
    }

    private static Maybe<TypeSubstitution> unifyInternal(Type<?> left, Type<?> right, TypeSubstitution substitution) {
        if (left.equals(right)) {
            return Maybe.some(substitution);
        }
        if (left instanceof Type.VariableType<?> variable) {
            return bind(variable, right, substitution);
        }
        if (right instanceof Type.VariableType<?> variable) {
            return bind(variable, left, substitution);
        }
        record TypePair(Type<?> l, Type<?> r) {
        }

        return switch (new TypePair(left, right)) {
            case TypePair(Product.ProductType<?, ?> l, Product.ProductType<?, ?> r) ->
                    unifyPair(l.first(), r.first(), l.second(), r.second(), substitution);

            case TypePair(Sum.SumType<?, ?> l, Sum.SumType<?, ?> r) ->
                    unifyPair(l.left(), r.left(), l.right(), r.right(), substitution);

            case TypePair(ListTemplate.ListType<?> l, ListTemplate.ListType<?> r) ->
                    unify(l.element(), r.element(), substitution);

            case TypePair(CompoundList.CompoundListType<?, ?> l, CompoundList.CompoundListType<?, ?> r) ->
                    unifyPair(l.key(), r.key(), l.element(), r.element(), substitution);

            case TypePair(Types.MapType<?, ?> l, Types.MapType<?, ?> r) ->
                    unifyPair(l.key(), r.key(), l.value(), r.value(), substitution);

            case TypePair(Types.MaybeType<?> l, Types.MaybeType<?> r) -> unify(l.value(), r.value(), substitution);

            case TypePair(Types.ValidatedType<?, ?> l, Types.ValidatedType<?, ?> r) ->
                    unifyPair(l.error(), r.error(), l.value(), r.value(), substitution);

            case TypePair(Func<?, ?> l, Func<?, ?> r) ->
                    unifyPair(l.input(), r.input(), l.output(), r.output(), substitution);

            case TypePair(Tag.TagType<?> l, Tag.TagType<?> r) when l.name().equals(r.name()) ->
                    unify(l.element(), r.element(), substitution);

            case TypePair(Named.NamedType<?> l, Named.NamedType<?> r) when l.name().equals(r.name()) ->
                    unify(l.element(), r.element(), substitution);

            case TypePair(Check.CheckType<?> l, Check.CheckType<?> r)
                    when l.name().equals(r.name()) && l.index() == r.index() && l.expectedIndex() == r.expectedIndex() ->
                    unify(l.element(), r.element(), substitution);

            case TypePair(TaggedChoice.TaggedChoiceType<?> l, TaggedChoice.TaggedChoiceType<?> r) ->
                    unifyTagged(l, r, substitution);

            default -> Maybe.none();
        };
    }

    private static Maybe<TypeSubstitution> bind(
            Type.VariableType<?> variable,
            Type<?> replacement,
            TypeSubstitution substitution) {
        if (replacement.containsVariable(variable.name())) {
            return Maybe.none();
        }
        return Maybe.some(substitution.plusVariable(variable.name(), replacement).normalized());
    }

    private static Maybe<TypeSubstitution> unifyPair(
            Type<?> left1,
            Type<?> right1,
            Type<?> left2,
            Type<?> right2,
            TypeSubstitution substitution) {
        Maybe<TypeSubstitution> first = unify(left1, right1, substitution);
        return first.flatMap(next -> unify(left2, right2, next));
    }

    private static Maybe<TypeSubstitution> unifyTagged(
            TaggedChoice.TaggedChoiceType<?> left,
            TaggedChoice.TaggedChoiceType<?> right,
            TypeSubstitution substitution) {
        if (!left.name().equals(right.name()) || !left.types().keySet().equals(right.types().keySet())) {
            return Maybe.none();
        }
        Maybe<TypeSubstitution> current = unify(left.keyType(), right.keyType(), substitution);
        for (Map.Entry<?, Type<?>> entry : left.types().entrySet()) {
            Object key = entry.getKey();
            Object2ObjectMap<?, Type<?>> rightTypes = right.types();
            current = current.flatMap(next -> unify(entry.getValue(), rightTypes.get(key), next));
            if (current.isEmpty()) {
                return Maybe.none();
            }
        }
        return current;
    }
}
