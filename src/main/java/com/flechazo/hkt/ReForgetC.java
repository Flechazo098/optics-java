package com.flechazo.hkt;

import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ReForgetC<R, A, B> extends App2<ReForgetC.Mu<R>, A, B> {
    final class Mu<R> implements K2 {
        private Mu() {
        }
    }

    static <R, A, B> ReForgetC<R, A, B> of(Either<Function<R, B>, BiFunction<A, R, B>> impl) {
        Objects.requireNonNull(impl, "impl");
        return () -> impl;
    }

    static <R, A, B> ReForgetC<R, A, B> fromContext(Function<R, B> function) {
        return of(Either.left(Objects.requireNonNull(function, "function")));
    }

    static <R, A, B> ReForgetC<R, A, B> fromFocus(BiFunction<A, R, B> function) {
        return of(Either.right(Objects.requireNonNull(function, "function")));
    }

    static <R, A, B> ReForgetC<R, A, B> unbox(App2<Mu<R>, A, B> value) {
        return (ReForgetC<R, A, B>) Validation.kind().narrowWithTypeCheck2(value, ReForgetC.class);
    }

    Either<Function<R, B>, BiFunction<A, R, B>> impl();

    default B run(A value, R context) {
        return impl().fold(fromContext -> fromContext.apply(context), fromFocus -> fromFocus.apply(value, context));
    }

    final class Instance<R> implements AffineP<ReForgetC.Mu<R>, Instance.Mu> {
        public static final class Mu implements AffineP.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, B, C, D> FunctionArrow<App2<ReForgetC.Mu<R>, A, B>, App2<ReForgetC.Mu<R>, C, D>> dimap(
                Function<? super C, ? extends A> left,
                Function<? super B, ? extends D> right) {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
            return FunctionArrow.of(value -> ReForgetC.of(ReForgetC.unbox(value).impl().mapBoth(
                    fromContext -> context -> right.apply(fromContext.apply(context)),
                    fromFocus -> (input, context) -> right.apply(fromFocus.apply(left.apply(input), context)))));
        }

        @Override
        public <A, B, C> App2<ReForgetC.Mu<R>, Tuple2<A, C>, Tuple2<B, C>> first(
                App2<ReForgetC.Mu<R>, A, B> value) {
            Either<Function<R, B>, BiFunction<A, R, B>> impl = ReForgetC.unbox(value).impl();
            return ReForgetC.of(impl.fold(
                    fromContext -> Either.right((Tuple2<A, C> Tuple2, R context) ->
                            Tuple2.of(fromContext.apply(context), Tuple2.second())),
                    fromFocus -> Either.right((Tuple2<A, C> Tuple2, R context) ->
                            Tuple2.of(fromFocus.apply(Tuple2.first(), context), Tuple2.second()))));
        }

        @Override
        public <A, B, C> App2<ReForgetC.Mu<R>, Either<A, C>, Either<B, C>> left(
                App2<ReForgetC.Mu<R>, A, B> value) {
            return ReForgetC.of(ReForgetC.unbox(value).impl().mapBoth(
                    fromContext -> context -> Either.left(fromContext.apply(context)),
                    fromFocus -> (either, context) -> either.mapLeft(left -> fromFocus.apply(left, context))));
        }
    }
}
