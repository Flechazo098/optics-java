package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.ThrowableSupplier;

import java.util.function.Function;

public final class Lazy<A> {
    private ThrowableSupplier<? extends A> supplier;
    private A value;
    private Throwable failure;
    private boolean evaluated;

    private Lazy(ThrowableSupplier<? extends A> supplier) {
        this.supplier = supplier;
    }

    public static <A> Lazy<A> now(A value) {
        Lazy<A> lazy = new Lazy<>(() -> value);
        lazy.value = value;
        lazy.evaluated = true;
        lazy.supplier = null;
        return lazy;
    }

    public static <A> Lazy<A> defer(ThrowableSupplier<? extends A> supplier) {
        return new Lazy<>(supplier);
    }

    public A force() throws Throwable {
        if (!evaluated) {
            try {
                value = supplier.get();
            } catch (Throwable error) {
                failure = error;
            } finally {
                evaluated = true;
                supplier = null;
            }
        }
        if (failure != null) {
            throw failure;
        }
        return value;
    }

    public A get() throws Throwable {
        return force();
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public <B> Lazy<B> map(Function<? super A, ? extends B> mapper) {
        return defer(() -> mapper.apply(force()));
    }

    public <B> Lazy<B> flatMap(Function<? super A, ? extends Lazy<B>> mapper) {
        return defer(() -> mapper.apply(force()).force());
    }
}
