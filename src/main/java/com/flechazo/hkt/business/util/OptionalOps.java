package com.flechazo.hkt.business.util;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class OptionalOps {
    private OptionalOps() {
    }

    public static <A> Maybe<A> toMaybe(Optional<? extends A> value) {
        Objects.requireNonNull(value, "value");
        return value.map(Maybe::<A>some).orElseGet(Maybe::none);
    }

    public static <A> Optional<A> fromMaybe(Maybe<? extends A> value) {
        Objects.requireNonNull(value, "value");
        return value.isDefined() ? Optional.of(value.get()) : Optional.empty();
    }

    public static <L, R> Either<L, R> toEither(Optional<? extends R> value, Supplier<? extends L> ifEmpty) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(ifEmpty, "ifEmpty");
        return value.<Either<L, R>>map(Either::right)
                .orElseGet(() -> Either.left(Objects.requireNonNull(ifEmpty.get(), "empty value")));
    }

    public static <L, R> Optional<R> fromEither(Either<L, ? extends R> value) {
        Objects.requireNonNull(value, "value");
        return value.isRight() ? Optional.of(value.right()) : Optional.empty();
    }
}
