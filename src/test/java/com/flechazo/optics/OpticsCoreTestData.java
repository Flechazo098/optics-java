package com.flechazo.optics;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.generated.OpticsSpec;
import com.flechazo.hkt.functions.OpticApp;
import com.flechazo.hkt.functions.PointFree;
import com.flechazo.hkt.functions.PointFreeOpticKind;
import com.flechazo.hkt.functions.ProductOpticElement;
import com.flechazo.hkt.functions.ProductSide;
import com.flechazo.hkt.functions.SumOpticElement;
import com.flechazo.hkt.functions.SumSide;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
  PLens<Team, Team, String, String> name();

  PTraversal<Team, Team, User, User> users();

  PAffine<Team, Team, User, User> owner();
}

interface OptionalTeamSpec extends OpticsSpec<OptionalTeam> {
  PAffine<OptionalTeam, OptionalTeam, User, User> owner();
}

interface LookupBoxSpec extends OpticsSpec<LookupBox> {
  PLens<LookupBox, LookupBox, String, String> value();

  PAffine<LookupBox, LookupBox, Integer, Integer> count();
}

sealed interface Shape permits Circle, Rect, Square {}

record Circle(int radius) implements Shape {}

record Square(int side) implements Shape {}

record Rect(int width, int height) implements Shape {}

record SourceBox(int value) {}

record TargetBox(String value) {}

record Box(int value) {}

record Account(String name, List<Integer> scores) {}

class OpticTestHelpers {
  static boolean isLensApp(PointFree<?> expression) {
    return expression instanceof OpticApp<?, ?, ?, ?> opticApp
        && opticApp.optic().containsOnly(PointFreeOpticKind.LENS);
  }

  static boolean isProductApp(PointFree<?> expression, ProductSide side) {
    return expression instanceof OpticApp<?, ?, ?, ?> opticApp
        && opticApp.optic().outermost().untyped() instanceof ProductOpticElement(ProductSide side1)
        && side1 == side;
  }

  static boolean isSumApp(PointFree<?> expression, SumSide side) {
    return expression instanceof OpticApp<?, ?, ?, ?> opticApp
        && opticApp.optic().outermost().untyped() instanceof SumOpticElement(SumSide side1)
        && side1 == side;
  }

  static PointFree<?> opticFunction(PointFree<?> expression) {
    return ((OpticApp<?, ?, ?, ?>) expression).function();
  }
}
