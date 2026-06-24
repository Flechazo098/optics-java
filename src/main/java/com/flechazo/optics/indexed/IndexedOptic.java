package com.flechazo.optics.indexed;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Pair;
import com.flechazo.optics.Optic;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface IndexedOptic<I, S, A> {
    <F extends K1> App<F, S> imodifyF(
            BiFunction<I, A, App<F, A>> f, S source, Applicative<F, ?> applicative);

    default Optic<S, S, A, A> unindexed() {
        IndexedOptic<I, S, A> self = this;
        return new Optic<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF((index, value) -> f.apply(value), source, applicative);
            }
        };
    }

    default <J, B> IndexedOptic<Pair<I, J>, S, B> iandThen(IndexedOptic<J, A, B> other) {
        IndexedOptic<I, S, A> self = this;
        return new IndexedOptic<>() {
            @Override
            public <F extends K1> App<F, S> imodifyF(
                    BiFunction<Pair<I, J>, B, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF(
                        (i, a) -> other.imodifyF((j, b) -> f.apply(Pair.of(i, j), b), a, applicative),
                        source,
                        applicative);
            }
        };
    }

    default <B> IndexedOptic<I, S, B> andThen(Optic<A, A, B, B> other) {
        IndexedOptic<I, S, A> self = this;
        return new IndexedOptic<>() {
            @Override
            public <F extends K1> App<F, S> imodifyF(
                    BiFunction<I, B, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.imodifyF(
                        (index, value) -> other.modifyF(next -> f.apply(index, next), value, applicative),
                        source,
                        applicative);
            }
        };
    }
}
