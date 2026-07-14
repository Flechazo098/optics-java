package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.flechazo.hkt.util.validation.Operation.FLAT_MAP;
import static com.flechazo.hkt.util.validation.Operation.IF_S;
import static com.flechazo.hkt.util.validation.Operation.SELECT;

public record IdF<A>(A value) implements App<IdF.Mu, A> {
    public IdF {
        Objects.requireNonNull(value, "value");
    }

    public static final class Mu implements K1 {
        private Mu() {
        }
    }

    public static <A> IdF<A> of(A value) {
        return new IdF<>(value);
    }

    public static <A> IdF<A> unbox(App<Mu, A> value) {
        return (IdF<A>) Validation.kind().narrowWithTypeCheck(value, IdF.class);
    }

    public static <A> A get(App<Mu, A> value) {
        return unbox(value).value();
    }

    public static Applicative<IdF.Mu, Instance.Mu> applicative() {
        return Instance.INSTANCE;
    }

    public static Monad<IdF.Mu, Instance.Mu> monad() {
        return Instance.INSTANCE;
    }

    public static Selective<IdF.Mu, Instance.Mu> selective() {
        return Instance.INSTANCE;
    }

    public enum Instance implements Monad<IdF.Mu, Instance.Mu>, Selective<IdF.Mu, Instance.Mu> {
        INSTANCE;

        public static final class Mu implements Applicative.Mu {
            private Mu() {
            }
        }

        @Override
        public <A> App<IdF.Mu, A> of(A value) {
            return IdF.of(value);
        }

        @Override
        public <A, B> App<IdF.Mu, B> flatMap(
                Function<? super A, ? extends App<IdF.Mu, B>> f, App<IdF.Mu, A> fa) {
            Validation.function().validateFlatMap(f, fa);
            return Validation.function().requireNonNullResult(f.apply(unbox(fa).value()), "f", FLAT_MAP);
        }

        @Override
        public <A, B> App<IdF.Mu, B> select(
                App<IdF.Mu, Either<A, B>> value, App<IdF.Mu, ? extends Function<A, B>> function) {
            Either<A, B> either = Validation.coreType().requireValue(unbox(value).value(), "select value", IdF.class, SELECT);
            if (either.isRight()) {
                return IdF.of(either.right());
            }
            Function<A, B> fn = Validation.coreType().requireValue(
                    unbox(function).value(),
                    "select function",
                    IdF.class,
                    SELECT);
            return IdF.of(fn.apply(either.left()));
        }

        @Override
        public <A> App<IdF.Mu, A> ifS(
                App<IdF.Mu, Boolean> condition,
                Supplier<? extends App<IdF.Mu, A>> thenValue,
                Supplier<? extends App<IdF.Mu, A>> elseValue) {
            Supplier<? extends App<IdF.Mu, A>> branch = Boolean.TRUE.equals(unbox(condition).value())
                    ? thenValue
                    : elseValue;
            return Validation.function().requireNonNullResult(branch.get(), "branch", IF_S);
        }
    }
}
