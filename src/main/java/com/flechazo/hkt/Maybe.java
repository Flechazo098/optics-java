package com.flechazo.hkt;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public sealed interface Maybe<A> extends App<Maybe.Mu, A> permits Maybe.Some, Maybe.None {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    boolean isDefined();

    default boolean isEmpty() {
        return !isDefined();
    }

    A get();

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

    default A orElse(A defaultValue) {
        return isDefined() ? get() : Objects.requireNonNull(defaultValue, "defaultValue");
    }

    default A orElseGet(Supplier<? extends A> defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue");
        return isDefined() ? get() : Objects.requireNonNull(defaultValue.get(), "defaultValue result");
    }

    static <A> Maybe<A> some(A value) {
        return new Some<>(Objects.requireNonNull(value, "value"));
    }

    @SuppressWarnings("unchecked")
    static <A> Maybe<A> none() {
        return (Maybe<A>) None.INSTANCE;
    }

    static <A> Maybe<A> ofNullable(A value) {
        return value == null ? none() : some(value);
    }

    static <A> Maybe<A> unbox(App<Mu, A> value) {
        return (Maybe<A>) Objects.requireNonNull(value, "value");
    }

    static Applicative<Maybe.Mu, MaybeApplicative.MuProof> applicative() {
        return MaybeApplicative.INSTANCE;
    }

    static Monad<Maybe.Mu, MaybeApplicative.MuProof> monad() {
        return MaybeApplicative.INSTANCE;
    }

    static Selective<Maybe.Mu, MaybeApplicative.MuProof> selective() {
        return MaybeApplicative.INSTANCE;
    }

    record Some<A>(A value) implements Maybe<A> {
        public Some {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public A get() {
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
        public Object get() {
            throw new NoSuchElementException("Maybe.none");
        }

        @Override
        public String toString() {
            return "None";
        }
    }

    enum MaybeApplicative implements Monad<Maybe.Mu, MaybeApplicative.MuProof>,
            Selective<Maybe.Mu, MaybeApplicative.MuProof> {
        INSTANCE;

        static final class MuProof implements Applicative.Mu {
            private MuProof() {
            }
        }

        @Override
        public <A> App<Maybe.Mu, A> of(A value) {
            return Maybe.some(value);
        }

        @Override
        public <A, B> App<Maybe.Mu, B> flatMap(
                Function<? super A, ? extends App<Maybe.Mu, B>> f, App<Maybe.Mu, A> fa) {
            Maybe<A> maybe = Maybe.unbox(fa);
            return maybe.isDefined() ? Objects.requireNonNull(f.apply(maybe.get()), "flatMap result") : Maybe.none();
        }

        @Override
        public <A, B> App<Maybe.Mu, B> select(
                App<Maybe.Mu, Either<A, B>> value, App<Maybe.Mu, ? extends Function<A, B>> function) {
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
        public <A> App<Maybe.Mu, A> ifS(
                App<Maybe.Mu, Boolean> condition,
                Supplier<? extends App<Maybe.Mu, A>> thenValue,
                Supplier<? extends App<Maybe.Mu, A>> elseValue) {
            Maybe<Boolean> maybeCondition = Maybe.unbox(condition);
            if (maybeCondition.isEmpty()) {
                return Maybe.none();
            }
            Supplier<? extends App<Maybe.Mu, A>> branch =
                    Boolean.TRUE.equals(maybeCondition.get()) ? thenValue : elseValue;
            return Objects.requireNonNull(branch.get(), "ifS branch result");
        }
    }
}
