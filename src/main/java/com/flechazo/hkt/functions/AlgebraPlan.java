package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.type.Types;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class AlgebraPlan {
    private final String name;
    private final RecursiveFamily family;
    private final List<Branch> branches;

    private AlgebraPlan(String name, RecursiveFamily family, List<Branch> branches) {
        this.name = Objects.requireNonNull(name, "name");
        this.family = Objects.requireNonNull(family, "family");
        if (branches.size() != family.size()) {
            throw new IllegalArgumentException("branch count must match family size");
        }
        this.branches = branches;
    }

    public static AlgebraPlan identity(String name, RecursiveFamily family) {
        ArrayList<Branch> branches = new ArrayList<>(family.size());
        for (int i = 0; i < family.size(); i++) {
            branches.add(Branch.identity());
        }
        return new AlgebraPlan(name, family, branches);
    }

    public static AlgebraPlan reflexiveIdentity(String name, RecursiveFamily family) {
        ArrayList<Branch> branches = new ArrayList<>(family.size());
        for (int i = 0; i < family.size(); i++) {
            branches.add(Branch.reflexiveIdentity(name + "#" + i));
        }
        return new AlgebraPlan(name, family, branches);
    }

    public AlgebraPlan rewrite(
            int index,
            Function<Object, Object> function) {
        family.checkIndex(index);
        ArrayList<Branch> next = new ArrayList<>(branches);
        next.set(index, Branch.rewrite(name + "#" + index, function));
        return new AlgebraPlan(name, family, next);
    }

    public AlgebraPlan rewrite(
            int index,
            Function<Object, Object> function,
            int firstRecursiveDependency,
            int... moreRecursiveDependencies) {
        return rewriteWithRecursiveDependencies(
                index,
                function,
                prepend(firstRecursiveDependency, moreRecursiveDependencies));
    }

    public AlgebraPlan rewriteWithoutRecursiveDependencies(
            int index,
            Function<Object, Object> function) {
        return rewriteWithRecursiveDependencies(index, function);
    }

    public AlgebraPlan rewriteWithRecursiveDependencies(
            int index,
            Function<Object, Object> function,
            int... recursiveDependencies) {
        family.checkIndex(index);
        ArrayList<Branch> next = new ArrayList<>(branches);
        next.set(index, Branch.rewriteWithRecursiveDependencies(name + "#" + index, function, recursiveDependencies));
        return new AlgebraPlan(name, family, next);
    }

    public AlgebraPlan rewrite(
            int index,
            AlgebraRewrite rewrite) {
        family.checkIndex(index);
        ArrayList<Branch> next = new ArrayList<>(branches);
        next.set(index, Branch.of(rewrite));
        return new AlgebraPlan(name, family, next);
    }

    public Branch branch(int index) {
        family.checkIndex(index);
        return branches.get(index);
    }

    public List<Integer> modifiedIndices() {
        ArrayList<Integer> indices = new ArrayList<>(branches.size());
        for (int i = 0; i < branches.size(); i++) {
            if (!branches.get(i).isIdentity()) {
                indices.add(i);
            }
        }
        return indices;
    }

    public String name() {
        return name;
    }

    public RecursiveFamily family() {
        return family;
    }

    public List<Branch> branches() {
        return branches;
    }

    public boolean isReflexiveIdentityAlgebra() {
        for (Branch branch : branches) {
            if (!branch.isReflexiveIdentityAlgebra()) {
                return false;
            }
        }
        return true;
    }

    Maybe<AlgebraPlan> fuseSame(AlgebraPlan inner) {
        Objects.requireNonNull(inner, "inner");
        if (!family.equals(inner.family)) {
            return Maybe.none();
        }
        ArrayList<Branch> fused = new ArrayList<>(family.size());
        boolean foundModifiedPair = false;
        for (int i = 0; i < family.size(); i++) {
            Branch outerBranch = branch(i);
            Branch innerBranch = inner.branch(i);
            if (outerBranch.isIdentity() && innerBranch.isIdentity()) {
                fused.add(outerBranch);
            } else if (!foundModifiedPair && !outerBranch.isIdentity() && !innerBranch.isIdentity()) {
                fused.add(Branch.compose(outerBranch, innerBranch));
                foundModifiedPair = true;
            } else {
                return Maybe.none();
            }
        }
        return foundModifiedPair
                ? Maybe.some(new AlgebraPlan(name + "+" + inner.name, family, fused))
                : Maybe.none();
    }

    Maybe<AlgebraPlan> fuseDifferent(AlgebraPlan inner) {
        Objects.requireNonNull(inner, "inner");
        if (!family.equals(inner.family)) {
            return Maybe.none();
        }
        BitSet outerModifies = modifiedBitSet();
        BitSet innerModifies = inner.modifiedBitSet();
        BitSet overlap = (BitSet) outerModifies.clone();
        overlap.and(innerModifies);
        if (!overlap.isEmpty()) {
            return Maybe.none();
        }
        for (int i = 0; i < family.size(); i++) {
            if (branch(i).dependsOnAny(innerModifies) || inner.branch(i).dependsOnAny(outerModifies)) {
                return Maybe.none();
            }
        }
        ArrayList<Branch> fused = new ArrayList<>(family.size());
        for (int i = 0; i < family.size(); i++) {
            Branch outerBranch = branch(i);
            Branch innerBranch = inner.branch(i);
            fused.add(outerBranch.isIdentity() ? innerBranch : outerBranch);
        }
        return Maybe.some(new AlgebraPlan(name + "+" + inner.name, family, fused));
    }

    private BitSet modifiedBitSet() {
        BitSet set = new BitSet(family.size());
        for (int i = 0; i < family.size(); i++) {
            set.set(i, !branch(i).isIdentity());
        }
        return set;
    }

    private static int[] prepend(int first, int[] rest) {
        int[] dependencies = new int[rest.length + 1];
        dependencies[0] = first;
        System.arraycopy(rest, 0, dependencies, 1, rest.length);
        return dependencies;
    }

    public record Branch(AlgebraRewrite rewrite) {
        public Branch {
            Objects.requireNonNull(rewrite, "rewrite");
        }

        static Branch identity() {
            return new Branch(AlgebraRewrite.identity());
        }

        static Branch reflexiveIdentity(String name) {
            return new Branch(AlgebraRewrite.reflexiveIdentityAlgebra(Types.variable(name + ".reflexive")));
        }

        static Branch rewrite(String name, Function<Object, Object> function) {
            return new Branch(AlgebraRewrite.rewrite(name, function));
        }

        static Branch rewriteWithRecursiveDependencies(
                String name,
                Function<Object, Object> function,
                int... recursiveDependencies) {
            return new Branch(AlgebraRewrite.rewriteWithRecursiveDependencies(name, function, recursiveDependencies));
        }

        static Branch of(AlgebraRewrite rewrite) {
            return new Branch(rewrite);
        }

        static Branch compose(Branch outer, Branch inner) {
            return new Branch(outer.rewrite.compose(inner.rewrite));
        }

        public boolean isIdentity() {
            return rewrite.isIdentity();
        }

        public boolean isReflexiveIdentityAlgebra() {
            return rewrite.isReflexiveIdentityAlgebra();
        }

        public Function<Object, Object> function() {
            return rewrite.function();
        }

        public Maybe<BitSet> recursiveDependencies() {
            return rewrite.recursiveDependencies();
        }

        public boolean hasRecursiveDependencyEvidence() {
            return rewrite.hasRecursiveDependencyEvidence();
        }

        boolean dependsOnAny(BitSet indices) {
            return rewrite.dependsOnAny(indices);
        }
    }
}
