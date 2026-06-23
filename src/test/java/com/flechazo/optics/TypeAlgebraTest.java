package com.flechazo.optics;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOpticTypes;
import com.flechazo.hkt.functions.ProductSide;
import com.flechazo.hkt.functions.SumSide;
import com.flechazo.hkt.type.RecursiveTypeFamily;
import com.flechazo.hkt.type.Sum;
import com.flechazo.hkt.type.TaggedChoice;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.TypeRewriteRule;
import com.flechazo.hkt.type.TypeSubstitution;
import com.flechazo.hkt.type.TypeUnifier;
import com.flechazo.hkt.type.Types;
import com.google.common.reflect.TypeToken;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeAlgebraTest {
  @Test
  void structuredTypesAreTypeAlgebraNotParameterizedRuntimeTokens() {
    Type<?> integer = Types.witness(Integer.class);
    Type<?> string = Types.witness(String.class);

    assertEquals(TypeToken.of(Integer.class), integer.runtimeWitness().get());
    assertTrue(Types.and(integer, string).runtimeWitness().isEmpty());
    assertTrue(Types.or(integer, string).runtimeWitness().isEmpty());
    assertTrue(Types.list(integer).runtimeWitness().isEmpty());
    assertTrue(Types.map(string, integer).runtimeWitness().isEmpty());
    assertTrue(Types.optional(integer).runtimeWitness().isEmpty());
  }

  @Test
  void substitutionRewritesVariablesAndRecursivePointsInsideCompositeTypes() {
    Type<?> variable = Types.variable("a");
    RecursiveTypeFamily family = new RecursiveTypeFamily("Tree", 1, ignored -> Types.id(0));
    Type<?> point = family.recursivePoint(0);
    Type<?> source =
        Types.named(
            "Node",
            Types.and(
                Types.field("value", variable),
                Types.field("next", Types.optional(point))));

    TypeSubstitution substitution =
        TypeSubstitution.variable("a", Types.witness(String.class))
            .plusRecursivePoint(family.recursivePoint(0), Types.witness(Integer.class));

    Type<?> expected =
        Types.named(
            "Node",
            Types.and(
                Types.field("value", Types.witness(String.class)),
                Types.field("next", Types.optional(Types.witness(Integer.class)))));

    assertEquals(expected, source.substitute(substitution));
  }

  @Test
  void functionTypesUnifyOnlyWhenMiddleTypesMatch() {
    Type<?> integer = Types.witness(Integer.class);
    Type<?> string = Types.witness(String.class);
    Type<?> bool = Types.witness(Boolean.class);
    Type<?> variable = Types.variable("middle");

    assertTrue(TypeUnifier.unify(Types.function(string, variable), Types.function(string, integer)).isDefined());
    assertTrue(TypeUnifier.unify(Types.function(string, integer), Types.function(bool, integer)).isEmpty());
  }

  @Test
  void taggedChoicesVariantsAndOpticTypesCarryStructuredMetadata() {
    Type<String> keyType = Types.witness(String.class);
    Type<Integer> valueType = Types.witness(Integer.class);
    Type<?> tagged = Types.taggedChoiceType("choice", keyType, Map.of("value", valueType));
    Type<?> variant = Types.variantType("Result", new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>(
        Map.of("ok", valueType, "error", keyType)));
    PointFreeOpticTypes opticTypes = PointFreeOpticTypes.endomorphic(tagged, variant);

    assertEquals(tagged, opticTypes.source());
    assertEquals(variant, opticTypes.focus());
    assertEquals(tagged, opticTypes.sourceType());
    assertTrue(variant.runtimeWitness().isEmpty());
  }

  @Test
  void typeRewriteStrategiesTraverseAndNormalizeTypeTrees() {
    Type<?> integer = Types.witness(Integer.class);
    Type<?> string = Types.witness(String.class);
    Type<?> source = Types.optional(Types.optional(Types.and(Types.variable("a"), string)));
    TypeRewriteRule removeNestedOptional =
        type ->
            type instanceof Sum.SumType<?, ?> outer
                    && outer.right().equals(Types.UNIT)
                    && outer.left() instanceof Sum.SumType<?, ?> inner
                    && inner.right().equals(Types.UNIT)
                ? Maybe.some(Types.optional(inner.left()))
                : Maybe.none();
    TypeRewriteRule replaceVariable =
        type ->
            type instanceof Type.VariableType<?> variable && variable.name().equals("a")
                ? Maybe.some(integer)
                : Maybe.none();

    Type<?> rewritten =
        TypeRewriteRule.bottomUp(TypeRewriteRule.choice(removeNestedOptional, replaceVariable))
            .rewrite(source)
            .get();

    assertEquals(Types.optional(Types.and(integer, string)), rewritten);
  }

  @Test
  void typeUnifierBuildsSubstitutionsAndRejectsRecursiveVariableBindings() {
    Type<?> left = Types.and(Types.variable("a"), Types.optional(Types.variable("a")));
    Type<?> right = Types.and(Types.witness(Integer.class), Types.optional(Types.witness(Integer.class)));

    TypeSubstitution substitution = TypeUnifier.unify(left, right).get();

    assertEquals(Types.witness(Integer.class), Types.variable("a").substitute(substitution));
    assertTrue(TypeUnifier.unify(Types.variable("a"), Types.optional(Types.variable("a"))).isEmpty());
    assertTrue(TypeUnifier.unify(Types.or(Types.variable("a"), Types.witness(String.class)), right).isEmpty());
  }

  @Test
  void recursiveTypeFamiliesExposeFixedPointSlotsAndTemplates() {
    RecursiveTypeFamily family =
        new RecursiveTypeFamily(
            "Tree",
            1,
            ignored -> Types.or(
                Types.field("leaf", Types.constType(Types.witness(Integer.class))),
                Types.field("node", Types.id(0))));

    assertEquals("Tree", family.recursivePoint(0).family().name());
    assertEquals(Types.id(0), family.recursivePoint(0).template());
    assertEquals(family.apply(0).unfold(), family.template(0).apply(family).apply(0));
  }

  @Test
  void pointFreeOpticFactoriesStoreStructuralTypesNotOnlyWitnesses() {
    Type<?> integer = Types.witness(Integer.class);
    Type<?> string = Types.witness(String.class);

    assertEquals(
        Types.and(integer, string),
        PointFreeOptic.product(ProductSide.FIRST, TypeToken.of(Integer.class), TypeToken.of(String.class))
            .types()
            .source());
    assertEquals(
        Types.or(integer, string),
        PointFreeOptic.sum(SumSide.RIGHT, TypeToken.of(Integer.class), TypeToken.of(String.class))
            .types()
            .source());
    assertEquals(
        Types.list(integer),
        PointFreeOptic.list(TypeToken.of(Integer.class)).types().source());
    assertInstanceOf(
        TaggedChoice.TaggedChoiceType.class,
        PointFreeOptic.tagged("value", TypeToken.of(String.class), TypeToken.of(Integer.class))
            .types()
            .source());
  }
}
