package com.flechazo.optics.internal;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.hkt.functions.PointFreeOptic;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class OpticMetadata {
    private static final Map<Object, PointFreeOptic<?, ?, ?, ?>> OPTICS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, PointFreeFold<?, ?>> FOLDS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private OpticMetadata() {
    }

    public static <S, T, A, B> Maybe<PointFreeOptic<S, T, A, B>> optic(Object value) {
        @SuppressWarnings("unchecked")
        PointFreeOptic<S, T, A, B> optic = (PointFreeOptic<S, T, A, B>) OPTICS.get(value);
        return optic == null ? Maybe.none() : Maybe.some(optic);
    }

    public static <S, A> Maybe<PointFreeFold<S, A>> fold(Object value) {
        @SuppressWarnings("unchecked")
        PointFreeFold<S, A> fold = (PointFreeFold<S, A>) FOLDS.get(value);
        return fold == null ? Maybe.none() : Maybe.some(fold);
    }

    public static <S, T, A, B, O> O optic(O value, Maybe<PointFreeOptic<S, T, A, B>> optic) {
        if (optic.isDefined()) {
            OPTICS.put(value, optic.get());
        }
        return value;
    }

    public static <S, A, O> O fold(O value, Maybe<PointFreeFold<S, A>> fold) {
        if (fold.isDefined()) {
            FOLDS.put(value, fold.get());
        }
        return value;
    }
}
