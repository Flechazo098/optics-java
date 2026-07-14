package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Validated;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.google.common.reflect.TypeToken;

import java.util.function.Function;

public final class ValidatedTraversals {
    private ValidatedTraversals() {
    }

    public static <E, A> Traversal<Validated<E, A>, A> valid() {
        return Traversal.from(ValidatedTraversals.<E, A, A>pValid());
    }

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

    public static <E, A> Traversal<Validated<E, A>, A> valid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return valid(Types.witness(errorType), Types.witness(valueType));
    }

    public static <E, A> Traversal<Validated<E, A>, A> valid(
            Type<E> errorType,
            Type<A> valueType) {
        return Traversal.from(ValidatedTraversals.<E, A, A>pValid(
                errorType, valueType, valueType));
    }

    public static <E, A, B> PTraversal<Validated<E, A>, Validated<E, B>, A, B> pValid(
            TypeToken<E> errorType,
            TypeToken<A> valueType,
            TypeToken<B> targetValueType) {
        return pValid(
                Types.witness(errorType), Types.witness(valueType), Types.witness(targetValueType));
    }

    public static <E, A, B> PTraversal<Validated<E, A>, Validated<E, B>, A, B> pValid(
            Type<E> errorType,
            Type<A> valueType,
            Type<B> targetValueType) {
        return OpticMetadata.optic(
                ValidatedTraversals.<E, A, B>pValid(),
                Maybe.some(PointFreeOptic.validatedValid(errorType, valueType, targetValueType)));
    }

    public static <E, A> Traversal<Validated<E, A>, E> invalid() {
        return Traversal.from(ValidatedTraversals.<E, E, A>pInvalid());
    }

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

    public static <E, A> Traversal<Validated<E, A>, E> invalid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return invalid(Types.witness(errorType), Types.witness(valueType));
    }

    public static <E, A> Traversal<Validated<E, A>, E> invalid(
            Type<E> errorType,
            Type<A> valueType) {
        return Traversal.from(ValidatedTraversals.<E, E, A>pInvalid(
                errorType, errorType, valueType));
    }

    public static <E, F, A> PTraversal<Validated<E, A>, Validated<F, A>, E, F> pInvalid(
            TypeToken<E> errorType,
            TypeToken<F> targetErrorType,
            TypeToken<A> valueType) {
        return pInvalid(
                Types.witness(errorType), Types.witness(targetErrorType), Types.witness(valueType));
    }

    public static <E, F, A> PTraversal<Validated<E, A>, Validated<F, A>, E, F> pInvalid(
            Type<E> errorType,
            Type<F> targetErrorType,
            Type<A> valueType) {
        return OpticMetadata.optic(
                ValidatedTraversals.<E, F, A>pInvalid(),
                Maybe.some(PointFreeOptic.validatedInvalid(errorType, targetErrorType, valueType)));
    }
}
 
