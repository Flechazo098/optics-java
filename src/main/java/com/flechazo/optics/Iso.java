package com.flechazo.optics;

import com.flechazo.hkt.*;

import java.util.function.Function;
import com.flechazo.optics.internal.OpticPrograms;

public interface Iso<S, A> extends PIso<S, S, A, A> {
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
        Lens<S, A> direct = Lens.of(this::get, (source, value) -> reverseGet(value));
        return OpticPrograms.lens(direct, OpticPrograms.programOrOpaque(this, "iso"));
    }

    default Traversal<S, A> asTraversal() {
        Traversal<S, A> direct = Iso.this::modifyF;
        return OpticPrograms.traversal(direct, OpticPrograms.programOrOpaque(this, "iso"));
    }

    default Fold<S, A> asFold() {
        Iso<S, A> self = this;
        Fold<S, A> direct = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return f.apply(self.get(source));
            }
        };
        return OpticPrograms.fold(direct, OpticPrograms.programOrOpaque(this, "iso"));
    }

    default <B> Iso<S, B> andThen(Iso<A, B> other) {
        Iso<S, B> direct = Iso.of(source -> other.get(get(source)), value -> reverseGet(other.reverseGet(value)));
        return OpticPrograms.iso(direct, OpticPrograms.compose(this, other));
    }

    default <B> Getter<S, B> andThen(Getter<A, B> other) {
        Getter<S, B> direct = source -> other.get(get(source));
        return OpticPrograms.getter(direct, OpticPrograms.compose(this, other));
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
        Prism<S, B> direct =
                Prism.of(source -> other.match(get(source)).mapLeft(this::reverseGet), value -> reverseGet(other.build(value)));
        return OpticPrograms.prism(direct, OpticPrograms.compose(this, other));
    }

    default <B> Affine<S, B> andThen(Affine<A, B> other) {
        Affine<S, B> direct = Affine.of(
                source -> other.preview(get(source)).mapLeft(this::reverseGet),
                (source, value) -> reverseGet(other.set(value, get(source))));
        return OpticPrograms.affine(direct, OpticPrograms.compose(this, other));
    }

    default <B> Traversal<S, B> andThen(Traversal<A, B> other) {
        Iso<S, A> self = this;
        Traversal<S, B> direct = new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<B, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return applicative.map(self::reverseGet, other.modifyF(f, self.get(source), applicative));
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.compose(this, other));
    }

    default Setter<S, A> asSetter() {
        return asLens().asSetter();
    }

    static <S, A> Iso<S, A> of(Function<? super S, ? extends A> get, Function<? super A, ? extends S> reverseGet) {
        Iso<S, A> direct = new Iso<>() {
            @Override
            public A get(S source) {
                return get.apply(source);
            }

            @Override
            public S reverseGet(A value) {
                return reverseGet.apply(value);
            }
        };
        return OpticPrograms.iso(direct, OpticPrograms.opaque("iso", null));
    }

    static <S, A> Iso<S, A> of(
            IsoGetter<? super S, ? extends A> get,
            IsoRebuilder<? super A, ? extends S> reverseGet) {
        PIso<S, S, A, A> lifted = PIso.of(get, reverseGet);
        Iso<S, A> direct = new Iso<>() {
            @Override
            public A get(S source) {
                return lifted.get(source);
            }

            @Override
            public S reverseGet(A value) {
                return lifted.reverseGet(value);
            }
        };
        return OpticPrograms.iso(direct, OpticPrograms.programOrOpaque(lifted, "iso"));
    }
}
