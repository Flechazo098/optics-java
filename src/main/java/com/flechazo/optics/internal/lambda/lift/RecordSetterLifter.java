package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.internal.lambda.ast.LambdaExpr;

import java.util.ArrayList;
import java.util.List;

public final class RecordSetterLifter {
    private final RecordLensLifter lensLifter = new RecordLensLifter();

    public Maybe<RecordPath> lift(LambdaExpr expression) {
        ArrayList<LambdaExpr.InstanceCall> applications = new ArrayList<>();
        collectApplications(expression, applications);
        if (applications.size() != 1) {
            return Maybe.none();
        }
        LambdaExpr.InstanceCall application = applications.getFirst();
        if (!argument(application.receiver(), 0) || application.arguments().size() != 1) {
            return Maybe.none();
        }
        LambdaExpr getter = replaceArgument(application.arguments().getFirst(), 1, 0);
        LambdaExpr setter = rewriteSetter(expression, application);
        return lensLifter.lift(getter, setter).map(LiftedLensNode::key);
    }

    private static LambdaExpr rewriteSetter(
            LambdaExpr expression,
            LambdaExpr.InstanceCall application) {
        if (expression == application) {
            return new LambdaExpr.Arg(1);
        }
        if (expression instanceof LambdaExpr.Arg argument) {
            if (argument.index() == 0) {
                return new LambdaExpr.OpaqueCall("modifier", "unexpected", "", List.of(expression));
            }
            return argument.index() == 1 ? new LambdaExpr.Arg(0) : argument;
        }
        if (expression instanceof LambdaExpr.Access(LambdaExpr receiver1, java.lang.reflect.Method accessor)) {
            return new LambdaExpr.Access(rewriteSetter(receiver1, application), accessor);
        }
        if (expression instanceof LambdaExpr.NewRecord(
                java.lang.reflect.Constructor<?> constructor, List<LambdaExpr> arguments3
        )) {
            return new LambdaExpr.NewRecord(
                    constructor,
                    arguments3.stream().map(value -> rewriteSetter(value, application)).toList());
        }
        if (expression instanceof LambdaExpr.StaticCall(java.lang.reflect.Method method1, List<LambdaExpr> arguments2)) {
            return new LambdaExpr.StaticCall(
                    method1,
                    arguments2.stream().map(value -> rewriteSetter(value, application)).toList());
        }
        if (expression instanceof LambdaExpr.InstanceCall(
                LambdaExpr receiver, java.lang.reflect.Method method, List<LambdaExpr> arguments1
        )) {
            return new LambdaExpr.InstanceCall(
                    rewriteSetter(receiver, application),
                    method,
                    arguments1.stream().map(value -> rewriteSetter(value, application)).toList());
        }
        if (expression instanceof LambdaExpr.Cast(Class<?> type1, LambdaExpr value4)) {
            return new LambdaExpr.Cast(type1, rewriteSetter(value4, application));
        }
        if (expression instanceof LambdaExpr.Box(Class<?> primitiveType1, LambdaExpr value3)) {
            return new LambdaExpr.Box(primitiveType1, rewriteSetter(value3, application));
        }
        if (expression instanceof LambdaExpr.Unbox(Class<?> primitiveType, LambdaExpr value2)) {
            return new LambdaExpr.Unbox(primitiveType, rewriteSetter(value2, application));
        }
        if (expression instanceof LambdaExpr.Conditional(LambdaExpr test, LambdaExpr ifTrue, LambdaExpr ifFalse)) {
            return new LambdaExpr.Conditional(
                    rewriteSetter(test, application),
                    rewriteSetter(ifTrue, application),
                    rewriteSetter(ifFalse, application));
        }
        if (expression instanceof LambdaExpr.InstanceOf(LambdaExpr value1, Class<?> type)) {
            return new LambdaExpr.InstanceOf(rewriteSetter(value1, application), type);
        }
        if (expression instanceof LambdaExpr.OpaqueCall(
                String owner, String name, String descriptor, List<LambdaExpr> arguments
        )) {
            return new LambdaExpr.OpaqueCall(
                    owner,
                    name,
                    descriptor,
                    arguments.stream().map(value -> rewriteSetter(value, application)).toList());
        }
        return expression;
    }

    private static LambdaExpr replaceArgument(LambdaExpr expression, int from, int to) {
        if (expression instanceof LambdaExpr.Arg argument) {
            return argument.index() == from ? new LambdaExpr.Arg(to) : argument;
        }
        if (expression instanceof LambdaExpr.Access(LambdaExpr receiver, java.lang.reflect.Method accessor)) {
            return new LambdaExpr.Access(replaceArgument(receiver, from, to), accessor);
        }
        if (expression instanceof LambdaExpr.Cast(Class<?> type1, LambdaExpr value2)) {
            return new LambdaExpr.Cast(type1, replaceArgument(value2, from, to));
        }
        if (expression instanceof LambdaExpr.Box(Class<?> type, LambdaExpr value1)) {
            return new LambdaExpr.Box(type, replaceArgument(value1, from, to));
        }
        if (expression instanceof LambdaExpr.Unbox(Class<?> primitiveType, LambdaExpr value)) {
            return new LambdaExpr.Unbox(primitiveType, replaceArgument(value, from, to));
        }
        return expression;
    }

    private static void collectApplications(
            LambdaExpr expression,
            List<LambdaExpr.InstanceCall> applications) {
        if (expression instanceof LambdaExpr.InstanceCall call) {
            if (call.method().getName().equals("apply")
                    && call.method().getDeclaringClass().isAssignableFrom(java.util.function.Function.class)) {
                applications.add(call);
            }
            collectApplications(call.receiver(), applications);
            call.arguments().forEach(value -> collectApplications(value, applications));
        } else if (expression instanceof LambdaExpr.Access access) {
            collectApplications(access.receiver(), applications);
        } else if (expression instanceof LambdaExpr.NewRecord creation) {
            creation.arguments().forEach(value -> collectApplications(value, applications));
        } else if (expression instanceof LambdaExpr.StaticCall call) {
            call.arguments().forEach(value -> collectApplications(value, applications));
        } else if (expression instanceof LambdaExpr.Cast cast) {
            collectApplications(cast.value(), applications);
        } else if (expression instanceof LambdaExpr.Box box) {
            collectApplications(box.value(), applications);
        } else if (expression instanceof LambdaExpr.Unbox unbox) {
            collectApplications(unbox.value(), applications);
        } else if (expression instanceof LambdaExpr.Conditional(LambdaExpr test, LambdaExpr ifTrue, LambdaExpr ifFalse)) {
            collectApplications(test, applications);
            collectApplications(ifTrue, applications);
            collectApplications(ifFalse, applications);
        } else if (expression instanceof LambdaExpr.InstanceOf instanceOf) {
            collectApplications(instanceOf.value(), applications);
        } else if (expression instanceof LambdaExpr.OpaqueCall call) {
            call.arguments().forEach(value -> collectApplications(value, applications));
        }
    }

    private static boolean argument(LambdaExpr expression, int index) {
        while (expression instanceof LambdaExpr.Cast cast) {
            expression = cast.value();
        }
        while (expression instanceof LambdaExpr.Unbox unbox) {
            expression = unbox.value();
        }
        return expression instanceof LambdaExpr.Arg(int index1) && index1 == index;
    }
}
