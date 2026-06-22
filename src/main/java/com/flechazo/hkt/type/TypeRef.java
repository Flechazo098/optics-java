package com.flechazo.hkt.type;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

public abstract class TypeRef<A> {
    private final Type type;

    protected TypeRef() {
        Type superclass = getClass().getGenericSuperclass();
        if (!(superclass instanceof ParameterizedType parameterized)) {
            throw new IllegalStateException("TypeRef must be created with a type parameter");
        }
        this.type = parameterized.getActualTypeArguments()[0];
    }

    private TypeRef(Type type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public static <A> TypeRef<A> of(Class<A> type) {
        return new Explicit<>(type);
    }

    public static <A> TypeRef<A> of(Type type) {
        return new Explicit<>(type);
    }

    public static <A> TypeRef<A> parameterized(Class<?> rawType, TypeRef<?> first, TypeRef<?>... rest) {
        Objects.requireNonNull(rawType, "rawType");
        Objects.requireNonNull(first, "first");
        Type[] arguments = new Type[rest.length + 1];
        arguments[0] = first.type();
        for (int i = 0; i < rest.length; i++) {
            arguments[i + 1] = Objects.requireNonNull(rest[i], "rest[" + i + "]").type();
        }
        return new Explicit<>(new Parameterized(rawType, arguments));
    }

    public static <A> TypeRef<A[]> array(TypeRef<A> componentType) {
        Objects.requireNonNull(componentType, "componentType");
        return new Explicit<>(new GenericArray(componentType.type()));
    }

    public Type type() {
        return type;
    }

    public TypeExpr expr() {
        return TypeExpr.witness(this);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeRef<?> that && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return type.getTypeName();
    }

    private static final class Explicit<A> extends TypeRef<A> {
        private Explicit(Type type) {
            super(type);
        }
    }

    private record Parameterized(Class<?> rawType, Type[] arguments) implements ParameterizedType {
        private Parameterized {
            Objects.requireNonNull(rawType, "rawType");
            arguments = arguments.clone();
        }

        @Override
        public Type[] getActualTypeArguments() {
            return arguments.clone();
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return rawType.getDeclaringClass();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ParameterizedType that
                    && Objects.equals(getOwnerType(), that.getOwnerType())
                    && Objects.equals(rawType, that.getRawType())
                    && Arrays.equals(arguments, that.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arguments) ^ Objects.hashCode(getOwnerType()) ^ Objects.hashCode(rawType);
        }

        @Override
        public String getTypeName() {
            return rawType.getTypeName() + "<"
                    + String.join(", ", Arrays.stream(arguments).map(Type::getTypeName).toList())
                    + ">";
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }

    private record GenericArray(Type componentType) implements GenericArrayType {
        private GenericArray {
            Objects.requireNonNull(componentType, "componentType");
        }

        @Override
        public Type getGenericComponentType() {
            return componentType;
        }

        @Override
        public String getTypeName() {
            return componentType.getTypeName() + "[]";
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }
}
