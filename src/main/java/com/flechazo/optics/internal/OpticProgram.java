package com.flechazo.optics.internal;

import java.util.Objects;

public sealed interface OpticProgram<S, T, A, B>
        permits OpticProgram.Opaque, OpticProgram.Structured, OpticProgram.Compose {
    String kind();

    record Opaque<S, T, A, B>(String kind, Object key) implements OpticProgram<S, T, A, B> {
        public Opaque {
            Objects.requireNonNull(kind, "kind");
        }
    }

    record Structured<S, T, A, B>(String kind, Object key) implements OpticProgram<S, T, A, B> {
        public Structured {
            Objects.requireNonNull(kind, "kind");
        }
    }

    record Compose<S, T, A, B, C, D>(
            OpticProgram<S, T, A, B> left,
            OpticProgram<A, B, C, D> right)
            implements OpticProgram<S, T, C, D> {
        public Compose {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
        }

        @Override
        public String kind() {
            return "compose";
        }
    }
}
