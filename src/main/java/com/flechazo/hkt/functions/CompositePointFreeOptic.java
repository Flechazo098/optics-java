package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record CompositePointFreeOptic<S>(
        List<PointFreeOpticElement> elements,
        Maybe<PointFreeOpticTypes> explicitTypes) implements PointFreeOptic<S> {
    public CompositePointFreeOptic(List<PointFreeOpticElement> elements) {
        this(elements, Maybe.none());
    }

    public CompositePointFreeOptic {
        Objects.requireNonNull(elements, "elements");
        Objects.requireNonNull(explicitTypes, "explicitTypes");
        elements = List.copyOf(elements);
    }

    @Override
    public Maybe<PointFreeOpticTypes> types() {
        if (explicitTypes.isDefined()) {
            return explicitTypes;
        }
        if (elements.isEmpty()) {
            return Maybe.none();
        }
        Maybe<PointFreeOpticTypes> outer = elements.getFirst().types();
        Maybe<PointFreeOpticTypes> inner = elements.getLast().types();
        if (outer.isEmpty() || inner.isEmpty()) {
            return Maybe.none();
        }
        return Maybe.some(outer.get().compose(inner.get()));
    }

    @Override
    public S modify(Function<Object, Object> function, S source) {
        return modifyAt(0, function, source);
    }

    @SuppressWarnings("unchecked")
    private S modifyAt(int index, Function<Object, Object> function, Object source) {
        if (index == elements.size()) {
            return (S) function.apply(source);
        }
        PointFreeOpticElement element = elements.get(index);
        return (S) element.modify(value -> modifyAt(index + 1, function, value), source);
    }

    @Override
    @NonNull
    public String toString() {
        return elements.isEmpty()
                ? "<id>"
                : String.join(".", elements.stream().map(element -> String.valueOf(element.key())).toList());
    }
}
