package com.flechazo.optics.indexed;

import com.flechazo.hkt.*;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.Lens;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;

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

    default <F extends K1> App<F, S> imodifyWhenS(
            BiFunction<? super I, ? super A, ? extends App<F, Boolean>> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.imodifyWhen(asIndexedTraversal(), condition, modifier, source, selective);
    }

    default <F extends K1> App<F, S> imodifyUnlessS(
            BiFunction<? super I, ? super A, ? extends App<F, Boolean>> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.imodifyUnless(asIndexedTraversal(), condition, modifier, source, selective);
    }

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

    default IndexedTraversal<I, S, A> asIndexedTraversal() {
        IndexedTraversal<I, S, A> direct = this::imodifyF;
        return OpticPrograms.indexedTraversal(
                direct, OpticPrograms.programOrOpaque(this, "indexedLens"));
    }

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

    default IndexedFold<I, S, A> asIndexedFold() {
        return asIndexedGetter();
    }

    default <J, B> IndexedLens<Tuple2<I, J>, S, B> iandThen(IndexedLens<J, A, B> other) {
        IndexedLens<I, S, A> self = this;
        IndexedLens<Tuple2<I, J>, S, B> direct = IndexedLens.of(
                Tuple2.of(self.index(), other.index()),
                source -> other.get(self.get(source)),
                (source, value) -> self.set(other.set(value, self.get(source)), source));
        return OpticPrograms.indexedLens(direct, OpticPrograms.compose(this, other));
    }

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
