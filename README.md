<h1 align="center">♞ Knight’s Tour Pro</h1>

<p align="center">
  <b>Clean. Fast. Extensible.</b><br>
  A modern Java-based solver for the Knight’s Tour problem with multiple search strategies, scalable architecture, and CLI support.
</p>

<p align="center">
  <img src="https://img.shields.io/github/languages/top/lokeshagarwal2304/knights-tour-pro?style=flat-square"/>
  <img src="https://img.shields.io/github/license/lokeshagarwal2304/knights-tour-pro?style=flat-square"/>
  <img src="https://img.shields.io/github/stars/lokeshagarwal2304/knights-tour-pro?style=flat-square"/>
  <img src="https://img.shields.io/github/last-commit/lokeshagarwal2304/knights-tour-pro?style=flat-square"/>
</p>

---

## 🔍 What is Knight’s Tour?

The **Knight's Tour** is a classic problem where the knight piece must move to every square on a chessboard exactly once.  
This repo provides high-performance solving strategies with educational clarity and clean architecture.

---

## 🎯 Key Goals

| 🎓 Educational | 🚀 Performance | 🧩 Extensibility | ✅ Polish |
|----------------|----------------|------------------|-----------|
| Easy-to-understand, readable code | Multiple strategies with benchmarking | Plug-in ready for new heuristics & GUI | Full CI/CD, test coverage, quality gates |

---

## ⚡ Features Implemented

- ✅ Backtracking Solver  
- ✅ Warnsdorff Heuristic  
- ✅ CLI Interface  
- ✅ JSON/TXT Export  

🔜 **Planned in Roadmap**:
- Parallel Fork/Join Solver  
- Algorithm X + DLX  
- JavaFX GUI  
- JMH Benchmarks  
- Docker Image + GitHub Pages Demo  

---

## 🧠 Architecture (Gradle Layout)

```bash
.
├── src
│   ├── main/java         # Solver core
│   ├── test/java         # Unit tests
│   └── benchmark         # JMH Benchmarks
├── docs/                 # Architecture diagrams
├── .github/workflows/    # CI/CD pipelines
└── build.gradle.kts      # Kotlin DSL Build script
````

> ✨ Follows Gradle conventions for compatibility with IDEs & CI tools.

---

## 🛠️ Build & Run Instructions

```bash
# Compile and run tests
./gradlew clean build

# Run the solver
java -jar build/libs/knights-tour.jar --help
```

> 📌 Requires **Java 17+**

---

## 🛣 Roadmap Preview

* [x] Implement backtracking + Warnsdorff
* [ ] Add Fork/Join parallel solver
* [ ] Add Algorithm X (DLX) solver
* [ ] JavaFX GUI & interactive board
* [ ] Docker support & GitHub Pages integration

View full roadmap in [`docs/ROADMAP.md`](docs/ROADMAP.md)

---

## 💡 Why Use This?

* 📚 For learning search techniques & clean Java code.
* 📈 For benchmarking solver strategies.
* ♟️ For hobby chess coders and competitive programmers.
* 🧱 For plugging into bigger AI or visual systems.

---

## 🤝 Contributing Guidelines

We love contributions!
Make sure to:

* Follow formatting (Spotless/Checkstyle)
* Write meaningful commits
* Submit clean PRs with passing tests

---

## 📜 License

Licensed under the **MIT License**. See [`LICENSE`](LICENSE) for more.

---

<p align="center">
  Made with 💻 and ❤️ by <a href="https://github.com/lokeshagarwal2304">lokeshagarwal2304</a>
</p>
