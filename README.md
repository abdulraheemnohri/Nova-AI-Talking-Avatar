# AI Talking Avatar (Nova) — Android Application

## Overview
AI Talking Avatar (Nova) is a local-first, privacy-respecting Android application built with **Kotlin**, **Jetpack Compose**, and **Google Filament (3D real-time rendering)**. It features an interactive 3D assistant character with customizable rendering quality, voice recognition, text-to-speech output, lip-sync animation, and flexible AI engine adapters (`Offline Demo`, `Ollama Local Endpoint`, `Google LiteRT-LM CLI`, and `Google Gemini API`).

## Key Features
- **Real-Time 3D Rendering:** Powered by Google Filament (`ModelViewer`, `FilamentRendererManager`, `GltfAssetLoader`), supporting `.glb` binary assets with camera orbit, zoom, lighting, and performance tiers.
- **AI Engine Flexibility:** Connect to local Ollama endpoints (`http://localhost:11434`), LiteRT-LM CLI (`http://localhost:8080`), Google Gemini API (with `BuildConfig.GEMINI_API_KEY` or custom key overrides), or run fully offline in rules-based demo mode.
- **Voice & Speech:** Speech-to-text input, Android Text-to-Speech output, voice rate/pitch controls, and real-time speech-synchronized jaw/lip motion.
- **State Management & Animations:** Robust state machine (`IDLE`, `LISTENING`, `THINKING`, `SPEAKING`, `ERROR`, `PAUSED`) mapped to character animation clips.
- **Comprehensive Settings & Diagnostics:** Per-adapter active toggles, hardware permission management, accessibility launcher, theme switching (`Dark Sci-Fi`, `Cyberpunk Cyan`, `OLED Ultra Dark`, `Light Modern`), and redacted runtime diagnostics.

## Build & Test Instructions
- Build Debug APK: `gradle assembleDebug`
- Run Unit Tests: `gradle :app:testDebugUnitTest`
