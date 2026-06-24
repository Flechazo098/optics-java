package com.flechazo.hkt;

import com.flechazo.hkt.function.Function3;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Applicative<F extends K1, Proof extends Applicative.Mu> extends Functor<F, Proof> {
    interface Mu extends Functor.Mu {
    }

    static <F extends K1, Proof extends Mu> Applicative<F, Proof> unbox(App<Proof, F> proofBox) {
        return (Applicative<F, Proof>) proofBox;
    }

    <A> App<F, A> of(A value);

    <A, B> App<F, B> ap(App<F, ? extends Function<A, B>> ff, App<F, A> fa);

    default <A, B, C> App<F, C> map2(
            App<F, A> fa, App<F, B> fb, BiFunction<? super A, ? super B, ? extends C> f) {
        Objects.requireNonNull(fa, "fa");
        Objects.requireNonNull(fb, "fb");
        Objects.requireNonNull(f, "f");
        return ap(map(a -> b -> f.apply(a, b), fa), fb);
    }

    default <A, B, C, D> App<F, D> map3(
            App<F, A> fa,
            App<F, B> fb,
            App<F, C> fc,
            Function3<? super A, ? super B, ? super C, ? extends D> f) {
        Objects.requireNonNull(f, "f");
        App<F, Function<B, Function<C, D>>> step1 =
                map(a -> b -> c -> f.apply(a, b, c), fa);
        App<F, Function<C, D>> step2 = ap(step1, fb);
        return ap(step2, fc);
    }
}
