package com.flechazo.hkt.exception;

/**
 * Indicates that a higher-kinded value cannot be narrowed to the requested representation.
 */
public class KindUnwrapException extends IllegalArgumentException {
    /**
     * Creates an exception with a detail message.
     *
     * @param message the detail message
     */
    public KindUnwrapException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the narrowing failure
     */
    public KindUnwrapException(String message, Throwable cause) {
        super(message, cause);
    }
}
