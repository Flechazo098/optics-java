package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.optics.internal.lambda.ast.LambdaExpr;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.AffinePreview;
import com.flechazo.optics.AffineRebuilder;

import java.util.List;

public final class AffineLifter {
    public Maybe<LiftedNodeKey> lift(LambdaExpr preview, LambdaExpr setter) {
        Maybe<LiftedNodeKey> indexed = liftIndexedContainer(preview, setter);
        if (indexed.isDefined()) {
            return indexed;
        }
        if (!(preview instanceof LambdaExpr.Conditional conditional)
                || !(conditional.test() instanceof LambdaExpr.InstanceCall test)
                || SumTypeShape.eitherValue(conditional.ifTrue(), "right").isEmpty()
                || SumTypeShape.eitherValue(conditional.ifFalse(), "left").isEmpty()) {
            return Maybe.none();
        }
        Maybe<SumTypeShape> shape = SumTypeShape.from(test);
        if (shape.isDefined()
                && shape.get().readsFocus(SumTypeShape.eitherValue(conditional.ifTrue(), "right").get())
                && shape.get().preservesMiss(SumTypeShape.eitherValue(conditional.ifFalse(), "left").get())
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
        if (!(first instanceof LambdaExpr.StaticCall read)
                || !(second instanceof LambdaExpr.StaticCall rebuild)
                || read.method().getDeclaringClass() != AffinePreview.class
                || rebuild.method().getDeclaringClass() != AffineRebuilder.class
                || !read.method().getName().equals(rebuild.method().getName())
                || read.arguments().size() != 2
                || rebuild.arguments().size() != 3
                || !SumTypeShape.argument(read.arguments().get(0), 0)
                || !SumTypeShape.argument(rebuild.arguments().get(0), 0)
                || !read.arguments().get(1).equals(rebuild.arguments().get(1))
                || !SumTypeShape.argument(rebuild.arguments().get(2), 1)) {
            return Maybe.none();
        }
        return switch (read.method().getName()) {
            case "mapValue" -> Maybe.some(new LiftedNodeKey("mapKeyAffine", List.of(preview, setter)));
            case "listIndex" -> Maybe.some(new LiftedNodeKey("listIndexAffine", List.of(preview, setter)));
            default -> Maybe.none();
        };
    }
}
