package com.flechazo.hkt.functions;

import com.flechazo.hkt.Cartesian;
import com.flechazo.hkt.K1;
import com.flechazo.optics.internal.OpticsLookupResolver;
import com.google.common.reflect.TypeToken;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record RecordLensOpticElement(
        Class<?> recordType,
        String componentName,
        int componentIndex,
        Class<?> componentType,
        String[] componentNames,
        Class<?>[] componentTypes)
        implements PointFreeOpticElement {
    private static final ClassValue<RecordAccess> ACCESS = new ClassValue<>() {
        @Override
        protected RecordAccess computeValue(Class<?> type) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                        type,
                        OpticsLookupResolver.lookupFor(type));
                RecordComponent[] components = type.getRecordComponents();
                MethodHandle[] accessors = new MethodHandle[components.length];
                Class<?>[] parameterTypes = new Class<?>[components.length];
                for (int index = 0; index < components.length; index++) {
                    accessors[index] = lookup.unreflect(components[index].getAccessor());
                    parameterTypes[index] = components[index].getType();
                }
                MethodHandle constructor = lookup.findConstructor(
                        type,
                        MethodType.methodType(void.class, parameterTypes));
                return new RecordAccess(accessors, constructor);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to create record access for " + type.getName(), exception);
            }
        }
    };

    public RecordLensOpticElement {
        Objects.requireNonNull(recordType, "recordType");
        Objects.requireNonNull(componentName, "componentName");
        Objects.requireNonNull(componentType, "componentType");
        Objects.requireNonNull(componentNames, "componentNames");
        Objects.requireNonNull(componentTypes, "componentTypes");
        componentNames = componentNames.clone();
        componentTypes = componentTypes.clone();
        if (!recordType.isRecord()) {
            throw new IllegalArgumentException(recordType.getName() + " is not a record");
        }
        if (componentNames.length != componentTypes.length) {
            throw new IllegalArgumentException("Component names/types length mismatch");
        }
        if (componentIndex < 0 || componentIndex >= componentTypes.length) {
            throw new IndexOutOfBoundsException(componentIndex);
        }
    }

    public static RecordLensOpticElement of(Class<?> recordType, RecordComponent[] components, int componentIndex) {
        Objects.requireNonNull(components, "components");
        Class<?>[] componentTypes = new Class<?>[components.length];
        String[] componentNames = new String[components.length];
        for (int i = 0; i < components.length; i++) {
            componentNames[i] = components[i].getName();
            componentTypes[i] = components[i].getType();
        }
        RecordComponent component = components[componentIndex];
        return new RecordLensOpticElement(
                recordType,
                component.getName(),
                componentIndex,
                component.getType(),
                componentNames,
                componentTypes);
    }

    @Override
    public String[] componentNames() {
        return componentNames.clone();
    }

    @Override
    public Class<?>[] componentTypes() {
        return componentTypes.clone();
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.LENS;
    }

    @Override
    public Set<TypeToken<? extends K1>> bounds() {
        return Set.of(Cartesian.Mu.TYPE_TOKEN);
    }

    @Override
    public Object key() {
        return new RecordComponentKey(recordType, componentName);
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        Object value = readComponent(source);
        return rebuild(function.apply(value), source);
    }

    public Object readComponent(Object source) {
        try {
            return ACCESS.get(recordType).accessors()[componentIndex].invoke(source);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Unable to read record component " + recordType.getName() + "." + componentName, e);
        }
    }

    public Object rebuild(Object replacement, Object source) {
        try {
            Object[] arguments = new Object[componentTypes.length];
            RecordAccess access = ACCESS.get(recordType);
            for (int i = 0; i < componentTypes.length; i++) {
                if (i == componentIndex) {
                    arguments[i] = replacement;
                } else {
                    arguments[i] = access.accessors()[i].invoke(source);
                }
            }
            return access.constructor().invokeWithArguments(arguments);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Unable to rebuild record component " + recordType.getName() + "." + componentName, e);
        }
    }

    @Override
    public boolean sameOptic(PointFreeOpticElement other) {
        return other.untyped() instanceof RecordLensOpticElement that
                && recordType.equals(that.recordType)
                && componentName.equals(that.componentName)
                && componentIndex == that.componentIndex
                && Arrays.equals(componentNames, that.componentNames)
                && Arrays.equals(componentTypes, that.componentTypes);
    }

    public record RecordComponentKey(Class<?> recordType, String componentName) {
        public RecordComponentKey {
            Objects.requireNonNull(recordType, "recordType");
            Objects.requireNonNull(componentName, "componentName");
        }
    }

    private record RecordAccess(MethodHandle[] accessors, MethodHandle constructor) {
    }
}
