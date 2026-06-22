package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TypeUnifier {
    private TypeUnifier() {
    }

    public static Maybe<TypeSubstitution> unify(TypeExpr left, TypeExpr right) {
        return unify(left, right, TypeSubstitution.empty());
    }

    public static Maybe<TypeSubstitution> unify(TypeExpr left, TypeExpr right, TypeSubstitution seed) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(seed, "seed");
        return unifyInternal(left.substitute(seed), right.substitute(seed), seed);
    }

    private static Maybe<TypeSubstitution> unifyInternal(
            TypeExpr left,
            TypeExpr right,
            TypeSubstitution substitution) {
        if (left.equals(right)) {
            return Maybe.some(substitution);
        }
        if (left instanceof TypeExpr.Variable variable) {
            return bind(variable, right, substitution);
        }
        if (right instanceof TypeExpr.Variable variable) {
            return bind(variable, left, substitution);
        }
        if (left.kind() != right.kind()) {
            return Maybe.none();
        }
        return switch (left) {
            case TypeExpr.Witness ignored -> Maybe.none();
            case TypeExpr.Variable ignored -> throw new IllegalStateException("variable case handled before switch");
            case TypeExpr.Product l -> {
                TypeExpr.Product r = (TypeExpr.Product) right;
                yield unifyAll(List.of(l.first(), l.second()), List.of(r.first(), r.second()), substitution);
            }
            case TypeExpr.Sum l -> {
                TypeExpr.Sum r = (TypeExpr.Sum) right;
                yield unifyAll(List.of(l.left(), l.right()), List.of(r.left(), r.right()), substitution);
            }
            case TypeExpr.ListOf l -> unifyInternal(l.element(), ((TypeExpr.ListOf) right).element(), substitution);
            case TypeExpr.MapOf l -> {
                TypeExpr.MapOf r = (TypeExpr.MapOf) right;
                yield unifyAll(List.of(l.key(), l.value()), List.of(r.key(), r.value()), substitution);
            }
            case TypeExpr.OptionalOf l -> unifyInternal(l.value(), ((TypeExpr.OptionalOf) right).value(), substitution);
            case TypeExpr.TaggedChoice l -> unifyTagged(l, (TypeExpr.TaggedChoice) right, substitution);
            case TypeExpr.Variant l -> unifyVariant(l, (TypeExpr.Variant) right, substitution);
            case TypeExpr.FunctionType l -> {
                TypeExpr.FunctionType r = (TypeExpr.FunctionType) right;
                yield unifyAll(List.of(l.argument(), l.result()), List.of(r.argument(), r.result()), substitution);
            }
            case TypeExpr.RecursiveSlot l -> l.family().equals(((TypeExpr.RecursiveSlot) right).family())
                    && l.index() == ((TypeExpr.RecursiveSlot) right).index()
                    ? Maybe.some(substitution)
                    : Maybe.none();
            case TypeExpr.RecordType l -> unifyRecord(l, (TypeExpr.RecordType) right, substitution);
        };
    }

    private static Maybe<TypeSubstitution> bind(
            TypeExpr.Variable variable,
            TypeExpr replacement,
            TypeSubstitution substitution) {
        if (replacement.containsVariable(variable.name())) {
            return Maybe.none();
        }
        TypeSubstitution next = substitution.plusVariable(variable.name(), replacement);
        return Maybe.some(next.normalized());
    }

    private static Maybe<TypeSubstitution> unifyAll(
            List<TypeExpr> left,
            List<TypeExpr> right,
            TypeSubstitution substitution) {
        if (left.size() != right.size()) {
            return Maybe.none();
        }
        TypeSubstitution current = substitution;
        for (int i = 0; i < left.size(); i++) {
            Maybe<TypeSubstitution> next = unify(left.get(i), right.get(i), current);
            if (next.isEmpty()) {
                return Maybe.none();
            }
            current = next.get();
        }
        return Maybe.some(current);
    }

    private static Maybe<TypeSubstitution> unifyTagged(
            TypeExpr.TaggedChoice left,
            TypeExpr.TaggedChoice right,
            TypeSubstitution substitution) {
        if (!left.name().equals(right.name()) || !left.choices().keySet().equals(right.choices().keySet())) {
            return Maybe.none();
        }
        ArrayList<TypeExpr> leftTypes = new ArrayList<>();
        ArrayList<TypeExpr> rightTypes = new ArrayList<>();
        leftTypes.add(left.keyType());
        rightTypes.add(right.keyType());
        for (Map.Entry<?, TypeExpr> entry : left.choices().entrySet()) {
            leftTypes.add(entry.getValue());
            rightTypes.add(right.choices().get(entry.getKey()));
        }
        return unifyAll(leftTypes, rightTypes, substitution);
    }

    private static Maybe<TypeSubstitution> unifyVariant(
            TypeExpr.Variant left,
            TypeExpr.Variant right,
            TypeSubstitution substitution) {
        if (!left.name().equals(right.name()) || left.cases().size() != right.cases().size()) {
            return Maybe.none();
        }
        ArrayList<TypeExpr> leftTypes = new ArrayList<>();
        ArrayList<TypeExpr> rightTypes = new ArrayList<>();
        for (int i = 0; i < left.cases().size(); i++) {
            TypeExpr.VariantCase l = left.cases().get(i);
            TypeExpr.VariantCase r = right.cases().get(i);
            if (!l.name().equals(r.name())) {
                return Maybe.none();
            }
            leftTypes.add(l.type());
            rightTypes.add(r.type());
        }
        return unifyAll(leftTypes, rightTypes, substitution);
    }

    private static Maybe<TypeSubstitution> unifyRecord(
            TypeExpr.RecordType left,
            TypeExpr.RecordType right,
            TypeSubstitution substitution) {
        if (!left.name().equals(right.name()) || left.fields().size() != right.fields().size()) {
            return Maybe.none();
        }
        ArrayList<TypeExpr> leftTypes = new ArrayList<>();
        ArrayList<TypeExpr> rightTypes = new ArrayList<>();
        for (int i = 0; i < left.fields().size(); i++) {
            TypeExpr.RecordField l = left.fields().get(i);
            TypeExpr.RecordField r = right.fields().get(i);
            if (!l.name().equals(r.name())) {
                return Maybe.none();
            }
            leftTypes.add(l.type());
            rightTypes.add(r.type());
        }
        return unifyAll(leftTypes, rightTypes, substitution);
    }
}
