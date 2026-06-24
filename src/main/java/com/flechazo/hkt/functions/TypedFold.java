package com.flechazo.hkt.functions;

import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.type.Type;
import com.flechazo.optics.Fold;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public record TypedFold<S, A>(
        Type<S> sourceType,
        Type<A> focusType,
        Node<S, A> node) {
    public TypedFold {
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(focusType, "focusType");
        Objects.requireNonNull(node, "node");
    }

    public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> mapper, S source) {
        Objects.requireNonNull(monoid, "monoid");
        Objects.requireNonNull(mapper, "mapper");
        return node.foldMap(monoid, mapper, source);
    }

    public <B> TypedFold<S, B> compose(TypedFold<A, B> other) {
        Objects.requireNonNull(other, "other");
        return new TypedFold<>(sourceType, other.focusType, new ComposeNode<>(node, other.node));
    }

    public TypedFold<S, A> filtered(Predicate<? super A> predicate) {
        return new TypedFold<>(sourceType, focusType, new FilterNode<>(node, predicate));
    }

    public TypedFold<S, A> plus(TypedFold<S, A> other) {
        Objects.requireNonNull(other, "other");
        return new TypedFold<>(sourceType, focusType, new SumNode<>(node, other.node));
    }

    public boolean sameFold(TypedFold<?, ?> other) {
        return other != null
                && Objects.equals(sourceType, other.sourceType)
                && Objects.equals(focusType, other.focusType)
                && node.sameNode(other.node);
    }

    public static <S, A> TypedFold<S, A> opaque(
            Object key,
            Fold<S, A> fold,
            Type<S> sourceType,
            Type<A> focusType) {
        return new TypedFold<>(sourceType, focusType, new LeafNode<>(key, fold));
    }

    public static <S, T, A, B> TypedFold<S, A> fromOptic(
            PointFreeOptic<S, T, A, B> optic,
            Fold<S, A> executable) {
        return new TypedFold<>(
                optic.sourceType(),
                optic.focusType(),
                new OpticNode<>(optic, executable));
    }

    public sealed interface Node<S, A> permits LeafNode, OpticNode, ComposeNode, FilterNode, SumNode {
        <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> mapper, S source);

        boolean sameNode(Node<?, ?> other);
    }

    public record LeafNode<S, A>(Object key, Fold<S, A> fold) implements Node<S, A> {
        public LeafNode {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(fold, "fold");
        }

        @Override
        public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> mapper, S source) {
            return fold.foldMap(monoid, mapper, source);
        }

        @Override
        public boolean sameNode(Node<?, ?> other) {
            return other instanceof LeafNode<?, ?> leaf && Objects.equals(key, leaf.key);
        }
    }

    public record OpticNode<S, T, A, B>(
            PointFreeOptic<S, T, A, B> optic,
            Fold<S, A> fold)
            implements Node<S, A> {
        public OpticNode {
            Objects.requireNonNull(optic, "optic");
            Objects.requireNonNull(fold, "fold");
        }

        @Override
        public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> mapper, S source) {
            return fold.foldMap(monoid, mapper, source);
        }

        @Override
        public boolean sameNode(Node<?, ?> other) {
            return other instanceof OpticNode<?, ?, ?, ?> opticNode
                    && optic.sameElements(opticNode.optic);
        }
    }

    public record ComposeNode<S, A, B>(Node<S, A> outer, Node<A, B> inner) implements Node<S, B> {
        public ComposeNode {
            Objects.requireNonNull(outer, "outer");
            Objects.requireNonNull(inner, "inner");
        }

        @Override
        public <M> M foldMap(Monoid<M> monoid, Function<? super B, ? extends M> mapper, S source) {
            return outer.foldMap(monoid, value -> inner.foldMap(monoid, mapper, value), source);
        }

        @Override
        public boolean sameNode(Node<?, ?> other) {
            return other instanceof ComposeNode<?, ?, ?> compose
                    && outer.sameNode(compose.outer)
                    && inner.sameNode(compose.inner);
        }
    }

    public record FilterNode<S, A>(Node<S, A> inner, Predicate<? super A> predicate) implements Node<S, A> {
        public FilterNode {
            Objects.requireNonNull(inner, "inner");
            Objects.requireNonNull(predicate, "predicate");
        }

        @Override
        public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> mapper, S source) {
            return inner.foldMap(
                    monoid,
                    value -> predicate.test(value) ? mapper.apply(value) : monoid.empty(),
                    source);
        }

        @Override
        public boolean sameNode(Node<?, ?> other) {
            return other instanceof FilterNode<?, ?> filter
                    && inner.sameNode(filter.inner)
                    && predicate == filter.predicate;
        }
    }

    public record SumNode<S, A>(Node<S, A> left, Node<S, A> right) implements Node<S, A> {
        public SumNode {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
        }

        @Override
        public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> mapper, S source) {
            return monoid.combine(left.foldMap(monoid, mapper, source), right.foldMap(monoid, mapper, source));
        }

        @Override
        public boolean sameNode(Node<?, ?> other) {
            return other instanceof SumNode<?, ?> sum
                    && left.sameNode(sum.left)
                    && right.sameNode(sum.right);
        }
    }
}
