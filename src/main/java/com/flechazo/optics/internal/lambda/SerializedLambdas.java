package com.flechazo.optics.internal.lambda;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.optics.internal.OpticsLookupResolver;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SerializedLambdas {
    private SerializedLambdas() {
    }

    public static Maybe<LambdaDescriptor> describe(Serializable function) {
        Objects.requireNonNull(function, "function");
        MethodHandles.Lookup callerLookup = OpticsLookupResolver.lookupFor(function.getClass());
        return Try.of(() -> {
            MethodHandles.Lookup lambdaLookup = MethodHandles.privateLookupIn(function.getClass(), callerLookup);
            MethodHandle replacement = lambdaLookup.findVirtual(
                    function.getClass(),
                    "writeReplace",
                    MethodType.methodType(Object.class));
            Object value = invoke(replacement, function);
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

    private static Object invoke(MethodHandle handle, Object receiver) throws Exception {
        try {
            return handle.invoke(receiver);
        } catch (Throwable throwable) {
            if (throwable instanceof Exception exception) {
                throw exception;
            }
            if (throwable instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(throwable);
        }
    }
}
