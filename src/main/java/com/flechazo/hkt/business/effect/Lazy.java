package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.*;
import com.flechazo.hkt.util.validation.Validation;
import com.google.common.reflect.TypeToken;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.flechazo.hkt.util.validation.Operation.*;

public final class Lazy<A> implements App<Lazy.Mu, A> {
    private final ThrowableSupplier<? extends A> computation;
    private volatile @Nullable A value;
    private volatile @Nullable Throwable failure;
    private volatile boolean evaluated;

    private Lazy(ThrowableSupplier<? extends A> computation) {
        this.computation = Validation.coreType().requireValue(computation, Lazy.class, CONSTRUCTION);
    }

    public static final class Mu implements K1 {
        public static final TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };

        private Mu() {
        }
    }

    public static <A> Lazy<A> now(A value) {
        A checked = Validation.coreType().requireValue(value, Lazy.class, OF);
        Lazy<A> lazy = new Lazy<>(() -> checked);
        lazy.value = checked;
        lazy.evaluated = true;
        return lazy;
    }

    public static <A> Lazy<A> pure(A value) {
        return now(value);
    }

    public static <A> Lazy<A> defer(ThrowableSupplier<? extends A> supplier) {
        Validation.function().require(supplier, "supplier", DEFER);
        return new Lazy<>(supplier);
    }

    public static <A> Lazy<A> unbox(App<Mu, A> value) {
        return cast(Validation.kind().narrowWithTypeCheck(value, Lazy.class));
    }

    public static Instance instance() {
        return Instance.INSTANCE;
    }

    public static Functor<Lazy.Mu, Instance.Mu> functor() {
        return Instance.INSTANCE;
    }

    public static Applicative<Lazy.Mu, Instance.Mu> applicative() {
        return Instance.INSTANCE;
    }

    public static Monad<Lazy.Mu, Instance.Mu> monad() {
        return Instance.INSTANCE;
    }

    public static Selective<Lazy.Mu, Instance.Mu> selective() {
        return Instance.INSTANCE;
    }

    public static Foldable<Lazy.Mu> foldable() {
        return Instance.INSTANCE;
    }

    public static Traversable<Lazy.Mu, Instance.Mu> traversable() {
        return Instance.INSTANCE;
    }

    public A force() throws Throwable {
        if (!evaluated) {
            synchronized (this) {
                if (!evaluated) {
                    try {
                        value = computation instanceof FlatMapComputation<?, ?>
                                ? evaluateFlatMapChain()
                                : Validation.coreType().requireValue(computation.get(), Lazy.class, RUN);
                    } catch (Throwable error) {
                        failure = error;
                    } finally {
                        evaluated = true;
                    }
                }
            }
        }
        Throwable evaluatedFailure = failure;
        if (evaluatedFailure != null) {
            throw evaluatedFailure;
        }
        A evaluatedValue = value;
        if (evaluatedValue == null) {
            throw new AssertionError("evaluated Lazy has neither a value nor a failure");
        }
        return evaluatedValue;
    }

    public A get() throws Throwable {
        return force();
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public boolean hasFailed() {
        return evaluated && failure != null;
    }

    public <B> Lazy<B> map(Function<? super A, ? extends B> mapper) {
        Validation.function().require(mapper, "mapper", MAP);
        return defer(() -> Validation.function().requireNonNullResult(mapper.apply(force()), "mapper", MAP));
    }

    public <B> Lazy<B> flatMap(Function<? super A, ? extends Lazy<B>> mapper) {
        Validation.function().require(mapper, "mapper", FLAT_MAP);
        return new Lazy<>(new FlatMapComputation<>(this, mapper));
    }

    @Override
    public String toString() {
        if (!evaluated) {
            return "Lazy[unevaluated]";
        }
        Throwable evaluatedFailure = failure;
        return evaluatedFailure == null
                ? "Lazy[" + value + "]"
                : "Lazy[failed: " + evaluatedFailure.getClass().getSimpleName() + "]";
    }

    @SuppressWarnings("unchecked")
    private A evaluateFlatMapChain() throws Throwable {
        Deque<Function<Object, Lazy<?>>> continuations = new ArrayDeque<>();
        Lazy<?> current = this;

        while (!current.evaluated
                && current.computation instanceof FlatMapComputation<?, ?>(var source, var function)) {
            continuations.push((Function<Object, Lazy<?>>) function);
            current = source;
        }

        Object result = current.force();
        while (!continuations.isEmpty()) {
            Lazy<?> next = Validation.function().requireNonNullResult(
                    continuations.pop().apply(result),
                    "mapper",
                    FLAT_MAP);
            while (!next.evaluated
                    && next.computation instanceof FlatMapComputation<?, ?>(var source, var function)) {
                continuations.push((Function<Object, Lazy<?>>) function);
                next = source;
            }
            result = next.force();
        }
        return (A) result;
    }

    private record FlatMapComputation<A, B>(
            Lazy<A> source,
            Function<? super A, ? extends Lazy<B>> function) implements ThrowableSupplier<B> {
        private FlatMapComputation {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(function, "function");
        }

        @Override
        public B get() throws Throwable {
            Lazy<B> next = Validation.function().requireNonNullResult(function.apply(source.force()), "mapper", FLAT_MAP);
            return next.force();
        }
    }

    public enum Instance implements Monad<Lazy.Mu, Instance.Mu>,
            Selective<Lazy.Mu, Instance.Mu>,
            Traversable<Lazy.Mu, Instance.Mu> {
        INSTANCE;

        public static final class Mu implements Monad.Mu, Traversable.Mu {
            public static final TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
            };

            private Mu() {
            }
        }

        /**
         * Creates an evaluated lazy value in encoded form.
         *
         * @param <A> the value type
         * @param value the contained value
         * @return the encoded lazy value
         */
        @Override
        public <A> App<Lazy.Mu, A> of(A value) {
            return Lazy.now(value);
        }

        /**
         * Defers transformation of an encoded lazy value until it is forced.
         *
         * @param <A> the source value type
         * @param <B> the result value type
         * @param f the value transformation
         * @param fa the source lazy value
         * @return the transformed lazy value in encoded form
         */
        @Override
        public <A, B> App<Lazy.Mu, B> map(Function<? super A, ? extends B> f, App<Lazy.Mu, A> fa) {
            Validation.function().validateMap(f, fa);
            return Lazy.unbox(fa).map(f);
        }

        /**
         * Applies an encoded lazy function to an encoded lazy argument when the result is forced.
         *
         * @param <A> the argument type
         * @param <B> the result type
         * @param ff the lazy function
         * @param fa the lazy argument
         * @return the application result in encoded lazy form
         */
        @Override
        public <A, B> App<Lazy.Mu, B> ap(App<Lazy.Mu, ? extends Function<A, B>> ff, App<Lazy.Mu, A> fa) {
            Validation.kind().validateAp(ff, fa);
            return Lazy.defer(() -> {
                Function<A, B> function = Validation.coreType().requireValue(Lazy.unbox(ff).force(), Lazy.class, AP);
                return Validation.function().requireNonNullResult(function.apply(Lazy.unbox(fa).force()), "function", AP);
            });
        }

        /**
         * Defers sequencing of an encoded lazy value selected from the source result.
         *
         * @param <A> the source value type
         * @param <B> the next value type
         * @param f the function selecting the next lazy value
         * @param fa the source lazy value
         * @return the sequenced lazy value in encoded form
         */
        @Override
        public <A, B> App<Lazy.Mu, B> flatMap(
                Function<? super A, ? extends App<Lazy.Mu, B>> f,
                App<Lazy.Mu, A> fa) {
            Validation.function().validateFlatMap(f, fa);
            return Lazy.unbox(fa).flatMap(value ->
                    Lazy.unbox(Validation.function().requireNonNullResult(f.apply(value), "f", FLAT_MAP)));
        }

        /**
         * Defers selective application and forces the encoded function only for a left result.
         *
         * @param <A> the function argument type
         * @param <B> the result type
         * @param value the lazy branch value
         * @param function the lazy function used for a left branch
         * @return the selected result in encoded lazy form
         */
        @Override
        public <A, B> App<Lazy.Mu, B> select(
                App<Lazy.Mu, Either<A, B>> value,
                App<Lazy.Mu, ? extends Function<A, B>> function) {
            Validation.kind().requireNonNull(value, SELECT, "value");
            Validation.kind().requireNonNull(function, SELECT, "function");
            return Lazy.defer(() -> {
                Either<A, B> either = Validation.coreType().requireValue(Lazy.unbox(value).force(), Lazy.class, SELECT);
                if (either.isRight()) {
                    return either.right();
                }
                Function<A, B> apply = Validation.coreType().requireValue(Lazy.unbox(function).force(), Lazy.class, SELECT);
                return Validation.function().requireNonNullResult(apply.apply(either.left()), "function", SELECT);
            });
        }

        /**
         * Defers evaluation of one lazy branch according to a lazy condition.
         *
         * @param <A> the result type
         * @param condition the lazy condition
         * @param thenValue the deferred lazy value used for a true condition
         * @param elseValue the deferred lazy value used for a false condition
         * @return the selected result in encoded lazy form
         */
        @Override
        public <A> App<Lazy.Mu, A> ifS(
                App<Lazy.Mu, Boolean> condition,
                Supplier<? extends App<Lazy.Mu, A>> thenValue,
                Supplier<? extends App<Lazy.Mu, A>> elseValue) {
            Validation.kind().requireNonNull(condition, IF_S, "condition");
            Validation.function().require(thenValue, "thenValue", IF_S);
            Validation.function().require(elseValue, "elseValue", IF_S);
            return Lazy.defer(() -> {
                Supplier<? extends App<Lazy.Mu, A>> branch =
                        Boolean.TRUE.equals(Lazy.unbox(condition).force()) ? thenValue : elseValue;
                App<Lazy.Mu, A> selected = Validation.function().requireNonNullResult(branch.get(), "branch", IF_S);
                return Lazy.unbox(selected).force();
            });
        }

        /**
         * Forces an encoded lazy value, maps it to a monoid, and returns the mapped value.
         *
         * @param <A> the source value type
         * @param <M> the mapped value type
         * @param monoid the monoid describing the mapped result type
         * @param f the value mapping function
         * @param value the lazy value to force
         * @return the mapped value
         */
        @Override
        public <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<Lazy.Mu, A> value) {
            Validation.function().validateFoldMap(monoid, f, value);
            try {
                return f.apply(Lazy.unbox(value).force());
            } catch (RuntimeException | Error error) {
                throw error;
            } catch (Throwable error) {
                throw new RuntimeException(error);
            }
        }

        /**
         * Forces an encoded lazy value and applies an applicative transformation.
         *
         * @param <F> the applicative witness type
         * @param <A> the source value type
         * @param <B> the result value type
         * @param applicative the applicative used for the transformation
         * @param f the effectful value transformation
         * @param value the lazy value to force
         * @return an encoded lazy result in the applicative context
         */
        @Override
        public <F extends K1, A, B> App<F, App<Lazy.Mu, B>> traverse(
                Applicative<F, ?> applicative,
                Function<? super A, ? extends App<F, B>> f,
                App<Lazy.Mu, A> value) {
            Validation.function().validateTraverse(applicative, f, value);
            try {
                App<F, B> mapped = Validation.function().requireNonNullResult(
                        f.apply(Lazy.unbox(value).force()),
                        "f",
                        TRAVERSE);
                return applicative.map(Lazy::now, mapped);
            } catch (RuntimeException | Error error) {
                throw error;
            } catch (Throwable error) {
                throw new RuntimeException(error);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <A> Lazy<A> cast(Lazy<?> value) {
        return (Lazy<A>) value;
    }
}
