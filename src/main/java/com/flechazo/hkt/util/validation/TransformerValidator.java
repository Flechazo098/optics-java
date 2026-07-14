package com.flechazo.hkt.util.validation;

import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.Functor;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Monad;

import java.util.Objects;

/**
 * Validates the outer capabilities and components required by monad transformers.
 */
public enum TransformerValidator {
    /**
     * Provides the shared transformer validator.
     */
    TRANSFORMER_VALIDATOR;

    /**
     * Returns an outer monad after verifying that it is present.
     *
     * @param <F> the outer witness type
     * @param monad the outer monad to verify
     * @param transformerClass the transformer using the monad
     * @param operation the transformer operation
     * @return {@code monad}
     */
    public <F extends K1> Monad<F, ?> requireOuterMonad(
            Monad<F, ?> monad,
            Class<?> transformerClass,
            Operation operation) {
        Objects.requireNonNull(transformerClass, "transformerClass");
        Objects.requireNonNull(operation, "operation");
        String context = transformerClass.getSimpleName() + " " + operation;
        return Objects.requireNonNull(monad, new DomainContext("Outer Monad", context).nullParameterMessage());
    }

    /**
     * Returns an outer applicative after verifying that it is present.
     *
     * @param <F> the outer witness type
     * @param applicative the outer applicative to verify
     * @param transformerClass the transformer using the applicative
     * @param operation the transformer operation
     * @return {@code applicative}
     */
    public <F extends K1> Applicative<F, ?> requireOuterApplicative(
            Applicative<F, ?> applicative,
            Class<?> transformerClass,
            Operation operation) {
        Objects.requireNonNull(transformerClass, "transformerClass");
        Objects.requireNonNull(operation, "operation");
        String context = transformerClass.getSimpleName() + " " + operation;
        return Objects.requireNonNull(applicative, new DomainContext("Outer Applicative", context).nullParameterMessage());
    }

    /**
     * Returns an outer functor after verifying that it is present.
     *
     * @param <F> the outer witness type
     * @param functor the outer functor to verify
     * @param transformerClass the transformer using the functor
     * @param operation the transformer operation
     * @return {@code functor}
     */
    public <F extends K1> Functor<F, ?> requireOuterFunctor(
            Functor<F, ?> functor,
            Class<?> transformerClass,
            Operation operation) {
        Objects.requireNonNull(transformerClass, "transformerClass");
        Objects.requireNonNull(operation, "operation");
        String context = transformerClass.getSimpleName() + " " + operation;
        return Objects.requireNonNull(functor, new DomainContext("Outer Functor", context).nullParameterMessage());
    }

    /**
     * Returns a transformer component after verifying that it is present.
     *
     * @param <T> the component type
     * @param component the component to verify
     * @param componentName the component name used in diagnostics
     * @param transformerClass the transformer using the component
     * @param operation the transformer operation
     * @return {@code component}
     */
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

    /**
     * Describes a required domain value and the object using it.
     *
     * @param domainType the role of the required value
     * @param objectName the object or operation requiring the value
     */
    public record DomainContext(String domainType, String objectName) {
        /**
         * Creates a domain context.
         *
         * @param domainType the role of the required value
         * @param objectName the object or operation requiring the value
         */
        public DomainContext {
            Objects.requireNonNull(domainType, "domainType");
            Objects.requireNonNull(objectName, "objectName");
        }

        /**
         * Creates a context for a transformer.
         *
         * @param transformerName the transformer name
         * @return a transformer context
         */
        public static DomainContext transformer(String transformerName) {
            return new DomainContext("Transformer", transformerName);
        }

        /**
         * Creates a context for a higher-kinded witness.
         *
         * @param operation the operation requiring the witness
         * @return a witness context
         */
        public static DomainContext witness(String operation) {
            return new DomainContext("Witness", operation);
        }

        /**
         * Returns the diagnostic message for an absent domain value.
         *
         * @return the diagnostic message
         */
        public String nullParameterMessage() {
            return "%s cannot be null for %s".formatted(domainType, objectName);
        }
    }
}
