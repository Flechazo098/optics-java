package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.TypedOptic;

import java.util.Objects;
import java.util.function.Supplier;

public record RecursivePoint(int index) implements TypeTemplate {
    public RecursivePoint {
        if (index < 0) {
            throw new IllegalArgumentException("index must be non-negative");
        }
    }

    @Override
    public String toString() {
        return "Id[" + index + "]";
    }

    @Override
    public int size() {
        return index + 1;
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        Type<?> result = family.apply(index);
        return ignored -> result;
    }

    public static final class RecursivePointType<A> extends Type<A> {
        private final RecursiveTypeFamily family;
        private final int index;
        private final Supplier<Type<A>> delegate;
        private volatile Type<A> type;

        public RecursivePointType(RecursiveTypeFamily family, int index, Supplier<Type<A>> delegate) {
            this.family = Objects.requireNonNull(family, "family");
            family.checkIndex(index);
            this.index = index;
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        public RecursiveTypeFamily family() {
            return family;
        }

        public int index() {
            return index;
        }

        public Type<A> unfold() {
            Type<A> result = type;
            if (result == null) {
                result = delegate.get();
                type = result;
            }
            return result;
        }

        @Override
        public TypeTemplate template() {
            return Types.id(index);
        }

        @Override
        public Type<?> updateMu(RecursiveTypeFamily newFamily) {
            return newFamily.apply(index);
        }

        @Override
        public <FT, FR> Maybe<TypedOptic<A, ?, FT, FR>> findTypeInChildren(
                Type<FT> type,
                Type<FR> resultType,
                TypeMatcher<FT, FR> matcher,
                boolean recurse) {
            return castMaybe(family.findType(index, type, resultType, matcher, recurse));
        }

        @Override
        public boolean equals(Type<?> other, boolean ignoreRecursionPoints, boolean checkIndex) {
            if (!(other instanceof RecursivePointType<?> that)) {
                return false;
            }
            if (ignoreRecursionPoints) {
                return !checkIndex || index == that.index;
            }
            return family.equals(that.family) && index == that.index;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof RecursivePointType<?> that && family.equals(that.family) && index == that.index;
        }

        @Override
        public int hashCode() {
            return 31 * family.hashCode() + index;
        }

        @Override
        public String toString() {
            return "MuType[" + family.name() + "_" + index + "]";
        }
    }
}
