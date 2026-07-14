package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.internal.lambda.ast.LambdaExpr;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RecordLensLifter {
    public Maybe<LiftedLensNode> lift(LambdaExpr getter, LambdaExpr setter) {
        Maybe<Path> path = path(getter);
        if (path.isEmpty() || path.get().components().isEmpty()) {
            return Maybe.none();
        }
        Path value = path.get();
        if (!rebuilds(setter, value.sourceType(), value.components(), 0, List.of())) {
            return Maybe.none();
        }
        List<String> names = value.components().stream().map(Method::getName).toList();
        return Maybe.some(new LiftedLensNode(new RecordPath(value.sourceType(), names), getter, setter));
    }

    public Maybe<RecordPath> liftGetter(LambdaExpr getter) {
        return path(getter).map(path -> new RecordPath(
                path.sourceType(),
                path.components().stream().map(Method::getName).toList()));
    }

    private static Maybe<Path> path(LambdaExpr expression) {
        ArrayList<Method> reversed = new ArrayList<>();
        LambdaExpr current = strip(expression);
        while (current instanceof LambdaExpr.Access(LambdaExpr receiver, Method accessor)) {
            reversed.add(accessor);
            current = strip(receiver);
        }
        if (!(current instanceof LambdaExpr.Arg(int index)) || index != 0) {
            return Maybe.none();
        }
        Collections.reverse(reversed);
        if (reversed.isEmpty()) {
            return Maybe.none();
        }
        Class<?> sourceType = reversed.getFirst().getDeclaringClass();
        Class<?> expected = sourceType;
        for (Method accessor : reversed) {
            if (accessor.getDeclaringClass() != expected || !recordAccessor(accessor)) {
                return Maybe.none();
            }
            expected = accessor.getReturnType();
        }
        return Maybe.some(new Path(sourceType, List.copyOf(reversed)));
    }

    private static boolean rebuilds(
            LambdaExpr expression,
            Class<?> recordType,
            List<Method> path,
            int depth,
            List<Method> prefix) {
        expression = strip(expression);
        if (!(expression instanceof LambdaExpr.NewRecord(
                java.lang.reflect.Constructor<?> constructor, List<LambdaExpr> arguments
        ))
                || constructor.getDeclaringClass() != recordType) {
            return false;
        }
        RecordComponent[] components = recordType.getRecordComponents();
        if (components.length != arguments.size()) {
            return false;
        }
        Method target = path.get(depth);
        for (int index = 0; index < components.length; index++) {
            Method accessor = components[index].getAccessor();
            LambdaExpr argument = arguments.get(index);
            if (accessor.equals(target)) {
                if (depth == path.size() - 1) {
                    if (!isArgument(argument, 1)) {
                        return false;
                    }
                } else {
                    ArrayList<Method> nestedPrefix = new ArrayList<>(prefix);
                    nestedPrefix.add(accessor);
                    if (!rebuilds(argument, accessor.getReturnType(), path, depth + 1, nestedPrefix)) {
                        return false;
                    }
                }
            } else if (!samePath(argument, prefix, accessor)) {
                return false;
            }
        }
        return true;
    }

    private static boolean samePath(LambdaExpr expression, List<Method> prefix, Method accessor) {
        ArrayList<Method> expected = new ArrayList<>(prefix);
        expected.add(accessor);
        LambdaExpr current = strip(expression);
        for (int index = expected.size() - 1; index >= 0; index--) {
            if (!(current instanceof LambdaExpr.Access(LambdaExpr receiver, Method accessor1)) || !accessor1.equals(expected.get(index))) {
                return false;
            }
            current = strip(receiver);
        }
        return isArgument(current, 0);
    }

    private static boolean isArgument(LambdaExpr expression, int index) {
        return strip(expression) instanceof LambdaExpr.Arg(int index1) && index1 == index;
    }

    private static LambdaExpr strip(LambdaExpr expression) {
        while (expression instanceof LambdaExpr.Cast cast) {
            expression = cast.value();
        }
        while (expression instanceof LambdaExpr.Box box) {
            expression = box.value();
        }
        while (expression instanceof LambdaExpr.Unbox unbox) {
            expression = unbox.value();
        }
        return expression;
    }

    private static boolean recordAccessor(Method method) {
        Class<?> owner = method.getDeclaringClass();
        if (!owner.isRecord() || method.getParameterCount() != 0) {
            return false;
        }
        for (RecordComponent component : owner.getRecordComponents()) {
            if (component.getAccessor().equals(method)) {
                return true;
            }
        }
        return false;
    }

    private record Path(Class<?> sourceType, List<Method> components) {
    }
}
