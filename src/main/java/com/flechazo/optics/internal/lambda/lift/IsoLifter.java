package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.internal.lambda.ast.LambdaExpr;

import java.lang.reflect.RecordComponent;
import java.util.List;

public final class IsoLifter {
    public Maybe<LiftedNodeKey> lift(LambdaExpr getter, LambdaExpr reverse) {
        if (!(getter instanceof LambdaExpr.Access(LambdaExpr receiver, java.lang.reflect.Method accessor))
                || !argument(receiver, 0)
                || !(reverse instanceof LambdaExpr.NewRecord(
                java.lang.reflect.Constructor<?> constructor, List<LambdaExpr> arguments
        ))
                || arguments.size() != 1
                || !argument(arguments.getFirst(), 0)) {
            return Maybe.none();
        }
        Class<?> recordType = constructor.getDeclaringClass();
        RecordComponent[] components = recordType.getRecordComponents();
        if (components.length != 1 || !components[0].getAccessor().equals(accessor)) {
            return Maybe.none();
        }
        return Maybe.some(new LiftedNodeKey("iso", List.of(getter, reverse)));
    }

    private static boolean argument(LambdaExpr expression, int index) {
        while (expression instanceof LambdaExpr.Cast cast) {
            expression = cast.value();
        }
        while (expression instanceof LambdaExpr.Box box) {
            expression = box.value();
        }
        while (expression instanceof LambdaExpr.Unbox unbox) {
            expression = unbox.value();
        }
        return expression instanceof LambdaExpr.Arg(int index1) && index1 == index;
    }
}
