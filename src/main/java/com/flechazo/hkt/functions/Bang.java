package com.flechazo.hkt.functions;

import com.flechazo.hkt.Unit;
import org.jspecify.annotations.NonNull;

import java.util.function.Function;

public record Bang<A>() implements PointFree<Function<A, Unit>> {
    @Override
    public Function<A, Unit> eval() {
        return ignored -> Unit.INSTANCE;
    }

    @Override
    @NonNull
    public String toString() {
        return "!";
    }
}
