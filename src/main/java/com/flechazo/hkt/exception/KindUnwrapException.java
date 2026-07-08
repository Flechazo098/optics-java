package com.flechazo.hkt.exception;

public class KindUnwrapException extends IllegalArgumentException {
    public KindUnwrapException(String message) {
        super(message);
    }

    public KindUnwrapException(String message, Throwable cause) {
        super(message, cause);
    }
}
