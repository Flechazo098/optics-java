package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.functions.AlgebraPlan;
import com.flechazo.hkt.functions.CataPlan;
import com.flechazo.hkt.functions.RecursiveFamily;
import com.flechazo.hkt.functions.RecursiveTerm;
import com.flechazo.hkt.type.TypeRef;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecursiveCataTest {

  @Test
  void recursiveFamilyCanCarryRuntimeSlotTypesWithoutGuavaTypeToken() {
    RecursiveFamily family =
        RecursiveFamily.typed(
            "Tree",
            TypeRef.of(String.class),
            new TypeRef<List<Integer>>() {});

    assertEquals("Tree", family.name());
    assertEquals(2, family.size());
    assertEquals(TypeRef.of(String.class), family.slot(0));
    assertEquals(new TypeRef<List<Integer>>() {}, family.slot(1));
  }

  @Test
  void cataPlanCanEvaluateRealRecursiveTermsBottomUp() {
    interface MiniTree extends RecursiveTerm<MiniTree> {}
    record Leaf(int value) implements MiniTree {
      @Override
      public int familyIndex() {
        return 0;
      }

      @Override
      public List<MiniTree> children() {
        return List.of();
      }

      @Override
      public MiniTree withChildren(List<MiniTree> children) {
        assertTrue(children.isEmpty());
        return this;
      }
    }
    record Node(List<MiniTree> children) implements MiniTree {
      @Override
      public int familyIndex() {
        return 1;
      }

      @Override
      public MiniTree withChildren(List<MiniTree> children) {
        return new Node(children);
      }
    }

    RecursiveFamily family =
        RecursiveFamily.typed("MiniTree", TypeRef.of(Leaf.class), TypeRef.of(Node.class));
    AlgebraPlan algebra =
        AlgebraPlan.identity("incLeaves", family)
            .rewrite(0, value -> new Leaf(((Leaf) value).value() + 1));
    CataPlan<MiniTree> cata = CataPlan.forTerms(family, 1, algebra);
    MiniTree source = new Node(List.of(new Leaf(1), new Node(List.of(new Leaf(2)))));

    assertEquals(new Node(List.of(new Leaf(2), new Node(List.of(new Leaf(3))))), cata.eval().apply(source));
  }
}
