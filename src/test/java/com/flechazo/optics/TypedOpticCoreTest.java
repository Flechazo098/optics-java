package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.ProfunctorBound;
import com.flechazo.hkt.functions.CompositePointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOpticTypes;
import com.flechazo.hkt.functions.ProductSide;
import com.flechazo.hkt.type.TypeExpr;
import com.flechazo.hkt.type.TypeRef;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypedOpticCoreTest {
  @Test
  void optimizerOpticsCarryFourStructuralTypeParameters() {
    TypeExpr source = TypeRef.of(SourceBox.class).expr();
    TypeExpr target = TypeRef.of(TargetBox.class).expr();
    TypeExpr focus = TypeRef.of(Integer.class).expr();
    TypeExpr replacement = TypeRef.of(String.class).expr();

    PointFreeOptic<SourceBox, TargetBox, Integer, String> optic =
        new CompositePointFreeOptic<>(
            List.of(),
            com.flechazo.hkt.Maybe.some(new PointFreeOpticTypes(source, target, focus, replacement)));

    PointFreeOpticTypes types = optic.types().get();

    assertEquals(source, types.source());
    assertEquals(target, types.target());
    assertEquals(focus, types.focus());
    assertEquals(replacement, types.replacement());
    assertEquals(TypeRef.of(SourceBox.class), types.sourceType());
    assertEquals(TypeRef.of(TargetBox.class), types.targetType());
  }

  @Test
  void optimizerOpticSpinesExposeProfunctorBoundTokens() {
    PointFreeOptic<?, ?, ?, ?> product = PointFreeOptic.product(ProductSide.FIRST);

    assertTrue(product.bounds().contains(ProfunctorBound.CARTESIAN));
    assertTrue(ProfunctorBound.CARTESIAN.isSatisfiedBy(com.flechazo.hkt.FunctionArrow.FunctionArrowInstance.class));
  }
}
