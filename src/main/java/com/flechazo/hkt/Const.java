package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;

public record Const<M, A>(M value) implements App<Const.Mu<M>, A> {
    public Const {
        Objects.requireNonNull(value, "value");
    }

    public static final class Mu<M> implements K1 {
        private Mu() {
        }
    }

    public static <M, A> Const<M, A> of(M value) {
        return new Const<>(value);
    }

    public static <M, A> Const<M, A> unbox(App<Mu<M>, A> value) {
        return (Const<M, A>) Validation.kind().narrowWithTypeCheck(value, Const.class);
    }

    public static <M, A> M get(App<Mu<M>, A> value) {
        return unbox(value).value();
    }

    public static <M> Applicative<Mu<M>, Instance.Mu> applicative(Monoid<M> monoid) {
        Objects.requireNonNull(monoid, "monoid");
        return new Instance<>(monoid);
    }

    public static final class Instance<M> implements Applicative<Const.Mu<M>, Instance.Mu> {
        private final Monoid<M> monoid;

        private Instance(Monoid<M> monoid) {
            this.monoid = Objects.requireNonNull(monoid, "monoid");
        }

        public static final class Mu implements Applicative.Mu {
            private Mu() {
            }
        }

        @Override
        public <A> App<Const.Mu<M>, A> of(A value) {
            Objects.requireNonNull(value, "value");
            return new Const<>(monoid.empty());
        }

        @Override
        public <A, B> App<Const.Mu<M>, B> map(
                Function<? super A, ? extends B> f, App<Const.Mu<M>, A> fa) {
            return new Const<>(unbox(fa).value());
        }

        @Override
        public <A, B> App<Const.Mu<M>, B> ap(
                App<Const.Mu<M>, ? extends Function<A, B>> ff, App<Const.Mu<M>, A> fa) {
            return new Const<>(monoid.combine(unbox(ff).value(), unbox(fa).value()));
        }
    }
}
