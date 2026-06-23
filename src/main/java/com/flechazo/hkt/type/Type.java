package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.App;
import com.flechazo.hkt.K1;
import com.google.common.reflect.TypeToken;

import java.util.Objects;

public abstract class Type<A> implements App<Type.Mu, A> {
    public static final class Mu implements K1 {
    }

    public static <A> Type<A> unbox(App<Mu, A> box) {
        return (Type<A>) box;
    }

    public Maybe<TypeToken<?>> runtimeWitness() {
        return Maybe.none();
    }

    public abstract TypeTemplate template();

    public boolean equals(Type<?> other, boolean ignoreRecursionPoints, boolean checkIndex) {
        return equals(other);
    }

    public Type<?> updateMu(RecursiveTypeFamily newFamily) {
        return this;
    }

    public Type<?> substitute(TypeSubstitution substitution) {
        return substitution.apply(this);
    }

    public boolean containsVariable(String name) {
        Objects.requireNonNull(name, "name");
        return false;
    }

    protected static String requireName(String value, String parameter) {
        Objects.requireNonNull(value, parameter);
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameter + " must not be blank");
        }
        return value;
    }

    public static final class VariableType<A> extends Type<A> {
        private final String name;

        public VariableType(String name) {
            this.name = requireName(name, "name");
        }

        public String name() {
            return name;
        }

        @Override
        public boolean containsVariable(String name) {
            return this.name.equals(Objects.requireNonNull(name, "name"));
        }

        @Override
        public TypeTemplate template() {
            return Types.constType(this);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof VariableType<?> that && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "'" + name;
        }
    }
}
