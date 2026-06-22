package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;

public record FunctionArrow<A, B>(Function<? super A, ? extends B> function)
        implements App2<FunctionArrow.Mu, A, B> {
    public static final class Mu implements K2 {
        private Mu() {
        }
    }

    public FunctionArrow {
        Objects.requireNonNull(function, "function");
    }

    public static <A, B> FunctionArrow<A, B> of(Function<? super A, ? extends B> function) {
        return new FunctionArrow<>(function);
    }

    public static <A, B> FunctionArrow<A, B> narrow(App2<Mu, A, B> value) {
        return (FunctionArrow<A, B>) Objects.requireNonNull(value, "value");
    }

    public B apply(A value) {
        return function.apply(value);
    }

    public static FunctionArrowInstance instance() {
        return FunctionArrowInstance.INSTANCE;
    }

    public enum FunctionArrowInstance implements Strong<Mu>, Choice<Mu>, Closed<Mu>, Monoidal<Mu> {
        INSTANCE;

        @Override
        public <A, B, C, D> App2<Mu, C, D> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right,
                App2<Mu, A, B> value) {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            FunctionArrow<A, B> arrow = narrow(value);
            return FunctionArrow.of(input -> right.apply(arrow.apply(left.apply(input))));
        }

        @Override
        public <A, B, C> App2<Mu, Pair<A, C>, Pair<B, C>> first(App2<Mu, A, B> value) {
            FunctionArrow<A, B> arrow = narrow(value);
            return FunctionArrow.of(pair -> Pair.of(arrow.apply(pair.first()), pair.second()));
        }

        @Override
        public <A, B, C> App2<Mu, Pair<C, A>, Pair<C, B>> second(App2<Mu, A, B> value) {
            FunctionArrow<A, B> arrow = narrow(value);
            return FunctionArrow.of(pair -> Pair.of(pair.first(), arrow.apply(pair.second())));
        }

        @Override
        public <A, B, C> App2<Mu, Either<A, C>, Either<B, C>> left(App2<Mu, A, B> value) {
            FunctionArrow<A, B> arrow = narrow(value);
            return FunctionArrow.of(either -> either.isLeft()
                    ? Either.left(arrow.apply(either.left()))
                    : Either.right(either.right()));
        }

        @Override
        public <A, B, C> App2<Mu, Either<C, A>, Either<C, B>> right(App2<Mu, A, B> value) {
            FunctionArrow<A, B> arrow = narrow(value);
            return FunctionArrow.of(either -> either.isRight()
                    ? Either.right(arrow.apply(either.right()))
                    : Either.left(either.left()));
        }

        @Override
        public <A, B, X> App2<Mu, Function<X, A>, Function<X, B>> closed(App2<Mu, A, B> value) {
            FunctionArrow<A, B> arrow = narrow(value);
            return FunctionArrow.of(function -> input -> arrow.apply(function.apply(input)));
        }

        @Override
        public <A, B, C, D> App2<Mu, Pair<A, C>, Pair<B, D>> par(
                App2<Mu, A, B> left,
                App2<Mu, C, D> right) {
            FunctionArrow<A, B> leftArrow = narrow(left);
            FunctionArrow<C, D> rightArrow = narrow(right);
            return FunctionArrow.of(pair -> Pair.of(leftArrow.apply(pair.first()), rightArrow.apply(pair.second())));
        }
    }
}
