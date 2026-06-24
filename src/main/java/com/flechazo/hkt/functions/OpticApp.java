package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record OpticApp<S, T, A, B>(
        PointFreeOptic<S, T, A, B> optic,
        PointFree<? extends Function<?, ?>> function,
        Type<Function<S, T>> type)
        implements PointFree<Function<S, T>> {
    public OpticApp(PointFreeOptic<S, T, A, B> optic, PointFree<? extends Function<?, ?>> function) {
        this(optic, function, PointFreeTypes.opticAppType(optic, function));
    }

    public OpticApp {
        Objects.requireNonNull(optic, "optic");
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(type, "type");
    }

    @Override
    public Function<S, T> eval() {
        Function<A, B> modifier = cast(function.eval());
        return source -> optic.modify(modifier, source);
    }

    @Override
    @NonNull
    public String toString() {
        return "(ap " + optic + " " + function + ")";
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }
}
