package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.optics.internal.lambda.ast.LambdaExpr;

import java.util.List;

public record LiftedNodeKey(String kind, List<LambdaExpr> expressions) {
    public LiftedNodeKey {
        expressions = List.copyOf(expressions);
    }
}
