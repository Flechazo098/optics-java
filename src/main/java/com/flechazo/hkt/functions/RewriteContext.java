package com.flechazo.hkt.functions;

import com.flechazo.hkt.type.Func;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.TypeUnifier;
import com.flechazo.hkt.type.Types;

import java.util.Arrays;
import java.util.Objects;

public final class RewriteContext {
    private static final ThreadLocal<FunctionTypeCache> FUNCTION_TYPES =
            ThreadLocal.withInitial(FunctionTypeCache::new);

    Func<?, ?> functionType(PointFree<?> expression) {
        Objects.requireNonNull(expression, "expression");
        return PointFreeTypes.requireFunction(expression.type(), expression);
    }

    boolean compatible(Type<?> left, Type<?> right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return PointFreeTypes.compatible(left, right);
    }

    <A, B> Func<A, B> function(Type<A> input, Type<B> output) {
        FunctionTypeCache cache = FUNCTION_TYPES.get();
        cache.activate(this);
        Func<?, ?> cached = cache.get(input, output);
        if (cached != null) {
            return castFunc(cached);
        }
        Func<A, B> created = Types.function(input, output);
        cache.add(input, output, created);
        return created;
    }

    void close() {
        FUNCTION_TYPES.get().close(this);
    }

    @SuppressWarnings("unchecked")
    private static <A, B> Func<A, B> castFunc(Func<?, ?> function) {
        return (Func<A, B>) function;
    }

    private static final class FunctionTypeCache {
        private RewriteContext owner;
        private Type<?> functionInput0;
        private Type<?> functionOutput0;
        private Func<?, ?> functionType0;
        private Type<?> functionInput1;
        private Type<?> functionOutput1;
        private Func<?, ?> functionType1;
        private Type<?> functionInput2;
        private Type<?> functionOutput2;
        private Func<?, ?> functionType2;
        private Type<?> functionInput3;
        private Type<?> functionOutput3;
        private Func<?, ?> functionType3;
        private Type<?>[] overflowFunctionInputs;
        private Type<?>[] overflowFunctionOutputs;
        private Func<?, ?>[] overflowFunctionTypes;
        private int functionTypeCount;

        void activate(RewriteContext context) {
            if (owner != context) {
                clear();
                owner = context;
            }
        }

        void close(RewriteContext context) {
            if (owner == context) {
                clear();
                owner = null;
            }
        }

        void clear() {
            functionInput0 = null;
            functionOutput0 = null;
            functionType0 = null;
            functionInput1 = null;
            functionOutput1 = null;
            functionType1 = null;
            functionInput2 = null;
            functionOutput2 = null;
            functionType2 = null;
            functionInput3 = null;
            functionOutput3 = null;
            functionType3 = null;
            if (overflowFunctionTypes != null) {
                int overflowSize = Math.max(0, functionTypeCount - 4);
                Arrays.fill(overflowFunctionInputs, 0, overflowSize, null);
                Arrays.fill(overflowFunctionOutputs, 0, overflowSize, null);
                Arrays.fill(overflowFunctionTypes, 0, overflowSize, null);
            }
            functionTypeCount = 0;
        }

        Func<?, ?> get(Type<?> input, Type<?> output) {
            for (int i = 0; i < functionTypeCount; i++) {
                Type<?> cachedInput = functionInput(i);
                Type<?> cachedOutput = functionOutput(i);
                if ((cachedInput == input || cachedInput.equals(input))
                        && (cachedOutput == output || cachedOutput.equals(output))) {
                    return functionType(i);
                }
            }
            return null;
        }

        void add(Type<?> input, Type<?> output, Func<?, ?> function) {
            switch (functionTypeCount++) {
                case 0 -> {
                    functionInput0 = input;
                    functionOutput0 = output;
                    functionType0 = function;
                }
                case 1 -> {
                    functionInput1 = input;
                    functionOutput1 = output;
                    functionType1 = function;
                }
                case 2 -> {
                    functionInput2 = input;
                    functionOutput2 = output;
                    functionType2 = function;
                }
                case 3 -> {
                    functionInput3 = input;
                    functionOutput3 = output;
                    functionType3 = function;
                }
                default -> addOverflow(input, output, function);
            }
        }

        private void addOverflow(Type<?> input, Type<?> output, Func<?, ?> function) {
            int index = functionTypeCount - 5;
            if (overflowFunctionTypes == null) {
                overflowFunctionInputs = new Type<?>[4];
                overflowFunctionOutputs = new Type<?>[4];
                overflowFunctionTypes = new Func<?, ?>[4];
            } else if (index == overflowFunctionTypes.length) {
                int nextSize = overflowFunctionTypes.length * 2;
                overflowFunctionInputs = Arrays.copyOf(overflowFunctionInputs, nextSize);
                overflowFunctionOutputs = Arrays.copyOf(overflowFunctionOutputs, nextSize);
                overflowFunctionTypes = Arrays.copyOf(overflowFunctionTypes, nextSize);
            }
            overflowFunctionInputs[index] = input;
            overflowFunctionOutputs[index] = output;
            overflowFunctionTypes[index] = function;
        }

        private Type<?> functionInput(int index) {
            return switch (index) {
                case 0 -> functionInput0;
                case 1 -> functionInput1;
                case 2 -> functionInput2;
                case 3 -> functionInput3;
                default -> overflowFunctionInputs[index - 4];
            };
        }

        private Type<?> functionOutput(int index) {
            return switch (index) {
                case 0 -> functionOutput0;
                case 1 -> functionOutput1;
                case 2 -> functionOutput2;
                case 3 -> functionOutput3;
                default -> overflowFunctionOutputs[index - 4];
            };
        }

        private Func<?, ?> functionType(int index) {
            return switch (index) {
                case 0 -> functionType0;
                case 1 -> functionType1;
                case 2 -> functionType2;
                case 3 -> functionType3;
                default -> overflowFunctionTypes[index - 4];
            };
        }
    }
}
