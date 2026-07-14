package com.flechazo.hkt.business.control;

import com.flechazo.hkt.*;
import com.flechazo.hkt.util.validation.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.flechazo.hkt.util.validation.Operation.*;

/**
 * Represents an immutable list in higher-kinded form.
 *
 * <p>Elements are non-null and preserve encounter order.
 *
 * @param <A> the element type
 */
public interface ListK<A> extends App<ListK.Mu, A> {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    /**
     * Wraps a list without changing its encounter order.
     *
     * @param <A> the element type
     * @param values the source list
     * @return an immutable list value
     */
    static <A> ListK<A> of(List<? extends A> values) {
        Objects.requireNonNull(values, "values");
        requireElements(values);
        return new Holder<>((List<A>) values);
    }

    /**
     * Creates a list value from encountered elements.
     *
     * @param <A> the element type
     * @param values the source elements
     * @return an immutable list value in encounter order
     */
    static <A> ListK<A> from(Iterable<? extends A> values) {
        Objects.requireNonNull(values, "values");
        if (values instanceof List<?> list) {
            return of((List<? extends A>) list);
        }
        ArrayList<A> collected = new ArrayList<>();
        for (A value : values) {
            collected.add(Objects.requireNonNull(value, "value"));
        }
        return new Holder<>(collected);
    }

    /**
     * Returns the empty list value.
     *
     * @param <A> the element type
     * @return the empty list value
     */
    static <A> ListK<A> empty() {
        return of(List.of());
    }

    /**
     * Creates a list value containing one element.
     *
     * @param <A> the element type
     * @param value the sole element
     * @return a singleton list value
     */
    static <A> ListK<A> singleton(A value) {
        return of(List.of(Objects.requireNonNull(value, "value")));
    }

    /**
     * Narrows an encoded list and returns its underlying list.
     *
     * @param <A> the element type
     * @param value the encoded list
     * @return the underlying unmodifiable list
     * @throws com.flechazo.hkt.exception.KindUnwrapException if {@code value} is not a list value
     */
    static <A> List<A> narrow(App<Mu, A> value) {
        return (List<A>) Validation.kind().narrowHolder(value, List.class, Holder.class, Holder::values);
    }

    /**
     * Narrows an encoded list value.
     *
     * @param <A> the element type
     * @param value the encoded list
     * @return the concrete list value
     * @throws com.flechazo.hkt.exception.KindUnwrapException if {@code value} is not a list value
     */
    static <A> ListK<A> unbox(App<Mu, A> value) {
        return (ListK<A>) Validation.kind().narrowWithTypeCheck(value, ListK.class);
    }

    /**
     * Returns the shared list type-class instance.
     *
     * @return the list instance
     */
    static Instance instance() {
        return Instance.INSTANCE;
    }

    /**
     * Returns the list functor instance.
     *
     * @return the list functor
     */
    static Functor<ListK.Mu, Instance.Mu> functor() {
        return Instance.INSTANCE;
    }

    /**
     * Returns the list applicative instance.
     *
     * @return the list applicative
     */
    static Applicative<ListK.Mu, Instance.Mu> applicative() {
        return Instance.INSTANCE;
    }

    /**
     * Returns the list monad instance.
     *
     * @return the list monad
     */
    static Monad<ListK.Mu, Instance.Mu> monad() {
        return Instance.INSTANCE;
    }

    /**
     * Returns the list monad-with-zero instance.
     *
     * @return the list monad-with-zero
     */
    static MonadZero<ListK.Mu, Instance.Mu> monadZero() {
        return Instance.INSTANCE;
    }

    /**
     * Returns the list selective instance.
     *
     * @return the list selective
     */
    static Selective<ListK.Mu, Instance.Mu> selective() {
        return Instance.INSTANCE;
    }

    /**
     * Returns the list traversable instance.
     *
     * @return the list traversable
     */
    static Traversable<ListK.Mu, Instance.Mu> traversable() {
        return Instance.INSTANCE;
    }

    /**
     * Returns the underlying unmodifiable list.
     *
     * @return the elements in encounter order
     */
    default List<A> toList() {
        return narrow(this);
    }

