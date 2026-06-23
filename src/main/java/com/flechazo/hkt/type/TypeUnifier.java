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
        if (left.getClass() != right.getClass()) {
            return Maybe.none();
        }
        if (left instanceof Product.ProductType<?, ?> l && right instanceof Product.ProductType<?, ?> r) {
            return unifyPair(l.first(), r.first(), l.second(), r.second(), substitution);
        }
        if (left instanceof Sum.SumType<?, ?> l && right instanceof Sum.SumType<?, ?> r) {
            return unifyPair(l.left(), r.left(), l.right(), r.right(), substitution);
        }
        if (left instanceof ListTemplate.ListType<?> l && right instanceof ListTemplate.ListType<?> r) {
            return unify(l.element(), r.element(), substitution);
        }
        if (left instanceof CompoundList.CompoundListType<?, ?> l && right instanceof CompoundList.CompoundListType<?, ?> r) {
            return unifyPair(l.key(), r.key(), l.element(), r.element(), substitution);
        }
        if (left instanceof Types.MapType<?, ?> l && right instanceof Types.MapType<?, ?> r) {
            return unifyPair(l.key(), r.key(), l.value(), r.value(), substitution);
        }
        if (left instanceof Func<?, ?> l && right instanceof Func<?, ?> r) {
            return unifyPair(l.input(), r.input(), l.output(), r.output(), substitution);
        }
        if (left instanceof Tag.TagType<?> l && right instanceof Tag.TagType<?> r) {
            return l.name().equals(r.name()) ? unify(l.element(), r.element(), substitution) : Maybe.none();
        }
        if (left instanceof Named.NamedType<?> l && right instanceof Named.NamedType<?> r) {
            return l.name().equals(r.name()) ? unify(l.element(), r.element(), substitution) : Maybe.none();
        }
        if (left instanceof Check.CheckType<?> l && right instanceof Check.CheckType<?> r) {
            return l.name().equals(r.name()) && l.index() == r.index()
                    ? unify(l.element(), r.element(), substitution)
                    : Maybe.none();
        }
        if (left instanceof TaggedChoice.TaggedChoiceType<?> l && right instanceof TaggedChoice.TaggedChoiceType<?> r) {
            return unifyTagged(l, r, substitution);
        }
        return Maybe.none();
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
