package com.flechazo.optics;

import com.flechazo.hkt.*;
import com.flechazo.hkt.function.Function3;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Lens<S, A> extends Optic<S, S, A, A> {
    A get(S source);

    S set(A value, S source);

    default S modify(Function<? super A, ? extends A> f, S source) {
        return set(f.apply(get(source)), source);
    }

    <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Functor<F> functor);

    @Override
    default <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Applicative<F> applicative) {
        return modifyF(f, source, (Functor<F>) applicative);
    }

    default Traversal<S, A> asTraversal() {
        return this::modifyF;
    }

    default Getter<S, A> asGetter() {
        return this::get;
    }

    default Setter<S, A> asSetter() {
        Lens<S, A> self = this;
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
        Lens<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                return f.apply(self.get(source));
            }
        };
    }

    default <B> Lens<S, B> andThen(Lens<A, B> other) {
        Lens<S, A> self = this;
        return Lens.of(
                source -> other.get(self.get(source)),
                (source, value) -> self.set(other.set(value, self.get(source)), source));
    }

    default <B> Fold<S, B> andThen(Fold<A, B> fold) {
        Lens<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super B, ? extends M> f, S source) {
                return fold.foldMap(monoid, f, self.get(source));
            }
        };
    }

    default <B> Lens<S, B> andThen(Iso<A, B> other) {
        return Lens.of(source -> other.get(get(source)), (source, value) -> set(other.reverseGet(value), source));
    }

    default <B> Affine<S, B> andThen(Prism<A, B> other) {
        Lens<S, A> self = this;
        return Affine.of(
                source -> other.getMaybe(self.get(source)),
                (source, value) -> self.set(other.build(value), source));
    }

    default <B> Affine<S, B> andThen(Affine<A, B> other) {
        Lens<S, A> self = this;
        return Affine.of(
                source -> other.getMaybe(self.get(source)),
                (source, value) -> self.set(other.set(value, self.get(source)), source));
    }

    default <B> Traversal<S, B> andThen(Traversal<A, B> other) {
        Lens<S, A> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<B, App<F, B>> f, S source, Applicative<F> applicative) {
                return applicative.map(
                        next -> self.set(next, source), other.modifyF(f, self.get(source), applicative));
            }

            @Override
            public S modify(Function<? super B, ? extends B> f, S source) {
                return self.set(other.modify(f, self.get(source)), source);
            }

            @Override
            public List<B> getAll(S source) {
                return other.getAll(self.get(source));
            }

            @Override
            public Maybe<B> preview(S source) {
                return other.preview(self.get(source));
            }

            @Override
            public int length(S source) {
                return other.length(self.get(source));
            }

            @Override
            public boolean exists(Predicate<? super B> predicate, S source) {
                return other.exists(predicate, self.get(source));
            }

            @Override
            public boolean all(Predicate<? super B> predicate, S source) {
                return other.all(predicate, self.get(source));
            }
        };
    }

    default S setIf(Predicate<? super A> predicate, A value, S source) {
        return predicate.test(value) ? set(value, source) : source;
    }

    default S setWhen(Predicate<? super A> predicate, A value, S source) {
        A current = get(source);
        return predicate.test(current) ? set(value, source) : source;
    }

    default S modifyWhen(Predicate<? super A> predicate, Function<? super A, ? extends A> f, S source) {
        A current = get(source);
        return predicate.test(current) ? set(f.apply(current), source) : source;
    }

    default S modifyBranch(
            Predicate<? super A> predicate,
            Function<? super A, ? extends A> thenModifier,
            Function<? super A, ? extends A> elseModifier,
            S source) {
        A current = get(source);
        return predicate.test(current)
                ? set(thenModifier.apply(current), source)
                : set(elseModifier.apply(current), source);
    }

    default <F extends K1> App<F, S> setIf(
            Predicate<? super A> predicate,
            A value,
            S source,
            Selective<F> selective) {
        Objects.requireNonNull(selective, "selective");
        return selective.ifS(
                selective.of(predicate.test(value)),
                () -> selective.of(set(value, source)),
                () -> selective.of(source));
    }

    default <F extends K1> App<F, S> setWhen(
            Predicate<? super A> predicate,
            A value,
            S source,
            Selective<F> selective) {
        Objects.requireNonNull(selective, "selective");
        A current = get(source);
        return selective.ifS(
                selective.of(predicate.test(current)),
                () -> selective.of(set(value, source)),
                () -> selective.of(source));
    }

    default <F extends K1> App<F, S> modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends App<F, A>> f,
            S source,
            Selective<F> selective) {
        Objects.requireNonNull(selective, "selective");
        A current = get(source);
        return selective.ifS(
                selective.of(predicate.test(current)),
                () -> selective.map(next -> set(next, source), Objects.requireNonNull(f.apply(current), "modify result")),
                () -> selective.of(source));
    }

    default <F extends K1> App<F, S> branch(
            Predicate<? super A> predicate,
            Function<? super A, ? extends App<F, A>> thenBranch,
            Function<? super A, ? extends App<F, A>> elseBranch,
            S source,
            Selective<F> selective) {
        return modifyBranch(predicate, thenBranch, elseBranch, source, selective);
    }

    default <F extends K1> App<F, S> modifyBranch(
            Predicate<? super A> predicate,
            Function<? super A, ? extends App<F, A>> thenModifier,
            Function<? super A, ? extends App<F, A>> elseModifier,
            S source,
            Selective<F> selective) {
        Objects.requireNonNull(selective, "selective");
        A current = get(source);
        return selective.ifS(
                selective.of(predicate.test(current)),
                () -> selective.map(
                        next -> set(next, source),
                        Objects.requireNonNull(thenModifier.apply(current), "then modifier result")),
                () -> selective.map(
                        next -> set(next, source),
                        Objects.requireNonNull(elseModifier.apply(current), "else modifier result")));
    }

    static <S, A> Lens<S, A> of(Function<? super S, ? extends A> getter, BiFunction<S, A, S> setter) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(setter, "setter");
        return new Lens<>() {
            @Override
            public A get(S source) {
                return getter.apply(source);
            }

            @Override
            public S set(A value, S source) {
                return setter.apply(source, value);
            }

            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f, S source, Functor<F> functor) {
                return functor.map(value -> set(value, source), f.apply(get(source)));
            }
        };
    }

    static <S, A, B> Lens<S, Pair<A, B>> paired(
            Lens<S, A> first, Lens<S, B> second, Function3<S, A, B, S> rebuild) {
        return Lens.of(
                source -> Pair.of(first.get(source), second.get(source)),
                (source, pair) -> rebuild.apply(source, pair.first(), pair.second()));
    }

    static <S, A, B> Lens<S, Pair<A, B>> paired(
            Lens<S, A> first, Lens<S, B> second, BiFunction<A, B, S> constructor) {
        return paired(first, second, (source, a, b) -> constructor.apply(a, b));
    }
}
