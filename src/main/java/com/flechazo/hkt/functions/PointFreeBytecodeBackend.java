package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.type.Func;
import com.flechazo.optics.util.Optionals;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.CodeBuilder;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public final class PointFreeBytecodeBackend {
    private static final AtomicLong GENERATED_ID = new AtomicLong();
    private static final ClassDesc CD_OBJECT_ARRAY = ConstantDescs.CD_Object.arrayType();
    private static final ClassDesc CD_POINT_FREE = classDesc(PointFree.class);
    private static final ClassDesc CD_FUNCTION = classDesc(Function.class);
    private static final ClassDesc CD_POINT_FREE_OPTIC = classDesc(PointFreeOptic.class);
    private static final ClassDesc CD_FOLD_QUERY = classDesc(FoldQuery.class);
    private static final ClassDesc CD_CATA_PLAN = classDesc(CataPlan.class);
    private static final ClassDesc CD_RECORD_LENS_OPTIC_ELEMENT = classDesc(RecordLensOpticElement.class);
    private static final ClassDesc CD_OPTIONALS = classDesc(Optionals.class);
    private static final ClassDesc CD_OPTIONAL = classDesc(Optional.class);
    private static final ClassDesc CD_MAYBE = classDesc(Maybe.class);
    private static final ClassDesc CD_ARRAY = classDesc(Array.class);
    private static final ClassDesc CD_COLLECTION = classDesc(Collection.class);
    private static final ClassDesc CD_ITERABLE = classDesc(Iterable.class);
    private static final ClassDesc CD_ITERATOR = classDesc(Iterator.class);
    private static final ClassDesc CD_MAP = classDesc(Map.class);
    private static final ClassDesc CD_MAP_ENTRY = classDesc(Map.Entry.class);
    private static final ClassDesc CD_IMMUTABLE_LIST = classDesc(ImmutableList.class);
    private static final ClassDesc CD_IMMUTABLE_LIST_BUILDER = ClassDesc.of("com.google.common.collect.ImmutableList$Builder");
    private static final ClassDesc CD_IMMUTABLE_SET = classDesc(ImmutableSet.class);
    private static final ClassDesc CD_IMMUTABLE_SET_BUILDER = ClassDesc.of("com.google.common.collect.ImmutableSet$Builder");
    private static final ClassDesc CD_IMMUTABLE_MAP = classDesc(ImmutableMap.class);
    private static final ClassDesc CD_IMMUTABLE_MAP_BUILDER = ClassDesc.of("com.google.common.collect.ImmutableMap$Builder");
    private static final ClassDesc CD_GENERATED_EXECUTOR = classDesc(GeneratedPointFreeExecutor.class);
    private static final ClassDesc CD_GENERATED_FUNCTION = classDesc(GeneratedPointFreeFunction.class);
    private static final ClassDesc CD_UNIT = classDesc(Unit.class);

    private PointFreeBytecodeBackend() {
    }

    public static <A> PointFreeExecutor<A> compile(PointFree<A> plan) {
        return compile(plan, MethodHandles.lookup());
    }

    public static <A> PointFreeExecutor<A> compile(PointFree<A> plan, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(lookup, "lookup");
        PointFree<A> optimized = PointFreeOptimizer.optimize(plan);
        Compiler compiler = new Compiler(lookup);
        return compiler.compileExecutor(optimized);
    }

    public static byte[] generateExecutorBytes(PointFree<?> optimizedPlan) {
        Objects.requireNonNull(optimizedPlan, "optimizedPlan");
        Compiler compiler = new Compiler(MethodHandles.lookup());
        return compiler.generateExecutorBytes(compiler.executorClassName(), optimizedPlan, compiler.canEmitValue(optimizedPlan));
    }

    public static boolean isGeneratedFunction(Object value) {
        return value instanceof GeneratedPointFreeFunction;
    }

    public static boolean generatedFunctionUsesInterpretedFallback(Object value) {
        return value instanceof GeneratedPointFreeFunction function && function.usesInterpretedFallback();
    }

    private record Compiler(MethodHandles.Lookup lookup) {

        private <A> PointFreeExecutor<A> compileExecutor(PointFree<A> optimizedPlan) {
                boolean specialized = canEmitValue(optimizedPlan);
                String className = executorClassName();
                ConstantTable constants = new ConstantTable();
                byte[] bytes = generateExecutorBytes(className, optimizedPlan, specialized, constants);
                try {
                    Class<?> executorClass = defineHidden(bytes);
                    Constructor<?> constructor =
                            executorClass.getDeclaredConstructor(Object[].class, PointFree.class, boolean.class);
                    return cast(constructor.newInstance(constants.toArray(), optimizedPlan, !specialized));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Unable to define generated point-free executor", e);
                }
            }

            private byte[] generateExecutorBytes(String className, PointFree<?> optimizedPlan, boolean specialized) {
                return generateExecutorBytes(className, optimizedPlan, specialized, new ConstantTable());
            }

            private byte[] generateExecutorBytes(
                    String className,
                    PointFree<?> optimizedPlan,
                    boolean specialized,
                    ConstantTable constants) {
                ClassDesc generatedClass = ClassDesc.of(className);
                return ClassFile.of()
                        .build(
                                generatedClass,
                                classBuilder ->
                                        classBuilder
                                                .withVersion(ClassFile.JAVA_21_VERSION, 0)
                                                .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SUPER)
                                                .withSuperclass(CD_GENERATED_EXECUTOR)
                                                .withMethodBody(
                                                        "<init>",
                                                        MethodTypeDesc.of(
                                                                ConstantDescs.CD_void,
                                                                CD_OBJECT_ARRAY,
                                                                CD_POINT_FREE,
                                                                ConstantDescs.CD_boolean),
                                                        ClassFile.ACC_PUBLIC,
                                                        code ->
                                                                code.aload(0)
                                                                        .aload(1)
                                                                        .aload(2)
                                                                        .iload(3)
                                                                        .invokespecial(
                                                                                CD_GENERATED_EXECUTOR,
                                                                                "<init>",
                                                                                MethodTypeDesc.of(
                                                                                        ConstantDescs.CD_void,
                                                                                        CD_OBJECT_ARRAY,
                                                                                        CD_POINT_FREE,
                                                                                        ConstantDescs.CD_boolean))
                                                                        .return_())
                                                .withMethodBody(
                                                        "executeRaw",
                                                        MethodTypeDesc.of(ConstantDescs.CD_Object),
                                                        ClassFile.ACC_PROTECTED,
                                                        code -> {
                                                            if (specialized) {
                                                                emitValue(code, constants, CD_GENERATED_EXECUTOR, optimizedPlan);
                                                                code.areturn();
                                                            } else {
                                                                code.aload(0)
                                                                        .invokevirtual(
                                                                                CD_GENERATED_EXECUTOR,
                                                                                "fallbackRaw",
                                                                                MethodTypeDesc.of(ConstantDescs.CD_Object))
                                                                        .areturn();
                                                            }
                                                        }));
            }

            private Function<Object, Object> compileFunction(PointFree<?> expression) {
                PointFree<?> unwrapped = unwrap(expression);
                boolean specialized = canEmitFunction(unwrapped);
                String className = functionClassName();
                ConstantTable constants = new ConstantTable();
                Function<Object, Object> fallback = cast(unwrapped.eval());
                byte[] bytes = generateFunctionBytes(className, unwrapped, specialized, constants);
                try {
                Class<?> functionClass = defineHidden(bytes);
                Constructor<?> constructor =
                        functionClass.getDeclaredConstructor(Object[].class, PointFree.class, Function.class, boolean.class);
                return cast(constructor.newInstance(constants.toArray(), unwrapped, fallback, !specialized));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Unable to define generated point-free function", e);
                }
            }

            private byte[] generateFunctionBytes(
                    String className,
                    PointFree<?> expression,
                    boolean specialized,
                    ConstantTable constants) {
                ClassDesc generatedClass = ClassDesc.of(className);
                return ClassFile.of()
                        .build(
                                generatedClass,
                                classBuilder ->
                                        classBuilder
                                                .withVersion(ClassFile.JAVA_21_VERSION, 0)
                                                .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SUPER)
                                                .withSuperclass(CD_GENERATED_FUNCTION)
                                                .withMethodBody(
                                                        "<init>",
                                                    MethodTypeDesc.of(
                                                            ConstantDescs.CD_void,
                                                            CD_OBJECT_ARRAY,
                                                            CD_POINT_FREE,
                                                            CD_FUNCTION,
                                                            ConstantDescs.CD_boolean),
                                                    ClassFile.ACC_PUBLIC,
                                                    code ->
                                                            code.aload(0)
                                                                    .aload(1)
                                                                    .aload(2)
                                                                    .aload(3)
                                                                    .iload(4)
                                                                    .invokespecial(
                                                                            CD_GENERATED_FUNCTION,
                                                                            "<init>",
                                                                            MethodTypeDesc.of(
                                                                                    ConstantDescs.CD_void,
                                                                                    CD_OBJECT_ARRAY,
                                                                                    CD_POINT_FREE,
                                                                                    CD_FUNCTION,
                                                                                    ConstantDescs.CD_boolean))
                                                                    .return_())
                                                .withMethodBody(
                                                        "apply",
                                                        MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object),
                                                        ClassFile.ACC_PUBLIC,
                                                        code -> {
                                                            if (specialized) {
                                                                emitFunctionBody(code, constants, expression);
                                                            } else {
                                                                code.aload(0)
                                                                        .aload(1)
                                                                        .invokevirtual(
                                                                                CD_GENERATED_FUNCTION,
                                                                                "fallbackApply",
                                                                                MethodTypeDesc.of(
                                                                                        ConstantDescs.CD_Object,
                                                                                        ConstantDescs.CD_Object))
                                                                        .areturn();
                                                            }
                                                        }));
            }

            private void emitValue(
                    CodeBuilder code,
                    ConstantTable constants,
                    ClassDesc constantsOwner,
                    PointFree<?> expression) {
                PointFree<?> unwrapped = unwrap(expression);
                switch (unwrapped) {
                    case Value<?>(Object value, var ignoredType) -> emitConstant(code, constants, constantsOwner, value);
                    case AppExpr<?, ?> app -> {
                        emitConstant(code, constants, constantsOwner, compileFunction(app.function()));
                        code.checkcast(CD_FUNCTION);
                        emitValue(code, constants, constantsOwner, app.argument());
                        code.invokeinterface(
                                CD_FUNCTION,
                                "apply",
                                MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object));
                    }
                    default -> {
                        if (isFunctionExpression(unwrapped)) {
                            emitConstant(code, constants, constantsOwner, compileFunction(unwrapped));
                        } else {
                            throw new IllegalArgumentException("Unsupported specialized value expression: " + unwrapped);
                        }
                    }
                }
            }

            private void emitFunctionBody(CodeBuilder code, ConstantTable constants, PointFree<?> expression) {
                PointFree<?> unwrapped = unwrap(expression);
                switch (unwrapped) {
                    case Id<?> ignored -> code.aload(1).areturn();
                    case In<?> ignored -> code.aload(1).areturn();
                    case Out<?> ignored -> code.aload(1).areturn();
                    case Bang<?> ignored -> code.getstatic(CD_UNIT, "INSTANCE", CD_UNIT)
                            .areturn();
                    case Fn<?, ?> fn -> {
                        emitConstant(code, constants, CD_GENERATED_FUNCTION, fn.function());
                        code.checkcast(CD_FUNCTION)
                                .aload(1)
                                .invokeinterface(
                                        CD_FUNCTION,
                                        "apply",
                                        MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
                                .areturn();
                    }
                    case OpticApp<?, ?, ?, ?> opticApp -> {
                        if (emitGeneratedOpticApp(code, constants, opticApp)) {
                            return;
                        }
                        emitConstant(code, constants, CD_GENERATED_FUNCTION, opticApp.optic());
                        code.checkcast(CD_POINT_FREE_OPTIC);
                        emitConstant(code, constants, CD_GENERATED_FUNCTION, compileFunction(opticApp.function()));
                        code.checkcast(CD_FUNCTION)
                                .aload(1)
                                .invokeinterface(
                                        CD_POINT_FREE_OPTIC,
                                        "modify",
                                        MethodTypeDesc.of(
                                                ConstantDescs.CD_Object,
                                                CD_FUNCTION,
                                                ConstantDescs.CD_Object))
                                .areturn();
                    }
                    case FoldQuery<?, ?, ?, ?> query -> {
                        emitConstant(code, constants, CD_GENERATED_FUNCTION, query);
                        code.checkcast(CD_FOLD_QUERY)
                                .aload(1)
                                .invokevirtual(
                                        CD_FOLD_QUERY,
                                        "run",
                                        MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
                                .areturn();
                    }
                    case CataPlan<?> cata -> {
                        emitConstant(code, constants, CD_GENERATED_FUNCTION, cata);
                        code.checkcast(CD_CATA_PLAN)
                                .invokevirtual(
                                        CD_CATA_PLAN,
                                        "eval",
                                        MethodTypeDesc.of(CD_FUNCTION))
                                .aload(1)
                                .invokeinterface(
                                        CD_FUNCTION,
                                        "apply",
                                        MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
                                .areturn();
                    }
                    case Comp<?, ?> comp -> {
                        code.aload(1).astore(2);
                        List<PointFree<? extends Function<?, ?>>> functions = comp.functions();
                        for (int i = functions.size() - 1; i >= 0; i--) {
                            emitConstant(code, constants, CD_GENERATED_FUNCTION, compileFunction(functions.get(i)));
                            code.checkcast(CD_FUNCTION)
                                    .aload(2)
                                    .invokeinterface(
                                            CD_FUNCTION,
                                            "apply",
                                            MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
                                    .astore(2);
                        }
                        code.aload(2).areturn();
                    }
                    default -> code.aload(0)
                            .aload(1)
                            .invokevirtual(
                                    CD_GENERATED_FUNCTION,
                                    "fallbackApply",
                                    MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
                            .areturn();
                }
            }

            private boolean emitGeneratedOpticApp(
                    CodeBuilder code, ConstantTable constants, OpticApp<?, ?, ?, ?> opticApp) {
                List<PointFreeOpticElement> elements = opticApp.optic().elements();
                if (elements.isEmpty()) {
                    return false;
                }
                if (elements.stream().allMatch(element -> element instanceof RecordLensOpticElement)) {
                    emitRecordLensPathOpticApp(code, constants, opticApp.function(), recordLensElements(elements));
                    return true;
                }
                if (elements.size() == 1 && elements.getFirst() instanceof RecordTraversalOpticElement traversal) {
                    emitRecordTraversalOpticApp(code, constants, opticApp.function(), traversal);
                    return true;
                }
                if (elements.size() == 1 && elements.getFirst() instanceof SubtypeOpticElement subtype) {
                    emitSubtypeOpticApp(code, constants, opticApp.function(), subtype);
                    return true;
                }
                return false;
            }

            private void emitRecordLensPathOpticApp(
                    CodeBuilder code,
                    ConstantTable constants,
                    PointFree<?> function,
                    List<RecordLensOpticElement> elements) {
                int sourcesBase = 2;
                int replacementSlot = sourcesBase + elements.size();
                code.aload(1);
                for (int i = 0; i < elements.size(); i++) {
                    RecordLensOpticElement element = elements.get(i);
                    code.dup().astore(sourcesBase + i);
                    if (canDirectRecordAccess(element)) {
                        emitRecordComponentGet(code, element);
                    } else {
                        code.astore(replacementSlot);
                        emitConstant(code, constants, CD_GENERATED_FUNCTION, element);
                        code.checkcast(CD_RECORD_LENS_OPTIC_ELEMENT)
                                .aload(replacementSlot)
                                .invokevirtual(
                                        CD_RECORD_LENS_OPTIC_ELEMENT,
                                        "readComponent",
                                        MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object));
                    }
                }
                code.astore(replacementSlot);
                emitConstant(code, constants, CD_GENERATED_FUNCTION, compileFunction(function));
                code.checkcast(CD_FUNCTION)
                        .aload(replacementSlot)
                        .invokeinterface(
                                CD_FUNCTION,
                                "apply",
                                MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
                        .astore(replacementSlot);
                for (int i = elements.size() - 1; i >= 0; i--) {
                    RecordLensOpticElement element = elements.get(i);
                    if (canDirectRecordAccess(element)) {
                        emitRecordRebuild(code, element, sourcesBase + i, replacementSlot);
                    } else {
                        emitConstant(code, constants, CD_GENERATED_FUNCTION, element);
                        code.checkcast(CD_RECORD_LENS_OPTIC_ELEMENT)
                                .aload(replacementSlot)
                                .aload(sourcesBase + i)
                                .invokevirtual(
                                        CD_RECORD_LENS_OPTIC_ELEMENT,
                                        "rebuild",
                                        MethodTypeDesc.of(
                                                ConstantDescs.CD_Object,
                                                ConstantDescs.CD_Object,
                                                ConstantDescs.CD_Object));
                    }
                    code.astore(replacementSlot);
                }
                code.aload(replacementSlot).areturn();
            }

            private void emitRecordTraversalOpticApp(
                    CodeBuilder code,
                    ConstantTable constants,
                    PointFree<?> function,
                    RecordTraversalOpticElement traversal) {
                RecordLensOpticElement component = traversal.component();
                int functionSlot = 2;
                int containerSlot = 3;
                int replacementSlot = 4;
                emitConstant(code, constants, CD_GENERATED_FUNCTION, compileFunction(function));
                code.checkcast(CD_FUNCTION).astore(functionSlot);
                if (canDirectRecordAccess(component)) {
                    code.aload(1);
                    emitRecordComponentGet(code, component);
                } else {
                    emitConstant(code, constants, CD_GENERATED_FUNCTION, component);
                    code.checkcast(CD_RECORD_LENS_OPTIC_ELEMENT)
                            .aload(1)
                            .invokevirtual(
                                    CD_RECORD_LENS_OPTIC_ELEMENT,
                                    "readComponent",
                                    MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object));
                }
                if (traversal.optionalContainer()) {
                    code.checkcast(CD_OPTIONAL)
                            .invokestatic(
                                    CD_OPTIONALS,
                                    "toMaybe",
                                    MethodTypeDesc.of(CD_MAYBE, CD_OPTIONAL));
                }
                code.astore(containerSlot);
                emitInlineTraversalContainerModify(code, traversal, functionSlot, containerSlot, replacementSlot);
                if (traversal.optionalContainer()) {
                    code.aload(replacementSlot)
                            .checkcast(CD_MAYBE)
                            .invokestatic(
                                    CD_OPTIONALS,
                                    "fromMaybe",
                                    MethodTypeDesc.of(CD_OPTIONAL, CD_MAYBE))
                            .astore(replacementSlot);
                }
                if (canDirectRecordAccess(component)) {
                    emitRecordRebuild(code, component, 1, replacementSlot);
                } else {
                    emitConstant(code, constants, CD_GENERATED_FUNCTION, component);
                    code.checkcast(CD_RECORD_LENS_OPTIC_ELEMENT)
                            .aload(replacementSlot)
                            .aload(1)
                            .invokevirtual(
                                    CD_RECORD_LENS_OPTIC_ELEMENT,
                                    "rebuild",
                                    MethodTypeDesc.of(
                                            ConstantDescs.CD_Object,
                                            ConstantDescs.CD_Object,
                                            ConstantDescs.CD_Object));
                }
                code.areturn();
            }

            private void emitInlineTraversalContainerModify(
                    CodeBuilder code,
                    RecordTraversalOpticElement traversal,
                    int functionSlot,
                    int containerSlot,
                    int replacementSlot) {
                switch (traversal.containerKind()) {
                    case GeneratedTraversalRuntime.LIST ->
                            emitImmutableCollectionTraversal(
                                    code,
                                    functionSlot,
                                    containerSlot,
                                    replacementSlot,
                                    CD_IMMUTABLE_LIST,
                                    CD_IMMUTABLE_LIST_BUILDER);
                    case GeneratedTraversalRuntime.SET ->
                            emitImmutableCollectionTraversal(
                                    code,
                                    functionSlot,
                                    containerSlot,
                                    replacementSlot,
                                    CD_IMMUTABLE_SET,
                                    CD_IMMUTABLE_SET_BUILDER);
                    case GeneratedTraversalRuntime.MAP_VALUES ->
                            emitImmutableMapValueTraversal(code, functionSlot, containerSlot, replacementSlot);
                    case GeneratedTraversalRuntime.MAYBE ->
                            emitMaybeTraversal(code, functionSlot, containerSlot, replacementSlot);
                    case GeneratedTraversalRuntime.ARRAY ->
                            emitArrayTraversal(
                                    code,
                                    functionSlot,
                                    containerSlot,
                                    replacementSlot,
                                    GeneratedTraversalRuntime.requireArrayComponentType(traversal.arrayComponentType()));
                    default -> throw new IllegalArgumentException(
                            "Unsupported generated traversal kind: " + traversal.containerKind());
                }
            }

            private void emitImmutableCollectionTraversal(
                    CodeBuilder code,
                    int functionSlot,
                    int containerSlot,
                    int replacementSlot,
                    ClassDesc immutableCollection,
                    ClassDesc builderDesc) {
                int iteratorSlot = 5;
                var loop = code.newLabel();
                var done = code.newLabel();
                code.aload(containerSlot)
                        .checkcast(CD_COLLECTION)
                        .invokeinterface(CD_COLLECTION, "size", MethodTypeDesc.of(ConstantDescs.CD_int))
                        .invokestatic(
                                immutableCollection,
                                "builderWithExpectedSize",
                                MethodTypeDesc.of(builderDesc, ConstantDescs.CD_int))
                        .astore(replacementSlot)
                        .aload(containerSlot)
                        .checkcast(CD_ITERABLE)
                        .invokeinterface(CD_ITERABLE, "iterator", MethodTypeDesc.of(CD_ITERATOR))
                        .astore(iteratorSlot)
                        .labelBinding(loop)
                        .aload(iteratorSlot)
                        .invokeinterface(CD_ITERATOR, "hasNext", MethodTypeDesc.of(ConstantDescs.CD_boolean))
                        .ifeq(done)
                        .aload(replacementSlot)
                        .aload(functionSlot)
                        .aload(iteratorSlot)
                        .invokeinterface(CD_ITERATOR, "next", MethodTypeDesc.of(ConstantDescs.CD_Object))
                        .invokeinterface(
                                CD_FUNCTION,
                                "apply",
                                MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
                        .invokevirtual(
                                builderDesc,
                                "add",
                                MethodTypeDesc.of(builderDesc, ConstantDescs.CD_Object))
                        .pop()
                        .goto_(loop)
                        .labelBinding(done)
                        .aload(replacementSlot)
                        .invokevirtual(builderDesc, "build", MethodTypeDesc.of(immutableCollection))
                        .astore(replacementSlot);
            }

            private void emitImmutableMapValueTraversal(
                    CodeBuilder code,
                    int functionSlot,
                    int containerSlot,
                    int replacementSlot) {
                int iteratorSlot = 5;
                int entrySlot = 6;
                var loop = code.newLabel();
                var done = code.newLabel();
                code.aload(containerSlot)
                        .checkcast(CD_MAP)
                        .invokeinterface(CD_MAP, "size", MethodTypeDesc.of(ConstantDescs.CD_int))
                        .invokestatic(
                                CD_IMMUTABLE_MAP,
                                "builderWithExpectedSize",
                                MethodTypeDesc.of(CD_IMMUTABLE_MAP_BUILDER, ConstantDescs.CD_int))
                        .astore(replacementSlot)
                        .aload(containerSlot)
                        .checkcast(CD_MAP)
                        .invokeinterface(CD_MAP, "entrySet", MethodTypeDesc.of(classDesc(java.util.Set.class)))
                        .invokeinterface(CD_ITERABLE, "iterator", MethodTypeDesc.of(CD_ITERATOR))
                        .astore(iteratorSlot)
                        .labelBinding(loop)
                        .aload(iteratorSlot)
                        .invokeinterface(CD_ITERATOR, "hasNext", MethodTypeDesc.of(ConstantDescs.CD_boolean))
                        .ifeq(done)
                        .aload(iteratorSlot)
                        .invokeinterface(CD_ITERATOR, "next", MethodTypeDesc.of(ConstantDescs.CD_Object))
                        .checkcast(CD_MAP_ENTRY)
                        .astore(entrySlot)
                        .aload(replacementSlot)
                        .aload(entrySlot)
                        .invokeinterface(CD_MAP_ENTRY, "getKey", MethodTypeDesc.of(ConstantDescs.CD_Object))
                        .aload(functionSlot)
                        .aload(entrySlot)
                        .invokeinterface(CD_MAP_ENTRY, "getValue", MethodTypeDesc.of(ConstantDescs.CD_Object))
                        .invokeinterface(
                                CD_FUNCTION,
                                "apply",
                                MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
                        .invokevirtual(
                                CD_IMMUTABLE_MAP_BUILDER,
                                "put",
                                MethodTypeDesc.of(
                                        CD_IMMUTABLE_MAP_BUILDER,
                                        ConstantDescs.CD_Object,
                                        ConstantDescs.CD_Object))
                        .pop()
                        .goto_(loop)
                        .labelBinding(done)
                        .aload(replacementSlot)
                        .invokevirtual(CD_IMMUTABLE_MAP_BUILDER, "build", MethodTypeDesc.of(CD_IMMUTABLE_MAP))
                        .astore(replacementSlot);
            }

            private void emitMaybeTraversal(
                    CodeBuilder code,
                    int functionSlot,
                    int containerSlot,
                    int replacementSlot) {
                var empty = code.newLabel();
                var done = code.newLabel();
                code.aload(containerSlot)
                        .checkcast(CD_MAYBE)
                        .invokeinterface(CD_MAYBE, "isDefined", MethodTypeDesc.of(ConstantDescs.CD_boolean))
                        .ifeq(empty)
                        .aload(functionSlot)
                        .aload(containerSlot)
                        .checkcast(CD_MAYBE)
                        .invokeinterface(CD_MAYBE, "get", MethodTypeDesc.of(ConstantDescs.CD_Object))
                        .invokeinterface(
                                CD_FUNCTION,
                                "apply",
                                MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
                        .invokestatic(
                                CD_MAYBE,
                                "ofNullable",
                                MethodTypeDesc.of(CD_MAYBE, ConstantDescs.CD_Object),
                                true)
                        .astore(replacementSlot)
                        .goto_(done)
                        .labelBinding(empty)
                        .invokestatic(CD_MAYBE, "none", MethodTypeDesc.of(CD_MAYBE), true)
                        .astore(replacementSlot)
                        .labelBinding(done);
            }

            private void emitArrayTraversal(
                    CodeBuilder code,
                    int functionSlot,
                    int containerSlot,
                    int replacementSlot,
                    Class<?> componentType) {
                int indexSlot = 5;
                int lengthSlot = 6;
                var loop = code.newLabel();
                var done = code.newLabel();
                code.aload(containerSlot)
                        .invokestatic(
                                CD_ARRAY,
                                "getLength",
                                MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_Object))
                        .istore(lengthSlot);
                emitClassConstant(code, componentType);
                code.iload(lengthSlot)
                        .invokestatic(
                                CD_ARRAY,
                                "newInstance",
                                MethodTypeDesc.of(
                                        ConstantDescs.CD_Object,
                                        ConstantDescs.CD_Class,
                                        ConstantDescs.CD_int))
                        .astore(replacementSlot)
                        .iconst_0()
                        .istore(indexSlot)
                        .labelBinding(loop)
                        .iload(indexSlot)
                        .iload(lengthSlot)
                        .if_icmpge(done)
                        .aload(replacementSlot)
                        .iload(indexSlot)
                        .aload(functionSlot)
                        .aload(containerSlot)
                        .iload(indexSlot)
                        .invokestatic(
                                CD_ARRAY,
                                "get",
                                MethodTypeDesc.of(
                                        ConstantDescs.CD_Object,
                                        ConstantDescs.CD_Object,
                                        ConstantDescs.CD_int))
                        .invokeinterface(
                                CD_FUNCTION,
                                "apply",
                                MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
                        .invokestatic(
                                CD_ARRAY,
                                "set",
                                MethodTypeDesc.of(
                                        ConstantDescs.CD_void,
                                        ConstantDescs.CD_Object,
                                        ConstantDescs.CD_int,
                                        ConstantDescs.CD_Object))
                        .iinc(indexSlot, 1)
                        .goto_(loop)
                        .labelBinding(done);
            }

            private void emitSubtypeOpticApp(
                    CodeBuilder code,
                    ConstantTable constants,
                    PointFree<?> function,
                    SubtypeOpticElement subtype) {
                var noMatch = code.newLabel();
                ClassDesc subtypeDesc = classDesc(subtype.subtype());
                code.aload(1)
                        .instanceOf(subtypeDesc)
                        .ifeq(noMatch);
                emitConstant(code, constants, CD_GENERATED_FUNCTION, compileFunction(function));
                code.checkcast(CD_FUNCTION)
                        .aload(1)
                        .checkcast(subtypeDesc)
                        .invokeinterface(
                                CD_FUNCTION,
                                "apply",
                                MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object))
                        .areturn()
                        .labelBinding(noMatch)
                        .aload(1)
                        .areturn();
            }

            private void emitRecordComponentGet(CodeBuilder code, RecordLensOpticElement element) {
                ClassDesc recordDesc = classDesc(element.recordType());
                Class<?> componentType = element.componentType();
                code.checkcast(recordDesc)
                        .invokevirtual(
                                recordDesc,
                                element.componentName(),
                                MethodTypeDesc.of(classDesc(componentType)));
                emitBoxIfPrimitive(code, componentType);
            }

            private void emitRecordRebuild(
                    CodeBuilder code, RecordLensOpticElement element, int sourceSlot, int replacementSlot) {
                ClassDesc recordDesc = classDesc(element.recordType());
                String[] names = element.componentNames();
                Class<?>[] types = element.componentTypes();
                ClassDesc[] componentDescs = new ClassDesc[types.length];
                for (int i = 0; i < types.length; i++) {
                    componentDescs[i] = classDesc(types[i]);
                }
                code.new_(recordDesc).dup();
                for (int i = 0; i < types.length; i++) {
                    if (i == element.componentIndex()) {
                        code.aload(replacementSlot);
                        emitUnboxOrCast(code, types[i]);
                    } else {
                        code.aload(sourceSlot)
                                .checkcast(recordDesc)
                                .invokevirtual(recordDesc, names[i], MethodTypeDesc.of(componentDescs[i]));
                    }
                }
                code.invokespecial(recordDesc, "<init>", MethodTypeDesc.of(ConstantDescs.CD_void, componentDescs));
            }

            private boolean canDirectRecordAccess(RecordLensOpticElement element) {
                return Modifier.isPublic(element.recordType().getModifiers());
            }

            private List<RecordLensOpticElement> recordLensElements(List<PointFreeOpticElement> elements) {
                ArrayList<RecordLensOpticElement> result = new ArrayList<>(elements.size());
                for (PointFreeOpticElement element : elements) {
                    result.add((RecordLensOpticElement) element);
                }
                return result;
            }

            private void emitClassConstant(CodeBuilder code, Class<?> type) {
                if (type == null) {
                    code.aconst_null();
                } else {
                    code.ldc(classDesc(type));
                }
            }

            private void emitBoxIfPrimitive(CodeBuilder code, Class<?> type) {
                if (!type.isPrimitive()) {
                    return;
                }
                Primitive primitive = Primitive.of(type);
                code.invokestatic(
                        primitive.wrapper(),
                        "valueOf",
                        MethodTypeDesc.of(primitive.wrapper(), primitive.descriptor()));
            }

            private void emitUnboxOrCast(CodeBuilder code, Class<?> type) {
                if (!type.isPrimitive()) {
                    code.checkcast(classDesc(type));
                    return;
                }
                Primitive primitive = Primitive.of(type);
                code.checkcast(primitive.wrapper())
                        .invokevirtual(
                                primitive.wrapper(),
                                primitive.unboxMethod(),
                                MethodTypeDesc.of(primitive.descriptor()));
            }

            private boolean canEmitValue(PointFree<?> expression) {
                PointFree<?> unwrapped = unwrap(expression);
                return switch (unwrapped) {
                    case Value<?> ignored -> true;
                    case AppExpr<?, ?> app -> canEmitFunction(app.function()) && canEmitValue(app.argument());
                    default -> isFunctionExpression(unwrapped);
                };
            }

            private boolean canEmitFunction(PointFree<?> expression) {
                PointFree<?> unwrapped = unwrap(expression);
                return switch (unwrapped) {
                    case Id<?> ignored -> true;
                    case In<?> ignored -> true;
                    case Out<?> ignored -> true;
                    case Bang<?> ignored -> true;
                    case Fn<?, ?> ignored -> true;
                    case OpticApp<?, ?, ?, ?> opticApp -> isFunctionExpression(opticApp.function());
                    case FoldQuery<?, ?, ?, ?> ignored -> true;
                    case CataPlan<?> ignored -> true;
                    case Comp<?, ?> comp -> comp.functions().stream().allMatch(this::isFunctionExpression);
                    default -> false;
                };
            }

            private boolean isFunctionExpression(PointFree<?> expression) {
                return unwrap(expression).type() instanceof Func<?, ?>;
            }

            private PointFree<?> unwrap(PointFree<?> expression) {
                PointFree<?> current = Objects.requireNonNull(expression, "expression");
                while (true) {
                    if (current instanceof TypedPointFree<?> typed) {
                        current = typed.expression();
                        continue;
                    }
                    if (current instanceof UnsafeTypedPointFree<?> typed) {
                        current = typed.expression();
                        continue;
                    }
                    return current;
                }
            }

            private void emitConstant(CodeBuilder code, ConstantTable constants, ClassDesc constantsOwner, Object value) {
                code.aload(0)
                        .getfield(constantsOwner, "constants", CD_OBJECT_ARRAY)
                        .ldc(constants.add(value))
                        .aaload();
            }

            private Class<?> defineHidden(byte[] bytes) {
                try {
                    return lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE)
                            .lookupClass();
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unable to define generated point-free hidden class", e);
                }
            }

            private String executorClassName() {
                return generatedName("PointFreeBytecode$Executor$");
            }

            private String functionClassName() {
                return generatedName("PointFreeBytecode$Function$");
            }

            private String generatedName(String prefix) {
                return PointFreeBytecodeBackend.class.getPackageName() + "." + prefix + GENERATED_ID.incrementAndGet();
            }
        }

    private static final class ConstantTable {
        private final ArrayList<Object> constants = new ArrayList<>();

        private int add(Object value) {
            constants.add(value);
            return constants.size() - 1;
        }

        private Object[] toArray() {
            return constants.toArray();
        }
    }

    private static ClassDesc classDesc(Class<?> type) {
        return ClassDesc.ofDescriptor(type.descriptorString());
    }

    private record Primitive(
            Class<?> type, ClassDesc descriptor, ClassDesc wrapper, String unboxMethod) {
        private static Primitive of(Class<?> type) {
            if (type == boolean.class) {
                return new Primitive(type, ConstantDescs.CD_boolean, ConstantDescs.CD_Boolean, "booleanValue");
            }
            if (type == byte.class) {
                return new Primitive(type, ConstantDescs.CD_byte, ConstantDescs.CD_Byte, "byteValue");
            }
            if (type == char.class) {
                return new Primitive(type, ConstantDescs.CD_char, ConstantDescs.CD_Character, "charValue");
            }
            if (type == short.class) {
                return new Primitive(type, ConstantDescs.CD_short, ConstantDescs.CD_Short, "shortValue");
            }
            if (type == int.class) {
                return new Primitive(type, ConstantDescs.CD_int, ConstantDescs.CD_Integer, "intValue");
            }
            if (type == long.class) {
                return new Primitive(type, ConstantDescs.CD_long, ConstantDescs.CD_Long, "longValue");
            }
            if (type == float.class) {
                return new Primitive(type, ConstantDescs.CD_float, ConstantDescs.CD_Float, "floatValue");
            }
            if (type == double.class) {
                return new Primitive(type, ConstantDescs.CD_double, ConstantDescs.CD_Double, "doubleValue");
            }
            throw new IllegalArgumentException("Unsupported primitive type: " + type.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }
}
