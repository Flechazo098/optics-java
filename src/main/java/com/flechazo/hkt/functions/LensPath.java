package com.flechazo.hkt.functions;

import com.flechazo.optics.Lens;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class LensPath<S, A> {
    private final List<Element> elements;

    private LensPath(List<Element> elements) {
        this.elements = elements;
    }

    static <S, A> LensPath<S, A> fromElements(List<Element> elements) {
        return new LensPath<>(elements);
    }

    public static <S> LensPath<S, S> identity() {
        return new LensPath<>(List.of());
    }

    public static <S, A> LensPath<S, A> of(Object key, Lens<S, S, A, A> lens) {
        return LensPath.<S>identity().andThen(key, lens);
    }

    @SuppressWarnings("unchecked")
    public <B> LensPath<S, B> andThen(Object key, Lens<A, A, B, B> lens) {
        Objects.requireNonNull(lens, "lens");
        ArrayList<Element> next = new ArrayList<>(elements);
        next.add(new Element(key, (Lens<Object, Object, Object, Object>) lens));
        return new LensPath<>(next);
    }

    @SuppressWarnings("unchecked")
    public A get(S source) {
        Object current = source;
        for (Element element : elements) {
            current = element.get(current);
        }
        return (A) current;
    }

    public S set(A value, S source) {
        return setAt(0, source, value);
    }

    public S modify(Function<? super A, ? extends A> f, S source) {
        Objects.requireNonNull(f, "f");
        return set(f.apply(get(source)), source);
    }

    public int size() {
        return elements.size();
    }

    public boolean isIdentity() {
        return elements.isEmpty();
    }

    public <B> LensPath<S, B> prefix(int size) {
        if (size < 0 || size > elements.size()) {
            throw new IndexOutOfBoundsException(size);
        }
        return new LensPath<>(elements.subList(0, size));
    }

    public <B, C> LensPath<B, C> suffix(int from) {
        if (from < 0 || from > elements.size()) {
            throw new IndexOutOfBoundsException(from);
        }
        return new LensPath<>(elements.subList(from, elements.size()));
    }

    public List<Object> keys() {
        return elements.stream().map(Element::key).toList();
    }

    List<Element> elements() {
        return elements;
    }

    @SuppressWarnings("unchecked")
    private S setAt(int index, Object source, Object value) {
        if (index == elements.size()) {
            return (S) value;
        }
        Element element = elements.get(index);
        Object focused = element.get(source);
        Object replaced = setAt(index + 1, focused, value);
        return (S) element.set(replaced, source);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LensPath<?, ?> that && elements.equals(that.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return isIdentity() ? "<id>" : String.join(".", keys().stream().map(String::valueOf).toList());
    }

    record Element(Object key, Lens<Object, Object, Object, Object> lens) {
        Element {
            Objects.requireNonNull(lens, "lens");
        }

        Object get(Object source) {
            return lens.get(source);
        }

        Object set(Object value, Object source) {
            return lens.set(value, source);
        }

        boolean sameOptic(Element other) {
            return Objects.equals(key, other.key) && lens == other.lens;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Element that && sameOptic(that);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hashCode(key) + System.identityHashCode(lens);
        }
    }
}
