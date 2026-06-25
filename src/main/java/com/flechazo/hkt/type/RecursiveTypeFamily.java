package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.TypedOptic;

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

    public RecursiveTypeFamily replaceUnfoldedTypes(Type<?>[] unfoldedTypes) {
        Objects.requireNonNull(unfoldedTypes, "unfoldedTypes");
        if (unfoldedTypes.length != size) {
            throw new IllegalArgumentException("unfoldedTypes length must match family size");
        }
        TypeTemplate[] templates = new TypeTemplate[size];
        boolean changed = false;
        for (int i = 0; i < size; i++) {
            Objects.requireNonNull(unfoldedTypes[i], "unfoldedTypes[" + i + "]");
            templates[i] = unfoldedTypes[i].template();
            changed |= !apply(i).unfold().equals(unfoldedTypes[i], true, false);
        }
        if (!changed) {
            return this;
        }
        return new RecursiveTypeFamily("ruled " + name, size, i -> templates[i]);
    }

    public RecursivePoint.RecursivePointType<?> buildMuType(int index, Type<?> unfoldedType) {
        checkIndex(index);
        Objects.requireNonNull(unfoldedType, "unfoldedType");
        Type<?>[] unfoldedTypes = currentUnfoldedTypes();
        unfoldedTypes[index] = unfoldedType;
        RecursiveTypeFamily newFamily = replaceUnfoldedTypes(unfoldedTypes);
        RecursivePoint.RecursivePointType<?> result = newFamily.apply(index);
        if (!result.unfold().equals(unfoldedType, true, false)) {
            throw new IllegalStateException("Couldn't determine the new recursive point type");
        }
        return result;
    }

    public <A, B> Maybe<TypedOptic<?, ?, A, B>> findType(
            int index,
            Type<A> aType,
            Type<B> bType,
            Type.TypeMatcher<A, B> matcher,
            boolean recurse) {
        checkIndex(index);
        RecursivePoint.RecursivePointType<?> source = apply(index);
        return source.unfold().findType(aType, bType, matcher, false)
                .map(optic -> {
                    RecursivePoint.RecursivePointType<?> target = buildMuType(index, optic.tType());
                    return optic.castOuter(source, target);
                });
    }

    public Type<?>[] currentUnfoldedTypes() {
        Type<?>[] unfoldedTypes = new Type<?>[size];
        for (int i = 0; i < size; i++) {
            unfoldedTypes[i] = apply(i).unfold();
        }
        return unfoldedTypes;
    }

    public void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
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
