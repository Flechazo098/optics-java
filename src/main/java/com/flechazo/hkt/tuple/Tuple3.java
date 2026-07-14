package com.flechazo.hkt.tuple;

import com.flechazo.hkt.*;
import com.flechazo.hkt.util.validation.Validation;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.flechazo.hkt.util.validation.Operation.*;

public record Tuple3<A, B, C>(A first, B second, C third)
        implements App2<Tuple3.Mu<C>, A, B>, App<Tuple3.WriterMu<A, B>, C> {
    public Tuple3 {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(third, "third");
    }

    public static final class Mu<C> implements K2 {
        private Mu() {
        }
    }

    public static final class WriterMu<A, B> implements K1 {
        private WriterMu() {
        }
    }

    public static <A, B, C> Tuple3<A, B, C> of(A first, B second, C third) {
        return new Tuple3<>(first, second, third);
    }

    public static <A, B, C> Tuple3<A, B, C> unbox(App<WriterMu<A, B>, C> value) {
        return (Tuple3<A, B, C>) Validation.kind().narrowWithTypeCheck(value, Tuple3.class);
    }

    public static <A, B, C> Tuple3<A, B, C> unbox(App2<Mu<C>, A, B> value) {
        return (Tuple3<A, B, C>) Validation.kind().narrowWithTypeCheck2(value, Tuple3.class);
    }

    public static <A, B> Applicative<WriterMu<A, B>, Tuple3Monad.Mu> applicative(
            Monoid<A> firstMonoid,
            Monoid<B> secondMonoid) {
        return new Tuple3Monad<>(firstMonoid, secondMonoid);
    }

    public static <A, B> Monad<WriterMu<A, B>, Tuple3Monad.Mu> monad(
            Monoid<A> firstMonoid,
            Monoid<B> secondMonoid) {
        return new Tuple3Monad<>(firstMonoid, secondMonoid);
    }

    public static <A, B> Selective<WriterMu<A, B>, Tuple3Monad.Mu> selective(
            Monoid<A> firstMonoid,
            Monoid<B> secondMonoid) {
        return new Tuple3Monad<>(firstMonoid, secondMonoid);
    }

    public <D> Tuple3<D, B, C> mapFirst(Function<? super A, ? extends D> mapper) {
        return new Tuple3<>(mapper.apply(first), second, third);
    }

    public <D> Tuple3<A, D, C> mapSecond(Function<? super B, ? extends D> mapper) {
        return new Tuple3<>(first, mapper.apply(second), third);
    }

    public <D> Tuple3<A, B, D> mapThird(Function<? super C, ? extends D> mapper) {
        return new Tuple3<>(first, second, mapper.apply(third));
    }

    private static <A> A requireWriterLog(A value, String name) {
        return Objects.requireNonNull(value, name);
    }

    public static final class Tuple3Monad<A, B>
            implements Monad<WriterMu<A, B>, Tuple3Monad.Mu>, Selective<WriterMu<A, B>, Tuple3Monad.Mu> {
        private final Monoid<A> firstMonoid;
        private final Monoid<B> secondMonoid;

        public static final class Mu implements Applicative.Mu {
            private Mu() {
            }
        }

        private Tuple3Monad(Monoid<A> firstMonoid, Monoid<B> secondMonoid) {
            this.firstMonoid = Objects.requireNonNull(firstMonoid, "firstMonoid");
            this.secondMonoid = Objects.requireNonNull(secondMonoid, "secondMonoid");
        }

        @Override
        public <C> App<WriterMu<A, B>, C> of(C value) {
            return Tuple3.of(
                    requireWriterLog(firstMonoid.empty(), "first writer log"),
                    requireWriterLog(secondMonoid.empty(), "second writer log"),
                    value);
        }

        @Override
        public <C, D> App<WriterMu<A, B>, D> flatMap(
                Function<? super C, ? extends App<WriterMu<A, B>, D>> f,
                App<WriterMu<A, B>, C> fa) {
            Tuple3<A, B, C> tuple = Tuple3.unbox(fa);
            Validation.function().validateFlatMap(f, fa);
            A firstLog = requireWriterLog(tuple.first(), "first writer log");
            B secondLog = requireWriterLog(tuple.second(), "second writer log");
            Tuple3<A, B, D> next = Tuple3.unbox(Validation.function()
                    .requireNonNullResult(f.apply(tuple.third()), "f", FLAT_MAP));
            return Tuple3.of(
                    firstMonoid.combine(firstLog, requireWriterLog(next.first(), "next first writer log")),
                    secondMonoid.combine(secondLog, requireWriterLog(next.second(), "next second writer log")),
                    next.third());
        }

        @Override
        public <C, D> App<WriterMu<A, B>, D> select(
                App<WriterMu<A, B>, Either<C, D>> value,
                App<WriterMu<A, B>, ? extends Function<C, D>> function) {
            Tuple3<A, B, Either<C, D>> tuple = Tuple3.unbox(value);
            A firstLog = requireWriterLog(tuple.first(), "first writer log");
            B secondLog = requireWriterLog(tuple.second(), "second writer log");
            Either<C, D> either = Validation.coreType().requireValue(tuple.third(), "select value", Tuple3.class, SELECT);
            if (either.isRight()) {
                return Tuple3.of(firstLog, secondLog, either.right());
            }
            Tuple3<A, B, ? extends Function<C, D>> fn = Tuple3.unbox(function);
            Function<C, D> apply = Validation.coreType().requireValue(fn.third(), "select function", Tuple3.class, SELECT);
            return Tuple3.of(
                    firstMonoid.combine(firstLog, requireWriterLog(fn.first(), "function first writer log")),
                    secondMonoid.combine(secondLog, requireWriterLog(fn.second(), "function second writer log")),
                    apply.apply(either.left()));
        }

        @Override
        public <C> App<WriterMu<A, B>, C> ifS(
                App<WriterMu<A, B>, Boolean> condition,
                Supplier<? extends App<WriterMu<A, B>, C>> thenValue,
                Supplier<? extends App<WriterMu<A, B>, C>> elseValue) {
            Tuple3<A, B, Boolean> test = Tuple3.unbox(condition);
            A firstLog = requireWriterLog(test.first(), "first writer log");
            B secondLog = requireWriterLog(test.second(), "second writer log");
            Supplier<? extends App<WriterMu<A, B>, C>> branch =
                    Boolean.TRUE.equals(test.third()) ? thenValue : elseValue;
            Tuple3<A, B, C> selected = Tuple3.unbox(Validation.function().requireNonNullResult(branch.get(), "branch", IF_S));
            return Tuple3.of(
                    firstMonoid.combine(firstLog, requireWriterLog(selected.first(), "selected first writer log")),
                    secondMonoid.combine(secondLog, requireWriterLog(selected.second(), "selected second writer log")),
                    selected.third());
        }
    }
}
