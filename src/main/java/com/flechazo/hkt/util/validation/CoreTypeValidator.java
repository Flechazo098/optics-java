package com.flechazo.hkt.util.validation;

import java.util.Objects;

/**
 * Validates values used by core sum and product types.
 */
public enum CoreTypeValidator {
    /**
     * Provides the shared core-type validator.
     */
    CORE_TYPE_VALIDATOR;

    /**
     * Returns a value after verifying that it is present.
     *
     * @param <T> the value type
     * @param value the value to verify
     * @param typeClass the type whose operation receives the value
     * @param operation the operation receiving the value
     * @return {@code value}
     */
    public <T> T requireValue(T value, Class<?> typeClass, Operation operation) {
        String context = context(typeClass, operation);
        return Objects.requireNonNull(value, context + " value cannot be null");
    }

    /**
     * Returns a named value after verifying that it is present.
     *
     * @param <T> the value type
     * @param value the value to verify
     * @param valueName the role of the value in the operation
     * @param typeClass the type whose operation receives the value
     * @param operation the operation receiving the value
     * @return {@code value}
     */
    public <T> T requireValue(T value, String valueName, Class<?> typeClass, Operation operation) {
        String context = context(typeClass, operation);
        return Objects.requireNonNull(value, "%s %s cannot be null".formatted(context, valueName));
    }

    /**
     * Returns an error value after verifying that it is present.
     *
     * @param <E> the error type
     * @param error the error value to verify
     * @param typeClass the type whose operation receives the error
     * @param operation the operation receiving the error
     * @return {@code error}
     */
    public <E> E requireError(E error, Class<?> typeClass, Operation operation) {
        String context = context(typeClass, operation);
        return Objects.requireNonNull(error, context + " error cannot be null");
    }

    private static String context(Class<?> typeClass, Operation operation) {
        Objects.requireNonNull(typeClass, "typeClass");
        Objects.requireNonNull(operation, "operation");
        return typeClass.getSimpleName() + "." + operation;
    }
}
