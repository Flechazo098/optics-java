package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public record Pair<A, B>(A first, B second) implements App<Pair.Mu<B>, A> {
    public Pair {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
    }

    public static final class Mu<B> implements K1 {
        private Mu() {
        }
    }

    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<>(first, second);
    }

    public static <A, B> Pair<A, B> unbox(App<Mu<B>, A> value) {
        return (Pair<A, B>) value;
    }

    public static <B> Instance<B> instance() {
        return new Instance<>();
    }

    public <C> Pair<C, B> mapFirst(Function<? super A, ? extends C> f) {
        return new Pair<>(f.apply(first), second);
    }

    public <C> Pair<A, C> mapSecond(Function<? super B, ? extends C> f) {
        return new Pair<>(first, f.apply(second));
    }

    public <C, D> Pair<C, D> mapBoth(
            Function<? super A, ? extends C> firstMapper, Function<? super B, ? extends D> secondMapper) {
        return new Pair<>(firstMapper.apply(first), secondMapper.apply(second));
    }

    public <C> C fold(BiFunction<? super A, ? super B, ? extends C> f) {
        return f.apply(first, second);
    }

    public Pair<B, A> swap() {
        return new Pair<>(second, first);
    }

    public static final class Instance<B> implements Traversable<Mu<B>, Instance.Mu> {
        public static final class Mu implements Traversable.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<Pair.Mu<B>, A> value) {
            return f.apply(Pair.unbox(value).first());
        }

        @Override
        public <F extends K1, A, C> App<F, App<Pair.Mu<B>, C>> traverse(
                Applicative<F, ?> applicative,
                Function<? super A, ? extends App<F, C>> f,
                App<Pair.Mu<B>, A> value) {
            Pair<A, B> pair = Pair.unbox(value);
            return applicative.map(mapped -> Pair.of(mapped, pair.second()), f.apply(pair.first()));
        }
    }
}
