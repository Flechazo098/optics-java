package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.functions.SelectivePlan;
import com.flechazo.optics.util.Prisms;
import com.flechazo.optics.util.Traversals;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class SelectiveTest {

  @Test
  void selectiveDerivedCombinatorsPreserveLazyBranching() {
    Selective<Maybe.Mu> selective = Maybe.selective();
    Function<Integer, String> leftToString = value -> "L" + value;
    Function<String, String> rightToString = value -> "R" + value;

    assertEquals(
        Maybe.some("L3"),
        Maybe.narrow(selective.branch(
            Maybe.some(Either.left(3)),
            Maybe.some(leftToString),
            Maybe.some(rightToString))));
    assertEquals(
        Maybe.some("Rok"),
        Maybe.narrow(selective.branch(
            Maybe.some(Either.right("ok")),
            Maybe.none(),
            Maybe.some(rightToString))));

    assertEquals(
        Maybe.some(Unit.INSTANCE),
        Maybe.narrow(selective.whenS(
            Maybe.some(false),
            () -> {
              throw new AssertionError("effect should not run");
            })));
    assertEquals(
        Maybe.some(Unit.INSTANCE),
        Maybe.narrow(selective.whenS_(
            Maybe.some(true),
            () -> Maybe.some("ignored"))));
    assertEquals(
        Maybe.some(Unit.INSTANCE),
        Maybe.narrow(selective.unlessS(
            Maybe.some(true),
            () -> {
              throw new AssertionError("effect should not run");
            })));

    List<App<Maybe.Mu, Either<String, Integer>>> alternatives =
        List.of(Maybe.some(Either.left("first")), Maybe.some(Either.right(2)));
    assertEquals(Maybe.some(Either.right(2)), Maybe.narrow(selective.orElse(alternatives)));

    List<App<Maybe.Mu, ? extends Function<Integer, Either<String, Integer>>>> steps =
        List.of(
            Maybe.some(value -> Either.right(value + 1)),
            Maybe.some(value -> Either.left("stop")),
            Maybe.none());
    assertEquals(
        Maybe.some(Either.left("stop")),
        Maybe.narrow(selective.apS(Maybe.some(Either.right(1)), steps)));
  }

  @Test
  void selectivePlanOptimizesStaticSelectAndLazyConditionals() {
    Selective<Maybe.Mu> selective = Maybe.selective();
    SelectivePlan<Maybe.Mu, String> rightSelect =
        SelectivePlan.select(
            SelectivePlan.pure(Either.right("ready")),
            SelectivePlan.lift(Maybe.none()));
    SelectivePlan<Maybe.Mu, String> leftSelect =
        SelectivePlan.select(
            SelectivePlan.pure(Either.left(3)),
            SelectivePlan.pure(value -> "L" + value));

    assertTrue(rightSelect.optimize() instanceof SelectivePlan.Pure<?, ?>);
    assertEquals(Maybe.some("ready"), Maybe.narrow(rightSelect.optimize().eval(selective)));
    assertEquals(Maybe.some("L3"), Maybe.narrow(leftSelect.optimize().eval(selective)));

    SelectivePlan<Maybe.Mu, String> conditional =
        SelectivePlan.ifS(
            SelectivePlan.pure(false),
            () -> {
              throw new AssertionError("unselected selective plan branch should not be requested");
            },
            () -> SelectivePlan.pure("else"));
    assertEquals(Maybe.some("else"), Maybe.narrow(conditional.optimize().eval(selective)));
  }

  @Test
  void selectivePlanOptimizesStaticBranchWhenSelectedFunctionIsPure() {
    Selective<Maybe.Mu> selective = Maybe.selective();
    SelectivePlan<Maybe.Mu, String> branch =
        SelectivePlan.branch(
            SelectivePlan.pure(Either.right("ok")),
            SelectivePlan.lift(Maybe.<Function<Integer, String>>none()),
            SelectivePlan.pure(value -> "R" + value));

    SelectivePlan<Maybe.Mu, String> optimized = branch.optimize();

    assertTrue(optimized instanceof SelectivePlan.Pure<?, ?>);
    assertEquals(Maybe.some("Rok"), Maybe.narrow(optimized.eval(selective)));
  }

  @Test
  void opticsExposeSelectiveConditionalModification() {
    record Counter(int value) {}
    Selective<Maybe.Mu> selective = Maybe.selective();
    Lens<Counter, Integer> value = Lens.of(Counter::value, (counter, next) -> new Counter(next));
    Traversal<List<Integer>, Integer> each = Traversals.forList();
    Affine<List<Integer>, Integer> second = Affine.listAt(1);
    Prism<Object, String> string = Prisms.instanceOf(String.class);

    assertEquals(
        Maybe.some(new Counter(2)),
        Maybe.narrow(value.modifyWhen(
            current -> current < 3,
            current -> Maybe.some(current + 1),
            new Counter(1),
            selective)));
    assertEquals(
        Maybe.some(new Counter(10)),
        Maybe.narrow(value.modifyWhen(
            current -> current < 3,
            current -> {
              throw new AssertionError("unselected lens branch should not run");
            },
            new Counter(10),
            selective)));
    assertEquals(
        Maybe.some(new Counter(20)),
        Maybe.narrow(value.modifyBranch(
            current -> current % 2 == 0,
            current -> Maybe.some(current * 2),
            current -> Maybe.some(current + 1),
            new Counter(10),
            selective)));
    assertEquals(
        Maybe.some(List.of(2, 20)),
        Maybe.narrow(each.branch(
            current -> current % 2 == 0,
            current -> Maybe.some(current * 10),
            current -> Maybe.some(current + 1),
            List.of(1, 2),
            selective)));
    assertEquals(
        Maybe.some(List.of(1, 20)),
        Maybe.narrow(each.modifyWhen(
            current -> current > 1,
            current -> Maybe.some(current * 10),
            List.of(1, 2),
            selective)));
    assertEquals(
        Maybe.some(List.of(1, 20)),
        Maybe.narrow(second.modifyWhen(
            current -> current > 1,
            current -> Maybe.some(current * 10),
            List.of(1, 2),
            selective)));
    assertEquals(
        Maybe.some(List.of(1)),
        Maybe.narrow(Affine.<Integer>listAt(3).modifyWhen(
            current -> true,
            current -> {
              throw new AssertionError("missing affine focus should not run");
            },
            List.of(1),
            selective)));
    assertEquals(
        Maybe.some((Object) 1),
        Maybe.narrow(string.modifyWhen(
            current -> true,
            current -> {
              throw new AssertionError("unmatched prism should not run");
            },
            1,
            selective)));
  }
}
