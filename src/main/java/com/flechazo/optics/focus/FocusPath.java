package com.flechazo.optics.focus;

import com.flechazo.optics.*;

import java.util.function.Function;

/**
 * Represents a reusable composition path whose resulting focus is always present.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
public final class FocusPath<S, A> {
    private final PLens<S, S, A, A> lens;

    private FocusPath(PLens<S, S, A, A> lens) {
        this.lens = lens;
    }

    /**
     * Creates a focus path from a monomorphic lens.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param lens the lens represented by the path
     * @return the resulting path
     */
    public static <S, A> FocusPath<S, A> of(PLens<S, S, A, A> lens) {
        return new FocusPath<>(lens);
    }

    /**
     * Returns the lens represented by this path.
     *
     * @return the lens
     */
    public PLens<S, S, A, A> toLens() {
        return lens;
    }

    /**
     * Returns a getter observing the path focus.
     *
     * @return the getter view
     */
    public Getter<S, A> toGetter() {
        return lens.asGetter();
    }

    /**
     * Returns a fold observing the path focus.
     *
     * @return the fold view
     */
    public Fold<S, A> toFold() {
        return lens.asFold();
    }

    /**
     * Gets the path focus.
     *
     * @param source the source to observe
     * @return the focus
     */
    public A get(S source) {
        return lens.get(source);
    }

    /**
     * Replaces the path focus.
     *
     * @param value the replacement focus
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    public S set(A value, S source) {
        return lens.set(value, source);
    }

    /**
     * Transforms the path focus.
     *
     * @param f the focus transformation
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    public S modify(Function<? super A, ? extends A> f, S source) {
        return lens.modify(f, source);
    }

    /**
     * Extends this path through a lens.
     *
     * @param <B> the resulting focus type
     * @param next the lens applied to the current focus
     * @return the extended focus path
     */
    public <B> FocusPath<S, B> via(PLens<A, A, B, B> next) {
        return new FocusPath<>(lens.andThen(next));
    }

    /**
     * Extends this path through another focus path.
     *
     * @param <B> the resulting focus type
     * @param next the path applied to the current focus
     * @return the extended focus path
     */
    public <B> FocusPath<S, B> via(FocusPath<A, B> next) {
        return via(next.toLens());
    }

    /**
     * Extends this path through a prism.
     *
     * @param <B> the resulting focus type
     * @param next the prism applied to the current focus
     * @return the resulting affine path
     */
    public <B> AffinePath<S, B> via(PPrism<A, A, B, B> next) {
        return AffinePath.of(lens.andThen(next));
    }

    /**
     * Extends this path through an affine optic.
     *
     * @param <B> the resulting focus type
     * @param next the affine optic applied to the current focus
     * @return the resulting affine path
     */
    public <B> AffinePath<S, B> via(PAffine<A, A, B, B> next) {
        return AffinePath.of(lens.andThen(next));
    }

    /**
     * Extends this path through a traversal.
     *
     * @param <B> the resulting focus type
     * @param next the traversal applied to the current focus
     * @return the resulting traversal path
     */
    public <B> TraversalPath<S, B> via(PTraversal<A, A, B, B> next) {
        return TraversalPath.of(lens.andThen(next));
    }

    /**
     * Traverses every nested focus using the supplied traversal.
     *
     * @param <B> the resulting focus type
     * @param traversal the traversal applied to the current focus
     * @return the resulting traversal path
     */
    public <B> TraversalPath<S, B> each(PTraversal<A, A, B, B> traversal) {
        return via(traversal);
    }
}
