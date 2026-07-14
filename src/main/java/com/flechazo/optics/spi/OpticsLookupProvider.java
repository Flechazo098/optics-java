package com.flechazo.optics.spi;

import java.lang.invoke.MethodHandles;

/**
 * Provides a caller-authorized lookup for generated optic executors.
 *
 * <p>A provider is discovered through {@link java.util.ServiceLoader}. The returned lookup must
 * have the access privileges required for every target type accepted by {@link #supports(Class)}.
 */
public interface OpticsLookupProvider {
    /**
     * Returns the lookup used to access or define generated executor classes.
     *
     * @return the authorized lookup
     */
    MethodHandles.Lookup lookup();

    /**
     * Determines whether this provider supplies a suitable lookup for a target type.
     *
     * <p>The default implementation accepts target types in the provider implementation's module.
     *
     * @param targetType the class whose module and package require access
     * @return {@code true} when {@link #lookup()} is suitable for the target type
     */
    default boolean supports(Class<?> targetType) {
        return targetType.getModule() == getClass().getModule();
    }
}
