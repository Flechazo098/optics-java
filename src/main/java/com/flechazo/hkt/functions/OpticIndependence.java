package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.type.TaggedChoice;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;

import java.util.Objects;

public final class OpticIndependence {
    private OpticIndependence() {
    }

    public static Maybe<SwapEvidence> swapEvidence(
            TypedOptic.Element<?, ?, ?, ?> outer,
            TypedOptic.Element<?, ?, ?, ?> inner) {
        Objects.requireNonNull(outer, "outer");
        Objects.requireNonNull(inner, "inner");
        if (outer.optic() instanceof ProductOpticElement(ProductSide side3)
                && inner.optic() instanceof ProductOpticElement(ProductSide side2)
                && rank(side3) > rank(side2)) {
            return Maybe.some(source -> castType(Types.and(castType(inner.aType()), castType(outer.bType()))));
        }
        if (outer.optic() instanceof SumOpticElement(SumSide side1)
                && inner.optic() instanceof SumOpticElement(SumSide side)
                && rank(side1) > rank(side)) {
            return Maybe.some(source -> castType(Types.or(castType(inner.aType()), castType(outer.bType()))));
        }
        if (outer.optic() instanceof TaggedOpticElement outerTagged
                && inner.optic() instanceof TaggedOpticElement innerTagged
                && compareTags(outerTagged.tag(), innerTagged.tag()) > 0
                && !Objects.equals(outerTagged.tag(), innerTagged.tag())) {
            return taggedSwapEvidence(outer, inner, outerTagged, innerTagged);
        }
        return Maybe.none();
    }

    private static Maybe<SwapEvidence> taggedSwapEvidence(
            TypedOptic.Element<?, ?, ?, ?> outer,
            TypedOptic.Element<?, ?, ?, ?> inner,
            TaggedOpticElement outerTagged,
            TaggedOpticElement innerTagged) {
        if (Objects.equals(outerTagged.tag(), innerTagged.tag())
                || !(inner.sType() instanceof TaggedChoice.TaggedChoiceType<?> source)) {
            return Maybe.none();
        }
        return taggedSwapEvidenceCaptured(source, outerTagged.tag(), outer.aType(), outer.bType())
                .filter(ignored -> taggedBranchMatches(source, innerTagged.tag(), inner.aType()));
    }

    @SuppressWarnings("unchecked")
    private static <K> Maybe<SwapEvidence> taggedSwapEvidenceCaptured(
            TaggedChoice.TaggedChoiceType<?> source,
            Object tag,
            Type<?> current,
            Type<?> replacement) {
        TaggedChoice.TaggedChoiceType<K> typedSource = (TaggedChoice.TaggedChoiceType<K>) source;
        K key = (K) tag;
        if (!typedSource.choiceType(key).filter(type -> type.equals(current)).isDefined()) {
            return Maybe.none();
        }
        return typedSource.replaceChoice(key, replacement)
                .map(updated -> ignored -> (Type<Object>) (Type<?>) updated);
    }

    @SuppressWarnings("unchecked")
    private static <K> boolean taggedBranchMatches(
            TaggedChoice.TaggedChoiceType<?> source,
            Object tag,
            Type<?> current) {
        TaggedChoice.TaggedChoiceType<K> typedSource = (TaggedChoice.TaggedChoiceType<K>) source;
        return typedSource.choiceType((K) tag).filter(type -> type.equals(current)).isDefined();
    }

    private static int rank(ProductSide side) {
        return switch (side) {
            case FIRST -> 0;
            case SECOND -> 1;
        };
    }

    private static int rank(SumSide side) {
        return switch (side) {
            case LEFT -> 0;
            case RIGHT -> 1;
        };
    }

    private static int compareTags(Object left, Object right) {
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    @SuppressWarnings("unchecked")
    private static <A> Type<A> castType(Type<?> type) {
        return (Type<A>) type;
    }

    @FunctionalInterface
    public interface SwapEvidence {
        Type<Object> intermediateType(Type<Object> source);
    }
}
