package com.flechazo.optics.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Accumulates traversal focuses in O(1) per step without repeatedly copying list prefixes.
public final class WanderBuffer<A> {
    private static final WanderBuffer<?> EMPTY = new WanderBuffer<>();

    private final A value;
    private final WanderBuffer<A> previous;
    private final boolean empty;
    private final int size;

    private WanderBuffer() {
        this.value = null;
        this.previous = this;
        this.empty = true;
        this.size = 0;
    }

    private WanderBuffer(A value, WanderBuffer<A> previous) {
        this.value = value;
        this.previous = previous;
        this.empty = false;
        this.size = previous.size + 1;
    }

    @SuppressWarnings("unchecked")
    public static <A> WanderBuffer<A> empty() {
        return (WanderBuffer<A>) EMPTY;
    }

    public WanderBuffer<A> prepend(A next) {
        return new WanderBuffer<>(next, this);
    }

    public List<A> toList() {
        ArrayList<A> values = new ArrayList<>(size);
        WanderBuffer<A> current = this;
        while (!current.empty) {
            values.add(current.value);
            current = current.previous;
        }
        Collections.reverse(values);
        return values;
    }
}
