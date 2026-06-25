package com.flechazo.hkt.type;

import com.flechazo.hkt.Pair;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.TypedOptic;

import java.util.Objects;

public record Named(String name, TypeTemplate element) implements TypeTemplate {
    public Named {
        name = Type.requireName(name, "name");
        Types.requireTemplate(element, "element");
    }

    @Override
    public int size() {
        return element.size();
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return index -> Types.named(name, element.apply(family).apply(index));
    }

    @Override
    public String toString() {
        return "NamedTypeTag[" + name + ": " + element + "]";
    }

    public static final class NamedType<A> extends Type<Pair<String, A>> {
        private final String name;
        private final Type<A> element;

        public NamedType(String name, Type<A> element) {
            this.name = Type.requireName(name, "name");
            this.element = Objects.requireNonNull(element, "element");
        }

        public String name() {
            return name;
        }

        public Type<A> element() {
            return element;
        }

        @Override
        public TypeTemplate template() {
            return Types.named(name, element.template());
        }

        @Override
        public boolean containsVariable(String name) {
            return element.containsVariable(name);
        }

        @Override
        public <FT, FR> Maybe<TypedOptic<Pair<String, A>, ?, FT, FR>> findTypeInChildren(
                Type<FT> type,
                Type<FR> resultType,
                TypeMatcher<FT, FR> matcher,
                boolean recurse) {
            Maybe<TypedOptic<A, ?, FT, FR>> optic = element.findType(type, resultType, matcher, recurse);
            if (optic.isEmpty()) {
                return Maybe.none();
            }
            Type<?> target = Types.named(name, optic.get().tType());
            return Maybe.some(castOptic(optic.get().castOuter(this, castType(target))));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            return obj instanceof NamedType<?> that && name.equals(that.name) && element.equals(that.element);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + element.hashCode();
        }

        @Override
        public String toString() {
            return "NamedType[\"" + name + "\", " + element + "]";
        }
    }
}
