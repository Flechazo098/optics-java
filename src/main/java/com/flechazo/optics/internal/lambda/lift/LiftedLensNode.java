package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.optics.internal.lambda.ast.LambdaExpr;

public record LiftedLensNode(
        RecordPath key,
        LambdaExpr getter,
        LambdaExpr setter) {
}
