package com.flechazo.hkt;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public record Pair<A, B>(@Nullable A first, @Nullable B second) {
    public static <A, B> Pair<A, B> of(@Nullable A first, @Nullable B second) {
        return new Pair<>(first, second);
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
}
