package com.flechazo.hkt.business.stream.internal;

import com.flechazo.hkt.business.stream.VStream;

public final class StreamTailMarker extends RuntimeException {
    private final VStream<?> remainingTail;

    public StreamTailMarker(VStream<?> remainingTail) {
        this.remainingTail = remainingTail;
    }

    public VStream<?> remainingTail() {
        return remainingTail;
    }
}
