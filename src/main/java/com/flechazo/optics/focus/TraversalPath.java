package com.flechazo.optics.focus;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Fold;
import com.flechazo.optics.PLens;
import com.flechazo.optics.PPrism;
import com.flechazo.optics.PTraversal;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a reusable composition path over zero or more focuses.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
public final class TraversalPath<S, A> {
    private final PTraversal<S, S, A, A> traversal;

    private TraversalPath(PTraversal<S, S, A, A> traversal) {
        this.traversal = traversal;
    }

    /**
     * Creates a traversal path from a monomorphic traversal.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param traversal the traversal represented by the path
     * @return the resulting path
     */
    public static <S, A> TraversalPath<S, A> of(PTraversal<S, S, A, A> traversal) {
        return new TraversalPath<>(traversal);
    }

    /**
     * Returns the traversal represented by this path.
     *
     * @return the traversal
     */
    public PTraversal<S, S, A, A> toTraversal() {
        return traversal;
    }

    /**
     * Returns a fold observing the same focuses.
     *
     * @return the fold view
     */
    public Fold<S, A> toFold() {
        return traversal.asFold();
    }

    /**
     * Returns all path focuses in encounter order.
     *
     * @param source the source to observe
     * @return an unmodifiable list of focuses
     */
    public List<A> getAll(S source) {
        return traversal.getAll(source);
    }

    /**
     * Returns the first path focus when present.
     *
     * @param source the source to observe
     * @return the first focus, or an empty value
     */
    public Maybe<A> preview(S source) {
        return traversal.preview(source);
    }

    /**
     * Transforms every path focus.
     *
     * @param f the focus transformation
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    public S modify(Function<? super A, ? extends A> f, S source) {
        return traversal.modify(f, source);
    }

    /**
     * Replaces every path focus.
     *
     * @param value the replacement focus
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    public S set(A value, S source) {
        return traversal.set(value, source);
    }

    /**
     * Extends this path through a traversal.
     *
     * @param <B> the resulting focus type
     * @param next the traversal applied to every focus
     * @return the extended traversal path
     */
    public <B> TraversalPath<S, B> via(PTraversal<A, A, B, B> next) {
        return new TraversalPath<>(traversal.andThen(next));
    }

    /**
     * Extends this path through a lens.
     *
     * @param <B> the resulting focus type
     * @param next the lens applied to every focus
     * @return the extended traversal path
     */
    public <B> TraversalPath<S, B> via(PLens<A, A, B, B> next) {
        return new TraversalPath<>(traversal.andThen(next));
    }

    /**
     * Extends this path through a prism.
     *
     * @param <B> the resulting focus type
     * @param next the prism applied to every focus
     * @return the extended traversal path
     */
    public <B> TraversalPath<S, B> via(PPrism<A, A, B, B> next) {
        return new TraversalPath<>(traversal.andThen(next));
    }
}
