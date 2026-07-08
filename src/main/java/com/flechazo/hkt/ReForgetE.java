package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface ReForgetE<R, A, B> extends App2<ReForgetE.Mu<R>, A, B> {
    final class Mu<R> implements K2 {
        private Mu() {
        }
    }

    static <R, A, B> ReForgetE<R, A, B> of(Function<? super Either<A, R>, ? extends B> function) {
        Objects.requireNonNull(function, "function");
        return function::apply;
    }

    static <R, A, B> ReForgetE<R, A, B> unbox(App2<Mu<R>, A, B> value) {
        return (ReForgetE<R, A, B>) Validation.kind().narrowWithTypeCheck2(value, ReForgetE.class);
    }

    B run(Either<A, R> value);

    final class Instance<R> implements Cocartesian<ReForgetE.Mu<R>, Instance.Mu> {
        public static final class Mu implements Cocartesian.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, B, C, D> FunctionArrow<App2<ReForgetE.Mu<R>, A, B>, App2<ReForgetE.Mu<R>, C, D>> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right) {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            return FunctionArrow.of(value -> {
                ReForgetE<R, A, B> reForget = ReForgetE.unbox(value);
                return ReForgetE.of(either -> right.apply(reForget.run(either.mapLeft(left))));
            });
        }

        @Override
        public <A, B, C> App2<ReForgetE.Mu<R>, Either<A, C>, Either<B, C>> left(
                App2<ReForgetE.Mu<R>, A, B> value) {
            ReForgetE<R, A, B> reForget = ReForgetE.unbox(value);
            return ReForgetE.of(either -> either.fold(
                    nested -> nested.fold(
                            left -> Either.left(reForget.run(Either.left(left))),
                            Either::right),
                    context -> Either.left(reForget.run(Either.right(context)))));
        }
    }
}
