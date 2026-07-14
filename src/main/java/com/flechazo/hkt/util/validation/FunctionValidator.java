package com.flechazo.hkt.util.validation;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.exception.KindUnwrapException;

import java.util.Objects;
import java.util.function.Function;

import static com.flechazo.hkt.util.validation.Operation.*;

/**
 * Validates function arguments and function results used by higher-kinded operations.
 */
public enum FunctionValidator {
    /**
     * Provides the shared function validator.
     */
    FUNCTION_VALIDATOR;

    /**
     * Returns an argument after verifying that it is present.
     *
     * @param <T> the argument type
     * @param parameter the argument to verify
     * @param parameterName the argument name used in diagnostics
     * @param operation the operation receiving the argument
     * @return {@code parameter}
     */
    public <T> T require(T parameter, String parameterName, Operation operation) {
        Objects.requireNonNull(parameterName, "parameterName");
        Objects.requireNonNull(operation, "operation");
        if (parameter == null) {
            throw new NullPointerException(new FunctionContext(parameterName, operation.toString()).nullParameterMessage());
        }
        return parameter;
    }

    /**
     * Returns a function result after verifying that it is present.
     *
     * @param <T> the result type
     * @param result the result to verify
     * @param functionName the function name used in diagnostics
     * @param operation the operation invoking the function
     * @return {@code result}
     * @throws KindUnwrapException if {@code result} is {@code null}
     */
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

    /**
     * Returns a handler after verifying that it is present.
     *
     * @param <T> the handler type
     * @param handler the handler to verify
     * @param operation the operation receiving the handler
     * @return {@code handler}
     */
    public <T> T requireHandler(T handler, Operation operation) {
        return require(handler, "handler", operation);
    }

    /**
     * Validates the arguments of a mapping operation.
     *
     * @param <F> the higher-kinded witness type
     * @param <A> the source value type
     * @param <B> the result value type
     * @param f the mapping function
     * @param fa the value to map
     */
    public <F extends K1, A, B> void validateMap(
            Function<? super A, ? extends B> f,
            App<F, A> fa) {
        require(f, "f", MAP);
        Validation.kind().requireNonNull(fa, MAP);
    }

    /**
     * Validates the arguments of a monadic sequencing operation.
     *
     * @param <F> the higher-kinded witness type
     * @param <A> the source value type
     * @param <B> the result value type
     * @param f the function selecting the next value
     * @param ma the source value
     */
    public <F extends K1, A, B> void validateFlatMap(
            Function<? super A, ? extends App<F, B>> f,
            App<F, A> ma) {
        require(f, "f", FLAT_MAP);
        Validation.kind().requireNonNull(ma, FLAT_MAP);
    }

    /**
     * Validates the arguments of a traversal operation.
     *
     * @param <F> the applicative witness type
     * @param <T> the traversable witness type
     * @param <A> the source element type
     * @param <B> the result element type
     * @param applicative the applicative used to combine effects
     * @param f the effectful element transformation
     * @param ta the traversable value
     */
    public <F extends K1, T extends K1, A, B> void validateTraverse(
            Applicative<F, ?> applicative,
            Function<? super A, ? extends App<F, B>> f,
            App<T, A> ta) {
        require(applicative, "applicative", TRAVERSE);
        require(f, "f", TRAVERSE);
        Validation.kind().requireNonNull(ta, TRAVERSE);
    }

    /**
     * Validates the arguments of a fold-mapping operation.
     *
     * @param <T> the foldable witness type
     * @param <M> the accumulated value type
     * @param <A> the element type
     * @param monoid the monoid used to combine mapped values
     * @param f the element mapping function
     * @param fa the foldable value
     */
    public <T extends K1, M, A> void validateFoldMap(
            Monoid<M> monoid,
            Function<? super A, ? extends M> f,
            App<T, A> fa) {
        require(monoid, "monoid", FOLD_MAP);
        require(f, "f", FOLD_MAP);
        Validation.kind().requireNonNull(fa, FOLD_MAP);
    }

    /**
     * Validates the arguments of an error-recovery operation.
     *
     * @param <F> the higher-kinded witness type
     * @param <A> the result value type
     * @param <E> the error type accepted by the handler
     * @param ma the source value
     * @param handler the recovery function
     */
    public <F extends K1, A, E> void validateHandleErrorWith(
            App<F, A> ma,
            Function<? super E, ? extends App<F, A>> handler) {
        Validation.kind().requireNonNull(ma, HANDLE_ERROR_WITH, "source");
        require(handler, "handler", HANDLE_ERROR_WITH);
    }

    /**
     * Describes a function argument within an operation.
     *
     * @param functionName the function argument name
     * @param operation the operation receiving the function
     */
    public record FunctionContext(String functionName, String operation) {
        /**
         * Creates a function context.
         *
         * @param functionName the function argument name
         * @param operation the operation receiving the function
         */
        public FunctionContext {
            Objects.requireNonNull(functionName, "functionName");
            Objects.requireNonNull(operation, "operation");
        }

        /**
         * Returns the diagnostic message for a missing function argument.
         *
         * @return the diagnostic message
         */
        public String nullParameterMessage() {
            return "%s for %s cannot be null".formatted(functionName, operation);
        }
    }
}
