package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.VTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Saga<A> {
    @FunctionalInterface
    interface SagaRunner<A> {
        A execute(List<CompletedStep<?>> completed) throws Throwable;
    }

    record CompletedStep<A>(SagaStep<A> step, A result) {
    }

    private final SagaRunner<A> runner;

    Saga(SagaRunner<A> runner) {
        this.runner = Objects.requireNonNull(runner, "runner");
    }

    public static <A> Saga<A> of(VTask<A> action, Consumer<? super A> compensate) {
        return singleStep(SagaStep.of("step-1", action, compensate));
    }

    public static <A> Saga<A> of(VTask<A> action, Function<? super A, VTask<Unit>> compensate) {
        return singleStep(SagaStep.ofAsync("step-1", action, compensate));
    }

    public static <A> Saga<A> noCompensation(VTask<A> action) {
        return singleStep(SagaStep.ofAsync("step-1", action, ignored -> VTask.unit()));
    }

    static <A> Saga<A> namedStep(String name, VTask<A> action, Function<? super A, VTask<Unit>> compensate) {
        return singleStep(SagaStep.ofAsync(name, action, compensate));
    }

    static <A> Saga<A> namedStepSync(String name, VTask<A> action, Consumer<? super A> compensate) {
        return singleStep(SagaStep.of(name, action, compensate));
    }

    public <B> Saga<B> map(Function<? super A, ? extends B> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new Saga<>(completed -> mapper.apply(runner.execute(completed)));
    }

    public <B> Saga<B> flatMap(Function<? super A, Saga<B>> mapper) {
        return andThen(mapper);
    }

    public <B> Saga<B> andThen(Function<? super A, Saga<B>> next) {
        Objects.requireNonNull(next, "next");
        return new Saga<>(completed -> {
            A result = runner.execute(completed);
            return Objects.requireNonNull(next.apply(result), "next saga").runner.execute(completed);
        });
    }

    public VTask<A> run() {
        return () -> {
            ArrayList<CompletedStep<?>> completed = new ArrayList<>();
            try {
                return runner.execute(completed);
            } catch (Throwable error) {
                SagaError sagaError = buildError(completed, error);
                if (sagaError.allCompensationsSucceeded()) {
                    throw sagaError.originalError();
                }
                throw new SagaExecutionException(sagaError);
            }
        };
    }

    public VTask<Either<SagaError, A>> runSafe() {
        return () -> {
            ArrayList<CompletedStep<?>> completed = new ArrayList<>();
            try {
                return Either.right(runner.execute(completed));
            } catch (Throwable error) {
                return Either.left(buildError(completed, error));
            }
        };
    }

    private SagaError buildError(List<CompletedStep<?>> completed, Throwable error) {
        Throwable original = error instanceof SagaStepFailure failure ? failure.originalError : error;
        String failedStep = error instanceof SagaStepFailure failure ? failure.stepName : "step-" + (completed.size() + 1);
        return compensate(completed, original, failedStep);
    }

    @SuppressWarnings("unchecked")
    private static <A> Saga<A> singleStep(SagaStep<A> step) {
        Objects.requireNonNull(step, "step");
        return new Saga<>(completed -> {
            try {
                A result = step.action().execute();
                completed.add(new CompletedStep<>((SagaStep<Object>) step, result));
                return result;
            } catch (Throwable error) {
                throw new SagaStepFailure(step.name(), error);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static SagaError compensate(List<CompletedStep<?>> completed, Throwable originalError, String failedStep) {
        ArrayList<SagaError.CompensationResult> results = new ArrayList<>();
        ArrayList<CompletedStep<?>> reversed = new ArrayList<>(completed);
        Collections.reverse(reversed);
        for (CompletedStep<?> completedStep : reversed) {
            CompletedStep<Object> typed = (CompletedStep<Object>) completedStep;
            try {
                typed.step().compensate().apply(typed.result()).execute();
                results.add(new SagaError.CompensationResult(typed.step().name(), Either.right(Unit.INSTANCE)));
            } catch (Throwable compensationError) {
                results.add(new SagaError.CompensationResult(typed.step().name(), Either.left(compensationError)));
            }
        }
        return new SagaError(originalError, failedStep, Collections.unmodifiableList(results));
    }

    private static final class SagaStepFailure extends RuntimeException {
        private final String stepName;
        private final Throwable originalError;

        private SagaStepFailure(String stepName, Throwable cause) {
            super(cause);
            this.stepName = stepName;
            this.originalError = cause;
        }
    }
}
