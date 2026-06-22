package com.flechazo.hkt.functions;

import com.flechazo.hkt.Pair;
import com.flechazo.hkt.ProfunctorBound;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record TaggedOpticElement(Object tag) implements PointFreeOpticElement {
    public TaggedOpticElement {
        Objects.requireNonNull(tag, "tag");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.TAGGED;
    }

    @Override
    public Set<ProfunctorBound> bounds() {
        return Set.of(ProfunctorBound.COCARTESIAN);
    }

    @Override
    public Object key() {
        return tag;
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        Pair<?, ?> tagged = (Pair<?, ?>) source;
        return Objects.equals(tag, tagged.first())
                ? Pair.of(tagged.first(), function.apply(tagged.second()))
                : tagged;
    }
}
