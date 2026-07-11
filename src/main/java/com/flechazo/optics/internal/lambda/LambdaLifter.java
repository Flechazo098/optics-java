package com.flechazo.optics.internal.lambda;

import com.flechazo.optics.AffinePreview;
import com.flechazo.optics.AffineRebuilder;
import com.flechazo.optics.FoldGetter;
import com.flechazo.optics.GetterReader;
import com.flechazo.optics.IsoGetter;
import com.flechazo.optics.IsoRebuilder;
import com.flechazo.optics.LensGetter;
import com.flechazo.optics.LensRebuilder;
import com.flechazo.optics.PrismBuilder;
import com.flechazo.optics.PrismMatcher;
import com.flechazo.optics.SetterModifier;
import com.flechazo.optics.WanderGetter;
import com.flechazo.optics.WanderRebuilder;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.PLens;
import com.flechazo.optics.PAffine;
import com.flechazo.optics.PIso;
import com.flechazo.optics.PPrism;
import com.flechazo.optics.PSetter;
import com.flechazo.optics.Fold;
import com.flechazo.optics.Getter;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticProgram;
import com.flechazo.optics.internal.OpticPrograms;
import com.flechazo.optics.internal.lambda.lift.RecordLensLifter;
import com.flechazo.optics.internal.lambda.lift.IsoLifter;
import com.flechazo.optics.internal.lambda.lift.PrismLifter;
import com.flechazo.optics.internal.lambda.lift.AffineLifter;
import com.flechazo.optics.internal.lambda.lift.RecordSetterLifter;
import com.flechazo.optics.internal.lambda.lift.TraversalLifter;
import com.flechazo.optics.internal.lambda.lift.FoldLifter;

import com.flechazo.optics.internal.lambda.lift.RecordPath;

public final class LambdaLifter {
    private static final LambdaAnalyzer ANALYZER = new LambdaAnalyzer();
    private static final RecordLensLifter LENS_LIFTER = new RecordLensLifter();
    private static final IsoLifter ISO_LIFTER = new IsoLifter();
    private static final PrismLifter PRISM_LIFTER = new PrismLifter();
    private static final AffineLifter AFFINE_LIFTER = new AffineLifter();
    private static final RecordSetterLifter SETTER_LIFTER = new RecordSetterLifter();
    private static final TraversalLifter TRAVERSAL_LIFTER = new TraversalLifter();
    private static final FoldLifter FOLD_LIFTER = new FoldLifter();

    private LambdaLifter() {
    }

    public static <S, T, A, B> PLens<S, T, A, B> lens(
            PLens<S, T, A, B> direct,
            LensGetter<? super S, ? extends A> getter,
            LensRebuilder<S, B, T> setter) {
        var getterExpr = ANALYZER.analyze(getter);
        var setterExpr = ANALYZER.analyze(setter);
        if (getterExpr.isDefined() && setterExpr.isDefined()) {
            var lifted = LENS_LIFTER.lift(getterExpr.get(), setterExpr.get());
            if (lifted.isDefined()) {
                OpticProgram<S, T, A, B> program = OpticPrograms.structured("recordPathLens", lifted.get().key());
                return OpticPrograms.lens(OpticMetadata.optic(direct, OpticMetadata.optic(direct)), program);
            }
        }
        return OpticPrograms.lens(direct, OpticPrograms.opaque("lens", null));
    }

    public static Maybe<RecordPath> recordPath(java.io.Serializable getter) {
        return ANALYZER.analyze(getter).flatMap(LENS_LIFTER::liftGetter);
    }

    public static <S, A> Getter<S, A> getter(
            Getter<S, A> direct,
            GetterReader<? super S, ? extends A> getter) {
        var expression = ANALYZER.analyze(getter);
        if (expression.isDefined()) {
            var path = FOLD_LIFTER.lift(expression.get());
            if (path.isDefined()) {
                return OpticPrograms.getter(direct, OpticPrograms.structured("recordGetter", path.get()));
            }
        }
        return OpticPrograms.getter(direct, OpticPrograms.opaque("getter", null));
    }

    public static <S, A> Fold<S, A> fold(
            Fold<S, A> direct,
            FoldGetter<? super S, A> getter) {
        var expression = ANALYZER.analyze(getter);
        if (expression.isDefined()) {
            var path = LENS_LIFTER.liftGetter(expression.get());
            if (path.isDefined()) {
                return OpticPrograms.fold(direct, OpticPrograms.structured("recordFold", path.get()));
            }
        }
        return OpticPrograms.fold(direct, OpticPrograms.opaque("fold", null));
    }

