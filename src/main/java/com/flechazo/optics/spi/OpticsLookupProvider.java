package com.flechazo.optics.spi;

import java.lang.invoke.MethodHandles;

public interface OpticsLookupProvider {
    MethodHandles.Lookup lookup();

    default boolean supports(Class<?> targetType) {
        return targetType.getModule() == getClass().getModule();
    }
}
