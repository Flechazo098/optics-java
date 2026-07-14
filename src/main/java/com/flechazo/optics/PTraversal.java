package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a polymorphic traversal over zero or more focuses.
 *
 * @param <S> the input source type
 * @param <T> the rebuilt source type
 * @param <A> the input focus type
 * @param <B> the replacement focus type
 */
public interface PTraversal<S, T, A, B> extends Optic<S, T, A, B> {
    /**
     * Applies an applicative transformation to every focus in encounter order.
     *
     * @param <F> the applicative witness type
     * @param f the effectful focus transformation
     * @param source the source to transform
     * @param applicative the applicative used to combine effects and rebuild the source
     * @return the rebuilt source in the applicative context
     */
    @Override
    <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative);

    /**
     * Transforms every focus and returns the rebuilt source.
     *
     * @param f the focus transformation
     * @param source the source to transform
     * @return the rebuilt source
     */
    default T modify(Function<? super A, ? extends B> f, S source) {
        App<IdF.Mu, T> result =
                modifyF(value -> new IdF<>(f.apply(value)), source, IdF.applicative());
        return IdF.get(result);
    }

    /**
     * Replaces every focus with a constant value.
     *
     * @param value the replacement focus
     * @param source the source to transform
     * @return the rebuilt source
     */
    default T set(B value, S source) {
        return modify(ignored -> value, source);
    }

    /**
     * Returns all focuses in encounter order.
     *
     * @param source the source to observe
     * @return an unmodifiable list of focuses
     */
    default List<A> getAll(S source) {
        return asFold().getAll(source);
    }

    /**
     * Returns the first focus when present.
     *
     * @param source the source to observe
     * @return the first focus, or an empty value when no focus is present
     */
    default Maybe<A> preview(S source) {
        return asFold().preview(source);
    }

    /**
     * Returns the number of focuses.
     *
     * @param source the source to observe
     * @return the focus count
     */
    default int length(S source) {
        return asFold().length(source);
    }

    /**
     * Determines whether at least one focus satisfies a predicate.
     *
     * @param predicate the condition applied to focuses
     * @param source the source to observe
     * @return {@code true} when at least one focus matches
     */
    default boolean exists(Predicate<? super A> predicate, S source) {
        return asFold().exists(predicate, source);
    }

    /**
     * Determines whether every focus satisfies a predicate.
     *
     * @param predicate the condition applied to focuses
     * @param source the source to observe
     * @return {@code true} when every focus matches, including when no focus is present
     */
    default boolean all(Predicate<? super A> predicate, S source) {
        return asFold().all(predicate, source);
    }

    /**
     * Returns a fold observing the same focuses in the same order.
     *
     * @return the fold view
     */
    default Fold<S, A> asFold() {
        PTraversal<S, T, A, B> self = this;
        Fold<S, A> fold = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                Applicative<Const.Mu<M>, ?> app = Const.applicative(monoid);
                App<Const.Mu<M>, T> folded =
                        self.modifyF(value -> new Const<>(f.apply(value)), source, app);
                return Const.get(folded);
            }
        };
        Fold<S, A> typed = OpticMetadata.fold(
                fold,
                OpticMetadata.<S, T, A, B>optic(self)
                        .map(optic -> PointFreeFold.fromOptic(optic, fold)));
        return OpticPrograms.fold(typed, OpticPrograms.programOrOpaque(self, "traversal"));
    }

    /**
     * Returns a setter updating the same focuses.
     *
     * @return the setter view
     */
    default PSetter<S, T, A, B> asSetter() {
        PTraversal<S, T, A, B> self = this;
        PSetter<S, T, A, B> direct = self::modify;
        return OpticPrograms.setter(direct, OpticPrograms.programOrOpaque(self, "traversal"));
    }

    /**
     * Composes this traversal with another traversal.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the traversal applied to every focus
     * @return the composed traversal
     */
    default <C, D> PTraversal<S, T, C, D> andThen(PTraversal<A, B, C, D> other) {
        PTraversal<S, T, A, B> self = this;
        PTraversal<S, T, C, D> composed = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(value -> other.modifyF(f, value, applicative), source, applicative);
            }
        };
        PTraversal<S, T, C, D> typed = OpticMetadata.optic(
                composed,
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.traversal(typed, OpticPrograms.compose(self, other));
    }

    /**
     * Composes this traversal with an isomorphism.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the isomorphism applied to every focus
     * @return the composed traversal
     */
    default <C, D> PTraversal<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PTraversal<S, T, A, B> self = this;
        PTraversal<S, T, C, D> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(
                        value -> applicative.map(other::reverseGet, f.apply(other.get(value))),
                        source,
                        applicative);
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this traversal with a fold.
     *
     * @param <C> the observed focus type
     * @param fold the fold applied to every focus
     * @return the composed fold
     */
    default <C> Fold<S, C> andThen(Fold<A, C> fold) {
        return asFold().andThen(fold);
    }

    /**
     * Composes this traversal with a lens.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the lens applied to every focus
     * @return the composed traversal
     */
    default <C, D> PTraversal<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        return andThen(other.asTraversal());
    }

    /**
     * Composes this traversal with a prism.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the prism applied to every focus
     * @return the composed traversal
     */
    default <C, D> PTraversal<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        return andThen(other.asTraversal());
    }

    /**
     * Composes this traversal with an affine optic.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the affine optic applied to every focus
     * @return the composed traversal
     */
    default <C, D> PTraversal<S, T, C, D> andThen(PAffine<A, B, C, D> other) {
        return andThen(other.asTraversal());
    }

    /**
     * Selects one of two transformations for every focus.
     *
     * @param predicate the condition applied to each focus
     * @param modifier the transformation used when the condition is true
     * @param otherwise the transformation used when the condition is false
     * @param source the source to transform
     * @return the rebuilt source
     */
    default T modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends B> modifier,
            Function<? super A, ? extends B> otherwise,
            S source) {
        return modify(value -> predicate.test(value) ? modifier.apply(value) : otherwise.apply(value), source);
    }
}
