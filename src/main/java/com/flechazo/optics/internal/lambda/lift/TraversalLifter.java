package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.WanderGetter;
import com.flechazo.optics.WanderRebuilder;
import com.flechazo.optics.internal.lambda.ast.LambdaExpr;

import java.lang.reflect.Method;
import java.util.List;

public final class TraversalLifter {
    private final RecordLensLifter recordLensLifter = new RecordLensLifter();

    public Maybe<LiftedNodeKey> lift(LambdaExpr targets, LambdaExpr rebuild) {
        Maybe<String> container = canonicalContainer(targets, rebuild);
        if (container.isDefined() && !container.get().equals("arrayTraversal")) {
            return Maybe.some(new LiftedNodeKey(container.get(), List.of(targets, rebuild)));
        }
        Maybe<LiftedLensNode> lens = recordLensLifter.lift(targets, rebuild);
        if (lens.isEmpty() || !iterablePath(lens.get().key())) {
            return Maybe.none();
        }
        return Maybe.some(new LiftedNodeKey("recordTraversal", List.of(targets, rebuild)));
    }

    public boolean liftsArray(LambdaExpr targets, LambdaExpr rebuild) {
        return canonicalContainer(targets, rebuild)
                .map("arrayTraversal"::equals)
                .orElse(false);
    }

    private static Maybe<String> canonicalContainer(LambdaExpr targets, LambdaExpr rebuild) {
        LambdaExpr target = SumTypeShape.strip(targets);
        LambdaExpr rebuilt = SumTypeShape.strip(rebuild);
        if (!(target instanceof LambdaExpr.StaticCall(Method method1, List<LambdaExpr> arguments1))
                || !(rebuilt instanceof LambdaExpr.StaticCall(Method method, List<LambdaExpr> arguments))
                || method1.getDeclaringClass() != WanderGetter.class
                || method.getDeclaringClass() != WanderRebuilder.class
                || !method1.getName().equals(method.getName())
                || arguments1.size() != 1
                || arguments.size() != 2
                || !SumTypeShape.argument(arguments1.getFirst(), 0)
                || !SumTypeShape.argument(arguments.get(0), 0)
                || !SumTypeShape.argument(arguments.get(1), 1)) {
            return Maybe.none();
        }
        return switch (method1.getName()) {
            case "list" -> Maybe.some("listTraversal");
            case "set" -> Maybe.some("setTraversal");
            case "mapValues" -> Maybe.some("mapValuesTraversal");
            case "mapEntries" -> Maybe.some("mapEntriesTraversal");
            case "array" -> Maybe.some("arrayTraversal");
            case "stringCharacters" -> Maybe.some("stringCharactersTraversal");
            default -> Maybe.none();
        };
    }

    private static boolean iterablePath(RecordPath path) {
        Class<?> current = path.sourceType();
        for (String component : path.components()) {
            Maybe<Method> accessor = recordAccessor(current, component);
            if (accessor.isEmpty()) {
                return false;
            }
            current = accessor.get().getReturnType();
        }
        return Iterable.class.isAssignableFrom(current);
    }

    private static Maybe<Method> recordAccessor(Class<?> type, String name) {
        if (!type.isRecord()) {
            return Maybe.none();
        }
        for (var component : type.getRecordComponents()) {
            if (component.getName().equals(name)) {
                return Maybe.some(component.getAccessor());
            }
        }
        return Maybe.none();
    }
}
