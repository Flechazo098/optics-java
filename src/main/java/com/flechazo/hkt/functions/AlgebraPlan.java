package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;

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
        this.branches = List.copyOf(branches);
    }

    public static AlgebraPlan identity(String name, RecursiveFamily family) {
        ArrayList<Branch> branches = new ArrayList<>(family.size());
        for (int i = 0; i < family.size(); i++) {
            branches.add(Branch.identity());
        }
        return new AlgebraPlan(name, family, branches);
    }

    public AlgebraPlan rewrite(
            int index,
            Function<Object, Object> function,
            int... recursiveDependencies) {
        family.checkIndex(index);
        ArrayList<Branch> next = new ArrayList<>(branches);
        next.set(index, Branch.rewrite(function, recursiveDependencies));
        return new AlgebraPlan(name, family, next);
    }

    public Branch branch(int index) {
        family.checkIndex(index);
        return branches.get(index);
    }

    public List<Integer> modifiedIndices() {
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < branches.size(); i++) {
            if (!branches.get(i).isIdentity()) {
                indices.add(i);
            }
        }
        return List.copyOf(indices);
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

    public record Branch(boolean isIdentity, Function<Object, Object> function, BitSet recursiveDependencies) {
        public Branch {
            Objects.requireNonNull(function, "function");
            Objects.requireNonNull(recursiveDependencies, "recursiveDependencies");
            recursiveDependencies = (BitSet) recursiveDependencies.clone();
        }

        static Branch identity() {
            return new Branch(true, Function.identity(), new BitSet());
        }

        static Branch rewrite(Function<Object, Object> function, int... recursiveDependencies) {
            Objects.requireNonNull(function, "function");
            BitSet dependencies = new BitSet();
            for (int dependency : recursiveDependencies) {
                if (dependency < 0) {
                    throw new IndexOutOfBoundsException(dependency);
                }
                dependencies.set(dependency);
            }
            return new Branch(false, function, dependencies);
        }

        static Branch compose(Branch outer, Branch inner) {
            BitSet dependencies = (BitSet) outer.recursiveDependencies.clone();
            dependencies.or(inner.recursiveDependencies);
            return new Branch(false, value -> outer.function.apply(inner.function.apply(value)), dependencies);
        }

        boolean dependsOnAny(BitSet indices) {
            BitSet dependencies = (BitSet) recursiveDependencies.clone();
            dependencies.and(indices);
            return !dependencies.isEmpty();
        }
    }
}
