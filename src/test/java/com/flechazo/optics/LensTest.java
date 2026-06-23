package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Pair;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class LensTest {

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
        Maybe.unbox(fromLens.modifyF(value -> Maybe.none(), new Box(1), Maybe.applicative())));
  }
}
