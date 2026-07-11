package com.flechazo.optics;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.function.Function;

public interface Prism<S, A> extends PPrism<S, S, A, A> {
    @Override
    default Traversal<S, A> asTraversal() {
        return Traversal.from(PPrism.super.asTraversal());
    }

    @Override
    default Setter<S, A> asSetter() {
        return Setter.from(PPrism.super.asSetter());
    }

    default <C> Prism<S, C> andThen(Prism<A, C> other) {
        return from(PPrism.super.andThen(other));
    }

    default <C> Prism<S, C> andThen(Iso<A, C> other) {
        return from(PPrism.super.andThen(other));
    }

    default <C> Affine<S, C> andThen(Lens<A, C> other) {
        return Affine.from(PPrism.super.andThen(other));
    }

    default <C> Affine<S, C> andThen(Affine<A, C> other) {
        return Affine.from(PPrism.super.andThen(other));
    }

    default <C> Traversal<S, C> andThen(Traversal<A, C> other) {
        return Traversal.from(PPrism.super.andThen(other));
    }

    static <S, A> Prism<S, A> of(
            PrismMatcher<? super S, S, A> match,
            PrismBuilder<? super A, ? extends S> build) {
        return from(PPrism.of(match, build));
    }

    static <S, A> Prism<S, A> of(
            Function<? super S, Either<S, A>> match,
            Function<? super A, ? extends S> build) {
        return from(PPrism.of(match, build));
    }

    static <S, A extends S> Prism<S, A> subtype(Class<S> baseType, Class<A> subtype) {
        return from(RecordOptics.subtypePrism(baseType, subtype));
    }

    static <S, A> Prism<S, A> from(PPrism<S, S, A, A> prism) {
        Prism<S, A> direct;
        if (prism instanceof Prism<?, ?> simple) {
            @SuppressWarnings("unchecked")
            Prism<S, A> result = (Prism<S, A>) simple;
            direct = result;
        } else {
            direct = new Prism<>() {
                @Override
                public Either<S, A> match(S source) {
                    return prism.match(source);
                }

                @Override
                public S build(A value) {
                    return prism.build(value);
                }
            };
        }
        Prism<S, A> typed = OpticMetadata.optic(direct, OpticMetadata.optic(prism));
        return OpticPrograms.prism(typed, OpticPrograms.programOrOpaque(prism, "prism"));
    }
}
