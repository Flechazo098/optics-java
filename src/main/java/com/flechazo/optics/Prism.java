package com.flechazo.optics;

import com.flechazo.hkt.*;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Prism<S, A> extends Optic<S, S, A, A> {
    Maybe<A> getMaybe(S source);

    S build(A value);

    @Override
    default <F extends K1> App<F, S> modifyF(
            Function<A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
        Maybe<A> value = getMaybe(source);
        return value.isDefined()
                ? applicative.map(this::build, f.apply(value.get()))
                : applicative.of(source);
    }

    default S modify(Function<? super A, ? extends A> f, S source) {
        Maybe<A> value = getMaybe(source);
        return value.isDefined() ? build(f.apply(value.get())) : source;
    }

    default S set(A value, S source) {
        return modify(ignored -> value, source);
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

    default <B> Maybe<B> mapMaybe(Function<? super A, ? extends B> f, S source) {
        return getMaybe(source).map(f);
    }

    default S modifyWhen(Predicate<? super A> predicate, Function<? super A, ? extends A> f, S source) {
        return getMaybe(source)
                .filter(predicate)
                .map(value -> build(f.apply(value)))
                .orElse(source);
    }

    default S setWhen(Predicate<? super A> predicate, A value, S source) {
        return modifyWhen(predicate, ignored -> value, source);
    }

    default S modifyBranch(
            Predicate<? super A> predicate,
            Function<? super A, ? extends A> thenModifier,
            Function<? super A, ? extends A> elseModifier,
            S source) {
        return getMaybe(source)
                .map(value -> predicate.test(value)
                        ? build(thenModifier.apply(value))
                        : build(elseModifier.apply(value)))
                .orElse(source);
    }

    default <F extends K1> App<F, S> setWhen(
            Predicate<? super A> predicate,
            A value,
            S source,
            Selective<F, ?> selective) {
        return modifyWhen(predicate, ignored -> selective.of(value), source, selective);
    }

    default <F extends K1> App<F, S> modifyWhen(
            Predicate<? super A> predicate,
            Function<? super A, ? extends App<F, A>> f,
            S source,
            Selective<F, ?> selective) {
        Objects.requireNonNull(selective, "selective");
        Maybe<A> current = getMaybe(source);
        if (current.isEmpty()) {
            return selective.of(source);
        }
        A value = current.get();
        return selective.ifS(
                selective.of(predicate.test(value)),
                () -> selective.map(this::build, Objects.requireNonNull(f.apply(value), "modify result")),
                () -> selective.of(source));
    }

    default <F extends K1> App<F, S> branch(
            Predicate<? super A> predicate,
            Function<? super A, ? extends App<F, A>> thenBranch,
            Function<? super A, ? extends App<F, A>> elseBranch,
            S source,
            Selective<F, ?> selective) {
        return modifyBranch(predicate, thenBranch, elseBranch, source, selective);
    }

    default <F extends K1> App<F, S> modifyBranch(
            Predicate<? super A> predicate,
            Function<? super A, ? extends App<F, A>> thenModifier,
            Function<? super A, ? extends App<F, A>> elseModifier,
            S source,
            Selective<F, ?> selective) {
        Objects.requireNonNull(selective, "selective");
        Maybe<A> current = getMaybe(source);
        if (current.isEmpty()) {
            return selective.of(source);
        }
        A value = current.get();
        return selective.ifS(
                selective.of(predicate.test(value)),
                () -> selective.map(
                        this::build,
                        Objects.requireNonNull(thenModifier.apply(value), "then modifier result")),
                () -> selective.map(
                        this::build,
                        Objects.requireNonNull(elseModifier.apply(value), "else modifier result")));
    }

    default Traversal<S, A> asTraversal() {
        return Prism.this::modifyF;
    }

    default Setter<S, A> asSetter() {
        Prism<S, A> self = this;
        return new Setter<>() {
            @Override
            public S modify(Function<? super A, ? extends A> f, S source) {
                return self.modify(f, source);
            }

            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
                return self.modifyF(f, source, applicative);
            }
        };
    }

    default Fold<S, A> asFold() {
        Prism<S, A> self = this;
        return new Fold<>() {
            @Override
            public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
                Maybe<A> value = self.getMaybe(source);
                return value.isDefined() ? f.apply(value.get()) : monoid.empty();
            }
        };
    }

    default <B> Prism<S, B> andThen(Prism<A, B> other) {
        return Prism.of(source -> getMaybe(source).flatMap(other::getMaybe), value -> build(other.build(value)));
    }

    default <B> Fold<S, B> andThen(Fold<A, B> other) {
        return asFold().andThen(other);
    }

    default <B> Affine<S, B> andThen(Lens<A, B> other) {
        Prism<S, A> self = this;
        return Affine.of(
                source -> self.getMaybe(source).map(other::get),
                (source, value) -> self.getMaybe(source).map(a -> self.build(other.set(value, a))).orElse(source));
    }

    default <B> Prism<S, B> andThen(Iso<A, B> other) {
        return Prism.of(
                source -> getMaybe(source).map(other::get),
                value -> build(other.reverseGet(value)));
    }

    default <B> Affine<S, B> andThen(Affine<A, B> other) {
        Prism<S, A> self = this;
        return Affine.of(
                source -> self.getMaybe(source).flatMap(other::getMaybe),
                (source, value) -> self.getMaybe(source).map(a -> self.build(other.set(value, a))).orElse(source));
    }

    default <B> Traversal<S, B> andThen(Traversal<A, B> other) {
        Prism<S, A> self = this;
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, S> modifyF(
                    Function<B, App<F, B>> f, S source, Applicative<F, ?> applicative) {
                return self.getMaybe(source)
                        .map(value -> applicative.map(self::build, other.modifyF(f, value, applicative)))
                        .orElseGet(() -> applicative.of(source));
            }
        };
    }

    default Prism<S, A> filtered(Predicate<? super A> predicate) {
        Prism<S, A> self = this;
        return Prism.of(source -> self.getMaybe(source).filter(predicate), self::build);
    }

    default Prism<S, A> orElse(Prism<S, A> other) {
        Prism<S, A> self = this;
        return Prism.of(source -> self.getMaybe(source).or(() -> other.getMaybe(source)), self::build);
    }

    static <S, A> Prism<S, A> of(
            Function<? super S, Maybe<A>> preview, Function<? super A, ? extends S> build) {
        return new Prism<>() {
            @Override
            public Maybe<A> getMaybe(S source) {
                return preview.apply(source);
            }

            @Override
            public S build(A value) {
                return build.apply(value);
            }
        };
    }
}
