package com.flechazo.hkt.type;

public record MaybeTemplate(TypeTemplate value) implements TypeTemplate {
    public MaybeTemplate {
        Types.requireTemplate(value, "value");
    }

    @Override
    public int size() {
        return value.size();
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return index -> Types.maybe(value.apply(family).apply(index));
    }
}
