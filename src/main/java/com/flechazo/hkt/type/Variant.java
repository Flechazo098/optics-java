package com.flechazo.hkt.type;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Objects;

public final class Variant implements TypeTemplate {
    private final String name;
    private final Object2ObjectMap<String, TypeTemplate> cases;
    private final int size;

    public Variant(String name, Object2ObjectMap<String, TypeTemplate> cases) {
        this.name = Type.requireName(name, "name");
        this.cases = Objects.requireNonNull(cases, "cases");
        this.size = cases.values().stream().mapToInt(TypeTemplate::size).max().orElse(0);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return index -> {
            Object2ObjectOpenHashMap<String, Type<?>> types = new Object2ObjectOpenHashMap<>(cases.size());
            for (Map.Entry<String, TypeTemplate> entry : Object2ObjectMaps.fastIterable(cases)) {
                types.put(entry.getKey(), entry.getValue().apply(family).apply(index));
            }
            return Types.variantType(name, types);
        };
    }

    public static final class VariantType extends Type<Object> {
        private final String name;
        private final Object2ObjectMap<String, Type<?>> cases;

        public VariantType(String name, Object2ObjectMap<String, Type<?>> cases) {
            this.name = Type.requireName(name, "name");
            this.cases = Objects.requireNonNull(cases, "cases");
        }

        public String name() {
            return name;
        }

        public Object2ObjectMap<String, Type<?>> cases() {
            return cases;
        }

        @Override
        public TypeTemplate template() {
            Object2ObjectOpenHashMap<String, TypeTemplate> templates = new Object2ObjectOpenHashMap<>(cases.size());
            for (Map.Entry<String, Type<?>> entry : Object2ObjectMaps.fastIterable(cases)) {
                templates.put(entry.getKey(), entry.getValue().template());
            }
            return Types.variant(name, templates);
        }

        @Override
        public boolean containsVariable(String name) {
            for (Type<?> type : cases.values()) {
                if (type.containsVariable(name)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof VariantType that && name.equals(that.name) && cases.equals(that.cases);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + cases.hashCode();
        }

        @Override
        public String toString() {
            return "Variant[" + name + ", " + cases + "]";
        }
    }
}
