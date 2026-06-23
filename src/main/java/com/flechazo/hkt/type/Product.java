package com.flechazo.hkt.type;

import com.flechazo.hkt.Pair;

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

    public static final class ProductType<F, G> extends Type<Pair<F, G>> {
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
        public boolean equals(Type<?> other, boolean ignoreRecursionPoints, boolean checkIndex) {
            return other instanceof ProductType<?, ?> that
                    && first.equals(that.first, ignoreRecursionPoints, checkIndex)
                    && second.equals(that.second, ignoreRecursionPoints, checkIndex);
        }

        @Override
        public boolean equals(Object obj) {
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
