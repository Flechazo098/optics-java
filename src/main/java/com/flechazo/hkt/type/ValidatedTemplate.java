package com.flechazo.hkt.type;

public record ValidatedTemplate(TypeTemplate error, TypeTemplate value) implements TypeTemplate {
    public ValidatedTemplate {
        Types.requireTemplate(error, "error");
        Types.requireTemplate(value, "value");
    }

    @Override
    public int size() {
        return Math.max(error.size(), value.size());
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return index -> Types.validated(error.apply(family).apply(index), value.apply(family).apply(index));
    }
}
