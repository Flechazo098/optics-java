package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.functions.SelectivePlan;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class SelectiveTest {

  @Test
  void selectiveDerivedCombinatorsPreserveLazyBranching() {
    Selective<Maybe.Mu, ?> selective = Maybe.selective();
    Function<Integer, String> leftToString = value -> "L" + value;
    Function<String, String> rightToString = value -> "R" + value;

    assertEquals(
        Maybe.some("L3"),
        Maybe.unbox(selective.branch(
            Maybe.some(Either.left(3)),
            Maybe.some(leftToString),
            Maybe.some(rightToString))));
    assertEquals(
        Maybe.some("Rok"),
        Maybe.unbox(selective.branch(
            Maybe.some(Either.right("ok")),
            Maybe.none(),
            Maybe.some(rightToString))));

    assertEquals(
        Maybe.some(Unit.INSTANCE),
        Maybe.unbox(selective.whenS(
            Maybe.some(false),
            () -> {
              throw new AssertionError("effect should not run");
            })));
    assertEquals(
        Maybe.some(Unit.INSTANCE),
        Maybe.unbox(selective.whenS_(
            Maybe.some(true),
            () -> Maybe.some("ignored"))));
    assertEquals(
        Maybe.some(Unit.INSTANCE),
        Maybe.unbox(selective.unlessS(
            Maybe.some(true),
            () -> {
              throw new AssertionError("effect should not run");
            })));

    List<App<Maybe.Mu, Either<String, Integer>>> alternatives =
        List.of(Maybe.some(Either.left("first")), Maybe.some(Either.right(2)));
    assertEquals(Maybe.some(Either.right(2)), Maybe.unbox(selective.orElse(alternatives)));

    List<App<Maybe.Mu, ? extends Function<Integer, Either<String, Integer>>>> steps =
        List.of(
            Maybe.some(value -> Either.right(value + 1)),
            Maybe.some(value -> Either.left("stop")),
            Maybe.none());
    assertEquals(
        Maybe.some(Either.left("stop")),
        Maybe.unbox(selective.apS(Maybe.some(Either.right(1)), steps)));
  }

  @Test
  void selectivePlanOptimizesStaticSelectAndLazyConditionals() {
    Selective<Maybe.Mu, ?> selective = Maybe.selective();
    SelectivePlan<Maybe.Mu, String> rightSelect =
        SelectivePlan.select(
            SelectivePlan.pure(Either.right("ready")),
            SelectivePlan.lift(Maybe.none()));
    SelectivePlan<Maybe.Mu, String> leftSelect =
        SelectivePlan.select(
            SelectivePlan.pure(Either.left(3)),
            SelectivePlan.pure(value -> "L" + value));

    assertTrue(rightSelect.optimize() instanceof SelectivePlan.Pure<?, ?>);
    assertEquals(Maybe.some("ready"), Maybe.unbox(rightSelect.optimize().eval(selective)));
    assertEquals(Maybe.some("L3"), Maybe.unbox(leftSelect.optimize().eval(selective)));

    SelectivePlan<Maybe.Mu, String> conditional =
        SelectivePlan.ifS(
            SelectivePlan.pure(false),
            () -> {
              throw new AssertionError("unselected selective plan branch should not be requested");
            },
            () -> SelectivePlan.pure("else"));
    assertEquals(Maybe.some("else"), Maybe.unbox(conditional.optimize().eval(selective)));
  }

  @Test
  void selectivePlanOptimizesStaticBranchWhenSelectedFunctionIsPure() {
    Selective<Maybe.Mu, ?> selective = Maybe.selective();
    SelectivePlan<Maybe.Mu, String> branch =
        SelectivePlan.branch(
            SelectivePlan.pure(Either.right("ok")),
            SelectivePlan.lift(Maybe.<Function<Integer, String>>none()),
            SelectivePlan.pure(value -> "R" + value));

    SelectivePlan<Maybe.Mu, String> optimized = branch.optimize();

    assertTrue(optimized instanceof SelectivePlan.Pure<?, ?>);
    assertEquals(Maybe.some("Rok"), Maybe.unbox(optimized.eval(selective)));
  }

}
