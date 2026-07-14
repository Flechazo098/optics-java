package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.*;
import com.flechazo.hkt.business.control.ListK;
import com.flechazo.hkt.tuple.Tuple2;
import com.flechazo.hkt.util.validation.Validation;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.flechazo.hkt.util.validation.Operation.*;

public interface StreamK<A> extends App<StreamK.Mu, A> {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    static <A> StreamK<A> of(Stream<A> stream) {
        return new Holder<>(stream);
    }

    static <A> StreamK<A> fromStream(Stream<A> stream) {
        return of(stream);
    }

    @SafeVarargs
    static <A> StreamK<A> of(A... values) {
        Objects.requireNonNull(values, "values");
        return of(Stream.of(values).map(value -> Objects.requireNonNull(value, "value")));
    }

    static <A> StreamK<A> pure(A value) {
        return of(Stream.of(Objects.requireNonNull(value, "value")));
    }

    static <A> StreamK<A> empty() {
        return of(Stream.empty());
    }

    static <A> StreamK<A> fromIterable(Iterable<? extends A> values) {
        Objects.requireNonNull(values, "values");
        return of(StreamSupport.stream(values.spliterator(), false).map(value -> Objects.requireNonNull(value, "value")));
    }

    static <A> StreamK<A> fromSupplier(Supplier<? extends Stream<A>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return of(Stream.of(Unit.INSTANCE).flatMap(ignored ->
                Objects.requireNonNull(supplier.get(), "stream")));
    }

    static <A> StreamK<A> defer(Supplier<? extends Stream<A>> supplier) {
        return fromSupplier(supplier);
    }

    static <A> StreamK<A> iterate(A seed, UnaryOperator<A> mapper) {
        Objects.requireNonNull(seed, "seed");
        Objects.requireNonNull(mapper, "mapper");
        return of(Stream.iterate(seed, value -> Objects.requireNonNull(mapper.apply(value), "iterate value")));
    }

    static <A> StreamK<A> generate(Supplier<? extends A> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return of(Stream.generate(() -> Objects.requireNonNull(supplier.get(), "generated value")));
    }

    static StreamK<Integer> range(int startInclusive, int endExclusive) {
        return of(Stream.iterate(startInclusive, value -> value < endExclusive, value -> value + 1));
    }

    static StreamK<Integer> rangeClosed(int startInclusive, int endInclusive) {
        return of(Stream.iterate(startInclusive, value -> value <= endInclusive, value -> value + 1));
    }

    static <A> Stream<A> narrow(App<Mu, A> value) {
        return (Stream<A>) Validation.kind().narrowHolder(value, Stream.class, Holder.class, Holder::stream);
    }

    static <A> StreamK<A> unbox(App<Mu, A> value) {
        return (StreamK<A>) Validation.kind().narrowWithTypeCheck(value, StreamK.class);
    }

    static Instance instance() {
        return Instance.INSTANCE;
    }

    static Functor<StreamK.Mu, Instance.Mu> functor() {
        return Instance.INSTANCE;
    }

    static Applicative<StreamK.Mu, Instance.Mu> applicative() {
        return Instance.INSTANCE;
    }

    static Monad<StreamK.Mu, Instance.Mu> monad() {
        return Instance.INSTANCE;
    }

    static MonadZero<StreamK.Mu, Instance.Mu> monadZero() {
        return Instance.INSTANCE;
    }

    static Selective<StreamK.Mu, Instance.Mu> selective() {
        return Instance.INSTANCE;
    }

    static Foldable<StreamK.Mu> foldable() {
        return Instance.INSTANCE;
    }

    static Traversable<StreamK.Mu, Instance.Mu> traversable() {
        return Instance.INSTANCE;
    }

    Stream<A> stream();

    default Stream<A> toStream() {
        return stream();
    }

    default List<A> toList() {
        return stream().map(value -> Objects.requireNonNull(value, "value")).toList();
    }

