package com.flechazo.optics.internal.lambda;

import java.lang.invoke.SerializedLambda;
import java.util.List;

public record LambdaDescriptor(
        SerializedLambda serialized,
        ClassLoader classLoader,
        List<Object> captured) {
    public LambdaDescriptor {
        captured = List.copyOf(captured);
    }
}
