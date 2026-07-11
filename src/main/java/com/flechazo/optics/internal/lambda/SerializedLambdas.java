package com.flechazo.optics.internal.lambda;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class SerializedLambdas {
    private SerializedLambdas() {
    }

    public static Maybe<LambdaDescriptor> describe(Serializable function) {
        return Try.of(() -> {
            Method replacement = function.getClass().getDeclaredMethod("writeReplace");
            replacement.setAccessible(true);
            Object value = replacement.invoke(function);
            if (!(value instanceof SerializedLambda lambda)) {
                return Maybe.<LambdaDescriptor>none();
            }
            List<Object> captured = new ArrayList<>(lambda.getCapturedArgCount());
            for (int index = 0; index < lambda.getCapturedArgCount(); index++) {
                captured.add(lambda.getCapturedArg(index));
            }
            return Maybe.some(new LambdaDescriptor(lambda, function.getClass().getClassLoader(), captured));
        }).toMaybe().flatMap(value -> value);
    }
}
