package com.flechazo.optics.indexed;

import com.flechazo.hkt.Monoid;

import java.util.function.BiFunction;

/**
 * Represents a read-only optic that observes exactly one focus and a fixed index.
 *
 * @param <I> the index type
 * @param <S> the source type
 * @param <A> the focus type
 */
public interface IndexedGetter<I, S, A> extends IndexedFold<I, S, A> {
    /**
     * Gets the focus from a source.
     *
     * @param source the source to observe
     * @return the focus
     */
    A get(S source);

    /**
     * Returns the index associated with the focus.
     *
     * @return the focus index
     */
    I index();

    /**
     * Maps the indexed focus into a monoid value.
     *
     * @param <M> the accumulated value type
     * @param monoid the identity and combination operation
     * @param f the function receiving the index and focus
     * @param source the source to observe
     * @return the mapped focus value
     */
    @Override
    default <M> M ifoldMap(
            Monoid<M> monoid,
            BiFunction<? super I, ? super A, ? extends M> f,
            S source) {
        return f.apply(index(), get(source));
    }
}
