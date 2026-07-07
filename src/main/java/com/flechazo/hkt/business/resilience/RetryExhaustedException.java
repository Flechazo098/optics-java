package com.flechazo.hkt.business.resilience;

public final class RetryExhaustedException extends RuntimeException {
    private final int attempts;

    public RetryExhaustedException(String message, Throwable cause, int attempts) {
        super(message, cause);
        this.attempts = attempts;
    }

    public int attempts() {
        return attempts;
    }

    public int getAttempts() {
        return attempts;
    }

    public static RetryExhaustedException of(Throwable cause, int attempts) {
        return new RetryExhaustedException("Retry exhausted after " + attempts + " attempts", cause, attempts);
    }
}