    public static <S, A> Traversal<S, A> traversal(
            Traversal<S, A> direct,
            WanderGetter<? super S, A> targets,
            WanderRebuilder<S, A> rebuild) {
        var first = ANALYZER.analyze(targets);
        var second = ANALYZER.analyze(rebuild);
        OpticProgram<S, S, A, A> program = first.isDefined() && second.isDefined()
                ? TRAVERSAL_LIFTER.lift(first.get(), second.get())
                        .<OpticProgram<S, S, A, A>>map(node -> OpticPrograms.structured(node.kind(), node))
                        .orElseGet(() -> OpticPrograms.opaque("traversal", null))
                : OpticPrograms.opaque("traversal", null);
        return OpticPrograms.traversal(direct, program);
    }

    public static <A> Traversal<A[], A> arrayTraversal(
            Traversal<A[], A> direct,
            Class<A> componentType,
            WanderGetter<A[], A> targets,
            WanderRebuilder<A[], A> rebuild) {
        var first = ANALYZER.analyze(targets);
        var second = ANALYZER.analyze(rebuild);
        OpticProgram<A[], A[], A, A> program = first.isDefined()
                && second.isDefined()
                && TRAVERSAL_LIFTER.liftsArray(first.get(), second.get())
                ? OpticPrograms.structured("arrayTraversal", componentType)
                : OpticPrograms.opaque("traversal", null);
        return OpticPrograms.traversal(direct, program);
    }

    public static <S, T, A, B> PIso<S, T, A, B> iso(
            PIso<S, T, A, B> direct,
            IsoGetter<? super S, ? extends A> get,
            IsoRebuilder<? super B, ? extends T> reverseGet) {
        var first = ANALYZER.analyze(get);
        var second = ANALYZER.analyze(reverseGet);
        OpticProgram<S, T, A, B> program = first.isDefined() && second.isDefined()
                ? ISO_LIFTER.lift(first.get(), second.get())
                        .<OpticProgram<S, T, A, B>>map(key -> OpticPrograms.structured("liftedIso", key))
                        .orElseGet(() -> OpticPrograms.opaque("iso", null))
                : OpticPrograms.opaque("iso", null);
        return OpticPrograms.iso(direct, program);
    }

    public static <S, T, A, B> PPrism<S, T, A, B> prism(
            PPrism<S, T, A, B> direct,
            PrismMatcher<? super S, T, A> match,
            PrismBuilder<? super B, ? extends T> build) {
        var first = ANALYZER.analyze(match);
        var second = ANALYZER.analyze(build);
        OpticProgram<S, T, A, B> program = first.isDefined() && second.isDefined()
                ? PRISM_LIFTER.lift(first.get(), second.get())
                        .<OpticProgram<S, T, A, B>>map(key -> OpticPrograms.structured(key.kind(), key))
                        .orElseGet(() -> OpticPrograms.opaque("prism", null))
                : OpticPrograms.opaque("prism", null);
        return OpticPrograms.prism(direct, program);
    }

    public static <S, T, A, B> PAffine<S, T, A, B> affine(
            PAffine<S, T, A, B> direct,
            AffinePreview<? super S, T, A> preview,
            AffineRebuilder<S, B, T> setter) {
        var first = ANALYZER.analyze(preview);
        var second = ANALYZER.analyze(setter);
        OpticProgram<S, T, A, B> program = first.isDefined() && second.isDefined()
                ? AFFINE_LIFTER.lift(first.get(), second.get())
                        .<OpticProgram<S, T, A, B>>map(key -> OpticPrograms.structured(key.kind(), key))
                        .orElseGet(() -> OpticPrograms.opaque("affine", null))
                : OpticPrograms.opaque("affine", null);
        return OpticPrograms.affine(direct, program);
    }

    public static <S, T, A, B> PSetter<S, T, A, B> setter(
            PSetter<S, T, A, B> direct,
            SetterModifier<S, T, A, B> modify) {
        OpticProgram<S, T, A, B> program = ANALYZER.analyze(modify)
                .flatMap(SETTER_LIFTER::lift)
                .<OpticProgram<S, T, A, B>>map(key -> OpticPrograms.structured("recordPathSetter", key))
                .orElseGet(() -> OpticPrograms.opaque("setter", null));
        return OpticPrograms.setter(direct, program);
    }

    public static <S, T, A, B> PSetter<S, T, A, B> setter(
            PSetter<S, T, A, B> direct,
            LensGetter<? super S, ? extends A> getter,
            LensRebuilder<S, B, T> setter) {
        var first = ANALYZER.analyze(getter);
        var second = ANALYZER.analyze(setter);
        OpticProgram<S, T, A, B> program = first.isDefined() && second.isDefined()
                ? LENS_LIFTER.lift(first.get(), second.get())
                        .<OpticProgram<S, T, A, B>>map(match ->
                                OpticPrograms.structured("recordPathSetter", match.key()))
                        .orElseGet(() -> OpticPrograms.opaque("setter", null))
                : OpticPrograms.opaque("setter", null);
        return OpticPrograms.setter(direct, program);
    }

}
