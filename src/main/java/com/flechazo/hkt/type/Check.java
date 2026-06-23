package com.flechazo.hkt.type;

import java.util.Objects;

public record Check(String name, int index, TypeTemplate element) implements TypeTemplate {
    public Check {
        name = Type.requireName(name, "name");
        if (index < 0) {
            throw new IllegalArgumentException("index must be non-negative");
        }
        Types.requireTemplate(element, "element");
    }

    @Override
    public int size() {
        return Math.max(index + 1, element.size());
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return i -> Types.checkedType(name, index, element.apply(family).apply(i));
    }

    @Override
    public String toString() {
        return "Tag[" + name + ", " + index + ": " + element + "]";
    }

    public static final class CheckType<A> extends Type<A> {
        private final String name;
        private final int index;
        private final Type<A> element;

        public CheckType(String name, int index, Type<A> element) {
            this.name = Type.requireName(name, "name");
            if (index < 0) {
                throw new IllegalArgumentException("index must be non-negative");
            }
            this.index = index;
            this.element = Objects.requireNonNull(element, "element");
        }

        public String name() {
            return name;
        }

        public int index() {
            return index;
        }

        public Type<A> element() {
            return element;
        }

        @Override
        public TypeTemplate template() {
            return Types.checked(name, index, element.template());
        }

        @Override
        public boolean containsVariable(String name) {
            return element.containsVariable(name);
        }

        @Override
        public Type<?> updateMu(RecursiveTypeFamily newFamily) {
            return new CheckType<>(name, index, element.updateMu(newFamily));
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CheckType<?> that
                    && name.equals(that.name)
                    && index == that.index
                    && element.equals(that.element);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, index, element);
        }

        @Override
        public String toString() {
            return "TypeTag[" + index + "][" + name + ": " + element + "]";
        }
    }
}
