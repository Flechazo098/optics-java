package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;

public interface Traversable<T extends K1, Proof extends Traversable.Mu> extends Functor<T, Proof>, Foldable<T> {
    interface Mu extends Functor.Mu {
    }

    static <T extends K1, Proof extends Mu> Traversable<T, Proof> unbox(App<Proof, T> proofBox) {
        return (Traversable<T, Proof>) Validation.kind().narrowWithTypeCheck(proofBox, Traversable.class);
    }

    <F extends K1, A, B> App<F, App<T, B>> traverse(
            Applicative<F, ?> applicative,
            Function<? super A, ? extends App<F, B>> f,
            App<T, A> value);

    default <F extends K1, A> App<F, App<T, A>> sequence(
            Applicative<F, ?> applicative,
            App<T, ? extends App<F, A>> value) {
        Objects.requireNonNull(applicative, "applicative");
        return traverse(applicative, Function.identity(), narrowApp(value));
    }

    @Override
    default <A, B> App<T, B> map(Function<? super A, ? extends B> f, App<T, A> fa) {
        return IdF.get(traverse(IdF.applicative(), a -> IdF.of(f.apply(a)), fa));
    }

    @SuppressWarnings("unchecked")
    private static <T extends K1, F extends K1, A> App<T, App<F, A>> narrowApp(
            App<T, ? extends App<F, A>> value) {
        return (App<T, App<F, A>>) value;
    }
}
