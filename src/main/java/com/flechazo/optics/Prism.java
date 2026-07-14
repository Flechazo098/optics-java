package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Selective;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a monomorphic prism that may match one focus and can build a matching source.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
public interface Prism<S, A> extends PPrism<S, S, A, A> {
    /**
     * Returns a traversal containing zero or one focus.
     *
     * @return the traversal view
     */
    @Override
    default Traversal<S, A> asTraversal() {
        return Traversal.from(PPrism.super.asTraversal());
    }

    /**
     * Returns a setter with this prism's conditional update behavior.
     *
     * @return the setter view
     */
    @Override
    default Setter<S, A> asSetter() {
        return Setter.from(PPrism.super.asSetter());
    }

    /**
     * Applies an effectful modifier when an effectful condition is true.
     *
     * @param <F> the selective witness type
     * @param condition the effectful condition evaluated for a matched focus
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
     * @param condition the effectful condition evaluated for a matched focus
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
     * Applies a modifier only when a predicate accepts the matched focus.
     *
     * @param predicate the condition applied to a matched focus
     * @param modifier the transformation applied when the condition is true
     * @param source the source to transform
     * @return the rebuilt source
     */
    default S modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends A> modifier,
            S source) {
        return PPrism.super.modifyWhen(predicate, modifier, Function.identity(), source);
    }

    /**
     * Composes this prism with another prism.
     *
     * @param <C> the composed focus type
     * @param other the prism applied to a matched focus
     * @return the composed prism
     */
    default <C> Prism<S, C> andThen(Prism<A, C> other) {
        return from(PPrism.super.andThen(other));
    }

    /**
     * Composes this prism with an isomorphism.
     *
     * @param <C> the composed focus type
     * @param other the isomorphism applied to a matched focus
     * @return the composed prism
     */
    default <C> Prism<S, C> andThen(Iso<A, C> other) {
        return from(PPrism.super.andThen(other));
    }

    /**
     * Composes this prism with a lens and returns an affine optic.
     *
     * @param <C> the composed focus type
     * @param other the lens applied to a matched focus
     * @return the composed affine optic
     */
    default <C> Affine<S, C> andThen(Lens<A, C> other) {
        return Affine.from(PPrism.super.andThen(other));
    }

    /**
     * Composes this prism with an affine optic.
     *
     * @param <C> the composed focus type
     * @param other the affine optic applied to a matched focus
     * @return the composed affine optic
     */
    default <C> Affine<S, C> andThen(Affine<A, C> other) {
        return Affine.from(PPrism.super.andThen(other));
    }

    /**
     * Composes this prism with a traversal.
     *
     * @param <C> the composed focus type
     * @param other the traversal applied to a matched focus
     * @return the composed traversal
     */
    default <C> Traversal<S, C> andThen(Traversal<A, C> other) {
        return Traversal.from(PPrism.super.andThen(other));
    }

    /**
     * Creates a prism from serializable match and build operations.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param match the source match operation
     * @param build the matching source builder
     * @return the resulting prism
     */
    static <S, A> Prism<S, A> of(
            PrismMatcher<? super S, S, A> match,
            PrismBuilder<? super A, ? extends S> build) {
        return from(PPrism.of(match, build));
    }

    /**
     * Creates a prism from match and build operations.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param match the source match operation
     * @param build the matching source builder
     * @return the resulting prism
     */
    static <S, A> Prism<S, A> of(
            Function<? super S, Either<S, A>> match,
            Function<? super A, ? extends S> build) {
        return from(PPrism.of(match, build));
    }

    /**
     * Creates a prism that matches instances of a subtype.
     *
     * @param <S> the base type
     * @param <A> the subtype
     * @param baseType the base class
     * @param subtype the class matched by the prism
     * @return the subtype prism
     */
    static <S, A extends S> Prism<S, A> subtype(Class<S> baseType, Class<A> subtype) {
        return from(RecordOptics.subtypePrism(baseType, subtype));
    }

    /**
     * Returns a monomorphic view of a monomorphic polymorphic prism.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param prism the prism to adapt
     * @return the corresponding monomorphic prism
     */
    static <S, A> Prism<S, A> from(PPrism<S, S, A, A> prism) {
        Prism<S, A> direct;
        if (prism instanceof Prism<?, ?> simple) {
            @SuppressWarnings("unchecked")
            Prism<S, A> result = (Prism<S, A>) simple;
            direct = result;
        } else {
            direct = new Prism<>() {
                @Override
                public Either<S, A> match(S source) {
                    return prism.match(source);
                }

                @Override
                public S build(A value) {
                    return prism.build(value);
                }
            };
        }
        Prism<S, A> typed = OpticMetadata.optic(direct, OpticMetadata.optic(prism));
        return OpticPrograms.prism(typed, OpticPrograms.programOrOpaque(prism, "prism"));
    }
}
