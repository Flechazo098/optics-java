package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.functions.LensPath;
import com.flechazo.hkt.functions.PointFree;
import com.flechazo.hkt.functions.PointFreeOptimizer;
import com.flechazo.hkt.functions.RewritePlan;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class RewritePlanTest {

  @Test
  void rewritePlanFusesRepeatedLensApplications() {
    record Box(int value) {}
    AtomicInteger sets = new AtomicInteger();
    Lens<Box, Integer> value =
        Lens.of(
            Box::value,
            (box, next) -> {
              sets.incrementAndGet();
              return new Box(next);
            });
    LensPath<Box, Integer> path = LensPath.of("value", value);

    RewritePlan<Box> plan =
        RewritePlan.<Box>identity()
            .modify(path, current -> current + 1)
            .modify(path, current -> current * 2)
            .optimize();

    assertEquals(new Box(4), plan.apply(new Box(1)));
    assertEquals(1, plan.size());
    assertEquals(1, sets.get());
  }

  @Test
  void rewritePlanLowersToPointFreeAndRebuildsOptimizedPlan() {
    record Box(int value) {}
    AtomicInteger sets = new AtomicInteger();
    Lens<Box, Integer> value =
        Lens.of(
            Box::value,
            (box, next) -> {
              sets.incrementAndGet();
              return new Box(next);
            });
    LensPath<Box, Integer> path = LensPath.of("value", value);
    RewritePlan<Box> plan =
        RewritePlan.<Box>identity()
            .modify(path, current -> current + 1)
            .modify(path, current -> current * 2);

    PointFree<Function<Box, Box>> optimizedExpression = PointFreeOptimizer.optimize(plan.toPointFree());
    RewritePlan<Box> optimizedPlan = RewritePlan.fromPointFree(optimizedExpression);

    assertTrue(OpticTestHelpers.isLensApp(optimizedExpression));
    assertEquals(1, optimizedPlan.size());
    assertEquals(new Box(4), optimizedPlan.apply(new Box(1)));
    assertEquals(1, sets.get());
  }

  @Test
  void rewritePlanFactorsCommonLensPrefix() {
    record Account(String name, Address address) {}
    AtomicInteger addressSets = new AtomicInteger();
    Lens<Account, Address> address =
        Lens.of(
            Account::address,
            (account, next) -> {
              addressSets.incrementAndGet();
              return new Account(account.name(), next);
            });
    Lens<Address, String> city = Lens.of(Address::city, (addr, next) -> new Address(next, addr.zip()));
    Lens<Address, Integer> zip = Lens.of(Address::zip, (addr, next) -> new Address(addr.city(), next));
    LensPath<Account, String> cityPath = LensPath.of("address", address).andThen("city", city);
    LensPath<Account, Integer> zipPath = LensPath.of("address", address).andThen("zip", zip);

    RewritePlan<Account> plan =
        RewritePlan.<Account>identity()
            .modify(cityPath, String::toUpperCase)
            .modify(zipPath, current -> current + 1)
            .optimize();

    assertEquals(new Account("root", new Address("LONDON", 12346)), plan.apply(new Account("root", new Address("london", 12345))));
    assertEquals(1, plan.size());
    assertEquals(1, addressSets.get());
  }
}
