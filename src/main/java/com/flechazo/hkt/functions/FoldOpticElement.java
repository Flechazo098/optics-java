package com.flechazo.hkt.functions;

import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.ProfunctorBound;
import com.flechazo.optics.Fold;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record FoldOpticElement(Object key, Fold<Object, Object> fold) implements PointFreeOpticElement {
    public FoldOpticElement {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(fold, "fold");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.FOLD;
    }

    @Override
    public Set<ProfunctorBound> bounds() {
        return Set.of(ProfunctorBound.MONOIDAL);
    }

    public <M> M foldMap(Monoid<M> monoid, Function<? super Object, ? extends M> function, Object source) {
        return fold.foldMap(monoid, function, source);
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        throw new UnsupportedOperationException("Fold optic elements are query-only and cannot modify");
    }
}
