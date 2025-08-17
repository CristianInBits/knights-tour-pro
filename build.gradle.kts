plugins {
    java
    application
    id("me.champeau.jmh") version "0.7.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "knights"
version = "1.0.0"

java {
    // Use the same Java version everywhere (IDE/CI/Gradle)
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

repositories {
    mavenCentral()
}

dependencies {
    // App
    implementation("com.google.code.gson:gson:2.10.1")

    // Tests (JUnit 5 BOM keeps versions aligned)
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // JMH (benchmarks in src/jmh/java/**)
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

application {
    // Must match your Main class
    mainClass.set("knights.Main")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    // Plain JAR (no dependencies). Use shadowJar for a fat/uber JAR.
    manifest { attributes["Main-Class"] = "knights.Main" }
}

tasks.shadowJar {
    // Create an all-in-one JAR: build/libs/<name>-all.jar
    archiveClassifier.set("all")
}

jmh {
    // Reasonable defaults; override with -P properties if needed
    warmupIterations.set(3)
    iterations.set(5)
    fork.set(1)
    benchmarkMode.set(listOf("Throughput"))
    timeUnit.set("ms")

    // Allow selecting benchmarks via: -PjmhInclude='.*YourBenchmark.*'
    val includeProp = project.findProperty("jmhInclude") as String?
    if (!includeProp.isNullOrBlank()) {
        includes.set(listOf(includeProp))
    }
    // Optional knobs: -PjmhFork=2 -PjmhIterations=10 -PjmhWarmup=5
    project.findProperty("jmhFork")?.toString()?.toIntOrNull()?.let { fork.set(it) }
    project.findProperty("jmhIterations")?.toString()?.toIntOrNull()?.let { iterations.set(it) }
    project.findProperty("jmhWarmup")?.toString()?.toIntOrNull()?.let { warmupIterations.set(it) }
}
