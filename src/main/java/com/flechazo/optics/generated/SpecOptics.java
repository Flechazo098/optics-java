package com.flechazo.optics.generated;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.*;
import io.smallrye.classfile.ClassFile;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SpecOptics {
    private static final ConcurrentHashMap<SpecKey, GeneratedSpec<?>> GENERATED_SPECS = new ConcurrentHashMap<>();

    private SpecOptics() {
    }

    @SuppressWarnings("unchecked")
    public static <S> GeneratedSpec<S> generate(Class<? extends OpticsSpec<S>> specType, Class<S> sourceType) {
        return (GeneratedSpec<S>) GENERATED_SPECS.computeIfAbsent(
                new SpecKey(specType, sourceType), ignored -> create(specType, sourceType));
    }

    private static <S> GeneratedSpec<S> create(Class<? extends OpticsSpec<S>> specType, Class<S> sourceType) {
        RecordOptics.ensureGeneratedHost(specType);
        DerivedSpec derived = build(specType, sourceType);
        Object implementation = defineImplementation(specType, derived.methods(), derived.orderedOptics());
        return new GeneratedSpec<>(specType, sourceType, derived.optics(), implementation);
    }

    public static <S, I extends OpticsSpec<S>> I implementation(Class<I> specType, Class<S> sourceType) {
        return generate(specType, sourceType).implementation(specType);
    }

    private static <S> DerivedSpec build(Class<?> specType, Class<S> sourceType) {
        LinkedHashMap<String, Object> optics = new LinkedHashMap<>();
        ArrayList<Method> methods = new ArrayList<>();
        ArrayList<Object> orderedOptics = new ArrayList<>();
        Map<String, Lens<S, ?>> lenses = sourceType.isRecord() ? ClassFileOptics.lenses(sourceType) : Map.of();
        Map<Class<? extends S>, Prism<S, ? extends S>> prisms =
                sourceType.isSealed() ? ClassFileOptics.prisms(sourceType) : Map.of();
        Map<String, Class<?>> componentTypes = sourceType.isRecord() ? recordComponentTypes(sourceType) : Map.of();

        for (Method method : specType.getMethods()) {
            if (method.isDefault()
                    || method.getDeclaringClass() == Object.class
                    || method.getDeclaringClass() == OpticsSpec.class
                    || method.getParameterCount() != 0) {
                continue;
            }
            Type returnType = method.getGenericReturnType();
            if (!(returnType instanceof ParameterizedType parameterizedType)) {
                continue;
            }
            Class<?> raw = rawClass(parameterizedType.getRawType());
            Object optic = null;
            if (raw == Lens.class) {
                optic = lenses.get(method.getName());
            } else if (raw == Getter.class) {
                Lens<S, ?> lens = lenses.get(method.getName());
                optic = lens == null ? null : lens.asGetter();
            } else if (raw == Setter.class) {
                Lens<S, ?> lens = lenses.get(method.getName());
                optic = lens == null ? null : lens.asSetter();
            } else if (raw == Fold.class) {
                Lens<S, ?> lens = lenses.get(method.getName());
                optic = lens == null ? null : lens.asFold();
            } else if (raw == Traversal.class) {
                optic = ClassFileOptics.traversals(sourceType).get(method.getName());
            } else if (raw == Affine.class) {
                Lens<S, ?> lens = lenses.get(method.getName());
                Class<?> componentType = componentTypes.get(method.getName());
                optic = lens == null ? null : affineFromLens(lens, componentType);
            } else if (raw == Prism.class) {
                Class<?> subtype = secondTypeArgument(parameterizedType);
                if (subtype != null) {
                    optic = prisms.get(subtype);
                }
            }

            if (optic == null) {
                throw new IllegalArgumentException(
                        "Cannot derive optic for method " + specType.getName() + "." + method.getName());
            }
            optics.put(method.getName(), optic);
            methods.add(method);
            orderedOptics.add(optic);
        }
        return new DerivedSpec(List.copyOf(methods), Map.copyOf(optics), orderedOptics.toArray());
    }

    private static Map<String, Class<?>> recordComponentTypes(Class<?> recordType) {
        LinkedHashMap<String, Class<?>> result = new LinkedHashMap<>();
        for (RecordComponent component : recordType.getRecordComponents()) {
            result.put(component.getName(), component.getType());
        }
        return Map.copyOf(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <S> Affine<S, ?> affineFromLens(Lens<S, ?> lens, Class<?> componentType) {
        Lens rawLens = lens;
        if (Maybe.class.isAssignableFrom(componentType)) {
            return Affine.of(
                    source -> (Maybe<Object>) rawLens.get(source),
                    (source, value) -> (S) rawLens.set(Maybe.some(value), source),
                    source -> (S) rawLens.set(Maybe.none(), source));
        }
        return Affine.of(source -> Maybe.some(rawLens.get(source)), (source, value) -> (S) rawLens.set(value, source));
    }

    private static Object defineImplementation(Class<?> specType, List<Method> methods, Object[] optics) {
        try {
            byte[] bytes = generateSpecImplementationBytes(specType, methods);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(specType, MethodHandles.lookup());
            Class<?> implClass =
                    lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE)
                            .lookupClass();
            return implClass.getDeclaredConstructor(Object[].class).newInstance((Object) optics);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to define generated spec implementation for " + specType.getName(), e);
        }
    }

    private static byte[] generateSpecImplementationBytes(Class<?> specType, List<Method> methods) {
        ClassDesc generatedClass = ClassDesc.of(generatedSpecBinaryName(specType));
        ClassDesc specDesc = classDesc(specType);
        ClassDesc objectArrayDesc = ConstantDescs.CD_Object.arrayType();

        return ClassFile.of()
                .build(
                        generatedClass,
                        classBuilder -> {
                            classBuilder
                                    .withVersion(ClassFile.JAVA_21_VERSION, 0)
                                    .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SUPER)
                                    .withInterfaceSymbols(specDesc)
                                    .withField("optics", objectArrayDesc, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL)
                                    .withMethodBody(
                                            "<init>",
                                            MethodTypeDesc.of(ConstantDescs.CD_void, objectArrayDesc),
                                            ClassFile.ACC_PUBLIC,
                                            code ->
                                                    code.aload(0)
                                                            .invokespecial(
                                                                    ConstantDescs.CD_Object,
                                                                    "<init>",
                                                                    MethodTypeDesc.of(ConstantDescs.CD_void))
                                                            .aload(0)
                                                            .aload(1)
                                                            .putfield(generatedClass, "optics", objectArrayDesc)
                                                            .return_());
                            for (int i = 0; i < methods.size(); i++) {
                                Method method = methods.get(i);
                                ClassDesc returnDesc = classDesc(method.getReturnType());
                                int slot = i;
                                classBuilder.withMethodBody(
                                        method.getName(),
                                        MethodTypeDesc.of(returnDesc),
                                        ClassFile.ACC_PUBLIC,
                                        code ->
                                                code.aload(0)
                                                        .getfield(generatedClass, "optics", objectArrayDesc)
                                                        .ldc(slot)
                                                        .aaload()
                                                        .checkcast(returnDesc)
                                                        .areturn());
                            }
                        });
    }

    private static Class<?> secondTypeArgument(ParameterizedType type) {
        Type[] args = type.getActualTypeArguments();
        return args.length >= 2 ? rawClass(args[1]) : null;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return rawClass(parameterizedType.getRawType());
        }
        return null;
    }

    private static String generatedSpecBinaryName(Class<?> specType) {
        String packageName = specType.getPackageName();
        String localName =
                packageName.isEmpty()
                        ? specType.getName()
                        : specType.getName().substring(packageName.length() + 1);
        String sanitized = localName.replace('.', '$').replace('[', '$').replace(';', '$');
        return packageName.isEmpty()
                ? "SpecOptics$Impl$" + sanitized
                : packageName + ".SpecOptics$Impl$" + sanitized;
    }

    private static ClassDesc classDesc(Class<?> type) {
        return ClassDesc.ofDescriptor(type.descriptorString());
    }

    private record DerivedSpec(List<Method> methods, Map<String, Object> optics, Object[] orderedOptics) {
    }

    private record SpecKey(Class<?> specType, Class<?> sourceType) {
    }

    public record GeneratedSpec<S>(
            Class<? extends OpticsSpec<S>> specType,
            Class<S> sourceType,
            Map<String, Object> optics,
            Object implementation) {
        public <I extends OpticsSpec<S>> I implementation(Class<I> expectedType) {
            return expectedType.cast(implementation);
        }

        public <O> O optic(String methodName, Class<O> expectedType) {
            return expectedType.cast(opticValue(methodName));
        }

        private Object opticValue(String methodName) {
            Object value = optics.get(methodName);
            if (value == null) {
                throw new IllegalArgumentException("No generated optic for method " + methodName);
            }
            return value;
        }

        @SuppressWarnings("unchecked")
        public <A> Lens<S, A> lens(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof Lens<?, ?> lens) {
                return (Lens<S, A>) lens;
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " is not a Lens");
        }

        @SuppressWarnings("unchecked")
        public <A> Getter<S, A> getter(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof Getter<?, ?> getter) {
                return (Getter<S, A>) getter;
            }
            if (value instanceof Lens<?, ?> lens) {
                return ((Lens<S, A>) lens).asGetter();
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a Getter");
        }

        @SuppressWarnings("unchecked")
        public <A> Setter<S, A> setter(String methodName) {
            Object value = opticValue(methodName);
            return switch (value) {
                case Setter<?, ?> setter -> (Setter<S, A>) setter;
                case Lens<?, ?> lens -> ((Lens<S, A>) lens).asSetter();
                case Traversal<?, ?> traversal -> ((Traversal<S, A>) traversal).asSetter();
                case Prism<?, ?> prism -> ((Prism<S, A>) prism).asSetter();
                case Affine<?, ?> affine -> ((Affine<S, A>) affine).asSetter();
                default ->
                        throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a Setter");
            };
        }

        @SuppressWarnings("unchecked")
        public <A> Fold<S, A> fold(String methodName) {
            Object value = opticValue(methodName);
            return switch (value) {
                case Fold<?, ?> fold -> (Fold<S, A>) fold;
                case Lens<?, ?> lens -> ((Lens<S, A>) lens).asFold();
                case Traversal<?, ?> traversal -> ((Traversal<S, A>) traversal).asFold();
                case Prism<?, ?> prism -> ((Prism<S, A>) prism).asFold();
                case Affine<?, ?> affine -> ((Affine<S, A>) affine).asFold();
                default ->
                        throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a Fold");
            };
        }

        @SuppressWarnings("unchecked")
        public <A> Traversal<S, A> traversal(String methodName) {
            Object value = opticValue(methodName);
            return switch (value) {
                case Traversal<?, ?> traversal -> (Traversal<S, A>) traversal;
                case Lens<?, ?> lens -> ((Lens<S, A>) lens).asTraversal();
                case Prism<?, ?> prism -> ((Prism<S, A>) prism).asTraversal();
                case Affine<?, ?> affine -> ((Affine<S, A>) affine).asTraversal();
                default ->
                        throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a Traversal");
            };
        }

        @SuppressWarnings("unchecked")
        public <A> Prism<S, A> prism(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof Prism<?, ?> prism) {
                return (Prism<S, A>) prism;
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " is not a Prism");
        }

        @SuppressWarnings("unchecked")
        public <A> Affine<S, A> affine(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof Affine<?, ?> affine) {
                return (Affine<S, A>) affine;
            }
            if (value instanceof Lens<?, ?> lens) {
                return Affine.of(
                        source -> Maybe.some(((Lens<S, A>) lens).get(source)),
                        (source, next) -> ((Lens<S, A>) lens).set(next, source));
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as an Affine");
        }
    }
}
