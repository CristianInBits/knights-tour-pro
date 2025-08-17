# ♞ Knight's Tour Pro

High-performance, modular solvers for the Knight's Tour — designed with clean architecture, strong tests, and practical benchmarking.

---

## ✨ Goals

* **Educational clarity** – readable, documented code that’s easy to extend.
* **Performance** – multiple strategies, parallel search, and JMH benchmarks.
* **Extensibility** – pluggable solver interfaces and exporters.
* **Professional polish** – reproducible builds, tests, and CI-friendly setup.

---

## ✅ Feature Overview

| Feature                         | Status |
| ------------------------------- | ------ |
| Backtracking (first solution)   | ✅      |
| Backtracking (all solutions)    | ✅      |
| Warnsdorff heuristic (single)   | ✅      |
| **Parallel backtracking (F/J)** | ✅      |
| Open / Closed tours             | ✅      |
| CLI with strategy & mode        | ✅      |
| TXT / JSON exporters            | ✅      |
| JMH benchmarks                  | ✅      |
| Fat JAR packaging (Shadow)      | ✅      |
| Gradle toolchains (JDK 17)      | ✅      |

> Warnsdorff is designed for **single** solution mode; it does not enumerate *all* tours.

---

## 🧱 Architecture

* **Interfaces**

  * `TourSolver` → computes **one** tour or returns an empty list.
  * `AllToursSolver` → enumerates **all** tours (returns `List<List<Position>>`).
* **Solvers**

  * `BacktrackingSolver` (single, DFS).
  * `BacktrackingAllSolutionsSolver` (all, DFS).
  * `WarnsdorffSolver` (single, greedy with degree tie-break).
  * `ParallelBacktrackingSolver` (single, **Fork/Join** with Warnsdorff ordering, early-stop).
* **Model**

  * `Board`, `Position`, `KnightMove` utilities.
* **Exporters**

  * `TxtExporter`, `JsonExporter` implementing `ResultExporter`.

---

## 📁 Project Structure

```text
knights-tour-pro/
├── src/
│   ├── main/java/…         # Application code (solvers, CLI, exporters)
│   ├── test/java/…         # Unit tests (JUnit 5)
│   └── jmh/java/…          # JMH benchmarks
├── docs/                   # Optional: design notes, reports
├── build.gradle.kts        # Gradle build (Kotlin DSL)
├── settings.gradle.kts     # Toolchain resolver (foojay)
└── output/                 # CLI exports (created at runtime)
```

---

## 🔧 Build & Toolchains

This project targets **Java 17** using **Gradle toolchains**. First run will auto-download a JDK 17.

