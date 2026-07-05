package com.flechazo.hkt.business.core;

import com.flechazo.hkt.business.capability.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.core.*;
import com.flechazo.hkt.business.data.*;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.*;

import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Semigroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Monoids {
    private Monoids() {
    }

    public static <A> Monoid<List<A>> list() {
        return Monoid.of(List.of(), (left, right) -> {
            ArrayList<A> result = new ArrayList<>(left.size() + right.size());
            result.addAll(left);
            result.addAll(right);
            return result;
        });
    }

    public static <A> Monoid<Set<A>> set() {
        return Monoid.of(Set.of(), (left, right) -> {
            LinkedHashSet<A> result = new LinkedHashSet<>(left);
            result.addAll(right);
            return result;
        });
    }

    public static <A> Monoid<LinkedHashSet<A>> linkedHashSet() {
        return Monoid.of(new LinkedHashSet<>(), (left, right) -> {
            LinkedHashSet<A> result = new LinkedHashSet<>(left);
            result.addAll(right);
            return result;
        });
    }

    public static Monoid<String> string() {
        return Monoid.of("", String::concat);
    }

    public static Monoid<Integer> intSum() {
        return Monoid.of(0, Integer::sum);
    }

    public static Monoid<Long> longSum() {
        return Monoid.of(0L, Long::sum);
    }

    public static Monoid<Boolean> booleanOr() {
        return Monoid.of(false, Boolean::logicalOr);
    }

    public static Monoid<Boolean> booleanAnd() {
        return Monoid.of(true, Boolean::logicalAnd);
    }

    public static <K, V> Monoid<Map<K, V>> map(Semigroup<V> values) {
        return Monoid.of(Map.of(), (left, right) -> {
            LinkedHashMap<K, V> result = new LinkedHashMap<>(left);
            for (Map.Entry<K, V> entry : right.entrySet()) {
                result.merge(entry.getKey(), entry.getValue(), values::combine);
            }
            return result;
        });
    }
}
