package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.Cartesian;
import com.flechazo.hkt.FunctionArrow;
import com.flechazo.hkt.Profunctor;
import com.flechazo.hkt.functions.AdapterOpticElement;
import com.flechazo.hkt.functions.CompositePointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOpticTypes;
import com.flechazo.hkt.functions.ProductSide;
import com.flechazo.hkt.functions.TypedOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
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
}

