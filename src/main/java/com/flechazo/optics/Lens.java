package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Functor;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.function.Function3;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a monomorphic lens that reads exactly one focus and rebuilds its source.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
public interface Lens<S, A> extends PLens<S, S, A, A> {
    /**
     * Returns a traversal containing exactly this lens's focus.
     *
     * @return the traversal view
     */
    @Override
    default Traversal<S, A> asTraversal() {
        return Traversal.from(PLens.super.asTraversal());
    }

    /**
     * Returns a setter with this lens's update behavior.
     *
     * @return the setter view
     */
    @Override
    default Setter<S, A> asSetter() {
        return Setter.from(PLens.super.asSetter());
    }

    /**
     * Applies an effectful modifier when an effectful condition is true.
     *
     * @param <F> the selective witness type
     * @param condition the effectful condition evaluated for the focus
     * @param modifier the transformation selected when the condition is true
     * @param source the source to transform
     * @param selective the selective used to evaluate and combine effects
     * @return the rebuilt source in the selective context
     */
    default <F extends K1> App<F, S> modifyWhenS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.modifyWhen(this, condition, modifier, source, selective);
    }

    /**
     * Applies an effectful modifier when an effectful condition is false.
     *
     * @param <F> the selective witness type
     * @param condition the effectful condition evaluated for the focus
     * @param modifier the transformation selected when the condition is false
     * @param source the source to transform
     * @param selective the selective used to evaluate and combine effects
     * @return the rebuilt source in the selective context
     */
    default <F extends K1> App<F, S> modifyUnlessS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.modifyUnless(this, condition, modifier, source, selective);
    }

    /**
     * Composes this lens with another lens.
     *
     * @param <C> the composed focus type
     * @param other the lens applied to this lens's focus
     * @return the composed lens
     */
    default <C> Lens<S, C> andThen(Lens<A, C> other) {
        return from(PLens.super.andThen(other));
    }

    /**
     * Composes this lens with an isomorphism.
     *
     * @param <C> the composed focus type
     * @param other the isomorphism applied to this lens's focus
     * @return the composed lens
     */
    default <C> Lens<S, C> andThen(Iso<A, C> other) {
        return from(PLens.super.andThen(other));
    }

    /**
     * Composes this lens with a prism.
     *
     * @param <C> the composed focus type
     * @param other the prism applied to this lens's focus
     * @return the composed affine optic
     */
    default <C> Affine<S, C> andThen(Prism<A, C> other) {
        return Affine.from(PLens.super.andThen(other));
    }

    /**
     * Composes this lens with an affine optic.
     *
     * @param <C> the composed focus type
     * @param other the affine optic applied to this lens's focus
     * @return the composed affine optic
     */
    default <C> Affine<S, C> andThen(Affine<A, C> other) {
        return Affine.from(PLens.super.andThen(other));
    }

    /**
     * Composes this lens with a traversal.
     *
     * @param <C> the composed focus type
     * @param other the traversal applied to this lens's focus
     * @return the composed traversal
     */
    default <C> Traversal<S, C> andThen(Traversal<A, C> other) {
        return Traversal.from(PLens.super.andThen(other));
    }

    /**
     * Creates a lens from serializable read and rebuild operations.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param getter the focus reader
     * @param setter the source rebuilder
     * @return the resulting lens
     */
    static <S, A> Lens<S, A> of(
            LensGetter<? super S, ? extends A> getter,
            LensRebuilder<S, A, S> setter) {
        return from(PLens.of(getter, setter));
    }

    /**
     * Creates a lens from read and rebuild operations.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param getter the focus reader
     * @param setter the source rebuilder
     * @return the resulting lens
     */
    static <S, A> Lens<S, A> of(
            Function<? super S, ? extends A> getter,
            BiFunction<S, A, S> setter) {
        return from(PLens.of(getter, setter));
    }

    /**
     * Creates an opaque lens from read and rebuild operations.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param getter the focus reader
     * @param setter the source rebuilder
     * @return the resulting lens
     */
    static <S, A> Lens<S, A> opaque(
            Function<? super S, ? extends A> getter,
            BiFunction<S, A, S> setter) {
        return from(PLens.opaque(getter, setter));
    }

    /**
     * Creates a lens for a record component identified by a serializable accessor.
     *
     * @param <S> the record type
     * @param <A> the component type
     * @param recordType the record class
     * @param getter the component accessor
     * @return the record-component lens
     * @throws IllegalArgumentException if the accessor does not identify a component of
     * {@code recordType}
     */
    static <S, A> Lens<S, A> of(Class<S> recordType, LensGetter<S, A> getter) {
        return from(RecordOptics.recordLens(recordType, getter));
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
    static <S, A, B> Lens<S, Tuple2<A, B>> paired(
            Lens<S, A> first, Lens<S, B> second, Function3<S, A, B, S> rebuild) {
        return from(PLens.paired(first, second, rebuild));
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
    static <S, A, B> Lens<S, Tuple2<A, B>> paired(
            Lens<S, A> first, Lens<S, B> second, BiFunction<A, B, S> constructor) {
        return from(PLens.paired(first, second, constructor));
    }

    /**
     * Returns a monomorphic view of a monomorphic polymorphic lens.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param lens the lens to adapt
     * @return the corresponding monomorphic lens
     */
    static <S, A> Lens<S, A> from(PLens<S, S, A, A> lens) {
        Lens<S, A> direct;
        if (lens instanceof Lens<?, ?> simple) {
            @SuppressWarnings("unchecked")
            Lens<S, A> result = (Lens<S, A>) simple;
            direct = result;
        } else {
            direct = new Lens<>() {
                @Override
                public A get(S source) {
                    return lens.get(source);
                }

                @Override
                public S set(A value, S source) {
                    return lens.set(value, source);
                }

                @Override
                public <F extends K1> App<F, S> modifyF(
                        Function<A, App<F, A>> f, S source, Functor<F, ?> functor) {
                    return lens.modifyF(f, source, functor);
                }
            };
        }
        return OpticPrograms.lens(direct, OpticPrograms.programOrOpaque(lens, "lens"));
    }
}
