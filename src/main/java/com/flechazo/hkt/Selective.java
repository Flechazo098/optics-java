package com.flechazo.hkt;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Selective<F extends K1, Proof extends Applicative.Mu> extends Applicative<F, Proof> {
    /**
     * Selects an already-computed right value, or applies an already-computed function to a left value.
     *
     * <p>The effect payload, control value, and function value are all non-null structural inputs.
     */
    <A, B> App<F, B> select(App<F, Either<A, B>> value, App<F, ? extends Function<A, B>> function);

    /**
     * Lazy conditional convenience operation. Only the selected branch is requested from its supplier.
     */
    <A> App<F, A> ifS(
            App<F, Boolean> condition,
            Supplier<? extends App<F, A>> thenValue,
            Supplier<? extends App<F, A>> elseValue);

    default <A> App<F, A> ifS(
            App<F, Boolean> condition,
            App<F, A> thenValue,
            App<F, A> elseValue) {
        Objects.requireNonNull(thenValue, "thenValue");
        Objects.requireNonNull(elseValue, "elseValue");
        return ifS(condition, () -> thenValue, () -> elseValue);
    }

    default <A, B, C> App<F, C> branch(
            App<F, Either<A, B>> value,
            App<F, ? extends Function<A, C>> leftFunction,
            App<F, ? extends Function<B, C>> rightFunction) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(leftFunction, "leftFunction");
        Objects.requireNonNull(rightFunction, "rightFunction");

        App<F, Either<A, Either<B, C>>> transformed =
                map(choice -> {
                    Either<A, B> selected = Objects.requireNonNull(choice, "branch value");
                    return selected.isLeft()
                            ? Either.left(selected.left())
                            : Either.right(Either.left(selected.right()));
                }, value);
        App<F, Function<A, Either<B, C>>> leftHandler =
                map(fn -> {
                    Function<A, C> apply = Objects.requireNonNull(fn, "branch left function");
                    return left -> Either.right(apply.apply(left));
                }, leftFunction);
        App<F, Either<B, C>> intermediate = select(transformed, leftHandler);
        return select(intermediate, rightFunction);
    }

    default App<F, Unit> whenS(
            App<F, Boolean> condition,
            Supplier<? extends App<F, Unit>> effect) {
        Objects.requireNonNull(effect, "effect");
        return ifS(condition, effect, () -> of(Unit.INSTANCE));
    }

    default App<F, Unit> whenS(App<F, Boolean> condition, App<F, Unit> effect) {
        Objects.requireNonNull(effect, "effect");
        return whenS(condition, () -> effect);
    }

    default <A> App<F, Unit> whenS_(
            App<F, Boolean> condition,
            Supplier<? extends App<F, A>> effect) {
        Objects.requireNonNull(effect, "effect");
        return ifS(
                condition,
                () -> map(ignored -> Unit.INSTANCE, Objects.requireNonNull(effect.get(), "effect result")),
                () -> of(Unit.INSTANCE));
    }

    default <A> App<F, Unit> whenS_(App<F, Boolean> condition, App<F, A> effect) {
        Objects.requireNonNull(effect, "effect");
        return whenS_(condition, () -> effect);
    }

    default App<F, Unit> unlessS(
            App<F, Boolean> condition,
            Supplier<? extends App<F, Unit>> effect) {
        Objects.requireNonNull(effect, "effect");
        return ifS(condition, () -> of(Unit.INSTANCE), effect);
    }

    default App<F, Unit> unlessS(App<F, Boolean> condition, App<F, Unit> effect) {
        Objects.requireNonNull(effect, "effect");
        return unlessS(condition, () -> effect);
    }

    default <A> App<F, Unit> unlessS_(
            App<F, Boolean> condition,
            Supplier<? extends App<F, A>> effect) {
        Objects.requireNonNull(effect, "effect");
        return ifS(
                condition,
                () -> of(Unit.INSTANCE),
                () -> map(ignored -> Unit.INSTANCE, Objects.requireNonNull(effect.get(), "effect result")));
    }

    default <A> App<F, Unit> unlessS_(App<F, Boolean> condition, App<F, A> effect) {
        Objects.requireNonNull(effect, "effect");
        return unlessS_(condition, () -> effect);
    }

    default <E, A> App<F, Either<E, A>> orElse(List<? extends App<F, Either<E, A>>> alternatives) {
        Objects.requireNonNull(alternatives, "alternatives");
        if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("orElse requires at least one alternative");
        }
        App<F, Either<E, A>> result = Objects.requireNonNull(alternatives.getFirst(), "alternative");
        for (int i = 1; i < alternatives.size(); i++) {
            result = selectOrElse(result, Objects.requireNonNull(alternatives.get(i), "alternative"));
        }
        return result;
    }

    private <E, A> App<F, Either<E, A>> selectOrElse(
            App<F, Either<E, A>> first,
            App<F, Either<E, A>> second) {
        App<F, Either<E, Either<E, A>>> transformed =
                map(choice -> {
                    Either<E, A> selected = Objects.requireNonNull(choice, "orElse value");
                    return selected.isRight()
                            ? Either.right(Either.right(selected.right()))
                            : Either.left(selected.left());
                }, first);
        App<F, Function<E, Either<E, A>>> fallback =
                map(next -> {
                    Either<E, A> selected = Objects.requireNonNull(next, "orElse fallback");
                    return ignored -> selected;
                }, second);
        return select(transformed, fallback);
    }

    default <E, A> App<F, Either<E, A>> apS(
            App<F, Either<E, A>> initial,
            List<? extends App<F, ? extends Function<A, Either<E, A>>>> functions) {
        Objects.requireNonNull(initial, "initial");
        Objects.requireNonNull(functions, "functions");
        App<F, Either<E, A>> result = initial;
        for (App<F, ? extends Function<A, Either<E, A>>> function : functions) {
            App<F, Either<A, Either<E, A>>> transformed =
                    map(choice -> {
                        Either<E, A> selected = Objects.requireNonNull(choice, "apS value");
                        return selected.isRight()
                                ? Either.left(selected.right())
                                : Either.right(Either.left(selected.left()));
                    }, result);
            result = select(transformed, Objects.requireNonNull(function, "function"));
        }
        return result;
    }
}
