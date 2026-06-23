package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record PointFreeOpticTypes<S, T, A, B>(Type<S> source, Type<T> target, Type<A> focus, Type<B> replacement) {
    public PointFreeOpticTypes(
            Type<S> source,
            Type<T> target,
            Type<A> focus,
            Type<B> replacement) {
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
        this.focus = Objects.requireNonNull(focus, "focus");
        this.replacement = Objects.requireNonNull(replacement, "replacement");
    }

    public static <S, A> PointFreeOpticTypes<S, S, A, A> endomorphic(Type<S> source, Type<A> focus) {
        return new PointFreeOpticTypes<>(source, source, focus, focus);
    }

    public Type<S> sourceType() {
        return source;
    }

    public Type<T> targetType() {
        return target;
    }

    public Type<A> focusType() {
        return focus;
    }

    public Type<B> replacementType() {
        return replacement;
    }

    public <A1, B1> PointFreeOpticTypes<S, T, A1, B1> compose(PointFreeOpticTypes<A, B, A1, B1> inner) {
        Objects.requireNonNull(inner, "inner");
        return new PointFreeOpticTypes<>(source, target, inner.focus, inner.replacement);
    }

    public <S2, T2> PointFreeOpticTypes<S2, T2, A, B> castOuter(Type<S2> source, Type<T2> target) {
        return new PointFreeOpticTypes<>(source, target, focus, replacement);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PointFreeOpticTypes(
                Type<?> source1, Type<?> target1, Type<?> focus1, Type<?> replacement1
        )
                && source.equals(source1)
                && target.equals(target1)
                && focus.equals(focus1)
                && replacement.equals(replacement1);
    }

    @Override
    @NonNull
    public String toString() {
        return "PointFreeOpticTypes["
                + source + " -> " + target
                + ", " + focus + " -> " + replacement
                + "]";
    }
}
