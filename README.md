# 🎤 Sesli Cevap AI (Advanced Conversational Voice Assistant)

`Sesli Cevap AI` is a modern, high-performance Android voice assistant application built with **Jetpack Compose**, **Kotlin Coroutines**, **Hilt**, and **Google Gemini AI**. It provides real-time conversational voice interaction, hybrid neural Text-To-Speech (TTS), sentiment analytics, and enterprise-grade security hardening.

---

## ✨ Key Features

- **🚀 Streaming LLM & Low-Latency TTS (Phase 1):**
  - Integrates Gemini 2.0 Flash `streamGenerateContent` SSE streaming for ultra-low Time-To-First-Token (<500ms TTFT).
  - Sentence-based streaming speech synthesis starts speaking the first sentence immediately while the AI is still generating the rest of the response.
- **🎙️ Hybrid Neural Voice (ElevenLabs + Native TTS):**
  - Supports ElevenLabs Free Tier neural voice (`eleven_turbo_v2_5`) for hyper-realistic human speech.
  - Automatic and seamless fallback to Android Native TTS with custom speech rate (`0.92f`) and pitch (`0.96f`) micro-tunings when quotas or offline modes occur.
- **👥 Voice Personas & Voice ID Selection:**
  - Choose between distinct AI Personalities: *Friendly Assistant*, *Technical Software Expert*, and *Life & Productivity Coach*.
  - Select ElevenLabs Voice IDs (*Rachel, Bella, Adam, Antoni*).
- **📊 Sentiment & Category Analytics Dashboard:**
  - Real-time sentiment breakdown (Positive, Curious, Thoughtful, Anxious) with percentage meters and category distribution.
  - Searchable and filterable conversation history.
- **🇹🇷 Full Turkish Character (`tr-TR`) Compatibility:**
  - Robust lowercase normalizations and keyword matching for `ı`, `İ`, `Ü`, `ü`, `Ğ`, `ğ`, `Ş`, `ş`, `Ç`, `ç`, `Ö`, `ö`.
- **🔒 Enterprise Security Hardening:**
  - **Network Security Config:** Enforces HTTPS (TLS 1.2/1.3) and blocks cleartext HTTP traffic.
  - **Android Keystore Encryption:** Protects local secrets and database passphrases.
  - **Release Log Shield:** Disables OkHttp logging in production releases (`Level.NONE`) to prevent API keys from leaking into Logcat.

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Architecture:** MVVM / Clean Architecture principles
- **Dependency Injection:** Hilt (Dagger)
- **Networking & API:** Retrofit, Moshi, OkHttp, Google Gemini API, ElevenLabs API
- **Local Storage:** Room Database with Migration strategies
- **Testing:** JUnit, Robolectric, Coroutines Test

---

## 🚀 Getting Started

### Prerequisites

- Android Studio (Ladybug / Jellyfish or newer)
- JDK 17 or higher
- A free Google Gemini API Key from [Google AI Studio](https://aistudio.google.com/)

### Configuration

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/SesliCevapAI.git
   cd SesliCevapAI
   ```

2. **Configure API Keys (`local.properties`):**
   Open `local.properties` in your project root and add your API keys:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ELEVENLABS_API_KEY=your_elevenlabs_api_key_here
   ```

3. **Build & Run:**
   Sync Gradle in Android Studio and run the `:app` module on your device or emulator.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
