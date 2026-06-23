package com.flechazo.hkt.type;

import com.flechazo.hkt.Pair;

import java.util.Objects;

public record Named(String name, TypeTemplate element) implements TypeTemplate {
    public Named {
        name = Type.requireName(name, "name");
        Types.requireTemplate(element, "element");
    }

    @Override
    public int size() {
        return element.size();
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return index -> Types.named(name, element.apply(family).apply(index));
    }

    @Override
    public String toString() {
        return "NamedTypeTag[" + name + ": " + element + "]";
    }

    public static final class NamedType<A> extends Type<Pair<String, A>> {
        private final String name;
        private final Type<A> element;

        public NamedType(String name, Type<A> element) {
            this.name = Type.requireName(name, "name");
            this.element = Objects.requireNonNull(element, "element");
        }

        public String name() {
            return name;
        }

        public Type<A> element() {
            return element;
        }

        @Override
        public TypeTemplate template() {
            return Types.named(name, element.template());
        }

        @Override
        public boolean containsVariable(String name) {
            return element.containsVariable(name);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof NamedType<?> that && name.equals(that.name) && element.equals(that.element);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + element.hashCode();
        }

        @Override
        public String toString() {
            return "NamedType[\"" + name + "\", " + element + "]";
        }
    }
}
