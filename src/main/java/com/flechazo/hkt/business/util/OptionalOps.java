package com.flechazo.hkt.business.util;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Converts between Java optional values and the library's explicit sum types.
 */
public final class OptionalOps {
    private OptionalOps() {
    }

    /**
     * Converts an {@link Optional} to a {@link Maybe}.
     *
     * @param <A> the value type
     * @param value the optional value to convert
     * @return a defined value when {@code value} is present, otherwise an empty value
     */
    public static <A> Maybe<A> toMaybe(Optional<? extends A> value) {
        Objects.requireNonNull(value, "value");
        return value.map(Maybe::<A>some).orElseGet(Maybe::none);
    }

    /**
     * Converts a {@link Maybe} to an {@link Optional}.
     *
     * @param <A> the value type
     * @param value the maybe value to convert
     * @return a present optional when {@code value} is defined, otherwise an empty optional
     */
    public static <A> Optional<A> fromMaybe(Maybe<? extends A> value) {
        Objects.requireNonNull(value, "value");
        return value.isDefined() ? Optional.of(value.get()) : Optional.empty();
    }

    /**
     * Converts an {@link Optional} to an {@link Either}.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @param value the optional right value
     * @param ifEmpty the supplier of the left value used when the optional is empty
     * @return a right value when present, otherwise the supplied left value
     */
    public static <L, R> Either<L, R> toEither(Optional<? extends R> value, Supplier<? extends L> ifEmpty) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(ifEmpty, "ifEmpty");
        return value.<Either<L, R>>map(Either::right)
                .orElseGet(() -> Either.left(Objects.requireNonNull(ifEmpty.get(), "empty value")));
    }

    /**
     * Converts an {@link Either} to an {@link Optional} of its right alternative.
     *
     * @param <L> the left value type
     * @param <R> the right value type
     * @param value the either value to convert
     * @return a present optional for a right value, otherwise an empty optional
     */
    public static <L, R> Optional<R> fromEither(Either<L, ? extends R> value) {
        Objects.requireNonNull(value, "value");
        return value.isRight() ? Optional.of(value.right()) : Optional.empty();
    }
}
