package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.business.data.Chain;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a monomorphic traversal over zero or more focuses.
 *
 * @param <S> the source type
 * @param <A> the focus type
 */
public interface Traversal<S, A> extends PTraversal<S, S, A, A> {
    /**
     * Returns a setter updating the same focuses.
     *
     * @return the setter view
     */
    @Override
    default Setter<S, A> asSetter() {
        return Setter.from(PTraversal.super.asSetter());
    }

    /**
     * Applies an effectful modifier when an effectful condition is true.
     *
     * @param <F> the selective witness type
     * @param condition the effectful condition evaluated for every focus
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
     * @param condition the effectful condition evaluated for every focus
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
     * Composes this traversal with another traversal.
     *
     * @param <C> the composed focus type
     * @param other the traversal applied to every focus
     * @return the composed traversal
     */
    default <C> Traversal<S, C> andThen(Traversal<A, C> other) {
        return from(PTraversal.super.andThen(other));
    }

    /**
     * Composes this traversal with an isomorphism.
     *
     * @param <C> the composed focus type
     * @param other the isomorphism applied to every focus
     * @return the composed traversal
     */
    default <C> Traversal<S, C> andThen(Iso<A, C> other) {
        return from(PTraversal.super.andThen(other));
    }

    /**
     * Composes this traversal with a lens.
     *
     * @param <C> the composed focus type
     * @param other the lens applied to every focus
     * @return the composed traversal
     */
    default <C> Traversal<S, C> andThen(Lens<A, C> other) {
        return from(PTraversal.super.andThen(other));
    }

    /**
     * Composes this traversal with a prism.
     *
     * @param <C> the composed focus type
     * @param other the prism applied to every focus
     * @return the composed traversal
     */
    default <C> Traversal<S, C> andThen(Prism<A, C> other) {
        return from(PTraversal.super.andThen(other));
    }

    /**
     * Composes this traversal with an affine optic.
     *
     * @param <C> the composed focus type
     * @param other the affine optic applied to every focus
     * @return the composed traversal
     */
    default <C> Traversal<S, C> andThen(Affine<A, C> other) {
        return from(PTraversal.super.andThen(other));
    }

    /**
     * Returns a traversal that updates only focuses satisfying a predicate.
     *
     * @param predicate the condition used to retain focuses
     * @return the filtered traversal
     */
    default Traversal<S, A> filtered(Predicate<? super A> predicate) {
        Traversal<S, A> self = this;
        Traversal<S, A> direct = new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> modifier,
                    S source,
                    Applicative<F, ?> applicative) {
                return self.modifyF(
                        value -> predicate.test(value)
                                ? modifier.apply(value)
                                : applicative.of(value),
                        source,
                        applicative);
            }
        };
        return OpticPrograms.traversal(
                direct,
                OpticPrograms.opaque("filteredTraversal", null));
    }

    /**
     * Applies a modifier only to focuses satisfying a predicate.
     *
     * @param predicate the condition applied to every focus
     * @param modifier the transformation applied when the condition is true
     * @param source the source to transform
     * @return the rebuilt source
     */
    default S modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends A> modifier,
            S source) {
        return PTraversal.super.modifyWhen(predicate, modifier, Function.identity(), source);
    }

    /**
     * Creates a traversal over a record component selected by a serializable accessor.
     *
     * <p>The selected component must itself be a supported traversable container.
     *
     * @param <S> the record type
     * @param <A> the contained focus type
     * @param recordType the record class
     * @param getter the accessor selecting one record component
     * @return the record-component traversal
     * @throws IllegalArgumentException if the accessor does not select exactly one component of
     * {@code recordType}
     */
    static <S, A> Traversal<S, A> of(Class<S> recordType, WanderGetter<S, A> getter) {
        var path = LambdaLifter.recordPath(getter).orElseGet(() -> {
            throw new IllegalArgumentException("Traversal getter must be an analyzable record component access");
        });
        if (path.sourceType() != recordType || path.components().size() != 1) {
            throw new IllegalArgumentException("Traversal getter must select one component on " + recordType.getName());
        }
        return from(RecordOptics.recordTraversal(recordType, path.components().getFirst()));
    }

    /**
     * Creates a traversal from serializable focus enumeration and rebuild operations.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param targets the operation returning focuses in encounter order
     * @param rebuild the operation rebuilding a source from replacement focuses
     * @return the resulting traversal
     */
    static <S, A> Traversal<S, A> of(
            WanderGetter<? super S, A> targets,
            WanderRebuilder<S, A> rebuild) {
        Traversal<S, A> direct = direct(targets, rebuild);
        return LambdaLifter.traversal(direct, targets, rebuild);
    }

    /**
     * Creates an opaque traversal from focus enumeration and rebuild operations.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param targets the operation returning focuses in encounter order
     * @param rebuild the operation rebuilding a source from replacement focuses
     * @return the resulting traversal
     */
    static <S, A> Traversal<S, A> opaque(
            Function<? super S, ? extends Iterable<? extends A>> targets,
            BiFunction<S, List<A>, S> rebuild) {
        return OpticPrograms.traversal(direct(targets, rebuild), OpticPrograms.opaque("traversal", null));
    }

    /**
     * Creates a traversal over every element of a reference array.
     *
     * @param <A> the component type
     * @param componentType the runtime component class
     * @param targets the operation returning array elements in index order
     * @param rebuild the operation rebuilding the array
     * @return the array traversal
     */
    static <A> Traversal<A[], A> ofArray(
            Class<A> componentType,
            WanderGetter<A[], A> targets,
            WanderRebuilder<A[], A> rebuild) {
        Objects.requireNonNull(componentType, "componentType");
        Traversal<A[], A> direct = direct(targets, rebuild);
        return LambdaLifter.arrayTraversal(direct, componentType, targets, rebuild);
    }

    private static <S, A> Traversal<S, A> direct(
            Function<? super S, ? extends Iterable<? extends A>> targets,
            BiFunction<S, List<A>, S> rebuild) {
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(rebuild, "rebuild");
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> modifier,
                    S source,
                    Applicative<F, ?> applicative) {
                App<F, Chain<A>> result = applicative.of(Chain.empty());
                for (A target : targets.apply(source)) {
                    result = applicative.map2(result, modifier.apply(target), Chain::append);
                }
                return applicative.map(values -> rebuild.apply(source, values.toList()), result);
            }
        };
    }

    /**
     * Returns a monomorphic view of a monomorphic polymorphic traversal.
     *
     * @param <S> the source type
     * @param <A> the focus type
     * @param traversal the traversal to adapt
     * @return the corresponding monomorphic traversal
     */
    static <S, A> Traversal<S, A> from(PTraversal<S, S, A, A> traversal) {
        Traversal<S, A> direct;
        if (traversal instanceof Traversal<?, ?> simple) {
            @SuppressWarnings("unchecked")
            Traversal<S, A> result = (Traversal<S, A>) simple;
            direct = result;
        } else {
            direct = traversal::modifyF;
        }
        Traversal<S, A> typed = OpticMetadata.optic(direct, OpticMetadata.optic(traversal));
        return OpticPrograms.traversal(typed, OpticPrograms.programOrOpaque(traversal, "traversal"));
    }
}
