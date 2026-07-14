package com.flechazo.optics.indexed;

import com.flechazo.hkt.*;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.Lens;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a lens that associates its single focus with a fixed index.
 *
 * @param <I> the index type
 * @param <S> the source type
 * @param <A> the focus type
 */
public interface IndexedLens<I, S, A> extends IndexedOptic<I, S, A> {
    /**
     * Returns the index associated with the focus.
     *
     * @return the focus index
     */
    I index();

    /**
     * Gets the focus from a source.
     *
     * @param source the source to read
     * @return the focus
     */
    A get(S source);

    /**
     * Replaces the focus and returns the rebuilt source.
     *
     * @param value the replacement focus
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    S set(A value, S source);

    /**
     * Returns the index and focus as a tuple.
     *
     * @param source the source to read
     * @return a tuple containing the fixed index and focus
     */
    default Tuple2<I, A> iget(S source) {
        return Tuple2.of(index(), get(source));
    }

    /**
     * Transforms the focus using its index.
     *
     * @param f the indexed focus transformation
     * @param source the source to transform
     * @return the rebuilt source
     */
    default S imodify(BiFunction<? super I, ? super A, ? extends A> f, S source) {
        return set(f.apply(index(), get(source)), source);
    }

    /**
     * Applies an indexed applicative transformation to the focus.
     *
     * @param <F> the applicative witness type
     * @param f the effectful indexed transformation
     * @param source the source to transform
     * @param applicative the applicative used to rebuild the source
     * @return the rebuilt source in the applicative context
     */
    @Override
    default <F extends K1> App<F, S> imodifyF(
            BiFunction<I, A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
        return applicative.map(value -> set(value, source), f.apply(index(), get(source)));
    }

    /**
     * Applies a functorial transformation to the focus without exposing its index.
     *
     * @param <F> the functor witness type
     * @param f the effectful focus transformation
     * @param source the source to transform
     * @param functor the functor used to rebuild the source
     * @return the rebuilt source in the functor context
     */
    default <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Functor<F, ?> functor) {
        return functor.map(value -> set(value, source), f.apply(get(source)));
    }

    /**
     * Applies an indexed effectful modifier when an indexed effectful condition is true.
     *
     * @param <F> the selective witness type
     * @param condition the indexed effectful condition
     * @param modifier the indexed effectful transformation
     * @param source the source to transform
     * @param selective the selective used to evaluate and combine effects
     * @return the rebuilt source in the selective context
     */
    default <F extends K1> App<F, S> imodifyWhenS(
            BiFunction<? super I, ? super A, ? extends App<F, Boolean>> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.imodifyWhen(asIndexedTraversal(), condition, modifier, source, selective);
    }

    /**
     * Applies an indexed effectful modifier when an indexed effectful condition is false.
     *
     * @param <F> the selective witness type
     * @param condition the indexed effectful condition
     * @param modifier the indexed effectful transformation
     * @param source the source to transform
     * @param selective the selective used to evaluate and combine effects
     * @return the rebuilt source in the selective context
     */
    default <F extends K1> App<F, S> imodifyUnlessS(
            BiFunction<? super I, ? super A, ? extends App<F, Boolean>> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.imodifyUnless(asIndexedTraversal(), condition, modifier, source, selective);
    }

    /**
     * Returns a lens that discards the focus index.
     *
     * @return the unindexed lens
     */
    default Lens<S, A> asLens() {
        IndexedLens<I, S, A> self = this;
        Lens<S, A> direct = new Lens<>() {
            @Override
            public A get(S source) {
                return self.get(source);
            }

            @Override
            public S set(A value, S source) {
                return self.set(value, source);
            }

            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f, S source, Functor<F, ?> functor) {
                return self.modifyF(f, source, functor);
            }
        };
        return OpticPrograms.lens(direct, OpticPrograms.programOrOpaque(this, "indexedLens"));
    }

    /**
     * Returns an indexed traversal containing exactly this lens's focus.
     *
     * @return the indexed traversal view
     */
    default IndexedTraversal<I, S, A> asIndexedTraversal() {
        IndexedTraversal<I, S, A> direct = this::imodifyF;
        return OpticPrograms.indexedTraversal(
                direct, OpticPrograms.programOrOpaque(this, "indexedLens"));
    }

    /**
     * Returns an indexed getter observing this lens's focus and index.
     *
     * @return the indexed getter view
     */
    default IndexedGetter<I, S, A> asIndexedGetter() {
        IndexedLens<I, S, A> self = this;
        IndexedGetter<I, S, A> direct = new IndexedGetter<>() {
            @Override
            public A get(S source) {
                return self.get(source);
            }

            @Override
            public I index() {
                return self.index();
            }
        };
        return OpticPrograms.indexedGetter(
                direct, OpticPrograms.programOrOpaque(this, "indexedLens"));
    }

    /**
     * Returns an indexed fold containing exactly this lens's focus.
     *
     * @return the indexed fold view
     */
    default IndexedFold<I, S, A> asIndexedFold() {
        return asIndexedGetter();
    }

    /**
     * Composes this indexed lens with another indexed lens and pairs their indexes.
     *
     * @param <J> the nested index type
     * @param <B> the nested focus type
     * @param other the indexed lens applied to this lens's focus
     * @return the composed indexed lens
     */
    default <J, B> IndexedLens<Tuple2<I, J>, S, B> iandThen(IndexedLens<J, A, B> other) {
        IndexedLens<I, S, A> self = this;
        IndexedLens<Tuple2<I, J>, S, B> direct = IndexedLens.of(
                Tuple2.of(self.index(), other.index()),
                source -> other.get(self.get(source)),
                (source, value) -> self.set(other.set(value, self.get(source)), source));
        return OpticPrograms.indexedLens(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Creates an indexed lens from a fixed index and read and rebuild operations.
     *
     * @param <I> the index type
     * @param <S> the source type
     * @param <A> the focus type
     * @param index the fixed focus index
     * @param getter the focus reader
     * @param setter the source rebuilder
     * @return the resulting indexed lens
     */
    static <I, S, A> IndexedLens<I, S, A> of(
            I index, Function<? super S, ? extends A> getter, BiFunction<S, A, S> setter) {
        IndexedLens<I, S, A> direct = new IndexedLens<>() {
            @Override
            public I index() {
                return index;
            }

            @Override
            public A get(S source) {
                return getter.apply(source);
            }

            @Override
            public S set(A value, S source) {
                return setter.apply(source, value);
            }
        };
        return OpticPrograms.indexedLens(direct, OpticPrograms.structured("indexedLens", index));
    }
}
