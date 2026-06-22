package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record CompositePointFreeOptic<S, T, A, B>(
        List<PointFreeOpticElement> elements,
        Maybe<PointFreeOpticTypes> explicitTypes) implements PointFreeOptic<S, T, A, B> {
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
    public T modify(Function<? super A, ? extends B> function, S source) {
        return cast(modifyAt(0, cast(function), source));
    }

    private Object modifyAt(int index, Function<Object, Object> function, Object source) {
        if (index == elements.size()) {
            return function.apply(source);
        }
        PointFreeOpticElement element = elements.get(index);
        return element.modify(value -> modifyAt(index + 1, function, value), source);
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }

    @Override
    @NonNull
    public String toString() {
        return elements.isEmpty()
                ? "<id>"
                : String.join(".", elements.stream().map(element -> String.valueOf(element.key())).toList());
    }
}
