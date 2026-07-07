package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.MonadError;
import com.flechazo.hkt.MonadZero;
import com.flechazo.hkt.Selective;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public enum VStreamAlternative implements MonadError<VStream.Mu, Throwable, VStream.InstanceMu>,
        MonadZero<VStream.Mu, VStream.InstanceMu>,
        Selective<VStream.Mu, VStream.InstanceMu> {
    INSTANCE;

    @Override
    public <A> App<VStream.Mu, A> of(A value) {
        return VStream.of(value);
    }

    @Override
    public <A, B> App<VStream.Mu, B> flatMap(
            Function<? super A, ? extends App<VStream.Mu, B>> f,
            App<VStream.Mu, A> fa) {
        return VStreamMonad.INSTANCE.flatMap(f, fa);
    }

    @Override
    public <A> App<VStream.Mu, A> zero() {
        return VStream.empty();
    }

    @Override
    public <A> App<VStream.Mu, A> orElse(
            App<VStream.Mu, A> first,
            Supplier<? extends App<VStream.Mu, A>> second) {
        Objects.requireNonNull(second, "second");
        return VStream.concat(VStream.unbox(first), VStream.defer(() -> VStream.unbox(second.get())));
    }

    @Override
    public <A> App<VStream.Mu, A> raiseError(Throwable error) {
        return VStream.fail(error);
    }

    @Override
    public <A> App<VStream.Mu, A> handleErrorWith(
            App<VStream.Mu, A> value,
            Function<? super Throwable, ? extends App<VStream.Mu, A>> handler) {
        Objects.requireNonNull(handler, "handler");
        return VStream.unbox(value).recoverWith(error ->
                VStream.unbox(Objects.requireNonNull(handler.apply(error), "handler result")));
    }

    @Override
    public <A, B> App<VStream.Mu, B> select(
            App<VStream.Mu, Either<A, B>> value,
            App<VStream.Mu, ? extends Function<A, B>> function) {
        VStream<? extends Function<A, B>> functions = VStream.unbox(function);
        return VStream.unbox(value).flatMap(choice -> {
            Either<A, B> either = Objects.requireNonNull(choice, "select value");
            if (either.isRight()) {
                return VStream.of(either.right());
            }
            return functions.map(fn -> Objects.requireNonNull(fn, "select function").apply(either.left()));
        });
    }

    @Override
    public <A> App<VStream.Mu, A> ifS(
            App<VStream.Mu, Boolean> condition,
            Supplier<? extends App<VStream.Mu, A>> thenValue,
            Supplier<? extends App<VStream.Mu, A>> elseValue) {
        Objects.requireNonNull(thenValue, "thenValue");
        Objects.requireNonNull(elseValue, "elseValue");
        return VStream.unbox(condition).flatMap(test ->
                VStream.unbox(Boolean.TRUE.equals(test) ? thenValue.get() : elseValue.get()));
    }
}
