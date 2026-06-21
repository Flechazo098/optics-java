package com.flechazo.optics;

@FunctionalInterface
public interface Ixed<S, I, A> {
    Traversal<S, A> ix(I index);
}
