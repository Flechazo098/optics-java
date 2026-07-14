package com.flechazo.optics.util;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.google.common.reflect.TypeToken;

import java.util.function.Function;

/**
 * Provides traversals over the alternatives of {@link Validated} values.
 */
public final class ValidatedTraversals {
    private ValidatedTraversals() {
    }

    /**
     * Creates a traversal that focuses a valid value.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @return a traversal with no focus for an invalid value
     */
    public static <E, A> Traversal<Validated<E, A>, A> valid() {
        return Traversal.from(ValidatedTraversals.pValid());
    }

    /**
     * Creates a polymorphic traversal that focuses a valid value.
     *
     * @param <E> the unchanged validation error type
     * @param <A> the source valid value type
     * @param <B> the replacement valid value type
     * @return a traversal that preserves invalid values
     */
    public static <E, A, B> PTraversal<Validated<E, A>, Validated<E, B>, A, B> pValid() {
        PTraversal<Validated<E, A>, Validated<E, B>, A, B> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Validated<E, B>> modifyF(
                    Function<A, App<F, B>> f, Validated<E, A> source, Applicative<F, ?> applicative) {
                return source.isValid()
                        ? applicative.map(Validated::valid, f.apply(source.value()))
                        : applicative.of(Validated.invalid(source.error()));
            }
        };
        return OpticPrograms.traversal(
                direct,
                OpticPrograms.structured("validatedValidTraversal", null));
    }

    /**
     * Creates a typed traversal that focuses a valid value.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param errorType the runtime description of the error type
     * @param valueType the runtime description of the valid value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <E, A> Traversal<Validated<E, A>, A> valid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return valid(Types.witness(errorType), Types.witness(valueType));
    }

    /**
     * Creates a typed traversal that focuses a valid value.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param errorType the runtime witness for the error type
     * @param valueType the runtime witness for the valid value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <E, A> Traversal<Validated<E, A>, A> valid(
            Type<E> errorType,
            Type<A> valueType) {
        return Traversal.from(ValidatedTraversals.pValid(
                errorType, valueType, valueType));
    }

    /**
     * Creates a typed polymorphic traversal that focuses a valid value.
     *
     * @param <E> the unchanged validation error type
     * @param <A> the source valid value type
     * @param <B> the replacement valid value type
     * @param errorType the runtime description of the error type
     * @param valueType the runtime description of the source valid value type
     * @param targetValueType the runtime description of the replacement valid value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <E, A, B> PTraversal<Validated<E, A>, Validated<E, B>, A, B> pValid(
            TypeToken<E> errorType,
            TypeToken<A> valueType,
            TypeToken<B> targetValueType) {
        return pValid(
                Types.witness(errorType), Types.witness(valueType), Types.witness(targetValueType));
    }

    /**
     * Creates a typed polymorphic traversal that focuses a valid value.
     *
     * @param <E> the unchanged validation error type
     * @param <A> the source valid value type
     * @param <B> the replacement valid value type
     * @param errorType the runtime witness for the error type
     * @param valueType the runtime witness for the source valid value type
     * @param targetValueType the runtime witness for the replacement valid value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <E, A, B> PTraversal<Validated<E, A>, Validated<E, B>, A, B> pValid(
            Type<E> errorType,
            Type<A> valueType,
            Type<B> targetValueType) {
        return OpticMetadata.optic(
                ValidatedTraversals.pValid(),
                Maybe.some(PointFreeOptic.validatedValid(errorType, valueType, targetValueType)));
    }

    /**
     * Creates a traversal that focuses an invalid error.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @return a traversal with no focus for a valid value
     */
    public static <E, A> Traversal<Validated<E, A>, E> invalid() {
        return Traversal.from(ValidatedTraversals.pInvalid());
    }

    /**
     * Creates a polymorphic traversal that focuses an invalid error.
     *
     * @param <E> the source validation error type
     * @param <F> the replacement validation error type
     * @param <A> the unchanged valid value type
     * @return a traversal that preserves valid values
     */
    public static <E, F, A> PTraversal<Validated<E, A>, Validated<F, A>, E, F> pInvalid() {
        PTraversal<Validated<E, A>, Validated<F, A>, E, F> direct = new PTraversal<>() {
            @Override
            public <G extends K1> App<G, Validated<F, A>> modifyF(
                    Function<E, App<G, F>> f, Validated<E, A> source, Applicative<G, ?> applicative) {
                return source.isInvalid()
                        ? applicative.map(Validated::invalid, f.apply(source.error()))
                        : applicative.of(Validated.valid(source.value()));
            }
        };
        return OpticPrograms.traversal(
                direct,
                OpticPrograms.structured("validatedInvalidTraversal", null));
    }

    /**
     * Creates a typed traversal that focuses an invalid error.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param errorType the runtime description of the error type
     * @param valueType the runtime description of the valid value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <E, A> Traversal<Validated<E, A>, E> invalid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return invalid(Types.witness(errorType), Types.witness(valueType));
    }

    /**
     * Creates a typed traversal that focuses an invalid error.
     *
     * @param <E> the validation error type
     * @param <A> the valid value type
     * @param errorType the runtime witness for the error type
     * @param valueType the runtime witness for the valid value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <E, A> Traversal<Validated<E, A>, E> invalid(
            Type<E> errorType,
            Type<A> valueType) {
        return Traversal.from(ValidatedTraversals.pInvalid(
                errorType, errorType, valueType));
    }

    /**
     * Creates a typed polymorphic traversal that focuses an invalid error.
     *
     * @param <E> the source validation error type
     * @param <F> the replacement validation error type
     * @param <A> the unchanged valid value type
     * @param errorType the runtime description of the source error type
     * @param targetErrorType the runtime description of the replacement error type
     * @param valueType the runtime description of the valid value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <E, F, A> PTraversal<Validated<E, A>, Validated<F, A>, E, F> pInvalid(
            TypeToken<E> errorType,
            TypeToken<F> targetErrorType,
            TypeToken<A> valueType) {
        return pInvalid(
                Types.witness(errorType), Types.witness(targetErrorType), Types.witness(valueType));
    }

    /**
     * Creates a typed polymorphic traversal that focuses an invalid error.
     *
     * @param <E> the source validation error type
     * @param <F> the replacement validation error type
     * @param <A> the unchanged valid value type
     * @param errorType the runtime witness for the source error type
     * @param targetErrorType the runtime witness for the replacement error type
     * @param valueType the runtime witness for the valid value type
     * @return a traversal with type metadata for the supplied alternatives
     */
    public static <E, F, A> PTraversal<Validated<E, A>, Validated<F, A>, E, F> pInvalid(
            Type<E> errorType,
            Type<F> targetErrorType,
            Type<A> valueType) {
        return OpticMetadata.optic(
                ValidatedTraversals.pInvalid(),
                Maybe.some(PointFreeOptic.validatedInvalid(errorType, targetErrorType, valueType)));
    }
}
 