    default Set<A> toSet() {
        LinkedHashSet<A> result = stream()
                .map(value -> Objects.requireNonNull(value, "value"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(result);
    }

    default <B> StreamK<B> map(Function<? super A, ? extends B> mapper) {
        Validation.function().require(mapper, "mapper", MAP);
        return of(stream().map(value ->
                Validation.function().requireNonNullResult(mapper.apply(value), "mapper", MAP)));
    }

    default <B> StreamK<B> flatMap(Function<? super A, ? extends StreamK<B>> mapper) {
        Validation.function().require(mapper, "mapper", FLAT_MAP);
        return of(stream().flatMap(value ->
                Validation.function().requireNonNullResult(mapper.apply(value), "mapper", FLAT_MAP).stream()));
    }

    default StreamK<A> filter(Predicate<? super A> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return of(stream().filter(predicate));
    }

    default StreamK<A> take(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        return of(stream().limit(n));
    }

    default StreamK<A> drop(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        return of(stream().skip(n));
    }

    default StreamK<A> concat(StreamK<? extends A> other) {
        Objects.requireNonNull(other, "other");
        return of(Stream.concat(stream(), other.stream()));
    }

    default StreamK<A> append(A value) {
        return concat(StreamK.pure(value));
    }

    default StreamK<A> prepend(A value) {
        return StreamK.pure(value).concat(this);
    }

    default <B, C> StreamK<C> zipWith(StreamK<B> other, BiFunction<? super A, ? super B, ? extends C> zipper) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(zipper, "zipper");
        Iterator<A> left = stream().iterator();
        Iterator<B> right = other.stream().iterator();
        Iterator<C> zipped = new Iterator<>() {
            @Override
            public boolean hasNext() {
                return left.hasNext() && right.hasNext();
            }

            @Override
            public C next() {
                return Objects.requireNonNull(zipper.apply(left.next(), right.next()), "zipped value");
            }
        };
        return of(StreamSupport.stream(Spliterators.spliteratorUnknownSize(zipped, Spliterator.ORDERED), false));
    }

    default StreamK<Tuple2<Integer, A>> zipWithIndex() {
        Iterator<A> iterator = stream().iterator();
        Iterator<Tuple2<Integer, A>> indexed = new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Tuple2<Integer, A> next() {
                return Tuple2.of(index++, iterator.next());
            }
        };
        return of(StreamSupport.stream(Spliterators.spliteratorUnknownSize(indexed, Spliterator.ORDERED), false));
    }

    default StreamK<A> peek(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action");
        return of(stream().peek(action));
    }

    default void forEach(Consumer<? super A> action) {
        Objects.requireNonNull(action, "action");
        stream().forEach(action);
    }

    default VStream<A> toVStream() {
        return VStream.fromStream(stream());
    }

    default StreamPath<A> toStreamPath() {
        return new StreamPath<>(stream());
    }

    record Holder<A>(Stream<A> stream) implements StreamK<A> {
        public Holder {
            Objects.requireNonNull(stream, "stream");
        }
    }

