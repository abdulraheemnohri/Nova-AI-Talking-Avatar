# Nova avatar production status — 2026-10-27

This status note distinguishes a generated pipeline prototype from a production character. It is intentionally evidence-based; a green CI job is not a physical-device rendering test.

## Latest generated artifact

- Workflow run: [prototype-export](https://github.com/abdulraheemnohri/Nova-AI-Talking-Avatar/actions/runs/40465041027)
- Artifact name: `nova-prototype-glb-and-audit`
- Artifact ID: `12716591060` (GitHub Actions artifact expires on 2026-11-03)
- Download: [GLB + audit ZIP](https://github.com/abdulraheemnohri/Nova-AI-Talking-Avatar/actions/runs/40465041027)
- Extracted GLB path inside ZIP: `app/src/main/assets/models/nova_prototype.glb`
- GLB SHA-256: `1a66612a1d39d09f199e762a22d7761970ab6b293c789bc657810ed3fbf81f58`
- GLB size: 753,292 bytes
- Audit JSON: `nova-prototype-audit.json` in the same ZIP.

The artifact is a **stylized pipeline prototype**, not a realistic Meshy-generated production character. The repository's Blender script constructs simplified geometry and rigid one-bone-weighted pieces. Its structural audit reports 1 skin, 5 animation clips, and named morphs `blinkLeft`, `blinkRight`, `jawOpen`, `viseme_A`, `viseme_E`, `viseme_M`, and `viseme_O`. These controls are useful for checking the export/runtime path, but they are not the full production facial set.

## Status by evidence

| Stage | Status | Evidence / limitation |
|---|---|---|
| Blender prototype generation | Generated | CI artifact above; 24 meshes, 41 nodes, 1 skin, 5 clips |
| GLB container / structural audit | Verified | Audit JSON reports GLB v2, matching declared/file length, no structural errors |
| Named facial controls | Partially verified | Seven prototype morph names are present; production targets `U`, `MBP`, `F`, `L`, `WQ`, and `smile` are not all present |
| Meshy source character | Pending | No Meshy-origin production GLB or source project has been confirmed in the repository |
| Blender production cleanup / smooth skinning review | Pending | Prototype uses deliberately simplified rigid-weighted parts |
| Khronos glTF Validator | Pending | Repository audit is not a substitute for the Khronos validator |
| Android Filament structural integration | Implemented in source; runtime verification pending | Filament renderer/loader and animation/morph discovery exist; this is not proof of rendering every asset correctly |
| Physical Pixel 6 rendering/performance | Pending | No device test report has been recorded |

## Hybrid Meshy + Blender production handoff

1. Generate an original adult humanoid assistant in Meshy; export GLB and retain the source/download provenance and licence terms.
2. Import that exact file into Blender. Preserve an untouched source copy, then fix topology/materials/UVs, fit a real humanoid armature, and validate skin weights.
3. Add facial shape keys that genuinely deform the face. Use the canonical target set from [the production asset specification](PRODUCTION_AVATAR_ASSET_SPEC.md): `jawOpen`, `A`, `E`, `O`, `U`, `MBP`, `F`, `L`, `WQ`, `smile`, `blinkLeft`, `blinkRight`.
4. Create real keyed actions named `Nova_Idle`, `Nova_Listening`, `Nova_Thinking`, `Nova_Greeting`, and `Nova_Speaking`; inspect them in Blender rather than relying on names alone.
5. Export a candidate GLB to a temporary path. Run the structural gate with skin, animations, and canonical morph names; run Khronos glTF Validator; inspect each facial target and animation visually.
6. Test the candidate in the Android Filament renderer, then record Pixel 6 load time, frame pacing, memory, thermal behaviour, and any warnings.
7. Only after all gates pass, add the licensed asset to `app/src/main/assets/models/assistant.glb` and update asset provenance / SHA-256 records.

## Truthfulness rules

- Do not rename the prototype as the production assistant.
- Do not claim phoneme-accurate lip-sync from a jaw sine wave or text-estimated visemes. Accurate lip-sync requires timestamped visemes from a TTS/alignment layer and matching morph targets.
- Do not label an asset “production ready” based only on a valid GLB header or a successful CI export.
- Keep Meshy credentials, private task URLs, and other secrets out of the repository.
