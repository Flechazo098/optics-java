package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Functor;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.function.Function3;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface Lens<S, A> extends PLens<S, S, A, A> {
    @Override
    default Traversal<S, A> asTraversal() {
        return Traversal.from(PLens.super.asTraversal());
    }

    @Override
    default Setter<S, A> asSetter() {
        return Setter.from(PLens.super.asSetter());
    }

    default <F extends K1> App<F, S> modifyWhenS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.modifyWhen(this, condition, modifier, source, selective);
    }

    default <F extends K1> App<F, S> modifyUnlessS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.modifyUnless(this, condition, modifier, source, selective);
    }

    default <C> Lens<S, C> andThen(Lens<A, C> other) {
        return from(PLens.super.andThen(other));
    }

    default <C> Lens<S, C> andThen(Iso<A, C> other) {
        return from(PLens.super.andThen(other));
    }

    default <C> Affine<S, C> andThen(Prism<A, C> other) {
        return Affine.from(PLens.super.andThen(other));
    }

    default <C> Affine<S, C> andThen(Affine<A, C> other) {
        return Affine.from(PLens.super.andThen(other));
    }

    default <C> Traversal<S, C> andThen(Traversal<A, C> other) {
        return Traversal.from(PLens.super.andThen(other));
    }

    static <S, A> Lens<S, A> of(
            LensGetter<? super S, ? extends A> getter,
            LensRebuilder<S, A, S> setter) {
        return from(PLens.of(getter, setter));
    }

    static <S, A> Lens<S, A> of(
            Function<? super S, ? extends A> getter,
            BiFunction<S, A, S> setter) {
        return from(PLens.of(getter, setter));
    }

    static <S, A> Lens<S, A> opaque(
            Function<? super S, ? extends A> getter,
            BiFunction<S, A, S> setter) {
        return from(PLens.opaque(getter, setter));
    }

    static <S, A> Lens<S, A> of(Class<S> recordType, LensGetter<S, A> getter) {
        return from(RecordOptics.recordLens(recordType, getter));
    }

    static <S, A, B> Lens<S, Tuple2<A, B>> paired(
            Lens<S, A> first, Lens<S, B> second, Function3<S, A, B, S> rebuild) {
        return from(PLens.paired(first, second, rebuild));
    }

    static <S, A, B> Lens<S, Tuple2<A, B>> paired(
            Lens<S, A> first, Lens<S, B> second, BiFunction<A, B, S> constructor) {
        return from(PLens.paired(first, second, constructor));
    }

    static <S, A> Lens<S, A> from(PLens<S, S, A, A> lens) {
        Lens<S, A> direct;
        if (lens instanceof Lens<?, ?> simple) {
            @SuppressWarnings("unchecked")
            Lens<S, A> result = (Lens<S, A>) simple;
            direct = result;
        } else {
            direct = new Lens<>() {
                @Override
                public A get(S source) {
                    return lens.get(source);
                }

                @Override
                public S set(A value, S source) {
                    return lens.set(value, source);
                }

                @Override
                public <F extends K1> App<F, S> modifyF(
                        Function<A, App<F, A>> f, S source, Functor<F, ?> functor) {
                    return lens.modifyF(f, source, functor);
                }
            };
        }
        return OpticPrograms.lens(direct, OpticPrograms.programOrOpaque(lens, "lens"));
    }
}
