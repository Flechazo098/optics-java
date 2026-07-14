package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Selective;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;

import java.util.function.Function;
import java.util.function.Predicate;

public interface Prism<S, A> extends PPrism<S, S, A, A> {
    @Override
    default Traversal<S, A> asTraversal() {
        return Traversal.from(PPrism.super.asTraversal());
    }

    @Override
    default Setter<S, A> asSetter() {
        return Setter.from(PPrism.super.asSetter());
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

    default S modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends A> modifier,
            S source) {
        return PPrism.super.modifyWhen(predicate, modifier, Function.identity(), source);
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
