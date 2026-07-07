package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Const;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Tuple2;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.util.Affines;
import com.flechazo.optics.util.ListPrisms;
import com.flechazo.optics.util.Optionals;
import com.flechazo.optics.util.Prisms;
import com.flechazo.optics.util.StringTraversals;
import com.flechazo.optics.util.Traversals;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UtilityOpticsTest {

  @Test
  void maybePrismAndTraversalUtilitiesWorkWithLibraryMaybe() {
    Prism<Maybe<String>, Maybe<String>, String, String> some = Prisms.some();
    Traversal<List<Integer>, List<Integer>, Integer, Integer> list = Traversals.forList();

    assertEquals(Maybe.some("x"), some.getMaybe(Maybe.some("x")));
    assertEquals(Maybe.none(), some.getMaybe(Maybe.none()));
    assertEquals(List.of(2, 3, 4), list.modify(value -> value + 1, List.of(1, 2, 3)));
  }

  @Test
  void utilLayerExposesOptionalAdaptersWhileCoreKeepsMaybe() {
    Prism<Maybe<String>, Maybe<String>, String, String> some = Prisms.some();
    Traversal<List<Integer>, List<Integer>, Integer, Integer> list = Traversals.forList();

    assertEquals(Maybe.some("x"), some.getMaybe(Maybe.some("x")));
    assertEquals(Optional.of("x"), Prisms.getOptional(some, Maybe.some("x")));
    assertEquals(Optional.empty(), Prisms.getOptional(some, Maybe.none()));
    assertEquals(Optional.of("X"), Prisms.mapOptional(some, String::toUpperCase, Maybe.some("x")));

    assertEquals(Optional.of(1), Traversals.previewOptional(list, List.of(1, 2, 3)));
    assertEquals(Optional.of(2), Traversals.findOptional(list, value -> value > 1, List.of(1, 2, 3)));
    assertEquals(Optional.empty(), Traversals.findOptional(list, value -> value > 9, List.of(1, 2, 3)));

    assertEquals(Optional.of("a"), Affines.getOptional(Affines.optionalValue(), Optional.of("a")));
    assertEquals(Optional.empty(), Affines.getOptional(Affines.optionalValue(), Optional.empty()));
    assertEquals(Optional.of("a"), ListPrisms.headOptional(List.of("a", "b")));
    assertEquals(Optional.empty(), ListPrisms.headOptional(List.of()));

    assertEquals(Optional.of("x"), Optionals.fromMaybe(Maybe.some("x")));
    assertEquals(Maybe.some("x"), Optionals.toMaybe(Optional.of("x")));
    assertThrows(NullPointerException.class, () -> Maybe.some(null));
  }

  @Test
  void optionalSetAndBuildRejectNullInsteadOfRemoving() {
    assertThrows(NullPointerException.class, () -> Affines.<String>optionalValue().set(null, Optional.of("a")));
    assertThrows(NullPointerException.class, () -> Prisms.<String>optionalSome().build(null));
  }

  @Test
  void writableOpticsDowngradeToEffectfulSetters() {
    Prism<Shape, Shape, Circle, Circle> circle = RecordOptics.subtypePrism(Shape.class, Circle.class);
    Affine<Maybe<Integer>, Maybe<Integer>, Integer, Integer> some = Affines.maybeValue();
    Traversal<List<Integer>, List<Integer>, Integer, Integer> each = Traversals.forList();
    Setter<Shape, Shape, Circle, Circle> prismSetter = circle.asSetter();
    Setter<Maybe<Integer>, Maybe<Integer>, Integer, Integer> affineSetter = some.asSetter();
    Setter<List<Integer>, List<Integer>, Integer, Integer> traversalSetter = each.asSetter();

    assertEquals(
        Maybe.some((Shape) new Circle(6)),
        prismSetter.modifyF(value -> Maybe.some(new Circle(value.radius() + 1)), new Circle(5), Maybe.applicative()));
    assertEquals(
        Maybe.some((Shape) new Rect(2, 3)),
        prismSetter.modifyF(value -> Maybe.some(new Circle(value.radius() + 1)), new Rect(2, 3), Maybe.applicative()));
    assertEquals(
        Maybe.some(Maybe.some(2)),
        affineSetter.modifyF(value -> Maybe.some(value + 1), Maybe.some(1), Maybe.applicative()));
    assertEquals(
        Maybe.some(Maybe.<Integer>none()),
        Maybe.unbox(affineSetter.modifyF(value -> Maybe.some(value + 1), Maybe.none(), Maybe.applicative())));
    assertEquals(
        Maybe.some(List.of(2, 3)),
        traversalSetter.modifyF(value -> Maybe.some(value + 1), List.of(1, 2), Maybe.applicative()));
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
