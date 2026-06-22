package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.App2;
import com.flechazo.hkt.Cartesian;
import com.flechazo.hkt.Choice;
import com.flechazo.hkt.Closed;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.FunctionArrow;
import com.flechazo.hkt.Monoidal;
import com.flechazo.hkt.Pair;
import com.flechazo.hkt.Profunctor;
import com.flechazo.hkt.ProfunctorBound;
import com.flechazo.hkt.Strong;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class HktFoundationTest {
  @Test
  void functionArrowImplementsProfunctorDimapAndMaps() {
    Profunctor<FunctionArrow.Mu> profunctor = FunctionArrow.instance();
    App2<FunctionArrow.Mu, String, Integer> length = FunctionArrow.of(String::length);

    FunctionArrow<Boolean, String> arrow =
        FunctionArrow.narrow(
            profunctor.dimap(
                flag -> flag ? "abcd" : "x",
                value -> "len=" + value,
                length));

    assertEquals("len=4", arrow.apply(true));
    assertEquals("len=1", arrow.apply(false));
  }

  @Test
  void functionArrowStrongCartesianLiftsThroughProducts() {
    Cartesian<FunctionArrow.Mu> cartesian = FunctionArrow.instance();
    Strong<FunctionArrow.Mu> strong = FunctionArrow.instance();
    App2<FunctionArrow.Mu, Integer, Integer> plusOne = FunctionArrow.of(value -> value + 1);

    FunctionArrow<Pair<Integer, String>, Pair<Integer, String>> first =
        FunctionArrow.narrow(cartesian.first(plusOne));
    FunctionArrow<Pair<String, Integer>, Pair<String, Integer>> second =
        FunctionArrow.narrow(strong.second(plusOne));

    assertEquals(Pair.of(2, "a"), first.apply(Pair.of(1, "a")));
    assertEquals(Pair.of("a", 2), second.apply(Pair.of("a", 1)));
  }

  @Test
  void functionArrowChoiceLiftsThroughSums() {
    Choice<FunctionArrow.Mu> choice = FunctionArrow.instance();
    App2<FunctionArrow.Mu, Integer, Integer> timesTwo = FunctionArrow.of(value -> value * 2);

    FunctionArrow<Either<Integer, String>, Either<Integer, String>> left =
        FunctionArrow.narrow(choice.left(timesTwo));
    FunctionArrow<Either<String, Integer>, Either<String, Integer>> right =
        FunctionArrow.narrow(choice.right(timesTwo));

    assertEquals(Either.left(6), left.apply(Either.left(3)));
    assertEquals(Either.right("x"), left.apply(Either.right("x")));
    assertEquals(Either.right(6), right.apply(Either.right(3)));
    assertEquals(Either.left("x"), right.apply(Either.left("x")));
  }

  @Test
  void functionArrowClosedAndMonoidalComposeFunctionSpacesAndProducts() {
    Closed<FunctionArrow.Mu> closed = FunctionArrow.instance();
    Monoidal<FunctionArrow.Mu> monoidal = FunctionArrow.instance();
    App2<FunctionArrow.Mu, Integer, Integer> plusOne = FunctionArrow.of(value -> value + 1);
    App2<FunctionArrow.Mu, String, String> bang = FunctionArrow.of(value -> value + "!");

    FunctionArrow<Function<String, Integer>, Function<String, Integer>> lifted =
        FunctionArrow.narrow(closed.closed(plusOne));
    FunctionArrow<Pair<Integer, String>, Pair<Integer, String>> paired =
        FunctionArrow.narrow(monoidal.par(plusOne, bang));

    assertEquals(4, lifted.apply(String::length).apply("abc"));
    assertEquals(Pair.of(2, "a!"), paired.apply(Pair.of(1, "a")));
  }

  @Test
  void profunctorBoundsUseGuavaTypeTokensForRuntimeBoundChecks() {
    assertEquals(Cartesian.class, ProfunctorBound.CARTESIAN.token().getRawType());
    assertTrue(ProfunctorBound.STRONG.isSatisfiedBy(FunctionArrow.FunctionArrowInstance.class));
    assertTrue(ProfunctorBound.CHOICE.isSatisfiedBy(FunctionArrow.FunctionArrowInstance.class));
    assertTrue(ProfunctorBound.CLOSED.isSatisfiedBy(FunctionArrow.FunctionArrowInstance.class));
    assertTrue(ProfunctorBound.MONOIDAL.isSatisfiedBy(FunctionArrow.FunctionArrowInstance.class));
  }
}
