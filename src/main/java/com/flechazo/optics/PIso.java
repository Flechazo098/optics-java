package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Functor;
import com.flechazo.hkt.K1;

import java.util.Objects;
import java.util.function.Function;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

public interface PIso<S, T, A, B> extends Optic<S, T, A, B> {
    A get(S source);

    T reverseGet(B value);

    default T modify(Function<? super A, ? extends B> f, S source) {
        return reverseGet(f.apply(get(source)));
    }

    @Override
    default <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, com.flechazo.hkt.Applicative<F, ?> applicative) {
        return applicative.map(this::reverseGet, f.apply(get(source)));
    }

    default <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Functor<F, ?> functor) {
        return functor.map(this::reverseGet, f.apply(get(source)));
    }

    default <C, D> PIso<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PIso<S, T, C, D> direct = PIso.of(
                source -> other.get(get(source)),
                value -> reverseGet(other.reverseGet(value)));
        return OpticPrograms.iso(direct, OpticPrograms.compose(this, other));
    }

    default <C, D> PLens<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        PIso<S, T, A, B> self = this;
        PLens<S, T, C, D> direct = PLens.of(
                source -> other.get(self.get(source)),
                (source, value) -> self.reverseGet(other.set(value, self.get(source))));
        return OpticPrograms.lens(direct, OpticPrograms.compose(this, other));
    }

    default <C, D> PPrism<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        PPrism<S, T, C, D> direct = PPrism.of(
                source -> other.match(get(source)).mapLeft(this::reverseGet),
                value -> reverseGet(other.build(value)));
        return OpticPrograms.prism(direct, OpticPrograms.compose(this, other));
    }

    default <C, D> PAffine<S, T, C, D> andThen(PAffine<A, B, C, D> other) {
        PAffine<S, T, C, D> direct = PAffine.of(
                source -> other.preview(get(source)).mapLeft(this::reverseGet),
                (source, value) -> reverseGet(other.set(value, get(source))));
        return OpticPrograms.affine(direct, OpticPrograms.compose(this, other));
    }

    default <C, D> PTraversal<S, T, C, D> andThen(PTraversal<A, B, C, D> other) {
        PIso<S, T, A, B> self = this;
        PTraversal<S, T, C, D> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, com.flechazo.hkt.Applicative<F, ?> applicative) {
                return applicative.map(self::reverseGet, other.modifyF(f, self.get(source), applicative));
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.compose(this, other));
    }

    default <C, D> PSetter<S, T, C, D> andThen(PSetter<A, B, C, D> other) {
        PSetter<S, T, C, D> direct =
                (f, source) -> reverseGet(other.modify(f, get(source)));
        return OpticPrograms.setter(direct, OpticPrograms.compose(this, other));
    }

    default <C> Getter<S, C> andThen(Getter<A, C> other) {
        Getter<S, C> direct = source -> other.get(get(source));
        return OpticPrograms.getter(direct, OpticPrograms.compose(this, other));
    }

    default <C> Fold<S, C> andThen(Fold<A, C> other) {
        PIso<S, T, A, B> self = this;
        Fold<S, C> direct = new Fold<>() {
            @Override
            public <M> M foldMap(
                    com.flechazo.hkt.Monoid<M> monoid,
                    Function<? super C, ? extends M> f,
                    S source) {
                return other.foldMap(monoid, f, self.get(source));
            }
        };
        return OpticPrograms.fold(direct, OpticPrograms.compose(this, other));
    }

    static <S, T, A, B> PIso<S, T, A, B> of(
            IsoGetter<? super S, ? extends A> get,
            IsoRebuilder<? super B, ? extends T> reverseGet) {
        PIso<S, T, A, B> direct = of(
                (Function<? super S, ? extends A>) get,
                (Function<? super B, ? extends T>) reverseGet);
        return LambdaLifter.iso(direct, get, reverseGet);
    }

    static <S, T, A, B> PIso<S, T, A, B> of(
            Function<? super S, ? extends A> get,
            Function<? super B, ? extends T> reverseGet) {
        Objects.requireNonNull(get, "get");
        Objects.requireNonNull(reverseGet, "reverseGet");
        PIso<S, T, A, B> direct = new PIso<>() {
            @Override
            public A get(S source) {
                return get.apply(source);
            }

            @Override
            public T reverseGet(B value) {
                return reverseGet.apply(value);
            }
        };
        return OpticPrograms.iso(direct, OpticPrograms.opaque("iso", null));
    }
}
