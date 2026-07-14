package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.function.Function3;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a polymorphic lens that reads exactly one focus and rebuilds its source.
 *
 * @param <S> the input source type
 * @param <T> the rebuilt source type
 * @param <A> the input focus type
 * @param <B> the replacement focus type
 */
public interface PLens<S, T, A, B> extends Optic<S, T, A, B> {
    /**
     * Gets the focus from a source.
     *
     * @param source the source to read
     * @return the focus
     */
    A get(S source);

    /**
     * Replaces the focus and returns the rebuilt source.
     *
     * @param value the replacement focus
     * @param source the source to rebuild
     * @return the rebuilt source
     */
    T set(B value, S source);

    /**
     * Transforms the focus and returns the rebuilt source.
     *
     * @param f the focus transformation
     * @param source the source to transform
     * @return the rebuilt source
     */
    default T modify(Function<? super A, ? extends B> f, S source) {
        return set(f.apply(get(source)), source);
    }

    /**
     * Applies a functorial transformation to the focus.
     *
     * @param <F> the functor witness type
     * @param f the effectful focus transformation
     * @param source the source to transform
     * @param functor the functor used to rebuild the source
     * @return the rebuilt source in the functor context
     */
    <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Functor<F, ?> functor);

    /**
     * Applies an applicative transformation to the focus.
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
        return modifyF(f, source, (Functor<F, ?>) applicative);
    }

    /**
     * Returns a traversal containing exactly this lens's focus.
     *
     * @return the traversal view
     */
    default PTraversal<S, T, A, B> asTraversal() {
        PLens<S, T, A, B> self = this;
        PTraversal<S, T, A, B> traversal = self::modifyF;
        PTraversal<S, T, A, B> typed = OpticMetadata.optic(traversal, OpticMetadata.optic(self));
        return OpticPrograms.traversal(typed, OpticPrograms.programOrOpaque(self, "lens"));
    }

    /**
     * Returns a getter that observes this lens's focus.
     *
     * @return the getter view
     */
    default Getter<S, A> asGetter() {
        PLens<S, T, A, B> self = this;
        Getter<S, A> getter = self::get;
        Getter<S, A> typed = OpticMetadata.fold(
                getter,
                OpticMetadata.<S, T, A, B>optic(self)
                        .map(optic -> PointFreeFold.fromOptic(optic, getter)));
        return OpticPrograms.getter(typed, OpticPrograms.programOrOpaque(self, "lens"));
    }

    /**
     * Returns a setter with this lens's update behavior.
     *
     * @return the setter view
     */
    default PSetter<S, T, A, B> asSetter() {
        PLens<S, T, A, B> self = this;
        PSetter<S, T, A, B> direct = self::modify;
        return OpticPrograms.setter(direct, OpticPrograms.programOrOpaque(self, "lens"));
    }

    /**
     * Returns a fold containing exactly this lens's focus.
     *
     * @return the fold view
     */
    default Fold<S, A> asFold() {
        PLens<S, T, A, B> self = this;
        Fold<S, A> fold = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return f.apply(self.get(source));
            }
        };
        Fold<S, A> typed = OpticMetadata.fold(
                fold,
                OpticMetadata.<S, T, A, B>optic(self)
                        .map(optic -> PointFreeFold.fromOptic(optic, fold)));
        return OpticPrograms.fold(typed, OpticPrograms.programOrOpaque(self, "lens"));
    }

    /**
     * Composes this lens with another lens.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the lens applied to this lens's focus
     * @return the composed lens
     */
    default <C, D> PLens<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        PLens<S, T, A, B> self = this;
        PLens<S, T, C, D> composed = new PLens<>() {
            @Override
            public C get(S source) {
                return other.get(self.get(source));
            }

            @Override
            public T set(D value, S source) {
                return self.set(other.set(value, self.get(source)), source);
            }

            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Functor<F, ?> functor) {
                return functor.map(next -> self.set(next, source), other.modifyF(f, self.get(source), functor));
            }
        };
        PLens<S, T, C, D> typed = OpticMetadata.optic(
                composed,
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.lens(typed, OpticPrograms.compose(self, other));
    }

    /**
     * Composes this lens with an isomorphism.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the isomorphism applied to this lens's focus
     * @return the composed lens
     */
    default <C, D> PLens<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PLens<S, T, A, B> self = this;
        PLens<S, T, C, D> direct = PLens.of(
                source -> other.get(self.get(source)),
                (source, value) -> self.set(other.reverseGet(value), source));
        return OpticPrograms.lens(direct, OpticPrograms.compose(this, other));
    }

    /**
     * Composes this lens with a fold.
     *
     * @param <C> the observed focus type
     * @param fold the fold applied to this lens's focus
     * @return the composed fold
     */
    default <C> Fold<S, C> andThen(Fold<A, C> fold) {
        return asFold().andThen(fold);
    }

    /**
     * Composes this lens with a prism.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the prism applied to this lens's focus
     * @return the composed affine optic
     */
    default <C, D> PAffine<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        PLens<S, T, A, B> self = this;
        PAffine<S, T, C, D> typed = OpticMetadata.optic(PAffine.of(
                        source -> other.match(self.get(source)).mapLeft(value -> self.set(value, source)),
                        (source, value) -> self.set(other.build(value), source)),
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.affine(typed, OpticPrograms.compose(self, other));
    }

    /**
     * Composes this lens with an affine optic.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the affine optic applied to this lens's focus
     * @return the composed affine optic
     */
    default <C, D> PAffine<S, T, C, D> andThen(PAffine<A, B, C, D> other) {
        PLens<S, T, A, B> self = this;
        PAffine<S, T, C, D> typed = OpticMetadata.optic(PAffine.of(
                        source -> other.preview(self.get(source)).mapLeft(value -> self.set(value, source)),
                        (source, value) -> self.set(other.set(value, self.get(source)), source)),
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.affine(typed, OpticPrograms.compose(self, other));
    }

    /**
     * Composes this lens with a traversal.
     *
     * @param <C> the composed input focus type
     * @param <D> the composed replacement focus type
     * @param other the traversal applied to this lens's focus
     * @return the composed traversal
     */
    default <C, D> PTraversal<S, T, C, D> andThen(PTraversal<A, B, C, D> other) {
        PLens<S, T, A, B> self = this;
        PTraversal<S, T, C, D> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return applicative.map(
                        next -> self.set(next, source), other.modifyF(f, self.get(source), applicative));
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.compose(self, other));
    }

    /**
     * Selects one of two focus transformations using a predicate.
     *
     * @param predicate the condition applied to the current focus
     * @param thenModifier the transformation used when the predicate is true
     * @param elseModifier the transformation used when the predicate is false
     * @param source the source to transform
     * @return the rebuilt source
     */
    default T modifyBranch(
            Predicate<? super A> predicate,
            Function<? super A, ? extends B> thenModifier,
            Function<? super A, ? extends B> elseModifier,
            S source) {
        A current = get(source);
        return predicate.test(current)
                ? set(thenModifier.apply(current), source)
                : set(elseModifier.apply(current), source);
    }

    /**
     * Creates a lens from serializable read and rebuild operations.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param getter the focus reader
     * @param setter the source rebuilder
     * @return the resulting lens
     */
    static <S, T, A, B> PLens<S, T, A, B> of(
            LensGetter<? super S, ? extends A> getter,
            LensRebuilder<S, B, T> setter) {
        PLens<S, T, A, B> direct = of(
                (Function<? super S, ? extends A>) getter,
                (BiFunction<S, B, T>) setter);
        return LambdaLifter.lens(direct, getter, setter);
    }

    /**
     * Creates a lens from read and rebuild operations.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param getter the focus reader
     * @param setter the source rebuilder
     * @return the resulting lens
     */
    static <S, T, A, B> PLens<S, T, A, B> of(
            Function<? super S, ? extends A> getter,
            BiFunction<S, B, T> setter) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(setter, "setter");
        PLens<S, T, A, B> direct = new PLens<>() {
            @Override
            public A get(S source) {
                return getter.apply(source);
            }

            @Override
            public T set(B value, S source) {
                return setter.apply(source, value);
            }

            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> f, S source, Functor<F, ?> functor) {
                return functor.map(value -> set(value, source), f.apply(get(source)));
            }
        };
        return OpticPrograms.lens(direct, OpticPrograms.opaque("lens", null));
    }

    /**
     * Creates an opaque lens from read and rebuild operations.
     *
     * @param <S> the input source type
     * @param <T> the rebuilt source type
     * @param <A> the input focus type
     * @param <B> the replacement focus type
     * @param getter the focus reader
     * @param setter the source rebuilder
     * @return the resulting lens
     */
    static <S, T, A, B> PLens<S, T, A, B> opaque(
            Function<? super S, ? extends A> getter,
            BiFunction<S, B, T> setter) {
        return of(getter, setter);
    }

    /**
     * Creates a lens for a record component identified by a serializable accessor.
     *
     * @param <S> the record type
     * @param <A> the component type
     * @param recordType the record class
     * @param getter the component accessor
     * @return the record-component lens
     * @throws IllegalArgumentException if the accessor does not identify a component of {@code recordType}
     */
    static <S, A> PLens<S, S, A, A> of(Class<S> recordType, LensGetter<S, A> getter) {
        return RecordOptics.recordLens(recordType, getter);
    }

    /**
     * Creates a lens focusing on a pair of focuses and rebuilding from the original source.
     *
     * @param <S> the source type
     * @param <A> the first focus type
     * @param <B> the second focus type
     * @param first the first lens
     * @param second the second lens
     * @param rebuild the source rebuild operation
     * @return the paired lens
     */
    static <S, A, B> PLens<S, S, Tuple2<A, B>, Tuple2<A, B>> paired(
            PLens<S, S, A, A> first, PLens<S, S, B, B> second, Function3<S, A, B, S> rebuild) {
        return PLens.of(
                source -> Tuple2.of(first.get(source), second.get(source)),
                (source, Tuple2) -> rebuild.apply(source, Tuple2.first(), Tuple2.second()));
    }

    /**
     * Creates a lens focusing on a pair of focuses and rebuilding through a constructor.
     *
     * @param <S> the source type
     * @param <A> the first focus type
     * @param <B> the second focus type
     * @param first the first lens
     * @param second the second lens
     * @param constructor the source constructor
     * @return the paired lens
     */
    static <S, A, B> PLens<S, S, Tuple2<A, B>, Tuple2<A, B>> paired(
            PLens<S, S, A, A> first, PLens<S, S, B, B> second, BiFunction<A, B, S> constructor) {
        return paired(first, second, (source, a, b) -> constructor.apply(a, b));
    }

    @SuppressWarnings("unchecked")
    private static <B> B cast(Object value) {
        return (B) value;
    }
}
