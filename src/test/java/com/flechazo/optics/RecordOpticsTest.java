package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.Maybe;

import com.flechazo.optics.generated.ClassFileOptics;
import com.flechazo.optics.generated.RecordOptics;
import org.junit.jupiter.api.Test;

class RecordOpticsTest {

  @Test
  void recordOpticsCreatesComponentLensesFromClassFileBackedGenerator() {
    var name = RecordOptics.<User, String>recordLens(User.class, "name");
    var nameByReference = RecordOptics.<User, String>recordLens(User.class, User::name);
    var facadeNameByReference = ClassFileOptics.<User, String>lens(User.class, User::name);
    User user = new User("Ada", new Address("London", 12345));

    assertTrue(RecordOptics.generateLensHostBytes(User.class).length > 0);
    assertEquals("Ada", name.get(user));
    assertEquals("Ada", nameByReference.get(user));
    assertEquals("Grace", facadeNameByReference.set("Grace", user).name());
    assertEquals("Grace", name.set("Grace", user).name());
    assertEquals(Maybe.some("Ada"), name.asFold().preview(user));

    var zip = RecordOptics.<Address, Integer>recordLens(Address.class, "zip");
    assertEquals(12345, zip.get(user.address()));
    assertEquals(54321, zip.set(54321, user.address()).zip());
  }

  @Test
  void recordOpticsCreatesSubtypePrismsForSealedHierarchies() {
    Prism<Shape, Shape, Circle, Circle> circle = RecordOptics.subtypePrism(Shape.class, Circle.class);
    Shape shape = new Circle(5);

    assertEquals(Maybe.some(new Circle(5)), circle.getMaybe(shape));
    assertEquals(Maybe.none(), circle.getMaybe(new Rect(2, 3)));
    assertEquals(new Circle(7), circle.modify(c -> new Circle(c.radius() + 2), shape));
    assertTrue(RecordOptics.sealedSubtypePrisms(Shape.class).containsKey(Circle.class));
  }
}
