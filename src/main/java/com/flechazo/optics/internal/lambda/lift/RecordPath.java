package com.flechazo.optics.internal.lambda.lift;

import java.util.List;

public record RecordPath(Class<?> sourceType, List<String> components) {
    public RecordPath {
        components = List.copyOf(components);
    }
}
