package com.flechazo.optics;

import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.function.Function;

/**
 * Represents a read-only optic that observes exactly one focus.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
@FunctionalInterface
public interface Getter<S, A> extends Fold<S, A> {
    /**
     * Gets the focus observed from a source.
     *
     * @param source the source to observe
     * @return the observed focus
     */
    A get(S source);

    /**
     * Maps the single observed focus into a monoid value.
     *
     * @param <M> the monoid value type
     * @param monoid the monoid describing accumulation
     * @param f the function applied to the focus
     * @param source the source to observe
     * @return the mapped focus value
     */
    @Override
    default <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
        return f.apply(get(source));
    }

    /**
     * Composes this getter with another getter.
     *
     * @param <B> the composed focus type
     * @param other the getter applied to this getter's focus
     * @return the composed getter
     */
    default <B> Getter<S, B> andThen(Getter<A, B> other) {
        Getter<S, A> self = this;
        Getter<S, B> composed = source -> other.get(self.get(source));
        Getter<S, B> typed = OpticMetadata.fold(
                composed,
                OpticMetadata.<S, A>fold(self)
                        .flatMap(left -> OpticMetadata.<A, B>fold(other)
                                .map((PointFreeFold<A, B> right) -> left.andThen(right))));
        return OpticPrograms.getter(typed, OpticPrograms.compose(self, other));
    }

    /**
     * Composes this getter with a monomorphic lens.
     *
     * @param <B> the composed focus type
     * @param other the lens applied to this getter's focus
     * @return the composed getter
     */
    default <B> Getter<S, B> andThen(PLens<A, A, B, B> other) {
        return andThen(other.asGetter());
    }

    /**
     * Composes this getter with an isomorphism.
     *
     * @param <B> the composed focus type
     * @param other the isomorphism applied to this getter's focus
     * @return the composed getter
     */
    default <B> Getter<S, B> andThen(Iso<A, B> other) {
        Getter<S, B> direct = source -> other.get(get(source));
        return OpticPrograms.getter(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this getter with a monomorphic prism.
     *
     * @param <B> the composed focus type
     * @param other the prism applied to this getter's focus
     * @return a fold over the possibly matching focus
     */
    default <B> Fold<S, B> andThen(PPrism<A, A, B, B> other) {
        return andThen(other.asFold());
    }

    /**
     * Composes this getter with a monomorphic affine optic.
     *
     * @param <B> the composed focus type
     * @param other the affine optic applied to this getter's focus
     * @return a fold over the possibly present focus
     */
    default <B> Fold<S, B> andThen(PAffine<A, A, B, B> other) {
        return andThen(other.asFold());
    }

    /**
     * Composes this getter with a monomorphic traversal.
     *
     * @param <B> the composed focus type
     * @param other the traversal applied to this getter's focus
     * @return a fold over all composed focuses
     */
    default <B> Fold<S, B> andThen(PTraversal<A, A, B, B> other) {
        return andThen(other.asFold());
    }

    /**
     * Composes this getter with a fold.
     *
     * @param <B> the composed focus type
     * @param other the fold applied to this getter's focus
     * @return the composed fold
     */
    default <B> Fold<S, B> andThen(Fold<A, B> other) {
        Getter<S, A> self = this;
        Fold<S, B> composed = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super B, ? extends M> f, S source) {
                return other.foldMap(monoid, f, self.get(source));
            }
        };
        Fold<S, B> typed = OpticMetadata.fold(
                composed,
                OpticMetadata.<S, A>fold(self)
                        .flatMap(left -> OpticMetadata.<A, B>fold(other)
                                .map((PointFreeFold<A, B> right) -> left.andThen(right))));
        return OpticPrograms.fold(typed, OpticPrograms.compose(self, other));
    }

    /**
     * Creates a getter from a function.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param getter the function that observes the focus
     * @return the resulting getter
     */
    static <S, A> Getter<S, A> of(Function<? super S, ? extends A> getter) {
        Getter<S, A> direct = getter::apply;
        return OpticPrograms.getter(direct, OpticPrograms.opaque("getter", null));
    }

    /**
     * Creates a getter from a serializable reader function.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param getter the serializable function that observes the focus
     * @return the resulting getter
     */
    static <S, A> Getter<S, A> of(GetterReader<? super S, ? extends A> getter) {
        Getter<S, A> direct = getter::apply;
        return LambdaLifter.getter(direct, getter);
    }
}
