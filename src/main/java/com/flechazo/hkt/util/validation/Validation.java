package com.flechazo.hkt.util.validation;

public interface Validation {
    CoreTypeValidator CORE = CoreTypeValidator.CORE_TYPE_VALIDATOR;
    FunctionValidator FUNCTION = FunctionValidator.FUNCTION_VALIDATOR;
    KindValidator KIND = KindValidator.KIND_VALIDATOR;
    TransformerValidator TRANSFORMER = TransformerValidator.TRANSFORMER_VALIDATOR;

    static CoreTypeValidator coreType() {
        return CORE;
    }

    static FunctionValidator function() {
        return FUNCTION;
    }

    static KindValidator kind() {
        return KIND;
    }

    static TransformerValidator transformer() {
        return TRANSFORMER;
    }
}
