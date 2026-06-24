package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.function.Function3;
import com.flechazo.hkt.functions.PointFreeOptic;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Lens<S, T, A, B> extends Optic<S, T, A, B> {
    A get(S source);

    T set(B value, S source);

    default T modify(Function<? super A, ? extends B> f, S source) {
        return set(f.apply(get(source)), source);
    }

    <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Functor<F, ?> functor);

    @Override
    default <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
        return modifyF(f, source, (Functor<F, ?>) applicative);
    }

    default Traversal<S, T, A, B> asTraversal() {
        Lens<S, T, A, B> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(f, source, applicative);
            }

            @Override
            public Maybe<PointFreeOptic<S, T, A, B>> typedOptic() {
                return self.typedOptic();
            }
        };
    }

    default Getter<S, A> asGetter() {
        return this::get;
    }

    default Setter<S, T, A, B> asSetter() {
        Lens<S, T, A, B> self = this;
        return new Setter<>() {
            @Override
            public T modify(Function<? super A, ? extends B> f, S source) {
                return self.modify(f, source);
            }

            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(f, source, applicative);
            }

            @Override
            public Maybe<PointFreeOptic<S, T, A, B>> typedOptic() {
                return self.typedOptic();
            }
        };
    }

    default Fold<S, A> asFold() {
        Lens<S, T, A, B> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return f.apply(self.get(source));
            }
        };
    }

    default <C, D> Lens<S, T, C, D> andThen(Lens<A, B, C, D> other) {
        Lens<S, T, A, B> self = this;
        return new Lens<>() {
            @Override
            public C get(S source) {
                return other.get(self.get(source));
            }

            @Override
            public T set(D value, S source) {
                return self.set(other.set(value, self.get(source)), source);
            }

            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Functor<F, ?> functor) {
                return functor.map(next -> self.set(next, source), other.modifyF(f, self.get(source), functor));
            }

            @Override
            public Maybe<PointFreeOptic<S, T, C, D>> typedOptic() {
                return self.typedOptic().flatMap(left -> other.typedOptic().map(left::andThen));
            }
        };
    }

    default <C> Fold<S, C> andThen(Fold<A, C> fold) {
        Lens<S, T, A, B> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super C, ? extends M> f, S source) {
                return fold.foldMap(monoid, f, self.get(source));
            }
        };
    }

    default <C, D> Affine<S, T, C, D> andThen(Prism<A, B, C, D> other) {
        Lens<S, T, A, B> self = this;
        return Affine.<S, T, C, D>of(
                source -> other.match(self.get(source)).mapLeft(value -> self.set(value, source)),
                (source, value) -> self.set(other.build(value), source))
                .withTypedOptic(self.typedOptic().flatMap(left -> other.typedOptic().map(left::andThen)));
    }

    default <C, D> Affine<S, T, C, D> andThen(Affine<A, B, C, D> other) {
        Lens<S, T, A, B> self = this;
        return Affine.<S, T, C, D>of(
                source -> other.preview(self.get(source)).mapLeft(value -> self.set(value, source)),
                (source, value) -> self.set(other.set(value, self.get(source)), source))
                .withTypedOptic(self.typedOptic().flatMap(left -> other.typedOptic().map(left::andThen)));
    }

    default <C, D> Traversal<S, T, C, D> andThen(Traversal<A, B, C, D> other) {
        Lens<S, T, A, B> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return applicative.map(
                        next -> self.set(next, source), other.modifyF(f, self.get(source), applicative));
            }

            @Override
            public Maybe<PointFreeOptic<S, T, C, D>> typedOptic() {
                return self.typedOptic().flatMap(left -> other.typedOptic().map(left::andThen));
            }
        };
    }

    default T modifyBranch(
            Predicate<? super A> predicate,
            Function<? super A, ? extends B> thenModifier,
            Function<? super A, ? extends B> elseModifier,
            S source) {
        A current = get(source);
        return predicate.test(current)
                ? set(thenModifier.apply(current), source)
                : set(elseModifier.apply(current), source);
    }

    static <S, T, A, B> Lens<S, T, A, B> of(
            Function<? super S, ? extends A> getter,
            BiFunction<S, B, T> setter) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(setter, "setter");
        return new Lens<>() {
            @Override
            public A get(S source) {
                return getter.apply(source);
            }

            @Override
            public T set(B value, S source) {
                return setter.apply(source, value);
            }

            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> f, S source, Functor<F, ?> functor) {
                return functor.map(value -> set(value, source), f.apply(get(source)));
            }
        };
    }

    static <S, A, B> Lens<S, S, Pair<A, B>, Pair<A, B>> paired(
            Lens<S, S, A, A> first, Lens<S, S, B, B> second, Function3<S, A, B, S> rebuild) {
        return Lens.of(
                source -> Pair.of(first.get(source), second.get(source)),
                (source, pair) -> rebuild.apply(source, pair.first(), pair.second()));
    }

    static <S, A, B> Lens<S, S, Pair<A, B>, Pair<A, B>> paired(
            Lens<S, S, A, A> first, Lens<S, S, B, B> second, BiFunction<A, B, S> constructor) {
        return paired(first, second, (source, a, b) -> constructor.apply(a, b));
    }

    @SuppressWarnings("unchecked")
    private static <B> B cast(Object value) {
        return (B) value;
    }
}
