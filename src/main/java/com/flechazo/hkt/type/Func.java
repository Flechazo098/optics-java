package com.flechazo.hkt.type;

import java.util.Objects;
import java.util.function.Function;

public final class Func<A, B> extends Type<Function<A, B>> {
    private final Type<A> input;
    private final Type<B> output;

    public Func(Type<A> input, Type<B> output) {
        this.input = Objects.requireNonNull(input, "input");
        this.output = Objects.requireNonNull(output, "output");
    }

    public Type<A> input() {
        return input;
    }

    public Type<B> output() {
        return output;
    }

    @Override
    public TypeTemplate template() {
        return Types.constType(this);
    }

    @Override
    public boolean containsVariable(String name) {
        return input.containsVariable(name) || output.containsVariable(name);
    }

    @Override
    public boolean equals(Type<?> other, boolean ignoreRecursionPoints, boolean checkIndex) {
        return other instanceof Func<?, ?> that
                && input.equals(that.input, ignoreRecursionPoints, checkIndex)
                && output.equals(that.output, ignoreRecursionPoints, checkIndex);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Func<?, ?> that && input.equals(that.input) && output.equals(that.output);
    }

    @Override
    public int hashCode() {
        return 31 * input.hashCode() + output.hashCode();
    }

    @Override
    public String toString() {
        return "(" + input + " -> " + output + ")";
    }
}
