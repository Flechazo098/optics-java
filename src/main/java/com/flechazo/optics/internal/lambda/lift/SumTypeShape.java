package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.internal.lambda.ast.LambdaExpr;

import java.util.List;
import java.util.Map;

record SumTypeShape(String owner, String read, String builder, String emptyBuilder) {
    private static final Map<String, SumTypeShape> SHAPES = Map.ofEntries(
            Map.entry("com.flechazo.hkt.Maybe#isDefined",
                    new SumTypeShape("com.flechazo.hkt.Maybe", "get", "some", "none")),
            Map.entry("java.util.Optional#isPresent",
                    new SumTypeShape("java.util.Optional", "get", "of", "empty")),
            Map.entry("com.flechazo.hkt.Either#isRight",
                    new SumTypeShape("com.flechazo.hkt.Either", "right", "right", "")),
            Map.entry("com.flechazo.hkt.Either#isLeft",
                    new SumTypeShape("com.flechazo.hkt.Either", "left", "left", "")),
            Map.entry("com.flechazo.hkt.Validated#isValid",
                    new SumTypeShape("com.flechazo.hkt.Validated", "value", "valid", "")),
            Map.entry("com.flechazo.hkt.Validated#isInvalid",
                    new SumTypeShape("com.flechazo.hkt.Validated", "error", "invalid", "")),
            Map.entry("com.flechazo.hkt.Try#isSuccess",
                    new SumTypeShape("com.flechazo.hkt.Try", "get", "success", "")),
            Map.entry("com.flechazo.hkt.Try#isFailure",
                    new SumTypeShape("com.flechazo.hkt.Try", "cause", "failure", "")));

    static Maybe<SumTypeShape> from(LambdaExpr.InstanceCall test) {
        if (!argument(test.receiver(), 0) || !test.arguments().isEmpty()) {
            return Maybe.none();
        }
        return Maybe.ofNullable(SHAPES.get(
                test.method().getDeclaringClass().getName() + "#" + test.method().getName()));
    }

    boolean readsFocus(LambdaExpr expression) {
        expression = strip(expression);
        return expression instanceof LambdaExpr.InstanceCall call
                && call.method().getDeclaringClass().getName().equals(owner)
                && call.method().getName().equals(read)
                && call.arguments().isEmpty()
                && argument(call.receiver(), 0);
    }

    boolean buildsFocus(LambdaExpr expression, int argumentIndex) {
        expression = strip(expression);
        return expression instanceof LambdaExpr.StaticCall call
                && call.method().getDeclaringClass().getName().equals(owner)
                && call.method().getName().equals(builder)
                && call.arguments().size() == 1
                && argument(call.arguments().get(0), argumentIndex);
    }

    boolean preservesMiss(LambdaExpr expression) {
        expression = strip(expression);
        if (argument(expression, 0)) {
            return true;
        }
        return !emptyBuilder.isEmpty()
                && expression instanceof LambdaExpr.StaticCall call
                && call.method().getDeclaringClass().getName().equals(owner)
                && call.method().getName().equals(emptyBuilder)
                && call.arguments().isEmpty();
    }

    static Maybe<LambdaExpr> eitherValue(LambdaExpr expression, String branch) {
        expression = strip(expression);
        if (expression instanceof LambdaExpr.StaticCall call
                && call.method().getDeclaringClass().getName().equals("com.flechazo.hkt.Either")
                && call.method().getName().equals(branch)
                && call.arguments().size() == 1) {
            return Maybe.some(call.arguments().get(0));
        }
        return Maybe.none();
    }

    static boolean argument(LambdaExpr expression, int index) {
        expression = strip(expression);
        return expression instanceof LambdaExpr.Arg argument && argument.index() == index;
    }

    static LambdaExpr strip(LambdaExpr expression) {
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
}
