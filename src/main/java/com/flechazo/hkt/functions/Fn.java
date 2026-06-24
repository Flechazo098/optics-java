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
        Type<B> resultType,
        Type<Function<A, B>> type)
        implements PointFree<Function<A, B>> {
    public Fn(
            String name,
            Function<? super A, ? extends B> function,
            Type<A> argumentType,
            Type<B> resultType) {
        this(name, function, argumentType, resultType, Types.function(argumentType, resultType));
    }

    public Fn {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(argumentType, "argumentType");
        Objects.requireNonNull(resultType, "resultType");
        Objects.requireNonNull(type, "type");
    }

    @Override
    public Function<A, B> eval() {
        return function::apply;
    }

    @Override
    @NonNull
    public String toString() {
        return name;
    }
}
