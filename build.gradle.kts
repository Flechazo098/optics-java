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
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
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
    testImplementation(files("F:/code/mcmod/source/DataFixerUpper/build/libs/datafixerupper-9.1.0-SNAPSHOT.jar"))
    testImplementation("com.google.code.gson:gson:2.10.1")
    testImplementation("org.slf4j:slf4j-api:2.0.9")
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

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.forkOptions.jvmArgs?.add("-Dfile.encoding=UTF-8")
}

//tasks.register<JavaExec>("runImplicitClassDemo") {
//    group = "application"
//    description = "Runs the implicit typeclass demo. Usage: -PrunType=中文版|英文版"
//    dependsOn(tasks.named("compileJava"))
//    classpath = sourceSets.main.get().runtimeClasspath
//    val runType = project.findProperty("runType") as? String ?: "cn"
//    mainClass.set(if (runType == "en")
//        "com.flechazo.hkt.ImplicitShow"
//    else
//        "com.flechazo.hkt.整活.隐式展示")
//    jvmArgs(
//        "-Dfile.encoding=UTF-8",
//        "-Dsun.stdout.encoding=UTF-8",
//        "-Dsun.stderr.encoding=UTF-8"
//    )
//}

tasks.register<JavaExec>("jmh") {
    group = "verification"
    description = "Runs JMH benchmarks using the benchmark classes' rigorous annotations."
    classpath = jmh.runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    args(
        "-rf", "json",
        "-rff", "build/reports/jmh.json",
        "com.flechazo.optics.*Benchmark"
    )
}

tasks.register<JavaExec>("jmhQuick") {
    group = "verification"
    description = "Runs a short smoke-test JMH pass. Do not use this for comparisons."
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
    extraArgs: List<String>,
    useAnnotatedTiming: Boolean = false
) {
    register<JavaExec>(name) {
        group = "verification"
        description = descriptionText
        classpath = jmh.runtimeClasspath
        mainClass.set("org.openjdk.jmh.Main")
        val benchmarkArgs = mutableListOf(include)
        if (!useAnnotatedTiming) {
            benchmarkArgs.addAll(listOf(
                "-wi", "3",
                "-i", "5",
                "-w", "1s",
                "-r", "1s",
                "-f", "1"
            ))
        }
        benchmarkArgs.addAll(listOf(
            "-rf", "json",
            "-rff", "build/reports/$name.json"
        ))
        benchmarkArgs.addAll(extraArgs)
        args(benchmarkArgs)
    }
}

tasks.registerJmhRun(
    "jmhStrictOptimizerScenarios",
    "Runs optimizer rewrite scenarios with annotation-defined rigorous timing.",
    "com.flechazo.optics.OptimizerScenarioBenchmark|com.flechazo.optics.LocalOptimizerScenarioBenchmark",
    emptyList(),
    useAnnotatedTiming = true
)

tasks.registerJmhRun(
    "jmhStrictMinecraftMigration",
    "Runs Minecraft-scale migration benchmarks with annotation-defined rigorous timing.",
    "com.flechazo.optics.MinecraftMigrationScaleBenchmark",
    emptyList(),
    useAnnotatedTiming = true
)

tasks.registerJmhRun(
    "jmhMinecraftMigrationGc",
    "Runs Minecraft-scale DFU/local migration benchmarks with JMH GC allocation profiling.",
    "com.flechazo.optics.MinecraftMigrationScaleBenchmark.*Migration",
    listOf("-prof", "gc")
)

tasks.registerJmhRun(
    "jmhMinecraftMigrationStack",
    "Runs Minecraft-scale local migration benchmarks with JMH stack profiling.",
    "com.flechazo.optics.MinecraftMigrationScaleBenchmark.local.*Migration",
    listOf("-prof", "stack")
)

tasks.registerJmhRun(
    "jmhMinecraftMigrationCpuFlamegraph",
    "Runs Minecraft-scale local migration benchmarks with JDK Flight Recorder profiling.",
    "com.flechazo.optics.MinecraftMigrationScaleBenchmark.local.*Migration",
    listOf("-prof", "jfr:dir=build/reports/jmh;configName=profile")
)

tasks.registerJmhRun(
    "jmhMinecraftMigrationAllocFlamegraph",
    "Runs Minecraft-scale local migration benchmarks with JDK Flight Recorder profiling.",
    "com.flechazo.optics.MinecraftMigrationScaleBenchmark.local.*Migration",
    listOf("-prof", "jfr:dir=build/reports/jmh;configName=profile")
)

tasks.registerJmhRun(
    "jmhFullProfile",
    "Runs ALL benchmarks with JFR profiling.",
    "com.flechazo.optics.*Benchmark",
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
