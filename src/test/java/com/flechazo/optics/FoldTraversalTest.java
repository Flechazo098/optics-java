package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Pair;
import com.flechazo.hkt.functions.FoldQuery;
import com.flechazo.optics.util.Affines;
import com.flechazo.optics.util.Prisms;
import com.flechazo.optics.util.Traversals;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FoldTraversalTest {

  @Test
  void traversalCanReadAndModifyEveryListElement() {
    Traversal<List<Integer>, Integer> each = Each.<Integer>listEach().each();

    assertEquals(List.of(1, 2, 3), each.asFold().getAll(List.of(1, 2, 3)));
    assertEquals(List.of(2, 4, 6), each.modify(value -> value * 2, List.of(1, 2, 3)));
  }

  @Test
  void foldsTraversalsAndMapHelpersExposeIndexedAndReadOnlyComposition() {
    record Names(List<String> values) {}

    Fold<List<Object>, String> strings = Fold.<List<Object>, Object>of(list -> list)
        .andThen(Prisms.instanceOf(String.class));
    assertEquals(List.of("a", "b"), strings.getAll(List.of("a", 1, "b")));
    assertEquals(Optional.of("a"), strings.previewOptional(List.of("a", 1, "b")));
    assertEquals("fallback", strings.firstOrElse("fallback", List.of(1, 2)));
    assertEquals(Optional.of("b"), strings.findOptional(value -> value.equals("b"), List.of("a", 1, "b")));

    Fold<List<String>, String> listFold = Fold.of(list -> list);
    assertEquals(Maybe.some("b"), listFold.at(1).getMaybe(List.of("a", "b", "c")));
    assertEquals(Maybe.none(), listFold.at(3).getMaybe(List.of("a", "b", "c")));
    assertThrows(UnsupportedOperationException.class, () -> listFold.at(1).set("x", List.of("a", "b")));

    Lens<Names, List<String>> values = Lens.of(Names::values, (names, next) -> new Names(next));
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
        values.asSetter().andThen(Affine.listAt(0)).modify(String::toUpperCase, new Names(List.of("a", "b"))));

    Traversal<List<List<Integer>>, List<Integer>> rows = Traversals.forList();
    assertEquals(List.of(1, 2, 3), rows.andThen(Fold.of(row -> row)).getAll(List.of(List.of(1, 2), List.of(3))));
    assertEquals(List.of(2), rows.andThen(Affine.listAt(1)).getAll(List.of(List.of(1, 2), List.of(3))));
    assertEquals(List.of(List.of(1, 20), List.of(3)), rows.andThen(Affine.listAt(1)).modify(value -> value * 10, List.of(List.of(1, 2), List.of(3))));

    Fold<List<Names>, Names> names = Fold.of(list -> list);
    assertEquals(List.of(List.of("a", "b")), names.andThen(values).getAll(List.of(new Names(List.of("a", "b")))));
    assertEquals(
        List.of("a"),
        names.andThen(values.andThen(Affine.listAt(0))).getAll(List.of(new Names(List.of("a", "b")))));
    assertEquals(List.of("a", "b"), names.andThen(values.andThen(Traversals.forList())).getAll(List.of(new Names(List.of("a", "b")))));

    Traversal<List<Integer>, Integer> each = Traversals.forList();
    assertEquals(Maybe.some(2), each.at(1).getMaybe(List.of(1, 2, 3)));
    assertEquals(List.of(1, 20, 3), each.at(1).set(20, List.of(1, 2, 3)));
    assertEquals(List.of(1, 2, 3), each.at(5).set(20, List.of(1, 2, 3)));
    assertEquals(Maybe.some("b"), Affine.<String>listAt(1).getMaybe(List.of("a", "b")));
    assertEquals(List.of("a", "B"), Affine.<String>listAt(1).set("B", List.of("a", "b")));
    assertEquals(List.of("a"), Affine.<String>listAt(1).remove(List.of("a", "b")));
    assertArrayEquals(new String[] {"a", "B"}, Affine.<String>arrayAt(1).set("B", new String[] {"a", "b"}));

    LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);

    assertEquals(List.of("a", "b"), Fold.<String, Integer>mapKeys().getAll(map));
    assertEquals(List.of(1, 2), Fold.<String, Integer>mapValues().getAll(map));
    assertEquals(List.of("a", "b"), Fold.<String, Integer>mapEntries().getAll(map).stream().map(Map.Entry::getKey).toList());
    assertEquals(Maybe.some(1), Affine.<String, Integer>mapValue("a").getMaybe(map));
    assertEquals(Map.of("a", 10, "b", 2), Affine.<String, Integer>mapValue("a").set(10, map));
    assertEquals(map, Affine.<String, Integer>mapValue("z").set(10, map));
    assertEquals(Map.of("b", 2), Affine.<String, Integer>mapValue("a").remove(map));
    assertEquals(Pair.of("a", 1), Affine.<String, Integer>mapEntry("a").getMaybe(map).get());
    assertEquals(Map.of("alpha", 10, "b", 2), Affine.<String, Integer>mapEntry("a").set(Pair.of("alpha", 10), map));
    assertEquals(Map.of("a", 2, "b", 3), Traversal.<String, Integer>mapValues().modify(value -> value + 1, map));
    assertEquals(
        Map.of("ka", 1, "kb", 2),
        Traversal.<String, Integer>mapEntries().modify(entry -> Pair.of("k" + entry.first(), entry.second()), map));
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

    Pair<Integer, Boolean> result =
        fold.foldMap2(
            Monoid.of(0, Integer::sum),
            value -> value,
            Monoid.of(false, Boolean::logicalOr),
            value -> value > 2,
            Pair::of,
            List.of(1, 2, 3));

    assertEquals(Pair.of(6, true), result);
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
    Pair<Integer, Boolean> result = sum.zipWith(hasEven, Pair::of).run(List.of(1, 2, 3));

    assertEquals(Pair.of(6, true), result);
    assertEquals(3, visits.get());
  }

  @Test
  void foldQueryRejectsFusionAcrossDifferentFoldInstances() {
    Fold<List<Integer>, Integer> first = Fold.of(list -> list);
    Fold<List<Integer>, Integer> second = Fold.of(list -> list);
    var sum = FoldQuery.foldMap(first, Monoid.of(0, Integer::sum), value -> value);
    var count = FoldQuery.foldMap(second, Monoid.of(0, Integer::sum), ignored -> 1);

    assertThrows(IllegalArgumentException.class, () -> sum.zip(count));
  }
}
