package com.flechazo.optics.indexed;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.Optic;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface IndexedOptic<I, S, A> {
    <F extends K1> App<F, S> imodifyF(
            BiFunction<I, A, App<F, A>> f, S source, Applicative<F, ?> applicative);

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
