package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Selective;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a monomorphic affine optic that focuses on at most one value.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
public interface Affine<S, A> extends PAffine<S, S, A, A> {
    /**
     * Returns a traversal containing zero or one focus.
     *
     * @return the traversal view
     */
    @Override
    default Traversal<S, A> asTraversal() {
        return Traversal.from(PAffine.super.asTraversal());
    }

    /**
     * Returns a setter with this affine optic's conditional update behavior.
     *
     * @return the setter view
     */
    @Override
    default Setter<S, A> asSetter() {
        return Setter.from(PAffine.super.asSetter());
    }

    /**
     * Applies an effectful modifier when an effectful condition is true.
     *
     * @param <F> the selective witness type
     * @param condition the effectful condition evaluated for a present focus
     * @param modifier the transformation selected when the condition is true
     * @param source the source to transform
     * @param selective the selective used to evaluate and combine effects
     * @return the rebuilt source in the selective context
     */
    default <F extends K1> App<F, S> modifyWhenS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.modifyWhen(this, condition, modifier, source, selective);
    }

    /**
     * Applies an effectful modifier when an effectful condition is false.
     *
     * @param <F> the selective witness type
     * @param condition the effectful condition evaluated for a present focus
     * @param modifier the transformation selected when the condition is false
     * @param source the source to transform
     * @param selective the selective used to evaluate and combine effects
     * @return the rebuilt source in the selective context
     */
    default <F extends K1> App<F, S> modifyUnlessS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.modifyUnless(this, condition, modifier, source, selective);
    }

    /**
     * Applies a modifier when a predicate accepts the present focus.
     *
     * @param predicate the condition applied to a present focus
     * @param modifier the transformation applied when the condition is true
     * @param source the source to transform
     * @return the rebuilt source
     */
    default S modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends A> modifier,
            S source) {
        return PAffine.super.modifyWhen(predicate, modifier, Function.identity(), source);
    }

    /**
     * Composes this affine optic with another affine optic.
     *
     * @param <C> the composed focus type
     * @param other the affine optic applied to a present focus
     * @return the composed affine optic
     */
    default <C> Affine<S, C> andThen(Affine<A, C> other) {
        return from(PAffine.super.andThen(other));
    }

    /**
     * Composes this affine optic with an isomorphism.
     *
     * @param <C> the composed focus type
     * @param other the isomorphism applied to a present focus
     * @return the composed affine optic
     */
    default <C> Affine<S, C> andThen(Iso<A, C> other) {
        return from(PAffine.super.andThen(other));
    }

    /**
     * Composes this affine optic with a lens.
     *
     * @param <C> the composed focus type
     * @param other the lens applied to a present focus
     * @return the composed affine optic
     */
    default <C> Affine<S, C> andThen(Lens<A, C> other) {
        return from(PAffine.super.andThen(other));
    }

    /**
     * Composes this affine optic with a prism.
     *
     * @param <C> the composed focus type
     * @param other the prism applied to a present focus
     * @return the composed affine optic
     */
    default <C> Affine<S, C> andThen(Prism<A, C> other) {
        return from(PAffine.super.andThen(other));
    }

    /**
     * Composes this affine optic with a traversal.
     *
     * @param <C> the composed focus type
     * @param other the traversal applied to a present focus
     * @return the composed traversal
     */
    default <C> Traversal<S, C> andThen(Traversal<A, C> other) {
        return Traversal.from(PAffine.super.andThen(other));
    }

    /**
     * Creates an affine optic from serializable preview and rebuild operations.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param preview the partial focus reader
     * @param setter the source rebuild operation
     * @return the resulting affine optic
     */
    static <S, A> Affine<S, A> of(
            AffinePreview<? super S, S, A> preview,
            AffineRebuilder<S, A, S> setter) {
        return from(PAffine.of(preview, setter));
    }

    /**
     * Creates an affine optic from preview and rebuild operations.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param preview the partial focus reader
     * @param setter the source rebuild operation
     * @return the resulting affine optic
     */
    static <S, A> Affine<S, A> of(
            Function<? super S, Either<S, A>> preview,
            BiFunction<S, A, S> setter) {
        return from(PAffine.of(preview, setter));
    }

    /**
     * Returns an affine optic focusing on a map value when a key is present.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key the focused key
     * @return the map-value affine optic
     */
    static <K, V> Affine<Map<K, V>, V> mapValue(K key) {
        return from(PAffine.mapValue(key));
    }

    /**
     * Returns an affine optic focusing on a list element when an index is in range.
     *
     * @param <A> the element type
     * @param index the focused zero-based index
     * @return the list-index affine optic
     */
    static <A> Affine<List<A>, A> listAt(int index) {
        return from(PAffine.listAt(index));
    }

    /**
     * Returns a monomorphic view of a monomorphic polymorphic affine optic.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param affine the affine optic to adapt
     * @return the corresponding monomorphic affine optic
     */
    static <S, A> Affine<S, A> from(PAffine<S, S, A, A> affine) {
        Affine<S, A> direct;
        if (affine instanceof Affine<?, ?> simple) {
            @SuppressWarnings("unchecked")
            Affine<S, A> result = (Affine<S, A>) simple;
            direct = result;
        } else {
            direct = new Affine<>() {
                @Override
                public Either<S, A> preview(S source) {
                    return affine.preview(source);
                }

                @Override
                public S set(A value, S source) {
                    return affine.set(value, source);
                }
            };
        }
        return OpticPrograms.affine(direct, OpticPrograms.programOrOpaque(affine, "affine"));
    }
}
