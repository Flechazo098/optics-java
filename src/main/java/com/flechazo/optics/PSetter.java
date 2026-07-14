package com.flechazo.optics;

import java.util.function.BiFunction;
import java.util.function.Function;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

@FunctionalInterface
public interface PSetter<S, T, A, B> {
    T modify(Function<? super A, ? extends B> f, S source);

    default T set(B value, S source) {
        return modify(ignored -> value, source);
    }

    default <C, D> PSetter<S, T, C, D> andThen(PSetter<A, B, C, D> other) {
        PSetter<S, T, A, B> self = this;
        PSetter<S, T, C, D> direct = (f, source) -> self.modify(value -> other.modify(f, value), source);
        return OpticPrograms.setter(direct, OpticPrograms.compose(self, other));
    }

    default <C, D> PSetter<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PSetter<S, T, C, D> direct =
                (f, source) -> modify(value -> other.reverseGet(f.apply(other.get(value))), source);
        return OpticPrograms.setter(direct, OpticPrograms.compose(this, other));
    }

    default <C, D> PSetter<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    default <C, D> PSetter<S, T, C, D> andThen(PAffine<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    default <C, D> PSetter<S, T, C, D> andThen(PTraversal<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    default <C, D> PSetter<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    static <S, T, A, B> PSetter<S, T, A, B> of(
            SetterModifier<S, T, A, B> modify) {
        PSetter<S, T, A, B> direct = of(
                (BiFunction<Function<? super A, ? extends B>, S, T>) modify);
        return LambdaLifter.setter(direct, modify);
    }

    static <S, T, A, B> PSetter<S, T, A, B> of(
            BiFunction<Function<? super A, ? extends B>, S, T> modify) {
        PSetter<S, T, A, B> direct = modify::apply;
        return OpticPrograms.setter(direct, OpticPrograms.opaque("setter", null));
    }

    static <S, T, A, B> PSetter<S, T, A, B> fromGetSet(
            LensGetter<? super S, ? extends A> getter,
            LensRebuilder<S, B, T> setter) {
        PSetter<S, T, A, B> direct = (f, source) -> setter.apply(source, f.apply(getter.apply(source)));
        return LambdaLifter.setter(direct, getter, setter);
    }

    static <S, T, A, B> PSetter<S, T, A, B> fromGetSet(
            Function<? super S, ? extends A> getter,
            BiFunction<S, B, T> setter) {
        PSetter<S, T, A, B> direct = (f, source) -> setter.apply(source, f.apply(getter.apply(source)));
        return OpticPrograms.setter(direct, OpticPrograms.opaque("setter", null));
    }

    static <S> PSetter<S, S, S, S> identity() {
        PSetter<S, S, S, S> direct = Function::apply;
        return OpticPrograms.setter(direct, OpticPrograms.structured("identitySetter", null));
    }
}
