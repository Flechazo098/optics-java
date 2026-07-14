package com.flechazo.optics.focus;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.*;

import java.util.function.Function;

/**
 * Represents a reusable composition path whose resulting focus may be absent.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
public final class AffinePath<S, A> {
    private final PAffine<S, S, A, A> affine;

    private AffinePath(PAffine<S, S, A, A> affine) {
        this.affine = affine;
    }

    /**
     * Creates an affine path from a monomorphic affine optic.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param affine the affine optic represented by the path
     * @return the resulting path
     */
    public static <S, A> AffinePath<S, A> of(PAffine<S, S, A, A> affine) {
        return new AffinePath<>(affine);
    }

    /**
     * Returns the affine optic represented by this path.
     *
     * @return the affine optic
     */
    public PAffine<S, S, A, A> toAffine() {
        return affine;
    }

    /**
     * Returns a fold observing the path focus when present.
     *
     * @return the fold view
     */
    public Fold<S, A> toFold() {
        return affine.asFold();
    }

    /**
     * Previews the path focus.
     *
     * @param source the source to observe
     * @return the focus, or an empty value when absent
     */
    public Maybe<A> preview(S source) {
        return affine.getMaybe(source);
    }

    /**
     * Replaces the path focus when present.
     *
     * @param value the replacement focus
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    public S set(A value, S source) {
        return affine.set(value, source);
    }

    /**
     * Transforms the path focus when present.
     *
     * @param f the focus transformation
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    public S modify(Function<? super A, ? extends A> f, S source) {
        return affine.modify(f, source);
    }

    /**
     * Extends this path through a lens.
     *
     * @param <B> the resulting focus type
     * @param next the lens applied to a present focus
     * @return the extended affine path
     */
    public <B> AffinePath<S, B> via(PLens<A, A, B, B> next) {
        return new AffinePath<>(affine.andThen(next));
    }

    /**
     * Extends this path through a prism.
     *
     * @param <B> the resulting focus type
     * @param next the prism applied to a present focus
     * @return the extended affine path
     */
    public <B> AffinePath<S, B> via(PPrism<A, A, B, B> next) {
        return new AffinePath<>(affine.andThen(next));
    }

    /**
     * Extends this path through another affine optic.
     *
     * @param <B> the resulting focus type
     * @param next the affine optic applied to a present focus
     * @return the extended affine path
     */
    public <B> AffinePath<S, B> via(PAffine<A, A, B, B> next) {
        return new AffinePath<>(affine.andThen(next));
    }

    /**
     * Extends this path through a traversal.
     *
     * @param <B> the resulting focus type
     * @param next the traversal applied to a present focus
     * @return the resulting traversal path
     */
    public <B> TraversalPath<S, B> via(PTraversal<A, A, B, B> next) {
        return TraversalPath.of(affine.andThen(next));
    }
}
