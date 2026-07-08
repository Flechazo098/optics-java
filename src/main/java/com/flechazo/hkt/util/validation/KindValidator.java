package com.flechazo.hkt.util.validation;

import com.flechazo.hkt.App;
import com.flechazo.hkt.App2;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.K2;
import com.flechazo.hkt.exception.KindUnwrapException;

import java.util.Objects;
import java.util.function.Function;

import static com.flechazo.hkt.util.validation.Operation.AP;
import static com.flechazo.hkt.util.validation.Operation.NARROW;
import static com.flechazo.hkt.util.validation.Operation.WIDEN;

public enum KindValidator {
    KIND_VALIDATOR;

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

    public <T> T requireForWiden(T input, Class<T> inputType) {
        if (input == null) {
            KindContext context = new KindContext(inputType, WIDEN.toString());
            throw new NullPointerException(context.nullInputMessage());
        }
        return input;
    }

    public <F extends K1, A> App<F, A> requireNonNull(App<F, A> kind, Operation operation) {
        return requireNonNull(kind, operation, null);
    }

    public <F extends K1, A> App<F, A> requireNonNull(
            App<F, A> kind,
            Operation operation,
            String descriptor) {
        if (kind == null) {
            String context = descriptor == null ? operation.toString() : operation + " (" + descriptor + ")";
            throw new NullPointerException("Kind for " + context + " cannot be null");
        }
        return kind;
    }

    public <F extends K2, A, B> App2<F, A, B> requireNonNull2(App2<F, A, B> kind, Operation operation) {
        return requireNonNull2(kind, operation, null);
    }

    public <F extends K2, A, B> App2<F, A, B> requireNonNull2(
            App2<F, A, B> kind,
            Operation operation,
            String descriptor) {
        if (kind == null) {
            String context = descriptor == null ? operation.toString() : operation + " (" + descriptor + ")";
            throw new NullPointerException("Kind2 for " + context + " cannot be null");
        }
        return kind;
    }

    public <F extends K1, A, B> void validateAp(
            App<F, ? extends Function<A, B>> ff,
            App<F, A> fa) {
        requireNonNull(ff, AP, "function");
        requireNonNull(fa, AP, "argument");
    }

    public record KindContext(Class<?> targetType, String operation) {
        public KindContext {
            Objects.requireNonNull(targetType, "targetType");
            Objects.requireNonNull(operation, "operation");
        }

        public String nullParameterMessage() {
            return "Cannot %s null Kind for %s".formatted(operation, targetType.getSimpleName());
        }

        public String nullInputMessage() {
            return "Input %s cannot be null for %s".formatted(targetType.getSimpleName(), operation);
        }

        public String invalidTypeMessage(Object actualKind) {
            return "Kind instance cannot be narrowed to %s (received: %s)"
                    .formatted(targetType.getSimpleName(), actualKind.getClass().getSimpleName());
        }
    }
}
