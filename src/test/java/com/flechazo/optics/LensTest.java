package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Pair;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class LensTest {

  @Test
  void lensComposesAndModifiesRecords() {
    var address =
        Lens.<User, User, Address, Address>of(User::address, (user, value) -> new User(user.name(), value));
    var city =
        Lens.<Address, Address, String, String>of(Address::city, (addr, value) -> new Address(value, addr.zip()));

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
    var lo = Lens.<Range, Range, Integer, Integer>of(Range::lo, (range, value) -> new Range(value, range.hi()));
    var hi = Lens.<Range, Range, Integer, Integer>of(Range::hi, (range, value) -> new Range(range.lo(), value));
    var bounds = Lens.paired(lo, hi, Range::new);

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
    Setter<Box, Box, Integer, Integer> pure = Setter.of((f, box) -> new Box(f.apply(box.value())));
    var lens = Lens.<Box, Box, Integer, Integer>of(Box::value, (box, value) -> new Box(value));
    Setter<Box, Box, Integer, Integer> fromLens = lens.asSetter();
    Setter<Box, Box, Integer, Integer> fromGetSet = Setter.fromGetSet(Box::value, (box, value) -> new Box(value));

    assertEquals(new Box(2), pure.modify(value -> value + 1, new Box(1)));
    assertThrows(
        UnsupportedOperationException.class,
        () -> pure.modifyF(value -> Maybe.some(value + 1), new Box(1), Maybe.applicative()));
    assertEquals(Maybe.some(new Box(2)), fromLens.modifyF(value -> Maybe.some(value + 1), new Box(1), Maybe.applicative()));
    assertEquals(Maybe.some(new Box(2)), fromGetSet.modifyF(value -> Maybe.some(value + 1), new Box(1), Maybe.applicative()));
    assertEquals(
        Maybe.<Box>none(),
        Maybe.unbox(fromLens.modifyF(value -> Maybe.none(), new Box(1), Maybe.applicative())));
  }
}
