package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;

import java.util.function.Function;

/**
 * Represents a monomorphic isomorphism between a source and exactly one focus.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
public interface Iso<S, A> extends PIso<S, S, A, A> {
    /**
     * Gets the focus corresponding to a source.
     *
     * @param source the source to convert
     * @return the corresponding focus
     */
    A get(S source);

    /**
     * Returns the source corresponding to a focus.
     *
     * @param value the focus to convert
     * @return the corresponding source
     */
    S reverseGet(A value);

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
    default <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
        return applicative.map(this::reverseGet, f.apply(get(source)));
    }

    /**
     * Transforms the focus and returns the corresponding source.
     *
     * @param f the focus transformation
     * @param source the source to transform
     * @return the rebuilt source
     */
    default S modify(Function<? super A, ? extends A> f, S source) {
        return reverseGet(f.apply(get(source)));
    }

    /**
     * Applies an effectful modifier when an effectful condition is true.
     *
     * @param <F> the selective witness type
     * @param condition the effectful condition evaluated for the focus
     * @param modifier the effectful transformation selected when the condition is true
     * @param source the source to transform
     * @param selective the selective used to evaluate the condition and modifier
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
     * @param condition the effectful condition evaluated for the focus
     * @param modifier the effectful transformation selected when the condition is false
     * @param source the source to transform
     * @param selective the selective used to evaluate the condition and modifier
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
     * Returns the inverse isomorphism.
     *
     * @return an isomorphism whose forward and reverse conversions are exchanged
     */
    default Iso<A, S> reverse() {
        return Iso.of(this::reverseGet, this::get);
    }

    /**
     * Returns a lens with the same focus and rebuild behavior.
     *
     * @return the lens view
     */
    default Lens<S, A> asLens() {
        Lens<S, A> direct = Lens.of(this::get, (source, value) -> reverseGet(value));
        return OpticPrograms.lens(direct, OpticPrograms.programOrOpaque(this, "iso"));
    }

    /**
     * Returns a traversal containing exactly this isomorphism's focus.
     *
     * @return the traversal view
     */
    default Traversal<S, A> asTraversal() {
        Traversal<S, A> direct = new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f,
                    S source,
                    Applicative<F, ?> applicative) {
                return applicative.map(Iso.this::reverseGet, f.apply(Iso.this.get(source)));
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.programOrOpaque(this, "iso"));
    }

    /**
     * Returns a fold containing exactly this isomorphism's focus.
     *
     * @return the fold view
     */
    default Fold<S, A> asFold() {
        Iso<S, A> self = this;
        Fold<S, A> direct = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return f.apply(self.get(source));
            }
        };
        return OpticPrograms.fold(direct, OpticPrograms.programOrOpaque(this, "iso"));
    }

    /**
     * Composes this isomorphism with another isomorphism.
     *
     * @param <B> the composed focus type
     * @param other the isomorphism applied after this one
     * @return the composed isomorphism
     */
    default <B> Iso<S, B> andThen(Iso<A, B> other) {
        Iso<S, B> direct = Iso.of(source -> other.get(get(source)), value -> reverseGet(other.reverseGet(value)));
        return OpticPrograms.iso(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this isomorphism with a getter.
     *
     * @param <B> the observed value type
     * @param other the getter applied after this isomorphism
     * @return the composed getter
     */
    default <B> Getter<S, B> andThen(Getter<A, B> other) {
        Getter<S, B> direct = source -> other.get(get(source));
        return OpticPrograms.getter(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this isomorphism with a lens.
     *
     * @param <B> the composed focus type
     * @param other the lens applied after this isomorphism
     * @return the composed lens
     */
    default <B> Lens<S, B> andThen(Lens<A, B> other) {
        return asLens().andThen(other);
    }

    /**
     * Composes this isomorphism with a fold.
     *
     * @param <B> the observed focus type
     * @param other the fold applied after this isomorphism
     * @return the composed fold
     */
    default <B> Fold<S, B> andThen(Fold<A, B> other) {
        return asFold().andThen(other);
    }

    /**
     * Composes this isomorphism with a setter.
     *
     * @param <B> the composed focus type
     * @param other the setter applied after this isomorphism
     * @return the composed setter
     */
    default <B> Setter<S, B> andThen(Setter<A, B> other) {
        return asSetter().andThen(other);
    }

    /**
     * Composes this isomorphism with a prism.
     *
     * @param <B> the composed focus type
     * @param other the prism applied after this isomorphism
     * @return the composed prism
     */
    default <B> Prism<S, B> andThen(Prism<A, B> other) {
        Prism<S, B> direct =
                Prism.of(source -> other.match(get(source)).mapLeft(this::reverseGet), value -> reverseGet(other.build(value)));
        return OpticPrograms.prism(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this isomorphism with an affine optic.
     *
     * @param <B> the composed focus type
     * @param other the affine optic applied after this isomorphism
     * @return the composed affine optic
     */
    default <B> Affine<S, B> andThen(Affine<A, B> other) {
        Affine<S, B> direct = Affine.of(
                source -> other.preview(get(source)).mapLeft(this::reverseGet),
                (source, value) -> reverseGet(other.set(value, get(source))));
        return OpticPrograms.affine(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this isomorphism with a traversal.
     *
     * @param <B> the composed focus type
     * @param other the traversal applied after this isomorphism
     * @return the composed traversal
     */
    default <B> Traversal<S, B> andThen(Traversal<A, B> other) {
        Iso<S, A> self = this;
        Traversal<S, B> direct = new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<B, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return applicative.map(self::reverseGet, other.modifyF(f, self.get(source), applicative));
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Returns a setter with the same focus and rebuild behavior.
     *
     * @return the setter view
     */
    default Setter<S, A> asSetter() {
        return asLens().asSetter();
    }

    /**
     * Creates an isomorphism from conversion functions.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param get the forward conversion
     * @param reverseGet the reverse conversion
     * @return the resulting isomorphism
     */
    static <S, A> Iso<S, A> of(Function<? super S, ? extends A> get, Function<? super A, ? extends S> reverseGet) {
        Iso<S, A> direct = new Iso<>() {
            @Override
            public A get(S source) {
                return get.apply(source);
            }

            @Override
            public S reverseGet(A value) {
                return reverseGet.apply(value);
            }
        };
        return OpticPrograms.iso(direct, OpticPrograms.opaque("iso", null));
    }

    /**
     * Creates an isomorphism from serializable conversion functions.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param get the serializable forward conversion
     * @param reverseGet the serializable reverse conversion
     * @return the resulting isomorphism
     */
    static <S, A> Iso<S, A> of(
            IsoGetter<? super S, ? extends A> get,
            IsoRebuilder<? super A, ? extends S> reverseGet) {
        PIso<S, S, A, A> lifted = PIso.of(get, reverseGet);
        Iso<S, A> direct = new Iso<>() {
            @Override
            public A get(S source) {
                return lifted.get(source);
            }

            @Override
            public S reverseGet(A value) {
                return lifted.reverseGet(value);
            }
        };
        return OpticPrograms.iso(direct, OpticPrograms.programOrOpaque(lifted, "iso"));
    }
}
