package com.flechazo.hkt.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Accumulates values in O(1) per step without repeatedly copying list prefixes.
public final class AccumulationBuffer<A> {
    private static final AccumulationBuffer<?> EMPTY = new AccumulationBuffer<>();

    private final A value;
    private final AccumulationBuffer<A> previous;
    private final boolean empty;
    private final int size;

    private AccumulationBuffer() {
        this.value = null;
        this.previous = this;
        this.empty = true;
        this.size = 0;
    }

    private AccumulationBuffer(A value, AccumulationBuffer<A> previous) {
        this.value = value;
        this.previous = previous;
        this.empty = false;
        this.size = previous.size + 1;
    }

    @SuppressWarnings("unchecked")
    public static <A> AccumulationBuffer<A> empty() {
        return (AccumulationBuffer<A>) EMPTY;
    }

    public AccumulationBuffer<A> prepend(A next) {
        return new AccumulationBuffer<>(next, this);
    }

    public List<A> toList() {
        ArrayList<A> values = new ArrayList<>(size);
        AccumulationBuffer<A> current = this;
        while (!current.empty) {
            values.add(current.value);
            current = current.previous;
        }
        Collections.reverse(values);
        return Collections.unmodifiableList(values);
    }
}
