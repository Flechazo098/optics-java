package com.flechazo.hkt;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public sealed interface Maybe<A> extends App<Maybe.Mu, A> permits Maybe.Some, Maybe.None {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    boolean isDefined();

    default boolean isEmpty() {
        return !isDefined();
    }

    @Nullable A get();

    default <B> Maybe<B> map(Function<? super A, ? extends B> f) {
        Objects.requireNonNull(f, "f");
        return isDefined() ? some(f.apply(get())) : none();
    }

    default <B> Maybe<B> flatMap(Function<? super A, Maybe<B>> f) {
        Objects.requireNonNull(f, "f");
        return isDefined() ? Objects.requireNonNull(f.apply(get()), "flatMap result") : none();
    }

    default Maybe<A> filter(Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return isDefined() && predicate.test(get()) ? this : none();
    }

    default Maybe<A> or(Supplier<Maybe<A>> fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return isDefined() ? this : Objects.requireNonNull(fallback.get(), "fallback result");
    }

    default @Nullable A orElse(@Nullable A defaultValue) {
        return isDefined() ? get() : defaultValue;
    }

    default @Nullable A orElseGet(Supplier<? extends A> defaultValue) {
        return isDefined() ? get() : defaultValue.get();
    }

    static <A> Maybe<A> some(@Nullable A value) {
        return new Some<>(value);
    }

    @SuppressWarnings("unchecked")
    static <A> Maybe<A> none() {
        return (Maybe<A>) None.INSTANCE;
    }

    static <A> Maybe<A> ofNullable(@Nullable A value) {
        return value == null ? none() : some(value);
    }

    static <A> Maybe<A> unbox(App<Mu, A> value) {
        return (Maybe<A>) Objects.requireNonNull(value, "value");
    }

    static Applicative<Mu> applicative() {
        return MaybeApplicative.INSTANCE;
    }

    static Monad<Mu> monad() {
        return MaybeApplicative.INSTANCE;
    }

    static Selective<Mu> selective() {
        return MaybeApplicative.INSTANCE;
    }

    record Some<A>(@Nullable A value) implements Maybe<A> {
        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public @Nullable A get() {
            return value;
        }
    }

    enum None implements Maybe<Object> {
        INSTANCE;

        @Override
        public boolean isDefined() {
            return false;
        }

        @Override
        public @Nullable Object get() {
            throw new NoSuchElementException("Maybe.none");
        }

        @Override
        public String toString() {
            return "None";
        }
    }

    enum MaybeApplicative implements Monad<Mu>, Selective<Mu> {
        INSTANCE;

        @Override
        public <A> App<Mu, A> of(@Nullable A value) {
            return Maybe.some(value);
        }

        @Override
        public <A, B> App<Mu, B> flatMap(
                Function<? super A, ? extends App<Mu, B>> f, App<Mu, A> fa) {
            Maybe<A> maybe = Maybe.unbox(fa);
            return maybe.isDefined() ? Objects.requireNonNull(f.apply(maybe.get()), "flatMap result") : Maybe.none();
        }

        @Override
        public <A, B> App<Mu, B> select(
                App<Mu, Either<A, B>> value, App<Mu, ? extends Function<A, B>> function) {
            Maybe<Either<A, B>> either = Maybe.unbox(value);
            if (either.isEmpty()) {
                return Maybe.none();
            }
            Either<A, B> inner = Objects.requireNonNull(either.get(), "select value");
            if (inner.isRight()) {
                return Maybe.some(inner.right());
            }
            Maybe<? extends Function<A, B>> maybeFunction = Maybe.unbox(function);
            if (maybeFunction.isEmpty()) {
                return Maybe.none();
            }
            Function<A, B> fn = Objects.requireNonNull(maybeFunction.get(), "select function");
            return Maybe.some(fn.apply(inner.left()));
        }

        @Override
        public <A> App<Mu, A> ifS(
                App<Mu, Boolean> condition,
                Supplier<? extends App<Mu, A>> thenValue,
                Supplier<? extends App<Mu, A>> elseValue) {
            Maybe<Boolean> maybeCondition = Maybe.unbox(condition);
            if (maybeCondition.isEmpty()) {
                return Maybe.none();
            }
            Supplier<? extends App<Mu, A>> branch = Boolean.TRUE.equals(maybeCondition.get()) ? thenValue : elseValue;
            return Objects.requireNonNull(branch.get(), "ifS branch result");
        }
    }
}
