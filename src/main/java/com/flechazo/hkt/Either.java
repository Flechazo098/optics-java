package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.flechazo.hkt.util.validation.Operation.FLAT_MAP;
import static com.flechazo.hkt.util.validation.Operation.FOLD_MAP;
import static com.flechazo.hkt.util.validation.Operation.HANDLE_ERROR_WITH;
import static com.flechazo.hkt.util.validation.Operation.IF_S;
import static com.flechazo.hkt.util.validation.Operation.LEFT;
import static com.flechazo.hkt.util.validation.Operation.MAP;
import static com.flechazo.hkt.util.validation.Operation.RIGHT;
import static com.flechazo.hkt.util.validation.Operation.SELECT;
import static com.flechazo.hkt.util.validation.Operation.TRAVERSE;

public sealed interface Either<L, R> extends App2<Either.Mu, L, R>, App<Either.RightMu<L>, R>
        permits Either.Left, Either.Right {
    final class Mu implements K2 {
        private Mu() {
        }
    }

    final class RightMu<L> implements K1 {
        private RightMu() {
        }
    }

    /**
     * RightMu keeps the normal Either semantics: fix L and map/flatMap the successful R side.
     * LeftMu is a separate optimizer view used by choice/traversal proofs when the left side must be treated
     * as the one-parameter container without flipping Either's public right-biased API.
     */
    final class LeftMu<R> implements K1 {
        private LeftMu() {
        }
    }

    boolean isLeft();

    default boolean isRight() {
        return !isLeft();
    }

    L left();

    R right();

    default <B> Either<L, B> map(Function<? super R, ? extends B> f) {
        return isRight() ? right(f.apply(right())) : left(left());
    }

    default <B> Either<B, R> mapLeft(Function<? super L, ? extends B> f) {
        return isLeft() ? left(f.apply(left())) : right(right());
    }

    default <L2, R2> Either<L2, R2> mapBoth(
            Function<? super L, ? extends L2> leftMapper,
            Function<? super R, ? extends R2> rightMapper) {
        return isLeft() ? left(leftMapper.apply(left())) : right(rightMapper.apply(right()));
    }

    default <T> T fold(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rightMapper) {
        return isLeft() ? leftMapper.apply(left()) : rightMapper.apply(right());
    }

    default <B> Either<L, B> flatMap(Function<? super R, Either<L, B>> f) {
        Validation.function().require(f, "f", FLAT_MAP);
        return isRight() ? Validation.function().requireNonNullResult(f.apply(right()), "f", FLAT_MAP) : left(left());
    }

    default Maybe<R> toMaybe() {
        return isRight() ? Maybe.some(right()) : Maybe.none();
    }

    default Validated<L, R> toValidated() {
        return isRight() ? Validated.valid(right()) : Validated.invalid(left());
    }

    default Try<R> toTry(Function<? super L, ? extends Throwable> leftToThrowable) {
        return isRight() ? Try.success(right()) : Try.failure(leftToThrowable.apply(left()));
    }

    default R getOrElse(R fallback) {
        return isRight() ? right() : Objects.requireNonNull(fallback, "fallback");
    }

    default R getOrElseGet(Supplier<? extends R> fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return isRight() ? right() : Objects.requireNonNull(fallback.get(), "fallback result");
    }

    default Either<L, R> peek(Consumer<? super R> action) {
        Objects.requireNonNull(action, "action");
        if (isRight()) action.accept(right());
        return this;
    }

    default Either<L, R> peekLeft(Consumer<? super L> action) {
        Objects.requireNonNull(action, "action");
        if (isLeft()) action.accept(left());
        return this;
    }

    default L getLeft() {
        return left();
    }

    default R getRight() {
        return right();
    }

    default void ifLeft(Consumer<? super L> action) {
        if (isLeft()) action.accept(left());
    }

    default void ifRight(Consumer<? super R> action) {
        if (isRight()) action.accept(right());
    }

    default Either<L, R> recover(Function<? super L, ? extends R> recovery) {
        return isRight() ? this : right(recovery.apply(left()));
    }

    default Either<L, R> recoverWith(Function<? super L, ? extends Either<L, R>> recovery) {
        return isRight() ? this : recovery.apply(left());
    }

    default Either<R, L> swap() {
        return isLeft() ? right(left()) : left(right());
    }

    static <L, R> Either<L, R> left(L value) {
        return new Left<>(Validation.coreType().requireError(value, Either.class, LEFT));
    }

    static <L, R> Either<L, R> right(R value) {
        return new Right<>(Validation.coreType().requireValue(value, Either.class, RIGHT));
    }

    static <L, R> Either<L, R> fromMaybe(Maybe<? extends R> value, Supplier<? extends L> ifEmpty) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(ifEmpty, "ifEmpty");
        return value.isDefined()
                ? Either.right(value.get())
                : Either.left(Objects.requireNonNull(ifEmpty.get(), "empty value"));
    }

    static <R> Either<Throwable, R> catching(CheckedSupplier<? extends R, ?> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        try {
            return Either.right(Objects.requireNonNull(supplier.get(), "supplier result"));
        } catch (Exception exception) {
            return Either.left(exception);
        }
    }

    static <L, R> Either<L, R> unbox(App<RightMu<L>, R> value) {
        return (Either<L, R>) Validation.kind().narrowWithTypeCheck(value, Either.class);
    }

    static <L, R> Either<L, R> unbox(App2<Mu, L, R> value) {
        return (Either<L, R>) Validation.kind().narrowWithTypeCheck2(value, Either.class);
    }

    @SuppressWarnings("unchecked")
    static <L> Applicative<RightMu<L>, EitherMonad.Mu> applicative() {
        return (Applicative<RightMu<L>, EitherMonad.Mu>) (Applicative<?, ?>) EitherMonad.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    static <L> Monad<RightMu<L>, EitherMonad.Mu> monad() {
        return (Monad<RightMu<L>, EitherMonad.Mu>) (Monad<?, ?>) EitherMonad.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    static <L> MonadError<RightMu<L>, L, EitherMonad.Mu> monadError() {
        return (MonadError<RightMu<L>, L, EitherMonad.Mu>) (MonadError<?, ?, ?>) EitherMonad.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    static <L> Foldable<RightMu<L>> foldable() {
        return (Foldable<RightMu<L>>) (Foldable<?>) EitherMonad.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    static <L> Traversable<RightMu<L>, EitherMonad.Mu> traversable() {
        return (Traversable<RightMu<L>, EitherMonad.Mu>) (Traversable<?, ?>) EitherMonad.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    static <L> Selective<RightMu<L>, EitherMonad.Mu> selective() {
        return (Selective<RightMu<L>, EitherMonad.Mu>) (Selective<?, ?>) EitherMonad.INSTANCE;
    }

    static <L, R> App<LeftMu<R>, L> leftProjection(Either<L, R> value) {
        return new LeftProjection<>(value);
    }

    static <L, R> Either<L, R> unboxLeftProjection(App<LeftMu<R>, L> value) {
        return ((LeftProjection<L, R>) Validation.kind().narrowWithTypeCheck(value, LeftProjection.class)).value();
    }

    record Left<L, R>(L value) implements Either<L, R> {
        public Left {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public L left() {
            return value;
        }

        @Override
        public R right() {
            throw new IllegalStateException("Either.left has no right value");
        }
    }

    record Right<L, R>(R value) implements Either<L, R> {
        public Right {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public boolean isLeft() {
            return false;
        }

        @Override
        public L left() {
            throw new IllegalStateException("Either.right has no left value");
        }

        @Override
        public R right() {
            return value;
        }
    }

    record LeftProjection<L, R>(Either<L, R> value) implements App<LeftMu<R>, L> {
        public LeftProjection {
            Objects.requireNonNull(value, "value");
        }
    }

    enum EitherMonad implements MonadError<RightMu<Object>, Object, EitherMonad.Mu>,
            Selective<RightMu<Object>, EitherMonad.Mu>,
            Traversable<RightMu<Object>, EitherMonad.Mu> {
        INSTANCE;

        public static final class Mu implements MonadError.Mu, Traversable.Mu {
            private Mu() {
            }
        }

        @Override
        public <A> App<RightMu<Object>, A> of(A value) {
            return Either.right(value);
        }

        @Override
        public <A, B> App<RightMu<Object>, B> map(Function<? super A, ? extends B> f, App<RightMu<Object>, A> fa) {
            Validation.function().validateMap(f, fa);
            return Either.unbox(fa).map(f);
        }

        @Override
        public <A, B> App<RightMu<Object>, B> flatMap(
                Function<? super A, ? extends App<RightMu<Object>, B>> f,
                App<RightMu<Object>, A> fa) {
            Either<Object, A> either = Either.unbox(fa);
            Validation.function().validateFlatMap(f, fa);
            return either.isRight()
                    ? Validation.function().requireNonNullResult(f.apply(either.right()), "f", FLAT_MAP)
                    : Either.left(either.left());
        }

        @Override
        public <A> App<RightMu<Object>, A> raiseError(Object error) {
            return Either.left(error);
        }

        @Override
        public <A> App<RightMu<Object>, A> handleErrorWith(
                App<RightMu<Object>, A> value,
                Function<? super Object, ? extends App<RightMu<Object>, A>> handler) {
            Validation.function().validateHandleErrorWith(value, handler);
            Either<Object, A> either = Either.unbox(value);
            return either.isRight()
                    ? either
                    : Validation.function().requireNonNullResult(handler.apply(either.left()), "handler", HANDLE_ERROR_WITH);
        }

        @Override
        public <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<RightMu<Object>, A> value) {
            Validation.function().validateFoldMap(monoid, f, value);
            Either<Object, A> either = Either.unbox(value);
            return either.isRight() ? f.apply(either.right()) : monoid.empty();
        }

        @Override
        public <F extends K1, A, B> App<F, App<RightMu<Object>, B>> traverse(
                Applicative<F, ?> applicative,
                Function<? super A, ? extends App<F, B>> f,
                App<RightMu<Object>, A> value) {
            Validation.function().validateTraverse(applicative, f, value);
            Either<Object, A> either = Either.unbox(value);
            if (either.isLeft()) {
                return applicative.of(Either.left(either.left()));
            }
            return applicative.map(Either::right,
                    Validation.function().requireNonNullResult(f.apply(either.right()), "f", TRAVERSE));
        }

        @Override
        public <A, B> App<RightMu<Object>, B> select(
                App<RightMu<Object>, Either<A, B>> value,
                App<RightMu<Object>, ? extends Function<A, B>> function) {
            Either<Object, Either<A, B>> either = Either.unbox(value);
            if (either.isLeft()) {
                return Either.left(either.left());
            }
            Either<A, B> inner = Validation.coreType().requireValue(either.right(), "select value", Either.class, SELECT);
            if (inner.isRight()) {
                return Either.right(inner.right());
            }
            Either<Object, ? extends Function<A, B>> fn = Either.unbox(function);
            if (fn.isLeft()) {
                return Either.left(fn.left());
            }
            Function<A, B> selected = Validation.coreType().requireValue(
                    fn.right(),
                    "select function",
                    Either.class,
                    SELECT);
            return Either.right(selected.apply(inner.left()));
        }

        @Override
        public <A> App<RightMu<Object>, A> ifS(
                App<RightMu<Object>, Boolean> condition,
                Supplier<? extends App<RightMu<Object>, A>> thenValue,
                Supplier<? extends App<RightMu<Object>, A>> elseValue) {
            Either<Object, Boolean> test = Either.unbox(condition);
            if (test.isLeft()) {
                return Either.left(test.left());
            }
            Supplier<? extends App<RightMu<Object>, A>> branch =
                    Boolean.TRUE.equals(test.right()) ? thenValue : elseValue;
            return Validation.function().requireNonNullResult(branch.get(), "branch", IF_S);
        }
    }
}
