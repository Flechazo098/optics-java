package com.flechazo.hkt.functions;

import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Traversing;
import com.flechazo.hkt.Validated;
import com.flechazo.optics.Traversal;
import com.google.common.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record TraversalOpticElement(Object key, Traversal<Object, Object, Object, Object> traversal) implements PointFreeOpticElement {
    public TraversalOpticElement {
        Objects.requireNonNull(key, "key");
    }

    public static TraversalOpticElement list() {
        return new TraversalOpticElement("list", null);
    }

    public static TraversalOpticElement maybe() {
        return new TraversalOpticElement("maybe", null);
    }

    public static TraversalOpticElement stringCharacters() {
        return new TraversalOpticElement("stringCharacters", null);
    }

    public static TraversalOpticElement validatedValid() {
        return new TraversalOpticElement("validatedValid", null);
    }

    public static TraversalOpticElement validatedInvalid() {
        return new TraversalOpticElement("validatedInvalid", null);
    }

    public static TraversalOpticElement of(Object key, Traversal<Object, Object, Object, Object> traversal) {
        return new TraversalOpticElement(key, Objects.requireNonNull(traversal, "traversal"));
    }

    @Override
    public PointFreeOpticKind kind() {
        return PointFreeOpticKind.TRAVERSAL;
    }

    @Override
    public Set<TypeToken<? extends K1>> bounds() {
        return Set.of(Traversing.Mu.TYPE_TOKEN);
    }

    @Override
    public Object modify(Function<Object, Object> function, Object source) {
        if (traversal != null) {
            return traversal.modify(function, source);
        }
        if (!Objects.equals(key, "list")) {
            if (Objects.equals(key, "maybe")) {
                Maybe<?> maybe = (Maybe<?>) source;
                return maybe.isDefined() ? Maybe.some(function.apply(maybe.get())) : Maybe.none();
            }
            if (Objects.equals(key, "stringCharacters")) {
                String string = (String) source;
                StringBuilder result = new StringBuilder(string.length());
                for (int i = 0; i < string.length(); i++) {
                    result.append(function.apply(string.charAt(i)));
                }
                return result.toString();
            }
            if (Objects.equals(key, "validatedValid")) {
                Validated<?, ?> validated = (Validated<?, ?>) source;
                return validated.isValid() ? Validated.valid(function.apply(validated.value())) : validated;
            }
            if (Objects.equals(key, "validatedInvalid")) {
                Validated<?, ?> validated = (Validated<?, ?>) source;
                return validated.isInvalid() ? Validated.invalid(function.apply(validated.error())) : validated;
            }
            throw new IllegalStateException("Unknown traversal optic: " + key);
        }
        List<?> values = (List<?>) source;
        ArrayList<Object> result = new ArrayList<>(values.size());
        for (Object value : values) {
            result.add(function.apply(value));
        }
        return result;
    }
}
