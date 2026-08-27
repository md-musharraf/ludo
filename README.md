# 🎲 Ludo Master — High-Performance Android Board Game Application

A modern, mobile-optimized, full-featured 3D Classic Ludo board game built natively for Android using **Kotlin + Jetpack Compose**, custom **Canvas 15x15 rendering**, and **zero-latency cached 16-bit PCM procedural sound synthesis**.

---

## 🌟 Mobile Optimization & Architecture Highlights

- **⚡ Zero Frame-Lag & Thermal Throttling Prevention**:
  - **Guarded Animations**: Infinite transitions only tick when interaction requires them; idle docks and inactive dice consume 0% CPU.
  - **Zero Young-Gen GC Allocations in Canvas**: Reusable paths, arrays, and color palettes eliminate garbage collection pauses (no micro-stutters).
  - **Single-Pass Layering**: Static board elements and dynamic pawns render smoothly at 60–120 FPS across budget, mid-range, and flagship devices.
- **🔊 Ultra-Fast Zero-Latency Sound Engine**:
  - Pre-synthesized 16-bit PCM waveforms cached in RAM on app launch.
  - Pooled `AudioTrack` playback with zero on-the-fly math during gameplay and automatic lifecycle teardown.
- **🏛️ SOLID Principles & Clean Architecture**:
  - **Single Responsibility (SRP)**: Audio, rendering, state flow, and AI logic are decoupled.
  - **Don't Repeat Yourself (DRY)**: Centralized `PlayerColorUtils` and shared geometry helpers.
  - **Open-Closed & Dependency Inversion**: Extensible AI strategies and clean ViewModel state management.
- **📊 Structured Logging & Observability**:
  - `AppLogger` utility with level toggling (`DEBUG`, `INFO`, `WARN`, `ERROR`), game event telemetry, and `measureTrace` frame performance metrics.
- **🛡️ Quality & Pre-Commit Automation**:
  - Husky git hooks (`.husky/pre-commit`) running unit tests before every commit.

---

## 🎲 Core Features

- **Dynamic 15x15 Canvas Board**: Symmetrical 52-cell track, 4 colored home quadrants, 4 colored home columns, and center goal.
- **8 Safe Zones**: Start squares and star-marked squares where tokens cannot be captured.
- **Smart Multi-Token Stacking**: If multiple tokens occupy the same cell, they are dynamically distributed around the square for easy visibility and touch selection.
- **Animated 3D Tumbling Dice**: Physics-like tumbling rotations, rapid pip number cycling, spring landing bounce, and celebration glow.
- **Photorealistic 3D Glossy Gotis (Pawns)**: Turned wooden/glossy plastic pawn anatomy with ambient occlusion ground contact shadows, beveled pedestals, and specular highlights.
- **Step-by-Step Physics Hop Motion**: Tokens smoothly hop tile-by-tile along the board path.
- **Strategic AI Opponent**: Heuristic decision engine with **Easy** and **Hard** difficulty settings.
- **Flexible Game Modes**: 2 Players (opposite sides Red vs Yellow), 3 Players, and 4 Players in Single Player (vs AI) or Local Pass-and-Play Multiplayer.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose + Compose Canvas API
- **Architecture**: Clean MVVM with Unidirectional Data Flow (UDF)
- **Audio Engine**: Low-latency cached `AudioTrack` 16-bit PCM sound engine
- **Navigation**: Type-safe Navigation Compose
- **Target SDK**: Android 36 (Min SDK: 24 — Android 7.0+)
- **Tooling**: Gradle 9, Kotlin Serialization, Husky

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17+
- Android SDK 35+

### Build & Run
```bash
# Clone the repository
git clone https://github.com/md-musharraf/ludo.git
cd ludo

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

---

## 📄 License
This project is open-source under the MIT License.
