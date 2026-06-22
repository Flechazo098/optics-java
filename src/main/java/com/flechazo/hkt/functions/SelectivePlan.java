package com.flechazo.hkt.functions;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Selective;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface SelectivePlan<F extends K1, A> permits
        SelectivePlan.Pure,
        SelectivePlan.Lift,
        SelectivePlan.Select,
        SelectivePlan.IfS,
        SelectivePlan.Branch {
    App<F, A> eval(Selective<F> selective);

    default SelectivePlan<F, A> optimize() {
        return this;
    }

    static <F extends K1, A> SelectivePlan<F, A> pure(A value) {
        return new Pure<>(value);
    }

    static <F extends K1, A> SelectivePlan<F, A> lift(App<F, A> value) {
        return new Lift<>(value);
    }

    static <F extends K1, A, B> SelectivePlan<F, B> select(
            SelectivePlan<F, Either<A, B>> value,
            SelectivePlan<F, ? extends Function<A, B>> function) {
        return new Select<>(value, function);
    }

    static <F extends K1, A> SelectivePlan<F, A> ifS(
            SelectivePlan<F, Boolean> condition,
            Supplier<? extends SelectivePlan<F, A>> thenPlan,
            Supplier<? extends SelectivePlan<F, A>> elsePlan) {
        return new IfS<>(condition, thenPlan, elsePlan);
    }

    static <F extends K1, A, B, C> SelectivePlan<F, C> branch(
            SelectivePlan<F, Either<A, B>> value,
            SelectivePlan<F, ? extends Function<A, C>> leftFunction,
            SelectivePlan<F, ? extends Function<B, C>> rightFunction) {
        return new Branch<>(value, leftFunction, rightFunction);
    }

    record Pure<F extends K1, A>(A value) implements SelectivePlan<F, A> {
        @Override
        public App<F, A> eval(Selective<F> selective) {
            return selective.of(value);
        }
    }

    record Lift<F extends K1, A>(App<F, A> value) implements SelectivePlan<F, A> {
        public Lift {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public App<F, A> eval(Selective<F> selective) {
            return value;
        }
    }

    record Select<F extends K1, A, B>(
            SelectivePlan<F, Either<A, B>> value,
            SelectivePlan<F, ? extends Function<A, B>> function) implements SelectivePlan<F, B> {
        public Select {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(function, "function");
        }

        @Override
        public App<F, B> eval(Selective<F> selective) {
            return selective.select(value.eval(selective), function.eval(selective));
        }

        @Override
        public SelectivePlan<F, B> optimize() {
            SelectivePlan<F, Either<A, B>> optimizedValue = value.optimize();
            SelectivePlan<F, ? extends Function<A, B>> optimizedFunction = function.optimize();
            if (optimizedValue instanceof Pure<?, ?>(Object rawEither)) {
                Either<A, B> either = cast(rawEither);
                if (either != null && either.isRight()) {
                    return new Pure<>(either.right());
                }
                if (either != null && optimizedFunction instanceof Pure<?, ?>(Object rawFunction)) {
                    Function<A, B> fn = cast(rawFunction);
                    return new Pure<>(Objects.requireNonNull(fn, "select function").apply(either.left()));
                }
            }
            return new Select<>(optimizedValue, optimizedFunction);
        }
    }

    record IfS<F extends K1, A>(
            SelectivePlan<F, Boolean> condition,
            Supplier<? extends SelectivePlan<F, A>> thenPlan,
            Supplier<? extends SelectivePlan<F, A>> elsePlan) implements SelectivePlan<F, A> {
        public IfS {
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(thenPlan, "thenPlan");
            Objects.requireNonNull(elsePlan, "elsePlan");
        }

        @Override
        public App<F, A> eval(Selective<F> selective) {
            SelectivePlan<F, Boolean> optimizedCondition = condition.optimize();
            return selective.ifS(
                    optimizedCondition.eval(selective),
                    () -> Objects.requireNonNull(thenPlan.get(), "then plan").optimize().eval(selective),
                    () -> Objects.requireNonNull(elsePlan.get(), "else plan").optimize().eval(selective));
        }

        @Override
        public SelectivePlan<F, A> optimize() {
            SelectivePlan<F, Boolean> optimizedCondition = condition.optimize();
            if (optimizedCondition instanceof Pure<?, ?>(Object value)) {
                return Boolean.TRUE.equals(value)
                        ? Objects.requireNonNull(thenPlan.get(), "then plan").optimize()
                        : Objects.requireNonNull(elsePlan.get(), "else plan").optimize();
            }
            return new IfS<>(
                    optimizedCondition,
                    () -> Objects.requireNonNull(thenPlan.get(), "then plan").optimize(),
                    () -> Objects.requireNonNull(elsePlan.get(), "else plan").optimize());
        }
    }

    record Branch<F extends K1, A, B, C>(
            SelectivePlan<F, Either<A, B>> value,
            SelectivePlan<F, ? extends Function<A, C>> leftFunction,
            SelectivePlan<F, ? extends Function<B, C>> rightFunction) implements SelectivePlan<F, C> {
        public Branch {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(leftFunction, "leftFunction");
            Objects.requireNonNull(rightFunction, "rightFunction");
        }

        @Override
        public App<F, C> eval(Selective<F> selective) {
            return selective.branch(value.eval(selective), leftFunction.eval(selective), rightFunction.eval(selective));
        }

        @Override
        public SelectivePlan<F, C> optimize() {
            SelectivePlan<F, Either<A, B>> optimizedValue = value.optimize();
            SelectivePlan<F, ? extends Function<A, C>> optimizedLeft = leftFunction.optimize();
            SelectivePlan<F, ? extends Function<B, C>> optimizedRight = rightFunction.optimize();
            if (optimizedValue instanceof Pure<?, ?>(Object rawEither)) {
                Either<A, B> either = cast(rawEither);
                if (either != null && either.isLeft() && optimizedLeft instanceof Pure<?, ?>(Object rawLeft)) {
                    Function<A, C> fn = cast(rawLeft);
                    return new Pure<>(Objects.requireNonNull(fn, "branch left function").apply(either.left()));
                }
                if (either != null && either.isRight() && optimizedRight instanceof Pure<?, ?>(Object rawRight)) {
                    Function<B, C> fn = cast(rawRight);
                    return new Pure<>(Objects.requireNonNull(fn, "branch right function").apply(either.right()));
                }
            }
            return new Branch<>(optimizedValue, optimizedLeft, optimizedRight);
        }
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }
}
