package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Monoid;

import java.util.function.Function;

public interface Iso<S, A> extends Optic<S, S, A, A> {
    A get(S source);

    S reverseGet(A value);

    @Override
    default <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
        return applicative.map(this::reverseGet, f.apply(get(source)));
    }

    default S modify(Function<? super A, ? extends A> f, S source) {
        return reverseGet(f.apply(get(source)));
    }

    default Iso<A, S> reverse() {
        return Iso.of(this::reverseGet, this::get);
    }

    default Lens<S, A> asLens() {
        return Lens.of(this::get, (source, value) -> reverseGet(value));
    }

    default Traversal<S, A> asTraversal() {
        return Iso.this::modifyF;
    }

    default Fold<S, A> asFold() {
        Iso<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return f.apply(self.get(source));
            }
        };
    }

    default <B> Iso<S, B> andThen(Iso<A, B> other) {
        return Iso.of(source -> other.get(get(source)), value -> reverseGet(other.reverseGet(value)));
    }

    default <B> Getter<S, B> andThen(Getter<A, B> other) {
        return source -> other.get(get(source));
    }

    default <B> Lens<S, B> andThen(Lens<A, B> other) {
        return asLens().andThen(other);
    }

    default <B> Fold<S, B> andThen(Fold<A, B> other) {
        return asFold().andThen(other);
    }

    default <B> Setter<S, B> andThen(Setter<A, B> other) {
        return asSetter().andThen(other);
    }

    default <B> Prism<S, B> andThen(Prism<A, B> other) {
        return Prism.of(source -> other.getMaybe(get(source)), value -> reverseGet(other.build(value)));
    }

    default <B> Affine<S, B> andThen(Affine<A, B> other) {
        return Affine.of(
                source -> other.getMaybe(get(source)),
                (source, value) -> reverseGet(other.set(value, get(source))));
    }

    default <B> Traversal<S, B> andThen(Traversal<A, B> other) {
        Iso<S, A> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<B, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return applicative.map(self::reverseGet, other.modifyF(f, self.get(source), applicative));
            }
        };
    }

    default Setter<S, A> asSetter() {
        return asLens().asSetter();
    }

    static <S, A> Iso<S, A> of(Function<? super S, ? extends A> get, Function<? super A, ? extends S> reverseGet) {
        return new Iso<>() {
            @Override
            public A get(S source) {
                return get.apply(source);
            }

            @Override
            public S reverseGet(A value) {
                return reverseGet.apply(value);
            }
        };
    }
}
