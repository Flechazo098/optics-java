package com.flechazo.hkt.functions;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record Comp<A, B>(List<PointFree<? extends Function<?, ?>>> functions)
        implements PointFree<Function<A, B>> {
    public Comp {
        Objects.requireNonNull(functions, "functions");
        if (functions.isEmpty()) {
            throw new IllegalArgumentException("functions must not be empty");
        }
        functions = List.copyOf(functions);
    }

    @Override
    public Function<A, B> eval() {
        return input -> {
            Object value = input;
            for (int i = functions.size() - 1; i >= 0; i--) {
                value = applyUnchecked(functions.get(i).eval(), value);
            }
            return cast(value);
        };
    }

    @Override
    @NonNull
    public String toString() {
        return "comp" + functions;
    }

    @SuppressWarnings("unchecked")
    private static <A, B> B applyUnchecked(Function<A, B> function, Object input) {
        return function.apply((A) input);
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }
}
