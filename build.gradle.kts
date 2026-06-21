plugins {
    id("java")
}

group = "com.flechazo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val jmh = sourceSets.create("jmh") {
    java.srcDir("src/jmh/java")
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("org.jspecify:jspecify:1.0.0")
    implementation("io.smallrye.classfile:jdk-classfile-backport:26")
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
        "com.flechazo.optics.OpticsGenerationBenchmark"
    )
}
