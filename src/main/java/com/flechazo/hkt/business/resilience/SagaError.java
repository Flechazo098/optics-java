package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Unit;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record SagaError(Throwable originalError, String failedStep, List<CompensationResult> compensationResults) {
    public SagaError {
        Objects.requireNonNull(originalError, "originalError");
        Objects.requireNonNull(failedStep, "failedStep");
        compensationResults = Collections.unmodifiableList(
                Objects.requireNonNull(compensationResults, "compensationResults"));
    }

    public record CompensationResult(String stepName, Either<Throwable, Unit> result) {
        public CompensationResult {
            Objects.requireNonNull(stepName, "stepName");
            Objects.requireNonNull(result, "result");
        }
    }

    public boolean allCompensationsSucceeded() {
        return compensationResults.stream().allMatch(result -> result.result().isRight());
    }

    public List<Throwable> compensationFailures() {
        return compensationResults.stream()
                .filter(result -> result.result().isLeft())
                .map(result -> result.result().left())
                .toList();
    }
}
