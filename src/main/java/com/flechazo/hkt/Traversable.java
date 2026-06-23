package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;

public interface Traversable<T extends K1> extends Functor<T>, Foldable<T> {
    <F extends K1, A, B> App<F, App<T, B>> traverse(
            Applicative<F> applicative,
            Function<? super A, ? extends App<F, B>> f,
            App<T, A> value);

    default <F extends K1, A> App<F, App<T, A>> sequence(
            Applicative<F> applicative,
            App<T, ? extends App<F, A>> value) {
        Objects.requireNonNull(applicative, "applicative");
        return traverse(applicative, Function.identity(), unbox(value));
    }

    @Override
    default <A, B> App<T, B> map(Function<? super A, ? extends B> f, App<T, A> fa) {
        return IdF.<App<T, B>>unbox(traverse(IdF.applicative(), a -> IdF.of(f.apply(a)), fa)).value();
    }

    @SuppressWarnings("unchecked")
    private static <T extends K1, F extends K1, A> App<T, App<F, A>> unbox(App<T, ? extends App<F, A>> value) {
        return (App<T, App<F, A>>) value;
    }
}
