package com.flechazo.hkt.type;

import com.flechazo.hkt.App;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.TypedOptic;
import com.flechazo.hkt.util.validation.Validation;
import com.google.common.reflect.TypeToken;

import java.util.Objects;

public abstract class Type<A> implements App<Type.Mu, A> {
    public static final class Mu implements K1 {
    }

    public static <A> Type<A> unbox(App<Mu, A> box) {
        return (Type<A>) Validation.kind().narrowWithTypeCheck(box, Type.class);
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

    public final <FT> Maybe<TypedOptic<A, ?, FT, FT>> findType(Type<FT> type, boolean recurse) {
        Objects.requireNonNull(type, "type");
        return findType(type, type, TypeMatcher.exact(), recurse);
    }

    public final <FT, FR> Maybe<TypedOptic<A, ?, FT, FR>> findType(
            Type<FT> type,
            Type<FR> resultType,
            boolean recurse) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(resultType, "resultType");
        return findType(type, resultType, TypeMatcher.exact(), recurse);
    }

    public final Maybe<TypedOptic<A, ?, ?, ?>> findField(String name, boolean recurse) {
        requireName(name, "name");
        return castMaybe(findType(Types.variable("field:" + name), Types.variable("field:" + name),
                TypeMatcher.field(name), recurse));
    }

    public final Maybe<Type<?>> findFieldType(String name) {
        return findField(name, false).map(TypedOptic::aType);
    }

    public <FT, FR> Maybe<TypedOptic<A, ?, FT, FR>> findType(
            Type<FT> type,
            Type<FR> resultType,
            TypeMatcher<FT, FR> matcher,
            boolean recurse) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(resultType, "resultType");
        Objects.requireNonNull(matcher, "matcher");
        Maybe<TypedOptic<A, ?, FT, FR>> local = matcher.match(this, type, resultType);
        return local.isDefined() ? local : findTypeInChildren(type, resultType, matcher, recurse);
    }

    public <FT, FR> Maybe<TypedOptic<A, ?, FT, FR>> findTypeInChildren(
            Type<FT> type,
            Type<FR> resultType,
            TypeMatcher<FT, FR> matcher,
            boolean recurse) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(resultType, "resultType");
        Objects.requireNonNull(matcher, "matcher");
        return Maybe.none();
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

    @FunctionalInterface
    public interface TypeMatcher<FT, FR> {
        <S> Maybe<TypedOptic<S, ?, FT, FR>> match(Type<S> sourceType, Type<FT> type, Type<FR> resultType);

        static <FT, FR> TypeMatcher<FT, FR> exact() {
            return new TypeMatcher<>() {
                @Override
                public <S> Maybe<TypedOptic<S, ?, FT, FR>> match(
                        Type<S> sourceType,
                        Type<FT> type,
                        Type<FR> resultType) {
                    if (!sourceType.equals(type)) {
                        return Maybe.none();
                    }
                    return Maybe.some(castOptic(TypedOptic.adapter(type, resultType)));
                }
            };
        }

        static TypeMatcher<Object, Object> field(String name) {
            requireName(name, "name");
            return new TypeMatcher<>() {
                @Override
                public <S> Maybe<TypedOptic<S, ?, Object, Object>> match(
                        Type<S> sourceType,
                        Type<Object> ignoredType,
                        Type<Object> ignoredResultType) {
                    if (sourceType instanceof Tag.TagType<?> tag && tag.name().equals(name)) {
                        return Maybe.some(castOptic(TypedOptic.retag(
                                tag,
                                Types.field(name, tag.element()),
                                tag.element(),
                                tag.element())));
                    }
                    return Maybe.none();
                }
            };
        }
    }

    @SuppressWarnings("unchecked")
    protected static <A> Type<A> castType(Type<?> type) {
        return (Type<A>) type;
    }

    @SuppressWarnings("unchecked")
    protected static <S, T, A, B> TypedOptic<S, T, A, B> castOptic(TypedOptic<?, ?, ?, ?> optic) {
        return (TypedOptic<S, T, A, B>) optic;
    }

    @SuppressWarnings("unchecked")
    protected static <S, T, A, B> TypedOptic<S, T, A, B> composeOptics(
            TypedOptic<?, ?, ?, ?> outer,
            TypedOptic<?, ?, ?, ?> inner) {
        TypedOptic<Object, Object, Object, Object> typedOuter =
                (TypedOptic<Object, Object, Object, Object>) outer;
        TypedOptic<Object, Object, Object, Object> typedInner =
                (TypedOptic<Object, Object, Object, Object>) inner;
        return (TypedOptic<S, T, A, B>) typedOuter.compose(typedInner);
    }

    @SuppressWarnings("unchecked")
    protected static <A> Maybe<A> castMaybe(Maybe<?> maybe) {
        return (Maybe<A>) maybe;
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
            if (this == obj) return true;
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
