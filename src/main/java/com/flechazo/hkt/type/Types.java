package com.flechazo.hkt.type;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Unit;
import com.google.common.reflect.TypeToken;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Objects;

public final class Types {
    public static final Type<Unit> UNIT = witness(Unit.class);

    private Types() {
    }

    public static <A> Type<A> witness(Class<A> type) {
        return witness(TypeToken.of(type));
    }

    public static <A> Type<A> witness(TypeToken<A> type) {
        return new Const.PrimitiveType<>(type);
    }

    public static <A> Type<A> variable(String name) {
        return new Type.VariableType<>(name);
    }

    public static TypeTemplate constType(Type<?> type) {
        return new Const(type);
    }

    public static <A> TypeTemplate constType(Class<A> type) {
        return constType(witness(type));
    }

    public static TypeTemplate id(int index) {
        return new RecursivePoint(index);
    }

    public static TypeTemplate and(TypeTemplate first, TypeTemplate second) {
        return new Product(first, second);
    }

    public static <F, G> Product.ProductType<F, G> and(Type<F> first, Type<G> second) {
        return new Product.ProductType<>(first, second);
    }

    public static TypeTemplate or(TypeTemplate left, TypeTemplate right) {
        return new Sum(left, right);
    }

    public static <L, R> Sum.SumType<L, R> or(Type<L> left, Type<R> right) {
        return new Sum.SumType<>(left, right);
    }

    public static TypeTemplate list(TypeTemplate element) {
        return new ListTemplate(element);
    }

    public static <A> ListTemplate.ListType<A> list(Type<A> element) {
        return new ListTemplate.ListType<>(element);
    }

    public static TypeTemplate compoundList(TypeTemplate key, TypeTemplate value) {
        return new CompoundList(key, value);
    }

    public static <K, V> CompoundList.CompoundListType<K, V> compoundList(Type<K> key, Type<V> value) {
        return new CompoundList.CompoundListType<>(key, value);
    }

    public static <K, V> MapType<K, V> map(Type<K> key, Type<V> value) {
        return new MapType<>(key, value);
    }

    public static <A> Type<Either<A, Unit>> optional(Type<A> value) {
        return or(value, UNIT);
    }

    public static TypeTemplate optional(TypeTemplate value) {
        return or(value, constType(UNIT));
    }

    public static <A, B> Func<A, B> function(Type<A> argument, Type<B> result) {
        return new Func<>(argument, result);
    }

    public static TypeTemplate field(String name, TypeTemplate element) {
        return new Tag(name, element);
    }

    public static <A> Tag.TagType<A> field(String name, Type<A> element) {
        return new Tag.TagType<>(name, element);
    }

    public static TypeTemplate named(String name, TypeTemplate element) {
        return new Named(name, element);
    }

    public static <A> Named.NamedType<A> named(String name, Type<A> element) {
        return new Named.NamedType<>(name, element);
    }

    public static <K> TaggedChoice<K> taggedChoice(
            String name,
            Type<K> keyType,
            Object2ObjectMap<K, TypeTemplate> templates) {
        return new TaggedChoice<>(name, keyType, templates);
    }

    public static <K> TaggedChoice.TaggedChoiceType<K> taggedChoiceType(
            String name,
            Type<K> keyType,
            Object2ObjectMap<K, Type<?>> types) {
        return new TaggedChoice.TaggedChoiceType<>(name, keyType, types);
    }

    public static <K> TaggedChoice.TaggedChoiceType<K> taggedChoiceType(
            String name,
            Type<K> keyType,
            Map<K, ? extends Type<?>> types) {
        Object2ObjectOpenHashMap<K, Type<?>> owned = new Object2ObjectOpenHashMap<>(types.size());
        for (Map.Entry<K, ? extends Type<?>> entry : types.entrySet()) {
            owned.put(entry.getKey(), entry.getValue());
        }
        return taggedChoiceType(name, keyType, owned);
    }

    public static TypeTemplate checked(String name, int index, TypeTemplate element) {
        return new Check(name, index, element);
    }

    public static <A> Check.CheckType<A> checkedType(String name, int index, Type<A> element) {
        return new Check.CheckType<>(name, index, element);
    }

    public static TypeTemplate variant(String name, Object2ObjectMap<String, TypeTemplate> cases) {
        return new Variant(name, cases);
    }

    public static Variant.VariantType variantType(String name, Object2ObjectMap<String, Type<?>> cases) {
        return new Variant.VariantType(name, cases);
    }

    public static TypeTemplate fields(String name, TypeTemplate element) {
        return field(name, element);
    }

    public static TypeTemplate fields(String name1, TypeTemplate element1, String name2, TypeTemplate element2) {
        return and(field(name1, element1), field(name2, element2));
    }

    static void requireType(Type<?> type, String parameter) {
        Objects.requireNonNull(type, parameter);
    }

    static void requireTemplate(TypeTemplate template, String parameter) {
        Objects.requireNonNull(template, parameter);
    }

    public static final class MapType<K, V> extends Type<Map<K, V>> {
        private final Type<K> key;
        private final Type<V> value;

        public MapType(Type<K> key, Type<V> value) {
            this.key = Objects.requireNonNull(key, "key");
            this.value = Objects.requireNonNull(value, "value");
        }

        public Type<K> key() {
            return key;
        }

        public Type<V> value() {
            return value;
        }

        @Override
        public TypeTemplate template() {
            return compoundList(key.template(), value.template());
        }

        @Override
        public boolean containsVariable(String name) {
            return key.containsVariable(name) || value.containsVariable(name);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MapType<?, ?> that && key.equals(that.key) && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return 31 * key.hashCode() + value.hashCode();
        }

        @Override
        public String toString() {
            return "Map[" + key + ", " + value + "]";
        }
    }
}
