# Validation Report — Nova AI Talking Avatar

## Purpose

This report separates repository-level checks from runtime behavior that still needs
to be demonstrated on a real build or device. A source file or implementation path
being present does not, by itself, prove that the feature works end to end.

## Verification status

| Area | Current status | Evidence / next verification |
| --- | --- | --- |
| GLB preflight | Implemented in source | `GlbPreflight.kt` checks basic GLB v2 header and JSON-chunk bounds. Run `:app:testDebugUnitTest` to verify tests in the current checkout. This is not a complete glTF validator. |
| Filament model loading | Implemented in source; runtime unverified here | Build the app and load a known-good, rigged GLB on-device. |
| Filament lifecycle | Source present; device behavior unverified | Verify initialization, rendering, pause/resume, and cleanup on a physical Android device. |
| AI provider adapters | Source may be present; end-to-end status unverified | Test each configured provider separately with valid local/network configuration and inspect streaming, cancellation, errors, and timeouts. |
| Animation state mapping | Source-level behavior requires test confirmation | Verify each state and confirm that requested animation clips actually exist in the loaded asset. |
| Lip-sync and speech output | End-to-end behavior unverified | Confirm TTS output and visible mouth/jaw motion with the final rigged GLB. |
| Android permissions and accessibility | Manifest/source review required | Test permission grant/denial, microphone flow, and accessibility behavior on supported Android versions. |
| Unit tests | Not independently verified by this report | Use the Android CI workflow or run `gradle --no-daemon :app:testDebugUnitTest`; record the actual result and commit. |
| Debug APK build | Not independently verified by this report | Use the Android CI workflow or run `gradle --no-daemon assembleDebug`; record the actual result and artifact. |
| Production avatar asset | Not yet verified as available | A final, production-quality `assistant.glb` with validated rig and animation clips must be generated, added, and audited. |
| Pixel 6 performance | Not tested by this report | Measure startup, frame pacing, memory use, thermal behavior, and sustained speech/animation on a physical Pixel 6. |

## Reproducible checks

From the repository root, with a compatible JDK and Android SDK installed:

```bash
gradle --no-daemon assembleDebug
gradle --no-daemon :app:testDebugUnitTest
```

The repository's Android CI workflow is at
`.github/workflows/android-build.yml`. A workflow definition is not proof of a
successful run; check the actual Actions run and its uploaded test reports/APK.

## Release gate

Do not describe the project as production-ready until all of the following have
evidence attached to a specific commit:

1. A successful Android build and unit-test run.
2. A valid, licensed, rigged `assistant.glb` that passes a full glTF validator.
3. Verified animation clips and visible speech-synchronized mouth movement.
4. Physical-device checks for lifecycle, permissions, memory, frame rate, and errors.
5. Setup and release instructions that match the tested build.

## Scope note

This document intentionally does not claim that the build, tests, provider
connections, final asset, or Pixel 6 runtime have passed. Update each row only
when a reproducible result or test artifact supports the claim.
