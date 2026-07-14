package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a polymorphic prism that may match one focus and can build a matching source.
 *
 * @param <S> the input source type
 * @param <T> the rebuilt source type
 * @param <A> the matched focus type
 * @param <B> the replacement focus type
 */
public interface PPrism<S, T, A, B> extends Optic<S, T, A, B> {
    /**
     * Matches a source as either a nonmatching rebuilt source or a focus.
     *
     * @param source the source to match
     * @return a right value containing the focus when matched, otherwise a left rebuilt source
     */
    Either<T, A> match(S source);

    /**
     * Builds a matching source from a replacement focus.
     *
     * @param value the replacement focus
     * @return the built source
     */
    T build(B value);

    /**
     * Returns the matched focus when present.
     *
     * @param source the source to match
     * @return the matched focus, or an empty value when the source does not match
     */
    default Maybe<A> getMaybe(S source) {
        Either<T, A> value = match(source);
        return value.isRight() ? Maybe.some(value.right()) : Maybe.none();
    }

    /**
     * Applies an applicative transformation when the source matches.
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
        Either<T, A> value = match(source);
        return value.isRight()
                ? applicative.map(this::build, f.apply(value.right()))
                : applicative.of(value.left());
    }

    /**
     * Transforms a matched focus and leaves a nonmatching source unchanged according to
     * {@link #match(Object)}.
     *
     * @param f the focus transformation
     * @param source the source to transform
     * @return the rebuilt source
     */
    default T modify(Function<? super A, ? extends B> f, S source) {
        Either<T, A> value = match(source);
        return value.isRight() ? build(f.apply(value.right())) : value.left();
    }

    /**
     * Replaces a matched focus.
     *
     * @param value the replacement focus
     * @param source the source to transform
     * @return the rebuilt source
     */
    default T set(B value, S source) {
        return modify(ignored -> value, source);
    }

    /**
     * Determines whether a source matches.
     *
     * @param source the source to test
     * @return {@code true} when the source matches
     */
    default boolean matches(S source) {
        return match(source).isRight();
    }

    /**
     * Determines whether a source does not match.
     *
     * @param source the source to test
     * @return {@code true} when the source does not match
     */
    default boolean doesNotMatch(S source) {
        return !matches(source);
    }

    /**
     * Applies a focus transformation only when a predicate accepts the matched focus.
     *
     * @param predicate the condition applied to a matched focus
     * @param modifier the transformation applied when the condition is true
     * @param otherwise the transformation applied when the condition is false
     * @param source the source to transform
     * @return the rebuilt source
     */
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

    /**
     * Returns a traversal containing zero or one focus.
     *
     * @return the traversal view
     */
    default PTraversal<S, T, A, B> asTraversal() {
        PPrism<S, T, A, B> self = this;
        PTraversal<S, T, A, B> direct = self::modifyF;
        return OpticPrograms.traversal(direct, OpticPrograms.programOrOpaque(self, "prism"));
    }

    /**
     * Returns a setter with this prism's conditional update behavior.
     *
     * @return the setter view
     */
    default PSetter<S, T, A, B> asSetter() {
        PPrism<S, T, A, B> self = this;
        PSetter<S, T, A, B> direct = self::modify;
        return OpticPrograms.setter(direct, OpticPrograms.programOrOpaque(self, "prism"));
    }

    /**
     * Returns a fold containing the matched focus when present.
     *
     * @return the fold view
     */
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

    /**
     * Composes this prism with another prism.
     *
     * @param <C> the composed matched focus type
     * @param <D> the composed replacement focus type
     * @param other the prism applied to a matched focus
     * @return the composed prism
     */
    default <C, D> PPrism<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        PPrism<S, T, A, B> self = this;
        PPrism<S, T, C, D> typed = OpticMetadata.optic(PPrism.of(
                        source -> self.match(source).fold(Either::left, focus -> other.match(focus).mapLeft(self::build)),
                        value -> self.build(other.build(value))),
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.prism(typed, OpticPrograms.compose(self, other));
    }

    /**
     * Composes this prism with an isomorphism.
     *
     * @param <C> the composed focus type
     * @param <D> the composed replacement focus type
     * @param other the isomorphism applied to a matched focus
     * @return the composed prism
     */
    default <C, D> PPrism<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PPrism<S, T, C, D> direct = PPrism.of(
                source -> match(source).map(other::get),
                value -> build(other.reverseGet(value)));
        return OpticPrograms.prism(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this prism with a fold.
     *
     * @param <C> the observed focus type
     * @param other the fold applied to a matched focus
     * @return the composed fold
     */
    default <C> Fold<S, C> andThen(Fold<A, C> other) {
        return asFold().andThen(other);
    }

    /**
     * Composes this prism with a lens.
     *
     * @param <C> the composed focus type
     * @param <D> the composed replacement focus type
     * @param other the lens applied to a matched focus
     * @return the composed affine optic
     */
    default <C, D> PAffine<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        PAffine<S, T, C, D> typed = OpticMetadata.optic(PAffine.of(
                        source -> match(source).fold(Either::left, focus -> Either.right(other.get(focus))),
                        (source, value) -> match(source).fold(Function.identity(), focus -> build(other.set(value, focus)))),
                OpticMetadata.<S, T, A, B>optic(this)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.affine(typed, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this prism with an affine optic.
     *
     * @param <C> the composed focus type
     * @param <D> the composed replacement focus type
     * @param other the affine optic applied to a matched focus
     * @return the composed affine optic
     */
    default <C, D> PAffine<S, T, C, D> andThen(PAffine<A, B, C, D> other) {
        PAffine<S, T, C, D> typed = OpticMetadata.optic(PAffine.of(
                        source -> match(source)
                                .fold(Either::left, focus -> other.preview(focus).mapLeft(this::build)),
                        (source, value) -> match(source)
                                .fold(Function.identity(), focus -> build(other.set(value, focus)))),
                OpticMetadata.<S, T, A, B>optic(this)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.affine(typed, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this prism with a traversal.
     *
     * @param <C> the composed focus type
     * @param <D> the composed replacement focus type
     * @param other the traversal applied to a matched focus
     * @return the composed traversal
     */
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

    /**
     * Creates a prism from serializable match and build operations.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the matched focus type
     * @param <B> the replacement focus type
     * @param match the source match operation
     * @param build the matching source builder
     * @return the resulting prism
     */
    static <S, T, A, B> PPrism<S, T, A, B> of(
            PrismMatcher<? super S, T, A> match,
            PrismBuilder<? super B, ? extends T> build) {
        PPrism<S, T, A, B> direct = of(
                (Function<? super S, Either<T, A>>) match,
                (Function<? super B, ? extends T>) build);
        return LambdaLifter.prism(direct, match, build);
    }

    /**
     * Creates a prism from match and build operations.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the matched focus type
     * @param <B> the replacement focus type
     * @param match the source match operation
     * @param build the matching source builder
     * @return the resulting prism
     */
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
