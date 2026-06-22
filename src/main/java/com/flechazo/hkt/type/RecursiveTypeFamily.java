package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RecursiveTypeFamily {
    private final String name;
    private final List<TypeExpr> slots;
    private final List<TypeExpr> bodies;

    private RecursiveTypeFamily(String name, List<TypeExpr> slots, List<TypeExpr> bodies) {
        this.name = requireName(name, "name");
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("family must contain at least one slot");
        }
        if (slots.size() != bodies.size()) {
            throw new IllegalArgumentException("slot and body counts must match");
        }
        this.slots = List.copyOf(slots);
        this.bodies = List.copyOf(bodies);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    public int size() {
        return slots.size();
    }

    public TypeExpr slot(int index) {
        checkIndex(index);
        return slots.get(index);
    }

    public TypeExpr.RecursiveSlot slotRef(int index) {
        checkIndex(index);
        Maybe<TypeRef<?>> witness = slots.get(index).witness();
        return new TypeExpr.RecursiveSlot(name, index, witness);
    }

    public TypeExpr body(int index) {
        checkIndex(index);
        return bodies.get(index);
    }

    public TypeExpr unfold(TypeExpr.RecursiveSlot slot) {
        Objects.requireNonNull(slot, "slot");
        if (!name.equals(slot.family())) {
            throw new IllegalArgumentException("slot belongs to a different family: " + slot);
        }
        return body(slot.index());
    }

    public TypeSubstitution slotSubstitution() {
        TypeSubstitution substitution = TypeSubstitution.empty();
        for (int i = 0; i < slots.size(); i++) {
            substitution = substitution.plusRecursiveSlot(slotRef(i), bodies.get(i));
        }
        return substitution;
    }

    public List<TypeExpr> slots() {
        return slots;
    }

    public List<TypeExpr> bodies() {
        return bodies;
    }

    public void checkIndex(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(index);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RecursiveTypeFamily that
                && name.equals(that.name)
                && slots.equals(that.slots)
                && bodies.equals(that.bodies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, slots, bodies);
    }

    @Override
    public String toString() {
        return "RecursiveTypeFamily[" + name + ", slots=" + slots + ", bodies=" + bodies + "]";
    }

    private static String requireName(String value, String parameter) {
        Objects.requireNonNull(value, parameter);
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameter + " must not be blank");
        }
        return value;
    }

    public static final class Builder {
        private final String name;
        private final ArrayList<TypeExpr> slots = new ArrayList<>();
        private final ArrayList<TypeExpr> bodies = new ArrayList<>();

        private Builder(String name) {
            this.name = requireName(name, "name");
        }

        public Builder slot(TypeExpr slot, TypeExpr body) {
            slots.add(Objects.requireNonNull(slot, "slot"));
            bodies.add(Objects.requireNonNull(body, "body"));
            return this;
        }

        public Builder slot(TypeRef<?> slot, TypeExpr body) {
            return slot(slot.expr(), body);
        }

        public RecursiveTypeFamily build() {
            return new RecursiveTypeFamily(name, slots, bodies);
        }
    }
}
