package com.flechazo.hkt.business.capability;

import com.flechazo.hkt.business.capability.combinable.Combinable;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Chainable<A> extends Combinable<A> {
    <B> Chainable<B> via(Function<? super A, ? extends Chainable<B>> mapper);

    default <B> Chainable<B> flatMap(Function<? super A, ? extends Chainable<B>> mapper) {
        return via(mapper);
    }

    <B> Chainable<B> then(Supplier<? extends Chainable<B>> supplier);
}
