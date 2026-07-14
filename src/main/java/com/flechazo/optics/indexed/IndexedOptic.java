package com.flechazo.optics.indexed;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.Optic;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a monomorphic optic that supplies an index with every focus transformation.
 *
 * @param <I> the index type
 * @param <S> the source type
 * @param <A> the focus type
 */
public interface IndexedOptic<I, S, A> {
    /**
     * Applies an indexed applicative transformation to every focus.
     *
     * @param <F> the applicative witness type
     * @param f the effectful transformation receiving each index and focus
     * @param source the source to transform
     * @param applicative the applicative used to combine effects and rebuild the source
     * @return the rebuilt source in the applicative context
     */
    <F extends K1> App<F, S> imodifyF(
            BiFunction<I, A, App<F, A>> f, S source, Applicative<F, ?> applicative);

    /**
     * Returns an optic that discards focus indexes.
     *
     * @return the unindexed optic
     */
    default Optic<S, S, A, A> unindexed() {
        IndexedOptic<I, S, A> self = this;
        Optic<S, S, A, A> direct = new Optic<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF((index, value) -> f.apply(value), source, applicative);
            }
        };
        return OpticPrograms.optic(direct, OpticPrograms.programOrOpaque(this, "indexedOptic"));
    }

    /**
     * Composes this indexed optic with another indexed optic and pairs their indexes.
     *
     * @param <J> the nested index type
     * @param <B> the nested focus type
     * @param other the indexed optic applied to every focus
     * @return the composed indexed optic with paired indexes
     */
    default <J, B> IndexedOptic<Tuple2<I, J>, S, B> iandThen(IndexedOptic<J, A, B> other) {
        IndexedOptic<I, S, A> self = this;
        IndexedOptic<Tuple2<I, J>, S, B> direct = new IndexedOptic<>() {
            @Override
            public <F extends K1> App<F, S> imodifyF(
                    BiFunction<Tuple2<I, J>, B, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF(
                        (i, a) -> other.imodifyF((j, b) -> f.apply(Tuple2.of(i, j), b), a, applicative),
                        source,
                        applicative);
            }
        };
        return OpticPrograms.indexedOptic(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this indexed optic with an unindexed optic while retaining the outer index.
     *
     * @param <B> the nested focus type
     * @param other the optic applied to every focus
     * @return the composed indexed optic
     */
    default <B> IndexedOptic<I, S, B> andThen(Optic<A, A, B, B> other) {
        IndexedOptic<I, S, A> self = this;
        IndexedOptic<I, S, B> direct = new IndexedOptic<>() {
            @Override
            public <F extends K1> App<F, S> imodifyF(
                    BiFunction<I, B, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF(
                        (index, value) -> other.modifyF(next -> f.apply(index, next), value, applicative),
                        source,
                        applicative);
            }
        };
        return OpticPrograms.indexedOptic(direct, OpticPrograms.compose(this, other));
    }
}
