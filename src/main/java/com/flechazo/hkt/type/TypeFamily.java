package com.flechazo.hkt.type;

@FunctionalInterface
public interface TypeFamily {
    Type<?> apply(int index);

    static TypeFamily constant(Type<?> type) {
        return ignored -> type;
    }
}
