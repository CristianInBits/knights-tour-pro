# Knight's Tour Pro

High‑performance, extensible solver for the Knight's Tour problem—designed to showcase clean architecture, advanced search techniques (Warnsdorff heuristic, back‑tracking, parallel Fork/Join, and Knuth’s Algorithm X with Dancing Links), and a modern development workflow.

---

##  Project Goals

* **Educational clarity** – readable, well‑documented code suitable for blog posts or talks.
* **Performance** – provide multiple solving strategies and benchmark them.
* **Extensibility** – plug‑in architecture for new heuristics or visual front‑ends.
* **Professional polish** – full CI/CD, >90 % test coverage, code quality gates.

---

##  Current Features (MVP)

| Feature                | Status |
| ---------------------- | ------ |
| Backtracking solver    | ✅      |
| Warnsdorff heuristic   | ✅      |
| Command‑line interface | ✅      |
| JSON/TXT export        | ✅      |

Planned items live in the [Roadmap](#roadmap).

---

##  Directory Structure (gradle layout)

```bash
├── src
│   ├── main
│   │   └── java       # Production code
│   ├── test
│   │   └── java       # JUnit tests
│   └── benchmark      # Micro‑benchmarks & JMH
├── docs               # Architecture diagrams & wiki assets
├── .github
│   └── workflows      # CI pipelines
└── build.gradle.kts   # Build script (Kotlin DSL)
```

> **Why this layout?** It is the default Gradle convention, instantly recognisable to recruiters and CI tools.

---

##  Building & Running

```bash
# Compile + run tests
./gradlew clean build

# Launch CLI with default settings
java -jar build/libs/knights-tour.jar --help
```

Java 17 or newer is required.

---

##  Roadmap

1. **Parallel Fork/Join solver** – configurable thread pool.
2. **Exact Cover (Algorithm X + DLX)** implementation.
3. **Benchmark module** with JMH + CSV output.
4. **JavaFX GUI** with interactive board.
5. **Docker image + GitHub Pages demo**.

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for a detailed timeline.

---

##  Contributing

Pull requests are welcome! Please follow the style guide enforced by Spotless & Checkstyle and ensure all tests pass.

---

##  License

This project is licensed under the MIT license – see the [LICENSE](LICENSE) file for details.
