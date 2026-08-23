# 🎲 Ludo Master — Android Board Game Application

A modern, full-featured, beautifully animated 2D Ludo board game built natively for Android using **Kotlin + Jetpack Compose**, custom **Canvas 15x15 rendering**, and real-time **16-bit PCM procedural sound synthesis**.

---

## 🌟 Features

- **Dynamic 15x15 Canvas Board**: Symmetrical 52-cell track, 4 colored home quadrants, 4 colored home columns, and center goal.
- **8 Safe Zones**: Start squares and star-marked squares where tokens cannot be captured.
- **Smart Multi-Token Stacking**: If multiple tokens occupy the same cell, they are dynamically distributed around the square for easy visibility and touch selection.
- **Animated 3D Tumbling Dice**: Physics-like tumbling rotations, rapid pip number cycling, spring landing bounce, and celebration glow.
- **3D Glossy Token Pieces**: High-fidelity metallic rims, radial color gradients, glossy reflections, and pulsing glow ripples on movable pieces.
- **Step-by-Step Hop Motion**: Tokens smoothly hop tile-by-tile along the board path.
- **Strategic AI Opponent**: Heuristic decision engine with **Easy** and **Hard** difficulty settings.
- **Procedural 16-Bit PCM Sound Effects**: Zero external asset dependencies — procedural audio for dice rolls, token steps, base exits, captures, sixes, and victory fanfare.
- **Flexible Game Modes**: 2 Players (opposite sides Red vs Yellow), 3 Players, and 4 Players in Single Player (vs AI) or Local Pass-and-Play Multiplayer.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose + Compose Canvas API
- **Architecture**: MVVM with Unidirectional Data Flow (UDF)
- **Audio Engine**: Low-latency `AudioTrack` 16-bit PCM sound synthesizer
- **Navigation**: Type-safe Navigation Compose
- **Target SDK**: Android 36 (Min SDK: 24 — Android 7.0+)

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
./gradlew testDebugUnitTest
```

---

## 📄 License
This project is open-source under the MIT License.
