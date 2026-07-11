package com.flechazo.optics.internal.lambda.lift;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.internal.lambda.ast.LambdaExpr;

public final class FoldLifter {
    private final RecordLensLifter recordLensLifter = new RecordLensLifter();

    public Maybe<RecordPath> lift(LambdaExpr targets) {
        return recordLensLifter.liftGetter(targets);
    }
}
