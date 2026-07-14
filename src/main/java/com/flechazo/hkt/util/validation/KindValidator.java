package com.flechazo.hkt.util.validation;

import com.flechazo.hkt.App;
import com.flechazo.hkt.App2;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.K2;
import com.flechazo.hkt.exception.KindUnwrapException;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

import static com.flechazo.hkt.util.validation.Operation.*;

/**
 * Validates and narrows encoded higher-kinded values.
 */
public enum KindValidator {
    /**
     * Provides the shared higher-kinded value validator.
     */
    KIND_VALIDATOR;

    /**
     * Narrows a unary higher-kinded value with a supplied conversion function.
     *
     * @param <F> the witness type
     * @param <A> the element type
     * @param <T> the target representation
     * @param kind the encoded value to narrow
     * @param targetType the expected target representation
     * @param narrower the conversion from the encoded value
     * @return the narrowed representation
     * @throws KindUnwrapException if the value cannot be narrowed
     */
    public <F extends K1, A, T> T narrow(
            App<F, A> kind,
            Class<T> targetType,
            Function<? super App<F, A>, ? extends T> narrower) {
        KindContext context = new KindContext(targetType, NARROW.toString());
        if (kind == null) {
            throw new KindUnwrapException(context.nullParameterMessage());
        }
        try {
            return narrower.apply(kind);
        } catch (RuntimeException error) {
            throw new KindUnwrapException(context.invalidTypeMessage(kind), error);
        }
    }

    /**
     * Narrows a unary higher-kinded value after checking its runtime type.
     *
     * @param <F> the witness type
     * @param <A> the element type
     * @param <T> the target representation
     * @param kind the encoded value to narrow
     * @param targetType the expected runtime type
     * @return the narrowed representation
     * @throws KindUnwrapException if {@code kind} is absent or has another runtime type
     */
    public <F extends K1, A, T> T narrowWithTypeCheck(
            App<F, A> kind,
            Class<T> targetType) {
        KindContext context = new KindContext(targetType, NARROW.toString());
        if (kind == null) {
            throw new KindUnwrapException(context.nullParameterMessage());
        }
        if (!targetType.isInstance(kind)) {
            throw new KindUnwrapException(context.invalidTypeMessage(kind));
        }
        return targetType.cast(kind);
    }

    /**
     * Narrows a unary higher-kinded holder and reads its represented value.
     *
     * @param <F> the witness type
     * @param <A> the element type
     * @param <T> the target representation
     * @param <H> the holder representation
     * @param kind the encoded holder to narrow
     * @param targetType the target type used in diagnostics
     * @param holderType the expected holder runtime type
     * @param accessor the function reading the target representation from the holder
     * @return the narrowed representation
     * @throws KindUnwrapException if {@code kind} is absent or has another holder type
     */
    public <F extends K1, A, T, H extends App<F, A>> T narrowHolder(
            App<F, A> kind,
            Class<T> targetType,
            Class<H> holderType,
            Function<? super H, ? extends T> accessor) {
        KindContext context = new KindContext(targetType, NARROW.toString());
        if (kind == null) {
            throw new KindUnwrapException(context.nullParameterMessage());
        }
        if (!holderType.isInstance(kind)) {
            throw new KindUnwrapException(context.invalidTypeMessage(kind));
        }
        return accessor.apply(holderType.cast(kind));
    }

    /**
     * Narrows a binary higher-kinded value with a supplied conversion function.
     *
     * @param <F> the witness type
     * @param <A> the first element type
     * @param <B> the second element type
     * @param <T> the target representation
     * @param kind the encoded value to narrow
     * @param targetType the expected target representation
     * @param narrower the conversion from the encoded value
     * @return the narrowed representation
     * @throws KindUnwrapException if the value cannot be narrowed
     */
    public <F extends K2, A, B, T> T narrow2(
            App2<F, A, B> kind,
            Class<T> targetType,
            Function<? super App2<F, A, B>, ? extends T> narrower) {
        KindContext context = new KindContext(targetType, NARROW.toString());
        if (kind == null) {
            throw new KindUnwrapException(context.nullParameterMessage());
        }
        try {
            return narrower.apply(kind);
        } catch (RuntimeException error) {
            throw new KindUnwrapException(context.invalidTypeMessage(kind), error);
        }
    }

    /**
     * Narrows a binary higher-kinded value after checking its runtime type.
     *
     * @param <F> the witness type
     * @param <A> the first element type
     * @param <B> the second element type
     * @param <T> the target representation
     * @param kind the encoded value to narrow
     * @param targetType the expected runtime type
     * @return the narrowed representation
     * @throws KindUnwrapException if {@code kind} is absent or has another runtime type
     */
    public <F extends K2, A, B, T> T narrowWithTypeCheck2(
            App2<F, A, B> kind,
            Class<T> targetType) {
        KindContext context = new KindContext(targetType, NARROW.toString());
        if (kind == null) {
            throw new KindUnwrapException(context.nullParameterMessage());
        }
        if (!targetType.isInstance(kind)) {
            throw new KindUnwrapException(context.invalidTypeMessage(kind));
        }
        return targetType.cast(kind);
    }

