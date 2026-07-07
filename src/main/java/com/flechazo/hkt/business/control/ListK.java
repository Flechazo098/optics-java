package com.flechazo.hkt.business.control;

import com.flechazo.hkt.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface ListK<A> extends App<ListK.Mu, A> {
    final class Mu implements K1 {
        private Mu() {
        }
    }

    static <A> ListK<A> of(List<? extends A> values) {
        Objects.requireNonNull(values, "values");
        requireElements(values);
        return new Holder<>((List<A>) values);
    }

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

    static <A> ListK<A> empty() {
        return of(List.of());
    }

    static <A> ListK<A> singleton(A value) {
        return of(List.of(Objects.requireNonNull(value, "value")));
    }

    static <A> List<A> narrow(App<Mu, A> value) {
        if (value instanceof Holder<?>(List<?> values)) {
            return (List<A>) values;
        }
        throw new ClassCastException("Not a ListK value: " + value);
    }

    static <A> ListK<A> unbox(App<Mu, A> value) {
        if (value instanceof ListK<?> list) {
            return (ListK<A>) list;
        }
        throw new ClassCastException("Not a ListK value: " + value);
    }

    static Instance instance() {
        return Instance.INSTANCE;
    }

    static Functor<ListK.Mu, Instance.Mu> functor() {
        return Instance.INSTANCE;
    }

    static Applicative<ListK.Mu, Instance.Mu> applicative() {
        return Instance.INSTANCE;
    }

    static Monad<ListK.Mu, Instance.Mu> monad() {
        return Instance.INSTANCE;
    }

    static MonadZero<ListK.Mu, Instance.Mu> monadZero() {
        return Instance.INSTANCE;
    }

    static Selective<ListK.Mu, Instance.Mu> selective() {
        return Instance.INSTANCE;
    }

    static Traversable<ListK.Mu, Instance.Mu> traversable() {
        return Instance.INSTANCE;
    }

    default List<A> toList() {
        return narrow(this);
    }

    record Holder<A>(List<A> values) implements ListK<A> {
        public Holder {
            Objects.requireNonNull(values, "values");
            requireElements(values);
        }
    }

    enum Instance implements MonadZero<ListK.Mu, Instance.Mu>,
            Selective<ListK.Mu, Instance.Mu>,
            Traversable<ListK.Mu, Instance.Mu> {
        INSTANCE;

        public static final class Mu implements MonadZero.Mu, Traversable.Mu {
            private Mu() {
            }
        }

        @Override
        public <A> App<ListK.Mu, A> of(A value) {
            return ListK.singleton(value);
        }

        @Override
        public <A, B> App<ListK.Mu, B> map(Function<? super A, ? extends B> f, App<ListK.Mu, A> fa) {
            Objects.requireNonNull(f, "f");
            List<A> input = ListK.narrow(fa);
            ArrayList<B> result = new ArrayList<>(input.size());
            for (A value : input) {
                result.add(Objects.requireNonNull(f.apply(value), "mapped value"));
            }
            return ListK.of(result);
        }

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

        @Override
        public <A, B> App<ListK.Mu, B> flatMap(
                Function<? super A, ? extends App<ListK.Mu, B>> f,
                App<ListK.Mu, A> fa) {
            Objects.requireNonNull(f, "f");
            ArrayList<B> result = new ArrayList<>();
            for (A value : ListK.narrow(fa)) {
                App<ListK.Mu, B> mapped = Objects.requireNonNull(f.apply(value), "flatMap result");
                for (B mappedValue : ListK.narrow(mapped)) {
                    result.add(Objects.requireNonNull(mappedValue, "flatMap value"));
                }
            }
            return ListK.of(result);
        }

        @Override
        public <A> App<ListK.Mu, A> zero() {
            return ListK.empty();
        }

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

        @Override
        public <A> App<ListK.Mu, A> orElseAll(Iterable<? extends App<ListK.Mu, A>> alternatives) {
            Objects.requireNonNull(alternatives, "alternatives");
            ArrayList<A> result = new ArrayList<>();
            for (App<ListK.Mu, A> alternative : alternatives) {
                result.addAll(ListK.narrow(Objects.requireNonNull(alternative, "alternative")));
            }
            return ListK.of(result);
        }

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

        @Override
        public <A, B> App<ListK.Mu, B> select(
                App<ListK.Mu, Either<A, B>> value,
                App<ListK.Mu, ? extends Function<A, B>> function) {
            List<Either<A, B>> choices = ListK.narrow(value);
            List<? extends Function<A, B>> functions = ListK.narrow(function);
            ArrayList<B> result = new ArrayList<>();
            for (Either<A, B> choice : choices) {
                Either<A, B> either = Objects.requireNonNull(choice, "select value");
                if (either.isRight()) {
                    result.add(either.right());
                } else {
                    for (Function<A, B> fn : functions) {
                        Function<A, B> apply = Objects.requireNonNull(fn, "select function");
                        result.add(Objects.requireNonNull(apply.apply(either.left()), "select result"));
                    }
                }
            }
            return ListK.of(result);
        }

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
                        thenValues = ListK.narrow(Objects.requireNonNull(thenValue.get(), "thenValue result"));
                    }
                    result.addAll(thenValues);
                } else {
                    if (elseValues == null) {
                        elseValues = ListK.narrow(Objects.requireNonNull(elseValue.get(), "elseValue result"));
                    }
                    result.addAll(elseValues);
                }
            }
            return ListK.of(result);
        }

        @Override
        public <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<ListK.Mu, A> value) {
            Objects.requireNonNull(monoid, "monoid");
            Objects.requireNonNull(f, "f");
            M result = monoid.empty();
            for (A element : ListK.narrow(value)) {
                result = monoid.combine(result, f.apply(element));
            }
            return result;
        }

        @Override
        public <F extends K1, A, B> App<F, App<ListK.Mu, B>> traverse(
                Applicative<F, ?> applicative,
                Function<? super A, ? extends App<F, B>> f,
                App<ListK.Mu, A> value) {
            Objects.requireNonNull(applicative, "applicative");
            Objects.requireNonNull(f, "f");
            App<F, FList<B>> result = applicative.of(new FList.Nil<>());
            for (A element : ListK.narrow(value)) {
                App<F, B> mapped = Objects.requireNonNull(f.apply(element), "mapped value");
                result = applicative.map2(result, mapped, FList::cons);
            }
            return applicative.map(values -> ListK.of(values.toList()), result);
        }
    }

    sealed interface FList<A> permits FList.Nil, FList.Cons {
        record Nil<A>() implements FList<A> {
        }

        record Cons<A>(A head, FList<A> tail) implements FList<A> {
            public Cons {
                Objects.requireNonNull(head, "head");
                Objects.requireNonNull(tail, "tail");
            }
        }

        default FList<A> cons(A value) {
            return new Cons<>(Objects.requireNonNull(value, "value"), this);
        }

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
