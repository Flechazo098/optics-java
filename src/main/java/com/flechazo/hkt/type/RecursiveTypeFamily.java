package com.flechazo.hkt.type;

import java.util.Objects;
import java.util.function.IntFunction;

public final class RecursiveTypeFamily implements TypeFamily {
    private final String name;
    private final IntFunction<TypeTemplate> templates;
    private final int size;
    private final RecursivePoint.RecursivePointType<?>[] types;

    public RecursiveTypeFamily(String name, int size, IntFunction<TypeTemplate> templates) {
        this.name = Type.requireName(name, "name");
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        this.size = size;
        this.templates = Objects.requireNonNull(templates, "templates");
        this.types = new RecursivePoint.RecursivePointType<?>[size];
    }

    public String name() {
        return name;
    }

    public int size() {
        return size;
    }

    @Override
    public RecursivePoint.RecursivePointType<?> apply(int index) {
        checkIndex(index);
        RecursivePoint.RecursivePointType<?> type = types[index];
        if (type == null) {
            type = new RecursivePoint.RecursivePointType<>(this, index, () -> templates.apply(index).apply(this).apply(index));
            types[index] = type;
        }
        return type;
    }

    public TypeTemplate template(int index) {
        checkIndex(index);
        return templates.apply(index);
    }

    public RecursivePoint.RecursivePointType<?> recursivePoint(int index) {
        return apply(index);
    }

    public void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RecursiveTypeFamily that && name.equals(that.name) && size == that.size;
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + size;
    }

    @Override
    public String toString() {
        return "Mu[" + name + ", " + size + "]";
    }
}
