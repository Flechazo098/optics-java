package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.functions.AlgebraPlan;
import com.flechazo.hkt.functions.CataPlan;
import com.flechazo.hkt.functions.Comp;
import com.flechazo.hkt.functions.Id;
import com.flechazo.hkt.functions.PointFree;
import com.flechazo.hkt.functions.PointFreeNormalForm;
import com.flechazo.hkt.functions.PointFreeOptimizer;
import com.flechazo.hkt.functions.RecursiveFamily;
import com.flechazo.hkt.functions.RecursiveTerm;
import com.google.common.reflect.TypeToken;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class RecursiveCataTest {
  interface TestTree extends RecursiveTerm<TestTree> {}

  record TestLeaf(int value) implements TestTree {
    @Override
    public int familyIndex() {
      return 0;
    }

    @Override
    public List<TestTree> children() {
      return List.of();
    }

    @Override
    public TestTree withChildren(List<TestTree> children) {
      assertTrue(children.isEmpty());
      return this;
    }
  }

  record TestNode(List<TestTree> children) implements TestTree {
    @Override
    public int familyIndex() {
      return 1;
    }

    @Override
    public TestTree withChildren(List<TestTree> children) {
      return new TestNode(children);
    }
  }

  @Test
  void recursiveFamilyCanCarryRuntimeSlotTypesWithoutGuavaTypeToken() {
    RecursiveFamily family =
        RecursiveFamily.typed(
            "Tree",
            TypeToken.of(String.class),
            new TypeToken<List<Integer>>() {});

    assertEquals("Tree", family.name());
    assertEquals(2, family.size());
    assertEquals(TypeToken.of(String.class), family.slot(0));
    assertEquals(new TypeToken<List<Integer>>() {}, family.slot(1));
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
        RecursiveFamily.typed("MiniTree", TypeToken.of(Leaf.class), TypeToken.of(Node.class));
    AlgebraPlan algebra =
        AlgebraPlan.identity("incLeaves", family)
            .rewrite(0, value -> new Leaf(((Leaf) value).value() + 1));
    CataPlan<MiniTree> cata = CataPlan.forTerms(family, 1, algebra);
    MiniTree source = new Node(List.of(new Leaf(1), new Node(List.of(new Leaf(2)))));

    assertEquals(new Node(List.of(new Leaf(2), new Node(List.of(new Leaf(3))))), cata.eval().apply(source));
  }

  @Test
  void genericRecursiveFunctionSpecializesIntoCataPlan() {
    RecursiveFamily family =
        RecursiveFamily.typed("GenericTree", TypeToken.of(TestLeaf.class), TypeToken.of(TestNode.class));
    AlgebraPlan algebra =
        AlgebraPlan.identity("incLeaves", family)
            .rewriteWithoutRecursiveDependencies(
                0, value -> new TestLeaf(((TestLeaf) value).value() + 1));
    PointFree<Function<TestTree, TestTree>> generic =
        PointFree.genericRecursive(
            "incLeavesGeneric", family, 1, algebra, TypeToken.of(TestTree.class));
    TestTree source = new TestNode(List.of(new TestLeaf(1), new TestNode(List.of(new TestLeaf(2)))));

    PointFree<Function<TestTree, TestTree>> optimized = PointFreeOptimizer.optimize(generic);

    assertTrue(optimized instanceof CataPlan<?>);
    CataPlan<?> cata = (CataPlan<?>) optimized;
    assertTrue(PointFreeNormalForm.isNormal(optimized));
    assertTrue(cata.algebra().branch(0).hasRecursiveDependencyEvidence());
    assertEquals(
        new TestNode(List.of(new TestLeaf(2), new TestNode(List.of(new TestLeaf(3))))),
        optimized.eval().apply(source));
  }

  @Test
  void specializedGenericRecursiveFunctionParticipatesInCataFusion() {
    RecursiveFamily family =
        RecursiveFamily.typed("FusionTree", TypeToken.of(TestLeaf.class), TypeToken.of(TestNode.class));
    AlgebraPlan innerAlgebra =
        AlgebraPlan.identity("incLeaves", family)
            .rewriteWithoutRecursiveDependencies(
                0, value -> new TestLeaf(((TestLeaf) value).value() + 1));
    AlgebraPlan outerAlgebra =
        AlgebraPlan.identity("doubleLeaves", family)
            .rewriteWithoutRecursiveDependencies(
                0, value -> new TestLeaf(((TestLeaf) value).value() * 2));
    PointFree<Function<TestTree, TestTree>> generic =
        PointFree.genericRecursive(
            "incLeavesGeneric", family, 1, innerAlgebra, TypeToken.of(TestTree.class));
    CataPlan<TestTree> outer =
        CataPlan.forTerms(family, 1, outerAlgebra, TypeToken.of(TestTree.class));
    PointFree<Function<TestTree, TestTree>> expression = PointFree.comp(outer, generic);
    TestTree source = new TestNode(List.of(new TestLeaf(1), new TestNode(List.of(new TestLeaf(2)))));

    PointFree<Function<TestTree, TestTree>> optimized = PointFreeOptimizer.optimize(expression);

    assertTrue(optimized instanceof CataPlan<?>);
    CataPlan<?> cata = (CataPlan<?>) optimized;
    assertTrue(PointFreeNormalForm.isNormal(optimized));
    assertTrue(cata.algebra().branch(0).rewrite().view() instanceof Comp<?, ?>);
    assertEquals(
        new TestNode(List.of(new TestLeaf(4), new TestNode(List.of(new TestLeaf(6))))),
        optimized.eval().apply(source));
  }

  @Test
  void reflexiveIdentityAlgebraCataEliminatesToIdWithExplicitEvidence() {
    RecursiveFamily family =
        RecursiveFamily.typed("ReflexTree", TypeToken.of(TestLeaf.class), TypeToken.of(TestNode.class));
    AlgebraPlan algebra = AlgebraPlan.reflexiveIdentity("reflexive", family);
    CataPlan<TestTree> cata = CataPlan.forTerms(family, 1, algebra, TypeToken.of(TestTree.class));
    TestTree source = new TestNode(List.of(new TestLeaf(1), new TestNode(List.of(new TestLeaf(2)))));

    PointFree<Function<TestTree, TestTree>> optimized = PointFreeOptimizer.optimize(cata);

    assertTrue(optimized instanceof Id<?>);
    assertTrue(PointFreeNormalForm.isNormal(optimized));
    assertEquals(source, optimized.eval().apply(source));
  }

  @Test
  void ordinaryNoOpAlgebraIsNotReflexiveIdentityEvidence() {
    RecursiveFamily family =
        RecursiveFamily.typed("NoOpTree", TypeToken.of(TestLeaf.class), TypeToken.of(TestNode.class));
    AlgebraPlan algebra = AlgebraPlan.identity("ordinaryNoOp", family);
    CataPlan<TestTree> cata = CataPlan.forTerms(family, 1, algebra, TypeToken.of(TestTree.class));

    PointFree<Function<TestTree, TestTree>> optimized = PointFreeOptimizer.optimize(cata);

    assertTrue(optimized instanceof CataPlan<?>);
    assertFalse(((CataPlan<?>) optimized).isReflexiveIdentityCata());
  }

  @Test
  void opaqueIdentityEvaluatorDoesNotTriggerReflexCataWithoutPlanEvidence() {
    RecursiveFamily family =
        RecursiveFamily.typed("OpaqueReflexTree", TypeToken.of(TestLeaf.class), TypeToken.of(TestNode.class));
    AlgebraPlan algebra = AlgebraPlan.reflexiveIdentity("reflexive", family);
    CataPlan<TestTree> cata =
        CataPlan.of(family, 1, algebra, Function.identity(), TypeToken.of(TestTree.class));

    PointFree<Function<TestTree, TestTree>> optimized = PointFreeOptimizer.optimize(cata);

    assertTrue(optimized instanceof CataPlan<?>);
    assertFalse(((CataPlan<?>) optimized).isReflexiveIdentityCata());
    assertFalse(((CataPlan<?>) optimized).planRewrite().hasRecursiveDependencyEvidence());
  }

  @Test
  void branchLambdaThatBehavesLikeIdentityIsNotReflexiveIdentityEvidence() {
    RecursiveFamily family =
        RecursiveFamily.typed("LambdaIdentityTree", TypeToken.of(TestLeaf.class), TypeToken.of(TestNode.class));
    AlgebraPlan algebra =
        AlgebraPlan.identity("lambdaIdentity", family)
            .rewriteWithoutRecursiveDependencies(0, Function.identity())
            .rewriteWithoutRecursiveDependencies(1, Function.identity());
    CataPlan<TestTree> cata = CataPlan.forTerms(family, 1, algebra, TypeToken.of(TestTree.class));

    PointFree<Function<TestTree, TestTree>> optimized = PointFreeOptimizer.optimize(cata);

    assertTrue(optimized instanceof CataPlan<?>);
    assertFalse(((CataPlan<?>) optimized).isReflexiveIdentityCata());
  }
}

