package com.flechazo.hkt.functions;

import com.flechazo.hkt.Cartesian;
import com.flechazo.hkt.K1;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
            Method accessor = recordType.getRecordComponents()[componentIndex].getAccessor();
            accessor.setAccessible(true);
            return accessor.invoke(source);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(
                    "Unable to read record component " + recordType.getName() + "." + componentName, e);
        }
    }

    public Object rebuild(Object replacement, Object source) {
        try {
            Object[] arguments = new Object[componentTypes.length];
            RecordComponent[] components = recordType.getRecordComponents();
            for (int i = 0; i < components.length; i++) {
                if (i == componentIndex) {
                    arguments[i] = replacement;
                } else {
                    Method accessor = components[i].getAccessor();
                    accessor.setAccessible(true);
                    arguments[i] = accessor.invoke(source);
                }
            }
            Constructor<?> constructor = recordType.getDeclaredConstructor(componentTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException e) {
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
}
