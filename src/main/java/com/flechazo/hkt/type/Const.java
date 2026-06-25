package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;
import com.google.common.reflect.TypeToken;

import java.util.Objects;

public record Const(Type<?> type) implements TypeTemplate {
    public Const {
        Types.requireType(type, "type");
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return TypeFamily.constant(type);
    }

    @Override
    public String toString() {
        return "Const[" + type + "]";
    }

    public static final class PrimitiveType<A> extends Type<A> {
        private final TypeToken<A> token;

        public PrimitiveType(TypeToken<A> token) {
            this.token = Objects.requireNonNull(token, "token");
        }

        @Override
        public Maybe<TypeToken<?>> runtimeWitness() {
            return Maybe.some(token);
        }

        @Override
        public TypeTemplate template() {
            return new Const(this);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            return obj instanceof PrimitiveType<?> that && token.equals(that.token);
        }

        @Override
        public int hashCode() {
            return token.hashCode();
        }

        @Override
        public String toString() {
            return token.toString();
        }
    }
}
