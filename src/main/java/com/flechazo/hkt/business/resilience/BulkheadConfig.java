package com.flechazo.hkt.business.resilience;

import java.time.Duration;
import java.util.Objects;

public record BulkheadConfig(int maxConcurrent, int maxWait, Duration waitTimeout, boolean fairness) {
    public static final int DEFAULT_MAX_CONCURRENT = 10;
    public static final int DEFAULT_MAX_WAIT = 0;
    public static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(5);

    public BulkheadConfig {
        if (maxConcurrent < 1) {
            throw new IllegalArgumentException("maxConcurrent must be at least 1");
        }
        if (maxWait < 0) {
            throw new IllegalArgumentException("maxWait must not be negative");
        }
        Objects.requireNonNull(waitTimeout, "waitTimeout");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int maxConcurrent = DEFAULT_MAX_CONCURRENT;
        private int maxWait = DEFAULT_MAX_WAIT;
        private Duration waitTimeout = DEFAULT_WAIT_TIMEOUT;
        private boolean fairness;

        private Builder() {
        }

        public Builder maxConcurrent(int value) {
            maxConcurrent = value;
            return this;
        }

        public Builder maxWait(int value) {
            maxWait = value;
            return this;
        }

        public Builder waitTimeout(Duration value) {
            waitTimeout = Objects.requireNonNull(value, "waitTimeout");
            return this;
        }

        public Builder fairness(boolean value) {
            fairness = value;
            return this;
        }

        public BulkheadConfig build() {
            return new BulkheadConfig(maxConcurrent, maxWait, waitTimeout, fairness);
        }
    }
}
