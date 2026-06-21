package com.flechazo.hkt;

@FunctionalInterface
public interface ThrowingSupplier<A> {
    A get() throws Exception;
}
