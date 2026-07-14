package com.flechazo.hkt.business.data;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.*;

/**
 * Represents an immutable, ordered sequence that supports efficient concatenation.
 *
 * <p>Elements are non-null and iteration preserves insertion order.
 *
 * @param <A> the element type
 */
public final class Chain<A> implements Iterable<A> {
    private static final EmptyTree<?> EMPTY_TREE = new EmptyTree<>();
    private static final Chain<?> EMPTY = new Chain<>(emptyTree(), null, null);

    private final FingerTree<A> root;
    private final @Nullable A first;
    private final @Nullable A last;

    private Chain(FingerTree<A> root, @Nullable A first, @Nullable A last) {
        this.root = root;
        this.first = first;
        this.last = last;
    }

    /**
     * Returns the empty chain.
     *
     * @param <A> the element type
     * @return the empty chain
     */
    @SuppressWarnings("unchecked")
    public static <A> Chain<A> empty() {
        return (Chain<A>) EMPTY;
    }

    /**
     * Creates a chain containing one element.
     *
     * @param <A> the element type
     * @param value the sole element
     * @return a singleton chain
     */
    public static <A> Chain<A> singleton(A value) {
        A element = Objects.requireNonNull(value, "value");
        return new Chain<>(new SingleTree<>(element, 1), element, element);
    }

    /**
     * Creates a chain containing the supplied elements in argument order.
     *
     * @param <A> the element type
     * @param values the elements of the chain
     * @return a chain containing {@code values}
     */
    @SafeVarargs
    public static <A> Chain<A> of(A... values) {
        Objects.requireNonNull(values, "values");
        Builder<A> builder = new Builder<>();
        for (A value : values) {
            builder.add(value);
        }
        return builder.build();
    }

    /**
     * Creates a chain from an iterable in encounter order.
     *
     * @param <A> the element type
     * @param values the elements of the chain
     * @return a chain containing the encountered elements
     */
    public static <A> Chain<A> from(Iterable<? extends A> values) {
        Objects.requireNonNull(values, "values");
        if (values instanceof Chain<?> chain) {
            return narrow(chain);
        }
        return build(values);
    }

    /**
     * Returns the concatenation monoid for chains.
     *
     * @param <A> the element type
     * @return a monoid whose identity is the empty chain
     */
    public static <A> Monoid<Chain<A>> monoid() {
        return Monoid.of(Chain.empty(), Chain::concat);
    }

    /**
     * Determines whether this chain contains no elements.
     *
     * @return {@code true} when this chain is empty
     */
    public boolean isEmpty() {
        return root.measure() == 0;
    }

    /**
     * Returns the number of elements in this chain.
     *
     * @return the element count
     */
    public int size() {
        return root.measure();
    }

    /**
     * Returns a chain with an element added at the end.
     *
     * @param value the element to append
     * @return a chain ending with {@code value}
     */
    public Chain<A> append(A value) {
        A element = Objects.requireNonNull(value, "value");
        return isEmpty()
                ? new Chain<>(new SingleTree<>(element, 1), element, element)
                : new Chain<>(append(root, element, Chain::unitMeasure), first, element);
    }

    /**
     * Returns a chain with encountered elements added at the end.
     *
     * @param values the elements to append
     * @return the concatenated chain
     */
    public Chain<A> appendAll(Iterable<? extends A> values) {
        Objects.requireNonNull(values, "values");
        if (values instanceof Chain<?> chain) {
            return concat(narrow(chain));
        }
        return concat(build(values));
    }

    /**
     * Returns a chain with an element added at the beginning.
     *
     * @param value the element to prepend
     * @return a chain beginning with {@code value}
     */
    public Chain<A> prepend(A value) {
        A element = Objects.requireNonNull(value, "value");
        return isEmpty()
                ? new Chain<>(new SingleTree<>(element, 1), element, element)
                : new Chain<>(prepend(element, root, Chain::unitMeasure), element, last);
    }

    /**
     * Concatenates this chain with another chain.
     *
     * @param other the chain whose elements follow this chain
     * @return the concatenated chain
     */
    @SuppressWarnings("Convert2Diamond")
    public Chain<A> concat(Chain<? extends A> other) {
        Objects.requireNonNull(other, "other");
        if (other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return narrow(other);
        }
        FingerTree<A> joined = appendBetween(root, List.of(), narrowTree(other.root), Chain::unitMeasure);
        return new Chain<A>(joined, first, other.last);
    }

