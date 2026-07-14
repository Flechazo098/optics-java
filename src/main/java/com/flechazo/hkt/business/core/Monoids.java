package com.flechazo.hkt.business.core;

import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Semigroup;

import java.util.*;

/**
 * Provides monoids for common immutable values and collections.
 */
public final class Monoids {
    private Monoids() {
    }

    /**
     * Returns a list concatenation monoid.
     *
     * @param <A> the element type
     * @return a monoid producing unmodifiable concatenated lists
     */
    public static <A> Monoid<List<A>> list() {
        return Monoid.of(List.of(), (left, right) -> {
            ArrayList<A> result = new ArrayList<>(left.size() + right.size());
            result.addAll(left);
            result.addAll(right);
            return Collections.unmodifiableList(result);
        });
    }

    /**
     * Returns a set union monoid preserving encounter order.
     *
     * @param <A> the element type
     * @return a monoid producing unmodifiable sets
     */
    public static <A> Monoid<Set<A>> set() {
        return Monoid.of(Set.of(), (left, right) -> {
            LinkedHashSet<A> result = new LinkedHashSet<>(left);
            result.addAll(right);
            return Collections.unmodifiableSet(result);
        });
    }

    /**
     * Returns a linked-order set union monoid.
     *
     * @param <A> the element type
     * @return a monoid producing unmodifiable encounter-ordered sets
     */
    public static <A> Monoid<Set<A>> linkedHashSet() {
        return Monoid.of(Set.of(), (left, right) -> {
            LinkedHashSet<A> result = new LinkedHashSet<>(left);
            result.addAll(right);
            return Collections.unmodifiableSet(result);
        });
    }

    /**
     * Returns the string concatenation monoid.
     *
     * @return the string concatenation monoid
     */
    public static Monoid<String> string() {
        return Monoid.of("", String::concat);
    }

    /**
     * Returns the integer addition monoid.
     *
     * @return the integer addition monoid
     */
    public static Monoid<Integer> intSum() {
        return Monoid.of(0, Integer::sum);
    }

    /**
     * Returns the long addition monoid.
     *
     * @return the long addition monoid
     */
    public static Monoid<Long> longSum() {
        return Monoid.of(0L, Long::sum);
    }

    /**
     * Returns the boolean disjunction monoid.
     *
     * @return the boolean disjunction monoid
     */
    public static Monoid<Boolean> booleanOr() {
        return Monoid.of(false, Boolean::logicalOr);
    }

    /**
     * Returns the boolean conjunction monoid.
     *
     * @return the boolean conjunction monoid
     */
    public static Monoid<Boolean> booleanAnd() {
        return Monoid.of(true, Boolean::logicalAnd);
    }

    /**
     * Returns a map union monoid that combines values of duplicate keys.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param values the semigroup used when both maps contain the same key
     * @return a monoid producing unmodifiable encounter-ordered maps
     */
    public static <K, V> Monoid<Map<K, V>> map(Semigroup<V> values) {
        return Monoid.of(Map.of(), (left, right) -> {
            LinkedHashMap<K, V> result = new LinkedHashMap<>(left);
            for (Map.Entry<K, V> entry : right.entrySet()) {
                result.merge(entry.getKey(), entry.getValue(), values::combine);
            }
            return Collections.unmodifiableMap(result);
        });
    }
}
