package com.flechazo.hkt.functions;

import com.google.common.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class RecursiveFamily {
    private final String name;
    private final List<TypeToken<?>> slots;

    public RecursiveFamily(String name, int size) {
        this(name, anonymousSlots(size));
    }

    private RecursiveFamily(String name, List<TypeToken<?>> slots) {
        this.name = Objects.requireNonNull(name, "name");
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("family must contain at least one slot");
        }
        this.slots = slots;
    }

    public static RecursiveFamily typed(String name, TypeToken<?> first, TypeToken<?>... rest) {
        Objects.requireNonNull(first, "first");
        TypeToken<?>[] slots = new TypeToken<?>[rest.length + 1];
        slots[0] = first;
        System.arraycopy(rest, 0, slots, 1, rest.length);
        return new RecursiveFamily(name, Arrays.asList(slots));
    }

    public String name() {
        return name;
    }

    public int size() {
        return slots.size();
    }

    public TypeToken<?> slot(int index) {
        checkIndex(index);
        return slots.get(index);
    }

    public List<TypeToken<?>> slots() {
        return slots;
    }

    public void checkIndex(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(index);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RecursiveFamily that
                && name.equals(that.name)
                && slots.equals(that.slots);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + slots.hashCode();
    }

    @Override
    public String toString() {
        return name + slots;
    }

    private static List<TypeToken<?>> anonymousSlots(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        TypeToken<?>[] slots = new TypeToken<?>[size];
        for (int i = 0; i < size; i++) {
            slots[i] = TypeToken.of(Object.class);
        }
        return Arrays.asList(slots);
    }
}
