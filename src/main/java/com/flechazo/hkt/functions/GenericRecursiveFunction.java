package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.google.common.reflect.TypeToken;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record GenericRecursiveFunction<A extends RecursiveTerm<A>>(
        String name,
        RecursiveFamily family,
        int index,
        AlgebraPlan algebra,
        Type<A> recursiveType)
        implements PointFree<Function<A, A>> {
    public GenericRecursiveFunction {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(family, "family");
        family.checkIndex(index);
        Objects.requireNonNull(algebra, "algebra");
        if (!family.equals(algebra.family())) {
            throw new IllegalArgumentException("algebra family must match generic recursive function family");
        }
        Objects.requireNonNull(recursiveType, "recursiveType");
    }

    public static <A extends RecursiveTerm<A>> GenericRecursiveFunction<A> of(
            String name,
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra) {
        //noinspection Convert2Diamond
        return new GenericRecursiveFunction<A>(
                name,
                family,
                index,
                algebra,
                castType(Types.variable(family.name() + "#" + index)));
    }

    public static <A extends RecursiveTerm<A>> GenericRecursiveFunction<A> of(
            String name,
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            Type<A> recursiveType) {
        return new GenericRecursiveFunction<>(name, family, index, algebra, recursiveType);
    }

    public static <A extends RecursiveTerm<A>> GenericRecursiveFunction<A> of(
            String name,
            RecursiveFamily family,
            int index,
            AlgebraPlan algebra,
            TypeToken<A> recursiveType) {
        return of(name, family, index, algebra, Types.witness(recursiveType));
    }

    public CataPlan<A> specialize() {
        return CataPlan.forTerms(family, index, algebra, recursiveType);
    }

    @Override
    public Function<A, A> eval() {
        return specialize().eval();
    }

    @Override
    public Type<Function<A, A>> type() {
        return Types.function(recursiveType, recursiveType);
    }

    @Override
    @NonNull
    public String toString() {
        return "genericRecursive(" + name + ", " + family.name() + "#" + index + ")";
    }

    @SuppressWarnings("unchecked")
    private static <A> Type<A> castType(Type<?> type) {
        return (Type<A>) type;
    }
}
