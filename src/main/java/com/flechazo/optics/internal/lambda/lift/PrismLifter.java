package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.internal.lambda.ast.LambdaExpr;

import java.util.List;

public final class PrismLifter {
    public Maybe<LiftedNodeKey> lift(LambdaExpr matcher, LambdaExpr builder) {
        if (!(matcher instanceof LambdaExpr.Conditional(LambdaExpr test1, LambdaExpr ifTrue, LambdaExpr ifFalse))
                || SumTypeShape.eitherValue(ifTrue, "right").isEmpty()
                || SumTypeShape.eitherValue(ifFalse, "left").isEmpty()) {
            return Maybe.none();
        }
        if (test1 instanceof LambdaExpr.InstanceOf(LambdaExpr value, Class<?> type)) {
            LambdaExpr focus = SumTypeShape.eitherValue(ifTrue, "right").get();
            LambdaExpr miss = SumTypeShape.eitherValue(ifFalse, "left").get();
            if (!SumTypeShape.argument(value, 0)
                    || !SumTypeShape.argument(miss, 0)
                    || !subtypeFocus(focus, type)
                    || !subtypeBuilder(builder, type)) {
                return Maybe.none();
            }
            return Maybe.some(new LiftedNodeKey("subtypePrism", List.of(matcher, builder)));
        }
        if (test1 instanceof LambdaExpr.InstanceCall test) {
            Maybe<SumTypeShape> shape = SumTypeShape.from(test);
            if (shape.isDefined()
                    && shape.get().readsFocus(SumTypeShape.eitherValue(ifTrue, "right").get())
                    && shape.get().preservesMiss(SumTypeShape.eitherValue(ifFalse, "left").get())
                    && shape.get().buildsFocus(builder, 0)) {
                return Maybe.some(new LiftedNodeKey("sumPrism", List.of(matcher, builder)));
            }
        }
        return Maybe.none();
    }

    private static boolean subtypeFocus(LambdaExpr expression, Class<?> subtype) {
        while (expression instanceof LambdaExpr.Box box) {
            expression = box.value();
        }
        while (expression instanceof LambdaExpr.Unbox unbox) {
            expression = unbox.value();
        }
        if (expression instanceof LambdaExpr.Access(LambdaExpr receiver, java.lang.reflect.Method accessor)) {
            return accessor.getDeclaringClass() == subtype
                    && subtypeReceiver(receiver, subtype);
        }
        if (expression instanceof LambdaExpr.InstanceCall call) {
            return call.method().getDeclaringClass() == subtype
                    && subtypeReceiver(call.receiver(), subtype);
        }
        return subtypeReceiver(expression, subtype);
    }

    private static boolean subtypeReceiver(LambdaExpr expression, Class<?> subtype) {
        return expression instanceof LambdaExpr.Cast(Class<?> type, LambdaExpr value)
                && type == subtype
                && SumTypeShape.argument(value, 0);
    }

    private static boolean subtypeBuilder(LambdaExpr builder, Class<?> subtype) {
        if (SumTypeShape.argument(builder, 0)) {
            return true;
        }
        if (!(builder instanceof LambdaExpr.NewRecord(
                java.lang.reflect.Constructor<?> constructor, List<LambdaExpr> arguments
        ))
                || constructor.getDeclaringClass() != subtype) {
            return false;
        }
        return arguments.stream().allMatch(argument -> SumTypeShape.argument(argument, 0));
    }
}
