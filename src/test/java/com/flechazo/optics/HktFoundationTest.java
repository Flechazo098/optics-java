package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.App;
import com.flechazo.hkt.App2;
import com.flechazo.hkt.Cartesian;
import com.flechazo.hkt.Choice;
import com.flechazo.hkt.Closed;
import com.flechazo.hkt.Const;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.FunctionArrow;
import com.flechazo.hkt.IdF;
import com.flechazo.hkt.Monoidal;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.Profunctor;
import com.flechazo.hkt.Strong;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

class HktFoundationTest {
  @Test
  void constAndIdFExposeDirectGetHelpers() {
    assertEquals("payload", Const.get(Const.of("payload")));
    assertEquals(42, IdF.get(IdF.of(42)));
  }

  @Test
  void functionArrowImplementsProfunctorDimapAndMaps() {
    Profunctor<FunctionArrow.Mu, FunctionArrow.FunctionArrowInstance.Mu> profunctor = FunctionArrow.instance();
    App2<FunctionArrow.Mu, String, Integer> length = FunctionArrow.of(String::length);

    FunctionArrow<Boolean, String> arrow =
        FunctionArrow.unbox(
            profunctor.dimap(
                length,
                flag -> flag ? "abcd" : "x",
                value -> "len=" + value));

    assertEquals("len=4", arrow.apply(true));
    assertEquals("len=1", arrow.apply(false));
  }

  @Test
  void functionArrowStrongCartesianLiftsThroughProducts() {
    Cartesian<FunctionArrow.Mu, FunctionArrow.FunctionArrowInstance.Mu> cartesian = FunctionArrow.instance();
    Strong<FunctionArrow.Mu, FunctionArrow.FunctionArrowInstance.Mu> strong = FunctionArrow.instance();
    App2<FunctionArrow.Mu, Integer, Integer> plusOne = FunctionArrow.of(value -> value + 1);

    FunctionArrow<Tuple2<Integer, String>, Tuple2<Integer, String>> first =
        FunctionArrow.unbox(cartesian.first(plusOne));
    FunctionArrow<Tuple2<String, Integer>, Tuple2<String, Integer>> second =
        FunctionArrow.unbox(strong.second(plusOne));

    assertEquals(Tuple2.of(2, "a"), first.apply(Tuple2.of(1, "a")));
    assertEquals(Tuple2.of("a", 2), second.apply(Tuple2.of("a", 1)));
  }

  @Test
  void functionArrowChoiceLiftsThroughSums() {
    Choice<FunctionArrow.Mu, FunctionArrow.FunctionArrowInstance.Mu> choice = FunctionArrow.instance();
    App2<FunctionArrow.Mu, Integer, Integer> timesTwo = FunctionArrow.of(value -> value * 2);

    FunctionArrow<Either<Integer, String>, Either<Integer, String>> left =
        FunctionArrow.unbox(choice.left(timesTwo));
    FunctionArrow<Either<String, Integer>, Either<String, Integer>> right =
        FunctionArrow.unbox(choice.right(timesTwo));

    assertEquals(Either.left(6), left.apply(Either.left(3)));
    assertEquals(Either.right("x"), left.apply(Either.right("x")));
    assertEquals(Either.right(6), right.apply(Either.right(3)));
    assertEquals(Either.left("x"), right.apply(Either.left("x")));
  }

  @Test
  void functionArrowClosedAndMonoidalComposeFunctionSpacesAndProducts() {
    Closed<FunctionArrow.Mu, FunctionArrow.FunctionArrowInstance.Mu> closed = FunctionArrow.instance();
    Monoidal<FunctionArrow.Mu, FunctionArrow.FunctionArrowInstance.Mu> monoidal = FunctionArrow.instance();
    App2<FunctionArrow.Mu, Integer, Integer> plusOne = FunctionArrow.of(value -> value + 1);
    App2<FunctionArrow.Mu, String, String> bang = FunctionArrow.of(value -> value + "!");

    FunctionArrow<Function<String, Integer>, Function<String, Integer>> lifted =
        FunctionArrow.unbox(closed.closed(plusOne));
    FunctionArrow<Tuple2<Integer, String>, Tuple2<Integer, String>> paired =
        FunctionArrow.unbox(monoidal.par(plusOne, () -> bang));

    assertEquals(4, lifted.apply(String::length).apply("abc"));
    assertEquals(Tuple2.of(2, "a!"), paired.apply(Tuple2.of(1, "a")));
  }

  @Test
  void profunctorBoundsUseGuavaTypeTokensForRuntimeBoundChecks() {
    assertEquals(Cartesian.Mu.class, Cartesian.Mu.TYPE_TOKEN.getRawType());
    assertTrue(Strong.Mu.TYPE_TOKEN.isSupertypeOf(FunctionArrow.FunctionArrowInstance.Mu.TYPE_TOKEN));
    assertTrue(Choice.Mu.TYPE_TOKEN.isSupertypeOf(FunctionArrow.FunctionArrowInstance.Mu.TYPE_TOKEN));
    assertTrue(Closed.Mu.TYPE_TOKEN.isSupertypeOf(FunctionArrow.FunctionArrowInstance.Mu.TYPE_TOKEN));
    assertTrue(Monoidal.Mu.TYPE_TOKEN.isSupertypeOf(FunctionArrow.FunctionArrowInstance.Mu.TYPE_TOKEN));
  }

  @Test
  void functionArrowProofInstanceIsAKind2Box() {
    App<FunctionArrow.FunctionArrowInstance.Mu, FunctionArrow.Mu> proof = FunctionArrow.instance();

    assertEquals(FunctionArrow.instance(), Profunctor.unbox(proof));
    assertEquals(FunctionArrow.instance(), Cartesian.unbox(proof));
  }
}
