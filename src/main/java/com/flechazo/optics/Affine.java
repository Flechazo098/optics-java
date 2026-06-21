package com.flechazo.optics;

import com.flechazo.hkt.*;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Affine<S, A> extends Optic<S, S, A, A> {
    Maybe<A> getMaybe(S source);

    S set(A value, S source);

    default S modify(Function<? super A, ? extends A> f, S source) {
        return getMaybe(source).map(value -> set(f.apply(value), source)).orElse(source);
    }

    @Override
    default <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Applicative<F> applicative) {
        Maybe<A> value = getMaybe(source);
        return value.isDefined()
                ? applicative.map(next -> set(next, source), f.apply(value.get()))
                : applicative.of(source);
    }

    default Traversal<S, A> asTraversal() {
        return Affine.this::modifyF;
    }

    default Setter<S, A> asSetter() {
        Affine<S, A> self = this;
        return new Setter<>() {
            @Override
            public S modify(Function<? super A, ? extends A> f, S source) {
                return self.modify(f, source);
            }

            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f, S source, Applicative<F> applicative) {
                return self.modifyF(f, source, applicative);
            }
        };
    }

    default Fold<S, A> asFold() {
        Affine<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                Maybe<A> value = self.getMaybe(source);
                return value.isDefined() ? f.apply(value.get()) : monoid.empty();
            }
        };
    }

    default boolean matches(S source) {
        return getMaybe(source).isDefined();
    }

    default boolean doesNotMatch(S source) {
        return !matches(source);
    }

    default A getOrElse(A defaultValue, S source) {
        return getMaybe(source).orElse(defaultValue);
    }

    default S modifyWhen(Predicate<? super A> predicate, Function<? super A, ? extends A> f, S source) {
        return getMaybe(source).filter(predicate).map(value -> set(f.apply(value), source)).orElse(source);
    }

    default S setWhen(Predicate<? super A> predicate, A value, S source) {
        return modifyWhen(predicate, ignored -> value, source);
    }

    default <B> Maybe<B> mapMaybe(Function<? super A, ? extends B> f, S source) {
        return getMaybe(source).map(f);
    }

    default Affine<S, A> filtered(Predicate<? super A> predicate) {
        Affine<S, A> self = this;
        return new Affine<>() {
            @Override
            public Maybe<A> getMaybe(S source) {
                return self.getMaybe(source).filter(predicate);
            }

            @Override
            public S set(A value, S source) {
                return self.getMaybe(source).filter(predicate).map(ignored -> self.set(value, source)).orElse(source);
            }

            @Override
            public S remove(S source) {
                return self.getMaybe(source).filter(predicate).map(ignored -> self.remove(source)).orElse(source);
            }
        };
    }

    default S remove(S source) {
        throw new UnsupportedOperationException("This Affine does not support removal");
    }

    default <B> Affine<S, B> andThen(Affine<A, B> other) {
        Affine<S, A> self = this;
        return Affine.of(
                source -> self.getMaybe(source).flatMap(other::getMaybe),
                (source, value) -> self.getMaybe(source).map(a -> self.set(other.set(value, a), source)).orElse(source));
    }

    default <B> Affine<S, B> andThen(Lens<A, B> other) {
        return andThen(Affine.of(source -> Maybe.some(other.get(source)), (source, value) -> other.set(value, source)));
    }

    default <B> Affine<S, B> andThen(Prism<A, B> other) {
        return andThen(Affine.of(other::getMaybe, (source, value) -> other.build(value)));
    }

    default <B> Affine<S, B> andThen(Iso<A, B> other) {
        return Affine.of(
                source -> getMaybe(source).map(other::get),
                (source, value) -> set(other.reverseGet(value), source));
    }

    default <B> Traversal<S, B> andThen(Traversal<A, B> other) {
        Affine<S, A> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<B, App<F, B>> f, S source, Applicative<F> applicative) {
                return self.getMaybe(source)
                        .map(value -> applicative.map(next -> self.set(next, source), other.modifyF(f, value, applicative)))
                        .orElseGet(() -> applicative.of(source));
            }
        };
    }

    static <S, A> Affine<S, A> of(
            Function<? super S, Maybe<A>> preview, BiFunction<S, A, S> setter) {
        return new Affine<>() {
            @Override
            public Maybe<A> getMaybe(S source) {
                return preview.apply(source);
            }

            @Override
            public S set(A value, S source) {
                return setter.apply(source, value);
            }
        };
    }

    static <S, A> Affine<S, A> of(
            Function<? super S, Maybe<A>> preview, BiFunction<S, A, S> setter, Function<S, S> remover) {
        return new Affine<>() {
            @Override
            public Maybe<A> getMaybe(S source) {
                return preview.apply(source);
            }

            @Override
            public S set(A value, S source) {
                return setter.apply(source, value);
            }

            @Override
            public S remove(S source) {
                return remover.apply(source);
            }
        };
    }

    static <S, A, B> Affine<S, B> fromLensAndPrism(Lens<S, A> lens, Prism<A, B> prism) {
        return lens.andThen(prism);
    }

    static <S, A, B> Affine<S, B> fromPrismAndLens(Prism<S, A> prism, Lens<A, B> lens) {
        return prism.andThen(lens);
    }
}
