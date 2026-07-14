package com.flechazo.hkt.tuple;

import com.flechazo.hkt.*;
import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.flechazo.hkt.util.validation.Operation.*;

public record Tuple2<A, B>(A first, B second)
        implements App2<Tuple2.Mu, A, B>, App {
    public Tuple2 {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
    }

    public static final class Mu implements K2 {
        private Mu() {
        }
    }

    public static final class WriterMu<A> implements K1 {
        private WriterMu() {
        }
    }

    public static final class FirstMu<B> implements K1 {
        private FirstMu() {
        }
    }

    public static <A, B> Tuple2<A, B> of(A first, B second) {
        return new Tuple2<>(first, second);
    }

    public static <B> Traversable<FirstMu<B>, Tuple2FirstInstance.Mu> instance() {
        return new Tuple2FirstInstance<>();
    }

    public static <A, B> Tuple2<A, B> unbox(App<WriterMu<A>, B> value) {
        return (Tuple2<A, B>) Validation.kind().narrowWithTypeCheck(value, Tuple2.class);
    }

    public static <A, B> Tuple2<A, B> unboxFirst(App<FirstMu<B>, A> value) {
        return (Tuple2<A, B>) Validation.kind().narrowWithTypeCheck(value, Tuple2.class);
    }

    public static <A, B> Tuple2<A, B> unbox(App2<Mu, A, B> value) {
        return (Tuple2<A, B>) Validation.kind().narrowWithTypeCheck2(value, Tuple2.class);
    }

    /**
     * Returns the writer-style applicative for {@code Tuple2<A, ?>}.
     *
     * <p>The first component is interpreted as a monoidal writer log.
     */
    public static <A> Applicative<WriterMu<A>, Tuple2Monad.Mu> applicative(Monoid<A> monoid) {
        return new Tuple2Monad<>(monoid);
    }

    /**
     * Returns the writer-style monad for {@code Tuple2<A, ?>}.
     *
     * <p>The first component is a non-null monoidal writer log in this interpretation.
     */
    public static <A> Monad<WriterMu<A>, Tuple2Monad.Mu> monad(Monoid<A> monoid) {
        return new Tuple2Monad<>(monoid);
    }

    /**
     * Returns the writer-style selective instance for {@code Tuple2<A, ?>}.
     *
     * <p>The first component is a non-null monoidal writer log in this interpretation.
     */
    public static <A> Selective<WriterMu<A>, Tuple2Monad.Mu> selective(Monoid<A> monoid) {
        return new Tuple2Monad<>(monoid);
    }

    public <C> Tuple2<C, B> mapFirst(Function<? super A, ? extends C> f) {
        return new Tuple2<>(f.apply(first), second);
    }

    public <C> Tuple2<A, C> mapSecond(Function<? super B, ? extends C> f) {
        return new Tuple2<>(first, f.apply(second));
    }

    public <C, D> Tuple2<C, D> mapBoth(
            Function<? super A, ? extends C> firstMapper,
            Function<? super B, ? extends D> secondMapper) {
        return new Tuple2<>(firstMapper.apply(first), secondMapper.apply(second));
    }

    public <C> C fold(BiFunction<? super A, ? super B, ? extends C> f) {
        return f.apply(first, second);
    }

    public Tuple2<B, A> swap() {
        return new Tuple2<>(second, first);
    }

    private static <A> A requireWriterLog(A value, String name) {
        return Objects.requireNonNull(value, name);
    }

    public static final class Tuple2Monad<A>
            implements Monad<WriterMu<A>, Tuple2Monad.Mu>, Selective<WriterMu<A>, Tuple2Monad.Mu> {
        private final Monoid<A> monoid;

        public static final class Mu implements Applicative.Mu {
            private Mu() {
            }
        }

        private Tuple2Monad(Monoid<A> monoid) {
            this.monoid = Objects.requireNonNull(monoid, "monoid");
        }

        @Override
        public <B> App<WriterMu<A>, B> of(B value) {
            return Tuple2.of(requireWriterLog(monoid.empty(), "writer log"), value);
        }

        @Override
        public <B, C> App<WriterMu<A>, C> flatMap(
                Function<? super B, ? extends App<WriterMu<A>, C>> f,
                App<WriterMu<A>, B> fa) {
            Tuple2<A, B> tuple = Tuple2.unbox(fa);
            Validation.function().validateFlatMap(f, fa);
            A log = requireWriterLog(tuple.first(), "writer log");
            Tuple2<A, C> next = Tuple2.unbox(Validation.function()
                    .requireNonNullResult(f.apply(tuple.second()), "f", FLAT_MAP));
            A nextLog = requireWriterLog(next.first(), "next writer log");
            return Tuple2.of(monoid.combine(log, nextLog), next.second());
        }

        @Override
        public <B, C> App<WriterMu<A>, C> select(
                App<WriterMu<A>, Either<B, C>> value,
                App<WriterMu<A>, ? extends Function<B, C>> function) {
            Tuple2<A, Either<B, C>> tuple = Tuple2.unbox(value);
            Either<B, C> either = Validation.coreType().requireValue(tuple.second(), "select value", Tuple2.class, SELECT);
            A log = requireWriterLog(tuple.first(), "writer log");
            if (either.isRight()) {
                return Tuple2.of(log, either.right());
            }
            Tuple2<A, ? extends Function<B, C>> fn = Tuple2.unbox(function);
            A functionLog = requireWriterLog(fn.first(), "function writer log");
            Function<B, C> apply = Validation.coreType().requireValue(fn.second(), "select function", Tuple2.class, SELECT);
            return Tuple2.of(monoid.combine(log, functionLog), apply.apply(either.left()));
        }

        @Override
        public <B> App<WriterMu<A>, B> ifS(
                App<WriterMu<A>, Boolean> condition,
                Supplier<? extends App<WriterMu<A>, B>> thenValue,
                Supplier<? extends App<WriterMu<A>, B>> elseValue) {
            Tuple2<A, Boolean> test = Tuple2.unbox(condition);
            A log = requireWriterLog(test.first(), "writer log");
            Supplier<? extends App<WriterMu<A>, B>> branch =
                    Boolean.TRUE.equals(test.second()) ? thenValue : elseValue;
            Tuple2<A, B> selected = Tuple2.unbox(Validation.function().requireNonNullResult(branch.get(), "branch", IF_S));
            A selectedLog = requireWriterLog(selected.first(), "selected writer log");
            return Tuple2.of(monoid.combine(log, selectedLog), selected.second());
        }
    }

    public static final class Tuple2FirstInstance<B> implements Traversable<FirstMu<B>, Tuple2FirstInstance.Mu> {
        public static final class Mu implements Traversable.Mu {
            private Mu() {
            }
        }

        @Override
        public <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<FirstMu<B>, A> value) {
            Validation.function().validateFoldMap(monoid, f, value);
            return f.apply(Tuple2.unboxFirst(value).first());
        }

        @Override
        public <F extends K1, A, C> App<F, App<FirstMu<B>, C>> traverse(
                Applicative<F, ?> applicative,
                Function<? super A, ? extends App<F, C>> f,
                App<FirstMu<B>, A> value) {
            Tuple2<A, B> tuple = Tuple2.unboxFirst(value);
            Validation.function().validateTraverse(applicative, f, value);
            return applicative.map(
                    mapped -> Tuple2.of(mapped, tuple.second()),
                    Validation.function().requireNonNullResult(f.apply(tuple.first()), "f", TRAVERSE));
        }
    }
}
