package com.flechazo.hkt.functions;

import com.flechazo.hkt.Choice;
import com.flechazo.hkt.K1;
import com.flechazo.optics.Prism;
import com.google.common.reflect.TypeToken;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record PrismOpticElement<S, A>(Object key, Prism<S, A> prism) implements PointFreeOpticElement {
    public PrismOpticElement {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(prism, "prism");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.PRISM;
    }

    @Override
    public Set<TypeToken<? extends K1>> bounds() {
        return Set.of(Choice.Mu.TYPE_TOKEN);
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        return prism.modify(value -> narrow(function.apply(value)), narrow(source));
    }

    @SuppressWarnings("unchecked")
    private static <A> A narrow(Object value) {
        return (A) value;
    }
}
