package com.flechazo.hkt.business.resilience;

import com.flechazo.hkt.business.effect.Task;
import com.flechazo.hkt.business.effect.VIO;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class Bulkhead {
    private final BulkheadConfig config;
    private final Semaphore semaphore;
    private final AtomicInteger waiting = new AtomicInteger();

    private Bulkhead(BulkheadConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.semaphore = new Semaphore(config.maxConcurrent(), config.fairness());
    }

    public static Bulkhead create(BulkheadConfig config) {
        return new Bulkhead(config);
    }

    public static Bulkhead withMaxConcurrent(int maxConcurrent) {
        return new Bulkhead(BulkheadConfig.builder().maxConcurrent(maxConcurrent).build());
    }

    public <A> Task<A> protect(Task<A> task) {
        return protectWithTimeout(task, config.waitTimeout());
    }

    public <A> Task<A> protectWithTimeout(Task<A> task, Duration waitTimeout) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(waitTimeout, "waitTimeout");
        return () -> {
            boolean acquired;
            if (config.maxWait() == 0) {
                acquired = semaphore.tryAcquire();
            } else {
                enterWaitQueue();
                try {
                    acquired = semaphore.tryAcquire(waitTimeout.toMillis(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new BulkheadFullException(config.maxConcurrent(), Math.max(waiting.get() - 1, 0));
                } finally {
                    waiting.decrementAndGet();
                }
            }
            if (!acquired) {
                throw new BulkheadFullException(config.maxConcurrent(), waiting.get());
            }
            try {
                return task.execute();
            } finally {
                semaphore.release();
            }
        };
    }

    public <A> VIO<A> protect(VIO<A> vio) {
        return () -> {
            try {
                return protect(vio.toTask()).execute();
            } catch (Exception exception) {
                throw exception;
            } catch (Throwable throwable) {
                if (throwable instanceof Error error) {
                    throw error;
                }
                throw new RuntimeException(throwable);
            }
        };
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }

    public int activeCount() {
        return config.maxConcurrent() - semaphore.availablePermits();
    }

    public int waitingCount() {
        return waiting.get();
    }

    private void enterWaitQueue() {
        int observed = waiting.getAndUpdate(current -> current >= config.maxWait() ? current : current + 1);
        if (observed >= config.maxWait()) {
            throw new BulkheadFullException(config.maxConcurrent(), observed);
        }
    }
}
