package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public record IdF<A>(@Nullable A value) implements App<IdF.Mu, A> {
    public static final class Mu implements K1 {
        private Mu() {
        }
    }

    public static <A> IdF<A> of(@Nullable A value) {
        return new IdF<>(value);
    }

    public static <A> IdF<A> narrow(App<Mu, A> value) {
        return (IdF<A>) Objects.requireNonNull(value, "value");
    }

    public static Applicative<Mu> applicative() {
        return IdFMonad.INSTANCE;
    }

    public static Monad<Mu> monad() {
        return IdFMonad.INSTANCE;
    }

    public static Selective<Mu> selective() {
        return IdFMonad.INSTANCE;
    }

    private enum IdFMonad implements Monad<Mu>, Selective<Mu> {
        INSTANCE;

        @Override
        public <A> App<Mu, A> of(@Nullable A value) {
            return IdF.of(value);
        }

        @Override
        public <A, B> App<Mu, B> flatMap(
                Function<? super A, ? extends App<Mu, B>> f, App<Mu, A> fa) {
            return Objects.requireNonNull(f.apply(narrow(fa).value()), "flatMap result");
        }

        @Override
        public <A, B> App<Mu, B> select(
                App<Mu, Either<A, B>> value, App<Mu, ? extends Function<A, B>> function) {
            Either<A, B> either = Objects.requireNonNull(narrow(value).value(), "select value");
            if (either.isRight()) {
                return IdF.of(either.right());
            }
            Function<A, B> fn = Objects.requireNonNull(narrow(function).value(), "select function");
            return IdF.of(fn.apply(either.left()));
        }

        @Override
        public <A> App<Mu, A> ifS(
                App<Mu, Boolean> condition,
                Supplier<? extends App<Mu, A>> thenValue,
                Supplier<? extends App<Mu, A>> elseValue) {
            Supplier<? extends App<Mu, A>> branch = Boolean.TRUE.equals(narrow(condition).value())
                    ? thenValue
                    : elseValue;
            return Objects.requireNonNull(branch.get(), "ifS branch result");
        }
    }
}
