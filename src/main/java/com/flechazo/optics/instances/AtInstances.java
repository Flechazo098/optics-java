package com.flechazo.optics.instances;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.At;
import com.flechazo.optics.Lens;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AtInstances {
    private AtInstances() {
    }

    public static <K, V> At<Map<K, V>, K, V> mapAt() {
        return key ->
                Lens.of(
                        source -> Maybe.ofNullable(source.get(key)),
                        (source, value) -> {
                            LinkedHashMap<K, V> copy = new LinkedHashMap<>(source);
                            if (value.isDefined()) {
                                copy.put(key, value.get());
                            } else {
                                copy.remove(key);
                            }
                            return copy;
                        });
    }

    public static <A> At<List<A>, Integer, A> listAt() {
        return index ->
                Lens.of(
                        source -> index >= 0 && index < source.size() ? Maybe.ofNullable(source.get(index)) : Maybe.none(),
                        (source, value) -> {
                            ArrayList<A> copy = new ArrayList<>(source);
                            if (value.isDefined()) {
                                if (index < 0) {
                                    return copy;
                                }
                                if (index < copy.size()) {
                                    copy.set(index, value.get());
                                } else if (index == copy.size()) {
                                    copy.add(value.get());
                                }
                            } else if (index >= 0 && index < copy.size()) {
                                copy.remove((int) index);
                            }
                            return copy;
                        });
    }
}
