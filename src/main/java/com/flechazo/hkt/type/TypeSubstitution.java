package com.flechazo.hkt.type;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record TypeSubstitution(
        Map<String, TypeExpr> variables,
        Map<TypeExpr.RecursiveSlot, TypeExpr> recursiveSlots) {
    public TypeSubstitution {
        Objects.requireNonNull(variables, "variables");
        Objects.requireNonNull(recursiveSlots, "recursiveSlots");
        variables = immutableCopy(variables);
        recursiveSlots = immutableCopy(recursiveSlots);
    }

    public static TypeSubstitution empty() {
        return new TypeSubstitution(Map.of(), Map.of());
    }

    public static TypeSubstitution variable(String name, TypeExpr replacement) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(replacement, "replacement");
        return new TypeSubstitution(Map.of(name, replacement), Map.of());
    }

    public static TypeSubstitution recursiveSlot(TypeExpr.RecursiveSlot slot, TypeExpr replacement) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(replacement, "replacement");
        return new TypeSubstitution(Map.of(), Map.of(slot, replacement));
    }

    public TypeSubstitution plusVariable(String name, TypeExpr replacement) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(replacement, "replacement");
        LinkedHashMap<String, TypeExpr> next = new LinkedHashMap<>(variables);
        next.put(name, replacement);
        return new TypeSubstitution(next, recursiveSlots);
    }

    public TypeSubstitution plusRecursiveSlot(TypeExpr.RecursiveSlot slot, TypeExpr replacement) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(replacement, "replacement");
        LinkedHashMap<TypeExpr.RecursiveSlot, TypeExpr> next = new LinkedHashMap<>(recursiveSlots);
        next.put(slot, replacement);
        return new TypeSubstitution(variables, next);
    }

    public TypeSubstitution andThen(TypeSubstitution after) {
        Objects.requireNonNull(after, "after");
        LinkedHashMap<String, TypeExpr> nextVariables = new LinkedHashMap<>();
        for (Map.Entry<String, TypeExpr> entry : variables.entrySet()) {
            nextVariables.put(entry.getKey(), entry.getValue().substitute(after));
        }
        for (Map.Entry<String, TypeExpr> entry : after.variables.entrySet()) {
            nextVariables.putIfAbsent(entry.getKey(), entry.getValue());
        }

        LinkedHashMap<TypeExpr.RecursiveSlot, TypeExpr> nextSlots = new LinkedHashMap<>();
        for (Map.Entry<TypeExpr.RecursiveSlot, TypeExpr> entry : recursiveSlots.entrySet()) {
            nextSlots.put(entry.getKey(), entry.getValue().substitute(after));
        }
        for (Map.Entry<TypeExpr.RecursiveSlot, TypeExpr> entry : after.recursiveSlots.entrySet()) {
            nextSlots.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return new TypeSubstitution(nextVariables, nextSlots);
    }

    public TypeExpr apply(TypeExpr type) {
        Objects.requireNonNull(type, "type");
        return switch (type) {
            case TypeExpr.Witness ignored -> type;
            case TypeExpr.Variable variable -> variables.getOrDefault(variable.name(), variable);
            case TypeExpr.Product product -> new TypeExpr.Product(
                    product.first().substitute(this),
                    product.second().substitute(this));
            case TypeExpr.Sum sum -> new TypeExpr.Sum(
                    sum.left().substitute(this),
                    sum.right().substitute(this));
            case TypeExpr.ListOf list -> new TypeExpr.ListOf(list.element().substitute(this));
            case TypeExpr.MapOf map -> new TypeExpr.MapOf(
                    map.key().substitute(this),
                    map.value().substitute(this));
            case TypeExpr.OptionalOf optional -> new TypeExpr.OptionalOf(optional.value().substitute(this));
            case TypeExpr.TaggedChoice choice -> new TypeExpr.TaggedChoice(
                    choice.name(),
                    choice.keyType().substitute(this),
                    substituteMap(choice.choices()),
                    choice.runtimeType());
            case TypeExpr.Variant variant -> new TypeExpr.Variant(
                    variant.name(),
                    variant.cases().stream().map(variantCase -> variantCase.substitute(this)).toList(),
                    variant.runtimeType());
            case TypeExpr.FunctionType function -> new TypeExpr.FunctionType(
                    function.argument().substitute(this),
                    function.result().substitute(this));
            case TypeExpr.RecursiveSlot slot -> recursiveSlots.getOrDefault(slot, slot);
            case TypeExpr.RecordType record -> new TypeExpr.RecordType(
                    record.name(),
                    record.fields().stream().map(field -> field.substitute(this)).toList(),
                    record.runtimeType());
        };
    }

    public TypeSubstitution normalized() {
        LinkedHashMap<String, TypeExpr> nextVariables = new LinkedHashMap<>();
        for (Map.Entry<String, TypeExpr> entry : variables.entrySet()) {
            nextVariables.put(entry.getKey(), entry.getValue().substitute(withoutVariable(entry.getKey())));
        }
        LinkedHashMap<TypeExpr.RecursiveSlot, TypeExpr> nextSlots = new LinkedHashMap<>();
        for (Map.Entry<TypeExpr.RecursiveSlot, TypeExpr> entry : recursiveSlots.entrySet()) {
            nextSlots.put(entry.getKey(), entry.getValue().substitute(this));
        }
        return new TypeSubstitution(nextVariables, nextSlots);
    }

    private Map<?, TypeExpr> substituteMap(Map<?, TypeExpr> choices) {
        LinkedHashMap<Object, TypeExpr> result = new LinkedHashMap<>();
        for (Map.Entry<?, TypeExpr> entry : choices.entrySet()) {
            result.put(entry.getKey(), entry.getValue().substitute(this));
        }
        return result;
    }

    private TypeSubstitution withoutVariable(String name) {
        LinkedHashMap<String, TypeExpr> next = new LinkedHashMap<>(variables);
        next.remove(name);
        return new TypeSubstitution(next, recursiveSlots);
    }

    private static <K, V> Map<K, V> immutableCopy(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
