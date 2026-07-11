package com.flechazo.optics;

import java.util.function.BiFunction;
import java.util.function.Function;
import com.flechazo.optics.internal.OpticPrograms;

@FunctionalInterface
public interface Setter<S, A> extends PSetter<S, S, A, A> {
    default <B> Setter<S, B> andThen(Setter<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    default <B> Setter<S, B> andThen(Iso<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    default <B> Setter<S, B> andThen(Lens<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    default <B> Setter<S, B> andThen(Prism<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    default <B> Setter<S, B> andThen(Affine<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    default <B> Setter<S, B> andThen(Traversal<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    static <S, A> Setter<S, A> of(
            SetterModifier<S, S, A, A> modify) {
        return from(PSetter.of(modify));
    }

    static <S, A> Setter<S, A> of(
            BiFunction<Function<? super A, ? extends A>, S, S> modify) {
        return from(PSetter.of(modify));
    }

    static <S, A> Setter<S, A> fromGetSet(
            LensGetter<? super S, ? extends A> getter,
            LensRebuilder<S, A, S> setter) {
        return from(PSetter.fromGetSet(getter, setter));
    }

    static <S, A> Setter<S, A> fromGetSet(
            Function<? super S, ? extends A> getter,
            BiFunction<S, A, S> setter) {
        return from(PSetter.fromGetSet(getter, setter));
    }

    static <S> Setter<S, S> identity() {
        return from(PSetter.identity());
    }

    static <S, A> Setter<S, A> from(PSetter<S, S, A, A> setter) {
        Setter<S, A> direct;
        if (setter instanceof Setter<?, ?> simple) {
            @SuppressWarnings("unchecked")
            Setter<S, A> result = (Setter<S, A>) simple;
            direct = result;
        } else {
            direct = setter::modify;
        }
        return OpticPrograms.setter(direct, OpticPrograms.programOrOpaque(setter, "setter"));
    }
}
