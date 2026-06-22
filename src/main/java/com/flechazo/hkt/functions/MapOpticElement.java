package com.flechazo.hkt.functions;

import com.flechazo.hkt.Pair;
import com.flechazo.hkt.ProfunctorBound;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    public Set<ProfunctorBound> bounds() {
        return Set.of(ProfunctorBound.TRAVERSING);
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
                    Pair<?, ?> next = (Pair<?, ?>) function.apply(Pair.of(entry.getKey(), entry.getValue()));
                    result.put(next.first(), next.second());
                }
            }
        }
        return Map.copyOf(result);
    }

    public enum Target {
        VALUES,
        ENTRIES
    }
}
