package com.flechazo.optics;

import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a polymorphic write-only optic that may update one or more focuses.
 *
 * @param <S> the input source type
 * @param <T> the rebuilt source type
 * @param <A> the input focus type
 * @param <B> the replacement focus type
 */
@FunctionalInterface
public interface PSetter<S, T, A, B> {
    /**
     * Applies a transformation to every focus and returns the rebuilt source.
     *
     * @param f the focus transformation
     * @param source the source to update
     * @return the rebuilt source
     */
    T modify(Function<? super A, ? extends B> f, S source);

    /**
     * Replaces every focus with a constant value.
     *
     * @param value the replacement focus
     * @param source the source to update
     * @return the rebuilt source
     */
    default T set(B value, S source) {
        return modify(ignored -> value, source);
    }

    /**
     * Composes this setter with another setter.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the setter applied to each focus
     * @return the composed setter
     */
    default <C, D> PSetter<S, T, C, D> andThen(PSetter<A, B, C, D> other) {
        PSetter<S, T, A, B> self = this;
        PSetter<S, T, C, D> direct = (f, source) -> self.modify(value -> other.modify(f, value), source);
        return OpticPrograms.setter(direct, OpticPrograms.compose(self, other));
    }

    /**
     * Composes this setter with an isomorphism.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the isomorphism applied to each focus
     * @return the composed setter
     */
    default <C, D> PSetter<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PSetter<S, T, C, D> direct =
                (f, source) -> modify(value -> other.reverseGet(f.apply(other.get(value))), source);
        return OpticPrograms.setter(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this setter with a lens.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the lens applied to each focus
     * @return the composed setter
     */
    default <C, D> PSetter<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    /**
     * Composes this setter with an affine optic.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the affine optic applied to each focus
     * @return the composed setter
     */
    default <C, D> PSetter<S, T, C, D> andThen(PAffine<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    /**
     * Composes this setter with a traversal.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the traversal applied to each focus
     * @return the composed setter
     */
    default <C, D> PSetter<S, T, C, D> andThen(PTraversal<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    /**
     * Composes this setter with a prism.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the prism applied to each focus
     * @return the composed setter
     */
    default <C, D> PSetter<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        return andThen(other.asSetter());
    }

    /**
     * Creates a setter from a serializable modifier operation.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param modify the operation that applies a focus transformation to a source
     * @return the resulting setter
     */
    static <S, T, A, B> PSetter<S, T, A, B> of(
            SetterModifier<S, T, A, B> modify) {
        PSetter<S, T, A, B> direct = of(
                (BiFunction<Function<? super A, ? extends B>, S, T>) modify);
        return LambdaLifter.setter(direct, modify);
    }

    /**
     * Creates a setter from a modifier operation.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param modify the operation that applies a focus transformation to a source
     * @return the resulting setter
     */
    static <S, T, A, B> PSetter<S, T, A, B> of(
            BiFunction<Function<? super A, ? extends B>, S, T> modify) {
        PSetter<S, T, A, B> direct = modify::apply;
        return OpticPrograms.setter(direct, OpticPrograms.opaque("setter", null));
    }

    /**
     * Creates a setter from serializable focus read and source rebuild operations.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param getter the operation that reads the current focus
     * @param setter the operation that rebuilds a source with a replacement focus
     * @return the resulting setter
     */
    static <S, T, A, B> PSetter<S, T, A, B> fromGetSet(
            LensGetter<? super S, ? extends A> getter,
            LensRebuilder<S, B, T> setter) {
        PSetter<S, T, A, B> direct = (f, source) -> setter.apply(source, f.apply(getter.apply(source)));
        return LambdaLifter.setter(direct, getter, setter);
    }

    /**
     * Creates a setter from focus read and source rebuild operations.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param getter the operation that reads the current focus
     * @param setter the operation that rebuilds a source with a replacement focus
     * @return the resulting setter
     */
    static <S, T, A, B> PSetter<S, T, A, B> fromGetSet(
            Function<? super S, ? extends A> getter,
            BiFunction<S, B, T> setter) {
        PSetter<S, T, A, B> direct = (f, source) -> setter.apply(source, f.apply(getter.apply(source)));
        return OpticPrograms.setter(direct, OpticPrograms.opaque("setter", null));
    }

    /**
     * Returns a setter that focuses on its entire source.
     *
     * @param <S> the source type
     * @return the identity setter
     */
    static <S> PSetter<S, S, S, S> identity() {
        PSetter<S, S, S, S> direct = Function::apply;
        return OpticPrograms.setter(direct, OpticPrograms.structured("identitySetter", null));
    }
}
