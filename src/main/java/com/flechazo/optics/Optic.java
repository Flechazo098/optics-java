package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Selective;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;

import java.util.function.Function;

/**
 * Represents a polymorphic optic that transforms focuses through an applicative effect.
 *
 * @param <S> the input source type
 * @param <T> the rebuilt source type
 * @param <A> the input focus type
 * @param <B> the replacement focus type
 */
public interface Optic<S, T, A, B> {
    /**
     * Applies an effectful transformation to every focus and rebuilds the source in the same
     * applicative context.
     *
     * @param <F> the applicative witness type
     * @param f the effectful focus transformation
     * @param source the source to transform
     * @param applicative the applicative used to combine focus effects
     * @return the rebuilt source in the applicative context
     */
    <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative);

    /**
     * Selects one of two effectful focus transformations for each focus.
     *
     * @param <F> the selective witness type
     * @param condition the effectful condition evaluated for each focus
     * @param thenModifier the transformation selected when the condition is true
     * @param elseModifier the transformation selected when the condition is false
     * @param source the source to transform
     * @param selective the selective used to evaluate and combine effects
     * @return the rebuilt source in the selective context
     */
    default <F extends K1> App<F, T> modifyBranchS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, B>> thenModifier,
            Function<? super A, ? extends App<F, B>> elseModifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.modifyBranch(
                this,
                condition,
                thenModifier,
                elseModifier,
                source,
                selective);
    }

    /**
     * Applies an effectful modifier when an effectful condition is true and otherwise applies the
     * supplied fallback transformation.
     *
     * @param <F> the selective witness type
     * @param condition the effectful condition evaluated for each focus
     * @param modifier the transformation selected when the condition is true
     * @param otherwise the transformation selected when the condition is false
     * @param source the source to transform
     * @param selective the selective used to evaluate and combine effects
     * @return the rebuilt source in the selective context
     */
    default <F extends K1> App<F, T> modifyWhenS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, B>> modifier,
            Function<? super A, ? extends App<F, B>> otherwise,
            S source,
            Selective<F, ?> selective) {
        return modifyBranchS(condition, modifier, otherwise, source, selective);
    }

    /**
     * Applies an effectful modifier when an effectful condition is false and otherwise applies the
     * supplied fallback transformation.
     *
     * @param <F> the selective witness type
     * @param condition the effectful condition evaluated for each focus
     * @param modifier the transformation selected when the condition is false
     * @param otherwise the transformation selected when the condition is true
     * @param source the source to transform
     * @param selective the selective used to evaluate and combine effects
     * @return the rebuilt source in the selective context
     */
    default <F extends K1> App<F, T> modifyUnlessS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, B>> modifier,
            Function<? super A, ? extends App<F, B>> otherwise,
            S source,
            Selective<F, ?> selective) {
        return modifyBranchS(condition, otherwise, modifier, source, selective);
    }

    /**
     * Composes this optic with an optic whose source is this optic's focus.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the optic applied after this optic
     * @return an optic that applies this optic followed by {@code other}
     */
    default <C, D> Optic<S, T, C, D> andThen(Optic<A, B, C, D> other) {
        Optic<S, T, A, B> self = this;
        Optic<S, T, C, D> composed = new Optic<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(a -> other.modifyF(f, a, applicative), source, applicative);
            }
        };
        Optic<S, T, C, D> typed = OpticMetadata.optic(
                composed,
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.optic(typed, OpticPrograms.compose(self, other));
    }

    /**
     * Maps rebuilt sources produced by this optic.
     *
     * @param <U> the mapped rebuilt source type
     * @param f the function applied to each rebuilt source
     * @return an optic with the mapped rebuilt source type
     */
    default <U> Optic<S, U, A, B> map(Function<? super T, ? extends U> f) {
        Optic<S, T, A, B> self = this;
        Optic<S, U, A, B> direct = new Optic<>() {
            @Override
            public <F extends K1> App<F, U> modifyF(
                    Function<A, App<F, B>> g, S source, Applicative<F, ?> applicative) {
                return applicative.map(f, self.modifyF(g, source, applicative));
            }
        };
        return OpticPrograms.optic(direct, OpticPrograms.opaque("mappedOptic", null));
    }

    /**
     * Adapts an input value before this optic processes it.
     *
     * @param <R> the adapted input source type
     * @param f the function that converts an adapted source to this optic's source type
     * @return an optic accepting the adapted source type
     */
    default <R> Optic<R, T, A, B> contramap(Function<? super R, ? extends S> f) {
        Optic<S, T, A, B> self = this;
        Optic<R, T, A, B> direct = new Optic<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> g, R source, Applicative<F, ?> applicative) {
                return self.modifyF(g, f.apply(source), applicative);
            }
        };
        return OpticPrograms.optic(direct, OpticPrograms.opaque("contramappedOptic", null));
    }

    /**
     * Adapts both the input source and rebuilt source of this optic.
     *
     * @param <R> the adapted input source type
     * @param <U> the adapted rebuilt source type
     * @param before the function applied before this optic
     * @param after the function applied to the rebuilt source
     * @return an optic with adapted source types
     */
    default <R, U> Optic<R, U, A, B> dimap(
            Function<? super R, ? extends S> before, Function<? super T, ? extends U> after) {
        Optic<S, T, A, B> self = this;
        Optic<R, U, A, B> direct = new Optic<>() {
            @Override
            public <F extends K1> App<F, U> modifyF(
                    Function<A, App<F, B>> f, R source, Applicative<F, ?> applicative) {
                return applicative.map(after, self.modifyF(f, before.apply(source), applicative));
            }
        };
        return OpticPrograms.optic(direct, OpticPrograms.opaque("dimappedOptic", null));
    }
}
