package com.flechazo.hkt.business.capability;

import java.util.function.Consumer;
import java.util.function.Function;

public interface Composable<A> {
    <B> Composable<B> map(Function<? super A, ? extends B> mapper);

    Composable<A> peek(Consumer<? super A> consumer);
}
