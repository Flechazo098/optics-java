package com.flechazo.hkt.business.context;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Monad;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.Unit;

import java.util.function.Function;
import java.util.function.Supplier;

public record Writer<W, A>(W written, A value) implements App<Writer.Mu<W>, A> {
    public static final class Mu<W> implements K1 {
        private Mu() {
        }
    }

    public static final class InstanceMu implements Applicative.Mu {
        private InstanceMu() {
        }
    }

    public static <W, A> Writer<W, A> value(Monoid<W> monoid, A value) {
        return new Writer<>(monoid.empty(), value);
    }

    public static <W, A> Writer<W, A> of(W written, A value) {
        return new Writer<>(written, value);
    }

    public static <W> Writer<W, Unit> tell(W written) {
        return new Writer<>(written, Unit.INSTANCE);
    }

    public static <W, A> Writer<W, A> unbox(App<Mu<W>, A> value) {
        return (Writer<W, A>) value;
    }

    public static <W> Applicative<Writer.Mu<W>, InstanceMu> applicative(Monoid<W> monoid) {
        return new Instance<>(monoid);
    }

    public static <W> Monad<Writer.Mu<W>, InstanceMu> monad(Monoid<W> monoid) {
        return new Instance<>(monoid);
    }

    public static <W> Selective<Writer.Mu<W>, InstanceMu> selective(Monoid<W> monoid) {
        return new Instance<>(monoid);
    }

    public A run() {
        return value;
    }

    public W exec() {
        return written;
    }

    public <B> Writer<W, B> map(Function<? super A, ? extends B> mapper) {
        return new Writer<>(written, mapper.apply(value));
    }

    public <W2> Writer<W2, A> mapWritten(Function<? super W, ? extends W2> mapper) {
        return new Writer<>(mapper.apply(written), value);
    }

    public <B> Writer<W, B> flatMap(Monoid<W> monoid, Function<? super A, Writer<W, B>> mapper) {
        Writer<W, B> next = mapper.apply(value);
        return new Writer<>(monoid.combine(written, next.written()), next.value());
    }

    private record Instance<W>(Monoid<W> monoid)
            implements Monad<Writer.Mu<W>, InstanceMu>, Selective<Writer.Mu<W>, InstanceMu> {
        @Override
        public <A> App<Writer.Mu<W>, A> of(A value) {
            return Writer.value(monoid, value);
        }

        @Override
        public <A, B> App<Writer.Mu<W>, B> flatMap(
                Function<? super A, ? extends App<Writer.Mu<W>, B>> f,
                App<Writer.Mu<W>, A> fa) {
            return Writer.unbox(fa).flatMap(monoid, value -> Writer.unbox(f.apply(value)));
        }

        @Override
        public <A, B> App<Writer.Mu<W>, B> select(
                App<Writer.Mu<W>, Either<A, B>> value,
                App<Writer.Mu<W>, ? extends Function<A, B>> function) {
            Writer<W, Either<A, B>> selected = Writer.unbox(value);
            if (selected.value().isRight()) {
                return new Writer<>(selected.written(), selected.value().right());
            }
            Writer<W, ? extends Function<A, B>> fn = Writer.unbox(function);
            return new Writer<>(
                    monoid.combine(selected.written(), fn.written()),
                    fn.value().apply(selected.value().left()));
        }

        @Override
        public <A> App<Writer.Mu<W>, A> ifS(
                App<Writer.Mu<W>, Boolean> condition,
                Supplier<? extends App<Writer.Mu<W>, A>> thenValue,
                Supplier<? extends App<Writer.Mu<W>, A>> elseValue) {
            Writer<W, Boolean> test = Writer.unbox(condition);
            Supplier<? extends App<Writer.Mu<W>, A>> branch =
                    Boolean.TRUE.equals(test.value()) ? thenValue : elseValue;
            Writer<W, A> selected = Writer.unbox(branch.get());
            return new Writer<>(monoid.combine(test.written(), selected.written()), selected.value());
        }
    }
}
