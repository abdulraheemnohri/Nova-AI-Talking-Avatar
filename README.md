# AI Talking Avatar (Nova) — Android Application

## Overview
AI Talking Avatar (Nova) is a local-first Android application built with **Kotlin**, **Jetpack Compose**, and **Google Filament** for real-time 3D rendering. It includes configurable AI adapters, voice input and speech output, persistent settings/history, avatar state handling and GLB asset loading.

## Key features
- **Real-time 3D rendering:** Filament and glTF loading for binary `.glb` assets, with rendering and lifecycle management.
- **AI engine adapters:** Offline demo, local Ollama endpoint, LiteRT-LM CLI adapter and optional Gemini API adapter.
- **Voice and speech:** Speech recognition and Android Text-to-Speech, with approximate mouth motion where supported.
- **Avatar state mapping:** Idle, listening, thinking, speaking, error and paused states; clip selection depends on clips actually present in the asset.
- **Settings and diagnostics:** AI/provider configuration, rendering options, permissions and diagnostic summaries.

## 3D asset production
The app source tree does not currently include a verified production `assistant.glb`. Use the [Meshy + Blender production pipeline](docs/MESHY_BLENDER_PIPELINE.md) to create and validate a character. The dependency-free structural audit tool is `tools/audit_glb.py`.

```bash
python3 tools/audit_glb.py /path/to/nova.glb --json-out nova-audit.json
```

For production acceptance, also use Blender, the Khronos glTF Validator, and actual Android/Pixel playback tests. Structural inspection alone does not verify animation quality, skin deformation or phoneme-accurate lip-sync.

## Build and test
- Build debug APK: `gradle assembleDebug`
- Run unit tests: `gradle :app:testDebugUnitTest`

Run these commands in an Android SDK/Gradle environment. Results must be recorded from the environment where they were executed; this repository update does not claim a fresh Android build or Pixel-device test.
