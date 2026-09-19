# Edge AI: On-Device LLM Android Assistant 🚀

A high-performance, 100% offline Android application for running Large Language Models (LLMs) locally on mobile devices using `llama.cpp` and ARM Neon / KleidiAI acceleration.

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Runtime](https://img.shields.io/badge/Runtime-llama.cpp%20Native-blue.svg)
![Acceleration](https://img.shields.io/badge/Acceleration-ARM%20Neon%20%2B%20KleidiAI-orange.svg)
![Mode](https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg)

---

## 🌟 Key Features

* **⚡ Ultra-Fast On-Device Inference**: Achieves **~28.8 tokens/sec** with **< 500 ms Time-To-First-Token (TTFT)** on mid-range Android hardware (tested on Motorola Moto G54 5G with MediaTek Dimensity 7020 CPU).
* **🧠 Optimized Model Support**:
  * **SmolLM2-360M-Instruct (Q4_K_M)** — Compact, lightning-fast, high-accuracy mobile assistant (270 MB GGUF).
  * **Qwen3-0.6B (Q4_0)** — Full support for Qwen3 non-thinking mode formatting.
* **🛡️ Private & 100% Offline**: Zero cloud dependence, zero network calls, zero data tracking.
* **🔄 Dynamic Model Switcher**: Seamlessly switch between loaded `.gguf` models directly from the UI without restarting the app.
* **🌊 Real-Time Token Streaming**: Low-latency token stream delivered natively from C++ to Kotlin UI via `Flow<String>`.
* **📊 Live Telemetry Bar**: Tracks generation speed (`tok/s`), TTFT (`ms`), context window (`4096 tokens`), and CPU thread count (`4`).

---

## 🛠️ Tech Stack & Architecture

* **UI Layer**: Android Jetpack, RecyclerView, Material Design 3.
* **Concurrency**: Kotlin Coroutines & `Flow`.
* **Native Engine**: C++ JNI bridge linking `llama.cpp` compiled with CMake (`Release` mode).
* **CPU Acceleration**: ARM v8a / v9a Neon intrinsics + ARM KleidiAI optimized GEMM kernels.
* **Chat Templates**: Native Jinja chat template engine with explicit non-thinking mode control.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug (2024.2+) or higher.
* JDK 17 or JDK 21.
* Android NDK (r27b+).
* An Android device running Android 13+ (API 33+).

### Building from Source

```bash
# Clone the repository
git clone https://github.com/VedantJadhav701/Edge-AI.git
cd Edge-AI

# Build Debug APK
./gradlew assembleDebug
```

---

## 📥 Getting GGUF Models

Place any compatible GGUF model in your phone's `Download` folder or pick it using the app's built-in file picker:

1. **SmolLM2-360M-Instruct (Q4_K_M)**:
   [Download from Hugging Face](https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf)
2. **Qwen3-0.6B (Q4_0)**:
   [Download from Hugging Face](https://huggingface.co/Qwen/Qwen3-0.6B-GGUF)

---

## 📱 Benchmark (Moto G54 5G)

| Model | Size | Quant | Generation Speed | TTFT | Context |
|---|---|---|---|---|---|
| **SmolLM2-360M-Instruct** | 270 MB | Q4_K_M | **28.8 tok/s** | **< 500 ms** | 4096 |
| **Qwen3-0.6B** | 428 MB | Q4_0 | **18.8 tok/s** | **380 ms** | 4096 |

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.
