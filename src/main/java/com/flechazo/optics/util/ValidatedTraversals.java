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
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.google.common.reflect.TypeToken;

import java.util.function.Function;

public final class ValidatedTraversals {
    private ValidatedTraversals() {
    }

    public static <E, A> PTraversal<Validated<E, A>, Validated<E, A>, A, A> valid() {
        PTraversal<Validated<E, A>, Validated<E, A>, A, A> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Validated<E, A>> modifyF(
                    Function<A, App<F, A>> f, Validated<E, A> source, Applicative<F, ?> applicative) {
                return source.isValid()
                        ? applicative.map(Validated::valid, f.apply(source.value()))
                        : applicative.of(source);
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.structured("validatedValidTraversal", null));
    }

    public static <E, A> PTraversal<Validated<E, A>, Validated<E, A>, A, A> valid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return valid(Types.witness(errorType), Types.witness(valueType));
    }

    public static <E, A> PTraversal<Validated<E, A>, Validated<E, A>, A, A> valid(
            Type<E> errorType,
            Type<A> valueType) {
        return OpticMetadata.optic(
                valid(), Maybe.some(PointFreeOptic.validatedValid(errorType, valueType)));
    }

    public static <E, A> PTraversal<Validated<E, A>, Validated<E, A>, E, E> invalid() {
        PTraversal<Validated<E, A>, Validated<E, A>, E, E> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, Validated<E, A>> modifyF(
                    Function<E, App<F, E>> f, Validated<E, A> source, Applicative<F, ?> applicative) {
                return source.isInvalid()
                        ? applicative.map(Validated::invalid, f.apply(source.error()))
                        : applicative.of(source);
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.structured("validatedInvalidTraversal", null));
    }

    public static <E, A> PTraversal<Validated<E, A>, Validated<E, A>, E, E> invalid(
            TypeToken<E> errorType,
            TypeToken<A> valueType) {
        return invalid(Types.witness(errorType), Types.witness(valueType));
    }

    public static <E, A> PTraversal<Validated<E, A>, Validated<E, A>, E, E> invalid(
            Type<E> errorType,
            Type<A> valueType) {
        return OpticMetadata.optic(
                invalid(), Maybe.some(PointFreeOptic.validatedInvalid(errorType, valueType)));
    }
}
 
