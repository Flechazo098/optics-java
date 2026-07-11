package com.flechazo.optics;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.function.Function;

@FunctionalInterface
public interface Getter<S, A> extends Fold<S, A> {
    A get(S source);

    @Override
    default <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
        return f.apply(get(source));
    }

    default <B> Getter<S, B> andThen(Getter<A, B> other) {
        Getter<S, A> self = this;
        Getter<S, B> composed = new Getter<>() {
            @Override
            public B get(S source) {
                return other.get(self.get(source));
            }
        };
        Getter<S, B> typed = OpticMetadata.fold(
                composed,
                OpticMetadata.<S, A>fold(self)
                        .flatMap(left -> OpticMetadata.<A, B>fold(other).map(left::andThen)));
        return OpticPrograms.getter(typed, OpticPrograms.compose(self, other));
    }

    default <B> Getter<S, B> andThen(PLens<A, A, B, B> other) {
        return andThen(other.asGetter());
    }

    default <B> Getter<S, B> andThen(Iso<A, B> other) {
        Getter<S, B> direct = source -> other.get(get(source));
        return OpticPrograms.getter(direct, OpticPrograms.compose(this, other));
    }

    default <B> Fold<S, B> andThen(PPrism<A, A, B, B> other) {
        return andThen(other.asFold());
    }

    default <B> Fold<S, B> andThen(PAffine<A, A, B, B> other) {
        return andThen(other.asFold());
    }

    default <B> Fold<S, B> andThen(PTraversal<A, A, B, B> other) {
        return andThen(other.asFold());
    }

    default <B> Fold<S, B> andThen(Fold<A, B> other) {
        Getter<S, A> self = this;
        Fold<S, B> composed = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super B, ? extends M> f, S source) {
                return other.foldMap(monoid, f, self.get(source));
            }
        };
        Fold<S, B> typed = OpticMetadata.fold(
                composed,
                OpticMetadata.<S, A>fold(self)
                        .flatMap(left -> OpticMetadata.<A, B>fold(other).map(left::andThen)));
        return OpticPrograms.fold(typed, OpticPrograms.compose(self, other));
    }

    static <S, A> Getter<S, A> of(Function<? super S, ? extends A> getter) {
        Getter<S, A> direct = getter::apply;
        return OpticPrograms.getter(direct, OpticPrograms.opaque("getter", null));
    }

    static <S, A> Getter<S, A> of(GetterReader<? super S, ? extends A> getter) {
        Getter<S, A> direct = getter::apply;
        return LambdaLifter.getter(direct, getter);
    }
}
