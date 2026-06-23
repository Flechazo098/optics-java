package com.flechazo.hkt.functions;

import com.flechazo.hkt.Cartesian;
import com.flechazo.hkt.Cocartesian;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Pair;
import com.flechazo.hkt.Profunctor;
import com.flechazo.hkt.Traversing;
import com.flechazo.hkt.type.TaggedChoice;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record TypedOptic<S, T, A, B>(
        Set<TypeToken<? extends K1>> bounds,
        List<? extends Element<?, ?, ?, ?>> elements) {
    public TypedOptic {
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(elements, "elements");
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("typed optic spine must not be empty");
        }
    }

    public TypedOptic(
            TypeToken<? extends K1> bound,
            Type<S> sType,
            Type<T> tType,
            Type<A> aType,
            Type<B> bType,
            PointFreeOpticElement optic) {
        this(Set.of(bound), List.of(new Element<>(sType, tType, aType, bType, optic)));
    }

    public Type<S> sType() {
        return outermostElement().sType();
    }

    public Type<T> tType() {
        return outermostElement().tType();
    }

    public Type<A> aType() {
        return innermostElement().aType();
    }

    public Type<B> bType() {
        return innermostElement().bType();
    }

    public PointFreeOpticElement outermost() {
        return outermostElement().optic();
    }

    public PointFreeOpticElement innermost() {
        return innermostElement().optic();
    }

    public List<PointFreeOpticElement> elementOptics() {
        return elements.stream().map(Element::optic).toList();
    }

    public T modify(Function<? super A, ? extends B> function, S source) {
        return cast(modifyFrom(0, castFunction(function), source));
    }

    public boolean isIdentity() {
        return elements.size() == 1 && outermost().kind() == PointFreeOpticKind.ADAPTER
                && Objects.equals(sType(), aType())
                && Objects.equals(tType(), bType());
    }

    public int size() {
        return elements.size();
    }

    public int commonPrefixLength(TypedOptic<?, ?, ?, ?> other) {
        int size = Math.min(elements.size(), other.elements.size());
        for (int i = 0; i < size; i++) {
            if (!elements.get(i).sameOptic(other.elements.get(i))) {
                return i;
            }
        }
        return size;
    }

    public boolean sameElements(TypedOptic<?, ?, ?, ?> other) {
        return elements.size() == other.elements.size()
                && commonPrefixLength(other) == elements.size();
    }

    public boolean containsOnly(PointFreeOpticKind kind) {
        return elements.stream().allMatch(element -> element.optic().kind() == kind);
    }

    public boolean startsWith(PointFreeOpticKind kind) {
        return outermost().kind() == kind;
    }

    public TypedOptic<S, T, Object, Object> prefix(int size) {
        if (size < 1 || size > elements.size()) {
            throw new IndexOutOfBoundsException(size);
        }
        return new TypedOptic<>(bounds, elements.subList(0, size));
    }

    public TypedOptic<Object, Object, A, B> suffix(int from) {
        if (from < 0 || from >= elements.size()) {
            throw new IndexOutOfBoundsException(from);
        }
        return new TypedOptic<>(bounds, elements.subList(from, elements.size()));
    }

    public <A1, B1> TypedOptic<S, T, A1, B1> compose(TypedOptic<A, B, A1, B1> other) {
        ImmutableSet.Builder<TypeToken<? extends K1>> nextBounds = ImmutableSet.builder();
        nextBounds.addAll(bounds);
        nextBounds.addAll(other.bounds);
        ImmutableList.Builder<Element<?, ?, ?, ?>> nextElements =
                ImmutableList.builderWithExpectedSize(elements.size() + other.elements.size());
        nextElements.addAll(elements);
        nextElements.addAll(other.elements);
        return new TypedOptic<>(nextBounds.build(), nextElements.build());
    }

    public <S2, T2> TypedOptic<S2, T2, A, B> castOuter(Type<S2> sType, Type<T2> tType) {
        ImmutableList.Builder<Element<?, ?, ?, ?>> nextElements =
                ImmutableList.builderWithExpectedSize(elements.size());
        nextElements.add(outermostElement().castOuter(sType, tType));
        nextElements.addAll(elements.subList(1, elements.size()));
        return new TypedOptic<>(bounds, nextElements.build());
    }

    @Override
    public String toString() {
        return "(" + elements.stream().map(Object::toString).collect(Collectors.joining(" \u25E6 ")) + ")";
    }

    @SuppressWarnings("unchecked")
    private Element<S, T, ?, ?> outermostElement() {
        return (Element<S, T, ?, ?>) elements.getFirst();
    }

    @SuppressWarnings("unchecked")
    private Element<?, ?, A, B> innermostElement() {
        return (Element<?, ?, A, B>) elements.getLast();
    }

    private Object modifyFrom(
            int index,
            java.util.function.Function<Object, Object> function,
            Object source) {
        if (index == elements.size()) {
            return function.apply(source);
        }
        Element<?, ?, ?, ?> element = elements.get(index);
        return element.optic().modify(value -> modifyFrom(index + 1, function, value), source);
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }

    @SuppressWarnings("unchecked")
    private static java.util.function.Function<Object, Object> castFunction(
            java.util.function.Function<?, ?> function) {
        return (java.util.function.Function<Object, Object>) function;
    }

    public static <S, T> TypedOptic<S, T, S, T> adapter(Type<S> sType, Type<T> tType) {
        return new TypedOptic<>(
                Profunctor.Mu.TYPE_TOKEN,
                sType,
                tType,
                sType,
                tType,
                new AdapterOpticElement());
    }

    public static <F, G, F2> TypedOptic<Pair<F, G>, Pair<F2, G>, F, F2> proj1(
            Type<F> fType,
            Type<G> gType,
            Type<F2> newType) {
        return new TypedOptic<>(
                Cartesian.Mu.TYPE_TOKEN,
                Types.and(fType, gType),
                Types.and(newType, gType),
                fType,
                newType,
                new ProductOpticElement(ProductSide.FIRST));
    }

    public static <F, G, G2> TypedOptic<Pair<F, G>, Pair<F, G2>, G, G2> proj2(
            Type<F> fType,
            Type<G> gType,
            Type<G2> newType) {
        return new TypedOptic<>(
                Cartesian.Mu.TYPE_TOKEN,
                Types.and(fType, gType),
                Types.and(fType, newType),
                gType,
                newType,
                new ProductOpticElement(ProductSide.SECOND));
    }

    public static <L, R, L2> TypedOptic<Either<L, R>, Either<L2, R>, L, L2> inj1(
            Type<L> lType,
            Type<R> rType,
            Type<L2> newType) {
        return new TypedOptic<>(
                Cocartesian.Mu.TYPE_TOKEN,
                Types.or(lType, rType),
                Types.or(newType, rType),
                lType,
                newType,
                new SumOpticElement(SumSide.LEFT));
    }

    public static <L, R, R2> TypedOptic<Either<L, R>, Either<L, R2>, R, R2> inj2(
            Type<L> lType,
            Type<R> rType,
            Type<R2> newType) {
        return new TypedOptic<>(
                Cocartesian.Mu.TYPE_TOKEN,
                Types.or(lType, rType),
                Types.or(lType, newType),
                rType,
                newType,
                new SumOpticElement(SumSide.RIGHT));
    }

    public static <A, B> TypedOptic<List<A>, List<B>, A, B> list(Type<A> aType, Type<B> bType) {
        return new TypedOptic<>(
                Traversing.Mu.TYPE_TOKEN,
                Types.list(aType),
                Types.list(bType),
                aType,
                bType,
                TraversalOpticElement.list());
    }

    public static <K, A, B> TypedOptic<Pair<K, ?>, Pair<K, ?>, A, B> tagged(
            TaggedChoice.TaggedChoiceType<K> sType,
            K key,
            Type<A> aType,
            Type<B> bType) {
        return new TypedOptic<>(
                Cocartesian.Mu.TYPE_TOKEN,
                sType,
                replaceTagged(sType, key, aType, bType),
                aType,
                bType,
                new TaggedOpticElement(key));
    }

    private static <K, A, B> Type<Pair<K, ?>> replaceTagged(
            TaggedChoice.TaggedChoiceType<K> sType,
            K key,
            Type<A> aType,
            Type<B> bType) {
        if (Objects.equals(aType, bType)) {
            return sType;
        }
        if (!Objects.equals(sType.choice(key), aType)) {
            throw new IllegalArgumentException("Focused type does not match tagged choice branch");
        }
        Object2ObjectOpenHashMap<K, Type<?>> next =
                new Object2ObjectOpenHashMap<>(sType.types());
        next.put(key, bType);
        return Types.taggedChoiceType(sType.name(), sType.keyType(), next);
    }

    public record Element<S, T, A, B>(
            Type<S> sType,
            Type<T> tType,
            Type<A> aType,
            Type<B> bType,
            PointFreeOpticElement optic) {
        public Element {
            Objects.requireNonNull(sType, "sType");
            Objects.requireNonNull(tType, "tType");
            Objects.requireNonNull(aType, "aType");
            Objects.requireNonNull(bType, "bType");
            Objects.requireNonNull(optic, "optic");
        }

        public <S2, T2> Element<S2, T2, A, B> castOuter(Type<S2> sType, Type<T2> tType) {
            return new Element<>(sType, tType, aType, bType, optic);
        }

        boolean sameOptic(Element<?, ?, ?, ?> other) {
            return optic.sameOptic(other.optic);
        }

        @Override
        public String toString() {
            return optic.toString();
        }
    }
}
