package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public sealed interface Validated<E, A> extends App2<Validated.Mu, E, A>
        permits Validated.Valid, Validated.Invalid {
    final class Mu implements K2 {
        private Mu() {
        }
    }

    boolean isValid();

    default boolean isInvalid() {
        return !isValid();
    }

    @Nullable A value();

    E error();

    default <B> Validated<E, B> map(Function<? super A, ? extends B> f) {
        return isValid() ? valid(f.apply(value())) : invalid(error());
    }

    static <E, A> Validated<E, A> valid(@Nullable A value) {
        return new Valid<>(value);
    }

    static <E, A> Validated<E, A> invalid(E error) {
        return new Invalid<>(Objects.requireNonNull(error, "error"));
    }

    static <E, A> Validated<E, A> narrow(App<App2.Mu<Mu, E>, A> value) {
        return (Validated<E, A>) Objects.requireNonNull(value, "value");
    }

    static <E, A> Validated<E, A> narrow2(App2<Mu, E, A> value) {
        return (Validated<E, A>) Objects.requireNonNull(value, "value");
    }

    static <E> Applicative<App2.Mu<Mu, E>> applicative(Semigroup<E> errors) {
        return new ValidatedApplicative<>(errors);
    }

    static <E> Selective<App2.Mu<Mu, E>> selective(Semigroup<E> errors) {
        return new ValidatedApplicative<>(errors);
    }

    record Valid<E, A>(@Nullable A value) implements Validated<E, A> {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public E error() {
            throw new IllegalStateException("Validated.valid has no error");
        }
    }

    record Invalid<E, A>(E error) implements Validated<E, A> {
        public Invalid {
            Objects.requireNonNull(error, "error");
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public @Nullable A value() {
            throw new IllegalStateException("Validated.invalid has no value");
        }
    }

    final class ValidatedApplicative<E> implements Selective<App2.Mu<Mu, E>> {
        private final Semigroup<E> errors;

        ValidatedApplicative(Semigroup<E> errors) {
            this.errors = Objects.requireNonNull(errors, "errors");
        }

        @Override
        public <A> App<App2.Mu<Mu, E>, A> of(@Nullable A value) {
            return Validated.valid(value);
        }

        @Override
        public <A, B> App<App2.Mu<Mu, E>, B> map(
                Function<? super A, ? extends B> f, App<App2.Mu<Mu, E>, A> fa) {
            return Validated.narrow(fa).map(f);
        }

        @Override
        public <A, B> App<App2.Mu<Mu, E>, B> ap(
                App<App2.Mu<Mu, E>, ? extends Function<A, B>> ff,
                App<App2.Mu<Mu, E>, A> fa) {
            Validated<E, ? extends Function<A, B>> function = Validated.narrow(ff);
            Validated<E, A> value = Validated.narrow(fa);
            if (function.isValid() && value.isValid()) {
                Function<A, B> apply = Objects.requireNonNull(function.value(), "applicative function");
                return Validated.valid(apply.apply(value.value()));
            }
            if (function.isInvalid() && value.isInvalid()) {
                return Validated.invalid(errors.combine(function.error(), value.error()));
            }
            return function.isInvalid() ? Validated.invalid(function.error()) : Validated.invalid(value.error());
        }

        @Override
        public <A, B> App<App2.Mu<Mu, E>, B> select(
                App<App2.Mu<Mu, E>, Either<A, B>> value,
                App<App2.Mu<Mu, E>, ? extends Function<A, B>> function) {
            Validated<E, Either<A, B>> either = Validated.narrow(value);
            Validated<E, ? extends Function<A, B>> fn = Validated.narrow(function);
            if (either.isInvalid()) {
                return fn.isInvalid()
                        ? Validated.invalid(errors.combine(either.error(), fn.error()))
                        : Validated.invalid(either.error());
            }
            Either<A, B> inner = Objects.requireNonNull(either.value(), "select value");
            if (inner.isRight()) {
                return Validated.valid(inner.right());
            }
            if (fn.isInvalid()) {
                return Validated.invalid(fn.error());
            }
            return Validated.valid(Objects.requireNonNull(fn.value(), "select function").apply(inner.left()));
        }

        @Override
        public <A> App<App2.Mu<Mu, E>, A> ifS(
                App<App2.Mu<Mu, E>, Boolean> condition,
                Supplier<? extends App<App2.Mu<Mu, E>, A>> thenValue,
                Supplier<? extends App<App2.Mu<Mu, E>, A>> elseValue) {
            Validated<E, Boolean> test = Validated.narrow(condition);
            if (test.isInvalid()) {
                return Validated.invalid(test.error());
            }
            Supplier<? extends App<App2.Mu<Mu, E>, A>> branch =
                    Boolean.TRUE.equals(test.value()) ? thenValue : elseValue;
            return Objects.requireNonNull(branch.get(), "ifS branch result");
        }
    }
}
