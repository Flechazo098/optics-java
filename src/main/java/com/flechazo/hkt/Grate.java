package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface Grate<S, T, A, B> extends App2<Grate.Mu<A, B>, S, T> {
    final class Mu<A, B> implements K2 {
        private Mu() {
        }
    }

    static <S, T, A, B> Grate<S, T, A, B> of(Function<? super Function<Function<S, A>, B>, ? extends T> grate) {
        Objects.requireNonNull(grate, "grate");
        return grate::apply;
    }

    static <S, T, A, B> Grate<S, T, A, B> unbox(App2<Mu<A, B>, S, T> value) {
        return (Grate<S, T, A, B>) Validation.kind().narrowWithTypeCheck2(value, Grate.class);
    }

    T grate(Function<Function<S, A>, B> function);

    final class Instance<A2, B2> implements Closed<Grate.Mu<A2, B2>, Instance.Mu> {
        public static final class Mu implements Closed.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, B, C, D> FunctionArrow<App2<Grate.Mu<A2, B2>, A, B>, App2<Grate.Mu<A2, B2>, C, D>> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right) {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            return FunctionArrow.of(value -> {
                Grate<A, B, A2, B2> grate = Grate.unbox(value);
                return Grate.of(function ->
                        right.apply(grate.grate(next -> function.apply(source -> next.apply(left.apply(source))))));
            });
        }

        @Override
        public <A, B, X> App2<Grate.Mu<A2, B2>, Function<X, A>, Function<X, B>> closed(
                App2<Grate.Mu<A2, B2>, A, B> value) {
            Grate<A, B, A2, B2> grate = Grate.unbox(value);
            return Grate.of(function -> input -> grate.grate(next -> function.apply(reader -> next.apply(reader.apply(input)))));
        }
    }
}
