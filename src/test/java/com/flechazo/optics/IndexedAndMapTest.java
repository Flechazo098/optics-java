package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.Tuple2;
import com.flechazo.optics.indexed.IndexedTraversal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IndexedAndMapTest {

  @Test
  void indexedTraversalKeepsPositions() {
    IndexedTraversal<Integer, List<String>, String> indexed = IndexedTraversal.forList();

    assertEquals(List.of("0:a", "1:b"), indexed.imodify((i, value) -> i + ":" + value, List.of("a", "b")));
    assertEquals(
        List.of(Tuple2.of(0, "a"), Tuple2.of(1, "b")),
        indexed.asIndexedFold().toIndexedList(List.of("a", "b")));
    assertEquals(List.of("a"), indexed.asIndexedFold().filterIndex(i -> i == 0).getAll(List.of("a", "b")));
    assertTrue(indexed.asIndexedFold().all(value -> value.length() == 1, List.of("a", "b")));
  }

  @Test
  void atAndIxedMapInstancesAreImmutable() {
    Map<String, Integer> source = new LinkedHashMap<>();
    source.put("a", 1);

    Map<String, Integer> inserted = At.<String, Integer>mapAt().insertOrUpdate("b", 2, source);
    Map<String, Integer> modified = Ixed.<String, Integer>mapIxed().ix("a").modify(value -> value + 10, inserted);
    Map<String, Integer> missing = Ixed.<String, Integer>mapIxed().ix("z").modify(value -> value + 10, modified);

    assertFalse(source.containsKey("b"));
    assertEquals(Map.of("a", 11, "b", 2), modified);
    assertEquals(modified, missing);
  }
}
