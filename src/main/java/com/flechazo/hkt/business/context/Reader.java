package com.flechazo.hkt.business.context;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Monad;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.util.validation.Validation;

import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface Reader<R, A> extends App<Reader.Mu<R>, A> {
    final class Mu<R> implements K1 {
        private Mu() {
        }
    }

    final class InstanceMu implements Applicative.Mu {
        private InstanceMu() {
        }
    }

    static <R, A> Reader<R, A> of(Function<? super R, ? extends A> run) {
        return run::apply;
    }

    static <R, A> Reader<R, A> constant(A value) {
        return ignored -> value;
    }

    static <R> Reader<R, R> ask() {
        return environment -> environment;
    }

    static <R, A> Reader<R, A> unbox(App<Mu<R>, A> value) {
        return (Reader<R, A>) Validation.kind().narrowWithTypeCheck(value, Reader.class);
    }

    static <R> Applicative<Reader.Mu<R>, InstanceMu> applicative() {
        return Instance.instance();
    }

    static <R> Monad<Reader.Mu<R>, InstanceMu> monad() {
        return Instance.instance();
    }

    static <R> Selective<Reader.Mu<R>, InstanceMu> selective() {
        return Instance.instance();
    }

    A run(R environment);

    default <B> Reader<R, B> map(Function<? super A, ? extends B> mapper) {
        return environment -> mapper.apply(run(environment));
    }

    default <B> Reader<R, B> flatMap(Function<? super A, ? extends Reader<R, B>> mapper) {
        return environment -> mapper.apply(run(environment)).run(environment);
    }

    default Reader<R, Unit> asUnit() {
        return map(ignored -> Unit.INSTANCE);
    }

    final class Instance<R> implements Monad<Reader.Mu<R>, InstanceMu>, Selective<Reader.Mu<R>, InstanceMu> {
        private static final Instance<?> INSTANCE = new Instance<>();

        private Instance() {
        }

        @SuppressWarnings("unchecked")
        static <R> Instance<R> instance() {
            return (Instance<R>) INSTANCE;
        }

        @Override
        public <A> App<Reader.Mu<R>, A> of(A value) {
            return Reader.constant(value);
        }

        @Override
        public <A, B> App<Reader.Mu<R>, B> flatMap(
                Function<? super A, ? extends App<Reader.Mu<R>, B>> f,
                App<Reader.Mu<R>, A> fa) {
            return Reader.unbox(fa).flatMap(value -> Reader.unbox(f.apply(value)));
        }

        @Override
        public <A, B> App<Reader.Mu<R>, B> select(
                App<Reader.Mu<R>, Either<A, B>> value,
                App<Reader.Mu<R>, ? extends Function<A, B>> function) {
            Reader<R, B> result = environment -> {
                Either<A, B> either = Reader.unbox(value).run(environment);
                return either.isRight()
                        ? either.right()
                        : Reader.unbox(function).run(environment).apply(either.left());
            };
            return result;
        }

        @Override
        public <A> App<Reader.Mu<R>, A> ifS(
                App<Reader.Mu<R>, Boolean> condition,
                Supplier<? extends App<Reader.Mu<R>, A>> thenValue,
                Supplier<? extends App<Reader.Mu<R>, A>> elseValue) {
            Reader<R, A> result = environment -> {
                Supplier<? extends App<Reader.Mu<R>, A>> branch =
                        Boolean.TRUE.equals(Reader.unbox(condition).run(environment)) ? thenValue : elseValue;
                return Reader.unbox(branch.get()).run(environment);
            };
            return result;
        }
    }
}
