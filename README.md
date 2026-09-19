# Edge AI: On-Device LLM Android Assistant 🚀

A high-performance, 100% offline Android application for running Large Language Models (LLMs) locally on mobile devices using `llama.cpp` and ARM Neon / KleidiAI acceleration.

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Runtime](https://img.shields.io/badge/Runtime-llama.cpp%20Native-blue.svg)
![Acceleration](https://img.shields.io/badge/Acceleration-ARM%20Neon%20%2B%20KleidiAI-orange.svg)
![Model](https://img.shields.io/badge/Model-SmolLM2--360M--Instruct-purple.svg)
![Mode](https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg)

---

## ⚡ Quick Start for Users (No Coding Required!)

### 📥 Step 1: Install the Android App (.apk)
Download and install the latest APK directly on your phone:
* **[Download Edge-AI APK (Releases Page)](https://github.com/VedantJadhav701/Edge-AI/releases)**

---

### 🧠 Step 2: Download SmolLM2 Model (.gguf)
Download the lightweight, ultra-fast SmolLM2 mobile GGUF model:
* **[Download SmolLM2-360M-Instruct (270 MB GGUF)](https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf)**

---

### 🚀 Step 3: Run 100% Offline
1. Open **Edge AI** on your Android device.
2. Tap the **Folder Icon (FAB)** or **Model Switcher Icon** at the top right.
3. Select `SmolLM2-360M-Instruct-Q4_K_M.gguf`.
4. Start chatting instantly with zero internet connection!

---

## 🌟 Key Features

* **⚡ Ultra-Fast On-Device Inference**: Achieves **~28.8 tokens/sec** with **< 500 ms Time-To-First-Token (TTFT)** on mid-range Android hardware (tested on Motorola Moto G54 5G with MediaTek Dimensity 7020 CPU).
* **🧠 SmolLM2-360M-Instruct Engine**: Specially instruction-tuned for on-device natural conversations, zero CoT reasoning clutter, and ultra-low RAM footprint (270 MB GGUF).
* **🛡️ Private & 100% Offline**: Zero cloud dependence, zero network calls, zero data tracking.
* **🌊 Real-Time Token Streaming**: Low-latency token stream delivered natively from C++ to Kotlin UI via `Flow<String>`.
* **📊 Live Telemetry Bar**: Tracks generation speed (`tok/s`), TTFT (`ms`), context window (`4096 tokens`), and CPU thread count (`4`).

---

## 📱 Performance Benchmark (Moto G54 5G)

| Model | Size | Quant | Generation Speed | TTFT | Context | Mode |
|---|---|---|---|---|---|---|
| **SmolLM2-360M-Instruct** | 270 MB | Q4_K_M | **28.8 tok/s** | **< 500 ms** | 4096 | 100% Offline |

---

## 🛠️ Building from Source (Developers)

```bash
# Clone the repository
git clone https://github.com/VedantJadhav701/Edge-AI.git
cd Edge-AI

# Build Debug APK
./gradlew assembleDebug
```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.
