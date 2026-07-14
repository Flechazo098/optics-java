package com.flechazo.optics;

import com.flechazo.optics.internal.OpticPrograms;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a monomorphic write-only optic that may update one or more focuses.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
@FunctionalInterface
public interface Setter<S, A> extends PSetter<S, S, A, A> {
    /**
     * Composes this setter with another setter.
     *
     * @param <B> the composed focus type
     * @param other the setter applied to each focus
     * @return the composed setter
     */
    default <B> Setter<S, B> andThen(Setter<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    /**
     * Composes this setter with an isomorphism.
     *
     * @param <B> the composed focus type
     * @param other the isomorphism applied to each focus
     * @return the composed setter
     */
    default <B> Setter<S, B> andThen(Iso<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    /**
     * Composes this setter with a lens.
     *
     * @param <B> the composed focus type
     * @param other the lens applied to each focus
     * @return the composed setter
     */
    default <B> Setter<S, B> andThen(Lens<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    /**
     * Composes this setter with a prism.
     *
     * @param <B> the composed focus type
     * @param other the prism applied to each focus
     * @return the composed setter
     */
    default <B> Setter<S, B> andThen(Prism<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    /**
     * Composes this setter with an affine optic.
     *
     * @param <B> the composed focus type
     * @param other the affine optic applied to each focus
     * @return the composed setter
     */
    default <B> Setter<S, B> andThen(Affine<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    /**
     * Composes this setter with a traversal.
     *
     * @param <B> the composed focus type
     * @param other the traversal applied to each focus
     * @return the composed setter
     */
    default <B> Setter<S, B> andThen(Traversal<A, B> other) {
        return from(PSetter.super.andThen(other));
    }

    /**
     * Creates a setter from a serializable modifier operation.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param modify the operation that applies a focus transformation
     * @return the resulting setter
     */
    static <S, A> Setter<S, A> of(
            SetterModifier<S, S, A, A> modify) {
        return from(PSetter.of(modify));
    }

    /**
     * Creates a setter from a modifier operation.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param modify the operation that applies a focus transformation
     * @return the resulting setter
     */
    static <S, A> Setter<S, A> of(
            BiFunction<Function<? super A, ? extends A>, S, S> modify) {
        return from(PSetter.of(modify));
    }

    /**
     * Creates a setter from serializable focus read and source rebuild operations.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param getter the operation that reads the focus
     * @param setter the operation that rebuilds the source
     * @return the resulting setter
     */
    static <S, A> Setter<S, A> fromGetSet(
            LensGetter<? super S, ? extends A> getter,
            LensRebuilder<S, A, S> setter) {
        return from(PSetter.fromGetSet(getter, setter));
    }

    /**
     * Creates a setter from focus read and source rebuild operations.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param getter the operation that reads the focus
     * @param setter the operation that rebuilds the source
     * @return the resulting setter
     */
    static <S, A> Setter<S, A> fromGetSet(
            Function<? super S, ? extends A> getter,
            BiFunction<S, A, S> setter) {
        return from(PSetter.fromGetSet(getter, setter));
    }

    /**
     * Returns a setter that focuses on its entire source.
     *
     * @param <S> the source type
     * @return the identity setter
     */
    static <S> Setter<S, S> identity() {
        return from(PSetter.identity());
    }

    /**
     * Returns a monomorphic view of a monomorphic polymorphic setter.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param setter the setter to adapt
     * @return the corresponding monomorphic setter
     */
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
