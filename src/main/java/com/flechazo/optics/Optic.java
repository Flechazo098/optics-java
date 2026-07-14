package com.flechazo.optics;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Selective;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.SelectiveOptics;

import java.util.function.Function;

public interface Optic<S, T, A, B> {
    <F extends K1> App<F, T> modifyF(
            Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative);

    default <F extends K1> App<F, T> modifyBranchS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, B>> thenModifier,
            Function<? super A, ? extends App<F, B>> elseModifier,
            S source,
            Selective<F, ?> selective) {
        return SelectiveOptics.modifyBranch(
                this,
                condition,
                thenModifier,
                elseModifier,
                source,
                selective);
    }

    default <F extends K1> App<F, T> modifyWhenS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, B>> modifier,
            Function<? super A, ? extends App<F, B>> otherwise,
            S source,
            Selective<F, ?> selective) {
        return modifyBranchS(condition, modifier, otherwise, source, selective);
    }

    default <F extends K1> App<F, T> modifyUnlessS(
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, B>> modifier,
            Function<? super A, ? extends App<F, B>> otherwise,
            S source,
            Selective<F, ?> selective) {
        return modifyBranchS(condition, otherwise, modifier, source, selective);
    }

    default <C, D> Optic<S, T, C, D> andThen(Optic<A, B, C, D> other) {
        Optic<S, T, A, B> self = this;
        Optic<S, T, C, D> composed = new Optic<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(a -> other.modifyF(f, a, applicative), source, applicative);
            }
        };
        Optic<S, T, C, D> typed = OpticMetadata.optic(
                composed,
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.optic(typed, OpticPrograms.compose(self, other));
    }

    default <U> Optic<S, U, A, B> map(Function<? super T, ? extends U> f) {
        Optic<S, T, A, B> self = this;
        Optic<S, U, A, B> direct = new Optic<>() {
            @Override
            public <F extends K1> App<F, U> modifyF(
                    Function<A, App<F, B>> g, S source, Applicative<F, ?> applicative) {
                return applicative.map(f, self.modifyF(g, source, applicative));
            }
        };
        return OpticPrograms.optic(direct, OpticPrograms.opaque("mappedOptic", null));
    }

    default <R> Optic<R, T, A, B> contramap(Function<? super R, ? extends S> f) {
        Optic<S, T, A, B> self = this;
        Optic<R, T, A, B> direct = new Optic<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> g, R source, Applicative<F, ?> applicative) {
                return self.modifyF(g, f.apply(source), applicative);
            }
        };
        return OpticPrograms.optic(direct, OpticPrograms.opaque("contramappedOptic", null));
    }

    default <R, U> Optic<R, U, A, B> dimap(
            Function<? super R, ? extends S> before, Function<? super T, ? extends U> after) {
        Optic<S, T, A, B> self = this;
        Optic<R, U, A, B> direct = new Optic<>() {
            @Override
            public <F extends K1> App<F, U> modifyF(
                    Function<A, App<F, B>> f, R source, Applicative<F, ?> applicative) {
                return applicative.map(after, self.modifyF(f, before.apply(source), applicative));
            }
        };
        return OpticPrograms.optic(direct, OpticPrograms.opaque("dimappedOptic", null));
    }
}
