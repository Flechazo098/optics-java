package com.flechazo.hkt.util.validation;

import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.Functor;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Monad;

import java.util.Objects;

public enum TransformerValidator {
    TRANSFORMER_VALIDATOR;

    public <F extends K1> Monad<F, ?> requireOuterMonad(
            Monad<F, ?> monad,
            Class<?> transformerClass,
            Operation operation) {
        Objects.requireNonNull(transformerClass, "transformerClass");
        Objects.requireNonNull(operation, "operation");
        String context = transformerClass.getSimpleName() + " " + operation;
        return Objects.requireNonNull(monad, new DomainContext("Outer Monad", context).nullParameterMessage());
    }

    public <F extends K1> Applicative<F, ?> requireOuterApplicative(
            Applicative<F, ?> applicative,
            Class<?> transformerClass,
            Operation operation) {
        Objects.requireNonNull(transformerClass, "transformerClass");
        Objects.requireNonNull(operation, "operation");
        String context = transformerClass.getSimpleName() + " " + operation;
        return Objects.requireNonNull(applicative, new DomainContext("Outer Applicative", context).nullParameterMessage());
    }

    public <F extends K1> Functor<F, ?> requireOuterFunctor(
            Functor<F, ?> functor,
            Class<?> transformerClass,
            Operation operation) {
        Objects.requireNonNull(transformerClass, "transformerClass");
        Objects.requireNonNull(operation, "operation");
        String context = transformerClass.getSimpleName() + " " + operation;
        return Objects.requireNonNull(functor, new DomainContext("Outer Functor", context).nullParameterMessage());
    }

    public <T> T requireTransformerComponent(
            T component,
            String componentName,
            Class<?> transformerClass,
            Operation operation) {
        Objects.requireNonNull(componentName, "componentName");
        Objects.requireNonNull(transformerClass, "transformerClass");
        Objects.requireNonNull(operation, "operation");
        String context = transformerClass.getSimpleName() + "." + operation;
        return Objects.requireNonNull(component, "%s cannot be null for %s".formatted(componentName, context));
    }

    public record DomainContext(String domainType, String objectName) {
        public DomainContext {
            Objects.requireNonNull(domainType, "domainType");
            Objects.requireNonNull(objectName, "objectName");
        }

        public static DomainContext transformer(String transformerName) {
            return new DomainContext("Transformer", transformerName);
        }

        public static DomainContext witness(String operation) {
            return new DomainContext("Witness", operation);
        }

        public String nullParameterMessage() {
            return "%s cannot be null for %s".formatted(domainType, objectName);
        }
    }
}
