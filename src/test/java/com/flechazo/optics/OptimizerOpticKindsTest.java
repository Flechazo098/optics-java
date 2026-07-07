package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.AffineP;
import com.flechazo.hkt.Choice;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.Monoidal;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Tuple2;
import com.flechazo.hkt.Profunctor;
import com.flechazo.hkt.Traversing;
import com.flechazo.hkt.functions.FoldOpticElement;
import com.flechazo.hkt.functions.MapOpticElement;
import com.flechazo.hkt.functions.Comp;
import com.flechazo.hkt.functions.OpticApp;
import com.flechazo.hkt.functions.PointFreeNormalForm;
import com.flechazo.hkt.functions.PointFree;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOpticKind;
import com.flechazo.hkt.functions.PointFreeOptimizer;
import com.flechazo.hkt.functions.ProductSide;
import com.flechazo.hkt.functions.CompositePointFreeOptic;
import com.flechazo.hkt.type.TaggedChoice;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.google.common.reflect.TypeToken;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class OptimizerOpticKindsTest {
  @Test
  void adapterOpticRepresentsTypedFunctionAdapter() {
    PointFreeOptic<Integer, String, Integer, String> adapter =
        PointFreeOptic.adapter(TypeToken.of(Integer.class), TypeToken.of(String.class));
    PointFree<Function<Integer, String>> app =
        PointFree.opticApp(adapter, PointFree.fn("toString", Object::toString));

    assertEquals(PointFreeOpticKind.ADAPTER, adapter.outermost().kind());
    assertTrue(adapter.bounds().contains(Profunctor.Mu.TYPE_TOKEN));
    assertEquals(Types.witness(Integer.class), adapter.types().source());
    assertEquals(Types.witness(String.class), adapter.types().target());
    assertEquals("7", app.eval().apply(7));
  }

  @Test
  void affineOpticRepresentsPartialSetter() {
    PointFreeOptic<Map<String, Integer>, Map<String, Integer>, Integer, Integer> affine =
        PointFreeOptic.affine(
            "a",
            Affine.mapValue("a"),
                new TypeToken<>() {
                },
            TypeToken.of(Integer.class));

    assertEquals(PointFreeOpticKind.AFFINE, affine.outermost().kind());
    assertTrue(affine.bounds().contains(AffineP.Mu.TYPE_TOKEN));
    PointFree<Function<Integer, Integer>> inc = PointFree.fn("inc", x -> x + 1);
    assertEquals(Map.of("a", 2), PointFree.opticApp(affine, inc).eval().apply(Map.of("a", 1)));
    assertEquals(Map.of("b", 1), PointFree.opticApp(affine, inc).eval().apply(Map.of("b", 1)));
  }

  @Test
  void prismOpticRepresentsChoiceBranch() {
    Prism<Either<Integer, String>, Either<Integer, String>, Integer, Integer> left =
        Prism.of(value -> value.isLeft() ? Either.right(value.left()) : Either.left(value), Either::left);
    PointFreeOptic<Either<Integer, String>, Either<Integer, String>, Integer, Integer> prism =
        PointFreeOptic.prism(
            "left",
            left,
                new TypeToken<>() {
                },
            TypeToken.of(Integer.class));

    assertEquals(PointFreeOpticKind.PRISM, prism.outermost().kind());
    assertTrue(prism.bounds().contains(Choice.Mu.TYPE_TOKEN));
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
                new TypeToken<>() {
                },
            TypeToken.of(Integer.class));
    FoldOpticElement element = (FoldOpticElement) fold.outermost().untyped();

    assertEquals(PointFreeOpticKind.FOLD, fold.outermost().kind());
    assertTrue(fold.bounds().contains(Monoidal.Mu.TYPE_TOKEN));
    assertEquals(6, element.foldMap(Monoid.of(0, Integer::sum), value -> (Integer) value, List.of(1, 2, 3)));
    PointFree<Function<Integer, Integer>> inc = PointFree.fn("inc", x -> x + 1);
    assertThrows(UnsupportedOperationException.class, () ->
        PointFree.opticApp(fold, inc).eval().apply(List.of(1, 2, 3)));
  }

  @Test
  void mapTraversalOpticsRepresentValuesAndEntries() {
    TypeToken<String> string = TypeToken.of(String.class);
    TypeToken<Integer> integer = TypeToken.of(Integer.class);
    PointFreeOptic<Map<String, Integer>, Map<String, Integer>, Integer, Integer> values =
        PointFreeOptic.mapValues(string, integer);
    PointFreeOptic<Map<String, Integer>, Map<String, Integer>, Tuple2<String, Integer>, Tuple2<String, Integer>> entries =
        PointFreeOptic.mapEntries(string, integer);
    LinkedHashMap<String, Integer> source = new LinkedHashMap<>();
    source.put("a", 1);
    source.put("b", 2);

    assertEquals(PointFreeOpticKind.MAP, values.outermost().kind());
    assertEquals(MapOpticElement.Target.VALUES, values.outermost().key());
    assertTrue(values.bounds().contains(Traversing.Mu.TYPE_TOKEN));
    assertEquals(Types.map(Types.witness(string), Types.witness(integer)), values.types().source());
    PointFree<Function<Integer, Integer>> inc = PointFree.fn("inc", x -> x + 1);
    assertEquals(Map.of("a", 2, "b", 3), PointFree.opticApp(values, inc).eval().apply(source));

    PointFree<Function<Tuple2<String, Integer>, Tuple2<String, Integer>>> bangKey =
        PointFree.fn("bangKey", tuple -> Tuple2.of(tuple.first() + "!", tuple.second()));
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
    assertTrue(subtype.bounds().contains(AffineP.Mu.TYPE_TOKEN));
    assertEquals(new Circle(3), app.eval().apply(new Circle(2)));
    assertEquals(new Square(4), app.eval().apply(new Square(4)));
  }

  @Test
  void sameOpticFusionCoversAffinePrismTraversalMapTaggedAndSubtypeOptics() {
    Type<Integer> intType = Types.witness(Integer.class);
    Type<String> stringType = Types.witness(String.class);
    Type<Circle> circleType = Types.witness(Circle.class);
    PointFree<Function<Integer, Integer>> inc = PointFree.fn("inc", value -> value + 1, intType, intType);
    PointFree<Function<Integer, Integer>> twice = PointFree.fn("twice", value -> value * 2, intType, intType);
    PointFree<Function<String, String>> appendBang = PointFree.fn("appendBang", value -> value + "!", stringType, stringType);
    PointFree<Function<String, String>> upper = PointFree.fn("upper", String::toUpperCase, stringType, stringType);
    PointFree<Function<Circle, Circle>> grow =
        PointFree.fn("grow", circle -> new Circle(circle.radius() + 1), circleType, circleType);
    PointFree<Function<Circle, Circle>> doubleRadius =
        PointFree.fn("doubleRadius", circle -> new Circle(circle.radius() * 2), circleType, circleType);

    PointFreeOptic<Map<String, Integer>, Map<String, Integer>, Integer, Integer> affine =
        PointFreeOptic.affine("a", Affine.mapValue("a"), Types.map(stringType, intType), intType);
    assertSingleOpticApp(
        PointFreeOptimizer.optimize(PointFree.comp(PointFree.opticApp(affine, twice), PointFree.opticApp(affine, inc))),
        PointFreeOpticKind.AFFINE);

    Prism<Either<Integer, String>, Either<Integer, String>, Integer, Integer> left =
        Prism.of(value -> value.isLeft() ? Either.right(value.left()) : Either.left(value), Either::left);
    PointFreeOptic<Either<Integer, String>, Either<Integer, String>, Integer, Integer> prism =
        PointFreeOptic.prism("left", left, Types.or(intType, stringType), intType);
    PointFree<Function<Either<Integer, String>, Either<Integer, String>>> fusedPrism =
        PointFreeOptimizer.optimize(PointFree.comp(PointFree.opticApp(prism, twice), PointFree.opticApp(prism, inc)));
    assertSingleOpticApp(fusedPrism, PointFreeOpticKind.PRISM);
    assertEquals(Either.left(4), fusedPrism.eval().apply(Either.left(1)));
    assertEquals(Either.right("x"), fusedPrism.eval().apply(Either.right("x")));

    PointFreeOptic<List<Integer>, List<Integer>, Integer, Integer> list = PointFreeOptic.list(intType);
    PointFree<Function<List<Integer>, List<Integer>>> fusedList =
        PointFreeOptimizer.optimize(PointFree.comp(PointFree.opticApp(list, twice), PointFree.opticApp(list, inc)));
    assertSingleOpticApp(fusedList, PointFreeOpticKind.TRAVERSAL);
    assertEquals(List.of(4, 6), fusedList.eval().apply(List.of(1, 2)));

    PointFreeOptic<Map<String, Integer>, Map<String, Integer>, Integer, Integer> values =
        PointFreeOptic.mapValues(stringType, intType);
    PointFree<Function<Map<String, Integer>, Map<String, Integer>>> fusedMap =
        PointFreeOptimizer.optimize(PointFree.comp(PointFree.opticApp(values, twice), PointFree.opticApp(values, inc)));
    assertSingleOpticApp(fusedMap, PointFreeOpticKind.MAP);
    assertEquals(Map.of("a", 4), fusedMap.eval().apply(Map.of("a", 1)));

    PointFreeOptic<Tuple2<String, ?>, Tuple2<String, ?>, String, String> tagged = taggedChoiceBranch("tag", stringType, stringType);
    PointFree<Function<Tuple2<String, ?>, Tuple2<String, ?>>> fusedTagged =
        PointFreeOptimizer.optimize(PointFree.comp(PointFree.opticApp(tagged, upper), PointFree.opticApp(tagged, appendBang)));
    assertSingleOpticApp(fusedTagged, PointFreeOpticKind.TAGGED);
    assertEquals(Tuple2.of("tag", "A!"), fusedTagged.eval().apply(Tuple2.of("tag", "a")));
    assertEquals(Tuple2.of("other", "a"), fusedTagged.eval().apply(Tuple2.of("other", "a")));

    PointFreeOptic<Shape, Shape, Circle, Circle> subtype = PointFreeOptic.subtype(Shape.class, Circle.class);
    PointFree<Function<Shape, Shape>> fusedSubtype =
        PointFreeOptimizer.optimize(PointFree.comp(PointFree.opticApp(subtype, doubleRadius), PointFree.opticApp(subtype, grow)));
    assertSingleOpticApp(fusedSubtype, PointFreeOpticKind.SUBTYPE);
    assertEquals(new Circle(6), fusedSubtype.eval().apply(new Circle(2)));
    assertEquals(new Square(2), fusedSubtype.eval().apply(new Square(2)));
  }

  @Test
  void sharedPrefixFactoringCoversStructuredTraversalSpines() {
    Type<Integer> intType = Types.witness(Integer.class);
    Type<String> stringType = Types.witness(String.class);
    Type<Tuple2<Integer, String>> pairType = Types.and(intType, stringType);
    PointFreeOptic<List<Tuple2<Integer, String>>, List<Tuple2<Integer, String>>, Tuple2<Integer, String>, Tuple2<Integer, String>> list =
        PointFreeOptic.list(pairType);
    PointFreeOptic<Tuple2<Integer, String>, Tuple2<Integer, String>, ?, ?> first =
        PointFreeOptic.product(ProductSide.FIRST, intType, stringType);
    PointFreeOptic<Tuple2<Integer, String>, Tuple2<Integer, String>, ?, ?> second =
        PointFreeOptic.product(ProductSide.SECOND, intType, stringType);
    PointFreeOptic<List<Tuple2<Integer, String>>, List<Tuple2<Integer, String>>, Object, Object> listFirst =
        list.andThen(castOptic(first));
    PointFreeOptic<List<Tuple2<Integer, String>>, List<Tuple2<Integer, String>>, Object, Object> listSecond =
        list.andThen(castOptic(second));
    PointFree<Function<Integer, Integer>> inc = PointFree.fn("inc", value -> value + 1, intType, intType);
    PointFree<Function<String, String>> upper = PointFree.fn("upper", String::toUpperCase, stringType, stringType);

    PointFree<Function<List<Tuple2<Integer, String>>, List<Tuple2<Integer, String>>>> optimized =
        PointFreeOptimizer.optimize(PointFree.comp(PointFree.opticApp(listSecond, upper), PointFree.opticApp(listFirst, inc)));

    assertTrue(optimized instanceof OpticApp<?, ?, ?, ?>);
    OpticApp<?, ?, ?, ?> opticApp = (OpticApp<?, ?, ?, ?>) optimized;
    assertEquals(PointFreeOpticKind.TRAVERSAL, opticApp.optic().outermost().kind());
    assertEquals(1, opticApp.optic().size());
    assertEquals(List.of(Tuple2.of(2, "A"), Tuple2.of(3, "B")), optimized.eval().apply(List.of(Tuple2.of(1, "a"), Tuple2.of(2, "b"))));
    assertTrue(PointFreeNormalForm.isNormal(optimized), () -> PointFreeNormalForm.firstViolation(optimized).orElse("normal"));
  }

  @Test
  void overlappingOpticKindsDoNotGainFakeIndependenceSorting() {
    Type<String> stringType = Types.witness(String.class);
    Type<Integer> intType = Types.witness(Integer.class);
    PointFreeOptic<Map<String, Integer>, Map<String, Integer>, Integer, Integer> values =
        PointFreeOptic.mapValues(stringType, intType);
    PointFreeOptic<Map<String, Integer>, Map<String, Integer>, Tuple2<String, Integer>, Tuple2<String, Integer>> entries =
        PointFreeOptic.mapEntries(stringType, intType);
    PointFree<Function<Integer, Integer>> inc = PointFree.fn("inc", value -> value + 1, intType, intType);
    PointFree<Function<Tuple2<String, Integer>, Tuple2<String, Integer>>> keepEntry =
        PointFree.fn("keepEntry", Function.identity(), Types.and(stringType, intType), Types.and(stringType, intType));
    PointFree<Function<Map<String, Integer>, Map<String, Integer>>> optimized =
        PointFreeOptimizer.optimize(PointFree.comp(PointFree.opticApp(entries, keepEntry), PointFree.opticApp(values, inc)));

    assertTrue(optimized instanceof Comp<?, ?> comp && comp.functions().size() == 2);
    assertTrue(PointFreeNormalForm.isNormal(optimized), () -> PointFreeNormalForm.firstViolation(optimized).orElse("normal"));
    assertEquals(Map.of("a", 2), optimized.eval().apply(Map.of("a", 1)));
  }

  private static void assertSingleOpticApp(PointFree<?> expression, PointFreeOpticKind kind) {
    assertTrue(expression instanceof OpticApp<?, ?, ?, ?>);
    OpticApp<?, ?, ?, ?> opticApp = (OpticApp<?, ?, ?, ?>) expression;
    assertEquals(kind, opticApp.optic().outermost().kind());
    assertTrue(PointFreeNormalForm.isNormal(expression), () -> PointFreeNormalForm.firstViolation(expression).orElse("normal"));
  }

  private static PointFreeOptic<Tuple2<String, ?>, Tuple2<String, ?>, String, String> taggedChoiceBranch(
      String tag,
      Type<String> keyType,
      Type<String> valueType) {
    TaggedChoice.TaggedChoiceType<String> type = Types.taggedChoiceType("choice", keyType, Map.of(tag, valueType));
    return new CompositePointFreeOptic<>(type.branchOptic(tag, valueType, valueType).get());
  }

  @SuppressWarnings("unchecked")
  private static <S, T, A, B> PointFreeOptic<S, T, A, B> castOptic(PointFreeOptic<?, ?, ?, ?> optic) {
    return (PointFreeOptic<S, T, A, B>) optic;
  }
}

