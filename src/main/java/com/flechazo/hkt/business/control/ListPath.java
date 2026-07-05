package com.flechazo.hkt.business.control;

import com.flechazo.hkt.business.capability.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.core.*;
import com.flechazo.hkt.business.data.*;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.*;

import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.business.capability.Chainable;
import com.flechazo.hkt.business.capability.Combinable;
import com.flechazo.hkt.function.Function3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ListPath<A> implements Chainable<A> {
    private final List<A> values;

    public ListPath(List<? extends A> values) {
        this.values = (List<A>) values;
    }

    public List<A> run() {
        return values;
    }

    public MaybePath<A> head() {
        return values.isEmpty() ? Pathway.nothing() : Pathway.just(values.getFirst());
    }

    public MaybePath<A> last() {
        return values.isEmpty() ? Pathway.nothing() : Pathway.just(values.getLast());
    }

    public MaybePath<A> get(int index) {
        return index >= 0 && index < values.size() ? Pathway.just(values.get(index)) : Pathway.nothing();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }

    public boolean anyMatch(Predicate<? super A> predicate) {
        return values.stream().anyMatch(predicate);
    }

    public boolean allMatch(Predicate<? super A> predicate) {
        return values.stream().allMatch(predicate);
    }

    @Override
    public <B> ListPath<B> map(Function<? super A, ? extends B> mapper) {
        ArrayList<B> result = new ArrayList<>(values.size());
        for (A value : values) {
            result.add(mapper.apply(value));
        }
        return new ListPath<>(result);
    }

    @Override
    public ListPath<A> peek(Consumer<? super A> consumer) {
        values.forEach(consumer);
        return this;
    }

    @Override
    public <B, C> ListPath<C> zipWith(
            Combinable<B> other,
            BiFunction<? super A, ? super B, ? extends C> combiner) {
        if (!(other instanceof ListPath<?> otherList)) {
            throw new IllegalArgumentException("Cannot zipWith non-ListPath: " + other.getClass());
        }
        ListPath<B> typedOther = (ListPath<B>) otherList;
        int size = Math.min(values.size(), typedOther.values.size());
        ArrayList<C> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(combiner.apply(values.get(i), typedOther.values.get(i)));
        }
        return new ListPath<>(result);
    }

    @Override
    public <B, C, D> ListPath<D> zipWith3(
            Combinable<B> second,
            Combinable<C> third,
            Function3<? super A, ? super B, ? super C, ? extends D> combiner) {
        return zipWith(second, Combinable.Pair2::new)
                .zipWith(third, (pair, c) -> combiner.apply(pair.first(), pair.second(), c));
    }

    @Override
    public <B> ListPath<B> via(Function<? super A, ? extends Chainable<B>> mapper) {
        ArrayList<B> result = new ArrayList<>();
        for (A value : values) {
            Chainable<B> mapped = mapper.apply(value);
            if (!(mapped instanceof ListPath<?> listPath)) {
                throw new IllegalArgumentException("via mapper must return ListPath, got: " + mapped.getClass());
            }
            result.addAll(((ListPath<B>) listPath).values);
        }
        return new ListPath<>(result);
    }

    @Override
    public <B> ListPath<B> then(Supplier<? extends Chainable<B>> supplier) {
        return via(ignored -> supplier.get());
    }

    public ListPath<A> filter(Predicate<? super A> predicate) {
        ArrayList<A> result = new ArrayList<>();
        for (A value : values) {
            if (predicate.test(value)) {
                result.add(value);
            }
        }
        return new ListPath<>(result);
    }

    public ListPath<A> take(int n) {
        return new ListPath<>(values.subList(0, Math.clamp(n, 0, values.size())));
    }

    public ListPath<A> drop(int n) {
        return new ListPath<>(values.subList(Math.clamp(n, 0, values.size()), values.size()));
    }

    public ListPath<A> takeWhile(Predicate<? super A> predicate) {
        ArrayList<A> result = new ArrayList<>();
        for (A value : values) {
            if (!predicate.test(value)) {
                break;
            }
            result.add(value);
        }
        return new ListPath<>(result);
    }

    public ListPath<A> dropWhile(Predicate<? super A> predicate) {
        int index = 0;
        while (index < values.size() && predicate.test(values.get(index))) {
            index++;
        }
        return new ListPath<>(values.subList(index, values.size()));
    }

    public ListPath<A> distinct() {
        return new ListPath<>(new ArrayList<>(new LinkedHashSet<>(values)));
    }

    public ListPath<A> concat(ListPath<A> other) {
        ArrayList<A> result = new ArrayList<>(values.size() + other.values.size());
        result.addAll(values);
        result.addAll(other.values);
        return new ListPath<>(result);
    }

    public <B> B foldLeft(B initial, BiFunction<B, A, B> f) {
        B result = initial;
        for (A value : values) {
            result = f.apply(result, value);
        }
        return result;
    }

    public <B> B foldRight(B initial, BiFunction<A, B, B> f) {
        B result = initial;
        for (int i = values.size() - 1; i >= 0; i--) {
            result = f.apply(values.get(i), result);
        }
        return result;
    }

    public A fold(A identity, BinaryOperator<A> op) {
        return foldLeft(identity, op);
    }

    public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f) {
        return foldLeft(monoid.empty(), (acc, value) -> monoid.combine(acc, f.apply(value)));
    }

    public ListPath<A> reverse() {
        ArrayList<A> result = new ArrayList<>(values);
        java.util.Collections.reverse(result);
        return new ListPath<>(result);
    }

    public MaybePath<A> toMaybePath() {
        return head();
    }

    public IOPath<List<A>> toIOPath() {
        return new IOPath<>(IO.pure(values));
    }

}
