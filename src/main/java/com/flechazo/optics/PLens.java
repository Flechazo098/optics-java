package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.hkt.function.Function3;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.LambdaLifter;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface PLens<S, T, A, B> extends Optic<S, T, A, B> {
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

    default PTraversal<S, T, A, B> asTraversal() {
        PLens<S, T, A, B> self = this;
        PTraversal<S, T, A, B> traversal = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(f, source, applicative);
            }
        };
        PTraversal<S, T, A, B> typed = OpticMetadata.optic(traversal, OpticMetadata.optic(self));
        return OpticPrograms.traversal(typed, OpticPrograms.programOrOpaque(self, "lens"));
    }

    default Getter<S, A> asGetter() {
        PLens<S, T, A, B> self = this;
        Getter<S, A> getter = new Getter<>() {
            @Override
            public A get(S source) {
                return self.get(source);
            }
        };
        Getter<S, A> typed = OpticMetadata.fold(
                getter,
                OpticMetadata.<S, T, A, B>optic(self)
                        .map(optic -> PointFreeFold.fromOptic(optic, getter)));
        return OpticPrograms.getter(typed, OpticPrograms.programOrOpaque(self, "lens"));
    }

    default PSetter<S, T, A, B> asSetter() {
        PLens<S, T, A, B> self = this;
        PSetter<S, T, A, B> direct = self::modify;
        return OpticPrograms.setter(direct, OpticPrograms.programOrOpaque(self, "lens"));
    }

    default Fold<S, A> asFold() {
        PLens<S, T, A, B> self = this;
        Fold<S, A> fold = new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return f.apply(self.get(source));
            }
        };
        Fold<S, A> typed = OpticMetadata.fold(
                fold,
                OpticMetadata.<S, T, A, B>optic(self)
                        .map(optic -> PointFreeFold.fromOptic(optic, fold)));
        return OpticPrograms.fold(typed, OpticPrograms.programOrOpaque(self, "lens"));
    }

    default <C, D> PLens<S, T, C, D> andThen(PLens<A, B, C, D> other) {
        PLens<S, T, A, B> self = this;
        PLens<S, T, C, D> composed = new PLens<>() {
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
        };
        PLens<S, T, C, D> typed = OpticMetadata.optic(
                composed,
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.lens(typed, OpticPrograms.compose(self, other));
    }

    default <C, D> PLens<S, T, C, D> andThen(PIso<A, B, C, D> other) {
        PLens<S, T, A, B> self = this;
        PLens<S, T, C, D> direct = PLens.of(
                source -> other.get(self.get(source)),
                (source, value) -> self.set(other.reverseGet(value), source));
        return OpticPrograms.lens(direct, OpticPrograms.compose(this, other));
    }

    default <C> Fold<S, C> andThen(Fold<A, C> fold) {
        return asFold().andThen(fold);
    }

    default <C, D> PAffine<S, T, C, D> andThen(PPrism<A, B, C, D> other) {
        PLens<S, T, A, B> self = this;
        PAffine<S, T, C, D> typed = OpticMetadata.optic(PAffine.<S, T, C, D>of(
                source -> other.match(self.get(source)).mapLeft(value -> self.set(value, source)),
                (source, value) -> self.set(other.build(value), source)),
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.affine(typed, OpticPrograms.compose(self, other));
    }

    default <C, D> PAffine<S, T, C, D> andThen(PAffine<A, B, C, D> other) {
        PLens<S, T, A, B> self = this;
        PAffine<S, T, C, D> typed = OpticMetadata.optic(PAffine.<S, T, C, D>of(
                source -> other.preview(self.get(source)).mapLeft(value -> self.set(value, source)),
                (source, value) -> self.set(other.set(value, self.get(source)), source)),
                OpticMetadata.<S, T, A, B>optic(self)
                        .flatMap(left -> OpticMetadata.<A, B, C, D>optic(other).map(left::andThen)));
        return OpticPrograms.affine(typed, OpticPrograms.compose(self, other));
    }

    default <C, D> PTraversal<S, T, C, D> andThen(PTraversal<A, B, C, D> other) {
        PLens<S, T, A, B> self = this;
        PTraversal<S, T, C, D> direct = new PTraversal<>() {
            @Override
            public <F extends K1> App<F, T> modifyF(
                    Function<C, App<F, D>> f, S source, Applicative<F, ?> applicative) {
                return applicative.map(
                        next -> self.set(next, source), other.modifyF(f, self.get(source), applicative));
            }
        };
        return OpticPrograms.traversal(direct, OpticPrograms.compose(self, other));
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

    static <S, T, A, B> PLens<S, T, A, B> of(
            LensGetter<? super S, ? extends A> getter,
            LensRebuilder<S, B, T> setter) {
        PLens<S, T, A, B> direct = of(
                (Function<? super S, ? extends A>) getter,
                (BiFunction<S, B, T>) setter);
        return LambdaLifter.lens(direct, getter, setter);
    }

    static <S, T, A, B> PLens<S, T, A, B> of(
            Function<? super S, ? extends A> getter,
            BiFunction<S, B, T> setter) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(setter, "setter");
        PLens<S, T, A, B> direct = new PLens<>() {
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
        return OpticPrograms.lens(direct, OpticPrograms.opaque("lens", null));
    }

    static <S, T, A, B> PLens<S, T, A, B> opaque(
            Function<? super S, ? extends A> getter,
            BiFunction<S, B, T> setter) {
        return of(getter, setter);
    }

    static <S, A> PLens<S, S, A, A> of(Class<S> recordType, LensGetter<S, A> getter) {
        return RecordOptics.recordLens(recordType, getter);
    }

    static <S, A, B> PLens<S, S, Tuple2<A, B>, Tuple2<A, B>> paired(
            PLens<S, S, A, A> first, PLens<S, S, B, B> second, Function3<S, A, B, S> rebuild) {
        return PLens.of(
                source -> Tuple2.of(first.get(source), second.get(source)),
                (source, Tuple2) -> rebuild.apply(source, Tuple2.first(), Tuple2.second()));
    }

    static <S, A, B> PLens<S, S, Tuple2<A, B>, Tuple2<A, B>> paired(
            PLens<S, S, A, A> first, PLens<S, S, B, B> second, BiFunction<A, B, S> constructor) {
        return paired(first, second, (source, a, b) -> constructor.apply(a, b));
    }

    @SuppressWarnings("unchecked")
    private static <B> B cast(Object value) {
        return (B) value;
    }
}
