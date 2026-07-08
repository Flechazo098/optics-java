package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Supplier;

public record Procompose<P extends K2, Q extends K2, A, B, C>(
        Supplier<App2<P, A, C>> first,
        App2<Q, C, B> second) implements App2<Procompose.Mu<P, Q>, A, B> {
    public static final class Mu<P extends K2, Q extends K2> implements K2 {
        private Mu() {
        }
    }

    public Procompose {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
    }

    public static <P extends K2, Q extends K2, A, B, C> Procompose<P, Q, A, B, C> of(
            Supplier<App2<P, A, C>> first,
            App2<Q, C, B> second) {
        return new Procompose<>(first, second);
    }

    public static <P extends K2, Q extends K2, A, B> Procompose<P, Q, A, B, ?> unbox(
            App2<Mu<P, Q>, A, B> value) {
        return (Procompose<P, Q, A, B, ?>) Validation.kind().narrowWithTypeCheck2(value, Procompose.class);
    }
}
