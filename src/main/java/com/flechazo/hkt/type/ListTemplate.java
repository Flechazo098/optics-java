package com.flechazo.hkt.type;

import java.util.List;
import java.util.Objects;

public record ListTemplate(TypeTemplate element) implements TypeTemplate {
    public ListTemplate {
        Types.requireTemplate(element, "element");
    }

    @Override
    public int size() {
        return element.size();
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return index -> Types.list(element.apply(family).apply(index));
    }

    public static final class ListType<A> extends Type<List<A>> {
        private final Type<A> element;

        public ListType(Type<A> element) {
            this.element = Objects.requireNonNull(element, "element");
        }

        public Type<A> element() {
            return element;
        }

        @Override
        public TypeTemplate template() {
            return Types.list(element.template());
        }

        @Override
        public boolean containsVariable(String name) {
            return element.containsVariable(name);
        }

        @Override
        public boolean equals(Type<?> other, boolean ignoreRecursionPoints, boolean checkIndex) {
            return other instanceof ListType<?> that && element.equals(that.element, ignoreRecursionPoints, checkIndex);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            return obj instanceof ListType<?> that && element.equals(that.element);
        }

        @Override
        public int hashCode() {
            return element.hashCode();
        }

        @Override
        public String toString() {
            return "List[" + element + "]";
        }
    }
}
