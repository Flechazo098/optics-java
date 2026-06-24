package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.TypedOptic;

import java.util.Objects;

public record Tag(String name, TypeTemplate element) implements TypeTemplate {
    public Tag {
        name = Type.requireName(name, "name");
        Types.requireTemplate(element, "element");
    }

    @Override
    public int size() {
        return element.size();
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return index -> Types.field(name, element.apply(family).apply(index));
    }

    @Override
    public String toString() {
        return "NameTag[" + name + ": " + element + "]";
    }

    public static final class TagType<A> extends Type<A> {
        private final String name;
        private final Type<A> element;

        public TagType(String name, Type<A> element) {
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
            return Types.field(name, element.template());
        }

        @Override
        public boolean containsVariable(String name) {
            return element.containsVariable(name);
        }

        @Override
        public <FT, FR> Maybe<TypedOptic<A, ?, FT, FR>> findTypeInChildren(
                Type<FT> type,
                Type<FR> resultType,
                TypeMatcher<FT, FR> matcher,
                boolean recurse) {
            Maybe<TypedOptic<A, ?, FT, FR>> optic = element.findType(type, resultType, matcher, recurse);
            if (optic.isEmpty()) {
                return Maybe.none();
            }
            Type<?> target = Types.field(name, optic.get().tType());
            return Maybe.some(castOptic(optic.get().castOuter(this, castType(target))));
        }

        @Override
        public boolean equals(Type<?> other, boolean ignoreRecursionPoints, boolean checkIndex) {
            return other instanceof TagType<?> that
                    && name.equals(that.name)
                    && element.equals(that.element, ignoreRecursionPoints, checkIndex);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TagType<?> that && name.equals(that.name) && element.equals(that.element);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + element.hashCode();
        }

        @Override
        public String toString() {
            return "Tag[\"" + name + "\", " + element + "]";
        }
    }
}
