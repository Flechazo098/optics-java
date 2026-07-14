package com.flechazo.hkt.business.core;

import com.flechazo.hkt.Semigroup;
import com.flechazo.hkt.business.data.NonEmptyList;

import java.util.*;

/**
 * Provides semigroups for common immutable values and collections.
 */
public final class Semigroups {
    private Semigroups() {
    }

    /**
     * Returns a non-empty-list concatenation semigroup.
     *
     * @param <A> the element type
     * @return the non-empty-list concatenation semigroup
     */
    public static <A> Semigroup<NonEmptyList<A>> nonEmptyList() {
        return NonEmptyList.semigroup();
    }

    /**
     * Returns a list concatenation semigroup.
     *
     * @param <A> the element type
     * @return a semigroup producing unmodifiable concatenated lists
     */
    public static <A> Semigroup<List<A>> list() {
        return Semigroup.of((left, right) -> {
            ArrayList<A> result = new ArrayList<>(left.size() + right.size());
            result.addAll(left);
            result.addAll(right);
            return Collections.unmodifiableList(result);
        });
    }

    /**
     * Returns a set union semigroup preserving encounter order.
     *
     * @param <A> the element type
     * @return a semigroup producing unmodifiable sets
     */
    public static <A> Semigroup<Set<A>> set() {
        return Semigroup.of((left, right) -> {
            LinkedHashSet<A> result = new LinkedHashSet<>(left);
            result.addAll(right);
            return Collections.unmodifiableSet(result);
        });
    }

    /**
     * Returns a linked-order set union semigroup.
     *
     * @param <A> the element type
     * @return a semigroup producing unmodifiable encounter-ordered sets
     */
    public static <A> Semigroup<Set<A>> linkedHashSet() {
        return Semigroup.of((left, right) -> {
            LinkedHashSet<A> result = new LinkedHashSet<>(left);
            result.addAll(right);
            return Collections.unmodifiableSet(result);
        });
    }

    /**
     * Returns the string concatenation semigroup.
     *
     * @return the string concatenation semigroup
     */
    public static Semigroup<String> string() {
        return Semigroup.of(String::concat);
    }

    /**
     * Returns the integer addition semigroup.
     *
     * @return the integer addition semigroup
     */
    public static Semigroup<Integer> intSum() {
        return Semigroup.of(Integer::sum);
    }

    /**
     * Returns the long addition semigroup.
     *
     * @return the long addition semigroup
     */
    public static Semigroup<Long> longSum() {
        return Semigroup.of(Long::sum);
    }

    /**
     * Returns the boolean disjunction semigroup.
     *
     * @return the boolean disjunction semigroup
     */
    public static Semigroup<Boolean> booleanOr() {
        return Semigroup.of(Boolean::logicalOr);
    }

    /**
     * Returns the boolean conjunction semigroup.
     *
     * @return the boolean conjunction semigroup
     */
    public static Semigroup<Boolean> booleanAnd() {
        return Semigroup.of(Boolean::logicalAnd);
    }
}
