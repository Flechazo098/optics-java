package com.flechazo.optics;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOpticTypes;
import com.flechazo.hkt.functions.OpticApp;
import com.flechazo.hkt.functions.PointFree;
import com.flechazo.hkt.functions.ProductSide;
import com.flechazo.hkt.functions.SumSide;
import com.flechazo.hkt.functions.TypedOptic;
import com.flechazo.hkt.type.Check;
import com.flechazo.hkt.type.RecursivePoint;
import com.flechazo.hkt.type.RecursiveTypeFamily;
import com.flechazo.hkt.type.TaggedChoice;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.TypeRewriteResult;
import com.flechazo.hkt.type.TypeFamily;
import com.flechazo.hkt.type.TypeRewriteRule;
import com.flechazo.hkt.type.TypeSubstitution;
import com.flechazo.hkt.type.TypeTemplate;
import com.flechazo.hkt.type.TypeUnifier;
import com.flechazo.hkt.type.Types;
import com.google.common.reflect.TypeToken;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    assertTrue(Types.maybe(integer).runtimeWitness().isEmpty());
    assertTrue(Types.validated(string, integer).runtimeWitness().isEmpty());
    assertInstanceOf(Types.MaybeType.class, Types.maybe(integer));
    assertInstanceOf(Types.ValidatedType.class, Types.validated(string, integer));
    assertEquals(
        Types.maybe(integer),
        Types.maybe(Types.constType(integer)).apply(TypeFamily.constant(string)).apply(0));
    assertEquals(
        Types.validated(string, integer),
        Types.validated(Types.constType(string), Types.constType(integer))
            .apply(TypeFamily.constant(Types.witness(Boolean.class)))
            .apply(0));
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
                Types.field("next", Types.maybe(point))));

    TypeSubstitution substitution =
        TypeSubstitution.variable("a", Types.witness(String.class))
            .plusRecursivePoint(family.recursivePoint(0), Types.witness(Integer.class));

    Type<?> expected =
        Types.named(
            "Node",
            Types.and(
                Types.field("value", Types.witness(String.class)),
                Types.field("next", Types.maybe(Types.witness(Integer.class)))));

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
  void taggedChoiceTypesFindRewriteAndApplyTypedBranchOptics() {
    Type<String> keyType = Types.witness(String.class);
    Type<Integer> intType = Types.witness(Integer.class);
    Type<Long> longType = Types.witness(Long.class);
    Type<String> stringType = Types.witness(String.class);
    TaggedChoice.TaggedChoiceType<String> source =
        Types.taggedChoiceType("choice", keyType, Map.of("value", intType, "error", stringType));
    TaggedChoice.TaggedChoiceType<String> target = source.replaceChoice("value", longType).get();

    TypedOptic<Tuple2<String, ?>, Tuple2<String, ?>, Integer, Long> optic =
        source.branchOptic("value", intType, longType).get();

    assertEquals(intType, source.choiceType("value").get());
    assertEquals(longType, target.choiceType("value").get());
    assertEquals(stringType, target.choiceType("error").get());
    assertEquals(source, optic.sType());
    assertEquals(target, optic.tType());
    assertEquals(intType, optic.aType());
    assertEquals(longType, optic.bType());
    assertEquals(Tuple2.of("value", 2L), optic.modify(value -> value + 1L, Tuple2.of("value", 1)));
    assertEquals(Tuple2.of("error", "bad"), optic.modify(value -> value + 1L, Tuple2.of("error", "bad")));
    assertTrue(source.choiceType("missing").isEmpty());
    assertTrue(source.replaceChoice("missing", longType).isEmpty());
    assertTrue(source.branchOptic("value", stringType, longType).isEmpty());
  }

  @Test
  void typeRewriteStrategiesTraverseAndNormalizeTypeTrees() {
    Type<?> integer = Types.witness(Integer.class);
    Type<?> string = Types.witness(String.class);
    Type<?> source = Types.maybe(Types.maybe(Types.and(Types.variable("a"), string)));
    TypeRewriteRule removeNestedMaybe =
        TypeRewriteRule.typeOnly(
        type ->
            type instanceof Types.MaybeType<?> outer
                    && outer.value() instanceof Types.MaybeType<?> inner
                ? Maybe.some(Types.maybe(inner.value()))
                : Maybe.none());
    TypeRewriteRule replaceVariable =
        TypeRewriteRule.typeOnly(
        type ->
            type instanceof Type.VariableType<?> variable && variable.name().equals("a")
                ? Maybe.some(integer)
                : Maybe.none());

    TypeRewriteResult<?, ?> rewritten =
        TypeRewriteRule.bottomUp(TypeRewriteRule.choice(removeNestedMaybe, replaceVariable))
            .rewrite(source)
            .get();

    assertEquals(source, rewritten.sourceType());
    assertEquals(Types.maybe(Types.and(integer, string)), rewritten.targetType());
    assertTrue(rewritten.view().isEmpty());
    assertTrue(rewritten.hasRecursiveDependencyEvidence());
  }

  @Test
  @SuppressWarnings("unchecked")
  void typeRewriteLowersExecutableProductChildRewriteThroughTypedOptic() {
    Type<Integer> integer = Types.witness(Integer.class);
    Type<Long> longType = Types.witness(Long.class);
    Type<String> string = Types.witness(String.class);
    Type<Tuple2<Integer, String>> source = Types.and(integer, string);
    Type<Tuple2<Long, String>> target = Types.and(longType, string);
    TypeRewriteRule widenInteger =
        TypeRewriteRule.result(
            type ->
                type.equals(integer)
                    ? Maybe.some(
                        TypeRewriteResult.executable(
                            integer,
                            longType,
                            PointFree.fn("intToLong", value -> value.longValue() + 1L, integer, longType)))
                    : Maybe.none());

    TypeRewriteResult<?, ?> result = TypeRewriteRule.all(widenInteger).rewrite(source).get();

    assertEquals(source, result.sourceType());
    assertEquals(target, result.targetType());
    assertTrue(result.hasExecutableView());
    assertTrue((Object) result.view().get() instanceof OpticApp<?, ?, ?, ?> app
        && app.optic().sourceType().equals(source)
        && app.optic().targetType().equals(target)
        && app.optic().focusType().equals(integer)
        && app.optic().replacementType().equals(longType));
    Function<Tuple2<Integer, String>, Tuple2<Long, String>> function =
        (Function<Tuple2<Integer, String>, Tuple2<Long, String>>) result.view().get().eval();
    assertEquals(Tuple2.of(2L, "x"), function.apply(Tuple2.of(1, "x")));
  }

  @Test
  @SuppressWarnings("unchecked")
  void typeRewriteLowersTaggedChoiceBranchRewriteThroughTypedOptic() {
    Type<String> keyType = Types.witness(String.class);
    Type<Integer> integer = Types.witness(Integer.class);
    Type<Long> longType = Types.witness(Long.class);
    Type<String> string = Types.witness(String.class);
    TaggedChoice.TaggedChoiceType<String> source =
        Types.taggedChoiceType("choice", keyType, Map.of("value", integer, "error", string));
    TaggedChoice.TaggedChoiceType<String> target =
        Types.taggedChoiceType("choice", keyType, Map.of("value", longType, "error", string));
    TypeRewriteRule widenInteger =
        TypeRewriteRule.result(
            type ->
                type.equals(integer)
                    ? Maybe.some(
                        TypeRewriteResult.executable(
                            integer,
                            longType,
                            PointFree.fn("intToLong", value -> value.longValue() + 1L, integer, longType)))
                    : Maybe.none());

    TypeRewriteResult<?, ?> result = TypeRewriteRule.all(widenInteger).rewrite(source).get();

    assertEquals(source, result.sourceType());
    assertEquals(target, result.targetType());
    assertTrue(result.hasExecutableView());
    assertTrue((Object) result.view().get() instanceof OpticApp<?, ?, ?, ?> app
        && app.optic().sourceType().equals(source)
        && app.optic().targetType().equals(target)
        && app.optic().focusType().equals(integer)
        && app.optic().replacementType().equals(longType));
    Function<Tuple2<String, ?>, Tuple2<String, ?>> function =
        (Function<Tuple2<String, ?>, Tuple2<String, ?>>) result.view().get().eval();
    assertEquals(Tuple2.of("value", 2L), function.apply(Tuple2.of("value", 1)));
    assertEquals(Tuple2.of("error", "bad"), function.apply(Tuple2.of("error", "bad")));
  }

  @Test
  void checkedTypesPruneChildRewriteWhenFamilyIndexDoesNotMatch() {
    Type<Integer> integer = Types.witness(Integer.class);
    Type<String> string = Types.witness(String.class);
    Type<Boolean> bool = Types.witness(Boolean.class);
    TypeRewriteRule rewriteInteger =
        TypeRewriteRule.typeOnly(
            type -> type.equals(integer) ? Maybe.some(bool) : Maybe.none());
    Type<?> matching = Types.checkedType("slot", 1, 1, Types.and(integer, string));
    Type<?> mismatching = Types.checkedType("slot", 0, 1, Types.and(integer, string));

    assertEquals(
        Types.checkedType("slot", 1, 1, Types.and(bool, string)),
        TypeRewriteRule.bottomUp(rewriteInteger).rewrite(matching).get().targetType());
    assertTrue(TypeRewriteRule.all(rewriteInteger).rewrite(mismatching).isEmpty());
    assertTrue(TypeRewriteRule.bottomUp(rewriteInteger).rewrite(mismatching).isEmpty());
    assertEquals(mismatching, TypeRewriteRule.bottomUp(rewriteInteger).rewriteOrSame(mismatching));
  }

  @Test
  void checkedTemplateApplyPreservesActualAndExpectedIndexes() {
    Type<Integer> integer = Types.witness(Integer.class);
    TypeTemplate checked = Types.checked("slot", 1, Types.constType(integer));
    Check.CheckType<?> actualZero = (Check.CheckType<?>) checked.apply(TypeFamily.constant(integer)).apply(0);
    Check.CheckType<?> actualOne = (Check.CheckType<?>) checked.apply(TypeFamily.constant(integer)).apply(1);

    assertEquals(0, actualZero.index());
    assertEquals(1, actualZero.expectedIndex());
    assertTrue(!actualZero.matchesIndex());
    assertEquals(1, actualOne.index());
    assertEquals(1, actualOne.expectedIndex());
    assertTrue(actualOne.matchesIndex());
    assertEquals(checked, actualZero.template());
    assertEquals(checked, actualOne.template());
  }

  @Test
  void typeUnifierBuildsSubstitutionsAndRejectsRecursiveVariableBindings() {
    Type<?> left = Types.and(Types.variable("a"), Types.maybe(Types.variable("a")));
    Type<?> right = Types.and(Types.witness(Integer.class), Types.maybe(Types.witness(Integer.class)));
    Type<?> validatedLeft = Types.validated(Types.variable("e"), Types.variable("a"));
    Type<?> validatedRight = Types.validated(Types.witness(String.class), Types.witness(Integer.class));

    TypeSubstitution substitution = TypeUnifier.unify(left, right).get();
    TypeSubstitution validatedSubstitution = TypeUnifier.unify(validatedLeft, validatedRight).get();

    assertEquals(Types.witness(Integer.class), Types.variable("a").substitute(substitution));
    assertEquals(Types.witness(String.class), Types.variable("e").substitute(validatedSubstitution));
    assertEquals(Types.witness(Integer.class), Types.variable("a").substitute(validatedSubstitution));
    assertTrue(TypeUnifier.unify(Types.variable("a"), Types.maybe(Types.variable("a"))).isEmpty());
    assertTrue(TypeUnifier.unify(Types.variable("a"), Types.validated(Types.witness(String.class), Types.variable("a"))).isEmpty());
    assertTrue(TypeUnifier.unify(Types.or(Types.variable("a"), Types.witness(String.class)), right).isEmpty());
  }

  @Test
  void emptyTypeSubstitutionIsSharedAndDoesNotRebuildGroundTypes() {
    Type<?> integer = Types.witness(Integer.class);
    Type<?> string = Types.witness(String.class);
    Type<?> ground = Types.function(Types.and(integer, string), Types.maybe(integer));
    TypeSubstitution empty = TypeSubstitution.empty();

    assertSame(empty, TypeSubstitution.empty());
    assertTrue(empty.isEmpty());
    assertSame(ground, ground.substitute(empty));
    assertSame(integer, integer.substitute(empty));
    assertEquals(string, Types.variable("a").substitute(TypeSubstitution.variable("a", string)));
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
  void fieldLookupUsesUnifiedTypedFinderPath() {
    Type<Integer> integer = Types.witness(Integer.class);
    Type<Long> longType = Types.witness(Long.class);
    Type<String> string = Types.witness(String.class);
    Type<Tuple2<Integer, String>> source =
        Types.and(Types.field("value", integer), Types.field("name", string));
    Type<Tuple2<Long, String>> target =
        Types.and(Types.field("value", longType), Types.field("name", string));

    Maybe<Type<?>> fieldType = source.findFieldType("value");
    Maybe<TypedOptic<Tuple2<Integer, String>, ?, Integer, Long>> optic =
        source.findType(integer, longType, false);

    assertEquals(integer, fieldType.get());
    assertTrue(source.findFieldType("missing").isEmpty());
    assertTrue(optic.isDefined());
    assertEquals(source, optic.get().sType());
    assertEquals(target, optic.get().tType());
    assertEquals(integer, optic.get().aType());
    assertEquals(longType, optic.get().bType());
  }

  @Test
  void recursivePointFindTypeInChildrenRepairsOuterMuTypes() {
    Type<Integer> integer = Types.witness(Integer.class);
    Type<Long> longType = Types.witness(Long.class);
    RecursiveTypeFamily family =
        new RecursiveTypeFamily(
            "Tree",
            1,
            ignored -> Types.or(
                Types.field("leaf", Types.constType(integer)),
                Types.field("node", Types.id(0))));
    RecursivePoint.RecursivePointType<?> sourcePoint = family.recursivePoint(0);

    Maybe<TypedOptic<Object, ?, Integer, Long>> optic =
        ((Type<Object>) sourcePoint).findType(integer, longType, true);

    assertTrue(optic.isDefined());
    assertEquals(sourcePoint, optic.get().sType());
    RecursivePoint.RecursivePointType<?> targetPoint =
        assertInstanceOf(RecursivePoint.RecursivePointType.class, optic.get().tType());
    assertEquals(
        Types.or(Types.field("leaf", longType), Types.field("node", targetPoint)),
        targetPoint.unfold());
    assertEquals(integer, optic.get().aType());
    assertEquals(longType, optic.get().bType());
  }

  @Test
  void recursivePointAllRewritesThroughTemplateAndKeepsMuBoundary() {
    Type<Integer> integer = Types.witness(Integer.class);
    Type<Long> longType = Types.witness(Long.class);
    RecursiveTypeFamily family =
        new RecursiveTypeFamily(
            "Tree",
            1,
            ignored -> Types.or(
                Types.field("leaf", Types.constType(integer)),
                Types.field("node", Types.id(0))));
    RecursivePoint.RecursivePointType<?> sourcePoint = family.recursivePoint(0);
    TypeRewriteRule widenInteger =
        TypeRewriteRule.result(
            type ->
                type.equals(integer)
                    ? Maybe.some(
                        TypeRewriteResult.executable(
                            integer,
                            longType,
                            PointFree.fn("intToLong", value -> value.longValue(), integer, longType)))
                    : Maybe.none());

    TypeRewriteResult<?, ?> result = TypeRewriteRule.all(widenInteger).rewrite(sourcePoint).get();

    assertEquals(sourcePoint, result.sourceType());
    RecursivePoint.RecursivePointType<?> targetPoint =
        assertInstanceOf(RecursivePoint.RecursivePointType.class, result.targetType());
    assertEquals(
        Types.or(Types.field("leaf", longType), Types.field("node", targetPoint)),
        targetPoint.unfold());
    assertTrue(result.hasExecutableView());
    assertTrue(result.recursiveDependencies().get().get(0));
    assertTrue((Object) result.view().get() instanceof OpticApp<?, ?, ?, ?> app
        && app.optic().sourceType().equals(sourcePoint)
        && app.optic().targetType().equals(targetPoint)
        && app.optic().focusType().equals(sourcePoint.unfold())
        && app.optic().replacementType().equals(targetPoint.unfold()));
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