    enum Instance implements MonadZero<StreamK.Mu, Instance.Mu>,
            Selective<StreamK.Mu, Instance.Mu>,
            Traversable<StreamK.Mu, Instance.Mu> {
        INSTANCE;

        public static final class Mu implements MonadZero.Mu, Traversable.Mu {
            private Mu() {
            }
        }

        /**
         * Creates a singleton encoded stream.
         *
         * @param <A> the element type
         * @param value the sole element
         * @return the singleton stream in encoded form
         */
        @Override
        public <A> App<StreamK.Mu, A> of(A value) {
            return StreamK.pure(value);
        }

        /**
         * Transforms encoded stream elements lazily in encounter order.
         *
         * @param <A> the source element type
         * @param <B> the result element type
         * @param f the element transformation
         * @param fa the source stream
         * @return the transformed stream in encoded form
         */
        @Override
        public <A, B> App<StreamK.Mu, B> map(Function<? super A, ? extends B> f, App<StreamK.Mu, A> fa) {
            Validation.function().validateMap(f, fa);
            return StreamK.unbox(fa).map(f);
        }

        /**
         * Applies encoded stream functions to encoded stream values.
         *
         * @param <A> the argument type
         * @param <B> the result type
         * @param ff the stream of functions
         * @param fa the stream of arguments
         * @return the application results in encoded stream form
         */
        @Override
        public <A, B> App<StreamK.Mu, B> ap(
                App<StreamK.Mu, ? extends Function<A, B>> ff,
                App<StreamK.Mu, A> fa) {
            List<A> values = StreamK.narrow(fa)
                    .map(value -> Objects.requireNonNull(value, "ap value"))
                    .toList();
            return StreamK.of(StreamK.narrow(ff).flatMap(function -> {
                Function<A, B> apply = Objects.requireNonNull(function, "ap function");
                return values.stream().map(value -> Objects.requireNonNull(apply.apply(value), "ap result"));
            }));
        }

        /**
         * Maps each stream element to an encoded stream and concatenates the results.
         *
         * @param <A> the source element type
         * @param <B> the result element type
         * @param f the stream-producing transformation
         * @param fa the source stream
         * @return the concatenated results in encoded stream form
         */
        @Override
        public <A, B> App<StreamK.Mu, B> flatMap(
                Function<? super A, ? extends App<StreamK.Mu, B>> f,
                App<StreamK.Mu, A> fa) {
            Validation.function().validateFlatMap(f, fa);
            return StreamK.of(StreamK.narrow(fa).flatMap(value ->
                    StreamK.narrow(Validation.function().requireNonNullResult(f.apply(value), "f", FLAT_MAP))));
        }

        /**
         * Returns the empty encoded stream.
         *
         * @param <A> the element type
         * @return the empty stream in encoded form
         */
        @Override
        public <A> App<StreamK.Mu, A> zero() {
            return StreamK.empty();
        }

        /**
         * Concatenates two encoded stream alternatives.
         *
         * @param <A> the element type
         * @param first the first stream
         * @param second the deferred second stream
         * @return the concatenated stream in encoded form
         */
        @Override
        public <A> App<StreamK.Mu, A> orElse(
                App<StreamK.Mu, A> first,
                Supplier<? extends App<StreamK.Mu, A>> second) {
            Objects.requireNonNull(second, "second");
            Stream<A> lazySecond = Stream.of(Unit.INSTANCE).flatMap(ignored ->
                    StreamK.narrow(Objects.requireNonNull(second.get(), "second result")));
            return StreamK.of(Stream.concat(StreamK.narrow(first), lazySecond));
        }

        /**
         * Concatenates encoded stream alternatives in encounter order.
         *
         * @param <A> the element type
         * @param alternatives the streams to concatenate
         * @return the concatenated stream in encoded form
         */
        @Override
        public <A> App<StreamK.Mu, A> orElseAll(Iterable<? extends App<StreamK.Mu, A>> alternatives) {
            Objects.requireNonNull(alternatives, "alternatives");
            return StreamK.of(StreamSupport.stream(alternatives.spliterator(), false)
                    .flatMap(alternative -> StreamK.narrow(Objects.requireNonNull(alternative, "alternative"))));
        }

        /**
         * Retains encoded stream elements satisfying a predicate.
         *
         * @param <A> the element type
         * @param predicate the condition for retaining an element
         * @param value the source stream
         * @return the matching elements in encoded stream form
         */
        @Override
        public <A> App<StreamK.Mu, A> filter(Predicate<? super A> predicate, App<StreamK.Mu, A> value) {
            Objects.requireNonNull(predicate, "predicate");
            return StreamK.unbox(value).filter(predicate);
        }

        /**
         * Resolves encoded either elements using encoded functions for left values.
         *
         * @param <A> the function argument type
         * @param <B> the result type
         * @param value the stream of branch values
         * @param function the stream of functions for left values
         * @return the selected results in encoded stream form
         */
        @Override
        public <A, B> App<StreamK.Mu, B> select(
                App<StreamK.Mu, Either<A, B>> value,
                App<StreamK.Mu, ? extends Function<A, B>> function) {
            List<? extends Function<A, B>> functions = StreamK.narrow(function)
                    .map(fn -> Objects.requireNonNull(fn, "select function"))
                    .toList();
            return StreamK.of(StreamK.narrow(value).flatMap(choice -> {
                Either<A, B> either = Validation.coreType().requireValue(choice, "select value", StreamK.class, SELECT);
                if (either.isRight()) {
                    return Stream.of(either.right());
                }
                return functions.stream().map(fn -> Objects.requireNonNull(fn.apply(either.left()), "select result"));
            }));
        }

        /**
         * Selects a deferred stream branch for encoded conditions.
         *
         * @param <A> the result type
         * @param condition the stream of conditions
         * @param thenValue the deferred stream for true conditions
         * @param elseValue the deferred stream for false conditions
         * @return the selected elements in encoded stream form
         */
        @Override
        public <A> App<StreamK.Mu, A> ifS(
                App<StreamK.Mu, Boolean> condition,
                Supplier<? extends App<StreamK.Mu, A>> thenValue,
                Supplier<? extends App<StreamK.Mu, A>> elseValue) {
            Objects.requireNonNull(thenValue, "thenValue");
            Objects.requireNonNull(elseValue, "elseValue");
            return StreamK.of(StreamK.narrow(condition).flatMap(test ->
                    StreamK.narrow(Boolean.TRUE.equals(test)
                            ? Validation.function().requireNonNullResult(thenValue.get(), "thenValue", IF_S)
                            : Validation.function().requireNonNullResult(elseValue.get(), "elseValue", IF_S))));
        }

        /**
         * Maps encoded stream elements to a monoid and combines them in encounter order.
         *
         * @param <A> the element type
         * @param <M> the accumulated value type
         * @param monoid the monoid used to combine mapped values
         * @param f the element mapping function
         * @param value the source stream
         * @return the combined mapped value
         */
        @Override
        public <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<StreamK.Mu, A> value) {
            Validation.function().validateFoldMap(monoid, f, value);
            M result = monoid.empty();
            Iterator<A> iterator = StreamK.narrow(value).iterator();
            while (iterator.hasNext()) {
                result = monoid.combine(result, f.apply(iterator.next()));
            }
            return result;
        }

        /**
         * Applies an applicative transformation to encoded stream elements in encounter order.
         *
         * @param <F> the applicative witness type
         * @param <A> the source element type
         * @param <B> the result element type
         * @param applicative the applicative used to combine effects
         * @param f the effectful element transformation
         * @param value the source stream
         * @return the transformed encoded stream in the applicative context
         */
        @Override
        public <F extends K1, A, B> App<F, App<StreamK.Mu, B>> traverse(
                Applicative<F, ?> applicative,
                Function<? super A, ? extends App<F, B>> f,
                App<StreamK.Mu, A> value) {
            Validation.function().validateTraverse(applicative, f, value);
            App<F, ListK<B>> result = applicative.of(ListK.empty());
            Iterator<A> iterator = StreamK.narrow(value).iterator();
            while (iterator.hasNext()) {
                App<F, B> mapped = Validation.function().requireNonNullResult(f.apply(iterator.next()), "f", TRAVERSE);
                result = applicative.map2(result, mapped, ListK::append);
            }
            return applicative.map(list -> StreamK.fromIterable(list.toList()), result);
        }
    }
}
