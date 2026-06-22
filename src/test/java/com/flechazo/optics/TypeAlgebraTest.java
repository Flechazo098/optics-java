package com.flechazo.optics;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Pair;
import com.flechazo.hkt.functions.PointFreeOpticTypes;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.ProductSide;
import com.flechazo.hkt.functions.SumSide;
import com.flechazo.hkt.type.RecursiveTypeFamily;
import com.flechazo.hkt.type.TypeExpr;
import com.flechazo.hkt.type.TypeRef;
import com.flechazo.hkt.type.TypeRewriteRule;
import com.flechazo.hkt.type.TypeSubstitution;
import com.flechazo.hkt.type.TypeUnifier;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeAlgebraTest {
  @Test
  void structuredTypesExposeRuntimeWitnessesWhenAllPartsHaveWitnesses() {
    TypeExpr intType = TypeRef.of(Integer.class).expr();
    TypeExpr stringType = TypeRef.of(String.class).expr();

    assertEquals(
        TypeRef.parameterized(Pair.class, TypeRef.of(Integer.class), TypeRef.of(String.class)),
        TypeExpr.product(intType, stringType).witness().get());
    assertEquals(
        TypeRef.parameterized(Either.class, TypeRef.of(Integer.class), TypeRef.of(String.class)),
        TypeExpr.sum(intType, stringType).witness().get());
    assertEquals(
        TypeRef.parameterized(List.class, TypeRef.of(Integer.class)),
        TypeExpr.list(intType).witness().get());
    assertEquals(
        TypeRef.parameterized(Map.class, TypeRef.of(String.class), TypeRef.of(Integer.class)),
        TypeExpr.map(stringType, intType).witness().get());
    assertEquals(
        TypeRef.parameterized(Maybe.class, TypeRef.of(Integer.class)),
        TypeExpr.optional(intType).witness().get());
  }

  @Test
  void substitutionRewritesVariablesAndRecursiveSlotsInsideCompositeTypes() {
    TypeExpr variable = TypeExpr.variable("a");
    TypeExpr.RecursiveSlot slot = new TypeExpr.RecursiveSlot("Tree", 0, Maybe.none());
    TypeExpr source =
        TypeExpr.record(
            "Node",
            List.of(
                TypeExpr.field("value", variable),
                TypeExpr.field("next", TypeExpr.optional(slot))));

    TypeSubstitution substitution =
        TypeSubstitution.variable("a", TypeRef.of(String.class).expr())
            .plusRecursiveSlot(slot, TypeRef.of(Integer.class).expr());

    TypeExpr expected =
        TypeExpr.record(
            "Node",
            List.of(
                TypeExpr.field("value", TypeRef.of(String.class).expr()),
                TypeExpr.field("next", TypeExpr.optional(TypeRef.of(Integer.class).expr()))));

    assertEquals(expected, source.substitute(substitution));
  }

  @Test
  void functionTypesComposeOnlyWhenMiddleTypesMatch() {
    TypeExpr intType = TypeRef.of(Integer.class).expr();
    TypeExpr stringType = TypeRef.of(String.class).expr();
    TypeExpr boolType = TypeRef.of(Boolean.class).expr();

    TypeExpr.FunctionType parse = new TypeExpr.FunctionType(stringType, intType);
    TypeExpr.FunctionType positive = new TypeExpr.FunctionType(intType, boolType);
    TypeExpr.FunctionType invalid = new TypeExpr.FunctionType(boolType, stringType);

    assertEquals(new TypeExpr.FunctionType(stringType, boolType), positive.compose(parse).get());
    assertTrue(positive.compose(invalid).isEmpty());
    assertEquals(
        TypeRef.parameterized(Function.class, TypeRef.of(String.class), TypeRef.of(Integer.class)),
        parse.witness().get());
  }

  @Test
  void taggedChoicesVariantsAndOpticTypesCarryStructuredMetadata() {
    TypeExpr keyType = TypeRef.of(String.class).expr();
    TypeExpr valueType = TypeRef.of(Integer.class).expr();
    TypeExpr tagged =
        TypeExpr.taggedChoice("choice", keyType, Map.of("value", valueType), TypeRef.of(Pair.class));
    TypeExpr variant =
        TypeExpr.variant(
            "Result",
            List.of(new TypeExpr.VariantCase("ok", valueType), new TypeExpr.VariantCase("error", keyType)));
    PointFreeOpticTypes opticTypes = PointFreeOpticTypes.endomorphic(tagged, variant);

    assertEquals(tagged, opticTypes.source());
    assertEquals(variant, opticTypes.focus());
    assertEquals(TypeRef.of(Pair.class), opticTypes.sourceType());
    assertTrue(variant.witness().isEmpty());
  }

  @Test
  void typeRewriteStrategiesTraverseAndNormalizeTypeTrees() {
    TypeExpr integer = TypeRef.of(Integer.class).expr();
    TypeExpr string = TypeRef.of(String.class).expr();
    TypeExpr source = TypeExpr.optional(TypeExpr.optional(TypeExpr.product(TypeExpr.variable("a"), string)));
    TypeRewriteRule removeNestedMaybe =
        expression ->
            expression instanceof TypeExpr.OptionalOf(TypeExpr.OptionalOf(TypeExpr value))
                ? Maybe.some(TypeExpr.optional(value))
                : Maybe.none();
    TypeRewriteRule replaceVariable =
        expression ->
            expression instanceof TypeExpr.Variable(String name) && name.equals("a")
                ? Maybe.some(integer)
                : Maybe.none();

    TypeExpr rewritten =
        TypeRewriteRule.bottomUp(TypeRewriteRule.choice(removeNestedMaybe, replaceVariable))
            .rewrite(source)
            .get();

    assertEquals(TypeExpr.optional(TypeExpr.product(integer, string)), rewritten);
  }

  @Test
  void typeUnifierBuildsSubstitutionsAndRejectsRecursiveVariableBindings() {
    TypeExpr left = TypeExpr.product(TypeExpr.variable("a"), TypeExpr.optional(TypeExpr.variable("a")));
    TypeExpr right =
        TypeExpr.product(TypeRef.of(Integer.class).expr(), TypeExpr.optional(TypeRef.of(Integer.class).expr()));

    TypeSubstitution substitution = TypeUnifier.unify(left, right).get();

    assertEquals(TypeRef.of(Integer.class).expr(), TypeExpr.variable("a").substitute(substitution));
    assertTrue(TypeUnifier.unify(TypeExpr.variable("a"), TypeExpr.optional(TypeExpr.variable("a"))).isEmpty());
    assertTrue(TypeUnifier.unify(TypeExpr.sum(TypeExpr.variable("a"), TypeRef.of(String.class).expr()), right).isEmpty());
  }

  @Test
  void recursiveTypeFamiliesExposeFixedPointSlotsAndUnfoldBodies() {
    TypeExpr leaf = TypeExpr.record("Leaf", List.of(TypeExpr.field("value", TypeRef.of(Integer.class).expr())));
    TypeExpr node =
        TypeExpr.record(
            "Node",
            List.of(
                TypeExpr.field("left", TypeExpr.recursiveSlot("Tree", 0)),
                TypeExpr.field("right", TypeExpr.recursiveSlot("Tree", 0))));
    RecursiveTypeFamily family =
        RecursiveTypeFamily.builder("Tree")
            .slot(TypeRef.of(Object.class), TypeExpr.sum(leaf, node))
            .build();

    TypeExpr.RecursiveSlot slot = family.slotRef(0);

    assertEquals(TypeExpr.sum(leaf, node), family.unfold(slot));
    assertTrue(family.body(0).containsRecursiveSlot(slot));
    assertEquals(family.body(0), slot.substitute(family.slotSubstitution()));
  }

  @Test
  void pointFreeOpticFactoriesStoreStructuralTypesNotOnlyWitnesses() {
    TypeExpr integer = TypeRef.of(Integer.class).expr();
    TypeExpr string = TypeRef.of(String.class).expr();

    assertEquals(
        TypeExpr.product(integer, string),
        PointFreeOptic.product(ProductSide.FIRST, TypeRef.of(Integer.class), TypeRef.of(String.class))
            .types()
            .get()
            .source());
    assertEquals(
        TypeExpr.sum(integer, string),
        PointFreeOptic.sum(SumSide.RIGHT, TypeRef.of(Integer.class), TypeRef.of(String.class))
            .types()
            .get()
            .source());
    assertEquals(
        TypeExpr.list(integer),
        PointFreeOptic.list(TypeRef.of(Integer.class)).types().get().source());
      assertInstanceOf(TypeExpr.TaggedChoice.class, PointFreeOptic.tagged("value", TypeRef.of(String.class), TypeRef.of(Integer.class))
              .types()
              .get()
              .source());
  }
}
