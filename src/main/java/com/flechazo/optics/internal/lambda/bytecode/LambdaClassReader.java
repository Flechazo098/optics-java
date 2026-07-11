package com.flechazo.optics.internal.lambda.bytecode;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.business.util.OptionalOps;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.MethodModel;

import java.io.InputStream;
import java.lang.invoke.SerializedLambda;

public final class LambdaClassReader {
    public Maybe<MethodModel> implementationMethod(SerializedLambda lambda, ClassLoader loader) {
        String resource = lambda.getImplClass() + ".class";
        return Try.of(() -> read(resource, lambda, loader)).toMaybe().flatMap(value -> value);
    }

    private static Maybe<MethodModel> read(
            String resource,
            SerializedLambda lambda,
            ClassLoader loader) throws Exception {
        try (InputStream input = loader.getResourceAsStream(resource)) {
            if (input == null) {
                return Maybe.none();
            }
            ClassModel model = ClassFile.of().parse(input.readAllBytes());
            return OptionalOps.toMaybe(model.methods().stream()
                    .filter(method -> method.methodName().equalsString(lambda.getImplMethodName()))
                    .filter(method -> method.methodType().equalsString(lambda.getImplMethodSignature()))
                    .findFirst());
        }
    }
}