    /**
     * Returns the first element when this chain is non-empty.
     *
     * @return the first element, or an empty value for an empty chain
     */
    public Maybe<A> head() {
        if (isEmpty()) {
            return Maybe.none();
        }
        A value = first;
        if (value == null) {
            throw new AssertionError("non-empty Chain has no first element");
        }
        return Maybe.some(value);
    }

    /**
     * Returns the last element when this chain is non-empty.
     *
     * @return the last element, or an empty value for an empty chain
     */
    public Maybe<A> last() {
        if (isEmpty()) {
            return Maybe.none();
        }
        A value = last;
        if (value == null) {
            throw new AssertionError("non-empty Chain has no last element");
        }
        return Maybe.some(value);
    }

    /**
     * Returns a chain with the reverse encounter order.
     *
     * @return the reversed chain
     */
    public Chain<A> reverse() {
        Builder<A> builder = new Builder<>();
        Iterator<A> iterator = new ChainIterator<>(root, true);
        while (iterator.hasNext()) {
            builder.addKnown(iterator.next());
        }
        return builder.build();
    }

    /**
     * Transforms every element while preserving encounter order.
     *
     * @param <B> the result element type
     * @param mapper the element transformation
     * @return a chain of transformed elements
     */
    public <B> Chain<B> map(Function<? super A, ? extends B> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        Builder<B> builder = new Builder<>();
        for (A value : this) {
            builder.add(mapper.apply(value));
        }
        return builder.build();
    }

    /**
     * Transforms every element to a chain and concatenates the results in encounter order.
     *
     * @param <B> the result element type
     * @param mapper the function producing a chain for each element
     * @return the concatenated mapped chains
     */
    public <B> Chain<B> flatMap(Function<? super A, ? extends Chain<? extends B>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        Chain<B> result = Chain.empty();
        for (A value : this) {
            result = result.concat(narrow(Objects.requireNonNull(mapper.apply(value), "mapped chain")));
        }
        return result;
    }

    /**
     * Retains elements satisfying a predicate.
     *
     * @param predicate the condition for retaining an element
     * @return a chain of matching elements in their original order
     */
    public Chain<A> filter(Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        Builder<A> builder = new Builder<>();
        for (A value : this) {
            if (predicate.test(value)) {
                builder.addKnown(value);
            }
        }
        return builder.build();
    }

    /**
     * Folds elements from first to last.
     *
     * @param <B> the accumulated value type
     * @param initial the initial accumulated value
     * @param operation the operation combining an accumulated value and the next element
     * @return the final accumulated value
     */
    public <B> B foldLeft(B initial, BiFunction<? super B, ? super A, ? extends B> operation) {
        Objects.requireNonNull(operation, "operation");
        B result = initial;
        for (A value : this) {
            result = operation.apply(result, value);
        }
        return result;
    }

    /**
     * Folds elements from last to first.
     *
     * @param <B> the accumulated value type
     * @param initial the initial accumulated value
     * @param operation the operation combining the next element and an accumulated value
     * @return the final accumulated value
     */
    public <B> B foldRight(B initial, BiFunction<? super A, ? super B, ? extends B> operation) {
        Objects.requireNonNull(operation, "operation");
        B result = initial;
        Iterator<A> iterator = new ChainIterator<>(root, true);
        while (iterator.hasNext()) {
            result = operation.apply(iterator.next(), result);
        }
        return result;
    }

    /**
     * Returns the elements as an unmodifiable list in encounter order.
     *
     * @return an unmodifiable list of the elements
     */
    public List<A> toList() {
        ArrayList<A> values = new ArrayList<>(size());
        for (A value : this) {
            values.add(value);
        }
        return Collections.unmodifiableList(values);
    }

    @Override
    public Iterator<A> iterator() {
        return new ChainIterator<>(root, false);
    }

