package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.App;
import com.flechazo.hkt.App2;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.Monad;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Natural;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.Tuple2;
import com.flechazo.hkt.Validated;
import com.flechazo.optics.generated.ClassFileOptics;
import com.flechazo.optics.generated.OpticsSpec;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.generated.SpecOptics;
import com.flechazo.optics.focus.FocusPath;
import com.flechazo.hkt.IdF;
import com.flechazo.optics.indexed.IndexedTraversal;
import com.flechazo.optics.indexed.Pair;
import com.flechazo.optics.instances.AtInstances;
import com.flechazo.optics.instances.EachInstances;
import com.flechazo.optics.instances.IxedInstances;
import com.flechazo.optics.util.Affines;
import com.flechazo.optics.util.EitherTraversals;
import com.flechazo.optics.util.ListPrisms;
import com.flechazo.optics.util.Optionals;
import com.flechazo.optics.util.Prisms;
import com.flechazo.optics.util.StringTraversals;
import com.flechazo.optics.util.TryTraversals;
import com.flechazo.optics.util.TupleTraversals;
import com.flechazo.optics.util.Traversals;
import com.flechazo.optics.util.ValidatedTraversals;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OpticsCoreTest {
  record Address(String city, int zip) {}

  record User(String name, Address address) {}

  record Team(String name, List<User> users, Maybe<User> owner) {}

  record OptionalTeam(String name, Optional<User> owner) {}

  record LookupBox(String value, Optional<Integer> count) {}

  record Containers(
      List<Integer> list,
      Set<Integer> set,
      Map<String, Integer> map,
      Maybe<Integer> maybe,
      Optional<Integer> optional,
      int[] ints) {}

  interface TeamSpec extends OpticsSpec<Team> {
    Lens<Team, String> name();

    Traversal<Team, User> users();

    Affine<Team, User> owner();
  }

  interface OptionalTeamSpec extends OpticsSpec<OptionalTeam> {
    Affine<OptionalTeam, User> owner();
  }

  interface LookupBoxSpec extends OpticsSpec<LookupBox> {
    Lens<LookupBox, String> value();

    Affine<LookupBox, Integer> count();
  }

  sealed interface Shape permits Circle, Rect {}

  record Circle(int radius) implements Shape {}

  record Rect(int width, int height) implements Shape {}

  @Test
  void lensComposesAndModifiesRecords() {
    Lens<User, Address> address =
        Lens.of(User::address, (user, value) -> new User(user.name(), value));
    Lens<Address, String> city =
        Lens.of(Address::city, (addr, value) -> new Address(value, addr.zip()));

    User user = new User("Ada", new Address("london", 12345));

    assertEquals("london", address.andThen(city).get(user));
    assertEquals("LONDON", address.andThen(city).modify(String::toUpperCase, user).address().city());
    assertEquals("LONDON", address.andThen(city).modifyWhen(value -> value.length() > 3, String::toUpperCase, user).address().city());
  }

  @Test
  void pairedLensUsesSingleIndexedPairType() {
    record Range(int lo, int hi) {}
    Lens<Range, Integer> lo = Lens.of(Range::lo, (range, value) -> new Range(value, range.hi()));
    Lens<Range, Integer> hi = Lens.of(Range::hi, (range, value) -> new Range(range.lo(), value));
    Lens<Range, Pair<Integer, Integer>> bounds = Lens.paired(lo, hi, Range::new);

    assertEquals(Pair.of(1, 3), bounds.get(new Range(1, 3)));
    assertEquals(
        new Range(2, 6),
        bounds.modify(
            pair -> Pair.of(
                Objects.requireNonNull(pair.first()) + 1,
                Objects.requireNonNull(pair.second()) * 2),
            new Range(1, 3)));
  }

  @Test
  void pureSetterDoesNotPretendToSupportEffectfulModifyF() {
    record Box(int value) {}
    Setter<Box, Integer> pure = Setter.of((f, box) -> new Box(f.apply(box.value())));
    Lens<Box, Integer> lens = Lens.of(Box::value, (box, value) -> new Box(value));
    Setter<Box, Integer> fromLens = lens.asSetter();
    Setter<Box, Integer> fromGetSet = Setter.fromGetSet(Box::value, (box, value) -> new Box(value));

    assertEquals(new Box(2), pure.modify(value -> value + 1, new Box(1)));
    assertThrows(
        UnsupportedOperationException.class,
        () -> pure.modifyF(value -> Maybe.some(value + 1), new Box(1), Maybe.applicative()));
    assertEquals(Maybe.some(new Box(2)), fromLens.modifyF(value -> Maybe.some(value + 1), new Box(1), Maybe.applicative()));
    assertEquals(Maybe.some(new Box(2)), fromGetSet.modifyF(value -> Maybe.some(value + 1), new Box(1), Maybe.applicative()));
    assertEquals(
        Maybe.<Box>none(),
        Maybe.narrow(fromLens.modifyF(value -> Maybe.none(), new Box(1), Maybe.applicative())));
  }

  @Test
  void traversalCanReadAndModifyEveryListElement() {
    Traversal<List<Integer>, Integer> each = EachInstances.<Integer>listEach().each();

    assertEquals(List.of(1, 2, 3), each.asFold().getAll(List.of(1, 2, 3)));
    assertEquals(List.of(2, 4, 6), each.modify(value -> value * 2, List.of(1, 2, 3)));
  }

  @Test
  void indexedTraversalKeepsPositions() {
    IndexedTraversal<Integer, List<String>, String> indexed = IndexedTraversal.forList();

    assertEquals(List.of("0:a", "1:b"), indexed.imodify((i, value) -> i + ":" + value, List.of("a", "b")));
    assertEquals(
        List.of(Pair.of(0, "a"), Pair.of(1, "b")),
        indexed.asIndexedFold().toIndexedList(List.of("a", "b")));
    assertEquals(List.of("a"), indexed.asIndexedFold().filterIndex(i -> i == 0).getAll(List.of("a", "b")));
    assertTrue(indexed.asIndexedFold().all(value -> value.length() == 1, List.of("a", "b")));
  }

  @Test
  void atAndIxedMapInstancesAreImmutable() {
    Map<String, Integer> source = new LinkedHashMap<>();
    source.put("a", 1);

    Map<String, Integer> inserted = AtInstances.<String, Integer>mapAt().insertOrUpdate("b", 2, source);
    Map<String, Integer> modified = IxedInstances.<String, Integer>mapIxed().ix("a").modify(value -> value + 10, inserted);
    Map<String, Integer> missing = IxedInstances.<String, Integer>mapIxed().ix("z").modify(value -> value + 10, modified);

    assertFalse(source.containsKey("b"));
    assertEquals(Map.of("a", 11, "b", 2), modified);
    assertEquals(modified, missing);
  }

  @Test
  void recordOpticsCreatesComponentLensesFromClassFileBackedGenerator() {
    Lens<User, String> name = RecordOptics.recordLens(User.class, "name");
    Lens<User, String> nameByReference = RecordOptics.recordLens(User.class, User::name);
    Lens<User, String> facadeNameByReference = ClassFileOptics.lens(User.class, User::name);
    User user = new User("Ada", new Address("London", 12345));

    assertTrue(RecordOptics.generateLensHostBytes(User.class).length > 0);
    assertEquals("Ada", name.get(user));
    assertEquals("Ada", nameByReference.get(user));
    assertEquals("Grace", facadeNameByReference.set("Grace", user).name());
    assertEquals("Grace", name.set("Grace", user).name());
    assertEquals(Maybe.some("Ada"), name.asFold().preview(user));

    Lens<Address, Integer> zip = RecordOptics.recordLens(Address.class, "zip");
    assertEquals(12345, zip.get(user.address()));
    assertEquals(54321, zip.set(54321, user.address()).zip());
  }

  @Test
  void recordOpticsCreatesSubtypePrismsForSealedHierarchies() {
    Prism<Shape, Circle> circle = RecordOptics.subtypePrism(Shape.class, Circle.class);
    Shape shape = new Circle(5);

    assertEquals(Maybe.some(new Circle(5)), circle.getMaybe(shape));
    assertEquals(Maybe.none(), circle.getMaybe(new Rect(2, 3)));
    assertEquals(new Circle(7), circle.modify(c -> new Circle(c.radius() + 2), shape));
    assertTrue(RecordOptics.sealedSubtypePrisms(Shape.class).containsKey(Circle.class));
  }

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

  @Test
  void specOpticsDerivesMethodsFromSpecInterfaceWithoutAnnotationProcessing() {
    SpecOptics.GeneratedSpec<Team> generated = SpecOptics.generate(TeamSpec.class, Team.class);
    TeamSpec implementation = generated.implementation(TeamSpec.class);
    User owner = new User("Grace", new Address("Paris", 75000));
    Team team = new Team("core", List.of(new User("Ada", new Address("London", 12345))), Maybe.some(owner));
    @SuppressWarnings("unchecked")
    Traversal<Team, User> users = generated.optic("users", Traversal.class);

    assertThrows(ClassCastException.class, () -> generated.optic("name", Prism.class));
    assertEquals("core", generated.<String>lens("name").get(team));
    assertEquals("core", generated.<String>getter("name").get(team));
    assertEquals("runtime", generated.<String>setter("name").set("runtime", team).name());
    assertEquals(List.of("core"), generated.<String>fold("name").getAll(team));
    assertEquals(List.of("core"), generated.<String>traversal("name").getAll(team));
    assertEquals("core", implementation.name().get(team));
    assertEquals(List.of("Ada"), implementation.users().getAll(team).stream().map(User::name).toList());
    assertEquals(Maybe.some(owner), implementation.owner().getMaybe(team));
    assertEquals(Maybe.none(), implementation.owner().remove(team).owner());
    Maybe<User> updatedOwner =
        generated.<User>affine("owner").set(new User("Linus", owner.address()), team).owner();
    assertEquals("Linus", Objects.requireNonNull(updatedOwner.get()).name());
    assertEquals(List.of("Ada"), generated.<User>traversal("users").getAll(team).stream().map(User::name).toList());
    assertEquals(List.of("Ada"), generated.<User>fold("users").getAll(team).stream().map(User::name).toList());
    assertEquals(
        List.of("ADA"),
        generated.<User>setter("users").modify(user -> new User(user.name().toUpperCase(), user.address()), team)
            .users()
            .stream()
            .map(User::name)
            .toList());
    assertEquals(List.of("Ada"), users.getAll(team).stream().map(User::name).toList());

    SpecOptics.GeneratedSpec<OptionalTeam> optionalGenerated =
        SpecOptics.generate(OptionalTeamSpec.class, OptionalTeam.class);
    OptionalTeamSpec optionalImplementation = optionalGenerated.implementation(OptionalTeamSpec.class);
    OptionalTeam optionalTeam = new OptionalTeam("core", Optional.of(owner));

    assertEquals(Maybe.some(owner), optionalImplementation.owner().getMaybe(optionalTeam));
    assertEquals(Optional.empty(), optionalImplementation.owner().remove(optionalTeam).owner());
    assertEquals(
        Optional.of(new User("Linus", owner.address())),
        optionalGenerated.<User>affine("owner").set(new User("Linus", owner.address()), optionalTeam).owner());
    assertThrows(NullPointerException.class, () -> optionalGenerated.<User>affine("owner").set(null, optionalTeam));
  }

  @Test
  void widerCoreTraversalsCoverEitherTryValidatedAndTuple() {
    assertEquals(Either.right(11), EitherTraversals.<String, Integer>right().modify(value -> value + 1, Either.right(10)));
    assertEquals(Try.success(11), TryTraversals.<Integer>success().modify(value -> value + 1, Try.success(10)));
    assertEquals(
        Validated.valid(11),
        ValidatedTraversals.<String, Integer>valid().modify(value -> value + 1, Validated.valid(10)));
    assertEquals(Tuple2.of("a", 11), TupleTraversals.<String, Integer>second().modify(value -> value + 1, Tuple2.of("a", 10)));
  }

  @Test
  void hktTypeclassesHaveConcreteInstancesAndTransformations() {
    Monad<Maybe.Mu> maybeMonad = Maybe.monad();
    Selective<Maybe.Mu> maybeSelective = Maybe.selective();
    App<Maybe.Mu, Integer> maybe =
        maybeMonad.flatMap(value -> Maybe.some(value + 1), Maybe.some(1));

    assertEquals(Maybe.some(2), Maybe.narrow(maybe));
    assertEquals(
        Maybe.some("yes"),
        Maybe.narrow(maybeSelective.ifS(Maybe.some(true), () -> Maybe.some("yes"), () -> Maybe.some("no"))));
    assertEquals(
        Maybe.some("else"),
        Maybe.narrow(maybeSelective.ifS(
            Maybe.some(false),
            () -> {
              throw new AssertionError("then branch should not be evaluated");
            },
            () -> Maybe.some("else"))));
    assertEquals(
        Maybe.some("ready"),
        Maybe.narrow(maybeSelective.select(Maybe.some(Either.right("ready")), Maybe.none())));
    assertEquals(Maybe.some(null), Maybe.narrow(maybeMonad.map(value -> value, Maybe.some(null))));
    assertThrows(NullPointerException.class, () -> maybeMonad.flatMap(value -> null, Maybe.some(1)));

    Monad<App2.Mu<Either.Mu, String>> eitherMonad = Either.monad();
    Selective<App2.Mu<Either.Mu, String>> eitherSelective = Either.selective();
    App<App2.Mu<Either.Mu, String>, Integer> right =
        eitherMonad.flatMap(value -> Either.right(value + 1), Either.right(1));
    App<App2.Mu<Either.Mu, String>, Integer> left =
        eitherMonad.flatMap(value -> Either.right(value + 1), Either.<String, Integer>left("bad"));
    App2<Either.Mu, String, Integer> right2 = Either.right(3);

    assertEquals(Either.right(2), Either.narrow(right));
    assertEquals(Either.left("bad"), Either.narrow(left));
    assertEquals(Either.right(3), Either.narrow2(right2));
    assertEquals(
        Either.right("else"),
        Either.narrow(eitherSelective.ifS(
            Either.right(false),
            () -> Either.left("then"),
            () -> Either.right("else"))));

    var validatedApplicative = Validated.<String>selective((firstError, secondError) -> firstError + "+" + secondError);
    App<App2.Mu<Validated.Mu, String>, Integer> invalid =
        validatedApplicative.map2(
            Validated.invalid("name"),
            Validated.invalid("age"),
            Integer::sum);

    assertEquals(Validated.invalid("name+age"), Validated.narrow(invalid));
    assertEquals(
        Validated.invalid("selector+function"),
        Validated.narrow(validatedApplicative.select(
            Validated.invalid("selector"),
            Validated.invalid("function"))));
    assertThrows(NullPointerException.class, () -> Validated.invalid(null));
    assertThrows(NullPointerException.class, () -> Monoid.of(null, String::concat));

    Monoid<String> log = Monoid.of("", String::concat);
    Monad<App2.Mu<Tuple2.Mu, String>> writer = Tuple2.monad(log);
    App<App2.Mu<Tuple2.Mu, String>, Integer> written =
        writer.flatMap(value -> Tuple2.of("b", value + 1), Tuple2.of("a", 1));

    assertEquals(Tuple2.of("ab", 2), Tuple2.narrow(written));
    assertThrows(
        NullPointerException.class,
        () -> writer.flatMap(value -> Tuple2.of("b", value + 1), Tuple2.of(null, 1)));

    Monad<Try.Mu> tryMonad = Try.monad();
    Selective<Try.Mu> trySelective = Try.selective();
    App<Try.Mu, Integer> success =
        tryMonad.flatMap(value -> Try.success(value + 1), Try.success(1));
    App<Try.Mu, Integer> failure =
        tryMonad.flatMap(
            value -> {
              throw new IllegalStateException("boom");
            },
            Try.success(1));

    assertEquals(Try.success(2), Try.narrow(success));
    assertTrue(Try.narrow(failure).isFailure());
    assertEquals(
        Try.success("else"),
        Try.narrow(trySelective.ifS(
            Try.success(false),
            () -> Try.failure(new IllegalStateException("then")),
            () -> Try.success("else"))));
    assertThrows(
        AssertionError.class,
        () -> Try.of(
            () -> {
              throw new AssertionError("fatal");
            }));
    assertTrue(Try.of(
            () -> {
              throw new Exception("checked");
            })
        .isFailure());

    Natural<Maybe.Mu, Try.Mu> maybeToTry =
        new Natural<>() {
          @Override
          public <A> App<Try.Mu, A> apply(App<Maybe.Mu, A> value) {
            Maybe<A> maybeValue = Maybe.narrow(value);
            return maybeValue.isDefined()
                ? Try.success(maybeValue.get())
                : Try.failure(new IllegalStateException("none"));
          }
        };
    Natural<Try.Mu, IdF.Mu> tryToId =
        new Natural<>() {
          @Override
          public <A> App<IdF.Mu, A> apply(App<Try.Mu, A> value) {
            return new IdF<>(Try.narrow(value).get());
          }
        };

    assertEquals(3, IdF.narrow(maybeToTry.andThen(tryToId).apply(Maybe.some(3))).value());
    assertEquals(Maybe.some(4), Maybe.narrow(Natural.<Maybe.Mu>identity().apply(Maybe.some(4))));
  }

  @Test
  void maybePrismAndTraversalUtilitiesWorkWithLibraryMaybe() {
    Prism<Maybe<String>, String> some = Prisms.some();
    Traversal<List<Integer>, Integer> list = Traversals.forList();

    assertEquals(Maybe.some("x"), some.getMaybe(Maybe.some("x")));
    assertEquals(Maybe.none(), some.getMaybe(Maybe.none()));
    assertEquals(List.of(2, 3, 4), Traversals.modify(list, value -> value + 1, List.of(1, 2, 3)));
  }

  @Test
  void utilLayerExposesOptionalAdaptersWhileCoreKeepsMaybe() {
    Prism<Maybe<String>, String> some = Prisms.some();
    Traversal<List<Integer>, Integer> list = Traversals.forList();

    assertEquals(Maybe.some("x"), some.getMaybe(Maybe.some("x")));
    assertEquals(Optional.of("x"), Prisms.getOptional(some, Maybe.some("x")));
    assertEquals(Optional.empty(), Prisms.getOptional(some, Maybe.none()));
    assertEquals(Optional.of("X"), Prisms.mapOptional(some, String::toUpperCase, Maybe.some("x")));

    assertEquals(Optional.of(1), Traversals.previewOptional(list, List.of(1, 2, 3)));
    assertEquals(Optional.of(2), Traversals.findOptional(list, value -> value > 1, List.of(1, 2, 3)));
    assertEquals(Optional.empty(), Traversals.findOptional(list, value -> value > 9, List.of(1, 2, 3)));

    assertEquals(Optional.of("a"), Affines.getOptional(Affines.optionalValue(), Optional.of("a")));
    assertEquals(Optional.empty(), Affines.getOptional(Affines.optionalValue(), Optional.empty()));
    assertEquals(Optional.of("a"), ListPrisms.headOptional(List.of("a", "b")));
    assertEquals(Optional.empty(), ListPrisms.headOptional(List.of()));

    assertEquals(Optional.of("x"), Optionals.fromMaybe(Maybe.some("x")));
    assertEquals(Maybe.some("x"), Optionals.toMaybe(Optional.of("x")));
    assertThrows(NullPointerException.class, () -> Optionals.fromMaybe(Maybe.some(null)));
  }

  @Test
  void optionalSetAndBuildRejectNullInsteadOfRemoving() {
    assertThrows(NullPointerException.class, () -> Affines.<String>optionalValue().set(null, Optional.of("a")));
    assertThrows(NullPointerException.class, () -> Prisms.<String>optionalSome().build(null));
  }

  @Test
  void affineFilteredDoesNotWriteWhenCurrentFocusDoesNotMatch() {
    Affine<Maybe<Integer>, Integer> positive = Affines.<Integer>maybeValue().filtered(value -> value > 0);

    assertEquals(Maybe.some(1), positive.getMaybe(Maybe.some(1)));
    assertEquals(Maybe.none(), positive.getMaybe(Maybe.some(-1)));
    assertEquals(Maybe.none(), positive.getMaybe(Maybe.none()));
    assertEquals(Maybe.some(2), positive.set(2, Maybe.some(1)));
    assertEquals(Maybe.some(-1), positive.set(2, Maybe.some(-1)));
    assertEquals(Maybe.none(), positive.set(2, Maybe.none()));
    assertEquals(Maybe.none(), positive.remove(Maybe.some(1)));
    assertEquals(Maybe.some(-1), positive.remove(Maybe.some(-1)));
  }

  @Test
  void writableOpticsDowngradeToEffectfulSetters() {
    Prism<Shape, Circle> circle = RecordOptics.subtypePrism(Shape.class, Circle.class);
    Affine<Maybe<Integer>, Integer> some = Affines.maybeValue();
    Traversal<List<Integer>, Integer> each = Traversals.forList();
    Setter<Shape, Circle> prismSetter = circle.asSetter();
    Setter<Maybe<Integer>, Integer> affineSetter = some.asSetter();
    Setter<List<Integer>, Integer> traversalSetter = each.asSetter();

    assertEquals(
        Maybe.some((Shape) new Circle(6)),
        prismSetter.modifyF(value -> Maybe.some(new Circle(value.radius() + 1)), new Circle(5), Maybe.applicative()));
    assertEquals(
        Maybe.some((Shape) new Rect(2, 3)),
        prismSetter.modifyF(value -> Maybe.some(new Circle(value.radius() + 1)), new Rect(2, 3), Maybe.applicative()));
    assertEquals(
        Maybe.some(Maybe.some(2)),
        affineSetter.modifyF(value -> Maybe.some(value + 1), Maybe.some(1), Maybe.applicative()));
    assertEquals(
        Maybe.some(Maybe.<Integer>none()),
        Maybe.narrow(affineSetter.modifyF(value -> Maybe.some(value + 1), Maybe.none(), Maybe.applicative())));
    assertEquals(
        Maybe.some(List.of(2, 3)),
        traversalSetter.modifyF(value -> Maybe.some(value + 1), List.of(1, 2), Maybe.applicative()));
  }

  @Test
  void pairSupportsMappingAndFoldingBothSides() {
    Pair<Integer, String> pair = Pair.of(1, "a");

    assertEquals(Pair.of(2, "a"), pair.mapFirst(value -> value + 1));
    assertEquals(Pair.of(1, "A"), pair.mapSecond(String::toUpperCase));
    assertEquals(Pair.of(2, "A"), pair.mapBoth(value -> value + 1, String::toUpperCase));
    assertEquals("1:a", pair.fold((first, second) -> first + ":" + second));
  }

  @Test
  void stringTraversalRebuildsImmutableStrings() {
    assertEquals("ABC", StringTraversals.characters().modify(Character::toUpperCase, "abc"));
  }
}
