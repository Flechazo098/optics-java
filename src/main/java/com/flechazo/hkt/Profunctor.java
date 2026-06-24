package com.flechazo.hkt;

import com.google.common.reflect.TypeToken;

import java.util.function.Function;

public interface Profunctor<P extends K2, Proof extends Profunctor.Mu> extends Kind2<P, Proof> {
    interface Mu extends Kind2.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    static <P extends K2, Proof extends Mu> Profunctor<P, Proof> unbox(App<Proof, P> proofBox) {
        return (Profunctor<P, Proof>) proofBox;
    }

    <A, B, C, D> FunctionArrow<App2<P, A, B>, App2<P, C, D>> dimap(
            Function<? super C, ? extends A> left,
            Function<? super B, ? extends D> right);

    default <A, B, C, D> App2<P, C, D> dimap(
            App2<P, A, B> value,
            Function<? super C, ? extends A> left,
            Function<? super B, ? extends D> right) {
        return this.<A, B, C, D>dimap(left, right).apply(value);
    }

    default <A, B, C> App2<P, C, B> lmap(
            Function<? super C, ? extends A> f,
            App2<P, A, B> value) {
        return dimap(value, f, Function.identity());
    }

    default <A, B, D> App2<P, A, D> rmap(
            Function<? super B, ? extends D> f,
            App2<P, A, B> value) {
        return dimap(value, Function.identity(), f);
    }
}
