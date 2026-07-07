package com.flechazo.hkt.type;

import com.flechazo.hkt.Tuple2;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.TypedOptic;

import java.util.Objects;

public record Product(TypeTemplate first, TypeTemplate second) implements TypeTemplate {
    public Product {
        Types.requireTemplate(first, "first");
        Types.requireTemplate(second, "second");
    }

    @Override
    public int size() {
        return Math.max(first.size(), second.size());
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return index -> Types.and(first.apply(family).apply(index), second.apply(family).apply(index));
    }

    public static final class ProductType<F, G> extends Type<Tuple2<F, G>> {
        private final Type<F> first;
        private final Type<G> second;

        public ProductType(Type<F> first, Type<G> second) {
            this.first = Objects.requireNonNull(first, "first");
            this.second = Objects.requireNonNull(second, "second");
        }

        public Type<F> first() {
            return first;
        }

        public Type<G> second() {
            return second;
        }

        @Override
        public TypeTemplate template() {
            return Types.and(first.template(), second.template());
        }

        @Override
        public boolean containsVariable(String name) {
            return first.containsVariable(name) || second.containsVariable(name);
        }

        @Override
        public <FT, FR> Maybe<TypedOptic<Tuple2<F, G>, ?, FT, FR>> findTypeInChildren(
                Type<FT> type,
                Type<FR> resultType,
                TypeMatcher<FT, FR> matcher,
                boolean recurse) {
            Maybe<TypedOptic<F, ?, FT, FR>> firstOptic = first.findType(type, resultType, matcher, recurse);
            if (firstOptic.isDefined()) {
                TypedOptic<Tuple2<F, G>, ?, F, ?> outer =
                        castOptic(TypedOptic.proj1(first, second, firstOptic.get().tType()));
                return Maybe.some(composeOptics(outer, firstOptic.get()));
            }
            Maybe<TypedOptic<G, ?, FT, FR>> secondOptic = second.findType(type, resultType, matcher, recurse);
            if (secondOptic.isDefined()) {
                TypedOptic<Tuple2<F, G>, ?, G, ?> outer =
                        castOptic(TypedOptic.proj2(first, second, secondOptic.get().tType()));
                return Maybe.some(composeOptics(outer, secondOptic.get()));
            }
            return Maybe.none();
        }

        @Override
        public boolean equals(Type<?> other, boolean ignoreRecursionPoints, boolean checkIndex) {
            return other instanceof ProductType<?, ?> that
                    && first.equals(that.first, ignoreRecursionPoints, checkIndex)
                    && second.equals(that.second, ignoreRecursionPoints, checkIndex);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            return obj instanceof ProductType<?, ?> that && first.equals(that.first) && second.equals(that.second);
        }

        @Override
        public int hashCode() {
            return 31 * first.hashCode() + second.hashCode();
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }
    }
}
