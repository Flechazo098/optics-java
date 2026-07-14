package com.flechazo.optics.internal;

import com.flechazo.hkt.*;
import com.flechazo.optics.*;
import com.flechazo.optics.indexed.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class OpticPrograms {
    private static final Map<Object, OpticProgram<?, ?, ?, ?>> PROGRAMS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private OpticPrograms() {
    }

    public static <S, T, A, B> OpticProgram<S, T, A, B> opaque(String kind, Object key) {
        return new OpticProgram.Opaque<>(kind, key);
    }

    public static <S, T, A, B> OpticProgram<S, T, A, B> structured(String kind, Object key) {
        return new OpticProgram.Structured<>(kind, key);
    }

    public static <S, T, A, B> Maybe<OpticProgram<S, T, A, B>> program(Object optic) {
        @SuppressWarnings("unchecked")
        OpticProgram<S, T, A, B> program = (OpticProgram<S, T, A, B>)
                (optic instanceof ProgramBacked backed ? backed.program() : PROGRAMS.get(optic));
        return program == null ? Maybe.none() : Maybe.some(program);
    }

    public static boolean isProgramBacked(Object optic) {
        return optic instanceof ProgramBacked || PROGRAMS.containsKey(optic);
    }

    public static <S, T, A, B> OpticProgram<S, T, A, B> programOrOpaque(Object optic, String kind) {
        return OpticPrograms.<S, T, A, B>program(optic).orElse(opaque(kind, null));
    }

    public static int depth(Object optic) {
        return program(optic).map(OpticPrograms::depth).orElse(0);
    }

    public static boolean isCompose(Object optic) {
        return program(optic).map(value -> value instanceof OpticProgram.Compose<?, ?, ?, ?, ?, ?>).orElse(false);
    }

    public static <S, T, A, B, C, D> OpticProgram<S, T, C, D> compose(Object left, Object right) {
        OpticProgram<S, T, A, B> leftProgram =
                OpticPrograms.<S, T, A, B>program(left).orElse(opaque("unknown", null));
        OpticProgram<A, B, C, D> rightProgram =
                OpticPrograms.<A, B, C, D>program(right).orElse(opaque("unknown", null));
        return new OpticProgram.Compose<>(leftProgram, rightProgram);
    }

    public static <S, T, A, B> PLens<S, T, A, B> lens(
            PLens<S, T, A, B> direct,
            OpticProgram<S, T, A, B> program) {
        return attachMetadata(new RuntimePLens<>(direct, program), direct, program);
    }

    public static <S, T, A, B> Optic<S, T, A, B> optic(
            Optic<S, T, A, B> direct,
            OpticProgram<S, T, A, B> program) {
        return attachMetadata(new RuntimeOptic<>(direct, program), direct, program);
    }

    public static <S, T, A, B> PIso<S, T, A, B> iso(
            PIso<S, T, A, B> direct,
            OpticProgram<S, T, A, B> program) {
        return attachMetadata(new RuntimePIso<>(direct, program), direct, program);
    }

    public static <S, A> Iso<S, A> iso(
            Iso<S, A> direct,
            OpticProgram<S, S, A, A> program) {
        return attachMetadata(new RuntimeIso<>(direct, program), direct, program);
    }

    public static <S, A> Lens<S, A> lens(
            Lens<S, A> direct,
            OpticProgram<S, S, A, A> program) {
        return attachMetadata(new RuntimeLens<>(direct, program), direct, program);
    }

    public static <S, T, A, B> PPrism<S, T, A, B> prism(
            PPrism<S, T, A, B> direct,
            OpticProgram<S, T, A, B> program) {
        return attachMetadata(new RuntimePPrism<>(direct, program), direct, program);
    }

    public static <S, A> Prism<S, A> prism(
            Prism<S, A> direct,
            OpticProgram<S, S, A, A> program) {
        return attachMetadata(new RuntimePrism<>(direct, program), direct, program);
    }

    public static <S, T, A, B> PAffine<S, T, A, B> affine(
            PAffine<S, T, A, B> direct,
            OpticProgram<S, T, A, B> program) {
        return attachMetadata(new RuntimePAffine<>(direct, program), direct, program);
    }

    public static <S, A> Affine<S, A> affine(
            Affine<S, A> direct,
            OpticProgram<S, S, A, A> program) {
        return attachMetadata(new RuntimeAffine<>(direct, program), direct, program);
    }

    public static <S, T, A, B> PTraversal<S, T, A, B> traversal(
            PTraversal<S, T, A, B> direct,
            OpticProgram<S, T, A, B> program) {
        return attachMetadata(new RuntimePTraversal<>(direct, program), direct, program);
    }

    public static <S, A> Traversal<S, A> traversal(
            Traversal<S, A> direct,
            OpticProgram<S, S, A, A> program) {
        return attachMetadata(new RuntimeTraversal<>(direct, program), direct, program);
    }

    public static <S, T, A, B> PSetter<S, T, A, B> setter(
            PSetter<S, T, A, B> direct,
            OpticProgram<S, T, A, B> program) {
        return attach(new RuntimePSetter<>(direct, program), program);
    }

    public static <S, A> Setter<S, A> setter(
            Setter<S, A> direct,
            OpticProgram<S, S, A, A> program) {
        return attach(new RuntimeSetter<>(direct, program), program);
    }

    public static <S, A> Fold<S, A> fold(
            Fold<S, A> direct,
            OpticProgram<S, S, A, A> program) {
        RuntimeFold<S, A> runtime = attach(new RuntimeFold<>(direct, program), program);
        OpticMetadata.fold(runtime, OpticMetadata.fold(direct));
        return runtime;
    }

    public static <S, A> Getter<S, A> getter(
            Getter<S, A> direct,
            OpticProgram<S, S, A, A> program) {
        RuntimeGetter<S, A> runtime = attach(new RuntimeGetter<>(direct, program), program);
        OpticMetadata.fold(runtime, OpticMetadata.fold(direct));
        return runtime;
    }

    public static <I, S, A> IndexedLens<I, S, A> indexedLens(
            IndexedLens<I, S, A> direct,
            OpticProgram<S, S, A, A> program) {
        return attach(new RuntimeIndexedLens<>(direct, program), program);
    }

    public static <I, S, A> IndexedOptic<I, S, A> indexedOptic(
            IndexedOptic<I, S, A> direct,
            OpticProgram<S, S, A, A> program) {
        return attach(new RuntimeIndexedOptic<>(direct, program), program);
    }

    public static <I, S, A> IndexedTraversal<I, S, A> indexedTraversal(
            IndexedTraversal<I, S, A> direct,
            OpticProgram<S, S, A, A> program) {
        return attach(new RuntimeIndexedTraversal<>(direct, program), program);
    }

    public static <I, S, A> IndexedFold<I, S, A> indexedFold(
            IndexedFold<I, S, A> direct,
            OpticProgram<S, S, A, A> program) {
        return attach(new RuntimeIndexedFold<>(direct, program), program);
    }

    public static <I, S, A> IndexedGetter<I, S, A> indexedGetter(
            IndexedGetter<I, S, A> direct,
            OpticProgram<S, S, A, A> program) {
        return attach(new RuntimeIndexedGetter<>(direct, program), program);
    }

    private static int depth(OpticProgram<?, ?, ?, ?> program) {
        if (program instanceof OpticProgram.Compose<?, ?, ?, ?, ?, ?>(
                OpticProgram<?, ?, ?, ?> left, OpticProgram<?, ?, ?, ?> right
        )) {
            return depth(left) + depth(right);
        }
        return 1;
    }

    private static <O> O attach(O optic, OpticProgram<?, ?, ?, ?> program) {
        PROGRAMS.put(optic, program);
        return optic;
    }

    private static <S, T, A, B, O> O attachMetadata(
            O runtime,
            Object direct,
            OpticProgram<S, T, A, B> program) {
        attach(runtime, program);
        OpticMetadata.optic(runtime, OpticMetadata.optic(direct));
        return runtime;
    }

    private record RuntimePLens<S, T, A, B>(PLens<S, T, A, B> direct, OpticProgram<S, T, A, B> program)
            implements PLens<S, T, A, B>, ProgramBacked {
        @Override
        public A get(S source) {
            return TerminalRuntime.execute(this, "get", () -> direct.get(source));
        }

        @Override
        public T set(B value, S source) {
            return TerminalRuntime.set(this, direct, value, source, () -> direct.set(value, source));
        }

        @Override
        public T modify(Function<? super A, ? extends B> f, S source) {
            return TerminalRuntime.modify(this, direct, f, source, () -> direct.modify(f, source));
        }

        @Override
        public <F extends K1> App<F, T> modifyF(
                Function<A, App<F, B>> f, S source, Functor<F, ?> functor) {
            return direct.modifyF(f, source, functor);
        }
    }

    private record RuntimeOptic<S, T, A, B>(Optic<S, T, A, B> direct, OpticProgram<S, T, A, B> program)
            implements Optic<S, T, A, B>, ProgramBacked {
        @Override
        public <F extends K1> App<F, T> modifyF(
                Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
            return direct.modifyF(f, source, applicative);
        }
    }

    private record RuntimePIso<S, T, A, B>(PIso<S, T, A, B> direct, OpticProgram<S, T, A, B> program)
            implements PIso<S, T, A, B>, ProgramBacked {
        @Override
        public A get(S source) {
            return TerminalRuntime.execute(this, "get", () -> direct.get(source));
        }

        @Override
        public T reverseGet(B value) {
            return TerminalRuntime.execute(this, "reverseGet", () -> direct.reverseGet(value));
        }

        @Override
        public T modify(Function<? super A, ? extends B> f, S source) {
            return TerminalRuntime.modify(this, direct, f, source, () -> direct.modify(f, source));
        }
    }

    private record RuntimeIso<S, A>(Iso<S, A> direct, OpticProgram<S, S, A, A> program)
            implements Iso<S, A>, ProgramBacked {
        @Override
        public A get(S source) {
            return TerminalRuntime.execute(this, "get", () -> direct.get(source));
        }

        @Override
        public S reverseGet(A value) {
            return TerminalRuntime.execute(this, "reverseGet", () -> direct.reverseGet(value));
        }

        @Override
        public S modify(Function<? super A, ? extends A> f, S source) {
            return TerminalRuntime.modify(this, direct, f, source, () -> direct.modify(f, source));
        }
    }

    private record RuntimeLens<S, A>(Lens<S, A> direct, OpticProgram<S, S, A, A> program)
            implements Lens<S, A>, ProgramBacked {
        @Override
        public A get(S source) {
            return TerminalRuntime.execute(this, "get", () -> direct.get(source));
        }

        @Override
        public S set(A value, S source) {
            return TerminalRuntime.set(this, direct, value, source, () -> direct.set(value, source));
        }

        @Override
        public S modify(Function<? super A, ? extends A> f, S source) {
            return TerminalRuntime.modify(this, direct, f, source, () -> direct.modify(f, source));
        }

        @Override
        public <F extends K1> App<F, S> modifyF(
                Function<A, App<F, A>> f, S source, Functor<F, ?> functor) {
            return direct.modifyF(f, source, functor);
        }
    }

    private record RuntimePPrism<S, T, A, B>(PPrism<S, T, A, B> direct, OpticProgram<S, T, A, B> program)
            implements PPrism<S, T, A, B>, ProgramBacked {
        @Override
        public Either<T, A> match(S source) {
            return TerminalRuntime.execute(this, "match", () -> direct.match(source));
        }

        @Override
        public T build(B value) {
            return TerminalRuntime.execute(this, "build", () -> direct.build(value));
        }

        @Override
        public T modify(Function<? super A, ? extends B> f, S source) {
            return TerminalRuntime.modify(this, direct, f, source, () -> direct.modify(f, source));
        }

        @Override
        public T set(B value, S source) {
            return TerminalRuntime.set(this, direct, value, source, () -> direct.set(value, source));
        }
    }

    private record RuntimePrism<S, A>(Prism<S, A> direct, OpticProgram<S, S, A, A> program)
            implements Prism<S, A>, ProgramBacked {
        @Override
        public Either<S, A> match(S source) {
            return TerminalRuntime.execute(this, "match", () -> direct.match(source));
        }

        @Override
        public S build(A value) {
            return TerminalRuntime.execute(this, "build", () -> direct.build(value));
        }

        @Override
        public S modify(Function<? super A, ? extends A> f, S source) {
            return TerminalRuntime.modify(this, direct, f, source, () -> direct.modify(f, source));
        }

        @Override
        public S set(A value, S source) {
            return TerminalRuntime.set(this, direct, value, source, () -> direct.set(value, source));
        }
    }

    private record RuntimePAffine<S, T, A, B>(PAffine<S, T, A, B> direct, OpticProgram<S, T, A, B> program)
            implements PAffine<S, T, A, B>, ProgramBacked {
        @Override
        public Either<T, A> preview(S source) {
            return TerminalRuntime.execute(this, "preview", () -> direct.preview(source));
        }

        @Override
        public T set(B value, S source) {
            return TerminalRuntime.set(this, direct, value, source, () -> direct.set(value, source));
        }

        @Override
        public T modify(Function<? super A, ? extends B> f, S source) {
            return TerminalRuntime.modify(this, direct, f, source, () -> direct.modify(f, source));
        }
    }

    private record RuntimeAffine<S, A>(Affine<S, A> direct, OpticProgram<S, S, A, A> program)
            implements Affine<S, A>, ProgramBacked {
        @Override
        public Either<S, A> preview(S source) {
            return TerminalRuntime.execute(this, "preview", () -> direct.preview(source));
        }

        @Override
        public S set(A value, S source) {
            return TerminalRuntime.set(this, direct, value, source, () -> direct.set(value, source));
        }

        @Override
        public S modify(Function<? super A, ? extends A> f, S source) {
            return TerminalRuntime.modify(this, direct, f, source, () -> direct.modify(f, source));
        }
    }

    private record RuntimePTraversal<S, T, A, B>(PTraversal<S, T, A, B> direct, OpticProgram<S, T, A, B> program)
            implements PTraversal<S, T, A, B>, ProgramBacked {
        @Override
        public T modify(Function<? super A, ? extends B> f, S source) {
            return TerminalRuntime.modify(this, direct, f, source, () -> direct.modify(f, source));
        }

        @Override
        public T set(B value, S source) {
            return TerminalRuntime.set(this, direct, value, source, () -> direct.set(value, source));
        }

        @Override
        public <F extends K1> App<F, T> modifyF(
                Function<A, App<F, B>> f, S source, Applicative<F, ?> applicative) {
            return direct.modifyF(f, source, applicative);
        }
    }

    private record RuntimeTraversal<S, A>(Traversal<S, A> direct, OpticProgram<S, S, A, A> program)
            implements Traversal<S, A>, ProgramBacked {
        @Override
        public S modify(Function<? super A, ? extends A> f, S source) {
            return TerminalRuntime.modify(this, direct, f, source, () -> direct.modify(f, source));
        }

        @Override
        public S set(A value, S source) {
            return TerminalRuntime.set(this, direct, value, source, () -> direct.set(value, source));
        }

        @Override
        public <F extends K1> App<F, S> modifyF(
                Function<A, App<F, A>> f, S source, Applicative<F, ?> applicative) {
            return direct.modifyF(f, source, applicative);
        }
    }

    private record RuntimePSetter<S, T, A, B>(PSetter<S, T, A, B> direct, OpticProgram<S, T, A, B> program)
            implements PSetter<S, T, A, B>, ProgramBacked {
        @Override
        public T modify(Function<? super A, ? extends B> f, S source) {
            return TerminalRuntime.modifySetter(this, direct, f, source, () -> direct.modify(f, source));
        }

        @Override
        public T set(B value, S source) {
            return TerminalRuntime.setSetter(this, direct, value, source, () -> direct.set(value, source));
        }
    }

    private record RuntimeSetter<S, A>(Setter<S, A> direct, OpticProgram<S, S, A, A> program)
            implements Setter<S, A>, ProgramBacked {
        @Override
        public S modify(Function<? super A, ? extends A> f, S source) {
            return TerminalRuntime.modifySetter(this, direct, f, source, () -> direct.modify(f, source));
        }

        @Override
        public S set(A value, S source) {
            return TerminalRuntime.setSetter(this, direct, value, source, () -> direct.set(value, source));
        }
    }

    private record RuntimeFold<S, A>(Fold<S, A> direct, OpticProgram<S, S, A, A> program)
            implements Fold<S, A>, ProgramBacked {
        @Override
        public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
            return TerminalRuntime.foldMap(this, direct, monoid, f, source, () -> direct.foldMap(monoid, f, source));
        }

        @Override
        public List<A> getAll(S source) {
            return TerminalRuntime.getAll(this, direct, source, () -> direct.getAll(source));
        }

        @Override
        public Maybe<A> preview(S source) {
            return TerminalRuntime.preview(this, direct, source, () -> direct.preview(source));
        }

        @Override
        public Maybe<A> find(Predicate<? super A> predicate, S source) {
            return TerminalRuntime.find(this, direct, predicate, source, () -> direct.find(predicate, source));
        }

        @Override
        public int length(S source) {
            return TerminalRuntime.length(this, direct, source, () -> direct.length(source));
        }

        @Override
        public boolean exists(Predicate<? super A> predicate, S source) {
            return TerminalRuntime.exists(this, direct, predicate, source, () -> direct.exists(predicate, source));
        }

        @Override
        public boolean all(Predicate<? super A> predicate, S source) {
            return TerminalRuntime.all(this, direct, predicate, source, () -> direct.all(predicate, source));
        }
    }

    private record RuntimeGetter<S, A>(Getter<S, A> direct, OpticProgram<S, S, A, A> program)
            implements Getter<S, A>, ProgramBacked {
        @Override
        public A get(S source) {
            return TerminalRuntime.execute(this, "get", () -> direct.get(source));
        }

        @Override
        public <M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, S source) {
            return TerminalRuntime.foldMap(this, direct, monoid, f, source, () -> direct.foldMap(monoid, f, source));
        }
    }

    private record RuntimeIndexedLens<I, S, A>(
            IndexedLens<I, S, A> direct,
            OpticProgram<S, S, A, A> program)
            implements IndexedLens<I, S, A>, ProgramBacked {
        @Override
        public I index() {
            return direct.index();
        }

        @Override
        public A get(S source) {
            return TerminalRuntime.execute(this, "indexedGet", () -> direct.get(source));
        }

        @Override
        public S set(A value, S source) {
            return TerminalRuntime.execute(this, "indexedSet", () -> direct.set(value, source));
        }

        @Override
        public S imodify(BiFunction<? super I, ? super A, ? extends A> f, S source) {
            return TerminalRuntime.execute(this, "indexedModify", () -> direct.imodify(f, source));
        }
    }

    private record RuntimeIndexedOptic<I, S, A>(
            IndexedOptic<I, S, A> direct,
            OpticProgram<S, S, A, A> program)
            implements IndexedOptic<I, S, A>, ProgramBacked {
        @Override
        public <F extends K1> App<F, S> imodifyF(
                BiFunction<I, A, App<F, A>> f,
                S source,
                Applicative<F, ?> applicative) {
            return direct.imodifyF(f, source, applicative);
        }
    }

    private record RuntimeIndexedTraversal<I, S, A>(
            IndexedTraversal<I, S, A> direct,
            OpticProgram<S, S, A, A> program)
            implements IndexedTraversal<I, S, A>, ProgramBacked {
        @Override
        public S imodify(BiFunction<? super I, ? super A, ? extends A> f, S source) {
            return TerminalRuntime.execute(this, "indexedModify", () -> direct.imodify(f, source));
        }

        @Override
        public <F extends K1> App<F, S> imodifyF(
                BiFunction<I, A, App<F, A>> f,
                S source,
                Applicative<F, ?> applicative) {
            return direct.imodifyF(f, source, applicative);
        }
    }

    private record RuntimeIndexedFold<I, S, A>(
            IndexedFold<I, S, A> direct,
            OpticProgram<S, S, A, A> program)
            implements IndexedFold<I, S, A>, ProgramBacked {
        @Override
        public <M> M ifoldMap(
                Monoid<M> monoid,
                BiFunction<? super I, ? super A, ? extends M> f,
                S source) {
            return TerminalRuntime.execute(this, "indexedFoldMap", () -> direct.ifoldMap(monoid, f, source));
        }

        @Override
        public List<A> getAll(S source) {
            return TerminalRuntime.execute(this, "indexedGetAll", () -> direct.getAll(source));
        }

        @Override
        public int length(S source) {
            return TerminalRuntime.execute(this, "indexedLength", () -> direct.length(source));
        }

        @Override
        public boolean exists(Predicate<? super A> predicate, S source) {
            return TerminalRuntime.execute(this, "indexedExists", () -> direct.exists(predicate, source));
        }

        @Override
        public boolean all(Predicate<? super A> predicate, S source) {
            return TerminalRuntime.execute(this, "indexedAll", () -> direct.all(predicate, source));
        }
    }

    private record RuntimeIndexedGetter<I, S, A>(
            IndexedGetter<I, S, A> direct,
            OpticProgram<S, S, A, A> program)
            implements IndexedGetter<I, S, A>, ProgramBacked {
        @Override
        public A get(S source) {
            return TerminalRuntime.execute(this, "indexedGet", () -> direct.get(source));
        }

        @Override
        public I index() {
            return direct.index();
        }

        @Override
        public <M> M ifoldMap(
                Monoid<M> monoid,
                BiFunction<? super I, ? super A, ? extends M> f,
                S source) {
            return TerminalRuntime.execute(this, "indexedFoldMap", () -> direct.ifoldMap(monoid, f, source));
        }
    }
}
