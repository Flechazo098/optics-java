package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public interface PPrism<S, T, A, B> extends Optic<S, T, A, B> {
    Either<T, A> match(S source);

    T build(B value);

    default Maybe<A> getMaybe(S source) {
        Either<T, A> value = match(source);
        return value.isRight() ? Maybe.some(value.right()) : Maybe.none();
    }

    @Override
    default <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
        Either<T, A> value = match(source);
        return value.isRight()
                ? applicative.map(this::build, f.apply(value.right()))
                : applicative.of(value.left());
    }

    default T modify(Function<? super A, ? extends B> f, S source) {
        Either<T, A> value = match(source);
        return value.isRight() ? build(f.apply(value.right())) : value.left();
    }

    default T set(B value, S source) {
        return modify(ignored -> value, source);
    }

    default boolean matches(S source) {
        return match(source).isRight();
    }

    default boolean doesNotMatch(S source) {
        return !matches(source);
    }

    default T modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends B> modifier,
            Function<? super A, ? extends B> otherwise,
            S source) {
        Either<T, A> value = match(source);
        if (value.isLeft()) {
            return value.left();
        }
        A focus = value.right();
        return build(predicate.test(focus) ? modifier.apply(focus) : otherwise.apply(focus));
    }

    default PTraversal<S, T, A, B> asTraversal() {
        PPrism<S, T, A, B> self = this;
        PTraversal<S, T, A, B> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(f, source, applicative);
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.programOrOpaque(self, "prism"));
    }

    default PSetter<S, T, A, B> asSetter() {
        PPrism<S, T, A, B> self = this;
        PSetter<S, T, A, B> direct = self::modify;
        return OpticPrograms.setter(direct, OpticPrograms.programOrOpaque(self, "prism"));
    }

    default Fold<S, A> asFold() {
        PPrism<S, T, A, B> self = this;
        Fold<S, A> fold = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                Either<T, A> value = self.match(source);
                return value.isRight() ? f.apply(value.right()) : monoid.empty();
            }
        };
        Fold<S, A> typed = OpticMetadata.fold(
                fold,
                OpticMetadata.<S, T, A, B>optic(self)
                        .map(optic -> PointFreeFold.fromOptic(optic, fold)));
        return OpticPrograms.fold(typed, OpticPrograms.programOrOpaque(self, "prism"));
    }

    default <C, D> PPrism<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        PPrism<S, T, A, B> self = this;
        PPrism<S, T, C, D> typed = OpticMetadata.optic(PPrism.<S, T, C, D>of(
                source -> self.match(source).fold(Either::left, focus -> other.match(focus).mapLeft(self::build)),
                value -> self.build(other.build(value))),
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.prism(typed, OpticPrograms.compose(self, other));
    }

    default <C, D> PPrism<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PPrism<S, T, C, D> direct = PPrism.of(
                source -> match(source).map(other::get),
                value -> build(other.reverseGet(value)));
        return OpticPrograms.prism(direct, OpticPrograms.compose(this, other));
    }

    default <C> Fold<S, C> andThen(Fold<A, C> other) {
        return asFold().andThen(other);
    }

    default <C, D> PAffine<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        PAffine<S, T, C, D> typed = OpticMetadata.optic(PAffine.<S, T, C, D>of(
                source -> match(source).fold(Either::left, focus -> Either.right(other.get(focus))),
                (source, value) -> match(source).fold(Function.identity(), focus -> build(other.set(value, focus)))),
                OpticMetadata.<S, T, A, B>optic(this)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.affine(typed, OpticPrograms.compose(this, other));
    }

    default <C, D> PAffine<S, T, C, D> andThen(PAffine<A, B, C, D> other) {
        PAffine<S, T, C, D> typed = OpticMetadata.optic(PAffine.<S, T, C, D>of(
                source -> match(source)
                        .fold(Either::left, focus -> other.preview(focus).mapLeft(this::build)),
                (source, value) -> match(source)
                        .fold(Function.identity(), focus -> build(other.set(value, focus)))),
                OpticMetadata.<S, T, A, B>optic(this)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.affine(typed, OpticPrograms.compose(this, other));
    }

    default <C, D> PTraversal<S, T, C, D> andThen(PTraversal<A, B, C, D> other) {
        PPrism<S, T, A, B> self = this;
        PTraversal<S, T, C, D> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                Either<T, A> value = self.match(source);
                return value.isRight()
                        ? applicative.map(self::build, other.modifyF(f, value.right(), applicative))
                        : applicative.of(value.left());
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.compose(self, other));
    }

    static <S, T, A, B> PPrism<S, T, A, B> of(
            PrismMatcher<? super S, T, A> match,
            PrismBuilder<? super B, ? extends T> build) {
        PPrism<S, T, A, B> direct = of(
                (Function<? super S, Either<T, A>>) match,
                (Function<? super B, ? extends T>) build);
        return LambdaLifter.prism(direct, match, build);
    }

    static <S, T, A, B> PPrism<S, T, A, B> of(
            Function<? super S, Either<T, A>> match,
            Function<? super B, ? extends T> build) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(build, "build");
        PPrism<S, T, A, B> direct = new PPrism<>() {
            @Override
            public Either<T, A> match(S source) {
                return match.apply(source);
            }

            @Override
            public T build(B value) {
                return build.apply(value);
            }
        };
        return OpticPrograms.prism(direct, OpticPrograms.opaque("prism", null));
    }
}
