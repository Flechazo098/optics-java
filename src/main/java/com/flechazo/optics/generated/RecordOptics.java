package com.flechazo.optics.generated;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Lens;
import com.flechazo.optics.Prism;
import com.flechazo.optics.Traversal;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.CodeBuilder;

import java.io.Serializable;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RecordOptics {
    private static final ConcurrentHashMap<Class<?>, Class<?>> GENERATED_HOSTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<LensKey, Lens<?, ?>> GENERATED_LENSES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<PrismKey, Prism<?, ?>> GENERATED_PRISMS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<TraversalKey, Traversal<?, ?>> GENERATED_TRAVERSALS =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Map<String, Lens<?, ?>>> GENERATED_LENS_MAPS =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Map<String, Traversal<?, ?>>> GENERATED_TRAVERSAL_MAPS =
            new ConcurrentHashMap<>();

    private RecordOptics() {
    }

    public static <S, A> Lens<S, A> recordLens(Class<S> recordType, String componentName) {
        Objects.requireNonNull(recordType, "recordType");
        Objects.requireNonNull(componentName, "componentName");
        Lens<S, ?> lens = recordLenses(recordType).get(componentName);
        if (lens == null) {
            throw new IllegalArgumentException(
                    "Record component '" + componentName + "' not found on " + recordType.getName());
        }
        @SuppressWarnings("unchecked")
        Lens<S, A> typed = (Lens<S, A>) lens;
        return typed;
    }

    public static <S, A> Lens<S, A> recordLens(Class<S> recordType, LensGetter<S, A> getter) {
        Objects.requireNonNull(getter, "getter");
        return recordLens(recordType, componentName(recordType, getter));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S> Map<String, Lens<S, ?>> recordLenses(Class<S> recordType) {
        Objects.requireNonNull(recordType, "recordType");
        ensureGeneratedHost(recordType);
        return (Map) GENERATED_LENS_MAPS.computeIfAbsent(recordType, RecordOptics::createRecordLensMap);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, Lens<?, ?>> createRecordLensMap(Class<?> recordType) {
        RecordComponent[] components = recordComponents(recordType);
        LinkedHashMap<String, Lens<?, ?>> lenses = new LinkedHashMap<>();
        for (int i = 0; i < components.length; i++) {
            lenses.put(components[i].getName(), componentLens((Class) recordType, components, i));
        }
        return Map.copyOf(lenses);
    }

    public static <S, A> Traversal<S, A> recordTraversal(Class<S> recordType, String componentName) {
        Objects.requireNonNull(recordType, "recordType");
        Objects.requireNonNull(componentName, "componentName");
        Traversal<S, ?> traversal = recordTraversals(recordType).get(componentName);
        if (traversal == null && recordLenses(recordType).containsKey(componentName)) {
            throw new IllegalArgumentException(
                    "Record component '" + componentName + "' is not a supported traversal container");
        }
        if (traversal == null) {
            throw new IllegalArgumentException(
                    "Record component '" + componentName + "' not found on " + recordType.getName());
        }
        @SuppressWarnings("unchecked")
        Traversal<S, A> typed = (Traversal<S, A>) traversal;
        return typed;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S> Map<String, Traversal<S, ?>> recordTraversals(Class<S> recordType) {
        Objects.requireNonNull(recordType, "recordType");
        ensureGeneratedHost(recordType);
        return (Map) GENERATED_TRAVERSAL_MAPS.computeIfAbsent(recordType, RecordOptics::createRecordTraversalMap);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, Traversal<?, ?>> createRecordTraversalMap(Class<?> recordType) {
        RecordComponent[] components = recordComponents(recordType);
        LinkedHashMap<String, Traversal<?, ?>> traversals = new LinkedHashMap<>();
        for (int i = 0; i < components.length; i++) {
            int kind = containerApp(components[i]);
            if (kind != 0) {
                traversals.put(components[i].getName(), componentTraversal((Class) recordType, components, i, kind));
            }
        }
        return Map.copyOf(traversals);
    }

    public static <S, A extends S> Prism<S, A> subtypePrism(Class<S> baseType, Class<A> subtype) {
        Objects.requireNonNull(baseType, "baseType");
        Objects.requireNonNull(subtype, "subtype");
        ensureGeneratedHost(baseType);
        if (!baseType.isAssignableFrom(subtype)) {
            throw new IllegalArgumentException(subtype.getName() + " is not a subtype of " + baseType.getName());
        }
        return generatedSubtypePrism(baseType, subtype);
    }

    public static <S> Map<Class<? extends S>, Prism<S, ? extends S>> sealedSubtypePrisms(
            Class<S> sealedType) {
        Objects.requireNonNull(sealedType, "sealedType");
        ensureGeneratedHost(sealedType);
        if (!sealedType.isSealed()) {
            throw new IllegalArgumentException(sealedType.getName() + " is not sealed");
        }
        LinkedHashMap<Class<? extends S>, Prism<S, ? extends S>> prisms = new LinkedHashMap<>();
        for (Class<?> permitted : sealedType.getPermittedSubclasses()) {
            @SuppressWarnings("unchecked")
            Class<? extends S> subtype = (Class<? extends S>) permitted;
            prisms.put(subtype, subtypePrism(sealedType, subtype));
        }
        return Map.copyOf(prisms);
    }

    public static Class<?> ensureGeneratedHost(Class<?> recordType) {
        return GENERATED_HOSTS.computeIfAbsent(recordType, RecordOptics::defineGeneratedHost);
    }

    public static byte[] generateLensHostBytes(Class<?> recordType) {
        Objects.requireNonNull(recordType, "recordType");

        ClassDesc generatedClass = ClassDesc.of(generatedBinaryName(recordType));
        return ClassFile.of()
                .build(
                        generatedClass,
                        classBuilder ->
                                classBuilder
                                        .withVersion(ClassFile.JAVA_21_VERSION, 0)
                                        .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SUPER)
                                        .withMethodBody(
                                                "<init>",
                                                MethodTypeDesc.of(ConstantDescs.CD_void),
                                                ClassFile.ACC_PUBLIC,
                                                code ->
                                                        code.aload(0)
                                                                .invokespecial(
                                                                        ConstantDescs.CD_Object,
                                                                        "<init>",
                                                                        MethodTypeDesc.of(ConstantDescs.CD_void))
                                                                .return_())
                                        .withMethodBody(
                                                "generatedFor",
                                                MethodTypeDesc.of(ConstantDescs.CD_String),
                                                ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
                                                code -> code.ldc(recordType.getName()).areturn()));
    }

    private static Class<?> defineGeneratedHost(Class<?> recordType) {
        try {
            byte[] bytes = generateLensHostBytes(recordType);
            return MethodHandles.lookup().defineHiddenClass(bytes, true).lookupClass();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to define generated optics host for " + recordType.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <S, A> Lens<S, A> componentLens(
            Class<S> recordType, RecordComponent[] components, int componentIndex) {
        LensKey key = new LensKey(recordType, components[componentIndex].getName());
        return (Lens<S, A>) GENERATED_LENSES.computeIfAbsent(
                key, ignored -> defineGeneratedLens(recordType, components, componentIndex));
    }

    private static <S> Lens<?, ?> defineGeneratedLens(
            Class<S> recordType, RecordComponent[] components, int componentIndex) {
        try {
            byte[] bytes = generateComponentLensBytes(recordType, components, componentIndex);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(recordType, MethodHandles.lookup());
            Class<?> lensClass =
                    lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE)
                            .lookupClass();
            return (Lens<?, ?>) lensClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Unable to define generated lens for "
                            + recordType.getName()
                            + "."
                            + components[componentIndex].getName(),
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <S, A> Traversal<S, A> componentTraversal(
            Class<S> recordType, RecordComponent[] components, int componentIndex, int kind) {
        TraversalKey key = new TraversalKey(recordType, components[componentIndex].getName());
        return (Traversal<S, A>) GENERATED_TRAVERSALS.computeIfAbsent(
                key, ignored -> defineGeneratedTraversal(recordType, components, componentIndex, kind));
    }

    private static <S> Traversal<?, ?> defineGeneratedTraversal(
            Class<S> recordType, RecordComponent[] components, int componentIndex, int kind) {
        try {
            byte[] bytes = generateComponentTraversalBytes(recordType, components, componentIndex, kind);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(recordType, MethodHandles.lookup());
            Class<?> traversalClass =
                    lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE)
                            .lookupClass();
            return (Traversal<?, ?>) traversalClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Unable to define generated traversal for "
                            + recordType.getName()
                            + "."
                            + components[componentIndex].getName(),
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <S, A extends S> Prism<S, A> generatedSubtypePrism(Class<S> baseType, Class<A> subtype) {
        PrismKey key = new PrismKey(baseType, subtype);
        return (Prism<S, A>) GENERATED_PRISMS.computeIfAbsent(key, ignored -> defineGeneratedPrism(baseType, subtype));
    }

    private static <S, A extends S> Prism<?, ?> defineGeneratedPrism(Class<S> baseType, Class<A> subtype) {
        try {
            byte[] bytes = generateSubtypePrismBytes(baseType, subtype);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(baseType, MethodHandles.lookup());
            Class<?> prismClass =
                    lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE)
                            .lookupClass();
            return (Prism<?, ?>) prismClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Unable to define generated prism for " + baseType.getName() + " -> " + subtype.getName(), e);
        }
    }

    private static byte[] generateSubtypePrismBytes(Class<?> baseType, Class<?> subtype) {
        ClassDesc generatedClass = ClassDesc.of(generatedPrismBinaryName(baseType, subtype));
        ClassDesc superDesc = ClassDesc.of(GeneratedPrism.class.getName());
        ClassDesc maybeDesc = ClassDesc.of(Maybe.class.getName());
        ClassDesc subtypeDesc = classDesc(subtype);

        return ClassFile.of()
                .build(
                        generatedClass,
                        classBuilder ->
                                classBuilder
                                        .withVersion(ClassFile.JAVA_21_VERSION, 0)
                                        .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SUPER)
                                        .withSuperclass(superDesc)
                                        .withMethodBody(
                                                "<init>",
                                                MethodTypeDesc.of(ConstantDescs.CD_void),
                                                ClassFile.ACC_PUBLIC,
                                                code ->
                                                        code.aload(0)
                                                                .invokespecial(
                                                                        superDesc,
                                                                        "<init>",
                                                                        MethodTypeDesc.of(ConstantDescs.CD_void))
                                                                .return_())
                                        .withMethodBody(
                                                "getMaybe",
                                                MethodTypeDesc.of(maybeDesc, ConstantDescs.CD_Object),
                                                ClassFile.ACC_PUBLIC,
                                                code -> emitSubtypeGetMaybe(code, subtypeDesc, maybeDesc))
                                        .withMethodBody(
                                                "build",
                                                MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object),
                                                ClassFile.ACC_PUBLIC,
                                                code ->
                                                        code.aload(1)
                                                                .checkcast(subtypeDesc)
                                                                .areturn()));
    }

    private static void emitSubtypeGetMaybe(CodeBuilder code, ClassDesc subtypeDesc, ClassDesc maybeDesc) {
        var noMatch = code.newLabel();
        code.aload(1)
                .instanceOf(subtypeDesc)
                .ifeq(noMatch)
                .aload(1)
                .checkcast(subtypeDesc)
                .invokestatic(
                        maybeDesc,
                        "some",
                        MethodTypeDesc.of(maybeDesc, ConstantDescs.CD_Object),
                        true)
                .areturn()
                .labelBinding(noMatch)
                .invokestatic(maybeDesc, "none", MethodTypeDesc.of(maybeDesc), true)
                .areturn();
    }

    private static byte[] generateComponentLensBytes(
            Class<?> recordType, RecordComponent[] components, int componentIndex) {
        ClassDesc generatedClass =
                ClassDesc.of(generatedLensBinaryName(recordType, components[componentIndex].getName()));
        ClassDesc recordDesc = classDesc(recordType);
        ClassDesc superDesc = ClassDesc.of(GeneratedLens.class.getName());
        ClassDesc[] componentDescs =
                Arrays.stream(components)
                        .map(component -> classDesc(component.getType()))
                        .toArray(ClassDesc[]::new);

        return ClassFile.of()
                .build(
                        generatedClass,
                        classBuilder ->
                                classBuilder
                                        .withVersion(ClassFile.JAVA_21_VERSION, 0)
                                        .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SUPER)
                                        .withSuperclass(superDesc)
                                        .withMethodBody(
                                                "<init>",
                                                MethodTypeDesc.of(ConstantDescs.CD_void),
                                                ClassFile.ACC_PUBLIC,
                                                code ->
                                                        code.aload(0)
                                                                .invokespecial(
                                                                        superDesc,
                                                                        "<init>",
                                                                        MethodTypeDesc.of(ConstantDescs.CD_void))
                                                                .return_())
                                        .withMethodBody(
                                                "get",
                                                MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object),
                                                ClassFile.ACC_PUBLIC,
                                                code -> emitGet(code, recordDesc, components[componentIndex]))
                                        .withMethodBody(
                                                "set",
                                                MethodTypeDesc.of(
                                                        ConstantDescs.CD_Object,
                                                        ConstantDescs.CD_Object,
                                                        ConstantDescs.CD_Object),
                                                ClassFile.ACC_PUBLIC,
                                                code -> emitSet(code, recordDesc, components, componentIndex, componentDescs)));
    }

    private static byte[] generateComponentTraversalBytes(
            Class<?> recordType, RecordComponent[] components, int componentIndex, int kind) {
        RecordComponent component = components[componentIndex];
        ClassDesc generatedClass = ClassDesc.of(generatedTraversalBinaryName(recordType, component.getName()));
        ClassDesc recordDesc = classDesc(recordType);
        ClassDesc superDesc = ClassDesc.of(GeneratedTraversal.class.getName());
        ClassDesc[] componentDescs =
                Arrays.stream(components)
                        .map(current -> classDesc(current.getType()))
                        .toArray(ClassDesc[]::new);

        return ClassFile.of()
                .build(
                        generatedClass,
                        classBuilder ->
                                classBuilder
                                        .withVersion(ClassFile.JAVA_21_VERSION, 0)
                                        .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SUPER)
                                        .withSuperclass(superDesc)
                                        .withMethodBody(
                                                "<init>",
                                                MethodTypeDesc.of(ConstantDescs.CD_void),
                                                ClassFile.ACC_PUBLIC,
                                                code -> {
                                                    code.aload(0).ldc(kind);
                                                    if (kind == GeneratedTraversal.ARRAY) {
                                                        code.ldc(classDesc(component.getType().getComponentType()));
                                                    } else {
                                                        code.aconst_null();
                                                    }
                                                    code.invokespecial(
                                                                    superDesc,
                                                                    "<init>",
                                                                    MethodTypeDesc.of(
                                                                            ConstantDescs.CD_void,
                                                                            ConstantDescs.CD_int,
                                                                            ConstantDescs.CD_Class))
                                                            .return_();
                                                })
                                        .withMethodBody(
                                                "getContainer",
                                                MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object),
                                                ClassFile.ACC_PROTECTED,
                                                code -> emitGet(code, recordDesc, component))
                                        .withMethodBody(
                                                "setContainer",
                                                MethodTypeDesc.of(
                                                        ConstantDescs.CD_Object,
                                                        ConstantDescs.CD_Object,
                                                        ConstantDescs.CD_Object),
                                                ClassFile.ACC_PROTECTED,
                                                code ->
                                                        emitSetContainer(
                                                                code,
                                                                recordDesc,
                                                                components,
                                                                componentIndex,
                                                                componentDescs)));
    }

    private static void emitGet(CodeBuilder code, ClassDesc recordDesc, RecordComponent component) {
        Class<?> type = component.getType();
        code.aload(1)
                .checkcast(recordDesc)
                .invokevirtual(recordDesc, component.getName(), MethodTypeDesc.of(classDesc(type)));
        boxIfPrimitive(code, type);
        code.areturn();
    }

    private static void emitSet(
            CodeBuilder code,
            ClassDesc recordDesc,
            RecordComponent[] components,
            int componentIndex,
            ClassDesc[] componentDescs) {
        code.new_(recordDesc).dup();
        for (int i = 0; i < components.length; i++) {
            Class<?> type = components[i].getType();
            if (i == componentIndex) {
                code.aload(1);
                unboxOrCast(code, type);
            } else {
                code.aload(2)
                        .checkcast(recordDesc)
                        .invokevirtual(recordDesc, components[i].getName(), MethodTypeDesc.of(componentDescs[i]));
            }
        }
        code.invokespecial(recordDesc, "<init>", MethodTypeDesc.of(ConstantDescs.CD_void, componentDescs))
                .areturn();
    }

    private static void emitSetContainer(
            CodeBuilder code,
            ClassDesc recordDesc,
            RecordComponent[] components,
            int componentIndex,
            ClassDesc[] componentDescs) {
        code.new_(recordDesc).dup();
        for (int i = 0; i < components.length; i++) {
            Class<?> type = components[i].getType();
            if (i == componentIndex) {
                code.aload(1);
                unboxOrCast(code, type);
            } else {
                code.aload(2)
                        .checkcast(recordDesc)
                        .invokevirtual(recordDesc, components[i].getName(), MethodTypeDesc.of(componentDescs[i]));
            }
        }
        code.invokespecial(recordDesc, "<init>", MethodTypeDesc.of(ConstantDescs.CD_void, componentDescs))
                .areturn();
    }

    private static int containerApp(RecordComponent component) {
        Class<?> rawType = component.getType();
        if (List.class.isAssignableFrom(rawType)) {
            return GeneratedTraversal.LIST;
        }
        if (Set.class.isAssignableFrom(rawType)) {
            return GeneratedTraversal.SET;
        }
        if (Map.class.isAssignableFrom(rawType)) {
            return GeneratedTraversal.MAP_VALUES;
        }
        if (Maybe.class.isAssignableFrom(rawType)) {
            return GeneratedTraversal.MAYBE;
        }
        if (rawType.isArray()) {
            return GeneratedTraversal.ARRAY;
        }
        return 0;
    }

    private static void boxIfPrimitive(CodeBuilder code, Class<?> type) {
        if (!type.isPrimitive()) {
            return;
        }
        Primitive primitive = Primitive.of(type);
        code.invokestatic(
                primitive.wrapper(),
                "valueOf",
                MethodTypeDesc.of(primitive.wrapper(), primitive.descriptor()));
    }

    private static void unboxOrCast(CodeBuilder code, Class<?> type) {
        if (!type.isPrimitive()) {
            code.checkcast(classDesc(type));
            return;
        }
        Primitive primitive = Primitive.of(type);
        code.checkcast(primitive.wrapper())
                .invokevirtual(primitive.wrapper(), primitive.unboxMethod(), MethodTypeDesc.of(primitive.descriptor()));
    }

    private static RecordComponent[] recordComponents(Class<?> recordType) {
        if (!recordType.isRecord()) {
            throw new IllegalArgumentException(recordType.getName() + " is not a record");
        }
        return recordType.getRecordComponents();
    }

    private static String componentName(Class<?> recordType, LensGetter<?, ?> getter) {
        Objects.requireNonNull(recordType, "recordType");
        String implMethodName = referencedRecordMethod(recordType, getter);
        for (RecordComponent component : recordComponents(recordType)) {
            if (component.getName().equals(implMethodName)) {
                return implMethodName;
            }
        }
        throw new IllegalArgumentException(
                "Lens getter must reference a record component accessor on "
                        + recordType.getName()
                        + ", but referenced method "
                        + implMethodName);
    }

    private static String referencedRecordMethod(Class<?> recordType, LensGetter<?, ?> getter) {
        SerializedLambda lambda = serializedLambda(getter);
        String implClass = lambda.getImplClass().replace('/', '.');
        String implMethodName = lambda.getImplMethodName();
        if (!recordType.getName().equals(implClass)) {
            throw new IllegalArgumentException(
                    "Lens getter must reference a component accessor on "
                            + recordType.getName()
                            + ", but referenced "
                            + implClass
                            + "."
                            + implMethodName);
        }
        return implMethodName;
    }

    private static SerializedLambda serializedLambda(Serializable getter) {
        try {
            Method writeReplace = getter.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            Object replaced = writeReplace.invoke(getter);
            if (replaced instanceof SerializedLambda lambda) {
                return lambda;
            }
            throw new IllegalArgumentException("Lens getter did not serialize to SerializedLambda");
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "Lens getter must be a serializable method reference to a record component accessor", e);
        }
    }

    private static String generatedBinaryName(Class<?> recordType) {
        String sanitized = recordType.getName().replace('.', '$').replace('[', '$').replace(';', '$');
        return RecordOptics.class.getPackageName() + ".RecordOptics$Generated$" + sanitized;
    }

    private static String generatedLensBinaryName(Class<?> recordType, String componentName) {
        String packageName = recordType.getPackageName();
        String localName =
                packageName.isEmpty()
                        ? recordType.getName()
                        : recordType.getName().substring(packageName.length() + 1);
        String sanitized = localName.replace('.', '$').replace('[', '$').replace(';', '$') + "$" + componentName;
        return packageName.isEmpty()
                ? "RecordOptics$Lens$" + sanitized
                : packageName + ".RecordOptics$Lens$" + sanitized;
    }

    private static String generatedPrismBinaryName(Class<?> baseType, Class<?> subtype) {
        String packageName = baseType.getPackageName();
        String baseLocal =
                packageName.isEmpty() ? baseType.getName() : baseType.getName().substring(packageName.length() + 1);
        String subtypeLocal =
                subtype.getPackageName().equals(packageName)
                        ? subtype.getName().substring(packageName.length() + 1)
                        : subtype.getName();
        String sanitized =
                (baseLocal + "$" + subtypeLocal).replace('.', '$').replace('[', '$').replace(';', '$');
        return packageName.isEmpty()
                ? "RecordOptics$Prism$" + sanitized
                : packageName + ".RecordOptics$Prism$" + sanitized;
    }

    private static String generatedTraversalBinaryName(Class<?> recordType, String componentName) {
        String packageName = recordType.getPackageName();
        String localName =
                packageName.isEmpty()
                        ? recordType.getName()
                        : recordType.getName().substring(packageName.length() + 1);
        String sanitized = localName.replace('.', '$').replace('[', '$').replace(';', '$') + "$" + componentName;
        return packageName.isEmpty()
                ? "RecordOptics$Traversal$" + sanitized
                : packageName + ".RecordOptics$Traversal$" + sanitized;
    }

    private static ClassDesc classDesc(Class<?> type) {
        return ClassDesc.ofDescriptor(type.descriptorString());
    }

    private record LensKey(Class<?> recordType, String componentName) {
    }

    private record PrismKey(Class<?> baseType, Class<?> subtype) {
    }

    private record TraversalKey(Class<?> recordType, String componentName) {
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
}
