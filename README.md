# ♞ Knight's Tour Pro

High-performance, modular solver for the Knight's Tour problem — built to demonstrate clean architecture, extensible design, and advanced solving strategies.

---

## ✨ Project Goals

- **Educational clarity** – readable, well-documented code suitable for talks or tutorials.
- **Performance** – multiple solving strategies with future benchmarking.
- **Extensibility** – architecture supports adding new solvers or front-ends.
- **Professional polish** – CI/CD, test coverage, code quality tools.

---

## ✅ Features (Current MVP)

| Feature                   | Status |
|---------------------------|--------|
| Backtracking solver       | ✅     |
| Warnsdorff heuristic      | ✅     |
| Command-line interface    | ✅     |
| Closed vs Open tour modes | ✅     |
| Basic CLI testing         | ✅     |
| Strategy selection via CLI| ✅     |
| JSON/TXT export           | ✅     |
| Benchmark module (JMH)    | ✅     |

> Planned items are tracked in the [Roadmap](#roadmap).

---

## 📁 Project Structure

```bash

knights-tour-pro/
├── src/
│   ├── main/java        # Application source
│   ├── test/java        # Unit tests (JUnit 5)
│   └── benchmark/       # Planned: JMH benchmarks
├── docs/                # Architecture & design docs
├── .github/workflows/   # CI pipelines (GitHub Actions)
└── build.gradle.kts     # Gradle build script (Kotlin DSL)

````

Follows Gradle's standard layout — ideal for CI tools and onboarding.

---

## 🧪 Tests

All critical logic is covered by unit tests, including:

- Solver validation (`BacktrackingSolver`, `WarnsdorffSolver`)
- Tour type handling (open vs closed)
- CLI integration (`CLITest`) for realistic scenarios
- Exporters validation (JSON/TXT output correctness)

Run tests with:

```bash
./gradlew test
````

---

## 📊 Benchmark Report

The project includes a JMH Benchmark module comparing solver performance.

See: [`docs/Knights Tour Pro - Benchmark Report (JMH).md`](docs/Knights%20Tour%20Pro%20-%20Benchmark%20Report%20(JMH).md)

Sample benchmark modes:

- Throughput measurement for BacktrackingSolver (5x5, 6x6)
- Heuristic performance with WarnsdorffSolver

Run benchmarks with:

```bash
./gradlew jmh
```

---

## 🚀 Usage

### 🛠️ Build

```bash
./gradlew clean build
```

### 🏁 Run

```bash
java -jar build/libs/knights-tour-pro-1.0.jar <rows> <cols> <startRow> <startCol> <mode> <tourType> [strategy]
```

### 📌 Parameters

| Parameter  | Values                    | Required               | Description                           |
| ---------- | ------------------------- | ---------------------- | ------------------------------------- |
| `rows`     | `int`                     | ✅                      | Number of board rows                  |
| `cols`     | `int`                     | ✅                      | Number of board columns               |
| `startRow` | `int`                     | ✅                      | Starting row (0-based)                |
| `startCol` | `int`                     | ✅                      | Starting column (0-based)             |
| `mode`     | `single`, `all`           | ✅                      | One solution or all possible ones     |
| `tourType` | `open`, `closed`          | ✅                      | Whether the tour returns to the start |
| `strategy` | `backtrack`, `warnsdorff` | ❌ (default: backtrack) | Solver to use                         |

---

### ✅ Example: Backtracking, all open tours on 5×5 board

```bash
java -jar knights-tour-pro-1.0.jar 5 5 0 0 all open backtrack
```

### ✅ Example: Warnsdorff heuristic, single open tour on 8×8 board

```bash
java -jar knights-tour-pro-1.0.jar 8 8 0 0 single open warnsdorff
```

> Note: `warnsdorff` strategy does **not** support `mode=all`.

---

## 🗺 Roadmap

Planned features include:

1. 🔄 **Parallel Fork/Join solver**
2. 🧩 **Exact Cover solver (Algorithm X + DLX)**
3. 📊 **JMH benchmark module** with CSV export
4. 🖥 **JavaFX GUI** with interactive board
5. 🐳 **Docker image** and GitHub Pages demo

Track detailed progress in [`docs/ROADMAP.md`](docs/ROADMAP.md).

---

## 🤝 Contributing

Pull requests are welcome! Please:

- Follow style conventions (Checkstyle, Spotless)
- Include or update unit tests
- Ensure all tests pass: `./gradlew test`

---

## 📄 License

Licensed under the [MIT License](LICENSE).
