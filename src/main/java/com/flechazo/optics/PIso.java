package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Functor;
import com.flechazo.hkt.K1;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a polymorphic isomorphism between a source and exactly one focus.
 *
 * @param <S> the input source type
 * @param <T> the rebuilt source type
 * @param <A> the input focus type
 * @param <B> the replacement focus type
 */
public interface PIso<S, T, A, B> extends Optic<S, T, A, B> {
    /**
     * Gets the focus corresponding to a source.
     *
     * @param source the source to convert
     * @return the corresponding focus
     */
    A get(S source);

    /**
     * Returns the source corresponding to a replacement focus.
     *
     * @param value the replacement focus
     * @return the corresponding rebuilt source
     */
    T reverseGet(B value);

    /**
     * Transforms the focus and returns the corresponding rebuilt source.
     *
     * @param f the focus transformation
     * @param source the source to transform
     * @return the rebuilt source
     */
    default T modify(Function<? super A, ? extends B> f, S source) {
        return reverseGet(f.apply(get(source)));
    }

    /**
     * Applies an applicative focus transformation and rebuilds the source in that context.
     *
     * @param <F> the applicative witness type
     * @param f the effectful focus transformation
     * @param source the source to transform
     * @param applicative the applicative used to map the rebuilt source
     * @return the rebuilt source in the applicative context
     */
    @Override
    default <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, com.flechazo.hkt.Applicative<F, ?> applicative) {
        return applicative.map(this::reverseGet, f.apply(get(source)));
    }

    /**
     * Applies a functorial focus transformation and rebuilds the source in that context.
     *
     * @param <F> the functor witness type
     * @param f the effectful focus transformation
     * @param source the source to transform
     * @param functor the functor used to map the rebuilt source
     * @return the rebuilt source in the functor context
     */
    default <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Functor<F, ?> functor) {
        return functor.map(this::reverseGet, f.apply(get(source)));
    }

    /**
     * Composes this isomorphism with another isomorphism.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the isomorphism applied after this one
     * @return the composed isomorphism
     */
    default <C, D> PIso<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PIso<S, T, C, D> direct = PIso.of(
                source -> other.get(get(source)),
                value -> reverseGet(other.reverseGet(value)));
        return OpticPrograms.iso(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this isomorphism with a lens.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the lens applied after this isomorphism
     * @return the composed lens
     */
    default <C, D> PLens<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        PIso<S, T, A, B> self = this;
        PLens<S, T, C, D> direct = PLens.of(
                source -> other.get(self.get(source)),
                (source, value) -> self.reverseGet(other.set(value, self.get(source))));
        return OpticPrograms.lens(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this isomorphism with a prism.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the prism applied after this isomorphism
     * @return the composed prism
     */
    default <C, D> PPrism<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        PPrism<S, T, C, D> direct = PPrism.of(
                source -> other.match(get(source)).mapLeft(this::reverseGet),
                value -> reverseGet(other.build(value)));
        return OpticPrograms.prism(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this isomorphism with an affine optic.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the affine optic applied after this isomorphism
     * @return the composed affine optic
     */
    default <C, D> PAffine<S, T, C, D> andThen(PAffine<A, B, C, D> other) {
        PAffine<S, T, C, D> direct = PAffine.of(
                source -> other.preview(get(source)).mapLeft(this::reverseGet),
                (source, value) -> reverseGet(other.set(value, get(source))));
        return OpticPrograms.affine(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this isomorphism with a traversal.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the traversal applied after this isomorphism
     * @return the composed traversal
     */
    default <C, D> PTraversal<S, T, C, D> andThen(PTraversal<A, B, C, D> other) {
        PIso<S, T, A, B> self = this;
        PTraversal<S, T, C, D> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, com.flechazo.hkt.Applicative<F, ?> applicative) {
                return applicative.map(self::reverseGet, other.modifyF(f, self.get(source), applicative));
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this isomorphism with a setter.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the setter applied after this isomorphism
     * @return the composed setter
     */
    default <C, D> PSetter<S, T, C, D> andThen(PSetter<A, B, C, D> other) {
        PSetter<S, T, C, D> direct =
                (f, source) -> reverseGet(other.modify(f, get(source)));
        return OpticPrograms.setter(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this isomorphism with a getter.
     *
     * @param <C> the observed value type
     * @param other the getter applied after this isomorphism
     * @return the composed getter
     */
    default <C> Getter<S, C> andThen(Getter<A, C> other) {
        Getter<S, C> direct = source -> other.get(get(source));
        return OpticPrograms.getter(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this isomorphism with a fold.
     *
     * @param <C> the observed focus type
     * @param other the fold applied after this isomorphism
     * @return the composed fold
     */
    default <C> Fold<S, C> andThen(Fold<A, C> other) {
        PIso<S, T, A, B> self = this;
        Fold<S, C> direct = new Fold<>() {
            @Override
            public <M> M foldMap(
                    com.flechazo.hkt.Monoid<M> monoid,
                    Function<? super C, ? extends M> f,
                    S source) {
                return other.foldMap(monoid, f, self.get(source));
            }
        };
        return OpticPrograms.fold(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Creates an isomorphism from serializable conversion functions.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param get the forward conversion
     * @param reverseGet the reverse conversion
     * @return the resulting isomorphism
     */
    static <S, T, A, B> PIso<S, T, A, B> of(
            IsoGetter<? super S, ? extends A> get,
            IsoRebuilder<? super B, ? extends T> reverseGet) {
        PIso<S, T, A, B> direct = of(
                (Function<? super S, ? extends A>) get,
                (Function<? super B, ? extends T>) reverseGet);
        return LambdaLifter.iso(direct, get, reverseGet);
    }

    /**
     * Creates an isomorphism from conversion functions.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param get the forward conversion
     * @param reverseGet the reverse conversion
     * @return the resulting isomorphism
     */
    static <S, T, A, B> PIso<S, T, A, B> of(
            Function<? super S, ? extends A> get,
            Function<? super B, ? extends T> reverseGet) {
        Objects.requireNonNull(get, "get");
        Objects.requireNonNull(reverseGet, "reverseGet");
        PIso<S, T, A, B> direct = new PIso<>() {
            @Override
            public A get(S source) {
                return get.apply(source);
            }

            @Override
            public T reverseGet(B value) {
                return reverseGet.apply(value);
            }
        };
        return OpticPrograms.iso(direct, OpticPrograms.opaque("iso", null));
    }
}
