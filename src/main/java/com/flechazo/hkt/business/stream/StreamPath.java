package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.business.control.ListPath;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class StreamPath<A> {
    private final Stream<A> value;

    public StreamPath(Stream<A> value) {
        this.value = value;
    }

    public Stream<A> run() {
        return value;
    }

    public <B> StreamPath<B> map(Function<? super A, ? extends B> mapper) {
        return new StreamPath<>(value.map(mapper));
    }

    public StreamPath<A> filter(Predicate<? super A> predicate) {
        return new StreamPath<>(value.filter(predicate));
    }

    public ListPath<A> toListPath() {
        return new ListPath<>(value.toList());
    }

    public VStreamPath<A> toVStreamPath() {
        return new VStreamPath<>(VStream.fromStream(value));
    }
}