    /**
     * Returns a list value with an element added at the beginning.
     *
     * @param value the element to prepend
     * @return the extended list value
     */
    default ListK<A> prepend(A value) {
        Objects.requireNonNull(value, "value");
        List<A> current = toList();
        ArrayList<A> result = new ArrayList<>(current.size() + 1);
        result.add(value);
        result.addAll(current);
        return ListK.of(result);
    }

    /**
     * Returns a list value with an element added at the end.
     *
     * @param value the element to append
     * @return the extended list value
     */
    default ListK<A> append(A value) {
        Objects.requireNonNull(value, "value");
        List<A> current = toList();
        ArrayList<A> result = new ArrayList<>(current.size() + 1);
        result.addAll(current);
        result.add(value);
        return ListK.of(result);
    }

    /**
     * Concatenates this list value with another list value.
     *
     * @param other the value whose elements follow this value
     * @return the concatenated list value
     */
    default ListK<A> concat(ListK<? extends A> other) {
        Objects.requireNonNull(other, "other");
        List<A> current = toList();
        List<? extends A> next = other.toList();
        ArrayList<A> result = new ArrayList<>(current.size() + next.size());
        result.addAll(current);
        result.addAll(next);
        return ListK.of(result);
    }

    /**
     * Stores the concrete list represented by a {@link ListK}.
     *
     * @param <A> the element type
     * @param values the represented elements
     */
    record Holder<A>(List<A> values) implements ListK<A> {
        /**
         * Creates a list holder.
         *
         * @param values the represented elements
         */
        public Holder {
            Objects.requireNonNull(values, "values");
            requireElements(values);
            values = Collections.unmodifiableList(values);
        }
    }

