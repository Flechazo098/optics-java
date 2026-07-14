package com.flechazo.optics.internal;

import com.flechazo.optics.spi.OpticsLookupProvider;
import org.jspecify.annotations.NonNull;

import java.lang.invoke.MethodHandles;
import java.util.*;

public final class OpticsLookupResolver {
    private static final ClassValue<MethodHandles.Lookup> LOOKUPS = new ClassValue<>() {
        @Override
        protected MethodHandles.Lookup computeValue(@NonNull Class<?> type) {
            return resolve(type);
        }
    };

    private OpticsLookupResolver() {
    }

    public static MethodHandles.Lookup lookupFor(Class<?> targetType) {
        return LOOKUPS.get(Objects.requireNonNull(targetType, "targetType"));
    }

    private static MethodHandles.Lookup resolve(Class<?> targetType) {
        Module targetModule = targetType.getModule();
        if (targetModule == OpticsLookupResolver.class.getModule()) {
            return MethodHandles.lookup();
        }

        List<MethodHandles.Lookup> matches = new ArrayList<>();
        try {
            for (OpticsLookupProvider provider : providers(targetType)) {
                if (provider.getClass().getModule() != targetModule || !provider.supports(targetType)) {
                    continue;
                }
                MethodHandles.Lookup lookup = Objects.requireNonNull(
                        provider.lookup(),
                        () -> provider.getClass().getName() + ".lookup()");
                if (lookup.lookupClass().getModule() != targetModule) {
                    throw configurationError(
                            targetType,
                            provider,
                            "returned a lookup belonging to module "
                                    + moduleName(lookup.lookupClass().getModule()));
                }
                if (!lookup.hasFullPrivilegeAccess()) {
                    throw configurationError(targetType, provider, "returned a lookup without full privilege access");
                }
                matches.add(lookup);
            }
        } catch (ServiceConfigurationError error) {
            throw new IllegalStateException(
                    "Unable to load OpticsLookupProvider for module " + moduleName(targetModule),
                    error);
        }

        if (matches.isEmpty()) {
            throw new IllegalStateException(
                    "No OpticsLookupProvider was found for module "
                            + moduleName(targetModule)
                            + " while generating optics for "
                            + targetType.getName()
                            + ". Register one with 'provides com.flechazo.optics.spi.OpticsLookupProvider with <provider-class>'.");
        }
        if (matches.size() != 1) {
            throw new IllegalStateException(
                    "Multiple OpticsLookupProvider implementations were found for module "
                            + moduleName(targetModule)
                            + " while generating optics for "
                            + targetType.getName());
        }
        return matches.getFirst();
    }

    private static ServiceLoader<OpticsLookupProvider> providers(Class<?> targetType) {
        ModuleLayer layer = targetType.getModule().getLayer();
        if (layer != null) {
            return ServiceLoader.load(layer, OpticsLookupProvider.class);
        }
        ClassLoader classLoader = targetType.getClassLoader();
        return ServiceLoader.load(
                OpticsLookupProvider.class,
                classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader);
    }

    private static IllegalStateException configurationError(
            Class<?> targetType,
            OpticsLookupProvider provider,
            String detail) {
        return new IllegalStateException(
                "Invalid OpticsLookupProvider "
                        + provider.getClass().getName()
                        + " for "
                        + targetType.getName()
                        + ": "
                        + detail);
    }

    private static String moduleName(Module module) {
        return module.isNamed() ? module.getName() : "<unnamed>";
    }
}
