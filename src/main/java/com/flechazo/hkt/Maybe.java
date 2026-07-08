package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.NoSuchElementException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.flechazo.hkt.util.validation.Operation.FLAT_MAP;
import static com.flechazo.hkt.util.validation.Operation.FOLD_MAP;
import static com.flechazo.hkt.util.validation.Operation.IF_S;
import static com.flechazo.hkt.util.validation.Operation.MAP;
import static com.flechazo.hkt.util.validation.Operation.SELECT;
import static com.flechazo.hkt.util.validation.Operation.SOME;
import static com.flechazo.hkt.util.validation.Operation.TRAVERSE;

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
        Validation.function().require(f, "f", MAP);
        return isDefined() ? some(f.apply(get())) : none();
    }

    default <B> Maybe<B> flatMap(Function<? super A, Maybe<B>> f) {
        Validation.function().require(f, "f", FLAT_MAP);
        return isDefined() ? Validation.function().requireNonNullResult(f.apply(get()), "f", FLAT_MAP) : none();
    }

    default <B> B fold(Supplier<? extends B> ifEmpty, Function<? super A, ? extends B> ifDefined) {
        return isDefined() ? ifDefined.apply(get()) : ifEmpty.get();
    }

    default Maybe<A> filter(Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return isDefined() && predicate.test(get()) ? this : none();
    }

    default Maybe<A> or(Supplier<Maybe<A>> fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return isDefined() ? this : Objects.requireNonNull(fallback.get(), "fallback result");
    }

    default <E> Either<E, A> toEither(Supplier<? extends E> ifEmpty) {
        Objects.requireNonNull(ifEmpty, "ifEmpty");
        return isDefined() ? Either.right(get()) : Either.left(Objects.requireNonNull(ifEmpty.get(), "empty value"));
    }

    default <E> Either<E, A> toEither(E error) {
        return isDefined() ? Either.right(get()) : Either.left(error);
    }

    default <E> Validated<E, A> toValidated(Supplier<? extends E> ifEmpty) {
        Objects.requireNonNull(ifEmpty, "ifEmpty");
        return isDefined() ? Validated.valid(get()) : Validated.invalid(Objects.requireNonNull(ifEmpty.get(), "empty value"));
    }

    default <E> Validated<E, A> toValidated(E error) {
        return isDefined() ? Validated.valid(get()) : Validated.invalid(error);
    }

    default Maybe<A> peek(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action");
        if (isDefined()) action.accept(get());
        return this;
    }

    default void ifPresent(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action");
        if (isDefined()) action.accept(get());
    }

    default void ifEmpty(Runnable action) {
        if (isEmpty()) action.run();
    }

    default Maybe<A> tapNone(Runnable action) {
        if (isEmpty()) action.run();
        return this;
    }

    default List<A> toList() {
        return isDefined() ? List.of(get()) : List.of();
    }

    default A orElse(A defaultValue) {
        return isDefined() ? get() : Objects.requireNonNull(defaultValue, "defaultValue");
    }

    default A orElseGet(Supplier<? extends A> defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue");
        return isDefined() ? get() : Objects.requireNonNull(defaultValue.get(), "defaultValue result");
    }

    static <A> Maybe<A> some(A value) {
        return new Some<>(Validation.coreType().requireValue(value, Maybe.class, SOME));
    }

    static <A> Maybe<A> just(A value) {
        return some(value);
    }

    @SuppressWarnings("unchecked")
    static <A> Maybe<A> none() {
        return (Maybe<A>) None.INSTANCE;
    }

    static <A> Maybe<A> nothing() {
        return none();
    }

    static <A> Maybe<A> ofNullable(A value) {
        return value == null ? none() : some(value);
    }

    static <A> Maybe<A> unbox(App<Mu, A> value) {
        return (Maybe<A>) Validation.kind().narrowWithTypeCheck(value, Maybe.class);
    }

    static Applicative<Maybe.Mu, MaybeApplicative.MuProof> applicative() {
        return MaybeApplicative.INSTANCE;
    }

    static Monad<Maybe.Mu, MaybeApplicative.MuProof> monad() {
        return MaybeApplicative.INSTANCE;
    }

    static MonadZero<Maybe.Mu, MaybeApplicative.MuProof> monadZero() {
        return MaybeApplicative.INSTANCE;
    }

    static Selective<Maybe.Mu, MaybeApplicative.MuProof> selective() {
        return MaybeApplicative.INSTANCE;
    }

    static Foldable<Maybe.Mu> foldable() {
        return MaybeApplicative.INSTANCE;
    }

    static Traversable<Maybe.Mu, MaybeApplicative.MuProof> traversable() {
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

    enum MaybeApplicative implements MonadZero<Maybe.Mu, MaybeApplicative.MuProof>,
            Selective<Maybe.Mu, MaybeApplicative.MuProof>,
            Traversable<Maybe.Mu, MaybeApplicative.MuProof> {
        INSTANCE;

        public static final class MuProof implements MonadZero.Mu, Traversable.Mu {
            private MuProof() {
            }
        }

        @Override
        public <A> App<Maybe.Mu, A> of(A value) {
            return Maybe.some(value);
        }

        @Override
        public <A, B> App<Maybe.Mu, B> map(Function<? super A, ? extends B> f, App<Maybe.Mu, A> fa) {
            Validation.function().validateMap(f, fa);
            return Maybe.unbox(fa).map(f);
        }

        @Override
        public <A, B> App<Maybe.Mu, B> flatMap(
                Function<? super A, ? extends App<Maybe.Mu, B>> f, App<Maybe.Mu, A> fa) {
            Maybe<A> maybe = Maybe.unbox(fa);
            Validation.function().validateFlatMap(f, fa);
            return maybe.isDefined()
                    ? Validation.function().requireNonNullResult(f.apply(maybe.get()), "f", FLAT_MAP)
                    : Maybe.none();
        }

        @Override
        public <A> App<Maybe.Mu, A> zero() {
            return Maybe.none();
        }

        @Override
        public <A> App<Maybe.Mu, A> orElse(App<Maybe.Mu, A> first, Supplier<? extends App<Maybe.Mu, A>> second) {
            Objects.requireNonNull(second, "second");
            Maybe<A> maybe = Maybe.unbox(first);
            return maybe.isDefined()
                    ? maybe
                    : Objects.requireNonNull(second.get(), "second result");
        }

        @Override
        public <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<Maybe.Mu, A> value) {
            Validation.function().validateFoldMap(monoid, f, value);
            Maybe<A> maybe = Maybe.unbox(value);
            return maybe.isDefined() ? f.apply(maybe.get()) : monoid.empty();
        }

        @Override
        public <F extends K1, A, B> App<F, App<Maybe.Mu, B>> traverse(
                Applicative<F, ?> applicative,
                Function<? super A, ? extends App<F, B>> f,
                App<Maybe.Mu, A> value) {
            Validation.function().validateTraverse(applicative, f, value);
            Maybe<A> maybe = Maybe.unbox(value);
            if (maybe.isEmpty()) {
                return applicative.of(Maybe.none());
            }
            return applicative.map(Maybe::some,
                    Validation.function().requireNonNullResult(f.apply(maybe.get()), "f", TRAVERSE));
        }

        @Override
        public <A, B> App<Maybe.Mu, B> select(
                App<Maybe.Mu, Either<A, B>> value, App<Maybe.Mu, ? extends Function<A, B>> function) {
            Maybe<Either<A, B>> either = Maybe.unbox(value);
            if (either.isEmpty()) {
                return Maybe.none();
            }
            Either<A, B> inner = Validation.coreType().requireValue(either.get(), "select value", Maybe.class, SELECT);
            if (inner.isRight()) {
                return Maybe.some(inner.right());
            }
            Maybe<? extends Function<A, B>> maybeFunction = Maybe.unbox(function);
            if (maybeFunction.isEmpty()) {
                return Maybe.none();
            }
            Function<A, B> fn = Validation.coreType().requireValue(
                    maybeFunction.get(),
                    "select function",
                    Maybe.class,
                    SELECT);
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
            return Validation.function().requireNonNullResult(branch.get(), "branch", IF_S);
        }
    }
}
