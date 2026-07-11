package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface PAffine<S, T, A, B> extends Optic<S, T, A, B> {
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

    default PTraversal<S, T, A, B> asTraversal() {
        PAffine<S, T, A, B> self = this;
        PTraversal<S, T, A, B> direct = self::modifyF;
        return OpticPrograms.traversal(direct, OpticPrograms.programOrOpaque(self, "affine"));
    }

    default PSetter<S, T, A, B> asSetter() {
        PAffine<S, T, A, B> self = this;
        PSetter<S, T, A, B> direct = self::modify;
        return OpticPrograms.setter(direct, OpticPrograms.programOrOpaque(self, "affine"));
    }

    default Fold<S, A> asFold() {
        PAffine<S, T, A, B> self = this;
        Fold<S, A> fold = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                Either<T, A> value = self.preview(source);
                return value.isRight() ? f.apply(value.right()) : monoid.empty();
            }
        };
        Fold<S, A> typed = OpticMetadata.fold(
                fold,
                OpticMetadata.<S, T, A, B>optic(self)
                        .map(optic -> PointFreeFold.fromOptic(optic, fold)));
        return OpticPrograms.fold(typed, OpticPrograms.programOrOpaque(self, "affine"));
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

    default <C, D> PAffine<S, T, C, D> andThen(PAffine<A, B, C, D> other) {
        PAffine<S, T, A, B> self = this;
        PAffine<S, T, C, D> typed = OpticMetadata.optic(PAffine.of(
                source -> self.preview(source)
                        .fold(Either::left, focus -> other.preview(focus).mapLeft(next -> self.set(next, source))),
                (source, value) -> self.preview(source)
                        .fold(Function.identity(), focus -> self.set(other.set(value, focus), source))),
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.affine(typed, OpticPrograms.compose(self, other));
    }

    default <C, D> PAffine<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PAffine<S, T, C, D> direct = PAffine.of(
                source -> preview(source).map(other::get),
                (source, value) -> set(other.reverseGet(value), source));
        return OpticPrograms.affine(direct, OpticPrograms.compose(this, other));
    }

    default <C> Fold<S, C> andThen(Fold<A, C> fold) {
        return asFold().andThen(fold);
    }

    default <C, D> PAffine<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        PAffine<S, T, A, B> self = this;
        PAffine<S, T, C, D> direct = PAffine.of(
                source -> self.preview(source).map(other::get),
                (source, value) -> self.preview(source)
                        .fold(Function.identity(), focus -> self.set(other.set(value, focus), source)));
        return OpticPrograms.affine(direct, OpticPrograms.compose(this, other));
    }

    default <C, D> PAffine<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        PAffine<S, T, A, B> self = this;
        PAffine<S, T, C, D> direct = PAffine.of(
                source -> self.preview(source)
                        .fold(Either::left, focus -> other.match(focus).mapLeft(next -> self.set(next, source))),
                (source, value) -> self.preview(source)
                        .fold(Function.identity(), focus -> self.set(other.build(value), source)));
        return OpticPrograms.affine(direct, OpticPrograms.compose(this, other));
    }

    default <C, D> PTraversal<S, T, C, D> andThen(PTraversal<A, B, C, D> other) {
        PAffine<S, T, A, B> self = this;
        PTraversal<S, T, C, D> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                Either<T, A> value = self.preview(source);
                return value.isRight()
                        ? applicative.map(next -> self.set(next, source), other.modifyF(f, value.right(), applicative))
                        : applicative.of(value.left());
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.compose(self, other));
    }

    static <K, V> PAffine<Map<K, V>, Map<K, V>, V, V> mapValue(K key) {
        PAffine<Map<K, V>, Map<K, V>, V, V> direct = new PAffine<>() {
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
        return OpticPrograms.affine(direct, OpticPrograms.structured("mapKeyAffine", key));
    }

    static <A> PAffine<List<A>, List<A>, A, A> listAt(int index) {
        PAffine<List<A>, List<A>, A, A> direct = new PAffine<>() {
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
        return OpticPrograms.affine(direct, OpticPrograms.structured("listIndexAffine", index));
    }

    static <S, T, A, B> PAffine<S, T, A, B> of(
            AffinePreview<? super S, T, A> preview,
            AffineRebuilder<S, B, T> setter) {
        PAffine<S, T, A, B> direct = of(
                (Function<? super S, Either<T, A>>) preview,
                (BiFunction<S, B, T>) setter);
        return LambdaLifter.affine(direct, preview, setter);
    }

    static <S, T, A, B> PAffine<S, T, A, B> of(
            Function<? super S, Either<T, A>> preview,
            BiFunction<S, B, T> setter) {
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(setter, "setter");
        PAffine<S, T, A, B> direct = new PAffine<>() {
            @Override
            public Either<T, A> preview(S source) {
                return preview.apply(source);
            }

            @Override
            public T set(B value, S source) {
                return setter.apply(source, value);
            }
        };
        return OpticPrograms.affine(direct, OpticPrograms.opaque("affine", null));
    }

    static <S, T, A, B> PAffine<S, T, A, B> fromLensAndPrism(
            PLens<S, T, A, B> lens,
            PPrism<A, B, A, B> prism) {
        return lens.andThen(prism);
    }
}
