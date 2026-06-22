package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.type.TypeExpr;
import com.flechazo.hkt.type.TypeRef;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record PointFreeOpticTypes(TypeExpr source, TypeExpr target, TypeExpr focus, TypeExpr replacement) {
    public PointFreeOpticTypes(
            TypeRef<?> sourceType,
            TypeRef<?> targetType,
            TypeRef<?> focusType,
            TypeRef<?> replacementType) {
        this(
                Objects.requireNonNull(sourceType, "sourceType").expr(),
                Objects.requireNonNull(targetType, "targetType").expr(),
                Objects.requireNonNull(focusType, "focusType").expr(),
                Objects.requireNonNull(replacementType, "replacementType").expr());
    }

    public PointFreeOpticTypes(
            TypeExpr source,
            TypeExpr target,
            TypeExpr focus,
            TypeExpr replacement) {
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
        this.focus = Objects.requireNonNull(focus, "focus");
        this.replacement = Objects.requireNonNull(replacement, "replacement");
    }

    public static PointFreeOpticTypes endomorphic(TypeRef<?> sourceType, TypeRef<?> focusType) {
        return endomorphic(sourceType.expr(), focusType.expr());
    }

    public static PointFreeOpticTypes endomorphic(TypeExpr source, TypeExpr focus) {
        return new PointFreeOpticTypes(source, source, focus, focus);
    }

    public TypeRef<?> sourceType() {
        return witness("source", source);
    }

    public TypeRef<?> targetType() {
        return witness("target", target);
    }

    public TypeRef<?> focusType() {
        return witness("focus", focus);
    }

    public TypeRef<?> replacementType() {
        return witness("replacement", replacement);
    }

    public PointFreeOpticTypes compose(PointFreeOpticTypes inner) {
        Objects.requireNonNull(inner, "inner");
        return new PointFreeOpticTypes(source, target, inner.focus, inner.replacement);
    }

    public PointFreeOpticTypes castOuter(TypeRef<?> sourceType, TypeRef<?> targetType) {
        return castOuter(sourceType.expr(), targetType.expr());
    }

    public PointFreeOpticTypes castOuter(TypeExpr source, TypeExpr target) {
        return new PointFreeOpticTypes(source, target, focus, replacement);
    }

    private static TypeRef<?> witness(String role, TypeExpr type) {
        Maybe<TypeRef<?>> witness = type.witness();
        if (witness.isEmpty()) {
            throw new IllegalStateException(role + " type has no runtime witness: " + type);
        }
        return witness.get();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PointFreeOpticTypes(
                TypeExpr source1, TypeExpr target1, TypeExpr focus1, TypeExpr replacement1
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
