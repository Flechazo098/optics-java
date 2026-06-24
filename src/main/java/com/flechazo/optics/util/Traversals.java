package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.generated.GeneratedTraversal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Traversals {
    private Traversals() {
    }

    public static <S, A> Optional<A> previewOptional(Traversal<S, S, A, A> traversal, S source) {
        return Optionals.fromMaybe(traversal.preview(source));
    }

    public static <S, A> Optional<A> findOptional(
            Traversal<S, S, A, A> traversal, Predicate<? super A> predicate, S source) {
        return Optionals.fromMaybe(traversal.asFold().find(predicate, source));
    }

    public static <A> Traversal<A, A, A, A> filtered(Predicate<? super A> predicate) {
        return new Traversal<>() {
            @Override
            public <F extends K1>
            App<F, A> modifyF(
                    Function<A, App<F, A>> f,
                    A source,
                    Applicative<F, ?> applicative) {
                return predicate.test(source) ? f.apply(source) : applicative.of(source);
            }
        };
    }

    public static <A> Traversal<List<A>, List<A>, A, A> forList() {
        return new GeneratedTraversal<>(GeneratedTraversal.LIST, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
    }

    public static <A> Traversal<Maybe<A>, Maybe<A>, A, A> forMaybe() {
        return new GeneratedTraversal<>(GeneratedTraversal.MAYBE, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
    }

    public static <K, V> Traversal<Map<K, V>, Map<K, V>, V, V> forMapValues() {
        return new GeneratedTraversal<>(GeneratedTraversal.MAP_VALUES, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
    }

    public static <A> Traversal<Set<A>, Set<A>, A, A> forSet() {
        return new GeneratedTraversal<>(GeneratedTraversal.SET, null) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
    }

    public static <A> Traversal<A[], A[], A, A> forArray(Class<A> componentType) {
        return new GeneratedTraversal<>(GeneratedTraversal.ARRAY, componentType) {
            @Override
            protected Object getContainer(Object source) {
                return source;
            }

            @Override
            protected Object setContainer(Object container, Object source) {
                return container;
            }
        };
    }
}
