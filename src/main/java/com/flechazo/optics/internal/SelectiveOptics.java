package com.flechazo.optics.internal;

import com.flechazo.hkt.App;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.functions.SelectivePlan;
import com.flechazo.optics.Optic;
import com.flechazo.optics.indexed.IndexedTraversal;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class SelectiveOptics {
    private SelectiveOptics() {
    }

    public static <F extends K1, S, A> App<F, S> modifyWhen(
            Optic<S, S, A, A> optic,
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return TerminalRuntime.execute(
                optic,
                "selective:modifyWhenS",
                () -> modify(optic, condition, modifier, source, selective, true));
    }

    public static <F extends K1, S, A> App<F, S> modifyUnless(
            Optic<S, S, A, A> optic,
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return TerminalRuntime.execute(
                optic,
                "selective:modifyUnlessS",
                () -> modify(optic, condition, modifier, source, selective, false));
    }

    public static <F extends K1, S, T, A, B> App<F, T> modifyBranch(
            Optic<S, T, A, B> optic,
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, B>> thenModifier,
            Function<? super A, ? extends App<F, B>> elseModifier,
            S source,
            Selective<F, ?> selective) {
        return TerminalRuntime.execute(
                optic,
                "selective:modifyBranchS",
                () -> modifyBranchDirect(
                        optic,
                        condition,
                        thenModifier,
                        elseModifier,
                        source,
                        selective));
    }

    public static <F extends K1, S, A> App<F, S> modifyWhen(
            Optic<S, S, A, A> optic,
            Predicate<? super A> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return TerminalRuntime.execute(
                optic,
                "selective:modifyWhenS",
                () -> modifyStatic(optic, condition, modifier, source, selective, true));
    }

    public static <F extends K1, S, A> App<F, S> modifyUnless(
            Optic<S, S, A, A> optic,
            Predicate<? super A> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return TerminalRuntime.execute(
                optic,
                "selective:modifyUnlessS",
                () -> modifyStatic(optic, condition, modifier, source, selective, false));
    }

    public static <F extends K1, I, S, A> App<F, S> imodifyWhen(
            IndexedTraversal<I, S, A> optic,
            BiFunction<? super I, ? super A, ? extends App<F, Boolean>> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return TerminalRuntime.execute(
                optic,
                "selective:indexedModifyWhenS",
                () -> imodify(optic, condition, modifier, source, selective, true));
    }

    public static <F extends K1, I, S, A> App<F, S> imodifyUnless(
            IndexedTraversal<I, S, A> optic,
            BiFunction<? super I, ? super A, ? extends App<F, Boolean>> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return TerminalRuntime.execute(
                optic,
                "selective:indexedModifyUnlessS",
                () -> imodify(optic, condition, modifier, source, selective, false));
    }

    public static <F extends K1, I, S, A> App<F, S> imodifyWhen(
            IndexedTraversal<I, S, A> optic,
            BiPredicate<? super I, ? super A> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return TerminalRuntime.execute(
                optic,
                "selective:indexedModifyWhenS",
                () -> imodifyStatic(optic, condition, modifier, source, selective, true));
    }

    public static <F extends K1, I, S, A> App<F, S> imodifyUnless(
            IndexedTraversal<I, S, A> optic,
            BiPredicate<? super I, ? super A> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective) {
        return TerminalRuntime.execute(
                optic,
                "selective:indexedModifyUnlessS",
                () -> imodifyStatic(optic, condition, modifier, source, selective, false));
    }

    private static <F extends K1, S, A> App<F, S> modify(
            Optic<S, S, A, A> optic,
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective,
            boolean modifyWhenTrue) {
        Objects.requireNonNull(optic, "optic");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(modifier, "modifier");
        Objects.requireNonNull(selective, "selective");
        return optic.modifyF(
                value -> choose(value, condition, modifier, selective, modifyWhenTrue),
                source,
                selective);
    }

    private static <F extends K1, I, S, A> App<F, S> imodify(
            IndexedTraversal<I, S, A> optic,
            BiFunction<? super I, ? super A, ? extends App<F, Boolean>> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective,
            boolean modifyWhenTrue) {
        Objects.requireNonNull(optic, "optic");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(modifier, "modifier");
        Objects.requireNonNull(selective, "selective");
        return optic.imodifyF(
                (index, value) -> choose(index, value, condition, modifier, selective, modifyWhenTrue),
                source,
                selective);
    }

    private static <F extends K1, S, A> App<F, S> modifyStatic(
            Optic<S, S, A, A> optic,
            Predicate<? super A> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective,
            boolean modifyWhenTrue) {
        Objects.requireNonNull(optic, "optic");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(modifier, "modifier");
        Objects.requireNonNull(selective, "selective");
        return optic.modifyF(
                value -> chooseStatic(value, condition.test(value), modifier, selective, modifyWhenTrue),
                source,
                selective);
    }

    private static <F extends K1, S, T, A, B> App<F, T> modifyBranchDirect(
            Optic<S, T, A, B> optic,
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, B>> thenModifier,
            Function<? super A, ? extends App<F, B>> elseModifier,
            S source,
            Selective<F, ?> selective) {
        Objects.requireNonNull(optic, "optic");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(thenModifier, "thenModifier");
        Objects.requireNonNull(elseModifier, "elseModifier");
        Objects.requireNonNull(selective, "selective");
        return optic.modifyF(
                value -> branch(
                        value,
                        condition,
                        thenModifier,
                        elseModifier,
                        selective),
                source,
                selective);
    }

    private static <F extends K1, I, S, A> App<F, S> imodifyStatic(
            IndexedTraversal<I, S, A> optic,
            BiPredicate<? super I, ? super A> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            S source,
            Selective<F, ?> selective,
            boolean modifyWhenTrue) {
        Objects.requireNonNull(optic, "optic");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(modifier, "modifier");
        Objects.requireNonNull(selective, "selective");
        return optic.imodifyF(
                (index, value) -> chooseStatic(
                        index,
                        value,
                        condition.test(index, value),
                        modifier,
                        selective,
                        modifyWhenTrue),
                source,
                selective);
    }

    private static <F extends K1, A> App<F, A> choose(
            A value,
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, A>> modifier,
            Selective<F, ?> selective,
            boolean modifyWhenTrue) {
        App<F, Boolean> test = Objects.requireNonNull(condition.apply(value), "condition result");
        return plan(
                SelectivePlan.lift(test),
                () -> SelectivePlan.lift(Objects.requireNonNull(modifier.apply(value), "modifier result")),
                value,
                selective,
                modifyWhenTrue);
    }

    private static <F extends K1, I, A> App<F, A> choose(
            I index,
            A value,
            BiFunction<? super I, ? super A, ? extends App<F, Boolean>> condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            Selective<F, ?> selective,
            boolean modifyWhenTrue) {
        App<F, Boolean> test = Objects.requireNonNull(condition.apply(index, value), "condition result");
        return plan(
                SelectivePlan.lift(test),
                () -> SelectivePlan.lift(Objects.requireNonNull(modifier.apply(index, value), "modifier result")),
                value,
                selective,
                modifyWhenTrue);
    }

    private static <F extends K1, A> App<F, A> chooseStatic(
            A value,
            boolean condition,
            Function<? super A, ? extends App<F, A>> modifier,
            Selective<F, ?> selective,
            boolean modifyWhenTrue) {
        return plan(
                SelectivePlan.pure(condition),
                () -> SelectivePlan.lift(Objects.requireNonNull(modifier.apply(value), "modifier result")),
                value,
                selective,
                modifyWhenTrue);
    }

    private static <F extends K1, A, B> App<F, B> branch(
            A value,
            Function<? super A, ? extends App<F, Boolean>> condition,
            Function<? super A, ? extends App<F, B>> thenModifier,
            Function<? super A, ? extends App<F, B>> elseModifier,
            Selective<F, ?> selective) {
        SelectivePlan<F, Boolean> test = SelectivePlan.lift(
                Objects.requireNonNull(condition.apply(value), "condition result"));
        SelectivePlan<F, B> plan = SelectivePlan.ifS(
                test,
                () -> SelectivePlan.lift(Objects.requireNonNull(
                        thenModifier.apply(value),
                        "thenModifier result")),
                () -> SelectivePlan.lift(Objects.requireNonNull(
                        elseModifier.apply(value),
                        "elseModifier result")));
        return plan.optimize().eval(selective);
    }

    private static <F extends K1, I, A> App<F, A> chooseStatic(
            I index,
            A value,
            boolean condition,
            BiFunction<? super I, ? super A, ? extends App<F, A>> modifier,
            Selective<F, ?> selective,
            boolean modifyWhenTrue) {
        return plan(
                SelectivePlan.pure(condition),
                () -> SelectivePlan.lift(Objects.requireNonNull(modifier.apply(index, value), "modifier result")),
                value,
                selective,
                modifyWhenTrue);
    }

    private static <F extends K1, A> App<F, A> plan(
            SelectivePlan<F, Boolean> condition,
            Supplier<? extends SelectivePlan<F, A>> modifier,
            A value,
            Selective<F, ?> selective,
            boolean modifyWhenTrue) {
        SelectivePlan<F, A> plan = modifyWhenTrue
                ? SelectivePlan.ifS(condition, modifier, () -> SelectivePlan.pure(value))
                : SelectivePlan.ifS(condition, () -> SelectivePlan.pure(value), modifier);
        return plan.optimize().eval(selective);
    }
}
