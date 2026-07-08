package com.flechazo.hkt.util.validation;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.exception.KindUnwrapException;

import java.util.Objects;
import java.util.function.Function;

import static com.flechazo.hkt.util.validation.Operation.FLAT_MAP;
import static com.flechazo.hkt.util.validation.Operation.FOLD_MAP;
import static com.flechazo.hkt.util.validation.Operation.HANDLE_ERROR_WITH;
import static com.flechazo.hkt.util.validation.Operation.MAP;
import static com.flechazo.hkt.util.validation.Operation.TRAVERSE;

public enum FunctionValidator {
    FUNCTION_VALIDATOR;

    public <T> T require(T parameter, String parameterName, Operation operation) {
        Objects.requireNonNull(parameterName, "parameterName");
        Objects.requireNonNull(operation, "operation");
        if (parameter == null) {
            throw new NullPointerException(new FunctionContext(parameterName, operation.toString()).nullParameterMessage());
        }
        return parameter;
    }

    public <T> T requireNonNullResult(T result, String functionName, Operation operation) {
        Objects.requireNonNull(functionName, "functionName");
        Objects.requireNonNull(operation, "operation");
        if (result == null) {
            String message = "Function %s in %s returned null, which is not allowed"
                    .formatted(functionName, operation);
            throw new KindUnwrapException(message);
        }
        return result;
    }

    public <T> T requireHandler(T handler, Operation operation) {
        return require(handler, "handler", operation);
    }

    public <F extends K1, A, B> void validateMap(
            Function<? super A, ? extends B> f,
            App<F, A> fa) {
        require(f, "f", MAP);
        Validation.kind().requireNonNull(fa, MAP);
    }

    public <F extends K1, A, B> void validateFlatMap(
            Function<? super A, ? extends App<F, B>> f,
            App<F, A> ma) {
        require(f, "f", FLAT_MAP);
        Validation.kind().requireNonNull(ma, FLAT_MAP);
    }

    public <F extends K1, T extends K1, A, B> void validateTraverse(
            Applicative<F, ?> applicative,
            Function<? super A, ? extends App<F, B>> f,
            App<T, A> ta) {
        require(applicative, "applicative", TRAVERSE);
        require(f, "f", TRAVERSE);
        Validation.kind().requireNonNull(ta, TRAVERSE);
    }

    public <T extends K1, M, A> void validateFoldMap(
            Monoid<M> monoid,
            Function<? super A, ? extends M> f,
            App<T, A> fa) {
        require(monoid, "monoid", FOLD_MAP);
        require(f, "f", FOLD_MAP);
        Validation.kind().requireNonNull(fa, FOLD_MAP);
    }

    public <F extends K1, A, E> void validateHandleErrorWith(
            App<F, A> ma,
            Function<? super E, ? extends App<F, A>> handler) {
        Validation.kind().requireNonNull(ma, HANDLE_ERROR_WITH, "source");
        require(handler, "handler", HANDLE_ERROR_WITH);
    }

    public record FunctionContext(String functionName, String operation) {
        public FunctionContext {
            Objects.requireNonNull(functionName, "functionName");
            Objects.requireNonNull(operation, "operation");
        }

        public String nullParameterMessage() {
            return "%s for %s cannot be null".formatted(functionName, operation);
        }
    }
}