    @Override
    public Spliterator<A> spliterator() {
        return new ChainSpliterator<>(root);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Chain<?> chain) || size() != chain.size()) {
            return false;
        }
        Iterator<A> left = iterator();
        Iterator<?> right = chain.iterator();
        while (left.hasNext()) {
            if (!left.next().equals(right.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (A value : this) {
            hash = 31 * hash + value.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        return toList().toString();
    }

    int pendingAfterFirst(boolean reversed) {
        ChainIterator<A> iterator = new ChainIterator<>(root, reversed);
        iterator.hasNext();
        return iterator.pendingSize();
    }

    private static <E> FingerTree<E> append(
            FingerTree<E> tree,
            E value,
            ToIntFunction<? super E> measure) {
        int valueMeasure = measure.applyAsInt(value);
        if (tree instanceof EmptyTree<?>) {
            return new SingleTree<>(value, valueMeasure);
        }
        if (tree instanceof SingleTree<E>(E value1, int measure1)) {
            return deep(
                    Digit.single(value1, measure1),
                    emptyTree(),
                    Digit.single(value, valueMeasure));
        }
        DeepTree<E> deep = (DeepTree<E>) tree;
        if (deep.suffix().count() < 4) {
            return deep(deep.prefix(), deep.middle(), deep.suffix().append(value, valueMeasure));
        }
        Node<E> promoted = Node.three(
                deep.suffix().get(0),
                deep.suffix().get(1),
                deep.suffix().get(2),
                measure);
        FingerTree<Node<E>> middle = append(deep.middle(), promoted, Node::measure);
        Digit<E> suffix = Digit.pair(
                deep.suffix().get(3),
                deep.suffix().elementMeasure(3),
                value,
                valueMeasure);
        return deep(deep.prefix(), middle, suffix);
    }

    private static <E> FingerTree<E> prepend(
            E value,
            FingerTree<E> tree,
            ToIntFunction<? super E> measure) {
        int valueMeasure = measure.applyAsInt(value);
        if (tree instanceof EmptyTree<?>) {
            return new SingleTree<>(value, valueMeasure);
        }
        if (tree instanceof SingleTree<E>(E value1, int measure1)) {
            return deep(
                    Digit.single(value, valueMeasure),
                    emptyTree(),
                    Digit.single(value1, measure1));
        }
        DeepTree<E> deep = (DeepTree<E>) tree;
        if (deep.prefix().count() < 4) {
            return deep(deep.prefix().prepend(value, valueMeasure), deep.middle(), deep.suffix());
        }
        Node<E> promoted = Node.three(
                deep.prefix().get(1),
                deep.prefix().get(2),
                deep.prefix().get(3),
                measure);
        FingerTree<Node<E>> middle = prepend(promoted, deep.middle(), Node::measure);
        Digit<E> prefix = Digit.pair(
                value,
                valueMeasure,
                deep.prefix().get(0),
                deep.prefix().elementMeasure(0));
        return deep(prefix, middle, deep.suffix());
    }

    private static <E> FingerTree<E> appendBetween(
            FingerTree<E> left,
            List<E> between,
            FingerTree<E> right,
            ToIntFunction<? super E> measure) {
        if (left instanceof EmptyTree<?>) {
            return prependAll(between, right, measure);
        }
        if (right instanceof EmptyTree<?>) {
            return appendAll(left, between, measure);
        }
        if (left instanceof SingleTree<E> single) {
            return prepend(single.value(), prependAll(between, right, measure), measure);
        }
        if (right instanceof SingleTree<E> single) {
            return append(appendAll(left, between, measure), single.value(), measure);
        }

        DeepTree<E> leftDeep = (DeepTree<E>) left;
        DeepTree<E> rightDeep = (DeepTree<E>) right;
        ArrayList<E> bridge = new ArrayList<>(
                leftDeep.suffix().count() + between.size() + rightDeep.prefix().count());
        leftDeep.suffix().addTo(bridge);
        bridge.addAll(between);
        rightDeep.prefix().addTo(bridge);
        List<Node<E>> nodes = nodes(bridge, measure);
        FingerTree<Node<E>> middle = appendBetween(
                leftDeep.middle(),
                nodes,
                rightDeep.middle(),
                Node::measure);
        return deep(leftDeep.prefix(), middle, rightDeep.suffix());
    }

    private static <E> FingerTree<E> prependAll(
            List<E> values,
            FingerTree<E> tree,
            ToIntFunction<? super E> measure) {
        FingerTree<E> result = tree;
        for (int index = values.size() - 1; index >= 0; index--) {
            result = prepend(values.get(index), result, measure);
        }
        return result;
    }

    private static <E> FingerTree<E> appendAll(
            FingerTree<E> tree,
            List<E> values,
            ToIntFunction<? super E> measure) {
        FingerTree<E> result = tree;
        for (E value : values) {
            result = append(result, value, measure);
        }
        return result;
    }

    private static <E> List<Node<E>> nodes(
            List<E> values,
            ToIntFunction<? super E> measure) {
        ArrayList<Node<E>> result = new ArrayList<>((values.size() + 1) / 2);
        int index = 0;
        int remaining = values.size();
        while (remaining > 4) {
            result.add(Node.three(values.get(index), values.get(index + 1), values.get(index + 2), measure));
            index += 3;
            remaining -= 3;
        }
        if (remaining == 2) {
            result.add(Node.two(values.get(index), values.get(index + 1), measure));
        } else if (remaining == 3) {
            result.add(Node.three(values.get(index), values.get(index + 1), values.get(index + 2), measure));
        } else if (remaining == 4) {
            result.add(Node.two(values.get(index), values.get(index + 1), measure));
            result.add(Node.two(values.get(index + 2), values.get(index + 3), measure));
        } else {
            throw new IllegalStateException("Finger tree bridge must contain at least two elements");
        }
        return result;
    }

    private static <E> DeepTree<E> deep(
            Digit<E> prefix,
            FingerTree<Node<E>> middle,
            Digit<E> suffix) {
        return new DeepTree<>(prefix, middle, suffix);
    }

    private static <A> Chain<A> build(Iterable<? extends A> values) {
        Builder<A> builder = new Builder<>();
        for (A value : values) {
            builder.add(value);
        }
        return builder.build();
    }

    private static boolean decompose(Object value, ArrayDeque<Object> pending, boolean reversed) {
        return switch (value) {
            case EmptyTree<?> ignored -> true;

            case SingleTree<?> single -> {
                pending.push(single.value());
                yield true;
            }

            case DeepTree<?> deep -> {
                if (reversed) {
                    pending.push(deep.prefix());
                    pending.push(deep.middle());
                    pending.push(deep.suffix());
                } else {
                    pending.push(deep.suffix());
                    pending.push(deep.middle());
                    pending.push(deep.prefix());
                }
                yield true;
            }

            case Digit<?> digit -> {
                digit.pushInto(pending, reversed);
                yield true;
            }

            case Node<?> node -> {
                node.pushInto(pending, reversed);
                yield true;
            }

            default -> false;
        };
    }

    private static int measuredSize(Object value) {
        return switch (value) {
            case FingerTree<?> tree -> tree.measure();
            case Digit<?> digit -> digit.measure();
            case Node<?> node -> node.measure();
            default -> 1;
        };
    }

    private static int depth(FingerTree<?> tree) {
        return tree instanceof DeepTree<?> deep ? 1 + depth(deep.middle()) : tree.measure() == 0 ? 0 : 1;
    }

    private static boolean validTree(FingerTree<?> tree) {
        if (tree instanceof EmptyTree<?>) {
            return tree.measure() == 0;
        }
        if (tree instanceof SingleTree<?>(Object value, int measure)) {
            return measure == measuredSize(value) && validElement(value);
        }
        DeepTree<?> deep = (DeepTree<?>) tree;
        return deep.measure()
                == deep.prefix().measure() + deep.middle().measure() + deep.suffix().measure()
                && validDigit(deep.prefix())
                && validTree(deep.middle())
                && validDigit(deep.suffix());
    }

    private static boolean validDigit(Digit<?> digit) {
        int measure = 0;
        for (int index = 0; index < digit.count(); index++) {
            Object value = digit.get(index);
            measure += measuredSize(value);
            if (!validElement(value)) {
                return false;
            }
        }
        return digit.count() >= 1 && digit.count() <= 4 && digit.measure() == measure;
    }

    private static boolean validNode(Node<?> node) {
        int measure = 0;
        for (int index = 0; index < node.count(); index++) {
            Object value = node.get(index);
            measure += measuredSize(value);
            if (!validElement(value)) {
                return false;
            }
        }
        return (node.count() == 2 || node.count() == 3) && node.measure() == measure;
    }

    private static boolean validElement(Object value) {
        return !(value instanceof Node<?> node) || validNode(node);
    }

    private static int unitMeasure(Object ignored) {
        return 1;
    }

    @SuppressWarnings("unchecked")
    private static <A> Chain<A> narrow(Chain<?> chain) {
        return (Chain<A>) chain;
    }

    @SuppressWarnings("unchecked")
    private static <E> FingerTree<E> narrowTree(FingerTree<?> tree) {
        return (FingerTree<E>) tree;
    }

    @SuppressWarnings("unchecked")
    private static <E> EmptyTree<E> emptyTree() {
        return (EmptyTree<E>) EMPTY_TREE;
    }

    private sealed interface FingerTree<E> permits EmptyTree, SingleTree, DeepTree {
        int measure();
    }

    private static final class EmptyTree<E> implements FingerTree<E> {
        @Override
        public int measure() {
            return 0;
        }
    }

    private record SingleTree<E>(E value, int measure) implements FingerTree<E> {
    }

    private static final class DeepTree<E> implements FingerTree<E> {
        private final Digit<E> prefix;
        private final FingerTree<Node<E>> middle;
        private final Digit<E> suffix;
        private final int measure;

        private DeepTree(Digit<E> prefix, FingerTree<Node<E>> middle, Digit<E> suffix) {
            this.prefix = prefix;
            this.middle = middle;
            this.suffix = suffix;
            this.measure = Math.addExact(Math.addExact(prefix.measure(), middle.measure()), suffix.measure());
        }

        private Digit<E> prefix() {
            return prefix;
        }

        private FingerTree<Node<E>> middle() {
            return middle;
        }

        private Digit<E> suffix() {
            return suffix;
        }

        @Override
        public int measure() {
            return measure;
        }
    }

    private record Digit<E>(Object[] values, int[] measures, int measure) {

        private static <E> Digit<E> single(E value, int measure) {
            return new Digit<>(new Object[]{value}, new int[]{measure}, measure);
        }

        private static <E> Digit<E> pair(E first, int firstMeasure, E second, int secondMeasure) {
            return new Digit<>(
                    new Object[]{first, second},
                    new int[]{firstMeasure, secondMeasure},
                    Math.addExact(firstMeasure, secondMeasure));
        }

        private int count() {
            return values.length;
        }

        @SuppressWarnings("unchecked")
        private E get(int index) {
            return (E) values[index];
        }

        private int elementMeasure(int index) {
            return measures[index];
        }

        private Digit<E> append(E value, int valueMeasure) {
            Object[] nextValues = new Object[values.length + 1];
            int[] nextMeasures = new int[measures.length + 1];
            System.arraycopy(values, 0, nextValues, 0, values.length);
            System.arraycopy(measures, 0, nextMeasures, 0, measures.length);
            nextValues[values.length] = value;
            nextMeasures[measures.length] = valueMeasure;
            return new Digit<>(nextValues, nextMeasures, Math.addExact(measure, valueMeasure));
        }

        private Digit<E> prepend(E value, int valueMeasure) {
            Object[] nextValues = new Object[values.length + 1];
            int[] nextMeasures = new int[measures.length + 1];
            nextValues[0] = value;
            nextMeasures[0] = valueMeasure;
            System.arraycopy(values, 0, nextValues, 1, values.length);
            System.arraycopy(measures, 0, nextMeasures, 1, measures.length);
            return new Digit<>(nextValues, nextMeasures, Math.addExact(valueMeasure, measure));
        }

        private void addTo(List<E> target) {
            for (Object value : values) {
                @SuppressWarnings("unchecked") E element = (E) value;
                target.add(element);
            }
        }

        private void pushInto(ArrayDeque<Object> pending, boolean reversed) {
            if (reversed) {
                for (Object value : values) {
                    pending.push(value);
                }
            } else {
                for (int index = values.length - 1; index >= 0; index--) {
                    pending.push(values[index]);
                }
            }
        }
    }

    private record Node<E>(Object[] values, int measure) {

        private static <E> Node<E> two(
                E first,
                E second,
                ToIntFunction<? super E> measure) {
            return new Node<>(
                    new Object[]{first, second},
                    Math.addExact(measure.applyAsInt(first), measure.applyAsInt(second)));
        }

        private static <E> Node<E> three(
                E first,
                E second,
                E third,
                ToIntFunction<? super E> measure) {
            return new Node<>(
                    new Object[]{first, second, third},
                    Math.addExact(
                            Math.addExact(measure.applyAsInt(first), measure.applyAsInt(second)),
                            measure.applyAsInt(third)));
        }

        private int count() {
            return values.length;
        }

        @SuppressWarnings("unchecked")
        private E get(int index) {
            return (E) values[index];
        }

        private void pushInto(ArrayDeque<Object> pending, boolean reversed) {
            if (reversed) {
                for (Object value : values) {
                    pending.push(value);
                }
            } else {
                for (int index = values.length - 1; index >= 0; index--) {
                    pending.push(values[index]);
                }
            }
        }
    }

    private static final class Builder<A> {
        private FingerTree<A> root = emptyTree();
        private @Nullable A first;
        private @Nullable A last;

        private void add(A value) {
            addKnown(Objects.requireNonNull(value, "value"));
        }

        private void addKnown(A element) {
            if (root.measure() == 0) {
                first = element;
            }
            last = element;
            root = append(root, element, Chain::unitMeasure);
        }

        private Chain<A> build() {
            return root.measure() == 0 ? empty() : new Chain<A>(root, first, last);
        }
    }

    private static final class ChainIterator<A> implements Iterator<A> {
        private final ArrayDeque<Object> pending = new ArrayDeque<>();
        private final boolean reversed;
        private @Nullable A next;
        private boolean ready;

        private ChainIterator(FingerTree<A> root, boolean reversed) {
            this.reversed = reversed;
            if (root.measure() != 0) {
                pending.push(root);
            }
        }

        @Override
        public boolean hasNext() {
            if (!ready) {
                advance();
            }
            return ready;
        }

        @Override
        public A next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            A value = next;
            if (value == null) {
                throw new AssertionError("ready Chain iterator has no next element");
            }
            next = null;
            ready = false;
            return value;
        }

        @SuppressWarnings("unchecked")
        private void advance() {
            while (!pending.isEmpty()) {
                Object value = pending.pop();
                if (decompose(value, pending, reversed)) {
                    continue;
                }
                next = (A) value;
                ready = true;
                return;
            }
        }

        private int pendingSize() {
            return pending.size();
        }
    }

    private static final class ChainSpliterator<A> implements Spliterator<A> {
        private final ArrayDeque<Object> pending;
        private long remaining;

        private ChainSpliterator(FingerTree<A> root) {
            this.pending = new ArrayDeque<>();
            this.remaining = root.measure();
            if (root.measure() != 0) {
                pending.push(root);
            }
        }

        private ChainSpliterator(ArrayDeque<Object> pending, long remaining) {
            this.pending = pending;
            this.remaining = remaining;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super A> action) {
            Objects.requireNonNull(action, "action");
            while (!pending.isEmpty()) {
                Object value = pending.pop();
                if (decompose(value, pending, false)) {
                    continue;
                }
                remaining--;
                action.accept((A) value);
                return true;
            }
            return false;
        }

        @Override
        public @Nullable Spliterator<A> trySplit() {
            if (remaining < 2) {
                return null;
            }
            long target = remaining >>> 1;
            long collected = 0;
            ArrayList<Object> prefix = new ArrayList<>();
            while (collected < target) {
                Object value = pending.pop();
                int measure = measuredSize(value);
                if (collected + measure <= target) {
                    prefix.add(value);
                    collected += measure;
                } else if (!decompose(value, pending, false)) {
                    throw new IllegalStateException("Unable to split measured chain element");
                }
            }
            ArrayDeque<Object> splitPending = new ArrayDeque<>(prefix.size());
            for (int index = prefix.size() - 1; index >= 0; index--) {
                splitPending.push(prefix.get(index));
            }
            remaining -= collected;
            return new ChainSpliterator<>(splitPending, collected);
        }

        @Override
        public long estimateSize() {
            return remaining;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED
                    | Spliterator.SIZED
                    | Spliterator.SUBSIZED
                    | Spliterator.IMMUTABLE
                    | Spliterator.NONNULL;
        }
    }
}
