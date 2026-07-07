package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.resilience.Bulkhead;
import com.flechazo.hkt.business.resilience.CircuitBreaker;
import com.flechazo.hkt.business.resilience.Retry;
import com.flechazo.hkt.business.resilience.RetryPolicy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface VIO<A> extends App<VIO.Mu, A> {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    A unsafeRun() throws Exception;

    default A unsafeRunSync() throws Exception {
        return unsafeRun();
    }

    static <A, X extends Exception> VIO<A> delay(CheckedSupplier<? extends A, X> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return () -> Objects.requireNonNull(supplier.get(), "supplier result");
    }

    static <A> VIO<A> pure(A value) {
        Objects.requireNonNull(value, "value");
        return () -> value;
    }

    static <A> VIO<A> failed(Throwable error) {
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

    static VIO<Unit> exec(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return () -> {
            runnable.run();
            return Unit.INSTANCE;
        };
    }

    static VIO<Unit> fromRunnable(Runnable runnable) {
        return exec(runnable);
    }

    static VIO<Unit> unit() {
        return pure(Unit.INSTANCE);
    }

    static <R, A> VIO<A> bracket(
            VIO<R> acquire,
            Function<? super R, ? extends VIO<A>> use,
            Function<? super R, VIO<Unit>> release) {
        return VIOResource.of(acquire, release).use(resource -> Objects.requireNonNull(use.apply(resource), "use result"));
    }

    static <A> VIO<A> unbox(App<Mu, A> value) {
        return (VIO<A>) Objects.requireNonNull(value, "value");
    }

    static Applicative<VIO.Mu, VIOMonad.Mu> applicative() {
        return VIOMonad.INSTANCE;
    }

    static Monad<VIO.Mu, VIOMonad.Mu> monad() {
        return VIOMonad.INSTANCE;
    }

    static MonadError<VIO.Mu, Throwable, VIOMonad.Mu> monadError() {
        return VIOMonad.INSTANCE;
    }

    static Selective<VIO.Mu, VIOMonad.Mu> selective() {
        return VIOMonad.INSTANCE;
    }

    default <B> VIO<B> map(Function<? super A, ? extends B> f) {
        Objects.requireNonNull(f, "f");
        return () -> Objects.requireNonNull(f.apply(unsafeRun()), "map result");
    }

    default <B> VIO<B> flatMap(Function<? super A, ? extends VIO<B>> f) {
        Objects.requireNonNull(f, "f");
        return new FlatMappedVIO<>(this, f);
    }

    default <B> VIO<B> then(Supplier<VIO<B>> next) {
        Objects.requireNonNull(next, "next");
        return flatMap(ignored -> Objects.requireNonNull(next.get(), "next VIO"));
    }

    default VIO<A> peek(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action");
        return map(value -> {
            action.accept(value);
            return value;
        });
    }

    default VIO<A> recover(Function<? super Throwable, ? extends A> f) {
        Objects.requireNonNull(f, "f");
        return () -> {
            try {
                return unsafeRun();
            } catch (Throwable error) {
                return Objects.requireNonNull(f.apply(error), "recover result");
            }
        };
    }

    default VIO<A> recoverWith(Function<? super Throwable, VIO<A>> f) {
        Objects.requireNonNull(f, "f");
        return () -> {
            try {
                return unsafeRun();
            } catch (Throwable error) {
                return Objects.requireNonNull(f.apply(error), "recoverWith result").unsafeRun();
            }
        };
    }

    default VIO<A> mapError(Function<? super Throwable, ? extends Throwable> f) {
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

    default VIO<Either<Throwable, A>> attempt() {
        return () -> {
            try {
                return Either.right(unsafeRun());
            } catch (Throwable error) {
                return Either.left(error);
            }
        };
    }

    default VIO<Unit> voided() {
        return map(ignored -> Unit.INSTANCE);
    }

    default VIO<Unit> asUnit() {
        return voided();
    }

    default Task<A> toTask() {
        return Task.delay(this::unsafeRun);
    }

    default VIO<A> guarantee(VIO<Unit> finalizer) {
        Objects.requireNonNull(finalizer, "finalizer");
        return () -> {
            Throwable primary = null;
            try {
                return unsafeRun();
            } catch (Throwable error) {
                primary = error;
                if (error instanceof Exception exception) {
                    throw exception;
                }
                if (error instanceof Error fatal) {
                    throw fatal;
                }
                throw new RuntimeException(error);
            } finally {
                try {
                    finalizer.unsafeRun();
                } catch (Throwable finalizerError) {
                    if (primary != null) {
                        primary.addSuppressed(finalizerError);
                    } else if (finalizerError instanceof Exception exception) {
                        throw exception;
                    } else if (finalizerError instanceof Error fatal) {
                        throw fatal;
                    } else {
                        throw new RuntimeException(finalizerError);
                    }
                }
            }
        };
    }

    default VIOResource<A> asResource(Function<? super A, VIO<Unit>> release) {
        return VIOResource.of(this, release);
    }

    default Task<A> toTask(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        return Task.async(() -> toTask().unsafeRunAsync(executor));
    }

    default VIO<A> retry(RetryPolicy policy) {
        return Retry.retryVIO(this, policy);
    }

    default VIO<A> circuitBreaker(CircuitBreaker circuitBreaker) {
        return circuitBreaker.protect(this);
    }

    default VIO<A> bulkhead(Bulkhead bulkhead) {
        return bulkhead.protect(this);
    }

    final class FlatMappedVIO<A, B> implements VIO<B> {
        private final VIO<A> source;
        private final Function<? super A, ? extends VIO<B>> f;

        FlatMappedVIO(VIO<A> source, Function<? super A, ? extends VIO<B>> f) {
            this.source = Objects.requireNonNull(source, "source");
            this.f = Objects.requireNonNull(f, "f");
        }

        @Override
        @SuppressWarnings("unchecked")
        public B unsafeRun() throws Exception {
            VIO<?> current = this;
            Deque<Function<Object, VIO<?>>> continuations = new ArrayDeque<>();

            while (true) {
                if (current instanceof FlatMappedVIO<?, ?> fm) {
                    continuations.push((Function<Object, VIO<?>>) fm.f);
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

    enum VIOMonad implements MonadError<VIO.Mu, Throwable, VIOMonad.Mu>,
            Selective<VIO.Mu, VIOMonad.Mu> {
        INSTANCE;

        public static final class Mu implements MonadError.Mu {
            private Mu() {
            }
        }

        @Override
        public <A> App<VIO.Mu, A> of(A value) {
            return VIO.pure(value);
        }

        @Override
        public <A, B> App<VIO.Mu, B> flatMap(
                Function<? super A, ? extends App<VIO.Mu, B>> f,
                App<VIO.Mu, A> fa) {
            Objects.requireNonNull(f, "f");
            return VIO.unbox(fa).flatMap(value -> VIO.unbox(Objects.requireNonNull(f.apply(value), "flatMap result")));
        }

        @Override
        public <A> App<VIO.Mu, A> raiseError(Throwable error) {
            return VIO.failed(error);
        }

        @Override
        public <A> App<VIO.Mu, A> handleErrorWith(
                App<VIO.Mu, A> value,
                Function<? super Throwable, ? extends App<VIO.Mu, A>> handler) {
            Objects.requireNonNull(handler, "handler");
            return VIO.unbox(value).recoverWith(error ->
                    VIO.unbox(Objects.requireNonNull(handler.apply(error), "handler result")));
        }

        @Override
        public <A, B> App<VIO.Mu, B> select(
                App<VIO.Mu, Either<A, B>> value,
                App<VIO.Mu, ? extends Function<A, B>> function) {
            return VIO.unbox(value).flatMap(inner -> {
                Either<A, B> either = Objects.requireNonNull(inner, "select value");
                if (either.isRight()) {
                    return VIO.pure(either.right());
                }
                return VIO.unbox(function)
                        .map(fn -> Objects.requireNonNull(fn, "select function").apply(either.left()));
            });
        }

        @Override
        public <A> App<VIO.Mu, A> ifS(
                App<VIO.Mu, Boolean> condition,
                Supplier<? extends App<VIO.Mu, A>> thenValue,
                Supplier<? extends App<VIO.Mu, A>> elseValue) {
            Objects.requireNonNull(thenValue, "thenValue");
            Objects.requireNonNull(elseValue, "elseValue");
            return VIO.unbox(condition).flatMap(test -> {
                Supplier<? extends App<VIO.Mu, A>> branch = Boolean.TRUE.equals(test) ? thenValue : elseValue;
                return VIO.unbox(Objects.requireNonNull(branch.get(), "ifS branch result"));
            });
        }
    }
}
