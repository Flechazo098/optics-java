package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.functions.OpticLowering;
import com.flechazo.hkt.functions.PointFree;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOpticKind;
import com.flechazo.hkt.type.Types;
import com.google.common.reflect.TypeToken;
import com.flechazo.optics.util.Traversals;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class AutomaticLoweringTest {
  @Test
  void lowersOrdinaryLensAndDirectModifySetOperations() {
    Lens<Box, Integer> value = Lens.of(Box::value, (box, next) -> new Box(next));
    PointFreeOptic<Box, Box, Integer, Integer> optic =
        OpticLowering.lens("value", value, TypeToken.of(Box.class), TypeToken.of(Integer.class));

    assertEquals(PointFreeOpticKind.LENS, optic.outermost().kind());
    assertEquals(Types.witness(Box.class), optic.sourceType());
    assertEquals(new Box(2), OpticLowering.applyModify(optic, "inc", current -> current + 1, new Box(1)));
    assertEquals(new Box(5), OpticLowering.applySet(optic, 5, new Box(1)));
  }

  @Test
  void lowersAffinePrismAndTraversalIntoExecutableOptimizerPlans() {
    PointFreeOptic<Map<String, Integer>, Map<String, Integer>, Integer, Integer> affine =
        OpticLowering.affine(
            "a",
            Affine.mapValue("a"),
                new TypeToken<>() {
                },
            TypeToken.of(Integer.class));
    Prism<Either<Integer, String>, Integer> left =
        Prism.of(value -> value.isLeft() ? Maybe.some(value.left()) : Maybe.none(), Either::left);
    PointFreeOptic<Either<Integer, String>, Either<Integer, String>, Integer, Integer> prism =
        OpticLowering.prism(
            "left",
            left,
                new TypeToken<>() {
                },
            TypeToken.of(Integer.class));
    PointFreeOptic<List<Integer>, List<Integer>, Integer, Integer> traversal =
        OpticLowering.traversal(
            "list",
            Traversals.forList(),
                new TypeToken<>() {
                },
            TypeToken.of(Integer.class));

    assertEquals(Map.of("a", 2), OpticLowering.applyModify(affine, "inc", x -> x + 1, Map.of("a", 1)));
    assertEquals(Either.left(4), OpticLowering.applyModify(prism, "double", x -> x * 2, Either.left(2)));
    assertEquals(List.of(2, 3, 4), OpticLowering.applyModify(traversal, "inc", x -> x + 1, List.of(1, 2, 3)));
  }

  @Test
  void lowersFoldQueriesAndPreviewOperations() {
    Fold<List<Integer>, Integer> fold = Fold.of(values -> values);

    assertEquals(Maybe.some(1), OpticLowering.preview(fold).eval().apply(List.of(1, 2, 3)));
    assertEquals(6, OpticLowering.foldMap(fold, Monoid.of(0, Integer::sum), value -> value).run(List.of(1, 2, 3)));
  }

  @Test
  void lowersGeneratedRecordLensAndTraversalWithRuntimeTypeWitnesses() {
    PointFreeOptic<Account, Account, String, String> name =
        OpticLowering.recordLens(Account.class, "name");
    PointFreeOptic<Account, Account, Integer, Integer> scores =
        OpticLowering.recordTraversal(Account.class, "scores");
    PointFree<Function<Account, Account>> upper =
        OpticLowering.modify(name, "upper", String::toUpperCase);

    assertEquals(PointFreeOpticKind.LENS, name.outermost().kind());
    assertEquals(Types.witness(Account.class), name.sourceType());
    assertEquals(Types.witness(String.class), name.focusType());
    assertEquals(new Account("ROOT", List.of(1, 2)), upper.eval().apply(new Account("root", List.of(1, 2))));
    assertEquals(
        new Account("root", List.of(2, 3)),
        OpticLowering.applyModify(scores, "incAll", value -> value + 1, new Account("root", List.of(1, 2))));
  }

  @Test
  void lowersSubtypePrismToOptimizerSubtypeSpine() {
    PointFreeOptic<Shape, Shape, Circle, Circle> subtype = OpticLowering.subtype(Shape.class, Circle.class);

    assertEquals(new Circle(3), OpticLowering.applyModify(subtype, "grow", circle -> new Circle(circle.radius() + 1), new Circle(2)));
    assertEquals(new Square(4), OpticLowering.applyModify(subtype, "grow", circle -> new Circle(circle.radius() + 1), new Square(4)));
  }
}

