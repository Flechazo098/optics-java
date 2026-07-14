package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.internal.AccumulationBuffer;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Traversal<S, A> extends PTraversal<S, S, A, A> {
    @Override
    default Setter<S, A> asSetter() {
        return Setter.from(PTraversal.super.asSetter());
    }

    default <F extends K1> App<F, S> modifyWhenS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.modifyWhen(this, condition, modifier, source, selective);
    }

    default <F extends K1> App<F, S> modifyUnlessS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.modifyUnless(this, condition, modifier, source, selective);
    }

    default <C> Traversal<S, C> andThen(Traversal<A, C> other) {
        return from(PTraversal.super.andThen(other));
    }

    default <C> Traversal<S, C> andThen(Iso<A, C> other) {
        return from(PTraversal.super.andThen(other));
    }

    default <C> Traversal<S, C> andThen(Lens<A, C> other) {
        return from(PTraversal.super.andThen(other));
    }

    default <C> Traversal<S, C> andThen(Prism<A, C> other) {
        return from(PTraversal.super.andThen(other));
    }

    default <C> Traversal<S, C> andThen(Affine<A, C> other) {
        return from(PTraversal.super.andThen(other));
    }

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

    default S modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends A> modifier,
            S source) {
        return PTraversal.super.modifyWhen(predicate, modifier, Function.identity(), source);
    }

    static <S, A> Traversal<S, A> of(Class<S> recordType, WanderGetter<S, A> getter) {
        var path = LambdaLifter.recordPath(getter).orElseGet(() -> {
            throw new IllegalArgumentException("Traversal getter must be an analyzable record component access");
        });
        if (path.sourceType() != recordType || path.components().size() != 1) {
            throw new IllegalArgumentException("Traversal getter must select one component on " + recordType.getName());
        }
        return from(RecordOptics.recordTraversal(recordType, path.components().get(0)));
    }

    static <S, A> Traversal<S, A> of(
            WanderGetter<? super S, A> targets,
            WanderRebuilder<S, A> rebuild) {
        Traversal<S, A> direct = direct(targets, rebuild);
        return LambdaLifter.traversal(direct, targets, rebuild);
    }

    static <S, A> Traversal<S, A> opaque(
            Function<? super S, ? extends Iterable<? extends A>> targets,
            BiFunction<S, List<A>, S> rebuild) {
        return OpticPrograms.traversal(direct(targets, rebuild), OpticPrograms.opaque("traversal", null));
    }

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
                App<F, AccumulationBuffer<A>> result = applicative.of(AccumulationBuffer.empty());
                for (A target : targets.apply(source)) {
                    result = applicative.map2(result, modifier.apply(target), AccumulationBuffer::prepend);
                }
                return applicative.map(values -> rebuild.apply(source, values.toList()), result);
            }
        };
    }

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
