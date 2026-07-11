package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Const;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Tuple2;
import com.flechazo.hkt.business.util.OptionalOps;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.util.Affines;
import com.flechazo.optics.util.ListPrisms;
import com.flechazo.optics.util.Prisms;
import com.flechazo.optics.util.StringTraversals;
import com.flechazo.optics.util.Traversals;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UtilityOpticsTest {

  @Test
  void maybePrismAndTraversalUtilitiesWorkWithLibraryMaybe() {
    PPrism<Maybe<String>, Maybe<String>, String, String> some = Prisms.some();
    PTraversal<List<Integer>, List<Integer>, Integer, Integer> list = Traversals.forList();

    assertEquals(Maybe.some("x"), some.getMaybe(Maybe.some("x")));
    assertEquals(Maybe.none(), some.getMaybe(Maybe.none()));
    assertEquals(List.of(2, 3, 4), list.modify(value -> value + 1, List.of(1, 2, 3)));
  }

  @Test
  void coreUsesMaybeAndOptionalOpsOwnsJdkConversion() {
    PPrism<Maybe<String>, Maybe<String>, String, String> some = Prisms.some();
    PTraversal<List<Integer>, List<Integer>, Integer, Integer> list = Traversals.forList();

    assertEquals(Maybe.some("x"), some.getMaybe(Maybe.some("x")));
    assertEquals(Maybe.some("X"), some.getMaybe(Maybe.some("x")).map(String::toUpperCase));
    assertEquals(Maybe.some(1), list.preview(List.of(1, 2, 3)));
    assertEquals(Maybe.some(2), list.asFold().find(value -> value > 1, List.of(1, 2, 3)));
    assertEquals(Maybe.none(), list.asFold().find(value -> value > 9, List.of(1, 2, 3)));
    assertEquals(Maybe.some("a"), ListPrisms.<String>head().getMaybe(List.of("a", "b")));
    assertEquals(Maybe.none(), ListPrisms.<String>head().getMaybe(List.of()));
    assertEquals(Optional.of("x"), OptionalOps.fromMaybe(Maybe.some("x")));
    assertEquals(Maybe.some("x"), OptionalOps.toMaybe(Optional.of("x")));
    assertThrows(NullPointerException.class, () -> Maybe.some(null));
  }

  @Test
  void writableOpticsDowngradeToPureSetters() {
    PPrism<Shape, Shape, Circle, Circle> circle = RecordOptics.subtypePrism(Shape.class, Circle.class);
    PAffine<Maybe<Integer>, Maybe<Integer>, Integer, Integer> some = Affines.maybeValue();
    PTraversal<List<Integer>, List<Integer>, Integer, Integer> each = Traversals.forList();
    PSetter<Shape, Shape, Circle, Circle> prismSetter = circle.asSetter();
    PSetter<Maybe<Integer>, Maybe<Integer>, Integer, Integer> affineSetter = some.asSetter();
    PSetter<List<Integer>, List<Integer>, Integer, Integer> traversalSetter = each.asSetter();

    assertEquals((Shape) new Circle(6), prismSetter.modify(value -> new Circle(value.radius() + 1), new Circle(5)));
    assertEquals((Shape) new Rect(2, 3), prismSetter.modify(value -> new Circle(value.radius() + 1), new Rect(2, 3)));
    assertEquals(Maybe.some(2), affineSetter.modify(value -> value + 1, Maybe.some(1)));
    assertEquals(Maybe.none(), affineSetter.modify(value -> value + 1, Maybe.none()));
    assertEquals(List.of(2, 3), traversalSetter.modify(value -> value + 1, List.of(1, 2)));
  }

  @Test
  void tuple2SupportsMappingAndFoldingBothSides() {
    Tuple2<Integer, String> tuple = Tuple2.of(1, "a");

    assertEquals(Tuple2.of(2, "a"), tuple.mapFirst(value -> value + 1));
    assertEquals(Tuple2.of(1, "A"), tuple.mapSecond(String::toUpperCase));
    assertEquals(Tuple2.of(2, "A"), tuple.mapBoth(value -> value + 1, String::toUpperCase));
    assertEquals("1:a", tuple.fold((first, second) -> first + ":" + second));

    var constApplicative = Const.applicative(Monoid.of("", String::concat));
    App<Const.Mu<String>, Integer> combined =
        constApplicative.map2(Const.of("left"), Const.of("right"), Integer::sum);
    assertEquals("leftright", Const.get(combined));
  }

  @Test
  void stringTraversalRebuildsImmutableStrings() {
    assertEquals("ABC", StringTraversals.characters().modify(Character::toUpperCase, "abc"));
  }
}
