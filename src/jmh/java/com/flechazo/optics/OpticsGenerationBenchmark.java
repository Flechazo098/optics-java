package com.flechazo.optics;

import com.flechazo.hkt.Maybe;
import com.flechazo.optics.generated.ClassFileOptics;
import com.flechazo.optics.generated.OpticsSpec;
import com.flechazo.optics.generated.RecordOptics;
import com.flechazo.optics.generated.SpecOptics;
import com.flechazo.hkt.IdF;
import com.flechazo.optics.util.Traversals;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class OpticsGenerationBenchmark {
    public record Address(String city, int zip) {}

    public record User(String name, Address address) {}

    public record Team(String name, List<User> users, Maybe<User> owner) {}

    public sealed interface Shape permits Circle, Rect {}

    public record Circle(int radius) implements Shape {}

    public record Rect(int width, int height) implements Shape {}

    public interface TeamSpec extends OpticsSpec<Team> {
        Lens<Team, String> name();

        Traversal<Team, User> users();
    }

    private static final Function<User, User> RENAME_USER =
            user -> new User(user.name().toUpperCase(), user.address());

    private Team team;
    private Shape circle;
    private Lens<Team, String> generatedName;
    private Lens<Team, String> lambdaName;
    private Traversal<Team, User> generatedUsers;
    private Traversal<Team, User> composedUsers;
    private Prism<Shape, Circle> generatedCircle;
    private Prism<Shape, Circle> lambdaCircle;
    private SpecOptics.GeneratedSpec<Team> generatedSpec;
    private TeamSpec specImplementation;

    @Setup
    @SuppressWarnings("unchecked")
    public void setup() {
        team =
                new Team(
                        "core",
                        List.of(
                                new User("ada", new Address("London", 12345)),
                                new User("grace", new Address("Paris", 75000)),
                                new User("barbara", new Address("New York", 10001))),
                        Maybe.some(new User("dorothy", new Address("Washington", 20001))));
        circle = new Circle(5);
        generatedName = RecordOptics.recordLens(Team.class, "name");
        lambdaName = Lens.of(Team::name, (source, value) -> new Team(value, source.users(), source.owner()));
        generatedUsers = RecordOptics.recordTraversal(Team.class, "users");
        composedUsers = RecordOptics.<Team, List<User>>recordLens(Team.class, "users").andThen(Traversals.forList());
        generatedCircle = RecordOptics.subtypePrism(Shape.class, Circle.class);
        lambdaCircle = Prism.of(
                source -> source instanceof Circle value ? Maybe.some(value) : Maybe.none(),
                value -> value);
        generatedSpec = SpecOptics.generate(TeamSpec.class, Team.class);
        specImplementation = generatedSpec.implementation(TeamSpec.class);
    }

    @Benchmark
    public String generatedLensGet() {
        return generatedName.get(team);
    }

    @Benchmark
    public String lambdaLensGet() {
        return lambdaName.get(team);
    }

    @Benchmark
    public Team generatedLensSet() {
        return generatedName.set("runtime", team);
    }

    @Benchmark
    public Team lambdaLensSet() {
        return lambdaName.set("runtime", team);
    }

    @Benchmark
    public Team generatedTraversalModify() {
        return generatedUsers.modify(RENAME_USER, team);
    }

    @Benchmark
    public Team composedTraversalModify() {
        return composedUsers.modify(RENAME_USER, team);
    }

    @Benchmark
    public List<User> generatedTraversalGetAll() {
        return generatedUsers.getAll(team);
    }

    @Benchmark
    public List<User> composedTraversalGetAll() {
        return composedUsers.getAll(team);
    }

    @Benchmark
    public Maybe<User> generatedTraversalPreview() {
        return generatedUsers.preview(team);
    }

    @Benchmark
    public Maybe<User> composedTraversalPreview() {
        return composedUsers.preview(team);
    }

    @Benchmark
    public int generatedTraversalLength() {
        return generatedUsers.length(team);
    }

    @Benchmark
    public int composedTraversalLength() {
        return composedUsers.length(team);
    }

    @Benchmark
    public Object generatedTraversalModifyFMaybe() {
        return generatedUsers.modifyF(user -> Maybe.some(RENAME_USER.apply(user)), team, Maybe.applicative());
    }

    @Benchmark
    public Object composedTraversalModifyFMaybe() {
        return composedUsers.modifyF(user -> Maybe.some(RENAME_USER.apply(user)), team, Maybe.applicative());
    }

    @Benchmark
    public Object generatedTraversalModifyFIdF() {
        return generatedUsers.modifyF(user -> new IdF<>(RENAME_USER.apply(user)), team, IdF.applicative());
    }

    @Benchmark
    public Object composedTraversalModifyFIdF() {
        return composedUsers.modifyF(user -> new IdF<>(RENAME_USER.apply(user)), team, IdF.applicative());
    }

    @Benchmark
    public Maybe<Circle> generatedPrismPreview() {
        return generatedCircle.getMaybe(circle);
    }

    @Benchmark
    public Maybe<Circle> lambdaPrismPreview() {
        return lambdaCircle.getMaybe(circle);
    }

    @Benchmark
    public Lens<Team, String> specImplementationMethod() {
        return specImplementation.name();
    }

    @Benchmark
    public Lens<Team, String> specMapLookup() {
        return generatedSpec.lens("name");
    }

    @Benchmark
    public Traversal<Team, User> specTraversalMethod() {
        return specImplementation.users();
    }

    @Benchmark
    public Lens<Team, String> recordLensCachedLookup() {
        return RecordOptics.recordLens(Team.class, "name");
    }

    @Benchmark
    public Traversal<Team, User> recordTraversalCachedLookup() {
        return RecordOptics.recordTraversal(Team.class, "users");
    }

    @Benchmark
    public Map<String, Traversal<Team, ?>> classFileTraversalsFacade() {
        return ClassFileOptics.traversals(Team.class);
    }

    @Benchmark
    public Map<String, com.flechazo.optics.focus.FocusPath<Team, ?>> classFileFocusFacade() {
        return ClassFileOptics.focus(Team.class);
    }

    @Benchmark
    public SpecOptics.GeneratedSpec<Team> specGenerate() {
        return SpecOptics.generate(TeamSpec.class, Team.class);
    }
}
