package com.flechazo.optics.internal.lambda.ast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public sealed interface LambdaExpr
        permits LambdaExpr.Arg,
                LambdaExpr.Captured,
                LambdaExpr.Access,
                LambdaExpr.NewRecord,
                LambdaExpr.StaticCall,
                LambdaExpr.InstanceCall,
                LambdaExpr.Cast,
                LambdaExpr.Box,
                LambdaExpr.Unbox,
                LambdaExpr.Conditional,
                LambdaExpr.InstanceOf,
                LambdaExpr.Constant,
                LambdaExpr.OpaqueCall {
    record Arg(int index) implements LambdaExpr {
    }

    record Captured(int index, Object value) implements LambdaExpr {
    }

    record Access(LambdaExpr receiver, Method accessor) implements LambdaExpr {
    }

    record NewRecord(Constructor<?> constructor, List<LambdaExpr> arguments) implements LambdaExpr {
        public NewRecord {
            arguments = List.copyOf(arguments);
        }
    }

    record StaticCall(Method method, List<LambdaExpr> arguments) implements LambdaExpr {
        public StaticCall {
            arguments = List.copyOf(arguments);
        }
    }

    record InstanceCall(LambdaExpr receiver, Method method, List<LambdaExpr> arguments) implements LambdaExpr {
        public InstanceCall {
            arguments = List.copyOf(arguments);
        }
    }

    record Cast(Class<?> type, LambdaExpr value) implements LambdaExpr {
    }

    record Box(Class<?> primitiveType, LambdaExpr value) implements LambdaExpr {
    }

    record Unbox(Class<?> primitiveType, LambdaExpr value) implements LambdaExpr {
    }

    record Conditional(LambdaExpr test, LambdaExpr ifTrue, LambdaExpr ifFalse) implements LambdaExpr {
    }

    record InstanceOf(LambdaExpr value, Class<?> type) implements LambdaExpr {
    }

    record Constant(Object value) implements LambdaExpr {
    }

    record OpaqueCall(String owner, String name, String descriptor, List<LambdaExpr> arguments)
            implements LambdaExpr {
        public OpaqueCall {
            arguments = List.copyOf(arguments);
        }
    }
}
