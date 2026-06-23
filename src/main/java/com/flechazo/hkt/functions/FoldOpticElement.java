package com.flechazo.hkt.functions;

import com.flechazo.hkt.K1;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Monoidal;
import com.flechazo.optics.Fold;
import com.google.common.reflect.TypeToken;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record FoldOpticElement<S, A>(Object key, Fold<S, A> fold) implements PointFreeOpticElement {
    public FoldOpticElement {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(fold, "fold");
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.FOLD;
    }

    @Override
    public Set<TypeToken<? extends K1>> bounds() {
        return Set.of(Monoidal.Mu.TYPE_TOKEN);
    }

    public <M> M foldMap(Monoid<M> monoid, Function<? super Object, ? extends M> function, Object source) {
        return fold.foldMap(monoid, function, (S) source);
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        throw new UnsupportedOperationException("Fold optic elements are query-only and cannot modify");
    }
}
