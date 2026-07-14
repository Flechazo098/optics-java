package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.flechazo.hkt.util.validation.Operation.*;

public sealed interface Validated<E, A> extends App2<Validated.Mu, E, A>, App<Validated.RightMu<E>, A>
        permits Validated.Valid, Validated.Invalid {
    final class Mu implements K2 {
        private Mu() {
        }
    }

    final class RightMu<E> implements K1 {
        private RightMu() {
        }
    }

    boolean isValid();

    default boolean isInvalid() {
        return !isValid();
    }

    A value();

    E error();

    default <B> Validated<E, B> map(Function<? super A, ? extends B> f) {
        return isValid() ? valid(f.apply(value())) : invalid(error());
    }

    default <E2> Validated<E2, A> mapError(Function<? super E, ? extends E2> f) {
        return isValid() ? valid(value()) : invalid(f.apply(error()));
    }

    default <B> B fold(Function<? super E, ? extends B> invalid, Function<? super A, ? extends B> valid) {
        return isValid() ? valid.apply(value()) : invalid.apply(error());
    }

    default Validated<E, A> peek(Consumer<? super A> action) {
        if (isValid()) action.accept(value());
        return this;
    }

    default Validated<E, A> peekInvalid(Consumer<? super E> action) {
        if (isInvalid()) action.accept(error());
        return this;
    }

    default Either<E, A> toEither() {
        return isValid() ? Either.right(value()) : Either.left(error());
    }

    default Maybe<A> toMaybe() {
        return isValid() ? Maybe.some(value()) : Maybe.none();
    }

    default Try<A> toTry(Function<? super E, ? extends Throwable> invalidToThrowable) {
        return isValid() ? Try.success(value()) : Try.failure(invalidToThrowable.apply(error()));
    }

    default Validated<E, A> recover(Function<? super E, ? extends A> recovery) {
        return isValid() ? this : valid(recovery.apply(error()));
    }

    default Validated<E, A> combine(Validated<E, A> other, Semigroup<E> semigroup) {
        if (isValid()) return other;
        if (other.isValid()) return this;
        return invalid(semigroup.combine(error(), other.error()));
    }

    static <E, A> Validated<E, A> valid(A value) {
        return new Valid<>(Validation.coreType().requireValue(value, Validated.class, RIGHT));
    }

    static <E, A> Validated<E, A> invalid(E error) {
        return new Invalid<>(Validation.coreType().requireError(error, Validated.class, INVALID));
    }

    static <E, A> Validated<E, A> unbox(App<RightMu<E>, A> value) {
        return (Validated<E, A>) Validation.kind().narrowWithTypeCheck(value, Validated.class);
    }

    static <E, A> Validated<E, A> unbox(App2<Mu, E, A> value) {
        return (Validated<E, A>) Validation.kind().narrowWithTypeCheck2(value, Validated.class);
    }

    static <E> Applicative<RightMu<E>, ValidatedApplicative.Mu> applicative(Semigroup<E> errors) {
        return new ValidatedApplicative<>(errors);
    }

    static <E> Selective<RightMu<E>, ValidatedApplicative.Mu> selective(Semigroup<E> errors) {
        return new ValidatedApplicative<>(errors);
    }

    record Valid<E, A>(A value) implements Validated<E, A> {
        public Valid {
            Objects.requireNonNull(value, "value");
        }

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
        public A value() {
            throw new IllegalStateException("Validated.invalid has no value");
        }
    }

    final class ValidatedApplicative<E> implements Selective<RightMu<E>, ValidatedApplicative.Mu> {
        static final class Mu implements Applicative.Mu {
            private Mu() {
            }
        }

        private final Semigroup<E> errors;

        ValidatedApplicative(Semigroup<E> errors) {
            this.errors = Objects.requireNonNull(errors, "errors");
        }

        @Override
        public <A> App<RightMu<E>, A> of(A value) {
            return Validated.valid(value);
        }

        @Override
        public <A, B> App<RightMu<E>, B> map(
                Function<? super A, ? extends B> f, App<RightMu<E>, A> fa) {
            Validation.function().validateMap(f, fa);
            return Validated.unbox(fa).map(f);
        }

        @Override
        public <A, B> App<RightMu<E>, B> ap(
                App<RightMu<E>, ? extends Function<A, B>> ff,
                App<RightMu<E>, A> fa) {
            Validation.kind().validateAp(ff, fa);
            Validated<E, ? extends Function<A, B>> function = Validated.unbox(ff);
            Validated<E, A> value = Validated.unbox(fa);
            if (function.isValid() && value.isValid()) {
                Function<A, B> apply = Validation.coreType().requireValue(function.value(), "applicative function", Validated.class, AP);
                return Validated.valid(apply.apply(value.value()));
            }
            if (function.isInvalid() && value.isInvalid()) {
                return Validated.invalid(errors.combine(function.error(), value.error()));
            }
            return function.isInvalid() ? Validated.invalid(function.error()) : Validated.invalid(value.error());
        }

        @Override
        public <A, B> App<RightMu<E>, B> select(
                App<RightMu<E>, Either<A, B>> value,
                App<RightMu<E>, ? extends Function<A, B>> function) {
            Validated<E, Either<A, B>> either = Validated.unbox(value);
            Validated<E, ? extends Function<A, B>> fn = Validated.unbox(function);
            if (either.isInvalid()) {
                return fn.isInvalid()
                        ? Validated.invalid(errors.combine(either.error(), fn.error()))
                        : Validated.invalid(either.error());
            }
            Either<A, B> inner = Validation.coreType().requireValue(either.value(), "select value", Validated.class, SELECT);
            if (inner.isRight()) {
                return Validated.valid(inner.right());
            }
            if (fn.isInvalid()) {
                return Validated.invalid(fn.error());
            }
            Function<A, B> selected = Validation.coreType().requireValue(
                    fn.value(),
                    "select function",
                    Validated.class,
                    SELECT);
            return Validated.valid(selected.apply(inner.left()));
        }

        @Override
        public <A> App<RightMu<E>, A> ifS(
                App<RightMu<E>, Boolean> condition,
                Supplier<? extends App<RightMu<E>, A>> thenValue,
                Supplier<? extends App<RightMu<E>, A>> elseValue) {
            Validated<E, Boolean> test = Validated.unbox(condition);
            if (test.isInvalid()) {
                return Validated.invalid(test.error());
            }
            Supplier<? extends App<RightMu<E>, A>> branch =
                    Boolean.TRUE.equals(test.value()) ? thenValue : elseValue;
            return Validation.function().requireNonNullResult(branch.get(), "branch", IF_S);
        }
    }
}
