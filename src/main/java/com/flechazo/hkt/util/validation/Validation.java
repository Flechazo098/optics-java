package com.flechazo.hkt.util.validation;

/**
 * Provides shared validators for higher-kinded and business APIs.
 */
public interface Validation {
    /**
     * Provides the core-value validator.
     */
    CoreTypeValidator CORE = CoreTypeValidator.CORE_TYPE_VALIDATOR;
    /**
     * Provides the function validator.
     */
    FunctionValidator FUNCTION = FunctionValidator.FUNCTION_VALIDATOR;
    /**
     * Provides the higher-kinded value validator.
     */
    KindValidator KIND = KindValidator.KIND_VALIDATOR;
    /**
     * Provides the transformer validator.
     */
    TransformerValidator TRANSFORMER = TransformerValidator.TRANSFORMER_VALIDATOR;

    /**
     * Returns the shared core-value validator.
     *
     * @return the core-value validator
     */
    static CoreTypeValidator coreType() {
        return CORE;
    }

    /**
     * Returns the shared function validator.
     *
     * @return the function validator
     */
    static FunctionValidator function() {
        return FUNCTION;
    }

    /**
     * Returns the shared higher-kinded value validator.
     *
     * @return the higher-kinded value validator
     */
    static KindValidator kind() {
        return KIND;
    }

    /**
     * Returns the shared transformer validator.
     *
     * @return the transformer validator
     */
    static TransformerValidator transformer() {
        return TRANSFORMER;
    }
}
