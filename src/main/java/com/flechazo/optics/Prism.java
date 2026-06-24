package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeOptic;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Prism<S, T, A, B> extends Optic<S, T, A, B> {
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

    default T modifyWhen(Predicate<? super A> predicate, Function<? super A, ? extends B> f, S source) {
        Either<T, A> value = match(source);
        return value.isRight() && predicate.test(value.right()) ? build(f.apply(value.right())) : value.left();
    }

    default Traversal<S, T, A, B> asTraversal() {
        Prism<S, T, A, B> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(f, source, applicative);
            }

            @Override
            public Maybe<PointFreeOptic<S, T, A, B>> typedOptic() {
                return self.typedOptic();
            }
        };
    }

    default Setter<S, T, A, B> asSetter() {
        Prism<S, T, A, B> self = this;
        return new Setter<>() {
            @Override
            public T modify(Function<? super A, ? extends B> f, S source) {
                return self.modify(f, source);
            }

            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(f, source, applicative);
            }

            @Override
            public Maybe<PointFreeOptic<S, T, A, B>> typedOptic() {
                return self.typedOptic();
            }
        };
    }

    default Fold<S, A> asFold() {
        Prism<S, T, A, B> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                Either<T, A> value = self.match(source);
                return value.isRight() ? f.apply(value.right()) : monoid.empty();
            }
        };
    }

    default <C, D> Prism<S, T, C, D> andThen(Prism<A, B, C, D> other) {
        Prism<S, T, A, B> self = this;
        return Prism.<S, T, C, D>of(
                source -> self.match(source).fold(Either::left, focus -> other.match(focus).mapLeft(self::build)),
                value -> self.build(other.build(value)))
                .withTypedOptic(self.typedOptic().flatMap(left -> other.typedOptic().map(left::andThen)));
    }

    default <C> Fold<S, C> andThen(Fold<A, C> other) {
        return asFold().andThen(other);
    }

    default <C, D> Affine<S, T, C, D> andThen(Lens<A, B, C, D> other) {
        return Affine.<S, T, C, D>of(
                source -> match(source).fold(Either::left, focus -> Either.right(other.get(focus))),
                (source, value) -> match(source).fold(Function.identity(), focus -> build(other.set(value, focus))))
                .withTypedOptic(typedOptic().flatMap(left -> other.typedOptic().map(left::andThen)));
    }

    default <C, D> Affine<S, T, C, D> andThen(Affine<A, B, C, D> other) {
        return Affine.<S, T, C, D>of(
                source -> match(source)
                        .fold(Either::left, focus -> other.preview(focus).mapLeft(this::build)),
                (source, value) -> match(source)
                        .fold(Function.identity(), focus -> build(other.set(value, focus))))
                .withTypedOptic(typedOptic().flatMap(left -> other.typedOptic().map(left::andThen)));
    }

    default <C, D> Traversal<S, T, C, D> andThen(Traversal<A, B, C, D> other) {
        Prism<S, T, A, B> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                Either<T, A> value = self.match(source);
                return value.isRight()
                        ? applicative.map(self::build, other.modifyF(f, value.right(), applicative))
                        : applicative.of(value.left());
            }

            @Override
            public Maybe<PointFreeOptic<S, T, C, D>> typedOptic() {
                return self.typedOptic().flatMap(left -> other.typedOptic().map(left::andThen));
            }
        };
    }

    default Prism<S, T, A, B> withTypedOptic(Maybe<PointFreeOptic<S, T, A, B>> optic) {
        Prism<S, T, A, B> self = this;
        return new Prism<>() {
            @Override
            public Either<T, A> match(S source) {
                return self.match(source);
            }

            @Override
            public T build(B value) {
                return self.build(value);
            }

            @Override
            public Maybe<PointFreeOptic<S, T, A, B>> typedOptic() {
                return optic;
            }
        };
    }

    static <S, T, A, B> Prism<S, T, A, B> of(
            Function<? super S, Either<T, A>> match,
            Function<? super B, ? extends T> build) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(build, "build");
        return new Prism<>() {
            @Override
            public Either<T, A> match(S source) {
                return match.apply(source);
            }

            @Override
            public T build(B value) {
                return build.apply(value);
            }
        };
    }
}
