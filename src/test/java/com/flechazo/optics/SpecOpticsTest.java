package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.generated.SpecOptics;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SpecOpticsTest {

  @Test
  void specOpticsDerivesMethodsFromSpecInterfaceWithoutAnnotationProcessing() {
    SpecOptics.GeneratedSpec<Team> generated = SpecOptics.generate(TeamSpec.class, Team.class);
    TeamSpec implementation = generated.implementation(TeamSpec.class);
    User owner = new User("Grace", new Address("Paris", 75000));
    Team team = new Team("core", List.of(new User("Ada", new Address("London", 12345))), Maybe.some(owner));
    @SuppressWarnings("unchecked")
    Traversal<Team, Team, User, User> users = generated.optic("users", Traversal.class);

    assertThrows(ClassCastException.class, () -> generated.optic("name", Prism.class));
    assertEquals("core", generated.<String>lens("name").get(team));
    assertEquals("core", generated.<String>getter("name").get(team));
    assertEquals("runtime", generated.<String>setter("name").set("runtime", team).name());
    assertEquals(List.of("core"), generated.<String>fold("name").getAll(team));
    assertEquals(List.of("core"), generated.<String>traversal("name").getAll(team));
    assertEquals("core", implementation.name().get(team));
    assertEquals(List.of("Ada"), userNames(implementation.users().getAll(team)));
    assertEquals(Maybe.some(owner), implementation.owner().getMaybe(team));
    Maybe<User> updatedOwner =
        generated.<User>affine("owner").set(new User("Linus", owner.address()), team).owner();
    assertEquals("Linus", Objects.requireNonNull(updatedOwner.get()).name());
    assertEquals(List.of("Ada"), userNames(generated.<User>traversal("users").getAll(team)));
    assertEquals(List.of("Ada"), userNames(generated.<User>fold("users").getAll(team)));
    assertEquals(
            List.of("ADA"),
            generated.<User>setter("users").modify(user -> new User(user.name().toUpperCase(), user.address()), team)
                    .users()
                    .stream()
                    .map(User::name)
                    .toList());
    assertEquals(List.of("Ada"), userNames(users.getAll(team)));

    SpecOptics.GeneratedSpec<OptionalTeam> optionalGenerated =
        SpecOptics.generate(OptionalTeamSpec.class, OptionalTeam.class);
    OptionalTeamSpec optionalImplementation = optionalGenerated.implementation(OptionalTeamSpec.class);
    OptionalTeam optionalTeam = new OptionalTeam("core", Optional.of(owner));

    assertEquals(Maybe.some(owner), optionalImplementation.owner().getMaybe(optionalTeam));
    assertEquals(
        Optional.of(new User("Linus", owner.address())),
        optionalGenerated.<User>affine("owner").set(new User("Linus", owner.address()), optionalTeam).owner());
    assertThrows(NullPointerException.class, () -> optionalGenerated.<User>affine("owner").set(null, optionalTeam));
  }

  private static List<String> userNames(List<User> users) {
    return users.stream().map(User::name).toList();
  }
}
