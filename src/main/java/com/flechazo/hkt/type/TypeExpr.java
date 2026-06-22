package com.flechazo.hkt.type;

import com.flechazo.hkt.Either;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Pair;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Function;

public sealed interface TypeExpr permits
        TypeExpr.Witness,
        TypeExpr.Variable,
        TypeExpr.Product,
        TypeExpr.Sum,
        TypeExpr.ListOf,
        TypeExpr.MapOf,
        TypeExpr.OptionalOf,
        TypeExpr.TaggedChoice,
        TypeExpr.Variant,
        TypeExpr.FunctionType,
        TypeExpr.RecursiveSlot,
        TypeExpr.RecordType {

    Maybe<TypeRef<?>> witness();

    TypeKind kind();

    default List<TypeExpr> children() {
        return switch (this) {
            case Witness ignored -> List.of();
            case Variable ignored -> List.of();
            case Product product -> List.of(product.first, product.second);
            case Sum sum -> List.of(sum.left, sum.right);
            case ListOf list -> List.of(list.element);
            case MapOf map -> List.of(map.key, map.value);
            case OptionalOf optional -> List.of(optional.value);
            case TaggedChoice choice -> {
                ArrayListBuilder children = new ArrayListBuilder(choice.choices.size() + 1);
                children.add(choice.keyType);
                choice.choices.values().forEach(children::add);
                yield children.toList();
            }
            case Variant variant -> variant.cases.stream().map(VariantCase::type).toList();
            case FunctionType function -> List.of(function.argument, function.result);
            case RecursiveSlot ignored -> List.of();
            case RecordType record -> record.fields.stream().map(RecordField::type).toList();
        };
    }

    default TypeExpr rebuild(List<TypeExpr> children) {
        Objects.requireNonNull(children, "children");
        return switch (this) {
            case Witness ignored -> requireChildCount(children, 0, this);
            case Variable ignored -> requireChildCount(children, 0, this);
            case Product ignored -> new Product(child(children, 0, this, 2), child(children, 1, this, 2));
            case Sum ignored -> new Sum(child(children, 0, this, 2), child(children, 1, this, 2));
            case ListOf ignored -> new ListOf(child(children, 0, this, 1));
            case MapOf ignored -> new MapOf(child(children, 0, this, 2), child(children, 1, this, 2));
            case OptionalOf ignored -> new OptionalOf(child(children, 0, this, 1));
            case TaggedChoice choice -> {
                requireSize(children, choice.choices.size() + 1, this);
                LinkedHashMap<Object, TypeExpr> next = new LinkedHashMap<>();
                int index = 1;
                for (Map.Entry<?, TypeExpr> entry : choice.choices.entrySet()) {
                    next.put(entry.getKey(), children.get(index++));
                }
                yield new TaggedChoice(choice.name, children.getFirst(), next, choice.runtimeType);
            }
            case Variant variant -> {
                requireSize(children, variant.cases.size(), this);
                ArrayListBuilder cases = new ArrayListBuilder(variant.cases.size());
                for (int i = 0; i < variant.cases.size(); i++) {
                    cases.add(new VariantCase(variant.cases.get(i).name, children.get(i)));
                }
                yield new Variant(variant.name, cases.toVariantCases(), variant.runtimeType);
            }
            case FunctionType ignored -> new FunctionType(child(children, 0, this, 2), child(children, 1, this, 2));
            case RecursiveSlot ignored -> requireChildCount(children, 0, this);
            case RecordType record -> {
                requireSize(children, record.fields.size(), this);
                ArrayListBuilder fields = new ArrayListBuilder(record.fields.size());
                for (int i = 0; i < record.fields.size(); i++) {
                    fields.add(new RecordField(record.fields.get(i).name, children.get(i)));
                }
                yield new RecordType(record.name, fields.toRecordFields(), record.runtimeType);
            }
        };
    }

    default Maybe<TypeExpr> all(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        List<TypeExpr> children = children();
        if (children.isEmpty()) {
            return Maybe.none();
        }
        boolean changed = false;
        ArrayListBuilder rewritten = new ArrayListBuilder(children.size());
        for (TypeExpr child : children) {
            TypeExpr next = rule.rewriteOrSame(child);
            changed |= next != child && !next.equals(child);
            rewritten.add(next);
        }
        return changed ? Maybe.some(rebuild(rewritten.toList())) : Maybe.none();
    }

    default Maybe<TypeExpr> one(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        List<TypeExpr> children = children();
        for (int i = 0; i < children.size(); i++) {
            Maybe<TypeExpr> next = rule.rewrite(children.get(i));
            if (next.isDefined()) {
                ArrayListBuilder rewritten = new ArrayListBuilder(children.size());
                for (int j = 0; j < children.size(); j++) {
                    rewritten.add(i == j ? next.get() : children.get(j));
                }
                return Maybe.some(rebuild(rewritten.toList()));
            }
        }
        return Maybe.none();
    }

    default TypeExpr substitute(TypeSubstitution substitution) {
        Objects.requireNonNull(substitution, "substitution");
        return substitution.apply(this);
    }

    default boolean containsVariable(String name) {
        Objects.requireNonNull(name, "name");
        if (this instanceof Variable(String name1) && name1.equals(name)) {
            return true;
        }
        for (TypeExpr child : children()) {
            if (child.containsVariable(name)) {
                return true;
            }
        }
        return false;
    }

    default boolean containsRecursiveSlot(TypeExpr.RecursiveSlot slot) {
        Objects.requireNonNull(slot, "slot");
        if (equals(slot)) {
            return true;
        }
        for (TypeExpr child : children()) {
            if (child.containsRecursiveSlot(slot)) {
                return true;
            }
        }
        return false;
    }

    default boolean sameType(TypeExpr other) {
        return equals(other);
    }

    static TypeExpr witness(TypeRef<?> type) {
        return new Witness(type);
    }

    static TypeExpr variable(String name) {
        return new Variable(name);
    }

    static TypeExpr product(TypeExpr first, TypeExpr second) {
        return new Product(first, second);
    }

    static TypeExpr sum(TypeExpr left, TypeExpr right) {
        return new Sum(left, right);
    }

    static TypeExpr list(TypeExpr element) {
        return new ListOf(element);
    }

    static TypeExpr map(TypeExpr key, TypeExpr value) {
        return new MapOf(key, value);
    }

    static TypeExpr optional(TypeExpr value) {
        return new OptionalOf(value);
    }

    static TypeExpr function(TypeExpr argument, TypeExpr result) {
        return new FunctionType(argument, result);
    }

    static TypeExpr recursiveSlot(String family, int index) {
        return new RecursiveSlot(family, index, Maybe.none());
    }

    static TypeExpr recursiveSlot(String family, int index, TypeRef<?> witness) {
        return new RecursiveSlot(family, index, Maybe.some(witness));
    }

    static RecordField field(String name, TypeExpr type) {
        return new RecordField(name, type);
    }

    static TypeExpr record(String name, List<RecordField> fields) {
        return new RecordType(name, fields, Maybe.none());
    }

    static TypeExpr record(String name, List<RecordField> fields, TypeRef<?> witness) {
        return new RecordType(name, fields, Maybe.some(witness));
    }

    static TypeExpr taggedChoice(String name, TypeExpr keyType, Map<?, TypeExpr> choices) {
        return new TaggedChoice(name, keyType, choices, Maybe.none());
    }

    static TypeExpr taggedChoice(String name, TypeExpr keyType, Map<?, TypeExpr> choices, TypeRef<?> witness) {
        return new TaggedChoice(name, keyType, choices, Maybe.some(witness));
    }

    static TypeExpr variant(String name, List<VariantCase> cases) {
        return new Variant(name, cases, Maybe.none());
    }

    static TypeExpr variant(String name, List<VariantCase> cases, TypeRef<?> witness) {
        return new Variant(name, cases, Maybe.some(witness));
    }

    record Witness(TypeRef<?> type) implements TypeExpr {
        public Witness {
            Objects.requireNonNull(type, "type");
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return Maybe.some(type);
        }

        @Override
        public TypeKind kind() {
            return TypeKind.WITNESS;
        }

        @Override
        @NonNull
        public String toString() {
            return type.toString();
        }
    }

    record Variable(String name) implements TypeExpr {
        public Variable {
            requireName(name, "name");
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return Maybe.none();
        }

        @Override
        public TypeKind kind() {
            return TypeKind.VARIABLE;
        }

        @Override
        @NonNull
        public String toString() {
            return "'" + name;
        }
    }

    record Product(TypeExpr first, TypeExpr second) implements TypeExpr {
        public Product {
            Objects.requireNonNull(first, "first");
            Objects.requireNonNull(second, "second");
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return first.witness().flatMap(firstType ->
                    second.witness().map(secondType -> TypeRef.parameterized(Pair.class, firstType, secondType)));
        }

        @Override
        public TypeKind kind() {
            return TypeKind.PRODUCT;
        }

        @Override
        @NonNull
        public String toString() {
            return "(" + first + ", " + second + ")";
        }
    }

    record Sum(TypeExpr left, TypeExpr right) implements TypeExpr {
        public Sum {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return left.witness().flatMap(leftType ->
                    right.witness().map(rightType -> TypeRef.parameterized(Either.class, leftType, rightType)));
        }

        @Override
        public TypeKind kind() {
            return TypeKind.SUM;
        }

        @Override
        @NonNull
        public String toString() {
            return "(" + left + " | " + right + ")";
        }
    }

    record ListOf(TypeExpr element) implements TypeExpr {
        public ListOf {
            Objects.requireNonNull(element, "element");
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return element.witness().map(elementType -> TypeRef.parameterized(List.class, elementType));
        }

        @Override
        public TypeKind kind() {
            return TypeKind.LIST;
        }

        @Override
        @NonNull
        public String toString() {
            return "[" + element + "]";
        }
    }

    record MapOf(TypeExpr key, TypeExpr value) implements TypeExpr {
        public MapOf {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return key.witness().flatMap(keyType ->
                    value.witness().map(valueType -> TypeRef.parameterized(Map.class, keyType, valueType)));
        }

        @Override
        public TypeKind kind() {
            return TypeKind.MAP;
        }

        @Override
        @NonNull
        public String toString() {
            return "Map[" + key + ", " + value + "]";
        }
    }

    record OptionalOf(TypeExpr value) implements TypeExpr {
        public OptionalOf {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return value.witness().map(valueType -> TypeRef.parameterized(Maybe.class, valueType));
        }

        @Override
        public TypeKind kind() {
            return TypeKind.OPTIONAL;
        }

        @Override
        @NonNull
        public String toString() {
            return "Maybe[" + value + "]";
        }
    }

    record TaggedChoice(
            String name,
            TypeExpr keyType,
            Map<?, TypeExpr> choices,
            Maybe<TypeRef<?>> runtimeType) implements TypeExpr {
        public TaggedChoice {
            requireName(name, "name");
            Objects.requireNonNull(keyType, "keyType");
            Objects.requireNonNull(choices, "choices");
            Objects.requireNonNull(runtimeType, "runtimeType");
            choices = copyMap(choices);
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return runtimeType;
        }

        @Override
        public TypeKind kind() {
            return TypeKind.TAGGED_CHOICE;
        }

        @Override
        @NonNull
        public String toString() {
            return "TaggedChoice[" + name + ", " + choices + "]";
        }
    }

    record Variant(
            String name,
            List<VariantCase> cases,
            Maybe<TypeRef<?>> runtimeType) implements TypeExpr {
        public Variant {
            requireName(name, "name");
            Objects.requireNonNull(cases, "cases");
            Objects.requireNonNull(runtimeType, "runtimeType");
            cases = List.copyOf(cases);
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return runtimeType;
        }

        @Override
        public TypeKind kind() {
            return TypeKind.VARIANT;
        }

        @Override
        @NonNull
        public String toString() {
            return "Variant[" + name + ", " + cases + "]";
        }
    }

    record FunctionType(TypeExpr argument, TypeExpr result) implements TypeExpr {
        public FunctionType {
            Objects.requireNonNull(argument, "argument");
            Objects.requireNonNull(result, "result");
        }

        public Maybe<FunctionType> compose(FunctionType inner) {
            Objects.requireNonNull(inner, "inner");
            return inner.result.sameType(argument)
                    ? Maybe.some(new FunctionType(inner.argument, result))
                    : Maybe.none();
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return argument.witness().flatMap(argumentType ->
                    result.witness().map(resultType -> TypeRef.parameterized(Function.class, argumentType, resultType)));
        }

        @Override
        public TypeKind kind() {
            return TypeKind.FUNCTION;
        }

        @Override
        @NonNull
        public String toString() {
            return argument + " -> " + result;
        }
    }

    record RecursiveSlot(
            String family,
            int index,
            Maybe<TypeRef<?>> runtimeType) implements TypeExpr {
        public RecursiveSlot {
            requireName(family, "family");
            if (index < 0) {
                throw new IllegalArgumentException("index must be non-negative");
            }
            Objects.requireNonNull(runtimeType, "runtimeType");
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return runtimeType;
        }

        @Override
        public TypeKind kind() {
            return TypeKind.RECURSIVE_SLOT;
        }

        @Override
        @NonNull
        public String toString() {
            return "Mu[" + family + "#" + index + "]";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof RecursiveSlot that
                    && family.equals(that.family)
                    && index == that.index;
        }

        @Override
        public int hashCode() {
            return 31 * family.hashCode() + index;
        }
    }

    record RecordType(
            String name,
            List<RecordField> fields,
            Maybe<TypeRef<?>> runtimeType) implements TypeExpr {
        public RecordType {
            requireName(name, "name");
            Objects.requireNonNull(fields, "fields");
            Objects.requireNonNull(runtimeType, "runtimeType");
            fields = List.copyOf(fields);
        }

        @Override
        public Maybe<TypeRef<?>> witness() {
            return runtimeType;
        }

        @Override
        public TypeKind kind() {
            return TypeKind.RECORD;
        }

        @Override
        @NonNull
        public String toString() {
            return "Record[" + name + ", " + fields + "]";
        }
    }

    record RecordField(String name, TypeExpr type) {
        public RecordField {
            requireName(name, "name");
            Objects.requireNonNull(type, "type");
        }

        RecordField substitute(TypeSubstitution substitution) {
            return new RecordField(name, type.substitute(substitution));
        }
    }

    record VariantCase(String name, TypeExpr type) {
        public VariantCase {
            requireName(name, "name");
            Objects.requireNonNull(type, "type");
        }

        VariantCase substitute(TypeSubstitution substitution) {
            return new VariantCase(name, type.substitute(substitution));
        }
    }

    private static void requireName(String value, String parameter) {
        Objects.requireNonNull(value, parameter);
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameter + " must not be blank");
        }
    }

    private static TypeExpr child(List<TypeExpr> children, int index, TypeExpr expression, int expected) {
        requireSize(children, expected, expression);
        return children.get(index);
    }

    private static TypeExpr requireChildCount(List<TypeExpr> children, int expected, TypeExpr expression) {
        requireSize(children, expected, expression);
        return expression;
    }

    private static void requireSize(List<TypeExpr> children, int expected, TypeExpr expression) {
        if (children.size() != expected) {
            throw new IllegalArgumentException(expression.kind() + " expects " + expected + " children, got " + children.size());
        }
    }

    private static Map<?, TypeExpr> copyMap(Map<?, TypeExpr> source) {
        LinkedHashMap<Object, TypeExpr> copy = new LinkedHashMap<>();
        for (Map.Entry<?, TypeExpr> entry : source.entrySet()) {
            copy.put(entry.getKey(), Objects.requireNonNull(entry.getValue(), "choice type"));
        }
        return Collections.unmodifiableMap(copy);
    }

    @SuppressWarnings("unchecked")
    record ArrayListBuilder(ArrayList<Object> values) {
            public ArrayListBuilder(int values) {
                this(new ArrayList<>(values));
            }

            private void add(Object value) {
                values.add(Objects.requireNonNull(value, "value"));
            }

            private List<TypeExpr> toList() {
                return List.copyOf((List<TypeExpr>) (List<?>) values);
            }

            private List<RecordField> toRecordFields() {
                return List.copyOf((List<RecordField>) (List<?>) values);
            }

            private List<VariantCase> toVariantCases() {
                return List.copyOf((List<VariantCase>) (List<?>) values);
            }
        }
}
