package com.flechazo.optics.indexed;

import com.flechazo.hkt.*;
import com.flechazo.optics.Lens;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface IndexedLens<I, S, A> extends IndexedOptic<I, S, A> {
    I index();

    A get(S source);

    S set(A value, S source);

    default Tuple2<I, A> iget(S source) {
        return Tuple2.of(index(), get(source));
    }

    default S imodify(BiFunction<? super I, ? super A, ? extends A> f, S source) {
        return set(f.apply(index(), get(source)), source);
    }

    @Override
    default <F extends K1> App<F, S> imodifyF(
            BiFunction<I, A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
        return applicative.map(value -> set(value, source), f.apply(index(), get(source)));
    }

    default <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Functor<F, ?> functor) {
        return functor.map(value -> set(value, source), f.apply(get(source)));
    }

    default Lens<S, S, A, A> asLens() {
        IndexedLens<I, S, A> self = this;
        return new Lens<>() {
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
    }

    default IndexedTraversal<I, S, A> asIndexedTraversal() {
        return this::imodifyF;
    }

    default IndexedFold<I, S, A> asIndexedFold() {
        IndexedLens<I, S, A> self = this;
        return new IndexedFold<>() {
            @Override
            public <M> M ifoldMap(
                    Monoid<M> monoid, BiFunction<? super I, ? super A, ? extends M> f, S source) {
                return f.apply(self.index(), self.get(source));
            }
        };
    }

    default <J, B> IndexedLens<Tuple2<I, J>, S, B> iandThen(IndexedLens<J, A, B> other) {
        IndexedLens<I, S, A> self = this;
        return IndexedLens.of(
                Tuple2.of(self.index(), other.index()),
                source -> other.get(self.get(source)),
                (source, value) -> self.set(other.set(value, self.get(source)), source));
    }

    static <I, S, A> IndexedLens<I, S, A> of(
            I index, Function<? super S, ? extends A> getter, BiFunction<S, A, S> setter) {
        return new IndexedLens<>() {
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
    }
}
