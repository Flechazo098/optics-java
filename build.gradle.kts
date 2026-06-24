plugins {
    id("java")
    id("maven-publish")
}

group = "com.flechazo"
version = "1.0-SNAPSHOT"

val isSnapshot = version.toString().endsWith("SNAPSHOT")
val publishUrl = if (isSnapshot) {
    "https://maven.sighs.cc/repository/maven-snapshots/"
} else {
    "https://maven.sighs.cc/repository/maven-releases/"
}

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
}

val jmh = sourceSets.create("jmh") {
    java.srcDir("src/jmh/java")
    compileClasspath += sourceSets.main.get().output
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("org.jspecify:jspecify:1.0.0")
    implementation("io.smallrye.classfile:jdk-classfile-backport:26")
    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("com.google.guava:guava:33.6.0-jre")
    "jmhImplementation"(files("F:/code/mcmod/source/DataFixerUpper/build/libs/datafixerupper-9.1.0-SNAPSHOT.jar"))
    "jmhImplementation"("com.google.code.gson:gson:2.10.1")
    "jmhImplementation"("org.slf4j:slf4j-api:2.0.9")
    "jmhImplementation"("org.openjdk.jmh:jmh-core:1.37")
    "jmhAnnotationProcessor"("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("jmh") {
    group = "verification"
    description = "Runs short JMH benchmarks for generated optics hot paths."
    classpath = jmh.runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    args(
        "-wi", "2",
        "-i", "3",
        "-w", "200ms",
        "-r", "200ms",
        "-f", "1",
        "com.flechazo.optics.*Benchmark"
    )
}

fun TaskContainer.registerJmhRun(
    name: String,
    descriptionText: String,
    include: String,
    extraArgs: List<String>
) {
    register<JavaExec>(name) {
        group = "verification"
        description = descriptionText
        classpath = jmh.runtimeClasspath
        mainClass.set("org.openjdk.jmh.Main")
        args(
            include,
            "-wi", "3",
            "-i", "5",
            "-w", "1s",
            "-r", "1s",
            "-f", "1",
            "-rf", "json",
            "-rff", "build/reports/$name.json",
            *extraArgs.toTypedArray()
        )
    }
}

tasks.registerJmhRun(
    "jmhDfuRewriteGc",
    "Runs DFU/local rewrite comparison benchmarks with JMH GC allocation profiling.",
    "com.flechazo.optics.DfuCoreComparisonBenchmark.*Rewrite",
    listOf("-prof", "gc")
)

tasks.registerJmhRun(
    "jmhDfuRewriteStack",
    "Runs local rewrite benchmarks with JMH stack profiling.",
    "com.flechazo.optics.DfuCoreComparisonBenchmark.local.*Rewrite",
    listOf("-prof", "stack")
)

tasks.registerJmhRun(
    "jmhDfuRewriteCpuFlamegraph",
    "Runs local rewrite benchmarks with JDK Flight Recorder profiling.",
    "com.flechazo.optics.DfuCoreComparisonBenchmark.local.*Rewrite",
    listOf("-prof", "jfr:dir=build/reports/jmh;configName=profile")
)

tasks.registerJmhRun(
    "jmhDfuRewriteAllocFlamegraph",
    "Runs local rewrite benchmarks with JDK Flight Recorder profiling.",
    "com.flechazo.optics.DfuCoreComparisonBenchmark.local.*Rewrite",
    listOf("-prof", "jfr:dir=build/reports/jmh;configName=profile")
)

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("optics-java")
                description.set("A simplified Java optics library.")
                url.set("https://github.com/Flechazo098/optics-java")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "sighs"
            url = uri(publishUrl)
            credentials {
                username = findProperty("mavenUsername") as String? ?: System.getenv("SIGHS_PUBLISH_USER")
                password = findProperty("mavenPassword") as String? ?: System.getenv("SIGHS_PUBLISH_PASSWORD")
            }
        }
    }
}
