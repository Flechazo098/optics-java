package com.flechazo.hkt.type;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.TypedOptic;

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
        public <FT, FR> Maybe<TypedOptic<Either<L, R>, ?, FT, FR>> findTypeInChildren(
                Type<FT> type,
                Type<FR> resultType,
                TypeMatcher<FT, FR> matcher,
                boolean recurse) {
            Maybe<TypedOptic<L, ?, FT, FR>> leftOptic = left.findType(type, resultType, matcher, recurse);
            if (leftOptic.isDefined()) {
                TypedOptic<Either<L, R>, ?, L, ?> outer =
                        castOptic(TypedOptic.inj1(left, right, leftOptic.get().tType()));
                return Maybe.some(composeOptics(outer, leftOptic.get()));
            }
            Maybe<TypedOptic<R, ?, FT, FR>> rightOptic = right.findType(type, resultType, matcher, recurse);
            if (rightOptic.isDefined()) {
                TypedOptic<Either<L, R>, ?, R, ?> outer =
                        castOptic(TypedOptic.inj2(left, right, rightOptic.get().tType()));
                return Maybe.some(composeOptics(outer, rightOptic.get()));
            }
            return Maybe.none();
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
