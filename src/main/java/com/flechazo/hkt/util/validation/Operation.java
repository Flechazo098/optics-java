package com.flechazo.hkt.util.validation;

/**
 * Identifies operations for validation diagnostics.
 */
public enum Operation {
    /**
     * Identifies applicative function application.
     */
    AP("ap"),
    /**
     * Identifies functor mapping.
     */
    MAP("map"),
    /**
     * Identifies binary applicative mapping.
     */
    MAP_2("map2"),
    /**
     * Identifies ternary applicative mapping.
     */
    MAP_3("map3"),
    /**
     * Identifies four-argument applicative mapping.
     */
    MAP_4("map4"),
    /**
     * Identifies five-argument applicative mapping.
     */
    MAP_5("map5"),
    /**
     * Identifies monadic sequencing.
     */
    FLAT_MAP("flatMap"),
    /**
     * Identifies filtering.
     */
    FILTER("filter"),
    /**
     * Identifies monoidal folding.
     */
    FOLD_MAP("foldMap"),
    /**
     * Identifies applicative traversal.
     */
    TRAVERSE("traverse"),
    /**
     * Identifies effectful error recovery.
     */
    HANDLE_ERROR_WITH("handleErrorWith"),
    /**
     * Identifies recovery with another value.
     */
    RECOVER_WITH("recoverWith"),
    /**
     * Identifies value recovery.
     */
    RECOVER("recover"),
    /**
     * Identifies explicit error creation.
     */
    RAISE_ERROR("raiseError"),
    /**
     * Identifies value construction.
     */
    CONSTRUCTION("construction"),
    /**
     * Identifies left-alternative construction or access.
     */
    LEFT("left"),
    /**
     * Identifies right-alternative construction or access.
     */
    RIGHT("right"),
    /**
     * Identifies narrowing from an encoded value.
     */
    FROM_KIND("fromKind"),
    /**
     * Identifies conversion from an either value.
     */
    FROM_EITHER("fromEither"),
    /**
     * Identifies conversion from a maybe value.
     */
    FROM_MAYBE("fromMaybe"),
    /**
     * Identifies conversion from a Java optional value.
     */
    FROM_OPTIONAL("fromOptional"),
    /**
     * Identifies conversion from a list.
     */
    FROM_LIST("fromList"),
    /**
     * Identifies conversion from an optional list.
     */
    FROM_OPTIONAL_LIST("fromOptionalList"),
    /**
     * Identifies lifting an encoded effect.
     */
    LIFT_F("liftF"),
    /**
     * Identifies pure-value construction.
     */
    OF("of"),
    /**
     * Identifies conversion of failure into an explicit value.
     */
    ATTEMPT("attempt"),
    /**
     * Identifies environment access.
     */
    ASK("ask"),
    /**
     * Identifies mapped environment access.
     */
    ASKS("asks"),
    /**
     * Identifies computation execution.
     */
    RUN("run"),
    /**
     * Identifies state replacement.
     */
    SET("set"),
    /**
     * Identifies state or value modification.
     */
    MODIFY("modify"),
    /**
     * Identifies state inspection.
     */
    INSPECT("inspect"),
    /**
     * Identifies stateful computation execution.
     */
    RUN_STATE("runState"),
    /**
     * Identifies state transformer construction.
     */
    STATE_T("stateT"),
    /**
     * Identifies state transformer execution.
     */
    RUN_STATE_T("runStateT"),
    /**
     * Identifies evaluation of a state transformer result.
     */
    EVAL_STATE_T("evalStateT"),
    /**
     * Identifies evaluation of a state transformer state.
     */
    EXEC_STATE_T("execStateT"),
    /**
     * Identifies defined-value construction.
     */
    JUST("just"),
    /**
     * Identifies deferred computation construction.
     */
    DEFER("defer"),
    /**
     * Identifies present-value construction.
     */
    SOME("some"),
    /**
     * Identifies absent-value construction.
     */
    NONE("none"),
    /**
     * Identifies reader construction.
     */
    READER("reader"),
    /**
     * Identifies reader execution.
     */
    RUN_READER("runReader"),
    /**
     * Identifies folding.
     */
    FOLD("fold"),
    /**
     * Identifies conversion to an either value.
     */
    TO_EITHER("toEither"),
    /**
     * Identifies alternative matching.
     */
    MATCH("match"),
    /**
     * Identifies selection of one alternative.
     */
    OR_ELSE("orElse"),
    /**
     * Identifies selection among multiple alternatives.
     */
    OR_ELSE_ALL("orElseAll"),
    /**
     * Identifies deferred alternative selection.
     */
    OR_ELSE_GET("orElseGet"),
    /**
     * Identifies conversion of an alternative to an either value.
     */
    OR_EITHER("orEither"),
    /**
     * Identifies construction of a recovery function.
     */
    RECOVER_FUNCTION("recoverFunction"),
    /**
     * Identifies value access.
     */
    VALUE("value"),
    /**
     * Identifies writer output emission.
     */
    TELL("tell"),
    /**
     * Identifies conditional handling of a left value.
     */
    IF_LEFT("ifLeft"),
    /**
     * Identifies conditional handling of a right value.
     */
    IF_RIGHT("ifRight"),
    /**
     * Identifies sequencing of the first alternative.
     */
    SEQUENCE_A("sequenceA"),
    /**
     * Identifies sequencing of the second alternative.
     */
    SEQUENCE_B("sequenceB"),
    /**
     * Identifies conditional handling of a valid value.
     */
    IF_VALID("ifValid"),
    /**
     * Identifies conditional handling of an invalid value.
     */
    IF_INVALID("ifInvalid"),
    /**
     * Identifies extraction or exception creation.
     */
    OR_ELSE_THROW("orElseThrow"),
    /**
     * Identifies delayed execution.
     */
    DELAY("delay"),
    /**
     * Identifies invalid-value construction.
     */
    INVALID("invalid"),
    /**
     * Identifies selective conditional branching.
     */
    IF_S("ifS"),
    /**
     * Identifies selective application.
     */
    SELECT("select"),
    /**
     * Identifies selective conditional execution.
     */
    WHEN_S("whenS"),
    /**
     * Identifies selective branching between functions.
     */
    BRANCH("branch"),
    /**
     * Identifies mapping over both alternatives.
     */
    BIMAP("bimap"),
    /**
     * Identifies mapping over a first component.
     */
    FIRST("first"),
    /**
     * Identifies mapping over a second component.
     */
    SECOND("second"),
    /**
     * Identifies mapping over a left alternative.
     */
    MAP_LEFT("mapLeft"),
    /**
     * Identifies mapping over a right alternative.
     */
    MAP_RIGHT("mapRight"),
    /**
     * Identifies mapping over a first position.
     */
    MAP_FIRST("mapFirst"),
    /**
     * Identifies mapping over a second position.
     */
    MAP_SECOND("mapSecond"),
    /**
     * Identifies mapping over a third position.
     */
    MAP_THIRD("mapThird"),
    /**
     * Identifies mapping over a fourth position.
     */
    MAP_FOURTH("mapFourth"),
    /**
     * Identifies mapping over a fifth position.
     */
    MAP_FIFTH("mapFifth"),
    /**
     * Identifies mapping over a sixth position.
     */
    MAP_SIXTH("mapSixth"),
    /**
     * Identifies mapping over a seventh position.
     */
    MAP_SEVENTH("mapSeventh"),
    /**
     * Identifies mapping over an eighth position.
     */
    MAP_EIGHTH("mapEighth"),
    /**
     * Identifies mapping over a ninth position.
     */
    MAP_NINTH("mapNinth"),
    /**
     * Identifies mapping over a tenth position.
     */
    MAP_TENTH("mapTenth"),
    /**
     * Identifies mapping over an eleventh position.
     */
    MAP_ELEVENTH("mapEleventh"),
    /**
     * Identifies mapping over a twelfth position.
     */
    MAP_TWELFTH("mapTwelfth"),
    /**
     * Identifies mapping over an error channel.
     */
    MAP_ERROR("mapError"),
    /**
     * Identifies mapping over writer output.
     */
    MAP_WRITTEN("mapWritten"),
    /**
     * Identifies transformation of an underlying encoded value.
     */
    MAP_T("mapT"),
    /**
     * Identifies observation of writer output.
     */
    LISTEN("listen"),
    /**
     * Identifies transformation of writer output by a produced function.
     */
    PASS("pass"),
    /**
     * Identifies local environment transformation.
     */
    LOCAL("local"),
    /**
     * Identifies computation construction from a runnable action.
     */
    FROM_RUNNABLE("fromRunnable"),
    /**
     * Identifies forward composition.
     */
    AND_THEN("andThen"),
    /**
     * Identifies reverse composition.
     */
    COMPOSE("compose"),
    /**
     * Identifies widening to an encoded representation.
     */
    WIDEN("widen"),
    /**
     * Identifies narrowing from an encoded representation.
     */
    NARROW("narrow");

    private final String label;

    Operation(String label) {
        this.label = label;
    }

    /**
     * Returns the operation label used in diagnostics.
     *
     * @return the operation label
     */
    @Override
    public String toString() {
        return label;
    }
}
