package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.CompositePointFreeOptic;
import com.flechazo.hkt.functions.Id;
import com.flechazo.hkt.functions.PointFree;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.TypedOptic;

import java.util.BitSet;
import java.util.Objects;
import java.util.function.Function;

public record TypeRewriteResult<A, B>(
        Type<A> sourceType,
        Type<B> targetType,
        Maybe<PointFree<Function<A, B>>> view,
        Maybe<BitSet> recursiveDependencies) {
    public TypeRewriteResult {
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(recursiveDependencies, "recursiveDependencies");
        recursiveDependencies = recursiveDependencies.map(TypeRewriteResult::cloneBitSet);
    }

    public static <A> TypeRewriteResult<A, A> nop(Type<A> type) {
        return executable(type, type, PointFree.id(type));
    }

    public static <A, B> TypeRewriteResult<A, B> typeOnly(Type<A> sourceType, Type<B> targetType) {
        return new TypeRewriteResult<>(sourceType, targetType, Maybe.none(), Maybe.some(new BitSet()));
    }

    public static <A, B> TypeRewriteResult<A, B> typeOnly(
            Type<A> sourceType,
            Type<B> targetType,
            Maybe<BitSet> recursiveDependencies) {
        return new TypeRewriteResult<>(sourceType, targetType, Maybe.none(), recursiveDependencies);
    }

    public static <A, B> TypeRewriteResult<A, B> executable(
            Type<A> sourceType,
            Type<B> targetType,
            PointFree<Function<A, B>> view) {
        return new TypeRewriteResult<>(sourceType, targetType, Maybe.some(view), Maybe.some(new BitSet()));
    }

    public TypeRewriteResult<A, B> withRecursiveDependencies(int... dependencies) {
        return new TypeRewriteResult<>(sourceType, targetType, view, Maybe.some(dependencySet(dependencies)));
    }

    public TypeRewriteResult<A, B> plusRecursiveDependencies(int... dependencies) {
        if (recursiveDependencies.isEmpty()) {
            return new TypeRewriteResult<>(sourceType, targetType, view, Maybe.none());
        }
        BitSet merged = cloneBitSet(recursiveDependencies.get());
        for (int dependency : dependencies) {
            if (dependency < 0) {
                throw new IndexOutOfBoundsException(dependency);
            }
            merged.set(dependency);
        }
        return new TypeRewriteResult<>(sourceType, targetType, view, Maybe.some(merged));
    }

    public TypeRewriteResult<A, B> withUnknownRecursiveDependencies() {
        return new TypeRewriteResult<>(sourceType, targetType, view, Maybe.none());
    }

    public <S, T> TypeRewriteResult<S, T> throughOptic(TypedOptic<S, T, A, B> optic) {
        return throughOptic(new CompositePointFreeOptic<>(optic));
    }

    public <S, T> TypeRewriteResult<S, T> throughOptic(PointFreeOptic<S, T, A, B> optic) {
        Objects.requireNonNull(optic, "optic");
        Maybe<PointFree<Function<S, T>>> lifted = view.map(function -> PointFree.opticApp(optic, function));
        return new TypeRewriteResult<>(optic.sourceType(), optic.targetType(), lifted, recursiveDependencies);
    }

    public <S, T> TypeRewriteResult<S, T> retag(Type<S> sourceType, Type<T> targetType) {
        Type<Function<S, T>> functionType = Types.function(sourceType, targetType);
        Maybe<PointFree<Function<S, T>>> retagged =
                view.map(function -> retagPointFree(function, functionType));
        return new TypeRewriteResult<>(sourceType, targetType, retagged, recursiveDependencies);
    }

    public boolean isNop() {
        //noinspection RedundantCast
        return sourceType.equals(targetType) && view.isDefined() && (Object) view.get() instanceof Id<?>;
    }

    public boolean hasExecutableView() {
        return view.isDefined();
    }

    public boolean hasRecursiveDependencyEvidence() {
        return recursiveDependencies.isDefined();
    }

    public Maybe<BitSet> recursiveDependencies() {
        return recursiveDependencies.map(TypeRewriteResult::cloneBitSet);
    }

    public <C> TypeRewriteResult<A, C> compose(TypeRewriteResult<B, C> next) {
        Objects.requireNonNull(next, "next");
        if (!TypeUnifier.unify(targetType, next.sourceType).isDefined()) {
            throw new IllegalArgumentException("type rewrite composition mismatch: "
                    + targetType + " then " + next.sourceType);
        }
        Maybe<PointFree<Function<A, C>>> composedView =
                view.isDefined() && next.view.isDefined()
                        ? Maybe.some(PointFree.comp(next.view.get(), view.get()))
                        : Maybe.none();
        return new TypeRewriteResult<>(sourceType, next.targetType, composedView, mergeDependencies(next));
    }

    public TypeRewriteResult<?, ?> erase() {
        return this;
    }

    @SuppressWarnings("unchecked")
    public static <A> Type<A> source(TypeRewriteResult<?, ?> result) {
        return (Type<A>) result.sourceType();
    }

    @SuppressWarnings("unchecked")
    public static <A> Type<A> target(TypeRewriteResult<?, ?> result) {
        return (Type<A>) result.targetType();
    }

    @SuppressWarnings("unchecked")
    public static <A, B> TypeRewriteResult<A, B> cast(TypeRewriteResult<?, ?> result) {
        return (TypeRewriteResult<A, B>) result;
    }

    private Maybe<BitSet> mergeDependencies(TypeRewriteResult<?, ?> next) {
        if (recursiveDependencies.isEmpty() || next.recursiveDependencies.isEmpty()) {
            return Maybe.none();
        }
        BitSet dependencies = cloneBitSet(recursiveDependencies.get());
        dependencies.or(next.recursiveDependencies.get());
        return Maybe.some(dependencies);
    }

    private static BitSet dependencySet(int... recursiveDependencies) {
        BitSet dependencies = new BitSet();
        for (int dependency : recursiveDependencies) {
            if (dependency < 0) {
                throw new IndexOutOfBoundsException(dependency);
            }
            dependencies.set(dependency);
        }
        return dependencies;
    }

    private static BitSet cloneBitSet(BitSet dependencies) {
        return (BitSet) dependencies.clone();
    }

    @SuppressWarnings("unchecked")
    private static <S, T> PointFree<Function<S, T>> retagPointFree(
            PointFree<?> function,
            Type<Function<S, T>> type) {
        return ((PointFree<Function<S, T>>) function).retagUnsafe(type);
    }
}
