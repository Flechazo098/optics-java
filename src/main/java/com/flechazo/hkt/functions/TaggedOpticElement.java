package com.flechazo.hkt.functions;

import com.flechazo.hkt.Cocartesian;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.tuple.Tuple2;
import com.google.common.reflect.TypeToken;

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
    public Set<TypeToken<? extends K1>> bounds() {
        return Set.of(Cocartesian.Mu.TYPE_TOKEN);
    }

    @Override
    public Object key() {
        return tag;
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        Tuple2<?, ?> tagged = (Tuple2<?, ?>) source;
        return Objects.equals(tag, tagged.first())
                ? Tuple2.of(tagged.first(), function.apply(tagged.second()))
                : tagged;
    }
}
