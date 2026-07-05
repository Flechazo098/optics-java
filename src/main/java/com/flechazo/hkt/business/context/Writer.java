package com.flechazo.hkt.business.context;

import com.flechazo.hkt.business.capability.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.core.*;
import com.flechazo.hkt.business.data.*;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.*;

import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Unit;

import java.util.function.Function;

public record Writer<W, A>(W written, A value) {
    public static <W, A> Writer<W, A> value(Monoid<W> monoid, A value) {
        return new Writer<>(monoid.empty(), value);
    }

    public static <W, A> Writer<W, A> of(W written, A value) {
        return new Writer<>(written, value);
    }

    public static <W> Writer<W, Unit> tell(W written) {
        return new Writer<>(written, Unit.INSTANCE);
    }

    public A run() {
        return value;
    }

    public W exec() {
        return written;
    }

    public <B> Writer<W, B> map(Function<? super A, ? extends B> mapper) {
        return new Writer<>(written, mapper.apply(value));
    }

    public <W2> Writer<W2, A> mapWritten(Function<? super W, ? extends W2> mapper) {
        return new Writer<>(mapper.apply(written), value);
    }
}
