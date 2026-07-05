package com.flechazo.hkt;

@FunctionalInterface
public interface ThrowableSupplier<A> {
    A get() throws Throwable;
}
