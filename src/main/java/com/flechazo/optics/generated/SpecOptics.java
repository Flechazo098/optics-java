package com.flechazo.optics.generated;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.OpticLowering;
import com.flechazo.hkt.functions.PointFree;
import com.flechazo.hkt.functions.PointFreeBytecodeBackend;
import com.flechazo.hkt.functions.PointFreeExecutor;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOptimizer;
import com.flechazo.optics.*;
import com.flechazo.optics.util.Optionals;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.classfile.ClassFile;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class SpecOptics {
    private static final ConcurrentHashMap<SpecKey, GeneratedSpec<?>> GENERATED_SPECS = new ConcurrentHashMap<>();

    private SpecOptics() {
    }

    @SuppressWarnings("unchecked")
    public static <S> GeneratedSpec<S> generate(Class<? extends OpticsSpec<S>> specType, Class<S> sourceType) {
        return generate(specType, sourceType, MethodHandles.lookup());
    }

    @SuppressWarnings("unchecked")
    public static <S> GeneratedSpec<S> generate(
            Class<? extends OpticsSpec<S>> specType, Class<S> sourceType, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(specType, "specType");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(lookup, "lookup");
        return (GeneratedSpec<S>) GENERATED_SPECS.computeIfAbsent(
                new SpecKey(specType, sourceType), ignored -> create(specType, sourceType, lookup));
    }

    private static <S> GeneratedSpec<S> create(
            Class<? extends OpticsSpec<S>> specType, Class<S> sourceType, MethodHandles.Lookup lookup) {
        RecordOptics.ensureGeneratedHost(specType);
        DerivedSpec derived = build(specType, sourceType);
        Object implementation = defineImplementation(specType, derived.methods(), derived.orderedOptics(), lookup);
        return new GeneratedSpec<>(specType, sourceType, derived.optics(), implementation);
    }

    public static <S, I extends OpticsSpec<S>> I implementation(Class<I> specType, Class<S> sourceType) {
        return generate(specType, sourceType).implementation(specType);
    }

    public static <S, I extends OpticsSpec<S>> I implementation(
            Class<I> specType, Class<S> sourceType, MethodHandles.Lookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return generate(specType, sourceType, lookup).implementation(specType);
    }

    private static <S> DerivedSpec build(Class<?> specType, Class<S> sourceType) {
        LinkedHashMap<String, Object> optics = new LinkedHashMap<>();
        ArrayList<Method> methods = new ArrayList<>();
        ArrayList<Object> orderedOptics = new ArrayList<>();
        Map<String, Lens<S, S, ?, ?>> lenses = sourceType.isRecord() ? ClassFileOptics.lenses(sourceType) : Map.of();
        Map<Class<? extends S>, Prism<S, S, ? extends S, ? extends S>> prisms =
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
                Lens<S, S, ?, ?> lens = lenses.get(method.getName());
                optic = lens == null ? null : lens.asGetter();
            } else if (raw == Setter.class) {
                Lens<S, S, ?, ?> lens = lenses.get(method.getName());
                optic = lens == null ? null : lens.asSetter();
            } else if (raw == Fold.class) {
                Lens<S, S, ?, ?> lens = lenses.get(method.getName());
                optic = lens == null ? null : lens.asFold();
            } else if (raw == Traversal.class) {
                optic = ClassFileOptics.traversals(sourceType).get(method.getName());
            } else if (raw == Affine.class) {
                Lens<S, S, ?, ?> lens = lenses.get(method.getName());
                Class<?> componentType = componentTypes.get(method.getName());
                optic = lens == null ? null : affineFromLens(lens, sourceType, method.getName(), componentType);
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
        return new DerivedSpec(ImmutableList.copyOf(methods), ImmutableMap.copyOf(optics), orderedOptics.toArray());
    }

    private static Map<String, Class<?>> recordComponentTypes(Class<?> recordType) {
        LinkedHashMap<String, Class<?>> result = new LinkedHashMap<>();
        for (RecordComponent component : recordType.getRecordComponents()) {
            result.put(component.getName(), component.getType());
        }
        return ImmutableMap.copyOf(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <S> Affine<S, S, ?, ?> affineFromLens(
            Lens<S, S, ?, ?> lens, Class<S> sourceType, String componentName, Class<?> componentType) {
        Lens rawLens = lens;
        Maybe<PointFreeOptic<S, S, Object, Object>> typed = castTypedOptic(lens.typedOptic());
        if (Maybe.class.isAssignableFrom(componentType)) {
            Traversal<S, S, ?, ?> traversal = ClassFileOptics.traversals(sourceType).get(componentName);
            typed = traversal == null ? Maybe.none() : castTypedOptic(traversal.typedOptic());
            return Affine.<S, S, Object, Object>of(
                    source -> {
                        Maybe<Object> value = (Maybe<Object>) rawLens.get(source);
                        return value.isDefined()
                                ? Either.right(value.get())
                                : Either.left((S) rawLens.set(Maybe.none(), source));
                    },
                    (source, value) -> (S) rawLens.set(Maybe.some(value), source))
                    .withTypedOptic(typed);
        }
        if (Optional.class.isAssignableFrom(componentType)) {
            Traversal<S, S, ?, ?> traversal = ClassFileOptics.traversals(sourceType).get(componentName);
            typed = traversal == null ? Maybe.none() : castTypedOptic(traversal.typedOptic());
            return Affine.<S, S, Object, Object>of(
                    source -> {
                        Maybe<Object> value = Optionals.toMaybe((Optional<?>) rawLens.get(source));
                        return value.isDefined()
                                ? Either.right(value.get())
                                : Either.left((S) rawLens.set(Optional.empty(), source));
                    },
                    (source, value) -> (S) rawLens.set(Optional.of(value), source))
                    .withTypedOptic(typed);
        }
        return Affine.<S, S, Object, Object>of(
                        source -> Either.right(rawLens.get(source)),
                        (source, value) -> (S) rawLens.set(value, source))
                .withTypedOptic(typed);
    }

    private static Object defineImplementation(
            Class<?> specType, List<Method> methods, Object[] optics, MethodHandles.Lookup callerLookup) {
        try {
            byte[] bytes = generateSpecImplementationBytes(specType, methods);
            MethodHandles.Lookup lookup = privateLookup(specType, callerLookup);
            Class<?> implClass =
                    lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE)
                            .lookupClass();
            return implClass.getDeclaredConstructor(Object[].class).newInstance((Object) optics);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to define generated spec implementation for " + specType.getName(), e);
        }
    }

    private static MethodHandles.Lookup privateLookup(Class<?> targetType, MethodHandles.Lookup callerLookup)
            throws IllegalAccessException {
        Objects.requireNonNull(callerLookup, "lookup");
        return MethodHandles.privateLookupIn(targetType, callerLookup);
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
                                    .withVersion(ClassFile.JAVA_25_VERSION, 0)
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

        public <A> Maybe<PointFreeOptic<S, S, A, A>> lower(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof Optic<?, ?, ?, ?> optic) {
                return castTypedOptic(optic.typedOptic());
            }
            return Maybe.none();
        }

        public <A> PointFreeOptic<S, S, A, A> lowerOrThrow(String methodName) {
            return this.<A>lower(methodName).orElseGet(() -> {
                throw new IllegalArgumentException(
                        "Generated optic for method " + methodName + " does not carry optimizer metadata");
            });
        }

        public <A> PointFree<Function<S, S>> modifyPlan(
                String methodName,
                String functionName,
                Function<? super A, ? extends A> function) {
            return OpticLowering.modify(lowerOrThrow(methodName), functionName, function);
        }

        public <A> PointFree<Function<S, S>> optimizedModifyPlan(
                String methodName,
                String functionName,
                Function<? super A, ? extends A> function) {
            return PointFreeOptimizer.optimize(modifyPlan(methodName, functionName, function));
        }

        public <A> PointFreeExecutor<Function<S, S>> compileModify(
                String methodName,
                String functionName,
                Function<? super A, ? extends A> function) {
            return PointFreeBytecodeBackend.compile(modifyPlan(methodName, functionName, function));
        }

        public <A> S applyModify(
                String methodName,
                String functionName,
                Function<? super A, ? extends A> function,
                S source) {
            return optimizedModifyPlan(methodName, functionName, function).eval().apply(source);
        }

        public <A> PointFree<Function<S, S>> setPlan(String methodName, A value) {
            return OpticLowering.set(lowerOrThrow(methodName), value);
        }

        public <A> PointFree<Function<S, S>> optimizedSetPlan(String methodName, A value) {
            return PointFreeOptimizer.optimize(setPlan(methodName, value));
        }

        public <A> PointFreeExecutor<Function<S, S>> compileSet(String methodName, A value) {
            return PointFreeBytecodeBackend.compile(setPlan(methodName, value));
        }

        public <A> S applySet(String methodName, A value, S source) {
            return optimizedSetPlan(methodName, value).eval().apply(source);
        }

        private Object opticValue(String methodName) {
            Object value = optics.get(methodName);
            if (value == null) {
                throw new IllegalArgumentException("No generated optic for method " + methodName);
            }
            return value;
        }

        @SuppressWarnings("unchecked")
        public <A> Lens<S, S, A, A> lens(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof Lens<?, ?, ?, ?> lens) {
                return (Lens<S, S, A, A>) lens;
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " is not a Lens");
        }

        @SuppressWarnings("unchecked")
        public <A> Getter<S, A> getter(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof Getter<?, ?> getter) {
                return (Getter<S, A>) getter;
            }
            if (value instanceof Lens<?, ?, ?, ?> lens) {
                return ((Lens<S, S, A, A>) lens).asGetter();
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a Getter");
        }

        @SuppressWarnings("unchecked")
        public <A> Setter<S, S, A, A> setter(String methodName) {
            Object value = opticValue(methodName);
            return switch (value) {
                case Setter<?, ?, ?, ?> setter -> (Setter<S, S, A, A>) setter;
                case Lens<?, ?, ?, ?> lens -> ((Lens<S, S, A, A>) lens).asSetter();
                case Traversal<?, ?, ?, ?> traversal -> ((Traversal<S, S, A, A>) traversal).asSetter();
                case Prism<?, ?, ?, ?> prism -> ((Prism<S, S, A, A>) prism).asSetter();
                case Affine<?, ?, ?, ?> affine -> ((Affine<S, S, A, A>) affine).asSetter();
                default ->
                        throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a Setter");
            };
        }

        @SuppressWarnings("unchecked")
        public <A> Fold<S, A> fold(String methodName) {
            Object value = opticValue(methodName);
            return switch (value) {
                case Fold<?, ?> fold -> (Fold<S, A>) fold;
                case Lens<?, ?, ?, ?> lens -> ((Lens<S, S, A, A>) lens).asFold();
                case Traversal<?, ?, ?, ?> traversal -> ((Traversal<S, S, A, A>) traversal).asFold();
                case Prism<?, ?, ?, ?> prism -> ((Prism<S, S, A, A>) prism).asFold();
                case Affine<?, ?, ?, ?> affine -> ((Affine<S, S, A, A>) affine).asFold();
                default ->
                        throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a Fold");
            };
        }

        @SuppressWarnings("unchecked")
        public <A> Traversal<S, S, A, A> traversal(String methodName) {
            Object value = opticValue(methodName);
            return switch (value) {
                case Traversal<?, ?, ?, ?> traversal -> (Traversal<S, S, A, A>) traversal;
                case Lens<?, ?, ?, ?> lens -> ((Lens<S, S, A, A>) lens).asTraversal();
                case Prism<?, ?, ?, ?> prism -> ((Prism<S, S, A, A>) prism).asTraversal();
                case Affine<?, ?, ?, ?> affine -> ((Affine<S, S, A, A>) affine).asTraversal();
                default ->
                        throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a Traversal");
            };
        }

        @SuppressWarnings("unchecked")
        public <A> Prism<S, S, A, A> prism(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof Prism<?, ?, ?, ?> prism) {
                return (Prism<S, S, A, A>) prism;
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " is not a Prism");
        }

        @SuppressWarnings("unchecked")
        public <A> Affine<S, S, A, A> affine(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof Affine<?, ?, ?, ?> affine) {
                return (Affine<S, S, A, A>) affine;
            }
            if (value instanceof Lens<?, ?, ?, ?> lens) {
                return Affine.of(
                        source -> Either.right(((Lens<S, S, A, A>) lens).get(source)),
                        (source, next) -> ((Lens<S, S, A, A>) lens).set(next, source));
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as an Affine");
        }
    }

    @SuppressWarnings("unchecked")
    private static <S, A> Maybe<PointFreeOptic<S, S, A, A>> castTypedOptic(Maybe<?> optic) {
        return (Maybe<PointFreeOptic<S, S, A, A>>) optic;
    }
}
