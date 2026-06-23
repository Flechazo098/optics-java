package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Func;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public record Comp<A, B>(
        List<PointFree<? extends Function<?, ?>>> functions,
        Type<Function<A, B>> type)
        implements PointFree<Function<A, B>> {
    public Comp(List<PointFree<? extends Function<?, ?>>> functions) {
        this(functions, compositionType(functions));
    }

    public Comp {
        Objects.requireNonNull(functions, "functions");
        Objects.requireNonNull(type, "type");
        if (functions.isEmpty()) {
            throw new IllegalArgumentException("functions must not be empty");
        }
    }

    @Override
    public Function<A, B> eval() {
        return input -> {
            Object value = input;
            for (int i = functions.size() - 1; i >= 0; i--) {
                value = applyUnchecked(functions.get(i).eval(), value);
            }
            return (B) value;
        };
    }

    @SuppressWarnings("unchecked")
    private static <A, B> B applyUnchecked(Function<A, B> function, Object input) {
        return function.apply((A) input);
    }

    @Override
    public Type<Function<A, B>> type() {
        return type;
    }

    @Override
    @NonNull
    public String toString() {
        return functions.stream().map(Object::toString).collect(Collectors.joining(" \u25E6 "));
    }

    private static <A, B> Type<Function<A, B>> compositionType(
            List<PointFree<? extends Function<?, ?>>> functions) {
        Objects.requireNonNull(functions, "functions");
        if (functions.isEmpty()) {
            throw new IllegalArgumentException("functions must not be empty");
        }
        Func<?, ?> current = PointFreeTypes.functionType(functions.getLast());
        for (int i = functions.size() - 2; i >= 0; i--) {
            Func<?, ?> inner = current;
            Func<?, ?> outer = PointFreeTypes.functionType(functions.get(i));
            if (!PointFreeTypes.compatible(inner.output(), outer.input())) {
                throw new IllegalArgumentException("composition type mismatch: inner output "
                        + inner.output() + ", outer input " + outer.input());
            }
            current = Types.function(inner.input(), outer.output());
        }
        return (Type<Function<A,B>>) (Type<?>) current;
    }
}
