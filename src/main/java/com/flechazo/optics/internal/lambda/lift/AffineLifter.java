package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.AffinePreview;
import com.flechazo.optics.AffineRebuilder;
import com.flechazo.optics.internal.lambda.ast.LambdaExpr;

import java.util.List;

public final class AffineLifter {
    public Maybe<LiftedNodeKey> lift(LambdaExpr preview, LambdaExpr setter) {
        Maybe<LiftedNodeKey> indexed = liftIndexedContainer(preview, setter);
        if (indexed.isDefined()) {
            return indexed;
        }
        if (!(preview instanceof LambdaExpr.Conditional(LambdaExpr test1, LambdaExpr ifTrue, LambdaExpr ifFalse))
                || !(test1 instanceof LambdaExpr.InstanceCall test)
                || SumTypeShape.eitherValue(ifTrue, "right").isEmpty()
                || SumTypeShape.eitherValue(ifFalse, "left").isEmpty()) {
            return Maybe.none();
        }
        Maybe<SumTypeShape> shape = SumTypeShape.from(test);
        if (shape.isDefined()
                && shape.get().readsFocus(SumTypeShape.eitherValue(ifTrue, "right").get())
                && shape.get().preservesMiss(SumTypeShape.eitherValue(ifFalse, "left").get())
                && shape.get().buildsFocus(setter, 1)) {
            String kind = switch (shape.get().owner()) {
                case "com.flechazo.hkt.Maybe" -> "maybeAffine";
                case "java.util.Optional" -> "optionalAffine";
                default -> "sumAffine";
            };
            return Maybe.some(new LiftedNodeKey(kind, List.of(preview, setter)));
        }
        return Maybe.none();
    }

    private static Maybe<LiftedNodeKey> liftIndexedContainer(LambdaExpr preview, LambdaExpr setter) {
        LambdaExpr first = SumTypeShape.strip(preview);
        LambdaExpr second = SumTypeShape.strip(setter);
        if (!(first instanceof LambdaExpr.StaticCall(java.lang.reflect.Method method1, List<LambdaExpr> arguments1))
                || !(second instanceof LambdaExpr.StaticCall(
                java.lang.reflect.Method method, List<LambdaExpr> arguments
        ))
                || method1.getDeclaringClass() != AffinePreview.class
                || method.getDeclaringClass() != AffineRebuilder.class
                || !method1.getName().equals(method.getName())
                || arguments1.size() != 2
                || arguments.size() != 3
                || !SumTypeShape.argument(arguments1.get(0), 0)
                || !SumTypeShape.argument(arguments.get(0), 0)
                || !arguments1.get(1).equals(arguments.get(1))
                || !SumTypeShape.argument(arguments.get(2), 1)) {
            return Maybe.none();
        }
        return switch (method1.getName()) {
            case "mapValue" -> Maybe.some(new LiftedNodeKey("mapKeyAffine", List.of(preview, setter)));
            case "listIndex" -> Maybe.some(new LiftedNodeKey("listIndexAffine", List.of(preview, setter)));
            default -> Maybe.none();
        };
    }
}
