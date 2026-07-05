package com.flechazo.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flechazo.hkt.App;
import com.flechazo.hkt.App2;
import com.flechazo.hkt.Either;
import com.flechazo.hkt.IdF;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Monad;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Natural;
import com.flechazo.hkt.Selective;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.Tuple2;
import com.flechazo.hkt.Validated;
import com.flechazo.hkt.type.Types;
import com.flechazo.optics.util.EitherTraversals;
import com.flechazo.optics.util.TryTraversals;
import com.flechazo.optics.util.TupleTraversals;
import com.flechazo.optics.util.ValidatedTraversals;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CoreTraversalsAndHktTest {

  @Test
  void widerCoreTraversalsCoverEitherTryValidatedAndTuple() {
    assertEquals(Either.right(11), EitherTraversals.<String, Integer>right().modify(value -> value + 1, Either.right(10)));
    assertEquals(Try.success(11), TryTraversals.<Integer>success().modify(value -> value + 1, Try.success(10)));
    assertEquals(
        Validated.valid(11),
        ValidatedTraversals.<String, Integer>valid().modify(value -> value + 1, Validated.valid(10)));
    assertEquals(Tuple2.of("a", 11), TupleTraversals.<String, Integer>second().modify(value -> value + 1, Tuple2.of("a", 10)));
  }

  @Test
  void hktTypeclassesHaveConcreteInstancesAndTransformations() {
    Monad<Maybe.Mu, ?> maybeMonad = Maybe.monad();
    Selective<Maybe.Mu, ?> maybeSelective = Maybe.selective();
    App<Maybe.Mu, Integer> maybe =
        maybeMonad.flatMap(value -> Maybe.some(value + 1), Maybe.some(1));

    assertEquals(Maybe.some(2), Maybe.unbox(maybe));
    assertEquals(
        Maybe.some("yes"),
        Maybe.unbox(maybeSelective.ifS(Maybe.some(true), () -> Maybe.some("yes"), () -> Maybe.some("no"))));
    assertEquals(
        Maybe.some("else"),
        Maybe.unbox(maybeSelective.ifS(
            Maybe.some(false),
            () -> {
              throw new AssertionError("then branch should not be evaluated");
            },
            () -> Maybe.some("else"))));
    assertEquals(
        Maybe.some("ready"),
        Maybe.unbox(maybeSelective.select(Maybe.some(Either.right("ready")), Maybe.none())));
    assertThrows(NullPointerException.class, () -> Maybe.some(null));
    assertThrows(NullPointerException.class, () -> maybeMonad.flatMap(value -> null, Maybe.some(1)));

    Monad<Either.RightMu<String>, ?> eitherMonad = Either.monad();
    Selective<Either.RightMu<String>, ?> eitherSelective = Either.selective();
    App<Either.RightMu<String>, Integer> right =
        eitherMonad.flatMap(value -> Either.right(value + 1), Either.right(1));
    App<Either.RightMu<String>, Integer> left =
        eitherMonad.flatMap(value -> Either.right(value + 1), Either.<String, Integer>left("bad"));
    App2<Either.Mu, String, Integer> right2 = Either.right(3);

    assertEquals(Either.right(2), Either.unbox(right));
    assertEquals(Either.left("bad"), Either.unbox(left));
    assertEquals(Either.right(3), Either.unbox(right2));
    assertEquals(
        Either.right("else"),
        Either.unbox(eitherSelective.ifS(
            Either.right(false),
            () -> Either.left("then"),
            () -> Either.right("else"))));

    var validatedApplicative = Validated.<String>selective((firstError, secondError) -> firstError + "+" + secondError);
    App<Validated.RightMu<String>, Integer> invalid =
        validatedApplicative.map2(
            Validated.invalid("name"),
            Validated.invalid("age"),
            Integer::sum);

    assertEquals(Validated.invalid("name+age"), Validated.unbox(invalid));
    assertEquals(
        Validated.invalid("selector+function"),
        Validated.unbox(validatedApplicative.select(
            Validated.invalid("selector"),
            Validated.invalid("function"))));
    assertThrows(NullPointerException.class, () -> Validated.invalid(null));
    assertThrows(NullPointerException.class, () -> Monoid.of(null, String::concat));

    Monoid<String> log = Monoid.of("", String::concat);
    Monad<Tuple2.WriterMu<String>, ?> writer = Tuple2.monad(log);
    App<Tuple2.WriterMu<String>, Integer> written =
        writer.flatMap(value -> Tuple2.of("b", value + 1), Tuple2.of("a", 1));

    assertEquals(Tuple2.of("ab", 2), Tuple2.unbox(written));
    assertThrows(
        NullPointerException.class,
        () -> writer.flatMap(value -> Tuple2.of("b", value + 1), Tuple2.of(null, 1)));

    Monad<Try.Mu, ?> tryMonad = Try.monad();
    Selective<Try.Mu, ?> trySelective = Try.selective();
    App<Try.Mu, Integer> success =
        tryMonad.flatMap(value -> Try.success(value + 1), Try.success(1));
    App<Try.Mu, Integer> failure =
        tryMonad.flatMap(
            value -> {
              throw new IllegalStateException("boom");
            },
            Try.success(1));

    assertEquals(Try.success(2), Try.unbox(success));
    assertTrue(Try.unbox(failure).isFailure());
    assertEquals(
        Try.success("else"),
        Try.unbox(trySelective.ifS(
            Try.success(false),
            () -> Try.failure(new IllegalStateException("then")),
            () -> Try.success("else"))));
    Try<Object> fatal =
        Try.of(
            () -> {
              throw new AssertionError("fatal");
            });
    assertTrue(fatal.isFailure());
    assertTrue(fatal.cause() instanceof AssertionError);
    assertTrue(Try.of(
            () -> {
              throw new Exception("checked");
            })
        .isFailure());

    Natural<Maybe.Mu, Try.Mu> maybeToTry =
        new Natural<>() {
          @Override
          public <A> App<Try.Mu, A> apply(App<Maybe.Mu, A> value) {
            Maybe<A> maybeValue = Maybe.unbox(value);
            return maybeValue.isDefined()
                ? Try.success(maybeValue.get())
                : Try.failure(new IllegalStateException("none"));
          }
        };
    Natural<Try.Mu, IdF.Mu> tryToId =
        new Natural<>() {
          @Override
          public <A> App<IdF.Mu, A> apply(App<Try.Mu, A> value) {
            return new IdF<>(Try.unbox(value).get());
          }
        };

    assertEquals(3, IdF.get(maybeToTry.andThen(tryToId).apply(Maybe.some(3))));
    assertEquals(Maybe.some(4), Maybe.unbox(Natural.<Maybe.Mu>identity().apply(Maybe.some(4))));
  }

  @Test
  void validatedTraversalPreservesApplicativeAccumulationSemantics() {
    var errors = Validated.<String>applicative((left, right) -> left + "+" + right);
    Traversal<Validated<String, Integer>, Validated<String, Integer>, Integer, Integer> valid =
        ValidatedTraversals.valid(Types.witness(String.class), Types.witness(Integer.class));
    AtomicInteger effects = new AtomicInteger();

    App<Validated.RightMu<String>, Validated<String, Integer>> first =
        valid.modifyF(
            value -> {
              effects.incrementAndGet();
              return Validated.invalid("first");
            },
            Validated.valid(1),
            errors);
    App<Validated.RightMu<String>, Validated<String, Integer>> second =
        valid.modifyF(
            value -> {
              effects.incrementAndGet();
              return Validated.invalid("second");
            },
            Validated.valid(2),
            errors);
    App<Validated.RightMu<String>, Integer> combined =
        errors.map2(first, second, (left, right) -> left.value() + right.value());

    assertEquals(Validated.invalid("first+second"), Validated.unbox(combined));
    assertEquals(2, effects.get());

    App<Validated.RightMu<String>, Validated<String, Integer>> skipped =
        valid.modifyF(
            value -> {
              effects.incrementAndGet();
              return Validated.invalid("should-not-run");
            },
            Validated.invalid("source"),
            errors);

    assertEquals(Validated.valid(Validated.invalid("source")), Validated.unbox(skipped));
    assertEquals(2, effects.get());
  }
}
