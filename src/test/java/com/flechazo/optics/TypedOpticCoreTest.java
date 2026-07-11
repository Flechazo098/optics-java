package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.App2;
import com.flechazo.hkt.Cartesian;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.FunctionArrow;
import com.flechazo.hkt.Profunctor;
import com.flechazo.hkt.functions.AdapterOpticElement;
import com.flechazo.hkt.functions.CompositePointFreeOptic;
import com.flechazo.hkt.functions.LensPath;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOpticTypes;
import com.flechazo.hkt.functions.ProductSide;
import com.flechazo.hkt.functions.TypedOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.flechazo.hkt.Tuple2;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypedOpticCoreTest {
  @Test
  void optimizerOpticsCarryFourStructuralTypeParameters() {
    Type<SourceBox> source = Types.witness(SourceBox.class);
    Type<TargetBox> target = Types.witness(TargetBox.class);
    Type<Integer> focus = Types.witness(Integer.class);
    Type<String> replacement = Types.witness(String.class);

    PointFreeOptic<SourceBox, TargetBox, Integer, String> optic =
        new CompositePointFreeOptic<>(new TypedOptic<>(
            Profunctor.Mu.TYPE_TOKEN,
            source,
            target,
            focus,
            replacement,
            new AdapterOpticElement()));

    PointFreeOpticTypes types = optic.types();

    assertEquals(source, types.source());
    assertEquals(target, types.target());
    assertEquals(focus, types.focus());
    assertEquals(replacement, types.replacement());
    assertEquals(source, types.sourceType());
    assertEquals(target, types.targetType());
  }

  @Test
  void optimizerOpticSpinesExposeProfunctorProofTokens() {
    PointFreeOptic<?, ?, ?, ?> product = PointFreeOptic.product(ProductSide.FIRST);

    assertTrue(product.bounds().contains(Cartesian.Mu.TYPE_TOKEN));
    assertTrue(Cartesian.Mu.TYPE_TOKEN.isSupertypeOf(FunctionArrow.FunctionArrowInstance.Mu.TYPE_TOKEN));
  }

  @Test
  void typedOpticUpCastRejectsInsufficientProofTokens() {
    TypedOptic<Tuple2<Integer, String>, Tuple2<Integer, String>, Integer, Integer> product =
        TypedOptic.proj1(Types.witness(Integer.class), Types.witness(String.class), Types.witness(Integer.class));

    assertTrue(product.upCast(FunctionArrow.FunctionArrowInstance.Mu.TYPE_TOKEN).isDefined());
    assertFalse(product.upCast(Profunctor.Mu.TYPE_TOKEN).isDefined());
  }

  @Test
  void typedOpticApplyExecutesAdapterProductLensSumAndTraversalSpines() {
    record Box(int value) {}

    Type<Integer> intType = Types.witness(Integer.class);
    Type<String> stringType = Types.witness(String.class);
    App2<FunctionArrow.Mu, Integer, Integer> plusOne = FunctionArrow.of(value -> value + 1);

    App2<FunctionArrow.Mu, Integer, String> adapterResult =
        TypedOptic.adapter(intType, stringType)
            .apply(
                FunctionArrow.FunctionArrowInstance.Mu.TYPE_TOKEN,
                FunctionArrow.instance(),
                FunctionArrow.of(Object::toString));
    assertEquals("1", FunctionArrow.<Integer, String>unbox(adapterResult).apply(1));

    App2<FunctionArrow.Mu, Tuple2<Integer, String>, Tuple2<Integer, String>> productResult =
        TypedOptic.proj1(intType, stringType, intType)
            .apply(FunctionArrow.FunctionArrowInstance.Mu.TYPE_TOKEN, FunctionArrow.instance(), plusOne);
    assertEquals(Tuple2.of(2, "a"), FunctionArrow.unbox(productResult).apply(Tuple2.of(1, "a")));

    var value = PLens.<Box, Box, Integer, Integer>of(Box::value, (box, next) -> new Box(next));
    App2<FunctionArrow.Mu, Box, Box> lensResult =
        PointFreeOptic.lens(LensPath.of("value", value), Types.witness(Box.class), intType)
            .typed()
            .apply(FunctionArrow.FunctionArrowInstance.Mu.TYPE_TOKEN, FunctionArrow.instance(), plusOne);
    assertEquals(new Box(2), FunctionArrow.unbox(lensResult).apply(new Box(1)));

    App2<FunctionArrow.Mu, Either<Integer, String>, Either<Integer, String>> sumResult =
        TypedOptic.inj1(intType, stringType, intType)
            .apply(FunctionArrow.FunctionArrowInstance.Mu.TYPE_TOKEN, FunctionArrow.instance(), plusOne);
    assertEquals(Either.left(2), FunctionArrow.unbox(sumResult).apply(Either.left(1)));
    assertEquals(Either.right("a"), FunctionArrow.unbox(sumResult).apply(Either.right("a")));

    App2<FunctionArrow.Mu, List<Integer>, List<Integer>> traversalResult =
        TypedOptic.list(intType, intType)
            .apply(FunctionArrow.FunctionArrowInstance.Mu.TYPE_TOKEN, FunctionArrow.instance(), plusOne);
    assertEquals(List.of(2, 3, 4), FunctionArrow.unbox(traversalResult).apply(List.of(1, 2, 3)));
  }
}

