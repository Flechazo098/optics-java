package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.hkt.functions.PointFreeOptic;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Affine<S, T, A, B> extends Optic<S, T, A, B> {
    Either<T, A> preview(S source);

    T set(B value, S source);

    default Maybe<A> getMaybe(S source) {
        Either<T, A> value = preview(source);
        return value.isRight() ? Maybe.some(value.right()) : Maybe.none();
    }

    default T modify(Function<? super A, ? extends B> f, S source) {
        Either<T, A> value = preview(source);
        return value.isRight() ? set(f.apply(value.right()), source) : value.left();
    }

    @Override
    default <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
        Either<T, A> value = preview(source);
        return value.isRight()
                ? applicative.map(next -> set(next, source), f.apply(value.right()))
                : applicative.of(value.left());
    }

    default Traversal<S, T, A, B> asTraversal() {
        Affine<S, T, A, B> self = this;
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
        Affine<S, T, A, B> self = this;
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
        Affine<S, T, A, B> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                Either<T, A> value = self.preview(source);
                return value.isRight() ? f.apply(value.right()) : monoid.empty();
            }

            @Override
            public Maybe<PointFreeFold<S, A>> typedFold() {
                return self.typedOptic().map(optic -> PointFreeFold.fromOptic(optic, this));
            }
        };
    }

    default boolean matches(S source) {
        return preview(source).isRight();
    }

    default boolean doesNotMatch(S source) {
        return !matches(source);
    }

    default T modifyWhen(Predicate<? super A> predicate, Function<? super A, ? extends B> f, S source) {
        Either<T, A> value = preview(source);
        return value.isRight() && predicate.test(value.right()) ? set(f.apply(value.right()), source) : value.left();
    }

    default <C, D> Affine<S, T, C, D> andThen(Affine<A, B, C, D> other) {
        Affine<S, T, A, B> self = this;
        return Affine.<S, T, C, D>of(
                source -> self.preview(source)
                        .fold(Either::left, focus -> other.preview(focus).mapLeft(next -> self.set(next, source))),
                (source, value) -> self.preview(source)
                        .fold(Function.identity(), focus -> self.set(other.set(value, focus), source)))
                .withTypedOptic(self.typedOptic().flatMap(left -> other.typedOptic().map(left::andThen)));
    }

    default <C> Fold<S, C> andThen(Fold<A, C> fold) {
        return asFold().andThen(fold);
    }

    default <C, D> Affine<S, T, C, D> andThen(Lens<A, B, C, D> other) {
        return andThen(Affine.<A, B, C, D>of(
                source -> Either.right(other.get(source)),
                (source, value) -> other.set(value, source))
                .withTypedOptic(other.typedOptic()));
    }

    default <C, D> Affine<S, T, C, D> andThen(Prism<A, B, C, D> other) {
        return andThen(Affine.<A, B, C, D>of(other::match, (source, value) -> other.build(value))
                .withTypedOptic(other.typedOptic()));
    }

    default <C, D> Traversal<S, T, C, D> andThen(Traversal<A, B, C, D> other) {
        Affine<S, T, A, B> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                Either<T, A> value = self.preview(source);
                return value.isRight()
                        ? applicative.map(next -> self.set(next, source), other.modifyF(f, value.right(), applicative))
                        : applicative.of(value.left());
            }

            @Override
            public Maybe<PointFreeOptic<S, T, C, D>> typedOptic() {
                return self.typedOptic().flatMap(left -> other.typedOptic().map(left::andThen));
            }
        };
    }

    default Affine<S, T, A, B> withTypedOptic(Maybe<PointFreeOptic<S, T, A, B>> optic) {
        Affine<S, T, A, B> self = this;
        return new Affine<>() {
            @Override
            public Either<T, A> preview(S source) {
                return self.preview(source);
            }

            @Override
            public T set(B value, S source) {
                return self.set(value, source);
            }

            @Override
            public Maybe<PointFreeOptic<S, T, A, B>> typedOptic() {
                return optic;
            }
        };
    }

    static <K, V> Affine<Map<K, V>, Map<K, V>, V, V> mapValue(K key) {
        return new Affine<>() {
            @Override
            public Either<Map<K, V>, V> preview(Map<K, V> source) {
                return source.containsKey(key) ? Either.right(source.get(key)) : Either.left(source);
            }

            @Override
            public Map<K, V> set(V value, Map<K, V> source) {
                if (!source.containsKey(key)) {
                    return source;
                }
                LinkedHashMap<K, V> copy = new LinkedHashMap<>(source);
                copy.put(key, value);
                return copy;
            }
        };
    }

    static <A> Affine<List<A>, List<A>, A, A> listAt(int index) {
        return new Affine<>() {
            @Override
            public Either<List<A>, A> preview(List<A> source) {
                return index >= 0 && index < source.size() ? Either.right(source.get(index)) : Either.left(source);
            }

            @Override
            public List<A> set(A value, List<A> source) {
                if (index < 0 || index >= source.size()) {
                    return source;
                }
                ArrayList<A> copy = new ArrayList<>(source);
                copy.set(index, value);
                return copy;
            }
        };
    }

    static <S, T, A, B> Affine<S, T, A, B> of(
            Function<? super S, Either<T, A>> preview,
            BiFunction<S, B, T> setter) {
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(setter, "setter");
        return new Affine<>() {
            @Override
            public Either<T, A> preview(S source) {
                return preview.apply(source);
            }

            @Override
            public T set(B value, S source) {
                return setter.apply(source, value);
            }
        };
    }

    static <S, T, A, B> Affine<S, T, A, B> fromLensAndPrism(
            Lens<S, T, A, B> lens,
            Prism<A, B, A, B> prism) {
        return lens.andThen(prism);
    }
}
