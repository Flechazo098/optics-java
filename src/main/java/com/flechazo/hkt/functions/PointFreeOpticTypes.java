package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.type.TypeExpr;
import com.flechazo.hkt.type.TypeRef;

import java.util.Objects;

public final class PointFreeOpticTypes {
    private final TypeExpr source;
    private final TypeExpr target;
    private final TypeExpr focus;
    private final TypeExpr replacement;

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

    public TypeExpr source() {
        return source;
    }

    public TypeExpr target() {
        return target;
    }

    public TypeExpr focus() {
        return focus;
    }

    public TypeExpr replacement() {
        return replacement;
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
        return obj instanceof PointFreeOpticTypes that
                && source.equals(that.source)
                && target.equals(that.target)
                && focus.equals(that.focus)
                && replacement.equals(that.replacement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, focus, replacement);
    }

    @Override
    public String toString() {
        return "PointFreeOpticTypes["
                + source + " -> " + target
                + ", " + focus + " -> " + replacement
                + "]";
    }
}
