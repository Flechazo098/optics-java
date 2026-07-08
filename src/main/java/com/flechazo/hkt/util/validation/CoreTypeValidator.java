package com.flechazo.hkt.util.validation;

import java.util.Objects;

public enum CoreTypeValidator {
    CORE_TYPE_VALIDATOR;

    public <T> T requireValue(T value, Class<?> typeClass, Operation operation) {
        String context = context(typeClass, operation);
        return Objects.requireNonNull(value, context + " value cannot be null");
    }

    public <T> T requireValue(T value, String valueName, Class<?> typeClass, Operation operation) {
        String context = context(typeClass, operation);
        return Objects.requireNonNull(value, "%s %s cannot be null".formatted(context, valueName));
    }

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
