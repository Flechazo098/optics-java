package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.control.ListK;
import com.flechazo.hkt.util.validation.Validation;

import java.util.List;
import java.util.function.Function;

import static com.flechazo.hkt.util.validation.Operation.TRAVERSE;

/**
 * Provides folding and traversal for virtual-thread streams.
 */
public enum VStreamTraverse implements Traversable<VStream.Mu, VStream.InstanceMu> {
    /**
     * Provides the shared virtual-thread stream traversable.
     */
    INSTANCE;

    /**
     * Transforms encoded stream elements lazily.
     *
     * @param <A> the source element type
     * @param <B> the result element type
     * @param f the element transformation
     * @param fa the source stream
     * @return the transformed stream in encoded form
     */
    @Override
    public <A, B> App<VStream.Mu, B> map(Function<? super A, ? extends B> f, App<VStream.Mu, A> fa) {
        return VStreamFunctor.INSTANCE.map(f, fa);
    }

    /**
     * Materializes the stream and applies an applicative transformation in encounter order.
     *
     * @param <F> the applicative witness type
     * @param <A> the source element type
     * @param <B> the result element type
     * @param applicative the applicative used to combine effects
     * @param f the effectful element transformation
     * @param value the source stream
     * @return the transformed encoded stream in the applicative context
     */
    @Override
    public <F extends K1, A, B> App<F, App<VStream.Mu, B>> traverse(
            Applicative<F, ?> applicative,
            Function<? super A, ? extends App<F, B>> f,
            App<VStream.Mu, A> value) {
        Validation.function().validateTraverse(applicative, f, value);
        List<A> elements = VStream.unbox(value).toList().unsafeRun();
        App<F, ListK<B>> result = applicative.of(ListK.empty());
        for (A element : elements) {
            App<F, B> mapped = Validation.function().requireNonNullResult(f.apply(element), "f", TRAVERSE);
            result = applicative.map2(result, mapped, ListK::append);
        }
        return applicative.map(list -> VStream.fromList(list.toList()), result);
    }

    /**
     * Maps stream elements to a monoid and combines them in encounter order.
     *
     * @param <A> the element type
     * @param <M> the accumulated value type
     * @param monoid the monoid used to combine mapped values
     * @param f the element mapping function
     * @param value the source stream
     * @return the combined mapped value
     */
    @Override
    public <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<VStream.Mu, A> value) {
        Validation.function().validateFoldMap(monoid, f, value);
        return VStream.unbox(value)
                .foldLeft(monoid.empty(), (acc, element) -> monoid.combine(acc, f.apply(element)))
                .unsafeRun();
    }

}
