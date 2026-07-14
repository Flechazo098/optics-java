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
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticsLookupResolver;
import com.flechazo.hkt.business.util.OptionalOps;
import io.smallrye.classfile.ClassFile;

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
        return generate(specType, sourceType, OpticsLookupResolver.lookupFor(specType));
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
        MethodHandles.Lookup sourceLookup = lookup.lookupClass().getModule() == sourceType.getModule()
                ? lookup
                : OpticsLookupResolver.lookupFor(sourceType);
        DerivedSpec derived = build(specType, sourceType, sourceLookup);
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

    private static <S> DerivedSpec build(
            Class<?> specType,
            Class<S> sourceType,
            MethodHandles.Lookup sourceLookup) {
        LinkedHashMap<String, Object> optics = new LinkedHashMap<>();
        ArrayList<Method> methods = new ArrayList<>();
        ArrayList<Object> orderedOptics = new ArrayList<>();
        Map<String, PLens<S, S, ?, ?>> lenses = sourceType.isRecord()
                ? ClassFileOptics.lenses(sourceType, sourceLookup)
                : Map.of();
        Map<Class<? extends S>, PPrism<S, S, ? extends S, ? extends S>> prisms =
                sourceType.isSealed() ? ClassFileOptics.prisms(sourceType, sourceLookup) : Map.of();
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
            if (raw == PLens.class) {
                optic = lenses.get(method.getName());
            } else if (raw == Getter.class) {
                PLens<S, S, ?, ?> lens = lenses.get(method.getName());
                optic = lens == null ? null : lens.asGetter();
            } else if (raw == PSetter.class) {
                PLens<S, S, ?, ?> lens = lenses.get(method.getName());
                optic = lens == null ? null : lens.asSetter();
            } else if (raw == Fold.class) {
                PLens<S, S, ?, ?> lens = lenses.get(method.getName());
                optic = lens == null ? null : lens.asFold();
            } else if (raw == PTraversal.class) {
                optic = ClassFileOptics.traversals(sourceType, sourceLookup).get(method.getName());
            } else if (raw == PAffine.class) {
                PLens<S, S, ?, ?> lens = lenses.get(method.getName());
                Class<?> componentType = componentTypes.get(method.getName());
                optic = lens == null ? null : affineFromLens(lens, sourceType, method.getName(), componentType);
            } else if (raw == PPrism.class) {
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
        return new DerivedSpec(
                Collections.unmodifiableList(methods),
                Collections.unmodifiableMap(optics),
                orderedOptics.toArray());
    }

    private static Map<String, Class<?>> recordComponentTypes(Class<?> recordType) {
        LinkedHashMap<String, Class<?>> result = new LinkedHashMap<>();
        for (RecordComponent component : recordType.getRecordComponents()) {
            result.put(component.getName(), component.getType());
        }
        return Collections.unmodifiableMap(result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <S> PAffine<S, S, ?, ?> affineFromLens(
            PLens<S, S, ?, ?> lens, Class<S> sourceType, String componentName, Class<?> componentType) {
        PLens rawLens = lens;
        Maybe<PointFreeOptic<S, S, Object, Object>> typed = castTypedOptic(OpticMetadata.optic(lens));
        if (Maybe.class.isAssignableFrom(componentType)) {
            PTraversal<S, S, ?, ?> traversal = ClassFileOptics.traversals(sourceType).get(componentName);
            typed = traversal == null ? Maybe.none() : castTypedOptic(OpticMetadata.optic(traversal));
            return OpticMetadata.optic(PAffine.<S, S, Object, Object>of(
                    source -> {
                        Maybe<Object> value = (Maybe<Object>) rawLens.get(source);
                        return value.isDefined()
                                ? Either.right(value.get())
                                : Either.left((S) rawLens.set(Maybe.none(), source));
                    },
                    (source, value) -> (S) rawLens.set(Maybe.some(value), source)), typed);
        }
        if (Optional.class.isAssignableFrom(componentType)) {
            PTraversal<S, S, ?, ?> traversal = ClassFileOptics.traversals(sourceType).get(componentName);
            typed = traversal == null ? Maybe.none() : castTypedOptic(OpticMetadata.optic(traversal));
            return OpticMetadata.optic(PAffine.<S, S, Object, Object>of(
                    source -> {
                        Maybe<Object> value = OptionalOps.toMaybe((Optional<?>) rawLens.get(source));
                        return value.isDefined()
                                ? Either.right(value.get())
                                : Either.left((S) rawLens.set(Optional.empty(), source));
                    },
                    (source, value) -> (S) rawLens.set(Optional.of(value), source)), typed);
        }
        return OpticMetadata.optic(PAffine.<S, S, Object, Object>of(
                        source -> Either.right(rawLens.get(source)),
                        (source, value) -> (S) rawLens.set(value, source)), typed);
    }

    private static Object defineImplementation(
            Class<?> specType, List<Method> methods, Object[] optics, MethodHandles.Lookup callerLookup) {
        try {
            byte[] bytes = generateSpecImplementationBytes(specType, methods);
            MethodHandles.Lookup lookup = privateLookup(specType, callerLookup);
            MethodHandles.Lookup generatedLookup =
                    lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
            Class<?> implClass = generatedLookup.lookupClass();
            return generatedLookup
                    .findConstructor(implClass, java.lang.invoke.MethodType.methodType(void.class, Object[].class))
                    .invoke((Object) optics);
        } catch (Error e) {
            throw e;
        } catch (Throwable e) {
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

        public <A> Maybe<PointFreeOptic<S, S, A, A>> lower(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof Optic<?, ?, ?, ?> optic) {
                return castTypedOptic(OpticMetadata.optic(optic));
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
        public <A> PLens<S, S, A, A> lens(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof PLens<?, ?, ?, ?> lens) {
                return (PLens<S, S, A, A>) lens;
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " is not a PLens");
        }

        @SuppressWarnings("unchecked")
        public <A> Getter<S, A> getter(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof Getter<?, ?> getter) {
                return (Getter<S, A>) getter;
            }
            if (value instanceof PLens<?, ?, ?, ?> lens) {
                return ((PLens<S, S, A, A>) lens).asGetter();
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a Getter");
        }

        @SuppressWarnings("unchecked")
        public <A> PSetter<S, S, A, A> setter(String methodName) {
            Object value = opticValue(methodName);
            return switch (value) {
                case PSetter<?, ?, ?, ?> setter -> (PSetter<S, S, A, A>) setter;
                case PLens<?, ?, ?, ?> lens -> ((PLens<S, S, A, A>) lens).asSetter();
                case PTraversal<?, ?, ?, ?> traversal -> ((PTraversal<S, S, A, A>) traversal).asSetter();
                case PPrism<?, ?, ?, ?> prism -> ((PPrism<S, S, A, A>) prism).asSetter();
                case PAffine<?, ?, ?, ?> affine -> ((PAffine<S, S, A, A>) affine).asSetter();
                default ->
                        throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a PSetter");
            };
        }

        @SuppressWarnings("unchecked")
        public <A> Fold<S, A> fold(String methodName) {
            Object value = opticValue(methodName);
            return switch (value) {
                case Fold<?, ?> fold -> (Fold<S, A>) fold;
                case PLens<?, ?, ?, ?> lens -> ((PLens<S, S, A, A>) lens).asFold();
                case PTraversal<?, ?, ?, ?> traversal -> ((PTraversal<S, S, A, A>) traversal).asFold();
                case PPrism<?, ?, ?, ?> prism -> ((PPrism<S, S, A, A>) prism).asFold();
                case PAffine<?, ?, ?, ?> affine -> ((PAffine<S, S, A, A>) affine).asFold();
                default ->
                        throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a Fold");
            };
        }

        @SuppressWarnings("unchecked")
        public <A> PTraversal<S, S, A, A> traversal(String methodName) {
            Object value = opticValue(methodName);
            return switch (value) {
                case PTraversal<?, ?, ?, ?> traversal -> (PTraversal<S, S, A, A>) traversal;
                case PLens<?, ?, ?, ?> lens -> ((PLens<S, S, A, A>) lens).asTraversal();
                case PPrism<?, ?, ?, ?> prism -> ((PPrism<S, S, A, A>) prism).asTraversal();
                case PAffine<?, ?, ?, ?> affine -> ((PAffine<S, S, A, A>) affine).asTraversal();
                default ->
                        throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as a PTraversal");
            };
        }

        @SuppressWarnings("unchecked")
        public <A> PPrism<S, S, A, A> prism(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof PPrism<?, ?, ?, ?> prism) {
                return (PPrism<S, S, A, A>) prism;
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " is not a PPrism");
        }

        @SuppressWarnings("unchecked")
        public <A> PAffine<S, S, A, A> affine(String methodName) {
            Object value = opticValue(methodName);
            if (value instanceof PAffine<?, ?, ?, ?> affine) {
                return (PAffine<S, S, A, A>) affine;
            }
            if (value instanceof PLens<?, ?, ?, ?> lens) {
                return PAffine.of(
                        source -> Either.right(((PLens<S, S, A, A>) lens).get(source)),
                        (source, next) -> ((PLens<S, S, A, A>) lens).set(next, source));
            }
            throw new IllegalArgumentException("Generated optic for method " + methodName + " cannot be viewed as an PAffine");
        }
    }

    @SuppressWarnings("unchecked")
    private static <S, A> Maybe<PointFreeOptic<S, S, A, A>> castTypedOptic(Maybe<?> optic) {
        return (Maybe<PointFreeOptic<S, S, A, A>>) optic;
    }
}
