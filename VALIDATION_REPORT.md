# Validation Report — Nova AI Talking Avatar

## Latest verified CI — TTS range synchronization

PR [#19 — Align heuristic viseme segments to Android TTS range callbacks](https://github.com/abdulraheemnohri/Nova-AI-Talking-Avatar/pull/19) was merged to `main` as commit `5589b959e5fac0aa418f3315a7bf427f584b2792`.

| Area | Status | Evidence |
| --- | --- | --- |
| Source checks | **Passed** | [Workflow run](https://github.com/abdulraheemnohri/Nova-AI-Talking-Avatar/actions/runs/40441285278) |
| Android debug build and unit tests | **Passed** | [Workflow run](https://github.com/abdulraheemnohri/Nova-AI-Talking-Avatar/actions/runs/40441285240) |
| Blender prototype export and GLB audit | **Passed** | [Workflow run](https://github.com/abdulraheemnohri/Nova-AI-Talking-Avatar/actions/runs/40441285317) |
| Python GLB audit and Android JVM tests | **Passed** | [Validation workflow](https://github.com/abdulraheemnohri/Nova-AI-Talking-Avatar/actions/runs/40441285346) |

The Android TTS adapter now consumes `UtteranceProgressListener.onRangeStart` callbacks when the installed engine reports them, and re-anchors short heuristic viseme timelines to the reported spoken text range. Engines without range callbacks retain the whole-text heuristic timeline. Stale callbacks from superseded utterances are ignored.

**Important:** range callbacks provide text/audio range timing, not phoneme identities or facial weights. This is best-effort range-aligned heuristic motion, not phoneme-accurate lip-sync. No physical Pixel 6 test is claimed.

## Previously verified CI results

The following checks passed on PR head commit `a00e688ad8537b254bf3c602e7deaf345ae71e83`, whose changes were merged to `main` as `6aade769643c7f0ef50be1f45488f5212e3d41d0`.

| Area | Status | Evidence |
| --- | --- | --- |
| Python source checks | **Passed** | [Avatar source checks run #35](https://github.com/abdulraheemnohri/Nova-AI-Talking-Avatar/actions/runs/40439090484) |
| GLB audit unit tests | **Passed** | Included in the source-check run above |
| Android debug APK build | **Passed** | [Android build and unit tests run #27](https://github.com/abdulraheemnohri/Nova-AI-Talking-Avatar/actions/runs/40439090577) |
| Android JVM unit tests | **Passed** | Same Android run; test step completed successfully |
| Blender prototype generation | **Passed** | [Blender export and GLB audit run #3](https://github.com/abdulraheemnohri/Nova-AI-Talking-Avatar/actions/runs/40439090464) |
| Prototype GLB structural audit | **Passed** | Same Blender run; requires skin, animation and morph targets |
| Prototype artifact | **Generated in CI** | Download `nova-prototype-glb-and-audit` from the Blender run above |
| Debug APK artifact | **Generated in CI** | Download `nova-debug-apk` from the Android run above |

The GitHub Actions artifacts are temporary. At the time of the earlier report, the prototype artifact was scheduled to expire on 2026-11-03 and the APK artifact on 2026-11-10.

## Implemented behavior

- GLB preflight rejects malformed or oversized containers before the renderer attempts loading.
- Filament discovers recognized facial morph targets and accepts timestamped viseme frames.
- Android Text-to-Speech provides a text-driven heuristic viseme timeline and uses reported text ranges as playback advances when the TTS engine supports range callbacks.
- The Blender prototype generator creates a stylized character with basic jaw/head, mouth, and blink shape keys and named animation actions.
- CI builds the debug APK, runs JVM tests, generates the Blender prototype, and audits the exported GLB.

## Still pending — do not mark production-ready

| Area | Status | Required next evidence |
| --- | --- | --- |
| Production character | **Pending** | Generate/obtain a properly licensed realistic Meshy character, rig it, and export the final `assistant.glb`. The CI prototype is not that asset. |
| Full glTF validation | **Pending** | Run Khronos glTF Validator on the final production asset. The repository audit is structural only. |
| Real phoneme/viseme alignment | **Pending** | Supply actual phoneme/viseme alignment timestamps instead of heuristic text timing; validate against spoken audio. |
| Physical Pixel 6 runtime | **Pending** | Test rendering, model replacement, lifecycle, mouth motion, frame pacing, memory, thermal behavior and battery use on-device. |
| Release build/signing | **Pending** | Verify a reproducible release build, signing instructions, and installation on a target device. |

## Reproducible commands

From the repository root, with JDK 17, Android SDK packages and Gradle 8.11.1 available:

```bash
gradle --no-daemon assembleDebug
gradle --no-daemon :app:testDebugUnitTest
python3 tools/audit_glb.py /path/to/model.glb --require-skin --require-animation --require-morph-targets
```

Generate the stylized Blender smoke-test model with:

```bash
blender --background --python blender/build_nova_prototype.py
python3 tools/audit_glb.py app/src/main/assets/models/nova_prototype.glb \\
  --require-skin --require-animation --require-morph-targets \\
  --json-out nova-prototype-audit.json
```

## Scope note

CI passing proves the source builds and the current unit tests pass in the GitHub runner. It does not establish that the production character exists, that the prototype is photorealistic, that the rig deforms well, or that speech motion is perceptually accurate on a physical device.
