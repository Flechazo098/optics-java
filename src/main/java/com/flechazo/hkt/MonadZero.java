package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface MonadZero<F extends K1, Proof extends MonadZero.Mu> extends Monad<F, Proof> {
    interface Mu extends Monad.Mu {
    }

    static <F extends K1, Proof extends Mu> MonadZero<F, Proof> unbox(App<Proof, F> proofBox) {
        return (MonadZero<F, Proof>) Validation.kind().narrowWithTypeCheck(proofBox, MonadZero.class);
    }

    <A> App<F, A> zero();

    <A> App<F, A> orElse(App<F, A> first, Supplier<? extends App<F, A>> second);

    default <A> App<F, A> filter(Predicate<? super A> predicate, App<F, A> value) {
        Objects.requireNonNull(predicate, "predicate");
        return flatMap(element -> predicate.test(element) ? of(element) : zero(), value);
    }

    default <A> App<F, A> orElse(App<F, A> first, App<F, A> second) {
        Objects.requireNonNull(second, "second");
        return orElse(first, () -> second);
    }

    default <A> App<F, A> orElseAll(Iterable<? extends App<F, A>> alternatives) {
        Objects.requireNonNull(alternatives, "alternatives");
        App<F, A> result = zero();
        for (App<F, A> alternative : alternatives) {
            result = orElse(result, Objects.requireNonNull(alternative, "alternative"));
        }
        return result;
    }
}
