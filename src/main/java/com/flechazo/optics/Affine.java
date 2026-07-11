package com.flechazo.optics;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Affine<S, A> extends PAffine<S, S, A, A> {
    @Override
    default Traversal<S, A> asTraversal() {
        return Traversal.from(PAffine.super.asTraversal());
    }

    @Override
    default Setter<S, A> asSetter() {
        return Setter.from(PAffine.super.asSetter());
    }

    default <C> Affine<S, C> andThen(Affine<A, C> other) {
        return from(PAffine.super.andThen(other));
    }

    default <C> Affine<S, C> andThen(Iso<A, C> other) {
        return from(PAffine.super.andThen(other));
    }

    default <C> Affine<S, C> andThen(Lens<A, C> other) {
        return from(PAffine.super.andThen(other));
    }

    default <C> Affine<S, C> andThen(Prism<A, C> other) {
        return from(PAffine.super.andThen(other));
    }

    default <C> Traversal<S, C> andThen(Traversal<A, C> other) {
        return Traversal.from(PAffine.super.andThen(other));
    }

    static <S, A> Affine<S, A> of(
            AffinePreview<? super S, S, A> preview,
            AffineRebuilder<S, A, S> setter) {
        return from(PAffine.of(preview, setter));
    }

    static <S, A> Affine<S, A> of(
            Function<? super S, Either<S, A>> preview,
            BiFunction<S, A, S> setter) {
        return from(PAffine.of(preview, setter));
    }

    static <K, V> Affine<Map<K, V>, V> mapValue(K key) {
        return from(PAffine.mapValue(key));
    }

    static <A> Affine<List<A>, A> listAt(int index) {
        return from(PAffine.listAt(index));
    }

    static <S, A> Affine<S, A> from(PAffine<S, S, A, A> affine) {
        Affine<S, A> direct;
        if (affine instanceof Affine<?, ?> simple) {
            @SuppressWarnings("unchecked")
            Affine<S, A> result = (Affine<S, A>) simple;
            direct = result;
        } else {
            direct = new Affine<>() {
                @Override
                public Either<S, A> preview(S source) {
                    return affine.preview(source);
                }

                @Override
                public S set(A value, S source) {
                    return affine.set(value, source);
                }
            };
        }
        return OpticPrograms.affine(direct, OpticPrograms.programOrOpaque(affine, "affine"));
    }
}
