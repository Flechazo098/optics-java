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

/**
 * Represents a polymorphic affine optic that focuses on at most one value.
 *
 * @param <S> the input source type
 * @param <T> the rebuilt source type
 * @param <A> the input focus type
 * @param <B> the replacement focus type
 */
public interface PAffine<S, T, A, B> extends Optic<S, T, A, B> {
    /**
     * Previews a source as either a nonmatching rebuilt source or a focus.
     *
     * @param source the source to preview
     * @return a right value containing the focus when present, otherwise a left rebuilt source
     */
    Either<T, A> preview(S source);

    /**
     * Replaces a present focus and returns the rebuilt source.
     *
     * @param value the replacement focus
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    T set(B value, S source);

    /**
     * Returns the focus when present.
     *
     * @param source the source to preview
     * @return the focus, or an empty value when absent
     */
    default Maybe<A> getMaybe(S source) {
        Either<T, A> value = preview(source);
        return value.isRight() ? Maybe.some(value.right()) : Maybe.none();
    }

    /**
     * Transforms a present focus and leaves an absent source unchanged according to
     * {@link #preview(Object)}.
     *
     * @param f the focus transformation
     * @param source the source to transform
     * @return the rebuilt source
     */
    default T modify(Function<? super A, ? extends B> f, S source) {
        Either<T, A> value = preview(source);
        return value.isRight() ? set(f.apply(value.right()), source) : value.left();
    }

    /**
     * Applies an applicative transformation when a focus is present.
     *
     * @param <F> the applicative witness type
     * @param f the effectful focus transformation
     * @param source the source to transform
     * @param applicative the applicative used to rebuild the source
     * @return the rebuilt source in the applicative context
     */
    @Override
    default <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
        Either<T, A> value = preview(source);
        return value.isRight()
                ? applicative.map(next -> set(next, source), f.apply(value.right()))
                : applicative.of(value.left());
    }

    /**
     * Returns a traversal containing zero or one focus.
     *
     * @return the traversal view
     */
    default PTraversal<S, T, A, B> asTraversal() {
        PAffine<S, T, A, B> self = this;
        PTraversal<S, T, A, B> direct = self::modifyF;
        return OpticPrograms.traversal(direct, OpticPrograms.programOrOpaque(self, "affine"));
    }

    /**
     * Returns a setter with this affine optic's conditional update behavior.
     *
     * @return the setter view
     */
    default PSetter<S, T, A, B> asSetter() {
        PAffine<S, T, A, B> self = this;
        PSetter<S, T, A, B> direct = self::modify;
        return OpticPrograms.setter(direct, OpticPrograms.programOrOpaque(self, "affine"));
    }

    /**
     * Returns a fold containing the focus when present.
     *
     * @return the fold view
     */
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

    /**
     * Determines whether a focus is present.
     *
     * @param source the source to test
     * @return {@code true} when the focus is present
     */
    default boolean matches(S source) {
        return preview(source).isRight();
    }

    /**
     * Determines whether a focus is absent.
     *
     * @param source the source to test
     * @return {@code true} when the focus is absent
     */
    default boolean doesNotMatch(S source) {
        return !matches(source);
    }

    /**
     * Selects one of two transformations for a present focus.
     *
     * @param predicate the condition applied to the focus
     * @param modifier the transformation used when the condition is true
     * @param otherwise the transformation used when the condition is false
     * @param source the source to transform
     * @return the rebuilt source
     */
    default T modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends B> modifier,
            Function<? super A, ? extends B> otherwise,
            S source) {
        Either<T, A> value = preview(source);
        if (value.isLeft()) {
            return value.left();
        }
        A focus = value.right();
        return set(predicate.test(focus) ? modifier.apply(focus) : otherwise.apply(focus), source);
    }

    /**
     * Composes this affine optic with another affine optic.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the affine optic applied to a present focus
     * @return the composed affine optic
     */
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

    /**
     * Composes this affine optic with an isomorphism.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the isomorphism applied to a present focus
     * @return the composed affine optic
     */
    default <C, D> PAffine<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PAffine<S, T, C, D> direct = PAffine.of(
                source -> preview(source).map(other::get),
                (source, value) -> set(other.reverseGet(value), source));
        return OpticPrograms.affine(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this affine optic with a fold.
     *
     * @param <C> the observed focus type
     * @param fold the fold applied to a present focus
     * @return the composed fold
     */
    default <C> Fold<S, C> andThen(Fold<A, C> fold) {
        return asFold().andThen(fold);
    }

    /**
     * Composes this affine optic with a lens.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the lens applied to a present focus
     * @return the composed affine optic
     */
    default <C, D> PAffine<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        PAffine<S, T, A, B> self = this;
        PAffine<S, T, C, D> direct = PAffine.of(
                source -> self.preview(source).map(other::get),
                (source, value) -> self.preview(source)
                        .fold(Function.identity(), focus -> self.set(other.set(value, focus), source)));
        return OpticPrograms.affine(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this affine optic with a prism.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the prism applied to a present focus
     * @return the composed affine optic
     */
    default <C, D> PAffine<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        PAffine<S, T, A, B> self = this;
        PAffine<S, T, C, D> direct = PAffine.of(
                source -> self.preview(source)
                        .fold(Either::left, focus -> other.match(focus).mapLeft(next -> self.set(next, source))),
                (source, value) -> self.preview(source)
                        .fold(Function.identity(), focus -> self.set(other.build(value), source)));
        return OpticPrograms.affine(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this affine optic with a traversal.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the traversal applied to a present focus
     * @return the composed traversal
     */
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

    /**
     * Returns an affine optic focusing on a map value when a key is present.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param key the focused key
     * @return the map-value affine optic
     */
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

    /**
     * Returns an affine optic focusing on a list element when an index is in range.
     *
     * @param <A> the element type
     * @param index the zero-based focused index
     * @return the list-index affine optic
     */
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

    /**
     * Creates an affine optic from serializable preview and rebuild operations.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param preview the partial focus reader
     * @param setter the source rebuild operation
     * @return the resulting affine optic
     */
    static <S, T, A, B> PAffine<S, T, A, B> of(
            AffinePreview<? super S, T, A> preview,
            AffineRebuilder<S, B, T> setter) {
        PAffine<S, T, A, B> direct = of(
                (Function<? super S, Either<T, A>>) preview,
                (BiFunction<S, B, T>) setter);
        return LambdaLifter.affine(direct, preview, setter);
    }

    /**
     * Creates an affine optic from preview and rebuild operations.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param preview the partial focus reader
     * @param setter the source rebuild operation
     * @return the resulting affine optic
     */
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

    /**
     * Composes a lens and prism as an affine optic.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param lens the lens applied first
     * @param prism the prism applied to the lens focus
     * @return the composed affine optic
     */
    static <S, T, A, B> PAffine<S, T, A, B> fromLensAndPrism(
            PLens<S, T, A, B> lens,
            PPrism<A, B, A, B> prism) {
        return lens.andThen(prism);
    }
}
