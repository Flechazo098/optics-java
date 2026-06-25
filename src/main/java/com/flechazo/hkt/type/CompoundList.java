package com.flechazo.hkt.type;

import com.flechazo.hkt.Pair;

import java.util.List;
import java.util.Objects;

public record CompoundList(TypeTemplate key, TypeTemplate element) implements TypeTemplate {
    public CompoundList {
        Types.requireTemplate(key, "key");
        Types.requireTemplate(element, "element");
    }

    @Override
    public int size() {
        return Math.max(key.size(), element.size());
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return index -> Types.compoundList(key.apply(family).apply(index), element.apply(family).apply(index));
    }

    @Override
    public String toString() {
        return "CompoundList[" + element + "]";
    }

    public static final class CompoundListType<K, V> extends Type<List<Pair<K, V>>> {
        private final Type<K> key;
        private final Type<V> element;

        public CompoundListType(Type<K> key, Type<V> element) {
            this.key = Objects.requireNonNull(key, "key");
            this.element = Objects.requireNonNull(element, "element");
        }

        public Type<K> key() {
            return key;
        }

        public Type<V> element() {
            return element;
        }

        @Override
        public TypeTemplate template() {
            return Types.compoundList(key.template(), element.template());
        }

        @Override
        public boolean containsVariable(String name) {
            return key.containsVariable(name) || element.containsVariable(name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            return obj instanceof CompoundListType<?, ?> that && key.equals(that.key) && element.equals(that.element);
        }

        @Override
        public int hashCode() {
            return 31 * key.hashCode() + element.hashCode();
        }

        @Override
        public String toString() {
            return "CompoundList[" + key + " -> " + element + "]";
        }
    }
}
