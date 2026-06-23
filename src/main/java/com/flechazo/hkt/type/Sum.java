package com.flechazo.hkt.type;

import com.flechazo.hkt.Either;

import java.util.Objects;

public record Sum(TypeTemplate left, TypeTemplate right) implements TypeTemplate {
    public Sum {
        Types.requireTemplate(left, "left");
        Types.requireTemplate(right, "right");
    }

    @Override
    public int size() {
        return Math.max(left.size(), right.size());
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return index -> Types.or(left.apply(family).apply(index), right.apply(family).apply(index));
    }

    public static final class SumType<L, R> extends Type<Either<L, R>> {
        private final Type<L> left;
        private final Type<R> right;

        public SumType(Type<L> left, Type<R> right) {
            this.left = Objects.requireNonNull(left, "left");
            this.right = Objects.requireNonNull(right, "right");
        }

        public Type<L> left() {
            return left;
        }

        public Type<R> right() {
            return right;
        }

        @Override
        public TypeTemplate template() {
            return Types.or(left.template(), right.template());
        }

        @Override
        public boolean containsVariable(String name) {
            return left.containsVariable(name) || right.containsVariable(name);
        }

        @Override
        public boolean equals(Type<?> other, boolean ignoreRecursionPoints, boolean checkIndex) {
            return other instanceof SumType<?, ?> that
                    && left.equals(that.left, ignoreRecursionPoints, checkIndex)
                    && right.equals(that.right, ignoreRecursionPoints, checkIndex);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof SumType<?, ?> that && left.equals(that.left) && right.equals(that.right);
        }

        @Override
        public int hashCode() {
            return 31 * left.hashCode() + right.hashCode();
        }

        @Override
        public String toString() {
            return "(" + left + " | " + right + ")";
        }
    }
}