    /**
     * Returns a concrete value after verifying that it can be widened.
     *
     * @param <T> the concrete value type
     * @param input the value to verify
     * @param inputType the input type used in diagnostics
     * @return {@code input}
     */
    public <T> T requireForWiden(T input, Class<T> inputType) {
        if (input == null) {
            KindContext context = new KindContext(inputType, WIDEN.toString());
            throw new NullPointerException(context.nullInputMessage());
        }
        return input;
    }

    /**
     * Returns a unary higher-kinded value after verifying that it is present.
     *
     * @param <F> the witness type
     * @param <A> the element type
     * @param kind the encoded value to verify
     * @param operation the operation receiving the value
     * @return {@code kind}
     */
    public <F extends K1, A> App<F, A> requireNonNull(App<F, A> kind, Operation operation) {
        return requireNonNull(kind, operation, null);
    }

    /**
     * Returns a unary higher-kinded value after verifying that it is present.
     *
     * @param <F> the witness type
     * @param <A> the element type
     * @param kind the encoded value to verify
     * @param operation the operation receiving the value
     * @param descriptor the optional role of the value in the operation
     * @return {@code kind}
     */
    public <F extends K1, A> App<F, A> requireNonNull(
            App<F, A> kind,
            Operation operation,
            @Nullable String descriptor) {
        if (kind == null) {
            String context = descriptor == null ? operation.toString() : operation + " (" + descriptor + ")";
            throw new NullPointerException("Kind for " + context + " cannot be null");
        }
        return kind;
    }

    /**
     * Returns a binary higher-kinded value after verifying that it is present.
     *
     * @param <F> the witness type
     * @param <A> the first element type
     * @param <B> the second element type
     * @param kind the encoded value to verify
     * @param operation the operation receiving the value
     * @return {@code kind}
     */
    public <F extends K2, A, B> App2<F, A, B> requireNonNull2(App2<F, A, B> kind, Operation operation) {
        return requireNonNull2(kind, operation, null);
    }

    /**
     * Returns a binary higher-kinded value after verifying that it is present.
     *
     * @param <F> the witness type
     * @param <A> the first element type
     * @param <B> the second element type
     * @param kind the encoded value to verify
     * @param operation the operation receiving the value
     * @param descriptor the optional role of the value in the operation
     * @return {@code kind}
     */
    public <F extends K2, A, B> App2<F, A, B> requireNonNull2(
            App2<F, A, B> kind,
            Operation operation,
            @Nullable String descriptor) {
        if (kind == null) {
            String context = descriptor == null ? operation.toString() : operation + " (" + descriptor + ")";
            throw new NullPointerException("Kind2 for " + context + " cannot be null");
        }
        return kind;
    }

    /**
     * Validates the function and argument values of an applicative application.
     *
     * @param <F> the witness type
     * @param <A> the argument type
     * @param <B> the result type
     * @param ff the encoded function
     * @param fa the encoded argument
     */
    public <F extends K1, A, B> void validateAp(
            App<F, ? extends Function<A, B>> ff,
            App<F, A> fa) {
        requireNonNull(ff, AP, "function");
        requireNonNull(fa, AP, "argument");
    }

    /**
     * Describes a narrowing operation and its target representation.
     *
     * @param targetType the expected target representation
     * @param operation the narrowing operation
     */
    public record KindContext(Class<?> targetType, String operation) {
        /**
         * Creates a narrowing context.
         *
         * @param targetType the expected target representation
         * @param operation the narrowing operation
         */
        public KindContext {
            Objects.requireNonNull(targetType, "targetType");
            Objects.requireNonNull(operation, "operation");
        }

        /**
         * Returns the diagnostic message for an absent encoded value.
         *
         * @return the diagnostic message
         */
        public String nullParameterMessage() {
            return "Cannot %s null Kind for %s".formatted(operation, targetType.getSimpleName());
        }

        /**
         * Returns the diagnostic message for an absent concrete input.
         *
         * @return the diagnostic message
         */
        public String nullInputMessage() {
            return "Input %s cannot be null for %s".formatted(targetType.getSimpleName(), operation);
        }

        /**
         * Returns the diagnostic message for an encoded value of an incompatible type.
         *
         * @param actualKind the incompatible encoded value
         * @return the diagnostic message
         */
        public String invalidTypeMessage(Object actualKind) {
            return "Kind instance cannot be narrowed to %s (received: %s)"
                    .formatted(targetType.getSimpleName(), actualKind.getClass().getSimpleName());
        }
    }
}
