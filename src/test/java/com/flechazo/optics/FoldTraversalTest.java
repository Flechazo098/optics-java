package com.flechazo.optics;

import com.flechazo.optics.internal.OpticMetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.Validated;
import com.flechazo.hkt.functions.FoldQuery;
import com.flechazo.hkt.functions.PointFreeFold;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.util.Affines;
import com.flechazo.optics.util.EitherTraversals;
import com.flechazo.optics.util.Prisms;
import com.flechazo.optics.util.StringTraversals;
import com.flechazo.optics.util.Traversals;
import com.flechazo.optics.util.ValidatedTraversals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FoldTraversalTest {

  @Test
  void traversalCanReadAndModifyEveryListElement() {
    PTraversal<List<Integer>, List<Integer>, Integer, Integer> each = Each.<Integer>listEach().each();

    assertEquals(List.of(1, 2, 3), each.asFold().getAll(List.of(1, 2, 3)));
    assertEquals(List.of(2, 4, 6), each.modify(value -> value * 2, List.of(1, 2, 3)));
  }

  @Test
  void foldsTraversalsAndMapHelpersExposeIndexedAndReadOnlyComposition() {
    record Names(List<String> values) {}

    Fold<List<Object>, String> strings = Fold.<List<Object>, Object>of(list -> list)
        .andThen(Prisms.instanceOf(String.class));
    assertEquals(List.of("a", "b"), strings.getAll(List.of("a", 1, "b")));
    assertEquals(Maybe.some("a"), strings.preview(List.of("a", 1, "b")));
    assertEquals("fallback", strings.firstOrElse("fallback", List.of(1, 2)));
    assertEquals(Maybe.some("b"), strings.find(value -> value.equals("b"), List.of("a", 1, "b")));

    Fold<List<String>, String> listFold = Fold.of(list -> list);
    assertEquals(Maybe.some("b"), listFold.at(1).preview(List.of("a", "b", "c")));
    assertEquals(Maybe.none(), listFold.at(3).preview(List.of("a", "b", "c")));

    var values = PLens.<Names, Names, List<String>, List<String>>of(Names::values, (names, next) -> new Names(next));
    Getter<List<Object>, Object> firstObject = List::getFirst;
    assertEquals(List.of("x"), firstObject.andThen(Prisms.instanceOf(String.class)).getAll(List.of("x", 1)));
    Iso<Names, List<String>> namesIso = Iso.of(Names::values, Names::new);
    assertEquals(2, namesIso.andThen(Getter.of(List::size)).get(new Names(List.of("a", "b"))));
    assertEquals(List.of("a", "b"), namesIso.andThen(listFold).getAll(new Names(List.of("a", "b"))));
    assertEquals(List.of("a", "b"), values.andThen(listFold).getAll(new Names(List.of("a", "b"))));
    assertEquals(List.of("a", "b"), Affines.<List<String>>maybeValue().andThen(listFold).getAll(Maybe.some(List.of("a", "b"))));
    assertEquals(
        List.of("a", "b"),
            Prisms.<List<String>>some().andThen(listFold).getAll(Maybe.some(List.of("a", "b"))));
    assertEquals(
        new Names(List.of("A", "b")),
        values.asSetter()
            .andThen(PAffine.<String>listAt(0))
            .modify(String::toUpperCase, new Names(List.of("a", "b"))));

    PTraversal<List<List<Integer>>, List<List<Integer>>, List<Integer>, List<Integer>> rows = Traversals.forList();
    assertEquals(
        List.of(1, 2, 3),
        rows.andThen(Fold.<List<Integer>, Integer>of(row -> row)).getAll(List.of(List.of(1, 2), List.of(3))));
    assertEquals(List.of(2), rows.andThen(PAffine.listAt(1)).getAll(List.of(List.of(1, 2), List.of(3))));
    assertEquals(List.of(List.of(1, 20), List.of(3)), rows.andThen(PAffine.listAt(1)).modify(value -> value * 10, List.of(List.of(1, 2), List.of(3))));

    Fold<List<Names>, Names> names = Fold.of(list -> list);
    assertEquals(List.of(List.of("a", "b")), names.andThen(values).getAll(List.of(new Names(List.of("a", "b")))));
    assertEquals(
        List.of("a"),
        names.andThen(values.andThen(PAffine.<String>listAt(0))).getAll(List.of(new Names(List.of("a", "b")))));
    assertEquals(
        List.of("a", "b"),
        names.andThen(values.andThen(Traversals.<String>forList())).getAll(List.of(new Names(List.of("a", "b")))));

    PTraversal<List<Integer>, List<Integer>, Integer, Integer> each = Traversals.forList();
    assertEquals(Maybe.some(2), each.asFold().at(1).preview(List.of(1, 2, 3)));
    assertEquals(List.of(1, 20, 3), PAffine.<Integer>listAt(1).set(20, List.of(1, 2, 3)));
    assertEquals(List.of(1, 2, 3), PAffine.<Integer>listAt(5).set(20, List.of(1, 2, 3)));
    assertEquals(Maybe.some("b"), PAffine.<String>listAt(1).getMaybe(List.of("a", "b")));
    assertEquals(List.of("a", "B"), PAffine.<String>listAt(1).set("B", List.of("a", "b")));

    LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);

    assertEquals(List.of("a", "b"), Fold.<String, Integer>mapKeys().getAll(map));
    assertEquals(List.of(1, 2), Fold.<String, Integer>mapValues().getAll(map));
    assertEquals(List.of("a", "b"), Fold.<String, Integer>mapEntries().getAll(map).stream().map(Map.Entry::getKey).toList());
    assertEquals(Maybe.some(1), PAffine.<String, Integer>mapValue("a").getMaybe(map));
    assertEquals(Map.of("a", 10, "b", 2), PAffine.<String, Integer>mapValue("a").set(10, map));
    assertEquals(map, PAffine.<String, Integer>mapValue("z").set(10, map));
    assertEquals(Map.of("a", 2, "b", 3), Traversals.<String, Integer>forMapValues().modify(value -> value + 1, map));
  }

  @Test
  void foldMap2FusesIndependentAggregationsIntoOneFoldPass() {
    AtomicInteger visits = new AtomicInteger();
    Fold<List<Integer>, Integer> fold =
        new Fold<>() {
          @Override
          public <M> M foldMap(Monoid<M> monoid, Function<? super Integer, ? extends M> f, List<Integer> source) {
            M result = monoid.empty();
            for (Integer value : source) {
              visits.incrementAndGet();
              result = monoid.combine(result, f.apply(value));
            }
            return result;
          }
        };

    Tuple2<Integer, Boolean> result =
        fold.foldMap2(
            Monoid.of(0, Integer::sum),
            value -> value,
            Monoid.of(false, Boolean::logicalOr),
            value -> value > 2,
            Tuple2::of,
            List.of(1, 2, 3));

    assertEquals(Tuple2.of(6, true), result);
    assertEquals(3, visits.get());
  }

  @Test
  void foldQueryFusesProductFoldPlansIntoOneFoldPass() {
    AtomicInteger visits = new AtomicInteger();
    Fold<List<Integer>, Integer> fold =
        new Fold<>() {
          @Override
          public <M> M foldMap(Monoid<M> monoid, Function<? super Integer, ? extends M> f, List<Integer> source) {
            M result = monoid.empty();
            for (Integer value : source) {
              visits.incrementAndGet();
              result = monoid.combine(result, f.apply(value));
            }
            return result;
          }
        };

    var sum = FoldQuery.foldMap(fold, Monoid.of(0, Integer::sum), value -> value);
    var hasEven = FoldQuery.foldMap(fold, Monoid.of(false, Boolean::logicalOr), value -> value % 2 == 0);
    Tuple2<Integer, Boolean> result = sum.zipWith(hasEven, Tuple2::of).run(List.of(1, 2, 3));

    assertEquals(Tuple2.of(6, true), result);
    assertEquals(3, visits.get());
  }

  @Test
  void typedFoldMetadataPropagatesThroughGeneratedAsFoldCompositionFiltersAndSums() {
    Fold<Account, String> name = RecordOptics.<Account, String>recordLens(Account.class, "name").asFold();
    Fold<User, String> city = RecordOptics.<User, Address>recordLens(User.class, "address")
        .asFold()
        .andThen(RecordOptics.<Address, String>recordLens(Address.class, "city"));
    Fold<User, String> lensThenFold = RecordOptics.<User, Address>recordLens(User.class, "address")
        .andThen(RecordOptics.<Address, String>recordLens(Address.class, "city").asFold());
    Fold<User, String> getterThenFold = RecordOptics.<User, Address>recordLens(User.class, "address")
        .asGetter()
        .andThen(RecordOptics.<Address, String>recordLens(Address.class, "city").asFold());
    Getter<User, String> getterThenGetter = RecordOptics.<User, Address>recordLens(User.class, "address")
        .asGetter()
        .andThen(RecordOptics.<Address, String>recordLens(Address.class, "city").asGetter());
    Fold<Account, String> filtered = name.filtered(value -> value.startsWith("r"));
    Fold<Account, String> doubled = name.plus(name);
    Fold<Account, String> opaque = Fold.of(account -> List.of(account.name()));
    Fold<User, String> opaqueGetterThenFold = Getter.<User, Address>of(User::address)
        .andThen(RecordOptics.<Address, String>recordLens(Address.class, "city").asFold());
    Account account = new Account("root", List.of(1, 2));

    assertTrue(OpticMetadata.fold(name).isDefined());
    assertEquals(Types.witness(Account.class), OpticMetadata.fold(name).get().sourceType());
    assertEquals(Types.witness(String.class), OpticMetadata.fold(name).get().focusType());
    assertTrue(OpticMetadata.fold(city).isDefined());
    assertEquals(List.of("Paris"), city.getAll(new User("Ada", new Address("Paris", 75000))));
    assertTrue(OpticMetadata.fold(lensThenFold).isDefined());
    assertEquals(List.of("Paris"), lensThenFold.getAll(new User("Ada", new Address("Paris", 75000))));
    assertTrue(OpticMetadata.fold(getterThenFold).isDefined());
    assertEquals(List.of("Paris"), getterThenFold.getAll(new User("Ada", new Address("Paris", 75000))));
    assertTrue(OpticMetadata.fold(getterThenGetter).isDefined());
    assertEquals("Paris", getterThenGetter.get(new User("Ada", new Address("Paris", 75000))));
    assertTrue(OpticMetadata.fold(filtered).isDefined());
    assertEquals(List.of("root"), filtered.getAll(account));
    assertTrue(OpticMetadata.fold(doubled).isDefined());
    assertEquals(List.of("root", "root"), doubled.getAll(account));
    assertFalse(OpticMetadata.fold(opaque).isDefined());
    assertFalse(OpticMetadata.fold(opaqueGetterThenFold).isDefined());
  }

  @Test
  void foldQueryFusionUsesTypedFoldStructureInsteadOfWrapperIdentity() {
    Fold<Account, String> first = RecordOptics.<Account, String>recordLens(Account.class, "name").asFold();
    Fold<Account, String> second = RecordOptics.<Account, String>recordLens(Account.class, "name").asFold();
    var length = FoldQuery.foldMap(first, Monoid.of(0, Integer::sum), String::length);
    var hasRoot = FoldQuery.foldMap(second, Monoid.of(false, Boolean::logicalOr), "root"::equals);

    Tuple2<Integer, Boolean> result = length.zipWith(hasRoot, Tuple2::of).run(new Account("root", List.of()));

    assertEquals(Tuple2.of(4, true), result);
  }

  @Test
  void foldQuerySpecializationsFuseThroughTypedFoldStructure() {
    AtomicInteger visits = new AtomicInteger();
    Fold<List<Integer>, Integer> first = typedCountingFold("numbers", visits);
    Fold<List<Integer>, Integer> second = typedCountingFold("numbers", visits);

    var count = FoldQuery.count(first);
    var hasEven = FoldQuery.any(second, value -> value % 2 == 0);
    Tuple2<Integer, Boolean> result = count.zipWith(hasEven, Tuple2::of).run(List.of(1, 2, 3));

    assertEquals(Tuple2.of(3, true), result);
    assertEquals(3, visits.get());
    assertEquals(Maybe.some(1), FoldQuery.preview(first).run(List.of(1, 2, 3)));
    assertEquals(Maybe.some(1), FoldQuery.first(second).run(List.of(1, 2, 3)));
    assertTrue(FoldQuery.all(first, value -> value > 0).run(List.of(1, 2, 3)));
  }

  @Test
  void structuredUtilityTraversalsLowerToTypedFoldSpines() {
    var integer = Types.witness(Integer.class);
    var string = Types.witness(String.class);
    Fold<List<Integer>, Integer> firstList = Traversals.forList(integer).asFold();
    Fold<List<Integer>, Integer> secondList = Traversals.forList(integer).asFold();
    Fold<Maybe<Integer>, Integer> maybe = Traversals.forMaybe(integer).asFold();
    Fold<Map<String, Integer>, Integer> mapValues = Traversals.forMapValues(string, integer).asFold();
    Fold<Map<String, Integer>, Tuple2<String, Integer>> mapEntries =
        Traversals.forMapEntries(string, integer).asFold();
    Fold<String, Character> characters = StringTraversals.characters().asFold();
    LinkedHashMap<String, Integer> orderedMap = new LinkedHashMap<>();
    orderedMap.put("a", 1);
    orderedMap.put("b", 2);

    assertTrue(OpticMetadata.fold(firstList).isDefined());
    assertEquals(Types.list(integer), OpticMetadata.fold(firstList).get().sourceType());
    assertFalse(OpticMetadata.fold(Traversals.<Integer>forList().asFold()).isDefined());

    assertTrue(OpticMetadata.fold(maybe).isDefined());
    assertEquals(Types.maybe(integer), OpticMetadata.fold(maybe).get().sourceType());
    assertEquals(List.of(1), maybe.getAll(Maybe.some(1)));
    assertEquals(List.of(), maybe.getAll(Maybe.none()));

    assertTrue(OpticMetadata.fold(mapValues).isDefined());
    assertTrue(OpticMetadata.fold(mapEntries).isDefined());
    assertEquals(List.of(1, 2), mapValues.getAll(orderedMap));
    assertEquals(List.of(Tuple2.of("a", 1), Tuple2.of("b", 2)), mapEntries.getAll(orderedMap));

    assertTrue(OpticMetadata.fold(characters).isDefined());
    assertEquals(Types.witness(String.class), OpticMetadata.fold(characters).get().sourceType());
    assertEquals(Types.witness(Character.class), OpticMetadata.fold(characters).get().focusType());
    assertEquals(List.of('a', 'b', 'c'), characters.getAll("abc"));

    Tuple2<Integer, Boolean> fused =
        FoldQuery.count(firstList)
            .zipWith(FoldQuery.any(secondList, value -> value > 2), Tuple2::of)
            .run(List.of(1, 2, 3));
    assertEquals(Tuple2.of(3, true), fused);
  }

  @Test
  void eitherAndValidatedHelpersLowerToTypedFoldSpines() {
    var string = Types.witness(String.class);
    var integer = Types.witness(Integer.class);
    Fold<Either<String, Integer>, Integer> firstRight = EitherTraversals.right(string, integer).asFold();
    Fold<Either<String, Integer>, Integer> secondRight = Prisms.right(string, integer).asFold();
    Fold<Either<String, Integer>, String> left = EitherTraversals.left(string, integer).asFold();
    Fold<Validated<String, Integer>, Integer> firstValid = ValidatedTraversals.valid(string, integer).asFold();
    Fold<Validated<String, Integer>, Integer> secondValid = ValidatedTraversals.valid(string, integer).asFold();
    Fold<Validated<String, Integer>, Integer> validPrism = Prisms.valid(string, integer).asFold();
    Fold<Validated<String, Integer>, String> invalid = ValidatedTraversals.invalid(string, integer).asFold();
    Fold<Validated<String, Integer>, String> invalidPrism = Prisms.invalid(string, integer).asFold();

    assertTrue(OpticMetadata.fold(firstRight).isDefined());
    assertEquals(Types.or(string, integer), OpticMetadata.fold(firstRight).get().sourceType());
    assertEquals(integer, OpticMetadata.fold(firstRight).get().focusType());
    assertFalse(OpticMetadata.fold(EitherTraversals.<String, Integer>right().asFold()).isDefined());
    assertEquals(List.of(2), firstRight.getAll(Either.right(2)));
    assertEquals(List.of(), firstRight.getAll(Either.left("bad")));
    assertEquals(List.of("bad"), left.getAll(Either.left("bad")));

    Tuple2<Integer, Boolean> rightQuery =
        FoldQuery.count(firstRight)
            .zipWith(FoldQuery.any(secondRight, value -> value > 1), Tuple2::of)
            .run(Either.right(2));
    assertEquals(Tuple2.of(1, true), rightQuery);

    assertTrue(OpticMetadata.fold(firstValid).isDefined());
    assertEquals(Types.validated(string, integer), OpticMetadata.fold(firstValid).get().sourceType());
    assertEquals(integer, OpticMetadata.fold(firstValid).get().focusType());
    assertTrue(OpticMetadata.fold(invalid).isDefined());
    assertEquals(string, OpticMetadata.fold(invalid).get().focusType());
    assertTrue(OpticMetadata.fold(validPrism).isDefined());
    assertEquals(Types.validated(string, integer), OpticMetadata.fold(validPrism).get().sourceType());
    assertTrue(OpticMetadata.fold(invalidPrism).isDefined());
    assertEquals(string, OpticMetadata.fold(invalidPrism).get().focusType());
    assertFalse(OpticMetadata.fold(ValidatedTraversals.<String, Integer>valid().asFold()).isDefined());
    assertEquals(List.of(3), firstValid.getAll(Validated.valid(3)));
    assertEquals(List.of(), firstValid.getAll(Validated.invalid("name")));
    assertEquals(List.of("name"), invalid.getAll(Validated.invalid("name")));

    Tuple2<Integer, Boolean> validQuery =
        FoldQuery.count(firstValid)
            .zipWith(FoldQuery.any(secondValid, value -> value > 2), Tuple2::of)
            .run(Validated.valid(3));
    assertEquals(Tuple2.of(1, true), validQuery);
  }

  @Test
  void foldQueryRejectsFusionAcrossDifferentFoldInstances() {
    Fold<List<Integer>, Integer> first = Fold.of(list -> list);
    Fold<List<Integer>, Integer> second = Fold.of(list -> list);
    var sum = FoldQuery.foldMap(first, Monoid.of(0, Integer::sum), value -> value);
    var count = FoldQuery.foldMap(second, Monoid.of(0, Integer::sum), ignored -> 1);

    assertThrows(IllegalArgumentException.class, () -> sum.zip(count));
  }

  private static Fold<List<Integer>, Integer> typedCountingFold(String key, AtomicInteger visits) {
    Fold<List<Integer>, Integer> executable =
        new Fold<>() {
          @Override
          public <M> M foldMap(Monoid<M> monoid, Function<? super Integer, ? extends M> f, List<Integer> source) {
            M result = monoid.empty();
            for (Integer value : source) {
              visits.incrementAndGet();
              result = monoid.combine(result, f.apply(value));
            }
            return result;
          }
        };
    return PointFreeFold.opaque(
        key,
        executable,
        Types.list(Types.witness(Integer.class)),
        Types.witness(Integer.class));
  }
}
