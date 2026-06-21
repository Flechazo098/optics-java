package com.flechazo.optics.util;

import com.flechazo.hkt.Maybe;

import java.util.Objects;
import java.util.Optional;

public final class Optionals {
    private Optionals() {
    }

    public static <A> Optional<A> fromMaybe(Maybe<A> value) {
        return value.isDefined() ? Optional.of(Objects.requireNonNull(value.get(), "value")) : Optional.empty();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <A> Maybe<A> toMaybe(Optional<? extends A> value) {
        return value.<Maybe<A>>map(Maybe::some).orElseGet(Maybe::none);
    }
}
