package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.optics.internal.lambda.ast.LambdaExpr;
import com.flechazo.hkt.Maybe;

import java.util.List;

public final class PrismLifter {
    public Maybe<LiftedNodeKey> lift(LambdaExpr matcher, LambdaExpr builder) {
        if (!(matcher instanceof LambdaExpr.Conditional conditional)
                || SumTypeShape.eitherValue(conditional.ifTrue(), "right").isEmpty()
                || SumTypeShape.eitherValue(conditional.ifFalse(), "left").isEmpty()) {
            return Maybe.none();
        }
        if (conditional.test() instanceof LambdaExpr.InstanceOf instanceOf) {
            LambdaExpr focus = SumTypeShape.eitherValue(conditional.ifTrue(), "right").get();
            LambdaExpr miss = SumTypeShape.eitherValue(conditional.ifFalse(), "left").get();
            if (!SumTypeShape.argument(instanceOf.value(), 0)
                    || !SumTypeShape.argument(miss, 0)
                    || !subtypeFocus(focus, instanceOf.type())
                    || !subtypeBuilder(builder, instanceOf.type())) {
                return Maybe.none();
            }
            return Maybe.some(new LiftedNodeKey("subtypePrism", List.of(matcher, builder)));
        }
        if (conditional.test() instanceof LambdaExpr.InstanceCall test) {
            Maybe<SumTypeShape> shape = SumTypeShape.from(test);
            if (shape.isDefined()
                    && shape.get().readsFocus(SumTypeShape.eitherValue(conditional.ifTrue(), "right").get())
                    && shape.get().preservesMiss(SumTypeShape.eitherValue(conditional.ifFalse(), "left").get())
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
        if (expression instanceof LambdaExpr.Access access) {
            return access.accessor().getDeclaringClass() == subtype
                    && subtypeReceiver(access.receiver(), subtype);
        }
        if (expression instanceof LambdaExpr.InstanceCall call) {
            return call.method().getDeclaringClass() == subtype
                    && subtypeReceiver(call.receiver(), subtype);
        }
        return subtypeReceiver(expression, subtype);
    }

    private static boolean subtypeReceiver(LambdaExpr expression, Class<?> subtype) {
        return expression instanceof LambdaExpr.Cast cast
                && cast.type() == subtype
                && SumTypeShape.argument(cast.value(), 0);
    }

    private static boolean subtypeBuilder(LambdaExpr builder, Class<?> subtype) {
        if (SumTypeShape.argument(builder, 0)) {
            return true;
        }
        if (!(builder instanceof LambdaExpr.NewRecord creation)
                || creation.constructor().getDeclaringClass() != subtype) {
            return false;
        }
        return creation.arguments().stream().allMatch(argument -> SumTypeShape.argument(argument, 0));
    }
}
