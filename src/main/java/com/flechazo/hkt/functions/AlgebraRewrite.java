package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;

import java.util.BitSet;
import java.util.Objects;
import java.util.function.Function;

public record AlgebraRewrite(
        boolean isIdentity,
        boolean isReflexiveIdentityAlgebra,
        PointFree<Function<Object, Object>> view,
        Type<Object> sourceType,
        Type<Object> targetType,
        Maybe<BitSet> recursiveDependencies) {
    public AlgebraRewrite {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(recursiveDependencies, "recursiveDependencies");
        recursiveDependencies = recursiveDependencies.map(AlgebraRewrite::cloneBitSet);
    }

    public static AlgebraRewrite identity() {
        Type<Object> type = Types.variable("RecursiveAlgebra.identity");
        return new AlgebraRewrite(true, false, PointFree.id(type), type, type, Maybe.some(new BitSet()));
    }

    public static AlgebraRewrite reflexiveIdentityAlgebra(Type<Object> type) {
        Objects.requireNonNull(type, "type");
        return new AlgebraRewrite(true, true, PointFree.id(type), type, type, Maybe.some(new BitSet()));
    }

    public static AlgebraRewrite rewrite(
            String name,
            Function<Object, Object> function) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(function, "function");
        Type<Object> sourceType = Types.variable(name + ".in");
        Type<Object> targetType = Types.variable(name + ".out");
        return rewrite(PointFree.fn(name, function, sourceType, targetType), sourceType, targetType);
    }

    public static AlgebraRewrite rewriteWithoutRecursiveDependencies(
            String name,
            Function<Object, Object> function) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(function, "function");
        Type<Object> sourceType = Types.variable(name + ".in");
        Type<Object> targetType = Types.variable(name + ".out");
        return rewriteWithRecursiveDependencies(
                PointFree.fn(name, function, sourceType, targetType),
                sourceType,
                targetType);
    }

    public static AlgebraRewrite rewriteWithRecursiveDependencies(
            String name,
            Function<Object, Object> function,
            int... recursiveDependencies) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(function, "function");
        Type<Object> sourceType = Types.variable(name + ".in");
        Type<Object> targetType = Types.variable(name + ".out");
        return rewriteWithRecursiveDependencies(
                PointFree.fn(name, function, sourceType, targetType),
                sourceType,
                targetType,
                recursiveDependencies);
    }

    public static AlgebraRewrite rewrite(
            PointFree<Function<Object, Object>> view,
            Type<Object> sourceType,
            Type<Object> targetType) {
        return new AlgebraRewrite(false, false, view, sourceType, targetType, Maybe.none());
    }

    public static AlgebraRewrite rewriteWithRecursiveDependencies(
            PointFree<Function<Object, Object>> view,
            Type<Object> sourceType,
            Type<Object> targetType,
            int... recursiveDependencies) {
        return new AlgebraRewrite(false, false, view, sourceType, targetType, Maybe.some(dependencySet(recursiveDependencies)));
    }

    public AlgebraRewrite compose(AlgebraRewrite inner) {
        Objects.requireNonNull(inner, "inner");
        PointFree<Function<Object, Object>> composed =
                PointFree.comp(view, inner.view);
        return new AlgebraRewrite(false, false, composed, inner.sourceType, targetType, mergeDependencies(inner));
    }

    public Function<Object, Object> function() {
        return view.eval();
    }

    public boolean hasRecursiveDependencyEvidence() {
        return recursiveDependencies.isDefined();
    }

    public Maybe<BitSet> recursiveDependencies() {
        return recursiveDependencies.map(AlgebraRewrite::cloneBitSet);
    }

    public boolean dependsOnAny(BitSet indices) {
        Objects.requireNonNull(indices, "indices");
        if (indices.isEmpty()) {
            return false;
        }
        if (recursiveDependencies.isEmpty()) {
            return true;
        }
        BitSet dependencies = cloneBitSet(recursiveDependencies.get());
        dependencies.and(indices);
        return !dependencies.isEmpty();
    }

    private Maybe<BitSet> mergeDependencies(AlgebraRewrite inner) {
        if (recursiveDependencies.isEmpty() || inner.recursiveDependencies.isEmpty()) {
            return Maybe.none();
        }
        BitSet dependencies = cloneBitSet(recursiveDependencies.get());
        dependencies.or(inner.recursiveDependencies.get());
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
}
