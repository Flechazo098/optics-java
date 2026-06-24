package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Type;

import java.util.Objects;

public final class RewriteResult<A> {
    private static final RewriteResult<?> UNCHANGED = new RewriteResult<>(null, null, false);

    private final PointFree<A> expression;
    private final Type<A> type;
    private final boolean changed;

    private RewriteResult(PointFree<A> expression, Type<A> type, boolean changed) {
        if (changed) {
            Objects.requireNonNull(expression, "expression");
            Objects.requireNonNull(type, "type");
        }
        this.expression = expression;
        this.type = type;
        this.changed = changed;
    }

    public static <A> RewriteResult<A> unchanged(PointFree<A> expression) {
        return unchanged();
    }

    @SuppressWarnings("unchecked")
    public static <A> RewriteResult<A> unchanged() {
        return (RewriteResult<A>) UNCHANGED;
    }

    public static <A> RewriteResult<A> changed(PointFree<A> expression) {
        return new RewriteResult<>(expression, expression.type(), true);
    }

    public static <A> RewriteResult<A> changed(PointFree<A> expression, Type<A> type) {
        return new RewriteResult<>(expression, type, true);
    }

    public PointFree<A> expression() {
        if (!changed) {
            throw new IllegalStateException("unchanged rewrite result has no replacement expression");
        }
        return expression;
    }

    public Type<A> type() {
        if (!changed) {
            throw new IllegalStateException("unchanged rewrite result has no replacement type");
        }
        return type;
    }

    public boolean changed() {
        return changed;
    }
}
