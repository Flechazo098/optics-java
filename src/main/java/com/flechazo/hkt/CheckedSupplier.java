package com.flechazo.hkt;

@FunctionalInterface
public interface CheckedSupplier<A, X extends Exception> {
    A get() throws X;
}
