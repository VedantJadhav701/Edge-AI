# Edge AI: On-Device LLM Android Assistant 🚀

A high-performance, 100% offline Android application for running Large Language Models (LLMs) locally on mobile devices using `llama.cpp` and ARM Neon / KleidiAI acceleration.

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Runtime](https://img.shields.io/badge/Runtime-llama.cpp%20Native-blue.svg)
![Acceleration](https://img.shields.io/badge/Acceleration-ARM%20Neon%20%2B%20KleidiAI-orange.svg)
![Models](https://img.shields.io/badge/Models-Bonsai--Q1__0%20%7C%20SmolLM2-purple.svg)
![Mode](https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg)

---

## ⚡ Quick Start for Users

### 📥 Step 1: Install the App (.apk)
Download and install the latest APK directly on your Android phone:
* **[Download Edge-AI APK (Releases Page)](https://github.com/VedantJadhav701/Edge-AI/releases)**

---

### 🧠 Step 2: Download GGUF Models
Select and download GGUF models directly to your device:
* **Bonsai-8B-Q1_0 / Bonsai-4B-Q1_0 (1-Bit Compressed)**: Fast edge inference with ultra-low RAM footprint.
* **[SmolLM2-360M-Instruct (270 MB GGUF)](https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf)**: Ultra-fast 360M parameter conversation engine.

---

### 🚀 Step 3: Run 100% Offline
1. Open **Edge AI** on your Android device.
2. Tap the **Model Chip Selector** `[⚡ Bonsai-Q1_0 ▾]` or the folder icon in the composer.
3. Pick your GGUF model file from Downloads/Storage.
4. Chat instantly with zero cloud dependency or internet connection!

---

## 🌟 Key Features

* **🎨 Modern Floating Chat UI**: Claude-style floating composer with top status banner (`Edge AI Pro`), multiline borderless text input, quick attach button, model chip selector, and responsive action FAB.
* **✨ Clean Markdown Formatting Engine**: Built-in parser renders headers (`#`, `##`, `###`), horizontal rules, bold, italics, code backticks, and bullet lists cleanly without raw Markdown markup leakage.
* **⚡ Ultra-Fast On-Device Inference**: Powered by native `llama.cpp` with ARM Neon + KleidiAI optimizations achieving low-latency inference on mid-range ARM devices.
* **🛡️ Private & 100% Offline**: All chats, models, and telemetry stay strictly on-device. Zero network calls or cloud tracking.
* **📂 Model Picker & Chat History Drawer**: Easily import GGUF models on the fly, toggle dark/light themes, and switch between recent chat sessions with persistent session storage.
* **📊 Hardware Telemetry**: Single-tap collapsible header displaying real-time generation speed (`tok/s`), Time-To-First-Token (`TTFT ms`), context window (`4096 tokens`), and CPU threads (`4`).

---

## 📱 Performance Benchmark (Moto G54 5G - Dimensity 7020)

| Model | Size | Quant | Generation Speed | TTFT | Context | Mode |
|---|---|---|---|---|---|---|
| **SmolLM2-360M-Instruct** | 270 MB | Q4_K_M | **28.8 tok/s** | **< 500 ms** | 4096 | 100% Offline |
| **Bonsai-4B-Q1_0** | 0.57 GB | Q1_0 (1-Bit) | **~20.0 tok/s** | **< 800 ms** | 4096 | 100% Offline |
| **Bonsai-8B-Q1_0** | 1.15 GB | Q1_0 (1-Bit) | **~18.5 tok/s** | **< 1000 ms** | 4096 | 100% Offline |

---

## 🛠️ Building from Source (Developers)

```bash
# Clone the repository
git clone https://github.com/VedantJadhav701/Edge-AI.git
cd Edge-AI/llama.cpp/examples/llama.android

# Build Debug APK
./gradlew assembleDebug

# Install to connected Android device via ADB
./gradlew installDebug
```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.
