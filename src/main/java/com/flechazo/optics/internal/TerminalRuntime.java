package com.flechazo.optics.internal;

import com.flechazo.hkt.*;
import com.flechazo.hkt.functions.OpticLowering;
import com.flechazo.hkt.functions.PointFree;
import com.flechazo.hkt.functions.PointFreeBytecodeBackend;
import com.flechazo.hkt.functions.PointFreeExecutor;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.functions.PointFreeOptimizer;
import com.flechazo.optics.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class TerminalRuntime {
    private static final ConcurrentMap<TerminalShape, CompiledTerminal> CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<ArrayDeque<Invocation>> INVOCATIONS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Integer> BYPASS = ThreadLocal.withInitial(() -> 0);
    private static final AtomicLong LOWERED = new AtomicLong();
    private static final AtomicLong COMPILED = new AtomicLong();
    private static final AtomicLong SPECIALIZED = new AtomicLong();
    private static final AtomicLong HITS = new AtomicLong();
    private static final AtomicLong ACCESS_FALLBACKS = new AtomicLong();
    private static final Function<Object, Object> RUNTIME_MODIFIER = TerminalRuntime::applyModifier;
    private static final Function<Object, Object> RUNTIME_SETTER = ignored -> current().value();
    private static final Predicate<Object> RUNTIME_PREDICATE = value ->
            Boolean.TRUE.equals(applyModifier(value));
    private static final Monoid<Object> RUNTIME_MONOID = new Monoid<>() {
        @Override
        public Object empty() {
            return TerminalRuntime.<Monoid<Object>>cast(current().value()).empty();
        }

        @Override
        public Object combine(Object left, Object right) {
            return TerminalRuntime.<Monoid<Object>>cast(current().value()).combine(left, right);
        }
    };
    private static final Fold<Object, Object> RUNTIME_FOLD = new Fold<>() {
        @Override
        public <M> M foldMap(
                Monoid<M> monoid,
                Function<? super Object, ? extends M> mapper,
                Object source) {
            return TerminalRuntime.<Fold<Object, Object>>cast(current().semantic())
                    .foldMap(monoid, mapper, source);
        }
    };
    private static final Set<String> SPECIALIZED_KINDS = Set.of(
            "recordLens",
            "recordPathLens",
            "liftedIso",
            "recordTraversal",
            "subtypePrism",
            "sumPrism",
            "containerAffine",
            "maybeAffine",
            "optionalAffine",
            "sumAffine",
            "mapKeyAffine",
            "listIndexAffine",
            "listTraversal",
            "maybeTraversal",
            "mapValuesTraversal",
            "mapEntriesTraversal",
            "setTraversal",
            "arrayTraversal",
            "stringCharactersTraversal");

    private TerminalRuntime() {
    }

    public static <S, T, A, B> T modify(
            Object optic,
            Optic<S, T, A, B> direct,
            Function<? super A, ? extends B> function,
            S source,
            Supplier<T> fallback) {
        Objects.requireNonNull(function, "function");
        if (BYPASS.get() > 0) {
            return fallback.get();
        }
        OpticProgram<S, T, A, B> program = OpticPrograms.programOrOpaque(optic, "optic");
        TerminalShape shape = new TerminalShape("modify", shape(program));
        CompiledTerminal terminal = terminal(shape, () -> compileModify(optic, direct, program, shape));
        return invoke(terminal, new Invocation(cast(function), null, source, fallback, direct));
    }

    public static <S, T, A, B> T set(
            Object optic,
            Optic<S, T, A, B> direct,
            B value,
            S source,
            Supplier<T> fallback) {
        if (BYPASS.get() > 0) {
            return fallback.get();
        }
        OpticProgram<S, T, A, B> program = OpticPrograms.programOrOpaque(optic, "optic");
        TerminalShape shape = new TerminalShape("set", shape(program));
        CompiledTerminal terminal = terminal(shape, () -> compileSet(optic, direct, program, shape));
        return invoke(terminal, new Invocation(null, value, source, fallback, direct));
    }

    public static <R> R execute(
            Object optic,
            String operation,
            Supplier<R> action) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(action, "action");
        if (BYPASS.get() > 0) {
            return action.get();
        }
        OpticProgram<?, ?, ?, ?> program = OpticPrograms.programOrOpaque(optic, "optic");
        TerminalShape shape = new TerminalShape(operation, shape(program));
        CompiledTerminal terminal = terminal(shape, () -> compileGeneric(shape));
        return invoke(terminal, new Invocation(null, null, null, action, null));
    }

    public static <S, T, A, B> T modifySetter(
            Object optic,
            PSetter<S, T, A, B> direct,
            Function<? super A, ? extends B> function,
            S source,
            Supplier<T> fallback) {
        if (BYPASS.get() > 0) {
            return fallback.get();
        }
        OpticProgram<?, ?, ?, ?> program = OpticPrograms.programOrOpaque(optic, "setter");
        TerminalShape shape = new TerminalShape("setter:modify", shape(program));
        CompiledTerminal terminal = terminal(shape, () -> compileSpecialized(OpticLowering.<S, T>terminal(
                "setter-modify",
                (S value) -> TerminalRuntime.<PSetter<S, T, A, B>>cast(current().semantic())
                        .modify(cast(RUNTIME_MODIFIER), value))));
        return invoke(terminal, new Invocation(cast(function), null, source, fallback, direct));
    }

    public static <S, T, A, B> T setSetter(
            Object optic,
            PSetter<S, T, A, B> direct,
            B value,
            S source,
            Supplier<T> fallback) {
        if (BYPASS.get() > 0) {
            return fallback.get();
        }
        OpticProgram<?, ?, ?, ?> program = OpticPrograms.programOrOpaque(optic, "setter");
        TerminalShape shape = new TerminalShape("setter:set", shape(program));
        CompiledTerminal terminal = terminal(shape, () -> compileSpecialized(OpticLowering.<S, T>terminal(
                "setter-set",
                (S sourceValue) -> TerminalRuntime.<PSetter<S, T, A, B>>cast(current().semantic())
                        .set(cast(current().value()), sourceValue))));
        return invoke(terminal, new Invocation(null, value, source, fallback, direct));
    }

    public static <S, A, M> M foldMap(
            Object optic,
            Fold<S, A> direct,
            Monoid<M> monoid,
            Function<? super A, ? extends M> mapper,
            S source,
            Supplier<M> fallback) {
        Objects.requireNonNull(monoid, "monoid");
        Objects.requireNonNull(mapper, "mapper");
        return foldTerminal(
                optic,
                "foldMap",
                direct,
                source,
                cast(mapper),
                monoid,
                fallback,
                () -> OpticLowering.foldMap(cast(RUNTIME_FOLD), cast(RUNTIME_MONOID), cast(RUNTIME_MODIFIER)));
    }

    public static <S, A> List<A> getAll(
            Object optic,
            Fold<S, A> direct,
            S source,
            Supplier<List<A>> fallback) {
        Monoid<List<A>> lists = Monoid.of(List.of(), (left, right) -> {
            ArrayList<A> result = new ArrayList<>(left.size() + right.size());
            result.addAll(left);
            result.addAll(right);
            return Collections.unmodifiableList(result);
        });
        return foldTerminal(
                optic,
                "getAll",
                direct,
                source,
                null,
                null,
                fallback,
                () -> OpticLowering.<S, A, List<A>>foldMap(
                        cast(RUNTIME_FOLD),
                        lists,
                        List::of));
    }

    public static <S, A> Maybe<A> preview(
            Object optic,
            Fold<S, A> direct,
            S source,
            Supplier<Maybe<A>> fallback) {
        return foldTerminal(
                optic,
                "preview",
                direct,
                source,
                null,
                null,
                fallback,
                () -> OpticLowering.terminal(
                        "fold-preview",
                        value -> TerminalRuntime.<Fold<S, A>>cast(current().semantic()).preview(value)));
    }

    public static <S, A> Maybe<A> find(
            Object optic,
            Fold<S, A> direct,
            Predicate<? super A> predicate,
            S source,
            Supplier<Maybe<A>> fallback) {
        return foldTerminal(
                optic,
                "find",
                direct,
                source,
                value -> predicate.test(cast(value)),
                null,
                fallback,
                () -> OpticLowering.terminal(
                        "fold-find",
                        value -> TerminalRuntime.<Fold<S, A>>cast(current().semantic())
                                .find(cast(RUNTIME_PREDICATE), value)));
    }

    public static <S, A> int length(
            Object optic,
            Fold<S, A> direct,
            S source,
            Supplier<Integer> fallback) {
        return foldTerminal(
                optic,
                "length",
                direct,
                source,
                null,
                null,
                fallback,
                () -> OpticLowering.count(cast(RUNTIME_FOLD)));
    }

    public static <S, A> boolean exists(
            Object optic,
            Fold<S, A> direct,
            Predicate<? super A> predicate,
            S source,
            Supplier<Boolean> fallback) {
        return foldTerminal(
                optic,
                "exists",
                direct,
                source,
                value -> predicate.test(cast(value)),
                null,
                fallback,
                () -> OpticLowering.terminal(
                        "fold-exists",
                        value -> TerminalRuntime.<Fold<S, A>>cast(current().semantic())
                                .exists(cast(RUNTIME_PREDICATE), value)));
    }

    public static <S, A> boolean all(
            Object optic,
            Fold<S, A> direct,
            Predicate<? super A> predicate,
            S source,
            Supplier<Boolean> fallback) {
        return foldTerminal(
                optic,
                "all",
                direct,
                source,
                value -> predicate.test(cast(value)),
                null,
                fallback,
                () -> OpticLowering.terminal(
                        "fold-all",
                        value -> TerminalRuntime.<Fold<S, A>>cast(current().semantic())
                                .all(cast(RUNTIME_PREDICATE), value)));
    }

    private static <S, A, R> R foldTerminal(
            Object optic,
            String operation,
            Fold<S, A> direct,
            S source,
            Function<Object, Object> function,
            Object value,
            Supplier<R> fallback,
            Supplier<? extends PointFree<Function<S, R>>> lowering) {
        if (BYPASS.get() > 0) {
            return fallback.get();
        }
        OpticProgram<?, ?, ?, ?> program = OpticPrograms.programOrOpaque(optic, "fold");
        TerminalShape shape = new TerminalShape(operation, shape(program));
        CompiledTerminal terminal = terminal(shape, () -> compileSpecialized(lowering.get()));
        return invoke(terminal, new Invocation(function, value, source, fallback, direct));
    }

    public static int cacheSize() {
        return CACHE.size();
    }

    public static long loweredCount() {
        return LOWERED.get();
    }

    public static long compiledCount() {
        return COMPILED.get();
    }

    public static long specializedCount() {
        return SPECIALIZED.get();
    }

    public static long cacheHitCount() {
        return HITS.get();
    }

    public static long accessFallbackCount() {
        return ACCESS_FALLBACKS.get();
    }

    public static boolean compiledAsHiddenClass(Object optic, String operation) {
        OpticProgram<?, ?, ?, ?> program = OpticPrograms.programOrOpaque(optic, "optic");
        String terminalOperation = optic instanceof PSetter<?, ?, ?, ?> && !(optic instanceof Optic<?, ?, ?, ?>)
                ? "setter:" + operation
                : operation;
        CompiledTerminal terminal = CACHE.get(new TerminalShape(terminalOperation, shape(program)));
        return terminal != null && terminal.executorClass().isHidden();
    }

    public static void clear() {
        CACHE.clear();
        LOWERED.set(0);
        COMPILED.set(0);
        SPECIALIZED.set(0);
        HITS.set(0);
        ACCESS_FALLBACKS.set(0);
    }

    private static CompiledTerminal terminal(
            TerminalShape shape,
            Supplier<CompiledTerminal> compiler) {
        CompiledTerminal existing = CACHE.get(shape);
        if (existing != null) {
            HITS.incrementAndGet();
            return existing;
        }
        return CACHE.computeIfAbsent(shape, ignored -> compiler.get());
    }

    private static <S, T, A, B> CompiledTerminal compileModify(
            Object optic,
            Optic<S, T, A, B> direct,
            OpticProgram<S, T, A, B> program,
            TerminalShape shape) {
        Maybe<PointFreeOptic<S, T, A, B>> metadata = OpticMetadata.<S, T, A, B>optic(optic)
                .or(() -> inferredMetadata(program, direct));
        if (metadata.isDefined() && specialized(program)) {
            PointFree<Function<S, T>> lowered = OpticLowering.modify(
                    metadata.get(), "terminal-modify-slot", cast(RUNTIME_MODIFIER));
            return compileSpecialized(lowered);
        }
        return compileGeneric(shape);
    }

    private static <S, T, A, B> CompiledTerminal compileSet(
            Object optic,
            Optic<S, T, A, B> direct,
            OpticProgram<S, T, A, B> program,
            TerminalShape shape) {
        Maybe<PointFreeOptic<S, T, A, B>> metadata = OpticMetadata.<S, T, A, B>optic(optic)
                .or(() -> inferredMetadata(program, direct));
        if (metadata.isDefined() && specialized(program)) {
            PointFree<Function<S, T>> lowered = OpticLowering.modify(
                    metadata.get(), "terminal-set-slot", cast(RUNTIME_SETTER));
            return compileSpecialized(lowered);
        }
        return compileGeneric(shape);
    }

    private static <S, T, A, B> Maybe<PointFreeOptic<S, T, A, B>> inferredMetadata(
            OpticProgram<S, T, A, B> program,
            Optic<S, T, A, B> direct) {
        if (!(program instanceof OpticProgram.Structured<?, ?, ?, ?> structured)
                || !SPECIALIZED_KINDS.contains(structured.kind())) {
            return Maybe.none();
        }
        Object key = structured.key() == null ? structured.kind() : structured.key();
        if (direct instanceof Lens<?, ?> lens) {
            return Maybe.some(cast(OpticLowering.lens(key, cast(lens))));
        }
        if (direct instanceof Prism<?, ?> prism) {
            return Maybe.some(cast(OpticLowering.prism(key, cast(prism))));
        }
        if (direct instanceof Affine<?, ?> affine) {
            return Maybe.some(cast(OpticLowering.affine(key, cast(affine))));
        }
        if (direct instanceof Traversal<?, ?> traversal) {
            return Maybe.some(cast(OpticLowering.traversal(key, cast(traversal))));
        }
        if (direct instanceof Iso<?, ?> iso) {
            Iso<Object, Object> typedIso = cast(iso);
            Lens<Object, Object> adapter = new Lens<>() {
                @Override
                public Object get(Object source) {
                    return typedIso.get(source);
                }

                @Override
                public Object set(Object value, Object source) {
                    return typedIso.reverseGet(value);
                }

                @Override
                public <F extends K1> App<F, Object> modifyF(
                        Function<Object, App<F, Object>> function,
                        Object source,
                        Functor<F, ?> functor) {
                    return functor.map(typedIso::reverseGet, function.apply(typedIso.get(source)));
                }
            };
            return Maybe.some(cast(OpticLowering.lens(key, adapter)));
        }
        return Maybe.none();
    }

    private static CompiledTerminal compileGeneric(TerminalShape shape) {
        LOWERED.incrementAndGet();
        PointFree<Function<Invocation, Object>> lowered =
                OpticLowering.terminal(
                        "terminal-" + shape.operation() + "-" + Integer.toUnsignedString(shape.program().hashCode()),
                        Invocation::execute);
        PointFree<Function<Invocation, Object>> optimized = PointFreeOptimizer.optimize(lowered);
        PointFreeExecutor<Function<Invocation, Object>> executor = PointFreeBytecodeBackend.compile(optimized);
        COMPILED.incrementAndGet();
        return new CompiledTerminal(call -> executor.execute().apply(call), executor.executorClass());
    }

    private static <S, T> CompiledTerminal compileSpecialized(PointFree<Function<S, T>> lowered) {
        LOWERED.incrementAndGet();
        SPECIALIZED.incrementAndGet();
        PointFree<Function<S, T>> optimized = PointFreeOptimizer.optimize(lowered);
        PointFreeExecutor<Function<S, T>> executor = PointFreeBytecodeBackend.compile(optimized);
        Function<S, T> function = executor.execute();
        COMPILED.incrementAndGet();
        return new CompiledTerminal(call -> {
            try {
                return function.apply(cast(call.source()));
            } catch (IllegalAccessError ignored) {
                if (call.userCodeStarted()) {
                    throw ignored;
                }
                ACCESS_FALLBACKS.incrementAndGet();
                return call.execute();
            }
        }, executor.executorClass());
    }

    private static Object applyModifier(Object value) {
        Invocation invocation = current();
        invocation.markUserCodeStarted();
        return invocation.function().apply(value);
    }

    private static boolean specialized(OpticProgram<?, ?, ?, ?> program) {
        if (program instanceof OpticProgram.Compose<?, ?, ?, ?, ?, ?> compose) {
            return specialized(compose.left()) && specialized(compose.right());
        }
        return program instanceof OpticProgram.Structured<?, ?, ?, ?> && SPECIALIZED_KINDS.contains(program.kind());
    }

    private static ProgramShape shape(OpticProgram<?, ?, ?, ?> program) {
        if (program instanceof OpticProgram.Compose<?, ?, ?, ?, ?, ?> compose) {
            return new ProgramShape(
                    ProgramNodeKind.COMPOSE,
                    "compose",
                    null,
                    shape(compose.left()),
                    shape(compose.right()));
        }
        if (program instanceof OpticProgram.Structured<?, ?, ?, ?> structured) {
            return new ProgramShape(
                    ProgramNodeKind.STRUCTURED,
                    structured.kind(),
                    structured.key(),
                    null,
                    null);
        }
        OpticProgram.Opaque<?, ?, ?, ?> opaque = (OpticProgram.Opaque<?, ?, ?, ?>) program;
        return new ProgramShape(
                ProgramNodeKind.OPAQUE,
                opaque.kind(),
                null,
                null,
                null);
    }

    private static Invocation current() {
        Invocation invocation = INVOCATIONS.get().peek();
        if (invocation == null) {
            throw new IllegalStateException("No terminal invocation is active");
        }
        return invocation;
    }

    private static <R> R invoke(CompiledTerminal terminal, Invocation invocation) {
        ArrayDeque<Invocation> stack = INVOCATIONS.get();
        stack.push(invocation);
        try {
            return invocation.source() == null
                    ? cast(terminal.function().apply(invocation))
                    : bypass(() -> cast(terminal.function().apply(invocation)));
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                INVOCATIONS.remove();
            }
        }
    }

    private static <R> R bypass(Supplier<R> action) {
        int depth = BYPASS.get();
        BYPASS.set(depth + 1);
        try {
            return action.get();
        } finally {
            if (depth == 0) {
                BYPASS.remove();
            } else {
                BYPASS.set(depth);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <A> A cast(Object value) {
        return (A) value;
    }

    private record TerminalShape(String operation, ProgramShape program) {
    }

    private enum ProgramNodeKind {
        OPAQUE,
        STRUCTURED,
        COMPOSE
    }

    private record ProgramShape(
            ProgramNodeKind nodeKind,
            String kind,
            Object key,
            ProgramShape left,
            ProgramShape right) {
    }

    private record CompiledTerminal(Function<Invocation, Object> function, Class<?> executorClass) {
    }

    private static final class Invocation {
        private final Function<Object, Object> function;
        private final Object value;
        private final Object source;
        private final Supplier<?> fallback;
        private final Object semantic;
        private boolean userCodeStarted;

        private Invocation(
                Function<Object, Object> function,
                Object value,
                Object source,
                Supplier<?> fallback,
                Object semantic) {
            this.function = function;
            this.value = value;
            this.source = source;
            this.fallback = fallback;
            this.semantic = semantic;
        }

        private Function<Object, Object> function() {
            return function;
        }

        private Object value() {
            return value;
        }

        private Object source() {
            return source;
        }

        private Supplier<?> fallback() {
            return fallback;
        }

        private Object semantic() {
            return semantic;
        }

        private void markUserCodeStarted() {
            userCodeStarted = true;
        }

        private boolean userCodeStarted() {
            return userCodeStarted;
        }

        private Object execute() {
            return bypass(fallback);
        }
    }
}
