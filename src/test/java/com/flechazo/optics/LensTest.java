package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.flechazo.hkt.tuple.Tuple2;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class LensTest {

  @Test
  void lensComposesAndModifiesRecords() {
    var address =
        PLens.<User, User, Address, Address>of(User::address, (user, value) -> new User(user.name(), value));
    var city =
        PLens.<Address, Address, String, String>of(Address::city, (addr, value) -> new Address(value, addr.zip()));

    User user = new User("Ada", new Address("london", 12345));

    assertEquals("london", address.andThen(city).get(user));
    assertEquals("LONDON", address.andThen(city).modify(String::toUpperCase, user).address().city());
    assertEquals(
        "LONDON",
        address.andThen(city)
            .modifyBranch(value -> value.length() > 3, String::toUpperCase, Function.identity(), user)
            .address()
            .city());
  }

  @Test
  void pairedLensUsesSingleIndexedPairType() {
    record Range(int lo, int hi) {}
    var lo = PLens.<Range, Range, Integer, Integer>of(Range::lo, (range, value) -> new Range(value, range.hi()));
    var hi = PLens.<Range, Range, Integer, Integer>of(Range::hi, (range, value) -> new Range(range.lo(), value));
    var bounds = PLens.paired(lo, hi, Range::new);

    assertEquals(Tuple2.of(1, 3), bounds.get(new Range(1, 3)));
    assertEquals(
        new Range(2, 6),
        bounds.modify(
            tuple -> Tuple2.of(
                Objects.requireNonNull(tuple.first()) + 1,
                Objects.requireNonNull(tuple.second()) * 2),
            new Range(1, 3)));
  }

  @Test
  void setterExposesOnlyPureWriteOperations() {
    record Box(int value) {}
    PSetter<Box, Box, Integer, Integer> pure = PSetter.of((f, box) -> new Box(f.apply(box.value())));
    var lens = PLens.<Box, Box, Integer, Integer>of(Box::value, (box, value) -> new Box(value));
    PSetter<Box, Box, Integer, Integer> fromLens = lens.asSetter();
    PSetter<Box, Box, Integer, Integer> fromGetSet = PSetter.fromGetSet(Box::value, (box, value) -> new Box(value));

    assertEquals(new Box(2), pure.modify(value -> value + 1, new Box(1)));
    assertEquals(new Box(3), fromLens.modify(value -> value + 2, new Box(1)));
    assertEquals(new Box(4), fromGetSet.set(4, new Box(1)));
    assertEquals(new Box(5), PSetter.<Box>identity().modify(box -> new Box(box.value() + 4), new Box(1)));
    assertEquals(false, Optic.class.isAssignableFrom(PSetter.class));
    assertEquals(false, Optic.class.isAssignableFrom(Fold.class));
  }
}
