package com.flechazo.hkt.business.core;

import com.flechazo.hkt.business.capability.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.core.*;
import com.flechazo.hkt.business.data.*;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.*;

import com.flechazo.hkt.Semigroup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Semigroups {
    private Semigroups() {
    }

    public static <A> Semigroup<NonEmptyList<A>> nonEmptyList() {
        return NonEmptyList.semigroup();
    }

    public static <A> Semigroup<List<A>> list() {
        return Semigroup.of((left, right) -> {
            ArrayList<A> result = new ArrayList<>(left.size() + right.size());
            result.addAll(left);
            result.addAll(right);
            return result;
        });
    }

    public static <A> Semigroup<Set<A>> set() {
        return Semigroup.of((left, right) -> {
            LinkedHashSet<A> result = new LinkedHashSet<>(left);
            result.addAll(right);
            return result;
        });
    }

    public static <A> Semigroup<LinkedHashSet<A>> linkedHashSet() {
        return Semigroup.of((left, right) -> {
            LinkedHashSet<A> result = new LinkedHashSet<>(left);
            result.addAll(right);
            return result;
        });
    }

    public static Semigroup<String> string() {
        return Semigroup.of(String::concat);
    }

    public static Semigroup<Integer> intSum() {
        return Semigroup.of(Integer::sum);
    }

    public static Semigroup<Long> longSum() {
        return Semigroup.of(Long::sum);
    }

    public static Semigroup<Boolean> booleanOr() {
        return Semigroup.of(Boolean::logicalOr);
    }

    public static Semigroup<Boolean> booleanAnd() {
        return Semigroup.of(Boolean::logicalAnd);
    }
}
