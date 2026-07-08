package com.flechazo.hkt.business.resilience;

public class BulkheadFullException extends RuntimeException {
    private final int maxConcurrent;
    private final int currentWaiting;

    public BulkheadFullException(int maxConcurrent, int currentWaiting) {
        super("Bulkhead full: maxConcurrent=" + maxConcurrent + ", waiting=" + currentWaiting);
        this.maxConcurrent = maxConcurrent;
        this.currentWaiting = currentWaiting;
    }

    public int maxConcurrent() {
        return maxConcurrent;
    }

    public int currentWaiting() {
        return currentWaiting;
    }
}