    /**
     * Provides list functor, monad, selective, and traversable operations.
     */
    enum Instance implements MonadZero<ListK.Mu, Instance.Mu>,
            Selective<ListK.Mu, Instance.Mu>,
            Traversable<ListK.Mu, Instance.Mu> {
        /**
         * Provides the shared list type-class instance.
         */
        INSTANCE;

        public static final class Mu implements MonadZero.Mu, Traversable.Mu {
            private Mu() {
            }
        }

        /**
         * Creates a singleton encoded list.
         *
         * @param <A> the element type
         * @param value the sole element
         * @return the singleton list in encoded form
         */
        @Override
        public <A> App<ListK.Mu, A> of(A value) {
            return ListK.singleton(value);
        }

        /**
         * Transforms every encoded list element in encounter order.
         *
         * @param <A> the source element type
         * @param <B> the result element type
         * @param f the element transformation
         * @param fa the source list
         * @return the transformed list in encoded form
         */
        @Override
        public <A, B> App<ListK.Mu, B> map(Function<? super A, ? extends B> f, App<ListK.Mu, A> fa) {
            Validation.function().validateMap(f, fa);
            List<A> input = ListK.narrow(fa);
            ArrayList<B> result = new ArrayList<>(input.size());
            for (A value : input) {
                result.add(Validation.function().requireNonNullResult(f.apply(value), "f", MAP));
            }
            return ListK.of(result);
        }

        /**
         * Applies every encoded function to every encoded value in function-major order.
         *
         * @param <A> the argument type
         * @param <B> the result type
         * @param ff the encoded functions
         * @param fa the encoded arguments
         * @return the application results in encoded list form
         */
        @Override
        public <A, B> App<ListK.Mu, B> ap(
                App<ListK.Mu, ? extends Function<A, B>> ff,
                App<ListK.Mu, A> fa) {
            List<? extends Function<A, B>> functions = ListK.narrow(ff);
            List<A> values = ListK.narrow(fa);
            ArrayList<B> result = new ArrayList<>(functions.size() * values.size());
            for (Function<A, B> function : functions) {
                Function<A, B> apply = Objects.requireNonNull(function, "function");
                for (A value : values) {
                    result.add(Objects.requireNonNull(apply.apply(value), "ap result"));
                }
            }
            return ListK.of(result);
        }

        /**
         * Maps each element to an encoded list and concatenates the results in encounter order.
         *
         * @param <A> the source element type
         * @param <B> the result element type
         * @param f the list-producing transformation
         * @param fa the source list
         * @return the concatenated results in encoded list form
         */
        @Override
        public <A, B> App<ListK.Mu, B> flatMap(
                Function<? super A, ? extends App<ListK.Mu, B>> f,
                App<ListK.Mu, A> fa) {
            Validation.function().validateFlatMap(f, fa);
            ArrayList<B> result = new ArrayList<>();
            for (A value : ListK.narrow(fa)) {
                App<ListK.Mu, B> mapped = Validation.function().requireNonNullResult(f.apply(value), "f", FLAT_MAP);
                for (B mappedValue : ListK.narrow(mapped)) {
                    result.add(Objects.requireNonNull(mappedValue, "flatMap value"));
                }
            }
            return ListK.of(result);
        }

        /**
         * Returns the empty encoded list.
         *
         * @param <A> the element type
         * @return the empty list in encoded form
         */
        @Override
        public <A> App<ListK.Mu, A> zero() {
            return ListK.empty();
        }

        /**
         * Concatenates two encoded list alternatives.
         *
         * @param <A> the element type
         * @param first the first list
         * @param second the deferred second list
         * @return both alternatives concatenated in encoded list form
         */
        @Override
        public <A> App<ListK.Mu, A> orElse(
                App<ListK.Mu, A> first,
                Supplier<? extends App<ListK.Mu, A>> second) {
            Objects.requireNonNull(second, "second");
            List<A> left = ListK.narrow(first);
            List<A> right = ListK.narrow(Objects.requireNonNull(second.get(), "second result"));
            ArrayList<A> result = new ArrayList<>(left.size() + right.size());
            result.addAll(left);
            result.addAll(right);
            return ListK.of(result);
        }

        /**
         * Concatenates encoded list alternatives in encounter order.
         *
         * @param <A> the element type
         * @param alternatives the lists to concatenate
         * @return all alternatives concatenated in encoded list form
         */
        @Override
        public <A> App<ListK.Mu, A> orElseAll(Iterable<? extends App<ListK.Mu, A>> alternatives) {
            Objects.requireNonNull(alternatives, "alternatives");
            ArrayList<A> result = new ArrayList<>();
            for (App<ListK.Mu, A> alternative : alternatives) {
                result.addAll(ListK.narrow(Objects.requireNonNull(alternative, "alternative")));
            }
            return ListK.of(result);
        }

        /**
         * Retains encoded list elements satisfying a predicate.
         *
         * @param <A> the element type
         * @param predicate the condition for retaining an element
         * @param value the source list
         * @return the matching elements in encoded list form
         */
        @Override
        public <A> App<ListK.Mu, A> filter(Predicate<? super A> predicate, App<ListK.Mu, A> value) {
            Objects.requireNonNull(predicate, "predicate");
            ArrayList<A> result = new ArrayList<>();
            for (A element : ListK.narrow(value)) {
                if (predicate.test(element)) {
                    result.add(element);
                }
            }
            return ListK.of(result);
        }

        /**
         * Resolves every encoded either value, applying every encoded function to each left value.
         *
         * @param <A> the function argument type
         * @param <B> the result type
         * @param value the encoded branch values
         * @param function the encoded functions used for left values
         * @return the selected results in encoded list form
         */
        @Override
        public <A, B> App<ListK.Mu, B> select(
                App<ListK.Mu, Either<A, B>> value,
                App<ListK.Mu, ? extends Function<A, B>> function) {
            List<Either<A, B>> choices = ListK.narrow(value);
            List<? extends Function<A, B>> functions = ListK.narrow(function);
            ArrayList<B> result = new ArrayList<>();
            for (Either<A, B> choice : choices) {
                Either<A, B> either = Validation.coreType().requireValue(choice, "select value", ListK.class, SELECT);
                if (either.isRight()) {
                    result.add(either.right());
                } else {
                    for (Function<A, B> fn : functions) {
                        Function<A, B> apply = Validation.coreType().requireValue(fn, "select function", ListK.class, SELECT);
                        result.add(Objects.requireNonNull(apply.apply(either.left()), "select result"));
                    }
                }
            }
            return ListK.of(result);
        }

        /**
         * Selects and concatenates a deferred list branch for every encoded condition.
         *
         * @param <A> the result type
         * @param condition the encoded conditions
         * @param thenValue the deferred list for true conditions
         * @param elseValue the deferred list for false conditions
         * @return the selected elements in encoded list form
         */
        @Override
        public <A> App<ListK.Mu, A> ifS(
                App<ListK.Mu, Boolean> condition,
                Supplier<? extends App<ListK.Mu, A>> thenValue,
                Supplier<? extends App<ListK.Mu, A>> elseValue) {
            Objects.requireNonNull(thenValue, "thenValue");
            Objects.requireNonNull(elseValue, "elseValue");
            ArrayList<A> result = new ArrayList<>();
            List<A> thenValues = null;
            List<A> elseValues = null;
            for (Boolean test : ListK.narrow(condition)) {
                if (Boolean.TRUE.equals(test)) {
                    if (thenValues == null) {
                        thenValues = ListK.narrow(Validation.function().requireNonNullResult(thenValue.get(), "thenValue", IF_S));
                    }
                    result.addAll(thenValues);
                } else {
                    if (elseValues == null) {
                        elseValues = ListK.narrow(Validation.function().requireNonNullResult(elseValue.get(), "elseValue", IF_S));
                    }
                    result.addAll(elseValues);
                }
            }
            return ListK.of(result);
        }

        /**
         * Maps encoded list elements to a monoid and combines them in encounter order.
         *
         * @param <A> the element type
         * @param <M> the accumulated value type
         * @param monoid the monoid used to combine mapped values
         * @param f the element mapping function
         * @param value the source list
         * @return the combined mapped value
         */
        @Override
        public <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<ListK.Mu, A> value) {
            Validation.function().validateFoldMap(monoid, f, value);
            M result = monoid.empty();
            for (A element : ListK.narrow(value)) {
                result = monoid.combine(result, f.apply(element));
            }
            return result;
        }

