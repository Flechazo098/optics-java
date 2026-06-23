package com.flechazo.hkt;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public record Tuple2<A, B>(@Nullable A first, @Nullable B second) implements App2<Tuple2.Mu, A, B> {
    public static final class Mu implements K2 {
        private Mu() {
        }
    }

    public static <A, B> Tuple2<A, B> of(@Nullable A first, @Nullable B second) {
        return new Tuple2<>(first, second);
    }

    public static <A, B> Tuple2<A, B> unbox(App<App2.Mu<Mu, A>, B> value) {
        return (Tuple2<A, B>) Objects.requireNonNull(value, "value");
    }

    public static <A, B> Tuple2<A, B> unbox(App2<Mu, A, B> value) {
        return (Tuple2<A, B>) Objects.requireNonNull(value, "value");
    }

    /**
     * Returns the writer-style applicative for {@code Tuple2<A, ?>}.
     *
     * <p>{@code Tuple2} itself may carry nullable payloads, but in this instance the first component
     * is interpreted as a monoidal writer log. Logs produced or consumed by this instance must be
     * non-null {@code Monoid<A>} values.
     */
    public static <A> Applicative<App2.Mu<Mu, A>> applicative(Monoid<A> monoid) {
        return new Tuple2Monad<>(monoid);
    }

    /**
     * Returns the writer-style monad for {@code Tuple2<A, ?>}.
     *
     * <p>The first component is a non-null monoidal writer log in this interpretation.
     */
    public static <A> Monad<App2.Mu<Mu, A>> monad(Monoid<A> monoid) {
        return new Tuple2Monad<>(monoid);
    }

    /**
     * Returns the writer-style selective instance for {@code Tuple2<A, ?>}.
     *
     * <p>The first component is a non-null monoidal writer log in this interpretation.
     */
    public static <A> Selective<App2.Mu<Mu, A>> selective(Monoid<A> monoid) {
        return new Tuple2Monad<>(monoid);
    }

    public <C> Tuple2<C, B> mapFirst(Function<? super A, ? extends C> f) {
        return new Tuple2<>(f.apply(first), second);
    }

    public <C> Tuple2<A, C> mapSecond(Function<? super B, ? extends C> f) {
        return new Tuple2<>(first, f.apply(second));
    }

    public Tuple2<B, A> swap() {
        return new Tuple2<>(second, first);
    }

    private static <A> A requireWriterLog(@Nullable A value, String name) {
        return Objects.requireNonNull(value, name);
    }

    private record Tuple2Monad<A>(Monoid<A> monoid)
            implements Monad<App2.Mu<Mu, A>>, Selective<App2.Mu<Mu, A>> {
        private Tuple2Monad(Monoid<A> monoid) {
            this.monoid = Objects.requireNonNull(monoid, "monoid");
        }

        @Override
        public <B> App<App2.Mu<Mu, A>, B> of(@Nullable B value) {
            return Tuple2.of(requireWriterLog(monoid.empty(), "writer log"), value);
        }

        @Override
        public <B, C> App<App2.Mu<Mu, A>, C> flatMap(
                Function<? super B, ? extends App<App2.Mu<Mu, A>, C>> f,
                App<App2.Mu<Mu, A>, B> fa) {
            Tuple2<A, B> tuple = Tuple2.unbox(fa);
            A log = requireWriterLog(tuple.first(), "writer log");
            Tuple2<A, C> next = Tuple2.unbox(Objects.requireNonNull(f.apply(tuple.second()), "flatMap result"));
            A nextLog = requireWriterLog(next.first(), "next writer log");
            return Tuple2.of(monoid.combine(log, nextLog), next.second());
        }

        @Override
        public <B, C> App<App2.Mu<Mu, A>, C> select(
                App<App2.Mu<Mu, A>, Either<B, C>> value,
                App<App2.Mu<Mu, A>, ? extends Function<B, C>> function) {
            Tuple2<A, Either<B, C>> tuple = Tuple2.unbox(value);
            Either<B, C> either = Objects.requireNonNull(tuple.second(), "select value");
            A log = requireWriterLog(tuple.first(), "writer log");
            if (either.isRight()) {
                return Tuple2.of(log, either.right());
            }
            Tuple2<A, ? extends Function<B, C>> fn = Tuple2.unbox(function);
            A functionLog = requireWriterLog(fn.first(), "function writer log");
            Function<B, C> apply = Objects.requireNonNull(fn.second(), "select function");
            return Tuple2.of(monoid.combine(log, functionLog), apply.apply(either.left()));
        }

        @Override
        public <B> App<App2.Mu<Mu, A>, B> ifS(
                App<App2.Mu<Mu, A>, Boolean> condition,
                Supplier<? extends App<App2.Mu<Mu, A>, B>> thenValue,
                Supplier<? extends App<App2.Mu<Mu, A>, B>> elseValue) {
            Tuple2<A, Boolean> test = Tuple2.unbox(condition);
            A log = requireWriterLog(test.first(), "writer log");
            Supplier<? extends App<App2.Mu<Mu, A>, B>> branch =
                    Boolean.TRUE.equals(test.second()) ? thenValue : elseValue;
            Tuple2<A, B> selected = Tuple2.unbox(Objects.requireNonNull(branch.get(), "ifS branch result"));
            A selectedLog = requireWriterLog(selected.first(), "selected writer log");
            return Tuple2.of(monoid.combine(log, selectedLog), selected.second());
        }
    }
}
