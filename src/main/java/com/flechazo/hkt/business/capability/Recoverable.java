package com.flechazo.hkt.business.capability;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Recoverable<E, A> extends Chainable<A> {
    Recoverable<E, A> recover(Function<? super E, ? extends A> recovery);

    Recoverable<E, A> recoverWith(Function<? super E, ? extends Recoverable<E, A>> recovery);

    Recoverable<E, A> orElse(Supplier<? extends Recoverable<E, A>> alternative);

    <E2> Recoverable<E2, A> mapError(Function<? super E, ? extends E2> mapper);
}
