package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.TypedOptic;
import com.flechazo.hkt.tuple.Tuple2;
import com.google.common.base.Joiner;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Objects;

public final class TaggedChoice<K> implements TypeTemplate {
    private final String name;
    private final Type<K> keyType;
    private final Object2ObjectMap<K, TypeTemplate> templates;
    private final int size;

    public TaggedChoice(String name, Type<K> keyType, Object2ObjectMap<K, TypeTemplate> templates) {
        this.name = Type.requireName(name, "name");
        this.keyType = Objects.requireNonNull(keyType, "keyType");
        this.templates = Objects.requireNonNull(templates, "templates");
        this.size = templates.values().stream().mapToInt(TypeTemplate::size).max().orElse(0);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public TypeFamily apply(TypeFamily family) {
        return index -> {
            Object2ObjectOpenHashMap<K, Type<?>> types = new Object2ObjectOpenHashMap<>(templates.size());
            for (Map.Entry<K, TypeTemplate> entry : Object2ObjectMaps.fastIterable(templates)) {
                types.put(entry.getKey(), entry.getValue().apply(family).apply(index));
            }
            return Types.taggedChoiceType(name, keyType, types);
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof TaggedChoice<?> that
                && name.equals(that.name)
                && keyType.equals(that.keyType)
                && templates.equals(that.templates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, keyType, templates);
    }

    @Override
    public String toString() {
        return "TaggedChoice[" + name + ", " + Joiner.on(", ").withKeyValueSeparator(" -> ").join(templates) + "]";
    }

    public static final class TaggedChoiceType<K> extends Type<Tuple2<K, ?>> {
        private final String name;
        private final Type<K> keyType;
        private final Object2ObjectMap<K, Type<?>> types;

        public TaggedChoiceType(String name, Type<K> keyType, Object2ObjectMap<K, Type<?>> types) {
            this.name = Type.requireName(name, "name");
            this.keyType = Objects.requireNonNull(keyType, "keyType");
            this.types = Objects.requireNonNull(types, "types");
        }

        public String name() {
            return name;
        }

        public Type<K> keyType() {
            return keyType;
        }

        public Object2ObjectMap<K, Type<?>> types() {
            return types;
        }

        public Type<?> choice(K key) {
            return types.get(key);
        }

        public Maybe<Type<?>> choiceType(K key) {
            Objects.requireNonNull(key, "key");
            return types.containsKey(key) ? Maybe.some(types.get(key)) : Maybe.none();
        }

        public Maybe<TaggedChoiceType<K>> replaceChoice(K key, Type<?> replacement) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(replacement, "replacement");
            if (!types.containsKey(key)) {
                return Maybe.none();
            }
            Object2ObjectOpenHashMap<K, Type<?>> updated = new Object2ObjectOpenHashMap<>(types);
            updated.put(key, replacement);
            return Maybe.some(Types.taggedChoiceType(name, keyType, updated));
        }

        public <A, B> Maybe<TypedOptic<Tuple2<K, ?>, Tuple2<K, ?>, A, B>> branchOptic(
                K key,
                Type<A> current,
                Type<B> replacement) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(current, "current");
            Objects.requireNonNull(replacement, "replacement");
            Type<?> branch = types.get(key);
            if (!Objects.equals(branch, current)) {
                return Maybe.none();
            }
            return Maybe.some(TypedOptic.tagged(this, key, current, replacement));
        }

        @Override
        public TypeTemplate template() {
            Object2ObjectOpenHashMap<K, TypeTemplate> templates = new Object2ObjectOpenHashMap<>(types.size());
            for (Map.Entry<K, Type<?>> entry : Object2ObjectMaps.fastIterable(types)) {
                templates.put(entry.getKey(), entry.getValue().template());
            }
            return Types.taggedChoice(name, keyType, templates);
        }

        @Override
        public boolean containsVariable(String name) {
            if (keyType.containsVariable(name)) {
                return true;
            }
            for (Type<?> type : types.values()) {
                if (type.containsVariable(name)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public <FT, FR> Maybe<TypedOptic<Tuple2<K, ?>, ?, FT, FR>> findTypeInChildren(
                Type<FT> type,
                Type<FR> resultType,
                TypeMatcher<FT, FR> matcher,
                boolean recurse) {
            for (Map.Entry<K, Type<?>> entry : Object2ObjectMaps.fastIterable(types)) {
                Maybe<TypedOptic<Object, ?, FT, FR>> branch =
                        castMaybe(entry.getValue().findType(type, resultType, matcher, recurse));
                if (branch.isDefined()) {
                    Maybe<TypedOptic<Tuple2<K, ?>, Tuple2<K, ?>, Object, ?>> outer =
                            castMaybe(branchOptic(entry.getKey(), entry.getValue(), branch.get().tType()));
                    if (outer.isEmpty()) {
                        return Maybe.none();
                    }
                    return Maybe.some(composeOptics(outer.get(), branch.get()));
                }
            }
            return Maybe.none();
        }

        @Override
        public Type<?> updateMu(RecursiveTypeFamily newFamily) {
            Object2ObjectOpenHashMap<K, Type<?>> updated = new Object2ObjectOpenHashMap<>(types.size());
            for (Map.Entry<K, Type<?>> entry : Object2ObjectMaps.fastIterable(types)) {
                updated.put(entry.getKey(), entry.getValue().updateMu(newFamily));
            }
            return Types.taggedChoiceType(name, keyType, updated);
        }

        @Override
        public boolean equals(Type<?> other, boolean ignoreRecursionPoints, boolean checkIndex) {
            if (!(other instanceof TaggedChoiceType<?> that)
                    || !name.equals(that.name)
                    || !keyType.equals(that.keyType, ignoreRecursionPoints, checkIndex)
                    || !types.keySet().equals(that.types.keySet())) {
                return false;
            }
            for (K key : types.keySet()) {
                if (!types.get(key).equals(that.types.get(key), ignoreRecursionPoints, checkIndex)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            return obj instanceof TaggedChoiceType<?> that
                    && name.equals(that.name)
                    && keyType.equals(that.keyType)
                    && types.equals(that.types);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, keyType, types);
        }

        @Override
        public String toString() {
            return "TaggedChoiceType[" + name + ", " + Joiner.on(", \n").withKeyValueSeparator(" -> ").join(types) + "]\n";
        }
    }
}
