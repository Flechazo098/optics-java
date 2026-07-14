package com.flechazo.optics.internal.lambda;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;

import com.flechazo.optics.internal.lambda.ast.LambdaExpr;
import com.flechazo.optics.internal.lambda.bytecode.BytecodeAbstractInterpreter;
import com.flechazo.optics.internal.lambda.bytecode.LambdaClassReader;

import java.io.Serializable;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class LambdaAnalyzer {
    private final LambdaClassReader classReader = new LambdaClassReader();
    private final BytecodeAbstractInterpreter interpreter = new BytecodeAbstractInterpreter();

    public Maybe<LambdaExpr> analyze(Serializable function) {
        return SerializedLambdas.describe(function).flatMap(this::analyze);
    }

    private Maybe<LambdaExpr> analyze(LambdaDescriptor descriptor) {
        return Try.of(() -> {
            String name = descriptor.serialized().getImplMethodName();
            if (name.startsWith("lambda$")) {
                return classReader.implementationMethod(descriptor.serialized(), descriptor.classLoader())
                        .flatMap(method -> interpreter.interpret(descriptor, method));
            }
            return directReference(descriptor);
        }).toMaybe().flatMap(value -> value);
    }

    private Maybe<LambdaExpr> directReference(LambdaDescriptor descriptor)
            throws ReflectiveOperationException {
        var lambda = descriptor.serialized();
        ClassLoader loader = descriptor.classLoader();
        Class<?> owner = Class.forName(lambda.getImplClass().replace('/', '.'), false, loader);
        MethodType type = MethodType.fromMethodDescriptorString(lambda.getImplMethodSignature(), loader);
        List<LambdaExpr> arguments = new ArrayList<>();
        for (int index = 0; index < type.parameterCount(); index++) {
            arguments.add(new LambdaExpr.Arg(index));
        }
        if (lambda.getImplMethodName().equals("<init>")) {
            Constructor<?> constructor = owner.getDeclaredConstructor(type.parameterArray());
            return owner.isRecord()
                    ? Maybe.some(new LambdaExpr.NewRecord(constructor, arguments))
                    : Maybe.none();
        }
        Method method = owner.getDeclaredMethod(lambda.getImplMethodName(), type.parameterArray());
        int kind = lambda.getImplMethodKind();
        if (kind == MethodHandleInfo.REF_invokeStatic) {
            return Maybe.some(new LambdaExpr.StaticCall(method, arguments));
        }
        LambdaExpr receiver;
        if (descriptor.captured().isEmpty()) {
            receiver = new LambdaExpr.Arg(0);
        } else {
            receiver = new LambdaExpr.Captured(0, descriptor.captured().getFirst());
        }
        if (method.getParameterCount() == 0 && owner.isRecord()) {
            return Maybe.some(new LambdaExpr.Access(receiver, method));
        }
        return Maybe.some(new LambdaExpr.InstanceCall(receiver, method, arguments));
    }
}
