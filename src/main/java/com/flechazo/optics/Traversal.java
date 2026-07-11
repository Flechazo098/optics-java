package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Tuple2;
import com.flechazo.optics.util.Traversals;
import com.flechazo.optics.util.StringTraversals;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.WanderBuffer;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Traversal<S, A> extends PTraversal<S, S, A, A> {
    @Override
    default Setter<S, A> asSetter() {
        return Setter.from(PTraversal.super.asSetter());
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

    static <A> Traversal<List<A>, A> forList() {
        Traversal<List<A>, A> direct = from(Traversals.forList());
        return OpticPrograms.traversal(direct, OpticPrograms.structured("listTraversal", null));
    }

    static <A> Traversal<Set<A>, A> forSet() {
        Traversal<Set<A>, A> direct = from(Traversals.forSet());
        return OpticPrograms.traversal(direct, OpticPrograms.structured("setTraversal", null));
    }

    static <K, V> Traversal<Map<K, V>, V> mapValues() {
        Traversal<Map<K, V>, V> direct = from(PTraversal.mapValues());
        return OpticPrograms.traversal(direct, OpticPrograms.structured("mapValuesTraversal", null));
    }

    static <K, V> Traversal<Map<K, V>, Tuple2<K, V>> mapEntries() {
        return from(Traversals.forMapEntries());
    }

    static <A> Traversal<A[], A> forArray(Class<A> componentType) {
        return from(Traversals.forArray(componentType));
    }

    static Traversal<String, Character> forStringCharacters() {
        return from(StringTraversals.characters());
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
                App<F, WanderBuffer<A>> result = applicative.of(WanderBuffer.empty());
                for (A target : targets.apply(source)) {
                    result = applicative.map2(result, modifier.apply(target), WanderBuffer::prepend);
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
        return OpticPrograms.traversal(direct, OpticPrograms.programOrOpaque(traversal, "traversal"));
    }
}
