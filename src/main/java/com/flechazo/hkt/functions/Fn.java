package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Function;

public record Fn<A, B>(
        String name,
        Function<? super A, ? extends B> function,
        Type<A> argumentType,
        Type<B> resultType)
        implements PointFree<Function<A, B>> {
    public Fn {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(argumentType, "argumentType");
        Objects.requireNonNull(resultType, "resultType");
    }

    @Override
    public Function<A, B> eval() {
        return function::apply;
    }

    @Override
    public Type<Function<A, B>> type() {
        return Types.function(argumentType, resultType);
    }

    @Override
    @NonNull
    public String toString() {
        return name;
    }
}