### **settings.gradle.kts**

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "knights-tour-pro"
```

### **build.gradle.kts (highlights)**

```kotlin
plugins {
    java
    application
    id("me.champeau.jmh") version "0.7.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

application { mainClass.set("knights.Main") }
```

### Common tasks

```bash
./gradlew clean build     # compile & test
./gradlew run --args="6 6 0 0 single open backtrack"
./gradlew test
./gradlew jmh
./gradlew shadowJar       # builds the fat jar
```

---

## 🚀 Running

### Using the fat JAR (recommended)

After `./gradlew shadowJar`:

```bash
java -jar build/libs/knights-tour-pro-1.0.0-all.jar <rows> <cols> <startRow> <startCol> <mode> <tourType> [strategy] [flags...]
```

### Using Gradle

```bash
./gradlew run --args="6 6 0 0 single open backtrack"
```

---

## 🎛 CLI Reference

### **Positional arguments**

| Arg        | Values                                    | Required                 | Description                          |
| ---------- | ----------------------------------------- | ------------------------ | ------------------------------------ |
| `rows`     | int                                       | ✅                        | Number of board rows                 |
| `cols`     | int                                       | ✅                        | Number of board columns              |
| `startRow` | int (0-based)                             | ✅                        | Starting row                         |
| `startCol` | int (0-based)                             | ✅                        | Starting column                      |
| `mode`     | `single` \| `all`                         | ✅                        | One tour vs. all tours               |
| `tourType` | `open` \| `closed`                        | ✅                        | Closed must return adjacent to start |
| `strategy` | `backtrack` \| `warnsdorff` \| `parallel` | ❌ (default: `backtrack`) | Search strategy                      |

### **Optional flags**

| Flag          | Description                                                                 |
| ------------- | --------------------------------------------------------------------------- |
| `--limit N`   | In `all` mode, limit how many solutions are printed to console (default 3). |
| `--out DIR`   | Output directory for exports (default `output`).                            |
| `--no-print`  | Do not print board(s) to console.                                           |
| `--no-export` | Do not write TXT/JSON files.                                                |

---

## 🧾 Examples

* **Backtracking, all open tours on 5×5**

  ```bash
  java -jar build/libs/knights-tour-pro-1.0.0-all.jar 5 5 0 0 all open backtrack --limit 2 --out output
  ```

* **Warnsdorff, single open tour on 8×8**

  ```bash
  java -jar build/libs/knights-tour-pro-1.0.0-all.jar 8 8 0 0 single open warnsdorff --out output
  ```

* **Parallel backtracking (Fork/Join), single open tour on 6×6**

  ```bash
  java -jar build/libs/knights-tour-pro-1.0.0-all.jar 6 6 0 0 single open parallel --out output
  ```

> `parallel` currently supports **single** mode (first tour). Enumeration of all tours is handled by `BacktrackingAllSolutionsSolver`.

---

## 📤 Exporters

Both TXT and JSON exports include metadata and paths.

* **TXT** (`output/tour.txt` or `output/tours.txt`)

  ```bash
  Tour Export
  ===============
  rows: 5
  cols: 5
  startRow: 0
  startCol: 0
  tourType: open
  mode: single
  strategy: warnsdorff

  Found Solutions: 1

  Solution #1:
    1: (0,0)
    2: (2,1)
    ...
  ```

* **JSON** (`output/tour.json` or `output/tours.json`)

  ```json
  {
    "metadata": { "rows":5, "cols":5, "startRow":0, "startCol":0, "tourType":"open", "mode":"single", "strategy":"backtrack" },
    "paths": [
      [ {"row":0,"col":0}, {"row":2,"col":1}, ... ]
    ]
  }
  ```

---

## 🧪 Tests

Highlights:

* Solvers: backtracking (single & all), Warnsdorff, **parallel backtracking**.
* Tour geometry checks: length, uniqueness, knight adjacency, closedness (when applicable).
* Exporters: TXT/JSON content & structure.
* CLI smoke tests (optional) for typical scenarios.

Run:

```bash
./gradlew test
```

---

## 📊 Benchmarks (JMH)

Benchmarks live in `src/jmh/java`.

* **SingleTourBenchmark** – compares strategies across sizes/starts for one tour.
* **ParallelVsSequentialBenchmark** – focused comparison:

  * Case: **6×6**, start at **center**, open tour.
  * Shows dramatic speedup of **parallel backtracking** over sequential.

Run:

```bash
./gradlew jmh
# or a specific class:
./gradlew jmh -PjmhInclude='.*ParallelVsSequentialBenchmark.*'
```

> Tip: switch to `Mode.AverageTime` if you prefer ms/op directly.

---

## 🛠 Troubleshooting

* **Toolchains error (JDK 17 not found)**

  * Ensure `settings.gradle.kts` includes:

    ```kotlin
    plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0" }
    ```

  * Or install a local JDK 17 and set:

    ```bash
    org.gradle.java.home=C:/Path/To/jdk-17
    ```

    in `gradle.properties`.

* **Exports not appearing**

  * Check `--out` value (defaults to `output/`) and write permissions.

---

## 🗺 Roadmap

* Parallel enumeration (all tours) with work-stealing.
* Precomputed neighbor tables for faster move generation.
* Optional GUI (JavaFX) and web frontends.
* Export formats: CSV, SVG board rendering.
* CI workflow & benchmark publishing.

---

## 🤝 Contributing

PRs welcome! Please:

* Keep code style consistent.
* Include tests for new features.
* Ensure `./gradlew test` and (when relevant) `./gradlew jmh` pass.

---

## 📄 License

MIT — see [LICENSE](LICENSE).
