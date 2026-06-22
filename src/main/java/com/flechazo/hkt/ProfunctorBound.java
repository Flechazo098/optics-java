package com.flechazo.hkt;

import com.google.common.reflect.TypeToken;

import java.util.Objects;

public record ProfunctorBound(String name, TypeToken<?> token) {
    public static final ProfunctorBound PROFUNCTOR = of("profunctor", Profunctor.class);
    public static final ProfunctorBound CARTESIAN = of("cartesian", Cartesian.class);
    public static final ProfunctorBound STRONG = of("strong", Strong.class);
    public static final ProfunctorBound COCARTESIAN = of("cocartesian", Cocartesian.class);
    public static final ProfunctorBound CHOICE = of("choice", Choice.class);
    public static final ProfunctorBound CLOSED = of("closed", Closed.class);
    public static final ProfunctorBound MAPPING = of("mapping", Mapping.class);
    public static final ProfunctorBound TRAVERSING = of("traversing", Traversing.class);
    public static final ProfunctorBound WANDER = of("wander", Wander.class);
    public static final ProfunctorBound MONOIDAL = of("monoidal", Monoidal.class);

    public ProfunctorBound {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(token, "token");
    }

    public static ProfunctorBound of(String name, Class<?> rawType) {
        Objects.requireNonNull(rawType, "rawType");
        return new ProfunctorBound(name, TypeToken.of(rawType));
    }

    public boolean isSatisfiedBy(Class<?> implementationType) {
        Objects.requireNonNull(implementationType, "implementationType");
        return token.getRawType().isAssignableFrom(implementationType);
    }
}
