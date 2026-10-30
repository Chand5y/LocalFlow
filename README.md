# LocalFlow AI for Xiaomi Poco F4 (Snapdragon 870 • 12 GB RAM)
**100% Offline Android Voice Dictation, Floating Bubble Overlay & Local AI Refinement**

Since you don't have a PC, this Android project includes a **GitHub Actions Cloud APK Builder** (`.github/workflows/build-apk.yml`). You can compile the installable `.apk` directly from Chrome on your Poco F4 in 3 minutes for free!

---

## 📱 How to Build the `.APK` Using Only Your Phone Browser

1. **Create a Free GitHub Repo on Your Phone**:
   - Open [github.com/new](https://github.com/new) in Chrome on your Poco F4.
   - Name the repository `poco-f4-localflow`, set it to **Private** (or Public), and tap **Create repository**.
2. **Upload These Project Files**:
   - Unzip `poco_f4_local_flow_android.zip` on your phone using your built-in **Files** app or **ZArchiver**.
   - On your GitHub repository page, tap **Add file → Upload files**, select the unzipped folders/files, and tap **Commit changes**.
3. **Download Your Compiled `.APK`**:
   - Tap the **Actions** tab at the top of your GitHub repository.
   - You will see the workflow **Build Poco F4 LocalFlow Android APK** running on GitHub's cloud server.
   - Once it shows a green checkmark (takes ~3 minutes), tap the workflow run and download **`PocoF4-LocalFlow-debug.apk`** under **Artifacts**!

---

## 🧠 Local AI Models Supported (Download Inside App to `/sdcard/LocalFlow/models/`)

| Stage | Recommended Model | File Format | Size | Speed on Snapdragon 870 |
| :--- | :--- | :--- | :--- | :--- |
| **Stage 1: Speech-to-Text** | `ggml-small-q5_1.bin` (`whisper.cpp`) | GGML Quantized | ~190 MB | ~0.6s per utterance (English + Hindi) |
| **Stage 1: Fast English** | `moonshine-base-int8.onnx` (`sherpa-onnx`) | ONNX Int8 | ~65 MB | <0.3s real-time |
| **Stage 2: AI Cleanup** | `qwen2.5-1.5b-instruct-q4_k_m.gguf` (`llama.cpp`) | GGUF Q4_K_M | ~980 MB | ~32 tokens/sec |
| **Stage 2: Ultra-Light** | `llama-3.2-1b-instruct-q4_k_m.gguf` (`llama.cpp`) | GGUF Q4_K_M | ~800 MB | ~40 tokens/sec |

---

## ✨ Features Included in This Native Android Build

1. **Context-Aware Floating Mic Bubble (`FlowAccessibilityService.kt`)**:
   - Automatically detects when any `EditText` field gains focus in WhatsApp, Gmail, Chrome, Slack, or Notion.
   - Injects polished text straight at your cursor (`ACTION_SET_TEXT` / `ACTION_PASTE`) without switching keyboards.
2. **Companion Voice Keyboard (`FlowVoiceImeService.kt`)**:
   - Optional `InputMethodService` voice bar for apps with custom non-standard text canvases.
3. **Two-Stage Offline AI Pipeline (`LocalAiPipeline.kt` & `native-lib.cpp`)**:
   - **Filler Word Removal**: Strips *"um"*, *"uh"*, *"like"*, *"you know"*.
   - **Live Backtracking & Self-Correction**: Resolves *"Let's meet at 4 PM, actually 3 PM"* → *"Let's meet at 3 PM"*.
   - **Auto-Punctuation & Numbered Lists**: Formats spoken lists (*"1. Apples 2. Bananas"*) automatically.
   - **Whisper Mode (2.5x Gain Boost)** + **Silero VAD Silence Trimming**.
   - **Personal Dictionary, Voice Snippets & Per-App Tone Profiles**.
   - **Smart Doze RAM Unloader**: Unloads GGUF models after 5 minutes of inactivity to save Poco F4 battery.
