package com.flechazo.hkt.functions;

import com.flechazo.hkt.K1;
import com.flechazo.hkt.Traversing;
import com.flechazo.hkt.tuple.Tuple2;
import com.google.common.reflect.TypeToken;

import java.util.*;
import java.util.function.Function;

public record MapOpticElement(Target target) implements PointFreeOpticElement {
    public MapOpticElement {
        Objects.requireNonNull(target, "target");
    }

    public static MapOpticElement values() {
        return new MapOpticElement(Target.VALUES);
    }

    public static MapOpticElement entries() {
        return new MapOpticElement(Target.ENTRIES);
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.MAP;
    }

    @Override
    public Set<TypeToken<? extends K1>> bounds() {
        return Set.of(Traversing.Mu.TYPE_TOKEN);
    }

    @Override
    public Object key() {
        return target;
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        Map<?, ?> map = (Map<?, ?>) source;
        LinkedHashMap<Object, Object> result = new LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            switch (target) {
                case VALUES -> result.put(entry.getKey(), function.apply(entry.getValue()));
                case ENTRIES -> {
                    Tuple2<?, ?> next = (Tuple2<?, ?>) function.apply(Tuple2.of(entry.getKey(), entry.getValue()));
                    result.put(next.first(), next.second());
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public enum Target {
        VALUES,
        ENTRIES
    }
}
