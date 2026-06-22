package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.focus.FocusPath;
import com.flechazo.optics.generated.ClassFileOptics;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.generated.SpecOptics;
import com.flechazo.optics.util.Traversals;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClassFileOpticsTest {

  @Test
  void classFileOpticsFacadeGeneratesRecordAndSubtypeOpticsWithoutAnnotations() {
    Team team =
        new Team(
            "core",
            List.of(new User("Ada", new Address("London", 12345))),
            Maybe.some(new User("Grace", new Address("Paris", 75000))));

    assertEquals("core", ClassFileOptics.getters(Team.class).get("name").get(team));
    assertEquals(List.of(team.users()), ClassFileOptics.folds(Team.class).get("users").getAll(team));
    assertEquals(2, ClassFileOptics.traversals(Team.class).size());
    assertTrue(ClassFileOptics.prisms(Shape.class).containsKey(Rect.class));
    assertTrue(ClassFileOptics.generatedHostBytes(Team.class).length > 0);
  }

  @Test
  void generatedOpticsAcceptCallerLookupForHiddenClassDefinition() {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    LookupBox source = new LookupBox("core", Optional.of(3));

    assertEquals("core", RecordOptics.recordLens(LookupBox.class, "value", lookup).get(source));
    assertEquals(List.of(3), ClassFileOptics.traversals(LookupBox.class, lookup).get("count").getAll(source));

    LookupBoxSpec implementation = SpecOptics.implementation(LookupBoxSpec.class, LookupBox.class, lookup);
    assertEquals("core", implementation.value().get(source));
    assertEquals(Maybe.some(3), implementation.count().getMaybe(source));
  }

  @Test
  void classFileRecordTraversalsCoverContainerFieldsDirectly() {
    LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    Containers source =
        new Containers(
            List.of(1, 2),
            new LinkedHashSet<>(List.of(3, 4)),
            map,
            Maybe.some(5),
            Optional.of(6),
            new int[] {6, 7});
    Map<String, Traversal<Containers, ?>> traversals = ClassFileOptics.traversals(Containers.class);

    assertEquals(Set.of("list", "set", "map", "maybe", "optional", "ints"), traversals.keySet());
    assertEquals(List.of(1, 2), traversals.get("list").getAll(source));
    assertEquals(List.of(3, 4), traversals.get("set").getAll(source));
    assertEquals(List.of(1, 2), traversals.get("map").getAll(source));
    assertEquals(List.of(5), traversals.get("maybe").getAll(source));
    assertEquals(List.of(6), traversals.get("optional").getAll(source));
    assertEquals(List.of(6, 7), traversals.get("ints").getAll(source));

    @SuppressWarnings("unchecked")
    Traversal<Containers, Integer> list = (Traversal<Containers, Integer>) traversals.get("list");
    @SuppressWarnings("unchecked")
    Traversal<Containers, Integer> optional = (Traversal<Containers, Integer>) traversals.get("optional");
    @SuppressWarnings("unchecked")
    Traversal<Containers, Integer> ints = (Traversal<Containers, Integer>) traversals.get("ints");

    assertEquals(List.of(10, 20), list.modify(value -> value * 10, source).list());
    assertEquals(Optional.of(60), optional.modify(value -> value * 10, source).optional());
    assertEquals(List.of(60, 70), ints.getAll(ints.modify(value -> value * 10, source)));
    assertEquals(
        Maybe.some(
            new Containers(
                List.of(2, 3), source.set(), source.map(), source.maybe(), source.optional(), source.ints())),
        list.modifyF(value -> Maybe.some(value + 1), source, Maybe.applicative()));
    assertEquals(
        Maybe.<Containers>none(),
        Maybe.narrow(list.modifyF(value -> value == 2 ? Maybe.none() : Maybe.some(value), source, Maybe.applicative())));
  }

  @Test
  void focusDslComposesGeneratedPaths() {
    @SuppressWarnings("unchecked")
    FocusPath<Team, List<User>> users =
        (FocusPath<Team, List<User>>) ClassFileOptics.focus(Team.class).get("users");
    Lens<User, String> userName = Lens.of(User::name, (user, name) -> new User(name, user.address()));
    Team team = new Team("core", List.of(new User("Ada", new Address("London", 12345))), Maybe.none());

    assertEquals(List.of("Ada"), users.each(Traversals.forList()).via(userName).getAll(team));
    assertEquals(
        List.of("ADA"),
        users.each(Traversals.forList()).via(userName).modify(String::toUpperCase, team).users().stream()
            .map(User::name)
            .toList());
  }
}
