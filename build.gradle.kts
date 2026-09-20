import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    java
    application
    id("me.champeau.jmh") version "0.7.3"
    id("com.gradleup.shadow") version "9.6.1"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "knights"
version = "1.0.0"

java {
    // Use the same Java version everywhere (IDE/CI/Gradle)
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

javafx {
    version = "17"
    modules = listOf("javafx.controls", "javafx.graphics")
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
    // Gradle 9 no longer adds the launcher implicitly; the BOM above pins its version.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

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

tasks.register<JavaExec>("runFx") {
    group = "application"
    description = "Run JavaFX UI"

    mainClass.set("knights.ui.MainFX")

    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })

    val runtimeCp = sourceSets["main"].runtimeClasspath
    classpath = runtimeCp

    doFirst {
        jvmArgs(
            "--module-path", runtimeCp.asPath,
            "--add-modules", "javafx.controls,javafx.graphics"
        )
    }
}

// ---- Desktop app ----
// jpackage wraps the all-in-one JAR in a real Windows .exe and puts a cut-down Java
// runtime next to it, so the app starts with a double click and runs on a machine with no
// Java installed. This builds the portable folder; a .msi installer would additionally
// need the WiX Toolset, which this does not assume.

val appName = "Knights Tour Pro"
val appDest = layout.buildDirectory.dir("dist")

val stageForPackaging = tasks.register<Copy>("stageForPackaging") {
    // jpackage copies everything in its input folder into the app, and build/libs also
    // holds the plain and benchmark JARs, so stage the one that should ship on its own.
    from(tasks.shadowJar)
    into(layout.buildDirectory.dir("jpackage-input"))
}

tasks.register<Exec>("packageApp") {
    group = "distribution"
    description = "Build a double-clickable Windows app with its own Java runtime"
    dependsOn(stageForPackaging)

    // Package with the same Java the code is built for, so the bundled runtime matches.
    val jpackage = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    }.map { it.metadata.installationPath.file("bin/jpackage.exe").asFile.absolutePath }

    val stagedJar = tasks.shadowJar.flatMap { it.archiveFileName }
    val input = layout.buildDirectory.dir("jpackage-input")
    val icon = layout.projectDirectory.file("packaging/knight.ico")
    val appVersion = version.toString()

    doFirst {
        // jpackage refuses to write over an app folder that is already there, and the
        // launcher it leaves behind is read-only, which Windows will not delete until the
        // flag is cleared. Clear it on the way down and check the folder really went.
        val previous = appDest.get().asFile.resolve(appName)
        previous.walkBottomUp().forEach {
            it.setWritable(true)
            it.delete()
        }
        check(!previous.exists()) { "Could not remove the previous app at " + previous }

        commandLine(
            jpackage.get(),
            "--type", "app-image",
            "--name", appName,
            "--app-version", appVersion,
            "--input", input.get().asFile.absolutePath,
            "--main-jar", stagedJar.get(),
            // Not MainFX: knights.ui.Launcher explains why it cannot be the main class.
            "--main-class", "knights.ui.Launcher",
            "--icon", icon.asFile.absolutePath,
            "--dest", appDest.get().asFile.absolutePath,
            "--vendor", "CristianInBits",
            "--description", "Knight's Tour solver and visualiser"
        )
    }

    doLast {
        logger.lifecycle("Ready: {}", appDest.get().asFile.resolve(appName).resolve("$appName.exe"))
    }
}

jmh {
    // No defaults on purpose. Anything set here is applied to every benchmark and wins
    // over its annotations, so the values below used to override each class's own
    // @BenchmarkMode, @Warmup and @Measurement without saying so. Each benchmark now
    // chooses its own mode and iteration counts; use the -P knobs for one-off runs.

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
