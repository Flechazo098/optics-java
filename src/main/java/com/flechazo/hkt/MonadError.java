package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;

public interface MonadError<F extends K1, E, Proof extends MonadError.Mu> extends Monad<F, Proof> {
    interface Mu extends Monad.Mu {
    }

    static <F extends K1, E, Proof extends Mu> MonadError<F, E, Proof> unbox(App<Proof, F> proofBox) {
        return (MonadError<F, E, Proof>) Validation.kind().narrowWithTypeCheck(proofBox, MonadError.class);
    }

    <A> App<F, A> raiseError(E error);

    <A> App<F, A> handleErrorWith(
            App<F, A> value,
            Function<? super E, ? extends App<F, A>> handler);

    default <A> App<F, A> recover(App<F, A> value, Function<? super E, ? extends A> handler) {
        Objects.requireNonNull(handler, "handler");
        return handleErrorWith(value, error -> of(handler.apply(error)));
    }

    default <A> App<F, A> recoverWith(App<F, A> value, App<F, A> fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return handleErrorWith(value, ignored -> fallback);
    }
}
