package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public record Const<M, A>(@Nullable M value) implements App<Const.Mu<M>, A> {
    public static final class Mu<M> implements K1 {
        private Mu() {
        }
    }

    public static <M, A> Const<M, A> of(@Nullable M value) {
        return new Const<>(value);
    }

    public static <M, A> Const<M, A> unbox(App<Mu<M>, A> value) {
        return (Const<M, A>) Objects.requireNonNull(value, "value");
    }

    public static <M> Applicative<Mu<M>> applicative(Monoid<M> monoid) {
        Objects.requireNonNull(monoid, "monoid");
        return new Applicative<>() {
            @Override
            public <A> App<Mu<M>, A> of(@Nullable A value) {
                return new Const<>(monoid.empty());
            }

            @Override
            public <A, B> App<Mu<M>, B> map(
                    Function<? super A, ? extends B> f, App<Mu<M>, A> fa) {
                return new Const<>(unbox(fa).value());
            }

            @Override
            public <A, B> App<Mu<M>, B> ap(
                    App<Mu<M>, ? extends Function<A, B>> ff, App<Mu<M>, A> fa) {
                return new Const<>(monoid.combine(unbox(ff).value(), unbox(fa).value()));
            }
        };
    }
}
