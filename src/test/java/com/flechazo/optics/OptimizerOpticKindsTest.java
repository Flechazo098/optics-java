package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Pair;
import com.flechazo.hkt.ProfunctorBound;
import com.flechazo.hkt.functions.FoldOpticElement;
import com.flechazo.hkt.functions.MapOpticElement;
import com.flechazo.hkt.functions.PointFree;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOpticKind;
import com.flechazo.hkt.type.TypeExpr;
import com.flechazo.hkt.type.TypeRef;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class OptimizerOpticKindsTest {
  @Test
  void adapterOpticRepresentsTypedFunctionAdapter() {
    PointFreeOptic<Integer, String, Integer, String> adapter =
        PointFreeOptic.adapter(TypeRef.of(Integer.class), TypeRef.of(String.class));
    PointFree<Function<Integer, String>> app =
        PointFree.opticApp(adapter, PointFree.fn("toString", Object::toString));

    assertEquals(PointFreeOpticKind.ADAPTER, adapter.outermost().kind());
    assertTrue(adapter.bounds().contains(ProfunctorBound.PROFUNCTOR));
    assertEquals(TypeRef.of(Integer.class).expr(), adapter.types().get().source());
    assertEquals(TypeRef.of(String.class).expr(), adapter.types().get().target());
    assertEquals("7", app.eval().apply(7));
  }

  @Test
  void affineOpticRepresentsPartialSetter() {
    PointFreeOptic<Map<String, Integer>, Map<String, Integer>, Integer, Integer> affine =
        PointFreeOptic.affine(
            "a",
            Affine.mapValue("a"),
                new TypeRef<>() {
                },
            TypeRef.of(Integer.class));

    assertEquals(PointFreeOpticKind.AFFINE, affine.outermost().kind());
    assertTrue(affine.bounds().contains(ProfunctorBound.AFFINE));
    PointFree<Function<Integer, Integer>> inc = PointFree.fn("inc", x -> x + 1);
    assertEquals(Map.of("a", 2), PointFree.opticApp(affine, inc).eval().apply(Map.of("a", 1)));
    assertEquals(Map.of("b", 1), PointFree.opticApp(affine, inc).eval().apply(Map.of("b", 1)));
  }

  @Test
  void prismOpticRepresentsChoiceBranch() {
    Prism<Either<Integer, String>, Integer> left =
        Prism.of(value -> value.isLeft() ? Maybe.some(value.left()) : Maybe.none(), Either::left);
    PointFreeOptic<Either<Integer, String>, Either<Integer, String>, Integer, Integer> prism =
        PointFreeOptic.prism(
            "left",
            left,
                new TypeRef<>() {
                },
            TypeRef.of(Integer.class));

    assertEquals(PointFreeOpticKind.PRISM, prism.outermost().kind());
    assertTrue(prism.bounds().contains(ProfunctorBound.CHOICE));
    PointFree<Function<Integer, Integer>> doubleValue = PointFree.fn("double", x -> x * 2);
    assertEquals(Either.left(4), PointFree.opticApp(prism, doubleValue).eval().apply(Either.left(2)));
    assertEquals(Either.right("x"), PointFree.opticApp(prism, doubleValue).eval().apply(Either.right("x")));
  }

  @Test
  void foldOpticRepresentsQueryOnlySpineElement() {
    PointFreeOptic<List<Integer>, List<Integer>, Integer, Integer> fold =
        PointFreeOptic.fold(
            "values",
            Fold.of(values -> values),
                new TypeRef<>() {
                },
            TypeRef.of(Integer.class));
    FoldOpticElement element = (FoldOpticElement) fold.outermost().untyped();

    assertEquals(PointFreeOpticKind.FOLD, fold.outermost().kind());
    assertTrue(fold.bounds().contains(ProfunctorBound.MONOIDAL));
    assertEquals(6, element.foldMap(Monoid.of(0, Integer::sum), value -> (Integer) value, List.of(1, 2, 3)));
    PointFree<Function<Integer, Integer>> inc = PointFree.fn("inc", x -> x + 1);
    assertThrows(UnsupportedOperationException.class, () ->
        PointFree.opticApp(fold, inc).eval().apply(List.of(1, 2, 3)));
  }

  @Test
  void mapTraversalOpticsRepresentValuesAndEntries() {
    TypeRef<String> string = TypeRef.of(String.class);
    TypeRef<Integer> integer = TypeRef.of(Integer.class);
    PointFreeOptic<Map<String, Integer>, Map<String, Integer>, Integer, Integer> values =
        PointFreeOptic.mapValues(string, integer);
    PointFreeOptic<Map<String, Integer>, Map<String, Integer>, Pair<String, Integer>, Pair<String, Integer>> entries =
        PointFreeOptic.mapEntries(string, integer);
    LinkedHashMap<String, Integer> source = new LinkedHashMap<>();
    source.put("a", 1);
    source.put("b", 2);

    assertEquals(PointFreeOpticKind.MAP, values.outermost().kind());
    assertEquals(MapOpticElement.Target.VALUES, values.outermost().key());
    assertTrue(values.bounds().contains(ProfunctorBound.TRAVERSING));
    assertEquals(TypeExpr.map(string.expr(), integer.expr()), values.types().get().source());
    PointFree<Function<Integer, Integer>> inc = PointFree.fn("inc", x -> x + 1);
    assertEquals(Map.of("a", 2, "b", 3), PointFree.opticApp(values, inc).eval().apply(source));

    PointFree<Function<Pair<String, Integer>, Pair<String, Integer>>> bangKey =
        PointFree.fn("bangKey", pair -> Pair.of(pair.first() + "!", pair.second()));
    assertEquals(
        Map.of("a!", 1, "b!", 2),
        PointFree.opticApp(entries, bangKey).eval().apply(source));
  }

  @Test
  void subtypeOpticRepresentsRuntimeSubtypeFocus() {
    PointFreeOptic<Shape, Shape, Circle, Circle> subtype = PointFreeOptic.subtype(Shape.class, Circle.class);
    PointFree<Function<Circle, Circle>> grow = PointFree.fn("grow", circle -> new Circle(circle.radius() + 1));
    PointFree<Function<Shape, Shape>> app =
        PointFree.opticApp(subtype, grow);

    assertEquals(PointFreeOpticKind.SUBTYPE, subtype.outermost().kind());
    assertTrue(subtype.bounds().contains(ProfunctorBound.AFFINE));
    assertEquals(new Circle(3), app.eval().apply(new Circle(2)));
    assertEquals(new Square(4), app.eval().apply(new Square(4)));
  }
}
