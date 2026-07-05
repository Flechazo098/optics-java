package com.flechazo.hkt.business.data;

import com.flechazo.hkt.business.capability.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.core.*;
import com.flechazo.hkt.business.data.*;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.*;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Semigroup;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class NonEmptyList<A> implements Iterable<A> {
    private final A head;
    private final List<A> tail;

    private NonEmptyList(A head, List<A> tail) {
        this.head = Objects.requireNonNull(head, "head");
        this.tail = Objects.requireNonNull(tail, "tail");
        for (A value : this.tail) {
            Objects.requireNonNull(value, "tail value");
        }
    }

    public static <A> NonEmptyList<A> of(A head) {
        return new NonEmptyList<>(head, List.of());
    }

    public static <A> NonEmptyList<A> of(A head, List<? extends A> tail) {
        return new NonEmptyList<>(head, narrow(tail));
    }

    public static <A> Maybe<NonEmptyList<A>> fromList(List<? extends A> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            return Maybe.none();
        }
        return Maybe.some(new NonEmptyList<>(values.getFirst(), narrow(values.subList(1, values.size()))));
    }

    public static <A> Semigroup<NonEmptyList<A>> semigroup() {
        return NonEmptyList::appendAll;
    }

    public A head() {
        return head;
    }

    public List<A> tail() {
        return tail;
    }

    public List<A> toList() {
        ArrayList<A> result = new ArrayList<>(tail.size() + 1);
        result.add(head);
        result.addAll(tail);
        return result;
    }

    public int size() {
        return tail.size() + 1;
    }

    public NonEmptyList<A> append(A value) {
        Objects.requireNonNull(value, "value");
        ArrayList<A> nextTail = new ArrayList<>(tail.size() + 1);
        nextTail.addAll(tail);
        nextTail.add(value);
        return new NonEmptyList<>(head, nextTail);
    }

    public NonEmptyList<A> appendAll(NonEmptyList<? extends A> other) {
        Objects.requireNonNull(other, "other");
        ArrayList<A> nextTail = new ArrayList<>(tail.size() + other.size());
        nextTail.addAll(tail);
        nextTail.add(other.head());
        nextTail.addAll(other.tail());
        return new NonEmptyList<>(head, nextTail);
    }

    public <B> NonEmptyList<B> map(Function<? super A, ? extends B> f) {
        Objects.requireNonNull(f, "f");
        B nextHead = Objects.requireNonNull(f.apply(head), "mapped head");
        ArrayList<B> nextTail = new ArrayList<>(tail.size());
        for (A value : tail) {
            nextTail.add(Objects.requireNonNull(f.apply(value), "mapped tail value"));
        }
        return new NonEmptyList<>(nextHead, nextTail);
    }

    @Override
    @NonNull
    public Iterator<A> iterator() {
        return new Iterator<>() {
            private boolean atHead = true;
            private final Iterator<A> tailIterator = tail.iterator();

            @Override
            public boolean hasNext() {
                return atHead || tailIterator.hasNext();
            }

            @Override
            public A next() {
                if (atHead) {
                    atHead = false;
                    return head;
                }
                return tailIterator.next();
            }
        };
    }

    @Override
    public String toString() {
        return toList().toString();
    }

    @SuppressWarnings("unchecked")
    private static <A> List<A> narrow(List<? extends A> values) {
        return (List<A>) Objects.requireNonNull(values, "values");
    }
}