        /**
         * Applies an applicative transformation to encoded list elements in encounter order.
         *
         * @param <F> the applicative witness type
         * @param <A> the source element type
         * @param <B> the result element type
         * @param applicative the applicative used to combine effects
         * @param f the effectful element transformation
         * @param value the source list
         * @return the transformed encoded list in the applicative context
         */
        @Override
        public <F extends K1, A, B> App<F, App<ListK.Mu, B>> traverse(
                Applicative<F, ?> applicative,
                Function<? super A, ? extends App<F, B>> f,
                App<ListK.Mu, A> value) {
            Validation.function().validateTraverse(applicative, f, value);
            App<F, FList<B>> result = applicative.of(new FList.Nil<>());
            for (A element : ListK.narrow(value)) {
                App<F, B> mapped = Validation.function().requireNonNullResult(f.apply(element), "f", TRAVERSE);
                result = applicative.map2(result, mapped, FList::cons);
            }
            return applicative.map(values -> ListK.of(values.toList()), result);
        }
    }

    /**
     * Represents an immutable sequence that supports prefix construction.
     *
     * @param <A> the element type
     */
    sealed interface FList<A> permits FList.Nil, FList.Cons {
        /**
         * Represents the empty sequence.
         *
         * @param <A> the element type
         */
        record Nil<A>() implements FList<A> {
        }

        /**
         * Represents a non-empty sequence with a head and tail.
         *
         * @param <A> the element type
         * @param head the first element
         * @param tail the remaining elements
         */
        record Cons<A>(A head, FList<A> tail) implements FList<A> {
            /**
             * Creates a non-empty sequence.
             *
             * @param head the first element
             * @param tail the remaining elements
             */
            public Cons {
                Objects.requireNonNull(head, "head");
                Objects.requireNonNull(tail, "tail");
            }
        }

        /**
         * Returns a sequence with an element added at the beginning.
         *
         * @param value the element to prepend
         * @return the extended sequence
         */
        default FList<A> cons(A value) {
            return new Cons<>(Objects.requireNonNull(value, "value"), this);
        }

        /**
         * Returns the elements in logical encounter order.
         *
         * @return a list of the represented elements
         */
        default List<A> toList() {
            ArrayList<A> result = new ArrayList<>();
            FList<A> current = this;
            while (current instanceof Cons<A>(A head, FList<A> tail)) {
                result.add(head);
                current = tail;
            }
            Collections.reverse(result);
            return result;
        }
    }

    private static void requireElements(List<?> values) {
        for (Object value : values) {
            Objects.requireNonNull(value, "value");
        }
    }
}
