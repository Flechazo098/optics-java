package com.flechazo.hkt.function;

import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface Function3<T1, T2, T3, R> {
    @Nullable R apply(T1 t1, T2 t2, T3 t3);
}
