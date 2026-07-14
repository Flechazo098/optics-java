package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.Cartesian;
import com.flechazo.hkt.Cocartesian;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.functions.AlgebraPlan;
import com.flechazo.hkt.functions.Bang;
import com.flechazo.hkt.functions.CataPlan;
import com.flechazo.hkt.functions.Comp;
import com.flechazo.hkt.functions.CompositePointFreeOptic;
import com.flechazo.hkt.functions.FoldQuery;
import com.flechazo.hkt.functions.Id;
import com.flechazo.hkt.functions.LensPath;
import com.flechazo.hkt.functions.OpticApp;
import com.flechazo.hkt.functions.OpticLowering;
import com.flechazo.hkt.Traversing;
import com.flechazo.hkt.functions.PointFreeOpticKind;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.ProductOpticElement;
import com.flechazo.hkt.functions.PointFree;
import com.flechazo.hkt.functions.PointFreeOptimizer;
import com.flechazo.hkt.functions.PointFreeRule;
import com.flechazo.hkt.functions.PointFreeRules;
import com.flechazo.hkt.functions.ProductSide;
import com.flechazo.hkt.functions.RecursiveFamily;
import com.flechazo.hkt.functions.RewriteResult;
import com.flechazo.hkt.functions.SumOpticElement;
import com.flechazo.hkt.functions.SumSide;
import com.flechazo.hkt.functions.TypedOptic;
import com.flechazo.hkt.type.Type;
import com.flechazo.hkt.type.Types;
import com.google.common.reflect.TypeToken;
import com.flechazo.hkt.functions.Value;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class PointFreeTest {

  @Test
  void pointFreeOpticsExposeElementsPrefixesAndOutermostOptic() {
    record Account(String name, Address address) {}
    var address =
        PLens.<Account, Account, Address, Address>of(Account::address, (account, next) -> new Account(account.name(), next));
    var city = PLens.<Address, Address, String, String>of(Address::city, (addr, next) -> new Address(next, addr.zip()));
    var zip = PLens.<Address, Address, Integer, Integer>of(Address::zip, (addr, next) -> new Address(addr.city(), next));
    PointFreeOptic<Account, Account, String, String> cityOptic = PointFreeOptic.lens(LensPath.of("address", address).andThen("city", city));
    PointFreeOptic<Account, Account, Integer, Integer> zipOptic = PointFreeOptic.lens(LensPath.of("address", address).andThen("zip", zip));
    PointFreeOptic<Tuple2<Integer, String>, Tuple2<Integer, String>, Object, Object> firstOptic = PointFreeOptic.product(ProductSide.FIRST);
    PointFreeOptic<Either<Integer, String>, Either<Integer, String>, Object, Object> rightOptic = PointFreeOptic.sum(SumSide.RIGHT);

    assertEquals(2, cityOptic.size());
    assertTrue(cityOptic.containsOnly(PointFreeOpticKind.LENS));
    assertTrue(cityOptic.bounds().contains(Cartesian.Mu.TYPE_TOKEN));
    assertEquals(1, cityOptic.commonPrefixLength(zipOptic));
    assertEquals("address", cityOptic.prefix(1).outermost().key());
    assertEquals("city", cityOptic.suffix(1).outermost().key());
    assertTrue(firstOptic.startsWith(PointFreeOpticKind.PRODUCT));
    assertTrue(firstOptic.bounds().contains(Cartesian.Mu.TYPE_TOKEN));
    assertTrue(rightOptic.startsWith(PointFreeOpticKind.SUM));
    assertTrue(rightOptic.bounds().contains(Cocartesian.Mu.TYPE_TOKEN));
    assertTrue(firstOptic.outermost().untyped() instanceof ProductOpticElement(ProductSide side)
        && side == ProductSide.FIRST);
    assertTrue(rightOptic.outermost().untyped() instanceof SumOpticElement(SumSide side)
        && side == SumSide.RIGHT);
  }

  @Test
  void pointFreeOpticsCarryDfuStyleTypeMetadataAndExtraOpticElements() {
    record Box(int value) {}
    var value = PLens.<Box, Box, Integer, Integer>of(Box::value, (box, next) -> new Box(next));
    LensPath<Box, Integer> path = LensPath.of("value", value);
    TypeToken<Box> boxType = TypeToken.of(Box.class);
    TypeToken<Integer> intType = TypeToken.of(Integer.class);
    TypeToken<String> stringType = TypeToken.of(String.class);

    PointFreeOptic<Box, Box, Integer, Integer> typedLens = PointFreeOptic.lens(path, boxType, intType);
    PointFreeOptic<Tuple2<Integer, String>, Tuple2<Integer, String>, ?, ?> typedProduct =
        PointFreeOptic.product(ProductSide.FIRST, intType, stringType);
    PointFreeOptic<List<Integer>, List<Integer>, Integer, Integer> listTraversal = PointFreeOptic.list(intType);
    PointFreeOptic<Tuple2<String, ?>, Tuple2<String, ?>, Integer, Integer> tagged = PointFreeOptic.tagged("value", stringType, intType);

    assertEquals(Types.witness(boxType), typedLens.sourceType());
    assertEquals(Types.witness(intType), typedLens.focusType());
    assertEquals(Types.and(Types.witness(intType), Types.witness(stringType)), typedProduct.sourceType());
    assertEquals(Types.witness(intType), typedProduct.focusType());
    assertTrue(listTraversal.bounds().contains(Traversing.Mu.TYPE_TOKEN));
    assertTrue(listTraversal.startsWith(PointFreeOpticKind.TRAVERSAL));
    assertTrue(tagged.bounds().contains(Cocartesian.Mu.TYPE_TOKEN));
    assertTrue(tagged.startsWith(PointFreeOpticKind.TAGGED));

    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<List<Integer>, List<Integer>>> listApp = PointFree.opticApp(listTraversal, plusOne);
    PointFree<Function<Tuple2<String, ?>, Tuple2<String, ?>>> taggedApp = PointFree.opticApp(tagged, plusOne);

    assertEquals(List.of(2, 3, 4), listApp.eval().apply(List.of(1, 2, 3)));
    assertEquals(Tuple2.of("value", 2), taggedApp.eval().apply(Tuple2.of("value", 1)));
    assertEquals(Tuple2.of("other", 1), taggedApp.eval().apply(Tuple2.of("other", 1)));
  }

  @Test
  void pointFreeRepresentsOpticTransformersAndTypedFoldQueries() {
    record Box(int value) {}
    TypeToken<Box> boxType = TypeToken.of(Box.class);
    TypeToken<Integer> intType = TypeToken.of(Integer.class);
    var value = PLens.<Box, Box, Integer, Integer>of(Box::value, (box, next) -> new Box(next));
    PointFreeOptic<Box, Box, Integer, Integer> optic =
        PointFreeOptic.lens(LensPath.of("value", value), boxType, intType);
    PointFree<Function<Integer, Integer>> plusOne =
        PointFree.fn("plusOne", current -> current + 1, intType, intType);
    PointFree<Function<Function<Integer, Integer>, Function<Box, Box>>> transformer =
        PointFree.opticTransformer(optic);
    PointFree<Function<Box, Box>> transformed = PointFree.app(transformer, plusOne);
    Type<?> boxEndo = Types.function(Types.witness(boxType), Types.witness(boxType));

    assertEquals(Types.function(Types.function(Types.witness(intType), Types.witness(intType)), boxEndo), transformer.type());
    assertEquals(boxEndo, transformed.type());
    assertEquals(new Box(2), transformed.eval().apply(new Box(1)));

    Fold<List<Integer>, Integer> fold = Fold.of(values -> values);
    Type<List<Integer>> intListType = Types.list(Types.witness(intType));
    FoldQuery<List<Integer>, Integer, Integer, Integer> sum =
        FoldQuery.foldMap(fold, Monoid.of(0, Integer::sum), current -> current, intListType, Types.witness(intType));
    FoldQuery<List<Integer>, Integer, Integer, Integer> count =
        FoldQuery.foldMap(fold, Monoid.of(0, Integer::sum), ignored -> 1, intListType, Types.witness(intType));
    PointFree<Function<List<Integer>, Integer>> query = sum;
    FoldQuery<List<Integer>, Integer, Tuple2<Integer, Integer>, Tuple2<Integer, Integer>> zipped = sum.zip(count);
    FoldQuery<List<Integer>, Integer, Tuple2<Integer, Integer>, Integer> zippedWith =
        sum.zipWith(count, Integer::sum);

    assertEquals(Types.function(intListType, Types.witness(intType)), query.type());
    assertEquals(6, query.eval().apply(List.of(1, 2, 3)));
    assertEquals(Types.function(intListType, Types.and(Types.witness(intType), Types.witness(intType))), zipped.type());
    assertEquals(Tuple2.of(6, 3), zipped.eval().apply(List.of(1, 2, 3)));
    assertEquals(Types.function(intListType, Types.variable("FoldZipResult")), zippedWith.type());
    assertEquals(9, zippedWith.eval().apply(List.of(1, 2, 3)));
  }

  @Test
  void pointFreeNodesCarryOptionalTypeMetadataThroughLoweringAndRewrites() {
    record Box(int value) {}
    TypeToken<Box> boxType = TypeToken.of(Box.class);
    TypeToken<Integer> intType = TypeToken.of(Integer.class);
    TypeToken<String> stringType = TypeToken.of(String.class);
    var value = PLens.<Box, Box, Integer, Integer>of(Box::value, (box, next) -> new Box(next));
    PointFreeOptic<Box, Box, Integer, Integer> optic =
        PointFreeOptic.lens(LensPath.of("value", value), boxType, intType);
    PointFree<Integer> literal = PointFree.value(1, intType);
    PointFree<Function<Integer, String>> stringify =
        PointFree.fn("stringify", Object::toString, intType, stringType);
    PointFree<Function<String, String>> upper =
        PointFree.fn("upper", String::toUpperCase, stringType, stringType);
    PointFree<Function<Integer, String>> composed = PointFree.comp(upper, stringify);
    PointFree<String> applied = PointFree.app(composed, literal);
    PointFree<Function<Box, Box>> updated =
        OpticLowering.modify(optic, "inc", current -> current + 1);
    PointFree<Function<Box, Box>> opticIdentity =
        PointFree.opticApp(optic, PointFree.id(intType));

    assertEquals(Types.witness(intType), literal.type());
    assertEquals(Types.function(Types.witness(intType), Types.witness(stringType)), composed.type());
    assertEquals(Types.witness(stringType), applied.type());
    assertEquals(Types.function(Types.witness(boxType), Types.witness(boxType)), updated.type());
    assertEquals(
        Types.function(Types.witness(boxType), Types.witness(boxType)),
        PointFreeOptimizer.optimize(opticIdentity).type());
    assertEquals("1", applied.eval());
    assertEquals(new Box(2), updated.eval().apply(new Box(1)));

    PointFree<Function<String, String>> wrapped =
        new Comp<String, String>(List.of(upper, PointFree.<String>id()))
            .withType(Types.function(Types.witness(stringType), Types.witness(stringType)));
    PointFree<Function<String, String>> optimized = PointFreeOptimizer.optimize(wrapped);

    assertEquals(Types.function(Types.witness(stringType), Types.witness(stringType)), optimized.type());
    assertEquals("ROOT", optimized.eval().apply("root"));
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void pointFreeTypesRejectInvalidApplicationOpticModifierAndRetagging() {
    record Box(int value) {}
    TypeToken<Box> boxType = TypeToken.of(Box.class);
    TypeToken<Integer> intType = TypeToken.of(Integer.class);
    TypeToken<String> stringType = TypeToken.of(String.class);
    var value = PLens.<Box, Box, Integer, Integer>of(Box::value, (box, next) -> new Box(next));
    PointFreeOptic<Box, Box, Integer, Integer> optic =
        PointFreeOptic.lens(LensPath.of("value", value), boxType, intType);
    PointFree<Function<Integer, String>> stringify =
        PointFree.fn("stringify", Object::toString, intType, stringType);
    PointFree<String> stringValue = PointFree.value("wrong", stringType);
    PointFree<Function<Object, String>> erasedStringify = (PointFree) stringify;
    PointFree<Object> erasedStringValue = (PointFree) stringValue;
    PointFree<Function<String, String>> wrongModifier =
        PointFree.fn("upper", String::toUpperCase, stringType, stringType);

    assertEquals(Types.witness(stringType), PointFree.app(stringify, PointFree.value(1, intType)).type());
    assertThrows(IllegalArgumentException.class, () -> PointFree.app(erasedStringify, erasedStringValue).type());
    assertThrows(IllegalArgumentException.class, () -> PointFree.opticApp(optic, wrongModifier).type());
    assertThrows(
        IllegalArgumentException.class,
        () -> ((PointFree) stringify).withType(Types.function(Types.witness(stringType), Types.witness(stringType))));
    assertEquals(
        Types.function(Types.witness(stringType), Types.witness(stringType)),
        ((PointFree) stringify).retagUnsafe(Types.function(Types.witness(stringType), Types.witness(stringType))).type());
  }

  @Test
  @SuppressWarnings("unchecked")
  void pointFreeRewritesPreserveTrustedTypeMetadata() {
    record Box(int value) {}
    TypeToken<Box> boxType = TypeToken.of(Box.class);
    TypeToken<Integer> intType = TypeToken.of(Integer.class);
    TypeToken<String> stringType = TypeToken.of(String.class);
    Type<?> boxEndo = Types.function(Types.witness(boxType), Types.witness(boxType));
    Type<?> intEndo = Types.function(Types.witness(intType), Types.witness(intType));
    var value = PLens.<Box, Box, Integer, Integer>of(Box::value, (box, next) -> new Box(next));
    PointFreeOptic<Box, Box, Integer, Integer> optic =
        PointFreeOptic.lens(LensPath.of("value", value), boxType, intType);
    PointFree<Function<Integer, Integer>> plusOne =
        PointFree.fn("plusOne", current -> current + 1, intType, intType);
    PointFree<Function<Integer, Integer>> timesTwo =
        PointFree.fn("timesTwo", current -> current * 2, intType, intType);

    PointFree<Function<Box, Box>> fusedOptic =
        PointFreeOptimizer.optimize(PointFree.comp(PointFree.opticApp(optic, timesTwo), PointFree.opticApp(optic, plusOne)));
    assertEquals(boxEndo, fusedOptic.type());
    assertEquals(new Box(4), fusedOptic.eval().apply(new Box(1)));

    PointFreeRule rewriteLiteral =
        new PointFreeRule() {
          @Override
          public <A> RewriteResult<A> rewrite(PointFree<A> expression) {
            if (expression instanceof Value<?>(Object value1, var ignoredType) && Objects.equals(value1, 1)) {
              return RewriteResult.changed((PointFree<A>) PointFree.value(2, intType));
            }
            return RewriteResult.unchanged(expression);
          }
        };
    PointFree<String> applied =
        PointFree.app(
            PointFree.fn("stringify", Object::toString, intType, stringType),
            PointFree.value(1, intType));
    PointFree<String> rewrittenApplied =
        PointFreeRule.once(rewriteLiteral).rewrite(applied).expression();
    assertEquals(Types.witness(stringType), rewrittenApplied.type());
    assertEquals("2", rewrittenApplied.eval());

    RecursiveFamily family = new RecursiveFamily("TypedTree", 1);
    PointFree<Function<Integer, Integer>> recursiveBoundary =
        PointFree.comp(PointFree.in(family, 0, intType), PointFree.out(family, 0, intType));
    PointFree<Function<Integer, Integer>> optimizedBoundary = PointFreeOptimizer.optimize(recursiveBoundary);
    assertEquals(intEndo, optimizedBoundary.type());
    assertEquals(7, optimizedBoundary.eval().apply(7));

    AlgebraPlan innerAlgebra =
        AlgebraPlan.identity("inner", family).rewrite(0, current -> (Integer) current + 1);
    AlgebraPlan outerAlgebra =
        AlgebraPlan.identity("outer", family).rewrite(0, current -> (Integer) current * 2);
    CataPlan<Integer> inner = CataPlan.of(family, 0, innerAlgebra, current -> current + 1, intType);
    CataPlan<Integer> outer = CataPlan.of(family, 0, outerAlgebra, current -> current * 2, intType);
    PointFree<Function<Integer, Integer>> fusedCata = PointFreeOptimizer.optimize(PointFree.comp(outer, inner));
    assertEquals(intEndo, fusedCata.type());
    assertEquals(8, fusedCata.eval().apply(3));
  }

  @Test
  void pointFreeAstEvaluatesCompositionApplicationAndLensApplication() {
    record Box(int value) {}
    var value = PLens.<Box, Box, Integer, Integer>of(Box::value, (box, next) -> new Box(next));
    LensPath<Box, Integer> path = LensPath.of("value", value);
    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<Integer, Integer>> timesTwo = PointFree.fn("timesTwo", current -> current * 2);

    PointFree<Function<Integer, Integer>> composed = PointFree.comp(timesTwo, PointFree.comp(plusOne, PointFree.id()));
    assertEquals(8, composed.eval().apply(3));
    assertTrue(composed instanceof Comp<Integer, Integer>(
            List<PointFree<? extends Function<?, ?>>> functions,
            var ignoredType
    ) && functions.size() == 2);

    assertEquals(8, PointFree.app(composed, PointFree.value(3)).eval());
    assertEquals(new Box(8), PointFree.lensApp(path, composed).eval().apply(new Box(3)));
  }

  @Test
  void pointFreeOptimizerRewritesNestedApplicationAndLensIdentity() {
    record Box(int value) {}
    var value = PLens.<Box, Box, Integer, Integer>of(Box::value, (box, next) -> new Box(next));
    LensPath<Box, Integer> path = LensPath.of("value", value);
    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<Integer, Integer>> timesTwo = PointFree.fn("timesTwo", current -> current * 2);

    PointFree<Integer> nested = PointFree.app(timesTwo, PointFree.app(plusOne, PointFree.value(3)));
    PointFree<Integer> optimizedNested = PointFreeOptimizer.optimize(nested);
    assertEquals(8, optimizedNested.eval());
    assertTrue(optimizedNested instanceof Value<?>);

    PointFree<Function<Box, Box>> lensIdentity = PointFree.lensApp(path, PointFree.id());
    PointFree<Function<Box, Box>> optimizedIdentity = PointFreeOptimizer.optimize(lensIdentity);
    assertTrue(optimizedIdentity instanceof Id<?>);
    assertEquals(new Box(3), optimizedIdentity.eval().apply(new Box(3)));
  }

  @Test
  @SuppressWarnings("unchecked")
  void pointFreeRuleOnceRewritesTheFirstMatchingNode() {
    PointFreeRule incrementLiteral =
        new PointFreeRule() {
          @Override
          public <A> RewriteResult<A> rewrite(PointFree<A> expression) {
            if (expression instanceof Value<?>(Object value1, var ignoredType) && Objects.equals(value1, 1)) {
              return RewriteResult.changed((PointFree<A>) PointFree.value(2));
            }
            return RewriteResult.unchanged(expression);
          }
        };
    PointFree<Integer> expression = PointFree.app(PointFree.id(), PointFree.value(1));

    PointFree<Integer> rewritten =
        PointFreeRule.once(incrementLiteral).rewrite(expression).expression();

    assertEquals(2, rewritten.eval());
  }

  @Test
  void pointFreeOptimizerAppliesBangEtaRules() {
    PointFree<Function<Integer, String>> stringify = PointFree.fn("stringify", Object::toString);
    PointFree<Function<String, String>> upper = PointFree.fn("upper", String::toUpperCase);
    PointFree<Function<Integer, Unit>> expression =
        PointFree.comp(PointFree.bang(), PointFree.comp(upper, stringify));
    PointFree<Unit> applied = PointFree.app(PointFree.bang(), PointFree.value("ignored"));

    PointFree<Function<Integer, Unit>> optimized = PointFreeOptimizer.optimize(expression);
    PointFree<Unit> optimizedApplied = PointFreeOptimizer.optimize(applied);

    assertTrue(optimized instanceof Bang<?>);
    assertEquals(Unit.INSTANCE, optimized.eval().apply(123));
    assertEquals(Types.witness(Unit.class), optimizedApplied.type());
    assertEquals(Unit.INSTANCE, optimizedApplied.eval());
  }

  @Test
  void pointFreeOptimizerFusesLensCompositionRules() {
    record Box(int value) {}
    AtomicInteger sets = new AtomicInteger();
    var value =
        PLens.<Box, Box, Integer, Integer>of(
            Box::value,
            (box, next) -> {
              sets.incrementAndGet();
              return new Box(next);
            });
    LensPath<Box, Integer> path = LensPath.of("value", value);
    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<Integer, Integer>> timesTwo = PointFree.fn("timesTwo", current -> current * 2);
    PointFree<Function<Box, Box>> expression =
        PointFree.comp(PointFree.lensApp(path, timesTwo), PointFree.lensApp(path, plusOne));

    PointFree<Function<Box, Box>> optimized = PointFreeOptimizer.optimize(expression);

    assertTrue(OpticTestHelpers.isLensApp(optimized));
    assertEquals(new Box(4), optimized.eval().apply(new Box(1)));
    assertEquals(1, sets.get());
  }

  @Test
  void pointFreeOptimizerFactorsCommonLensPrefix() {
    record Account(String name, Address address) {}
    AtomicInteger addressSets = new AtomicInteger();
    var address =
        PLens.<Account, Account, Address, Address>of(
            Account::address,
            (account, next) -> {
              addressSets.incrementAndGet();
              return new Account(account.name(), next);
            });
    var city = PLens.<Address, Address, String, String>of(Address::city, (addr, next) -> new Address(next, addr.zip()));
    var zip = PLens.<Address, Address, Integer, Integer>of(Address::zip, (addr, next) -> new Address(addr.city(), next));
    LensPath<Account, String> cityPath = LensPath.of("address", address).andThen("city", city);
    LensPath<Account, Integer> zipPath = LensPath.of("address", address).andThen("zip", zip);
    PointFree<Function<String, String>> upper = PointFree.fn("upper", String::toUpperCase);
    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<Account, Account>> expression =
        PointFree.comp(PointFree.lensApp(zipPath, plusOne), PointFree.lensApp(cityPath, upper));

    PointFree<Function<Account, Account>> optimized = PointFreeOptimizer.optimize(expression);

    assertTrue(OpticTestHelpers.isLensApp(optimized));
    assertEquals(new Account("root", new Address("LONDON", 12346)), optimized.eval().apply(new Account("root", new Address("london", 12345))));
    assertEquals(1, addressSets.get());
  }

  @Test
  void pointFreeOptimizerFusesProductProjectionApplications() {
    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<Integer, Integer>> timesTwo = PointFree.fn("timesTwo", current -> current * 2);
    PointFree<Function<Tuple2<Integer, String>, Tuple2<Integer, String>>> expression =
        PointFree.comp(PointFree.productFirst(timesTwo), PointFree.productFirst(plusOne));

    PointFree<Function<Tuple2<Integer, String>, Tuple2<Integer, String>>> optimized =
        PointFreeOptimizer.optimize(expression);

    assertTrue(OpticTestHelpers.isProductApp(optimized, ProductSide.FIRST));
    assertEquals(Tuple2.of(4, "a"), optimized.eval().apply(Tuple2.of(1, "a")));
  }

  @Test
  void pointFreeOptimizerRewritesInsideProductAndSumApplications() {
    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<Integer, Integer>> functionWithIdentity =
        new Comp<>(List.of(plusOne, PointFree.id()));
    PointFree<Function<Tuple2<Integer, String>, Tuple2<Integer, String>>> product =
        PointFree.productFirst(functionWithIdentity);
    PointFree<Function<Either<Integer, String>, Either<Integer, String>>> sum =
        PointFree.sumLeft(functionWithIdentity);

    PointFree<Function<Tuple2<Integer, String>, Tuple2<Integer, String>>> optimizedProduct =
        PointFreeOptimizer.optimize(product);
    PointFree<Function<Either<Integer, String>, Either<Integer, String>>> optimizedSum =
        PointFreeOptimizer.optimize(sum);

    assertTrue(OpticTestHelpers.isProductApp(optimizedProduct, ProductSide.FIRST)
        && !(OpticTestHelpers.opticFunction(optimizedProduct) instanceof Comp<?, ?>));
    assertTrue(OpticTestHelpers.isSumApp(optimizedSum, SumSide.LEFT)
        && !(OpticTestHelpers.opticFunction(optimizedSum) instanceof Comp<?, ?>));
    assertEquals(Tuple2.of(2, "a"), optimizedProduct.eval().apply(Tuple2.of(1, "a")));
    assertEquals(Either.left(2), optimizedSum.eval().apply(Either.left(1)));
  }

  @Test
  void pointFreeOptimizerSortsProductProjectionsToEnableFusion() {
    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<Integer, Integer>> timesTwo = PointFree.fn("timesTwo", current -> current * 2);
    PointFree<Function<String, String>> appendBang = PointFree.fn("appendBang", current -> current + "!");
    PointFree<Function<Tuple2<Integer, String>, Tuple2<Integer, String>>> expression =
        PointFree.comp(
            PointFree.productFirst(timesTwo),
            PointFree.comp(PointFree.productSecond(appendBang), PointFree.productFirst(plusOne)));

    PointFree<Function<Tuple2<Integer, String>, Tuple2<Integer, String>>> optimized =
        PointFreeOptimizer.optimize(expression);

    assertEquals(Tuple2.of(4, "a!"), optimized.eval().apply(Tuple2.of(1, "a")));
    assertTrue(optimized instanceof Comp<?, ?> comp && comp.functions().size() == 2);
    PointFree<? extends Function<?, ?>> first = ((Comp<?, ?>) optimized).functions().getFirst();
    assertTrue(OpticTestHelpers.isProductApp(first, ProductSide.FIRST));
  }

  @Test
  void pointFreeOptimizerSortsTypedProductProjectionsToEnableFusion() {
    TypeToken<Integer> intType = TypeToken.of(Integer.class);
    TypeToken<String> stringType = TypeToken.of(String.class);
    PointFreeOptic<Tuple2<Integer, String>, Tuple2<Integer, String>, ?, ?> first =
        PointFreeOptic.product(ProductSide.FIRST, intType, stringType);
    PointFreeOptic<Tuple2<Integer, String>, Tuple2<Integer, String>, ?, ?> second =
        PointFreeOptic.product(ProductSide.SECOND, intType, stringType);
    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<Integer, Integer>> timesTwo = PointFree.fn("timesTwo", current -> current * 2);
    PointFree<Function<String, String>> appendBang = PointFree.fn("appendBang", current -> current + "!");
    PointFree<Function<Tuple2<Integer, String>, Tuple2<Integer, String>>> expression =
        PointFree.comp(
            PointFree.opticApp(first, timesTwo),
            PointFree.comp(PointFree.opticApp(second, appendBang), PointFree.opticApp(first, plusOne)));

    PointFree<Function<Tuple2<Integer, String>, Tuple2<Integer, String>>> optimized =
        PointFreeOptimizer.optimize(expression);

    assertEquals(Tuple2.of(4, "a!"), optimized.eval().apply(Tuple2.of(1, "a")));
    assertTrue(optimized instanceof Comp<?, ?> comp && comp.functions().size() == 2);
    assertTrue(OpticTestHelpers.isProductApp(((Comp<?, ?>) optimized).functions().getFirst(), ProductSide.FIRST));
  }

  @Test
  void pointFreeOptimizerRepairsTypedProductOuterMetadataAfterSorting() {
    Type<Integer> intType = Types.witness(Integer.class);
    Type<Long> longType = Types.witness(Long.class);
    Type<String> stringType = Types.witness(String.class);
    Type<Boolean> boolType = Types.witness(Boolean.class);
    Type<Tuple2<Integer, String>> sourceType = Types.and(intType, stringType);
    Type<Tuple2<Integer, Boolean>> intermediateType = Types.and(intType, boolType);
    Type<Tuple2<Long, Boolean>> targetType = Types.and(longType, boolType);
    PointFreeOptic<Tuple2<Integer, String>, Tuple2<Long, String>, Integer, Long> first =
        new CompositePointFreeOptic<>(TypedOptic.proj1(intType, stringType, longType));
    PointFreeOptic<Tuple2<Long, String>, Tuple2<Long, Boolean>, String, Boolean> second =
        new CompositePointFreeOptic<>(TypedOptic.proj2(longType, stringType, boolType));
    PointFree<Function<Integer, Long>> widen =
        PointFree.fn("widen", Integer::longValue, intType, longType);
    PointFree<Function<String, Boolean>> nonEmpty =
        PointFree.fn("nonEmpty", value -> !value.isEmpty(), stringType, boolType);
    PointFree<Function<Tuple2<Integer, String>, Tuple2<Long, Boolean>>> expression =
        PointFree.comp(PointFree.opticApp(second, nonEmpty), PointFree.opticApp(first, widen));

    PointFree<Function<Tuple2<Integer, String>, Tuple2<Long, Boolean>>> optimized =
        PointFreeOptimizer.optimize(expression);

    assertEquals(Tuple2.of(1L, true), optimized.eval().apply(Tuple2.of(1, "a")));
    assertEquals(Types.function(sourceType, targetType), optimized.type());
    assertTrue(optimized instanceof Comp<?, ?> comp && comp.functions().size() == 2);
    var sortedFirst = (OpticApp<?, ?, ?, ?>) ((Comp<?, ?>) optimized).functions().getFirst();
    var sortedSecond = (OpticApp<?, ?, ?, ?>) ((Comp<?, ?>) optimized).functions().get(1);
    assertEquals(intermediateType, sortedFirst.optic().sourceType());
    assertEquals(targetType, sortedFirst.optic().targetType());
    assertEquals(sourceType, sortedSecond.optic().sourceType());
    assertEquals(intermediateType, sortedSecond.optic().targetType());
  }

  @Test
  void pointFreeBasicRulePerformsDfuStyleQueuedCompositionRewrite() {
    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<Integer, Integer>> timesTwo = PointFree.fn("timesTwo", current -> current * 2);
    PointFree<Function<Integer, Integer>> minusThree = PointFree.fn("minusThree", current -> current - 3);
    PointFree<Function<String, String>> appendBang = PointFree.fn("appendBang", current -> current + "!");
    PointFree<Function<String, String>> upper = PointFree.fn("upper", String::toUpperCase);
    PointFree<Function<Tuple2<Integer, String>, Tuple2<Integer, String>>> expression =
        PointFree.comp(
            PointFree.productFirst(minusThree),
            PointFree.comp(
                PointFree.productSecond(upper),
                PointFree.comp(
                    PointFree.productFirst(timesTwo),
                    PointFree.comp(PointFree.productSecond(appendBang), PointFree.productFirst(plusOne)))));

    PointFree<Function<Tuple2<Integer, String>, Tuple2<Integer, String>>> rewritten =
        PointFreeRules.basic().rewrite(expression).expression();

    assertEquals(Tuple2.of(1, "A!"), rewritten.eval().apply(Tuple2.of(1, "a")));
    assertTrue(rewritten instanceof Comp<?, ?> comp && comp.functions().size() == 2);
    Comp<?, ?> comp = (Comp<?, ?>) rewritten;
    assertTrue(OpticTestHelpers.isProductApp(comp.functions().getFirst(), ProductSide.FIRST)
        && OpticTestHelpers.opticFunction(comp.functions().getFirst()) instanceof Comp<?, ?>);
    assertTrue(OpticTestHelpers.isProductApp(comp.functions().get(1), ProductSide.SECOND)
        && OpticTestHelpers.opticFunction(comp.functions().get(1)) instanceof Comp<?, ?>);
  }

  @Test
  void pointFreeOptimizerFusesSumInjectionApplications() {
    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<Integer, Integer>> timesTwo = PointFree.fn("timesTwo", current -> current * 2);
    PointFree<Function<Either<Integer, String>, Either<Integer, String>>> expression =
        PointFree.comp(PointFree.sumLeft(timesTwo), PointFree.sumLeft(plusOne));

    PointFree<Function<Either<Integer, String>, Either<Integer, String>>> optimized =
        PointFreeOptimizer.optimize(expression);

    assertTrue(OpticTestHelpers.isSumApp(optimized, SumSide.LEFT));
    assertEquals(Either.left(4), optimized.eval().apply(Either.left(1)));
    assertEquals(Either.right("a"), optimized.eval().apply(Either.right("a")));
  }

  @Test
  void pointFreeOptimizerCancelsMatchingRecursiveInOutMarkers() {
    RecursiveFamily family = new RecursiveFamily("Tree", 2);
    PointFree<Function<String, String>> expression =
        PointFree.comp(PointFree.in(family, 0), PointFree.out(family, 0));
    PointFree<Function<String, String>> reverse =
        PointFree.comp(PointFree.out(family, 0), PointFree.in(family, 0));
    PointFree<Function<String, String>> differentSlot =
        PointFree.comp(PointFree.in(family, 0), PointFree.out(family, 1));

    PointFree<Function<String, String>> optimized = PointFreeOptimizer.optimize(expression);
    PointFree<Function<String, String>> optimizedReverse = PointFreeOptimizer.optimize(reverse);
    PointFree<Function<String, String>> optimizedDifferentSlot = PointFreeOptimizer.optimize(differentSlot);

    assertTrue(optimized instanceof Id<?>);
    assertTrue(optimizedReverse instanceof Id<?>);
    assertTrue(optimizedDifferentSlot instanceof Comp<?, ?>);
    assertEquals("x", optimized.eval().apply("x"));
    assertEquals("x", optimizedReverse.eval().apply("x"));
    assertEquals("x", optimizedDifferentSlot.eval().apply("x"));
  }

  @Test
  void pointFreeOptimizerSortsSumInjectionsToEnableFusion() {
    PointFree<Function<Integer, Integer>> plusOne = PointFree.fn("plusOne", current -> current + 1);
    PointFree<Function<Integer, Integer>> timesTwo = PointFree.fn("timesTwo", current -> current * 2);
    PointFree<Function<String, String>> appendBang = PointFree.fn("appendBang", current -> current + "!");
    PointFree<Function<Either<Integer, String>, Either<Integer, String>>> expression =
        PointFree.comp(
            PointFree.sumLeft(timesTwo),
            PointFree.comp(PointFree.sumRight(appendBang), PointFree.sumLeft(plusOne)));

    PointFree<Function<Either<Integer, String>, Either<Integer, String>>> optimized =
        PointFreeOptimizer.optimize(expression);

    assertEquals(Either.left(4), optimized.eval().apply(Either.left(1)));
    assertEquals(Either.right("a!"), optimized.eval().apply(Either.right("a")));
    assertTrue(optimized instanceof Comp<?, ?> comp && comp.functions().size() == 2);
    PointFree<? extends Function<?, ?>> first = ((Comp<?, ?>) optimized).functions().getFirst();
    assertTrue(OpticTestHelpers.isSumApp(first, SumSide.LEFT));
  }

  @Test
  void pointFreeOptimizerRepairsTypedSumOuterMetadataAfterSorting() {
    Type<Integer> intType = Types.witness(Integer.class);
    Type<Long> longType = Types.witness(Long.class);
    Type<String> stringType = Types.witness(String.class);
    Type<Boolean> boolType = Types.witness(Boolean.class);
    Type<Either<Integer, String>> sourceType = Types.or(intType, stringType);
    Type<Either<Integer, Boolean>> intermediateType = Types.or(intType, boolType);
    Type<Either<Long, Boolean>> targetType = Types.or(longType, boolType);
    PointFreeOptic<Either<Integer, String>, Either<Long, String>, Integer, Long> left =
        new CompositePointFreeOptic<>(TypedOptic.inj1(intType, stringType, longType));
    PointFreeOptic<Either<Long, String>, Either<Long, Boolean>, String, Boolean> right =
        new CompositePointFreeOptic<>(TypedOptic.inj2(longType, stringType, boolType));
    PointFree<Function<Integer, Long>> widen =
        PointFree.fn("widen", Integer::longValue, intType, longType);
    PointFree<Function<String, Boolean>> nonEmpty =
        PointFree.fn("nonEmpty", value -> !value.isEmpty(), stringType, boolType);
    PointFree<Function<Either<Integer, String>, Either<Long, Boolean>>> expression =
        PointFree.comp(PointFree.opticApp(right, nonEmpty), PointFree.opticApp(left, widen));

    PointFree<Function<Either<Integer, String>, Either<Long, Boolean>>> optimized =
        PointFreeOptimizer.optimize(expression);

    assertEquals(Either.left(1L), optimized.eval().apply(Either.left(1)));
    assertEquals(Either.right(true), optimized.eval().apply(Either.right("a")));
    assertEquals(Types.function(sourceType, targetType), optimized.type());
    assertTrue(optimized instanceof Comp<?, ?> comp && comp.functions().size() == 2);
    var sortedFirst = (OpticApp<?, ?, ?, ?>) ((Comp<?, ?>) optimized).functions().getFirst();
    var sortedSecond = (OpticApp<?, ?, ?, ?>) ((Comp<?, ?>) optimized).functions().get(1);
    assertEquals(intermediateType, sortedFirst.optic().sourceType());
    assertEquals(targetType, sortedFirst.optic().targetType());
    assertEquals(sourceType, sortedSecond.optic().sourceType());
    assertEquals(intermediateType, sortedSecond.optic().targetType());
  }

  @Test
  void pointFreeOptimizerFusesSameRecursiveAlgebraBranch() {
    RecursiveFamily family = new RecursiveFamily("Tree", 2);
    AlgebraPlan innerAlgebra =
        AlgebraPlan.identity("inner", family).rewrite(0, value -> (Integer) value + 1);
    AlgebraPlan outerAlgebra =
        AlgebraPlan.identity("outer", family).rewrite(0, value -> (Integer) value * 2);
    CataPlan<Integer> inner = CataPlan.of(family, 0, innerAlgebra, value -> value + 1);
    CataPlan<Integer> outer = CataPlan.of(family, 0, outerAlgebra, value -> value * 2);
    PointFree<Function<Integer, Integer>> expression = PointFree.comp(outer, inner);

    PointFree<Function<Integer, Integer>> optimized = PointFreeOptimizer.optimize(expression);

    assertTrue(optimized instanceof CataPlan<?>);
    CataPlan<Integer> cata = (CataPlan<Integer>) optimized;
    assertEquals(List.of(0), cata.algebra().modifiedIndices());
    assertEquals(8, cata.algebra().branch(0).function().apply(3));
    assertTrue(cata.algebra().branch(0).rewrite().view() instanceof Comp<?, ?>);
    assertFalse(cata.algebra().branch(0).hasRecursiveDependencyEvidence());
    assertEquals(8, cata.eval().apply(3));
  }

  @Test
  void pointFreeOptimizerFusesDifferentRecursiveAlgebraBranchesWhenIndependent() {
    RecursiveFamily family = new RecursiveFamily("Tree", 3);
    AlgebraPlan innerAlgebra =
        AlgebraPlan.identity("inner", family)
            .rewriteWithoutRecursiveDependencies(0, value -> (Integer) value + 1);
    AlgebraPlan outerAlgebra =
        AlgebraPlan.identity("outer", family)
            .rewriteWithoutRecursiveDependencies(1, value -> (Integer) value * 2);
    CataPlan<Integer> inner = CataPlan.of(family, 0, innerAlgebra, value -> value + 1);
    CataPlan<Integer> outer = CataPlan.of(family, 0, outerAlgebra, value -> value * 2);
    PointFree<Function<Integer, Integer>> expression = PointFree.comp(outer, inner);

    PointFree<Function<Integer, Integer>> optimized = PointFreeOptimizer.optimize(expression);

    assertTrue(optimized instanceof CataPlan<?>);
    CataPlan<Integer> cata = (CataPlan<Integer>) optimized;
    assertEquals(List.of(0, 1), cata.algebra().modifiedIndices());
    assertEquals(4, cata.algebra().branch(0).function().apply(3));
    assertEquals(6, cata.algebra().branch(1).function().apply(3));
    assertTrue(cata.planRewrite().view() instanceof Comp<?, ?>);
    assertTrue(cata.algebra().branch(1).hasRecursiveDependencyEvidence());
    assertTrue(cata.algebra().branch(1).rewrite().recursiveDependencies().get().isEmpty());
    assertEquals(8, cata.eval().apply(3));
  }

  @Test
  void pointFreeOptimizerDoesNotFuseDifferentRecursiveAlgebraBranchesWithoutDependencyEvidence() {
    RecursiveFamily family = new RecursiveFamily("Tree", 3);
    AlgebraPlan innerAlgebra =
        AlgebraPlan.identity("inner", family).rewrite(0, value -> (Integer) value + 1);
    AlgebraPlan outerAlgebra =
        AlgebraPlan.identity("outer", family).rewrite(1, value -> (Integer) value * 2);
    CataPlan<Integer> inner = CataPlan.of(family, 0, innerAlgebra, value -> value + 1);
    CataPlan<Integer> outer = CataPlan.of(family, 0, outerAlgebra, value -> value * 2);
    PointFree<Function<Integer, Integer>> expression = PointFree.comp(outer, inner);

    PointFree<Function<Integer, Integer>> optimized = PointFreeOptimizer.optimize(expression);

    assertTrue(optimized instanceof Comp<?, ?>);
    assertEquals(8, optimized.eval().apply(3));
  }

  @Test
  void pointFreeOptimizerDoesNotFuseRecursiveAlgebrasWithDependencyConflict() {
    RecursiveFamily family = new RecursiveFamily("Tree", 3);
    AlgebraPlan innerAlgebra =
        AlgebraPlan.identity("inner", family).rewrite(0, value -> (Integer) value + 1);
    AlgebraPlan outerAlgebra =
        AlgebraPlan.identity("outer", family).rewrite(1, value -> (Integer) value * 2, 0);
    CataPlan<Integer> inner = CataPlan.of(family, 0, innerAlgebra, value -> value + 1);
    CataPlan<Integer> outer = CataPlan.of(family, 0, outerAlgebra, value -> value * 2);
    PointFree<Function<Integer, Integer>> expression = PointFree.comp(outer, inner);

    PointFree<Function<Integer, Integer>> optimized = PointFreeOptimizer.optimize(expression);

    assertTrue(optimized instanceof Comp<?, ?>);
    assertEquals(8, optimized.eval().apply(3));
  }
}
