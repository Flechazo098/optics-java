package com.flechazo.hkt;

import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.util.validation.Validation;
import com.google.common.reflect.TypeToken;

import java.util.Objects;
import java.util.function.Function;

public record FunctionArrow<A, B>(Function<? super A, ? extends B> function)
        implements Function<A, B>, App2<FunctionArrow.Mu, A, B>, App<FunctionArrow.ReaderMu<A>, B> {
    public static final class Mu implements K2 {
        private Mu() {
        }
    }

    public static final class ReaderMu<A> implements K1 {
        private ReaderMu() {
        }
    }

    public FunctionArrow {
        Objects.requireNonNull(function, "function");
    }

    public static <A, B> FunctionArrow<A, B> of(Function<? super A, ? extends B> function) {
        return new FunctionArrow<>(function);
    }

    public static <A, B> FunctionArrow<A, B> unbox(App2<Mu, A, B> value) {
        return (FunctionArrow<A, B>) Validation.kind().narrowWithTypeCheck2(value, FunctionArrow.class);
    }

    public static <A, B> FunctionArrow<A, B> unbox(App<ReaderMu<A>, B> value) {
        return (FunctionArrow<A, B>) Validation.kind().narrowWithTypeCheck(value, FunctionArrow.class);
    }

    @Override
    public B apply(A value) {
        return function.apply(value);
    }

    public static FunctionArrowInstance instance() {
        return FunctionArrowInstance.INSTANCE;
    }

    public enum FunctionArrowInstance
            implements AffineP<FunctionArrow.Mu, FunctionArrowInstance.Mu>,
            Strong<FunctionArrow.Mu, FunctionArrowInstance.Mu>,
            Choice<FunctionArrow.Mu, FunctionArrowInstance.Mu>,
            Closed<FunctionArrow.Mu, FunctionArrowInstance.Mu>,
            Monoidal<FunctionArrow.Mu, FunctionArrowInstance.Mu>,
            MonoidProfunctor<FunctionArrow.Mu, FunctionArrowInstance.Mu>,
            Mapping<FunctionArrow.Mu, FunctionArrowInstance.Mu> {
        INSTANCE;

        public static final class Mu implements AffineP.Mu, Strong.Mu, Choice.Mu, Closed.Mu, Traversing.Mu,
                Mapping.Mu, Monoidal.Mu, MonoidProfunctor.Mu {
            public static final TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
            };

            private Mu() {
            }
        }

        @Override
        public <A, B, C, D> FunctionArrow<App2<FunctionArrow.Mu, A, B>, App2<FunctionArrow.Mu, C, D>> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right) {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            return FunctionArrow.of(value -> {
                FunctionArrow<A, B> arrow = unbox(value);
                return FunctionArrow.of(input -> right.apply(arrow.apply(left.apply(input))));
            });
        }

        @Override
        public <A, B, C> App2<FunctionArrow.Mu, Tuple2<A, C>, Tuple2<B, C>> first(
                App2<FunctionArrow.Mu, A, B> value) {
            FunctionArrow<A, B> arrow = unbox(value);
            return FunctionArrow.of(tuple -> Tuple2.of(arrow.apply(tuple.first()), tuple.second()));
        }

        @Override
        public <A, B, C> App2<FunctionArrow.Mu, Either<A, C>, Either<B, C>> left(
                App2<FunctionArrow.Mu, A, B> value) {
            FunctionArrow<A, B> arrow = unbox(value);
            return FunctionArrow.of(either -> either.isLeft()
                    ? Either.left(arrow.apply(either.left()))
                    : Either.right(either.right()));
        }

        @Override
        public <A, B, X> App2<FunctionArrow.Mu, Function<X, A>, Function<X, B>> closed(
                App2<FunctionArrow.Mu, A, B> value) {
            FunctionArrow<A, B> arrow = unbox(value);
            return FunctionArrow.of(function -> input -> arrow.apply(function.apply(input)));
        }

        @Override
        public <A, B, C, D> App2<FunctionArrow.Mu, Tuple2<A, C>, Tuple2<B, D>> par(
                App2<FunctionArrow.Mu, A, B> first,
                java.util.function.Supplier<App2<FunctionArrow.Mu, C, D>> second) {
            FunctionArrow<A, B> leftArrow = unbox(first);
            return FunctionArrow.of(tuple -> Tuple2.of(
                    leftArrow.apply(tuple.first()),
                    FunctionArrow.unbox(second.get()).apply(tuple.second())));
        }

        @Override
        public App2<FunctionArrow.Mu, Unit, Unit> empty() {
            return FunctionArrow.of(unit -> Unit.INSTANCE);
        }

        @Override
        public <S, T, A, B> App2<FunctionArrow.Mu, S, T> wander(
                Wander<S, T, A, B> wander,
                App2<FunctionArrow.Mu, A, B> input) {
            FunctionArrow<A, B> arrow = unbox(input);
            FunctionArrow<S, App<IdF.Mu, T>> traversed =
                    wander.wander(IdF.applicative(), FunctionArrow.of(a -> IdF.of(arrow.apply(a))));
            return FunctionArrow.of(source -> IdF.get(traversed.apply(source)));
        }

        @Override
        public <F extends K1, A, B> App2<FunctionArrow.Mu, App<F, A>, App<F, B>> mapping(
                Functor<F, ?> functor,
                App2<FunctionArrow.Mu, A, B> value) {
            FunctionArrow<A, B> arrow = unbox(value);
            return FunctionArrow.of(fa -> functor.map(arrow, fa));
        }

        @Override
        public <A, B> App2<FunctionArrow.Mu, A, B> zero(App2<FunctionArrow.Mu, A, B> function) {
            return function;
        }

        @Override
        public <A, B> App2<FunctionArrow.Mu, A, B> plus(
                App2<Procompose.Mu<FunctionArrow.Mu, FunctionArrow.Mu>, A, B> input) {
            return compose(Procompose.unbox(input));
        }

        private <A, B, C> App2<FunctionArrow.Mu, A, B> compose(
                Procompose<FunctionArrow.Mu, FunctionArrow.Mu, A, B, C> input) {
            FunctionArrow<A, C> first = unbox(input.first().get());
            FunctionArrow<C, B> second = unbox(input.second());
            return FunctionArrow.of(value -> second.apply(first.apply(value)));
        }
    }
}
