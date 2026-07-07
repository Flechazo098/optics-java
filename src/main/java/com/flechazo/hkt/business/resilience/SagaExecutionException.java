package com.flechazo.hkt.business.resilience;

public final class SagaExecutionException extends RuntimeException {
    private final SagaError error;

    public SagaExecutionException(SagaError error) {
        super("Saga failed at " + error.failedStep(), error.originalError());
        this.error = error;
    }

    public SagaError error() {
        return error;
    }
}
