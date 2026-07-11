package com.flechazo.optics.internal.lambda.bytecode;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.optics.internal.lambda.LambdaDescriptor;
import com.flechazo.optics.internal.lambda.ast.LambdaExpr;
import io.smallrye.classfile.CodeElement;
import io.smallrye.classfile.Instruction;
import io.smallrye.classfile.MethodModel;
import io.smallrye.classfile.Opcode;
import io.smallrye.classfile.extras.reflect.AccessFlag;
import io.smallrye.classfile.instruction.ConstantInstruction;
import io.smallrye.classfile.instruction.BranchInstruction;
import io.smallrye.classfile.instruction.ArrayLoadInstruction;
import io.smallrye.classfile.instruction.ArrayStoreInstruction;
import io.smallrye.classfile.instruction.InvokeInstruction;
import io.smallrye.classfile.instruction.InvokeDynamicInstruction;
import io.smallrye.classfile.instruction.LoadInstruction;
import io.smallrye.classfile.instruction.LabelTarget;
import io.smallrye.classfile.instruction.NewObjectInstruction;
import io.smallrye.classfile.instruction.NewReferenceArrayInstruction;
import io.smallrye.classfile.instruction.NewPrimitiveArrayInstruction;
import io.smallrye.classfile.instruction.OperatorInstruction;
import io.smallrye.classfile.instruction.ReturnInstruction;
import io.smallrye.classfile.instruction.StackInstruction;
import io.smallrye.classfile.instruction.StoreInstruction;
import io.smallrye.classfile.instruction.TypeCheckInstruction;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class BytecodeAbstractInterpreter {
    public Maybe<LambdaExpr> interpret(LambdaDescriptor descriptor, MethodModel method) {
        return Try.of(() -> interpretChecked(descriptor, method)).toMaybe().flatMap(value -> value);
    }

    private static Maybe<LambdaExpr> interpretChecked(LambdaDescriptor descriptor, MethodModel method)
            throws ReflectiveOperationException {
            ClassLoader loader = descriptor.classLoader();
            MethodType type = MethodType.fromMethodDescriptorString(
                    descriptor.serialized().getImplMethodSignature(), loader);
            Map<Integer, LambdaExpr> locals = locals(descriptor, method, type);
            if (method.code().orElseThrow().elementStream().anyMatch(BranchInstruction.class::isInstance)) {
                return interpretConditional(descriptor, method, locals);
            }
            ArrayDeque<Object> stack = new ArrayDeque<>();
            for (CodeElement element : method.code().orElseThrow().elementList()) {
                if (!(element instanceof Instruction instruction)) {
                    continue;
                }
                if (instruction instanceof LoadInstruction load) {
                    stack.push(locals.getOrDefault(load.slot(), new LambdaExpr.Arg(load.slot())));
                } else if (instruction instanceof StoreInstruction store) {
                    locals.put(store.slot(), expression(stack.pop()));
                } else if (instruction instanceof ConstantInstruction constant) {
                    stack.push(new LambdaExpr.Constant(constant.constantValue()));
                } else if (instruction instanceof NewObjectInstruction created) {
                    stack.push(new PendingNew(loadClass(created.className().asInternalName(), loader)));
                } else if (instruction instanceof StackInstruction stackInstruction) {
                    applyStack(stackInstruction.opcode(), stack);
                } else if (instruction instanceof TypeCheckInstruction check) {
                    LambdaExpr value = expression(stack.pop());
                    Class<?> target = loadClass(check.type().asInternalName(), loader);
                    stack.push(check.opcode() == Opcode.INSTANCEOF
                            ? new LambdaExpr.InstanceOf(value, target)
                            : new LambdaExpr.Cast(target, value));
                } else if (instruction instanceof InvokeInstruction invoke) {
                    invoke(invoke, stack, loader);
                } else if (instruction instanceof InvokeDynamicInstruction dynamic) {
                    invokeDynamic(dynamic, stack, loader);
                } else if (instruction instanceof ArrayLoadInstruction) {
                    arrayLoad(stack);
                } else if (instruction instanceof ArrayStoreInstruction) {
                    arrayStore(stack);
                } else if (instruction instanceof NewReferenceArrayInstruction array) {
                    newArray(stack, array.componentType().asInternalName());
                } else if (instruction instanceof NewPrimitiveArrayInstruction array) {
                    newArray(stack, array.typeKind().name());
                } else if (instruction instanceof OperatorInstruction operator) {
                    operator(operator, stack);
                } else if (instruction.opcode() == Opcode.ARRAYLENGTH) {
                    LambdaExpr array = expression(stack.pop());
                    stack.push(new LambdaExpr.OpaqueCall("array", "length", "()I", List.of(array)));
                } else if (instruction instanceof ReturnInstruction) {
                    return stack.isEmpty() ? Maybe.none() : Maybe.some(expression(stack.pop()));
                } else if (!ignored(instruction.opcode())) {
                    return Maybe.none();
                }
            }
            return Maybe.none();
    }

    private static Maybe<LambdaExpr> interpretConditional(
            LambdaDescriptor descriptor,
            MethodModel method,
            Map<Integer, LambdaExpr> initialLocals) {
        return Try.of(() -> interpretConditionalChecked(descriptor, method, initialLocals))
                .toMaybe()
                .flatMap(value -> value);
    }

    private static Maybe<LambdaExpr> interpretConditionalChecked(
            LambdaDescriptor descriptor,
            MethodModel method,
            Map<Integer, LambdaExpr> initialLocals) throws ReflectiveOperationException {
            List<CodeElement> elements = method.code().orElseThrow().elementList();
            Map<Object, Integer> labels = new HashMap<>();
            for (int index = 0; index < elements.size(); index++) {
                if (elements.get(index) instanceof LabelTarget target) {
                    labels.put(target.label(), index);
                }
            }
            ArrayDeque<State> pending = new ArrayDeque<>();
            pending.add(new State(0, new ArrayDeque<>(), new HashMap<>(initialLocals), null, true));
            ArrayList<Result> results = new ArrayList<>();
            Set<StateKey> visited = new HashSet<>();
            while (!pending.isEmpty()) {
                State state = pending.removeFirst();
                int pc = state.pc();
                ArrayDeque<Object> stack = new ArrayDeque<>(state.stack());
                Map<Integer, LambdaExpr> locals = new HashMap<>(state.locals());
                while (pc < elements.size()) {
                    CodeElement element = elements.get(pc++);
                    if (!(element instanceof Instruction instruction)) {
                        continue;
                    }
                    if (instruction instanceof BranchInstruction branch) {
                        if (branch.opcode() == Opcode.GOTO || branch.opcode() == Opcode.GOTO_W) {
                            pc = labels.get(branch.target());
                            continue;
                        }
                        if (branch.opcode() != Opcode.IFEQ && branch.opcode() != Opcode.IFNE) {
                            return Maybe.none();
                        }
                        LambdaExpr test = expression(stack.pop());
                        boolean targetTruth = branch.opcode() == Opcode.IFNE;
                        State target = new State(
                                labels.get(branch.target()),
                                new ArrayDeque<>(stack),
                                new HashMap<>(locals),
                                test,
                                targetTruth);
                        State fallthrough = new State(
                                pc,
                                new ArrayDeque<>(stack),
                                new HashMap<>(locals),
                                test,
                                !targetTruth);
                        if (visited.add(new StateKey(target.pc(), target.condition(), target.conditionTruth()))) {
                            pending.add(target);
                        }
                        state = fallthrough;
                        pc = state.pc();
                        stack = new ArrayDeque<>(state.stack());
                        locals = new HashMap<>(state.locals());
                        continue;
                    }
                    if (instruction instanceof ReturnInstruction) {
                        if (!stack.isEmpty()) {
                            results.add(new Result(expression(stack.pop()), state.condition(), state.conditionTruth()));
                        }
                        break;
                    }
                    execute(instruction, stack, locals, descriptor.classLoader());
                }
            }
            if (results.size() == 1) {
                return Maybe.some(results.getFirst().expression());
            }
            if (results.size() == 2) {
                Result first = results.get(0);
                Result second = results.get(1);
                if (first.condition() != null && first.condition().equals(second.condition())
                        && first.conditionTruth() != second.conditionTruth()) {
                    LambdaExpr whenTrue = first.conditionTruth() ? first.expression() : second.expression();
                    LambdaExpr whenFalse = first.conditionTruth() ? second.expression() : first.expression();
                    return Maybe.some(new LambdaExpr.Conditional(first.condition(), whenTrue, whenFalse));
                }
            }
            return Maybe.none();
    }

    private static void execute(
            Instruction instruction,
            ArrayDeque<Object> stack,
            Map<Integer, LambdaExpr> locals,
            ClassLoader loader) throws ReflectiveOperationException {
        if (instruction instanceof LoadInstruction load) {
            stack.push(locals.getOrDefault(load.slot(), new LambdaExpr.Arg(load.slot())));
        } else if (instruction instanceof StoreInstruction store) {
            locals.put(store.slot(), expression(stack.pop()));
        } else if (instruction instanceof ConstantInstruction constant) {
            stack.push(new LambdaExpr.Constant(constant.constantValue()));
        } else if (instruction instanceof NewObjectInstruction created) {
            stack.push(new PendingNew(loadClass(created.className().asInternalName(), loader)));
        } else if (instruction instanceof StackInstruction stackInstruction) {
            applyStack(stackInstruction.opcode(), stack);
        } else if (instruction instanceof TypeCheckInstruction check) {
            LambdaExpr value = expression(stack.pop());
            Class<?> target = loadClass(check.type().asInternalName(), loader);
            stack.push(check.opcode() == Opcode.INSTANCEOF
                    ? new LambdaExpr.InstanceOf(value, target)
                    : new LambdaExpr.Cast(target, value));
        } else if (instruction instanceof InvokeInstruction invoke) {
            invoke(invoke, stack, loader);
        } else if (instruction instanceof InvokeDynamicInstruction dynamic) {
            invokeDynamic(dynamic, stack, loader);
        } else if (instruction instanceof ArrayLoadInstruction) {
            arrayLoad(stack);
        } else if (instruction instanceof ArrayStoreInstruction) {
            arrayStore(stack);
        } else if (instruction instanceof NewReferenceArrayInstruction array) {
            newArray(stack, array.componentType().asInternalName());
        } else if (instruction instanceof NewPrimitiveArrayInstruction array) {
            newArray(stack, array.typeKind().name());
        } else if (instruction instanceof OperatorInstruction operator) {
            operator(operator, stack);
        } else if (instruction.opcode() == Opcode.ARRAYLENGTH) {
            LambdaExpr array = expression(stack.pop());
            stack.push(new LambdaExpr.OpaqueCall("array", "length", "()I", List.of(array)));
        } else if (!ignored(instruction.opcode())) {
            throw new IllegalArgumentException("Unsupported opcode " + instruction.opcode());
        }
    }

    private static Map<Integer, LambdaExpr> locals(
            LambdaDescriptor descriptor,
            MethodModel method,
            MethodType type) {
        Map<Integer, LambdaExpr> locals = new HashMap<>();
        int slot = 0;
        int captured = 0;
        if (!method.flags().has(AccessFlag.STATIC)) {
            Object receiver = descriptor.captured().isEmpty() ? null : descriptor.captured().get(0);
            locals.put(slot++, receiver == null ? new LambdaExpr.Arg(0) : new LambdaExpr.Captured(0, receiver));
            if (receiver != null) {
                captured++;
            }
        }
        int argument = 0;
        for (Class<?> parameter : type.parameterArray()) {
            LambdaExpr value;
            if (captured < descriptor.captured().size()) {
                value = new LambdaExpr.Captured(captured, descriptor.captured().get(captured));
                captured++;
            } else {
                value = new LambdaExpr.Arg(argument++);
            }
            locals.put(slot, value);
            slot += parameter == long.class || parameter == double.class ? 2 : 1;
        }
        return locals;
    }

    private static void invoke(
            InvokeInstruction invoke,
            ArrayDeque<Object> stack,
            ClassLoader loader) throws ReflectiveOperationException {
        MethodType type = MethodType.fromMethodDescriptorString(invoke.type().stringValue(), loader);
        List<LambdaExpr> arguments = new ArrayList<>(type.parameterCount());
        for (int index = type.parameterCount() - 1; index >= 0; index--) {
            arguments.add(0, expression(stack.pop()));
        }
        String ownerName = invoke.owner().asInternalName();
        String name = invoke.name().stringValue();
        Class<?> owner = loadClass(ownerName, loader);
        if (name.equals("<init>")) {
            Object receiver = stack.pop();
            Constructor<?> constructor = owner.getDeclaredConstructor(type.parameterArray());
            constructor.setAccessible(true);
            LambdaExpr result = owner.isRecord()
                    ? new LambdaExpr.NewRecord(constructor, arguments)
                    : new LambdaExpr.OpaqueCall(ownerName, name, invoke.type().stringValue(), arguments);
            replacePending(stack, receiver, result);
            return;
        }
        boolean staticCall = invoke.opcode() == Opcode.INVOKESTATIC;
        LambdaExpr receiver = staticCall ? null : expression(stack.pop());
        Method method = findMethod(owner, name, type.parameterArray());
        LambdaExpr result;
        if (staticCall && name.equals("valueOf") && arguments.size() == 1 && wrapperPrimitive(owner) != null) {
            result = new LambdaExpr.Box(wrapperPrimitive(owner), arguments.get(0));
        } else if (!staticCall
                && arguments.isEmpty()
                && wrapperPrimitive(owner) != null
                && name.equals(wrapperPrimitive(owner).getName() + "Value")) {
            result = new LambdaExpr.Unbox(wrapperPrimitive(owner), receiver);
        } else if (staticCall) {
            result = new LambdaExpr.StaticCall(method, arguments);
        } else if (arguments.isEmpty() && recordAccessor(owner, method)) {
            result = new LambdaExpr.Access(receiver, method);
        } else {
            result = new LambdaExpr.InstanceCall(receiver, method, arguments);
        }
        if (type.returnType() != void.class) {
            stack.push(result);
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>[] parameters)
            throws ReflectiveOperationException {
        Class<?> current = owner;
        while (current != null) {
            Class<?> candidate = current;
            Try<Method> lookup = Try.of(() -> candidate.getDeclaredMethod(name, parameters));
            if (lookup.isSuccess()) {
                Method method = lookup.get();
                method.setAccessible(true);
                return method;
            }
            current = current.getSuperclass();
        }
        Method method = owner.getMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static void invokeDynamic(
            InvokeDynamicInstruction dynamic,
            ArrayDeque<Object> stack,
            ClassLoader loader) {
        MethodType type = MethodType.fromMethodDescriptorString(dynamic.type().stringValue(), loader);
        ArrayList<LambdaExpr> arguments = new ArrayList<>(type.parameterCount());
        for (int index = type.parameterCount() - 1; index >= 0; index--) {
            arguments.addFirst(expression(stack.pop()));
        }
        stack.push(new LambdaExpr.OpaqueCall(
                dynamic.bootstrapMethod().owner().displayName(),
                dynamic.name().stringValue(),
                dynamic.type().stringValue(),
                arguments));
    }

    private static void arrayLoad(ArrayDeque<Object> stack) {
        LambdaExpr index = expression(stack.pop());
        LambdaExpr array = expression(stack.pop());
        stack.push(new LambdaExpr.OpaqueCall("array", "get", "", List.of(array, index)));
    }

    private static void arrayStore(ArrayDeque<Object> stack) {
        LambdaExpr value = expression(stack.pop());
        LambdaExpr index = expression(stack.pop());
        LambdaExpr array = expression(stack.pop());
        stack.push(new LambdaExpr.OpaqueCall("array", "set", "", List.of(array, index, value)));
    }

    private static void newArray(ArrayDeque<Object> stack, String component) {
        LambdaExpr length = expression(stack.pop());
        stack.push(new LambdaExpr.OpaqueCall("array", "new:" + component, "", List.of(length)));
    }

    private static void operator(OperatorInstruction operator, ArrayDeque<Object> stack) {
        LambdaExpr right = expression(stack.pop());
        LambdaExpr left = expression(stack.pop());
        stack.push(new LambdaExpr.OpaqueCall(
                "operator", operator.opcode().name(), "", List.of(left, right)));
    }

    private static boolean recordAccessor(Class<?> owner, Method method) {
        if (!owner.isRecord() || method.getParameterCount() != 0) {
            return false;
        }
        return Arrays.stream(owner.getRecordComponents())
                .map(RecordComponent::getAccessor)
                .anyMatch(method::equals);
    }

    private static Class<?> wrapperPrimitive(Class<?> type) {
        if (type == Boolean.class) return boolean.class;
        if (type == Byte.class) return byte.class;
        if (type == Character.class) return char.class;
        if (type == Short.class) return short.class;
        if (type == Integer.class) return int.class;
        if (type == Long.class) return long.class;
        if (type == Float.class) return float.class;
        if (type == Double.class) return double.class;
        return null;
    }

    private static void applyStack(Opcode opcode, ArrayDeque<Object> stack) {
        if (opcode == Opcode.DUP) {
            stack.push(stack.peek());
        } else if (opcode == Opcode.POP) {
            stack.pop();
        } else if (opcode == Opcode.SWAP) {
            Object first = stack.pop();
            Object second = stack.pop();
            stack.push(first);
            stack.push(second);
        } else {
            throw new IllegalArgumentException("Unsupported stack opcode " + opcode);
        }
    }

    private static void replacePending(ArrayDeque<Object> stack, Object pending, LambdaExpr result) {
        Object[] values = stack.toArray();
        stack.clear();
        for (int index = values.length - 1; index >= 0; index--) {
            stack.push(values[index] == pending ? result : values[index]);
        }
        if (values.length == 0) {
            stack.push(result);
        }
    }

    private static boolean ignored(Opcode opcode) {
        return opcode == Opcode.NOP;
    }

    private static LambdaExpr expression(Object value) {
        if (value instanceof LambdaExpr expression) {
            return expression;
        }
        throw new IllegalArgumentException("Expected expression");
    }

    private static Class<?> loadClass(String internalName, ClassLoader loader) throws ClassNotFoundException {
        if (internalName.startsWith("[")) {
            return Class.forName(internalName.replace('/', '.'), false, loader);
        }
        return Class.forName(internalName.replace('/', '.'), false, loader);
    }

    private record PendingNew(Class<?> type) {
    }

    private record State(
            int pc,
            ArrayDeque<Object> stack,
            Map<Integer, LambdaExpr> locals,
            LambdaExpr condition,
            boolean conditionTruth) {
    }

    private record StateKey(int pc, LambdaExpr condition, boolean conditionTruth) {
    }

    private record Result(LambdaExpr expression, LambdaExpr condition, boolean conditionTruth) {
    }
}
