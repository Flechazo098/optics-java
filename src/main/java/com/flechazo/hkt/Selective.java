package com.flechazo.hkt;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Selective<F extends K1> extends Applicative<F> {
    /**
     * Selects an already-computed right value, or applies an already-computed function to a left value.
     *
     * <p>The outer effect follows each data type's normal nullable-value policy, but a successful
     * control payload must contain a non-null {@link Either}, and a successful function payload must
     * contain a non-null {@link Function}. Those positions are structural inputs to selection, not
     * ordinary payload slots.
     */
    <A, B> App<F, B> select(App<F, Either<A, B>> value, App<F, ? extends Function<A, B>> function);

    /**
     * Lazy conditional convenience operation. Only the selected branch is requested from its supplier.
     */
    <A> App<F, A> ifS(
            App<F, Boolean> condition,
            Supplier<? extends App<F, A>> thenValue,
            Supplier<? extends App<F, A>> elseValue);
}
