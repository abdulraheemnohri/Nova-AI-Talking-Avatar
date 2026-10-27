# Nova Avatar — Production Readiness

## Current status

The repository now has an Android debug build/test workflow, a Blender prototype export workflow, a structural GLB audit, and heuristic speech-viseme playback. The generated Blender model is a **stylized pipeline-test prototype**, not the final realistic Nova character.

## Implemented and verified in GitHub Actions

- Android debug APK build and JVM unit tests pass on the recorded PR validation run.
- Python source checks and structural GLB audit tests pass.
- Blender prototype generation and structural audit pass; CI produces a temporary downloadable GLB artifact.
- The GLB preflight checks container header/JSON chunk bounds before loading.
- The Filament renderer maps recognized facial morph targets and samples a timestamped viseme timeline.
- Android Text-to-Speech triggers a heuristic text-driven viseme timeline; this is not phoneme-accurate audio alignment.
- The prototype has basic jaw/mouth/blink shape keys and named animation actions.

See [Validation Report](../VALIDATION_REPORT.md) and [Blender CI](BLENDER_CI.md) for recorded evidence and artifact links. Artifact retention is limited; rerun the workflow to regenerate expired artifacts.

## Not yet verified / production blockers

- A final, realistic, properly licensed, rigged `assistant.glb` is not yet present as a verified repository asset.
- The stylized CI prototype has not been approved as the shipping character.
- Khronos glTF Validator has not been run against the final production asset.
- Heuristic text-to-viseme timing is not aligned to the actual TTS audio and may drift.
- No physical Pixel 6 run has verified rendering, model replacement, lifecycle behavior, frame pacing, memory, thermal behavior, or battery usage.
- Release build reproducibility, signing, and installation have not been validated.

## Acceptance gates before release

1. Generate or obtain a properly licensed realistic character with a humanoid armature and useful facial controls.
2. Open the source scene in the documented Blender version and inspect the rig and animations visually.
3. Export the production GLB and run the repository audit plus Khronos glTF Validator.
4. Confirm required nodes, animation clips, skinning, morph-target names, and deformation quality.
5. Load and animate the production asset in the Android app.
6. Replace heuristic viseme estimates with timestamps from a TTS provider or alignment component when accurate lip-sync is required; compare motion against recorded speech.
7. Test on a physical Pixel 6, recording model-load time, sustained frame rate, memory, thermal behavior, and lifecycle transitions.
8. Build and sign a release APK using documented, reproducible steps.

## CI scope and evidence

The repository workflows now do more than source checks: they build/test the Android debug app and generate/audit the stylized Blender prototype. These are runner-level checks, not physical-device tests. A passing structural audit does not prove that a model looks realistic, that its rig deforms well, or that mouth motion matches audio.



## Strict candidate asset gate

Run the repository's stricter acceptance script against a candidate GLB:

```bash
bash tools/validate_production_avatar.sh /path/to/assistant.glb audit-report.json
```

The gate requires a skin, at least one animation, morph targets, and the canonical named morph set. The JSON report records structural findings. A pass is not proof of visual quality, correct deformation, redistribution rights, complete glTF conformance, or device performance.

## Production acceptance checklist

- [ ] Original or properly licensed character; provenance, licence/terms, attribution and any generation/export costs documented.
- [ ] Final GLB placed at `app/src/main/assets/models/assistant.glb`, with editable source project retained where licensing permits.
- [ ] Strict repository gate passes and its JSON report is saved.
- [ ] Khronos glTF Validator report reviewed; errors resolved and warnings assessed.
- [ ] Blender inspection confirms each required morph affects the intended facial region without visible tearing or intersections.
- [ ] Idle, talking, blink and interruption transitions inspected visually.
- [ ] Physical Pixel 6 test records Android build, asset size, frame pacing, peak memory, thermal behaviour and crashes during repeated/long speech.
- [ ] Release signing and reproducible release steps verified without committing keystore or credential files.

**Do not mark production complete from CI prototype success alone.** Accurate phoneme-aligned lip-sync and physical Pixel 6 performance remain separate acceptance items.
