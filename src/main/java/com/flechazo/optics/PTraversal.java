package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public interface PTraversal<S, T, A, B> extends Optic<S, T, A, B> {
    @Override
    <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative);

    default T modify(Function<? super A, ? extends B> f, S source) {
        App<IdF.Mu, T> result =
                modifyF(value -> new IdF<>(f.apply(value)), source, IdF.applicative());
        return IdF.get(result);
    }

    default T set(B value, S source) {
        return modify(ignored -> value, source);
    }

    default List<A> getAll(S source) {
        return asFold().getAll(source);
    }

    default Maybe<A> preview(S source) {
        return asFold().preview(source);
    }

    default int length(S source) {
        return asFold().length(source);
    }

    default boolean exists(Predicate<? super A> predicate, S source) {
        return asFold().exists(predicate, source);
    }

    default boolean all(Predicate<? super A> predicate, S source) {
        return asFold().all(predicate, source);
    }

    default Fold<S, A> asFold() {
        PTraversal<S, T, A, B> self = this;
        Fold<S, A> fold = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                Applicative<Const.Mu<M>, ?> app = Const.applicative(monoid);
                App<Const.Mu<M>, T> folded =
                        self.modifyF(value -> new Const<>(f.apply(value)), source, app);
                return Const.get(folded);
            }
        };
        Fold<S, A> typed = OpticMetadata.fold(
                fold,
                OpticMetadata.<S, T, A, B>optic(self)
                        .map(optic -> PointFreeFold.fromOptic(optic, fold)));
        return OpticPrograms.fold(typed, OpticPrograms.programOrOpaque(self, "traversal"));
    }

    default PSetter<S, T, A, B> asSetter() {
        PTraversal<S, T, A, B> self = this;
        PSetter<S, T, A, B> direct = self::modify;
        return OpticPrograms.setter(direct, OpticPrograms.programOrOpaque(self, "traversal"));
    }

    default <C, D> PTraversal<S, T, C, D> andThen(PTraversal<A, B, C, D> other) {
        PTraversal<S, T, A, B> self = this;
        PTraversal<S, T, C, D> composed = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(value -> other.modifyF(f, value, applicative), source, applicative);
            }
        };
        PTraversal<S, T, C, D> typed = OpticMetadata.optic(
                composed,
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.traversal(typed, OpticPrograms.compose(self, other));
    }

    default <C, D> PTraversal<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PTraversal<S, T, A, B> self = this;
        PTraversal<S, T, C, D> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(
                        value -> applicative.map(other::reverseGet, f.apply(other.get(value))),
                        source,
                        applicative);
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.compose(this, other));
    }

    default <C> Fold<S, C> andThen(Fold<A, C> fold) {
        return asFold().andThen(fold);
    }

    default <C, D> PTraversal<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        return andThen(other.asTraversal());
    }

    default <C, D> PTraversal<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        return andThen(other.asTraversal());
    }

    default <C, D> PTraversal<S, T, C, D> andThen(PAffine<A, B, C, D> other) {
        return andThen(other.asTraversal());
    }

    default T modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends B> modifier,
            Function<? super A, ? extends B> otherwise,
            S source) {
        return modify(value -> predicate.test(value) ? modifier.apply(value) : otherwise.apply(value), source);
    }
}
