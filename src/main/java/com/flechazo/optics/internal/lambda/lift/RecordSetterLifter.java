package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.optics.internal.lambda.ast.LambdaExpr;
import com.flechazo.hkt.Maybe;

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
        LambdaExpr.InstanceCall application = applications.get(0);
        if (!argument(application.receiver(), 0) || application.arguments().size() != 1) {
            return Maybe.none();
        }
        LambdaExpr getter = replaceArgument(application.arguments().get(0), 1, 0);
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
        if (expression instanceof LambdaExpr.Access access) {
            return new LambdaExpr.Access(rewriteSetter(access.receiver(), application), access.accessor());
        }
        if (expression instanceof LambdaExpr.NewRecord creation) {
            return new LambdaExpr.NewRecord(
                    creation.constructor(),
                    creation.arguments().stream().map(value -> rewriteSetter(value, application)).toList());
        }
        if (expression instanceof LambdaExpr.StaticCall call) {
            return new LambdaExpr.StaticCall(
                    call.method(),
                    call.arguments().stream().map(value -> rewriteSetter(value, application)).toList());
        }
        if (expression instanceof LambdaExpr.InstanceCall call) {
            return new LambdaExpr.InstanceCall(
                    rewriteSetter(call.receiver(), application),
                    call.method(),
                    call.arguments().stream().map(value -> rewriteSetter(value, application)).toList());
        }
        if (expression instanceof LambdaExpr.Cast cast) {
            return new LambdaExpr.Cast(cast.type(), rewriteSetter(cast.value(), application));
        }
        if (expression instanceof LambdaExpr.Box box) {
            return new LambdaExpr.Box(box.primitiveType(), rewriteSetter(box.value(), application));
        }
        if (expression instanceof LambdaExpr.Unbox unbox) {
            return new LambdaExpr.Unbox(unbox.primitiveType(), rewriteSetter(unbox.value(), application));
        }
        if (expression instanceof LambdaExpr.Conditional conditional) {
            return new LambdaExpr.Conditional(
                    rewriteSetter(conditional.test(), application),
                    rewriteSetter(conditional.ifTrue(), application),
                    rewriteSetter(conditional.ifFalse(), application));
        }
        if (expression instanceof LambdaExpr.InstanceOf instanceOf) {
            return new LambdaExpr.InstanceOf(rewriteSetter(instanceOf.value(), application), instanceOf.type());
        }
        if (expression instanceof LambdaExpr.OpaqueCall call) {
            return new LambdaExpr.OpaqueCall(
                    call.owner(),
                    call.name(),
                    call.descriptor(),
                    call.arguments().stream().map(value -> rewriteSetter(value, application)).toList());
        }
        return expression;
    }

    private static LambdaExpr replaceArgument(LambdaExpr expression, int from, int to) {
        if (expression instanceof LambdaExpr.Arg argument) {
            return argument.index() == from ? new LambdaExpr.Arg(to) : argument;
        }
        if (expression instanceof LambdaExpr.Access access) {
            return new LambdaExpr.Access(replaceArgument(access.receiver(), from, to), access.accessor());
        }
        if (expression instanceof LambdaExpr.Cast cast) {
            return new LambdaExpr.Cast(cast.type(), replaceArgument(cast.value(), from, to));
        }
        if (expression instanceof LambdaExpr.Box box) {
            return new LambdaExpr.Box(box.primitiveType(), replaceArgument(box.value(), from, to));
        }
        if (expression instanceof LambdaExpr.Unbox unbox) {
            return new LambdaExpr.Unbox(unbox.primitiveType(), replaceArgument(unbox.value(), from, to));
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
        } else if (expression instanceof LambdaExpr.Conditional conditional) {
            collectApplications(conditional.test(), applications);
            collectApplications(conditional.ifTrue(), applications);
            collectApplications(conditional.ifFalse(), applications);
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
        return expression instanceof LambdaExpr.Arg argument && argument.index() == index;
    }
}
