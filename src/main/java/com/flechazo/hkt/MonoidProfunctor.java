package com.flechazo.hkt;

import com.google.common.reflect.TypeToken;

import java.util.function.Supplier;

public interface MonoidProfunctor<P extends K2, Proof extends MonoidProfunctor.Mu> extends Profunctor<P, Proof> {
    interface Mu extends Profunctor.Mu {
        TypeToken<Mu> TYPE_TOKEN = new TypeToken<>() {
        };
    }

    static <P extends K2, Proof extends Mu> MonoidProfunctor<P, Proof> unbox(App<Proof, P> proofBox) {
        return (MonoidProfunctor<P, Proof>) proofBox;
    }

    <A, B> App2<P, A, B> zero(App2<FunctionArrow.Mu, A, B> function);

    <A, B> App2<P, A, B> plus(App2<Procompose.Mu<P, P>, A, B> input);

    default <A, B, C> App2<P, A, C> compose(
            App2<P, B, C> first,
            Supplier<App2<P, A, B>> second) {
        return plus(Procompose.of(second, first));
    }
}
