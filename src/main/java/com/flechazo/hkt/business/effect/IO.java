package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.business.capability.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.core.*;
import com.flechazo.hkt.business.data.*;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.*;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Monad;
import com.flechazo.hkt.MonadError;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.CheckedSupplier;
import com.flechazo.hkt.Unit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface IO<A> extends App<IO.Mu, A> {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    A unsafeRun() throws Exception;

    default A unsafeRunSync() throws Exception {
        return unsafeRun();
    }

    static <A, X extends Exception> IO<A> delay(CheckedSupplier<? extends A, X> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return () -> Objects.requireNonNull(supplier.get(), "supplier result");
    }

    static <A> IO<A> pure(A value) {
        Objects.requireNonNull(value, "value");
        return () -> value;
    }

    static <A> IO<A> failed(Throwable error) {
        Objects.requireNonNull(error, "error");
        return () -> {
            if (error instanceof Exception exception) {
                throw exception;
            }
            if (error instanceof Error fatal) {
                throw fatal;
            }
            throw new RuntimeException(error);
        };
    }

    static IO<Unit> exec(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return () -> {
            runnable.run();
            return Unit.INSTANCE;
        };
    }

    static IO<Unit> fromRunnable(Runnable runnable) {
        return exec(runnable);
    }

    static IO<Unit> unit() {
        return pure(Unit.INSTANCE);
    }

    static <A> IO<A> unbox(App<Mu, A> value) {
        return (IO<A>) Objects.requireNonNull(value, "value");
    }

    static Applicative<IO.Mu, IOMonad.Mu> applicative() {
        return IOMonad.INSTANCE;
    }

    static Monad<IO.Mu, IOMonad.Mu> monad() {
        return IOMonad.INSTANCE;
    }

    static MonadError<IO.Mu, Throwable, IOMonad.Mu> monadError() {
        return IOMonad.INSTANCE;
    }

    static Selective<IO.Mu, IOMonad.Mu> selective() {
        return IOMonad.INSTANCE;
    }

    default <B> IO<B> map(Function<? super A, ? extends B> f) {
        Objects.requireNonNull(f, "f");
        return () -> Objects.requireNonNull(f.apply(unsafeRun()), "map result");
    }

    default <B> IO<B> flatMap(Function<? super A, ? extends IO<B>> f) {
        Objects.requireNonNull(f, "f");
        return new FlatMappedIO<>(this, f);
    }

    default <B> IO<B> then(Supplier<IO<B>> next) {
        Objects.requireNonNull(next, "next");
        return flatMap(ignored -> Objects.requireNonNull(next.get(), "next io"));
    }

    default IO<A> peek(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action");
        return map(value -> {
            action.accept(value);
            return value;
        });
    }

    default IO<A> recover(Function<? super Throwable, ? extends A> f) {
        Objects.requireNonNull(f, "f");
        return () -> {
            try {
                return unsafeRun();
            } catch (Throwable error) {
                return Objects.requireNonNull(f.apply(error), "recover result");
            }
        };
    }

    default IO<A> recoverWith(Function<? super Throwable, IO<A>> f) {
        Objects.requireNonNull(f, "f");
        return () -> {
            try {
                return unsafeRun();
            } catch (Throwable error) {
                return Objects.requireNonNull(f.apply(error), "recoverWith result").unsafeRun();
            }
        };
    }

    default IO<A> mapError(Function<? super Throwable, ? extends Throwable> f) {
        Objects.requireNonNull(f, "f");
        return () -> {
            try {
                return unsafeRun();
            } catch (Throwable error) {
                Throwable mapped = Objects.requireNonNull(f.apply(error), "mapError result");
                if (mapped instanceof Exception exception) {
                    throw exception;
                }
                if (mapped instanceof Error fatal) {
                    throw fatal;
                }
                throw new RuntimeException(mapped);
            }
        };
    }

    default IO<Either<Throwable, A>> attempt() {
        return () -> {
            try {
                return Either.right(unsafeRun());
            } catch (Throwable error) {
                return Either.left(error);
            }
        };
    }

    default IO<Unit> voided() {
        return map(ignored -> Unit.INSTANCE);
    }

    default IO<Unit> asUnit() {
        return voided();
    }

    default Task<A> toTask() {
        return Task.delay(this::unsafeRun);
    }

    default Task<A> toTask(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return Task.async(() -> toTask().unsafeRunAsync(executor));
    }

    final class FlatMappedIO<A, B> implements IO<B> {
        private final IO<A> source;
        private final Function<? super A, ? extends IO<B>> f;

        FlatMappedIO(IO<A> source, Function<? super A, ? extends IO<B>> f) {
            this.source = Objects.requireNonNull(source, "source");
            this.f = Objects.requireNonNull(f, "f");
        }

        @Override
        @SuppressWarnings("unchecked")
        public B unsafeRun() throws Exception {
            IO<?> current = this;
            Deque<Function<Object, IO<?>>> continuations = new ArrayDeque<>();

            while (true) {
                if (current instanceof FlatMappedIO<?, ?> fm) {
                    continuations.push((Function<Object, IO<?>>) fm.f);
                    current = fm.source;
                } else {
                    Object result = current.unsafeRun();
                    if (continuations.isEmpty()) {
                        return (B) result;
                    }
                    current = Objects.requireNonNull(continuations.pop().apply(result), "flatMap result");
                }
            }
        }
    }

    enum IOMonad implements MonadError<IO.Mu, Throwable, IOMonad.Mu>,
            Selective<IO.Mu, IOMonad.Mu> {
        INSTANCE;

        public static final class Mu implements MonadError.Mu {
            private Mu() {
            }
        }

        @Override
        public <A> App<IO.Mu, A> of(A value) {
            return IO.pure(value);
        }

        @Override
        public <A, B> App<IO.Mu, B> flatMap(
                Function<? super A, ? extends App<IO.Mu, B>> f,
                App<IO.Mu, A> fa) {
            Objects.requireNonNull(f, "f");
            return IO.unbox(fa).flatMap(value -> IO.unbox(Objects.requireNonNull(f.apply(value), "flatMap result")));
        }

        @Override
        public <A> App<IO.Mu, A> raiseError(Throwable error) {
            return IO.failed(error);
        }

        @Override
        public <A> App<IO.Mu, A> handleErrorWith(
                App<IO.Mu, A> value,
                Function<? super Throwable, ? extends App<IO.Mu, A>> handler) {
            Objects.requireNonNull(handler, "handler");
            return IO.unbox(value).recoverWith(error ->
                    IO.unbox(Objects.requireNonNull(handler.apply(error), "handler result")));
        }

        @Override
        public <A, B> App<IO.Mu, B> select(
                App<IO.Mu, Either<A, B>> value,
                App<IO.Mu, ? extends Function<A, B>> function) {
            return IO.unbox(value).flatMap(inner -> {
                Either<A, B> either = Objects.requireNonNull(inner, "select value");
                if (either.isRight()) {
                    return IO.pure(either.right());
                }
                return IO.unbox(function)
                        .map(fn -> Objects.requireNonNull(fn, "select function").apply(either.left()));
            });
        }

        @Override
        public <A> App<IO.Mu, A> ifS(
                App<IO.Mu, Boolean> condition,
                Supplier<? extends App<IO.Mu, A>> thenValue,
                Supplier<? extends App<IO.Mu, A>> elseValue) {
            Objects.requireNonNull(thenValue, "thenValue");
            Objects.requireNonNull(elseValue, "elseValue");
            return IO.unbox(condition).flatMap(test -> {
                Supplier<? extends App<IO.Mu, A>> branch = Boolean.TRUE.equals(test) ? thenValue : elseValue;
                return IO.unbox(Objects.requireNonNull(branch.get(), "ifS branch result"));
            });
        }
    }
}
