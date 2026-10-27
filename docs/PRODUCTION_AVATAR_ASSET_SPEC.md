# Production Avatar Asset Specification

This document is the acceptance contract for replacing the stylized CI prototype with a production-quality assistant character. It does not claim that a production asset has already been generated. The repository's `nova_prototype.glb` remains a smoke-test asset only.

## Deliverable

- Final runtime asset: `app/src/main/assets/models/assistant.glb`
- Source project: editable Blender `.blend` file plus any original source files allowed by the asset licence.
- Preview: front, three-quarter, side, and expression-sheet renders.
- Provenance: creator/source, generation date, licence or terms, attribution requirements, and any paid generation/export costs.
- Validation output: Khronos glTF Validator report and this repository's `tools/audit_glb.py` report.

Do not commit a generated asset until the licence permits redistribution in this repository. Do not commit API keys, provider credentials, or private task URLs.

## Visual target

Create a polished, friendly, realistic-but-stylized adult virtual assistant with a consistent identity across front and side views. Use believable skin shading, natural eye placement, clean facial topology, subtle asymmetry, and a neutral resting expression. Avoid uncanny exaggerated facial proportions, overly glossy skin, tiny pupils, and hair or accessories intersecting the face.

The face must remain readable at a phone-sized portrait framing. Prefer a clean upper-body composition with shoulders and neck included. Keep hair, eyelashes, teeth, tongue, and clothing as separate, well-named meshes where practical. The model must be original or properly licensed; do not imitate a real person without permission.

## Android performance budget

These are project targets, not guarantees. Measure the actual exported asset on the target Pixel 6 before release.

| Property | Target / acceptance |
| --- | --- |
| Container | Binary glTF 2.0 `.glb`, loadable by the project's Filament asset loader |
| Geometry | Prefer 30k–80k triangles total; avoid hidden high-poly geometry |
| Textures | 1K base textures by default; 2K only where visible detail justifies the memory |
| Materials | Keep material count modest; avoid unnecessary transparency and layered effects |
| Rig | One clean facial/neck skeleton if needed; no dangling or unused bones |
| Facial controls | Named morph targets listed below, with neutral shape at weight 0 |
| Animation | At least one neutral idle animation; optional blink animation must not fight runtime blink morphs |
| Scale/orientation | Upright, centered, face toward camera, sensible real-world scale and pivot |
| Delivery | No missing external texture references; all required resources embedded in GLB |

## Required facial morph targets

Use the following names exactly when possible. The renderer normalizes known aliases, but exact canonical names are preferred. Each morph should deform the face naturally without tearing lips or moving the whole head.

- `jawOpen`: jaw opening with believable lower-face motion.
- `A`, `E`, `O`, `U`: distinct open-vowel mouth shapes.
- `MBP`: lips closed for M/B/P consonants.
- `F`: upper teeth/lower lip contact approximation for F/V.
- `L`: tongue/lip shape approximation for L.
- `WQ`: rounded lips.
- `smile`: subtle bilateral smile.
- `blinkLeft`, `blinkRight`: independently closable eyelids.

### Facial deformation checks

For each morph, inspect weights 0.0, 0.5, and 1.0. Check front and three-quarter views. Lips should not detach, teeth should not protrude through the face, eyes should remain seated, and the nose/cheeks should not collapse. Check combined shapes such as `jawOpen + A`, `jawOpen + O`, and `smile + blinkLeft`. Keep the neutral face at zero for all expression morphs.

The current runtime may use procedural jaw movement when suitable morphs are missing. That fallback is a compatibility path, not acceptance for the production asset.

## Blender export workflow

1. Preserve a source `.blend` file and keep mesh/object/morph names readable.
2. Apply transforms where appropriate and remove unused meshes, materials, bones, and shape keys.
3. Check normals, UVs, material assignments, texture paths, eye placement, and facial topology.
4. Confirm the neutral pose and all required morph targets before export.
5. Export GLB with glTF 2.0 settings; include required meshes, materials, skin, animations, and morph targets.
6. Place the candidate at a temporary path first; never overwrite a known-good production asset without review.
7. Run the repository audit:
   ```bash
   python3 tools/audit_glb.py /path/to/assistant-candidate.glb \
     --require-skin --require-animation --require-morph-targets \
     --json-out assistant-candidate-audit.json
   ``
8. Run Khronos glTF Validator against the same candidate and resolve all errors; review warnings individually.
9. Install the candidate in a debug build and check rendering, lifecycle, frame pacing, memory, and thermal behavior on a physical Pixel 6.
10. Only after review, copy the validated candidate to `app/src/main/assets/models/assistant.glb` and update `ASSET_NOTES.md` with its provenance.

## Release acceptance checklist

- [ ] Redistribution licence and attribution recorded in `LICENSES.md` / `ASSET_NOTES.md`.
- [ ] Source file and final GLB are archived.
- [ ] GLB preflight passes.
- [ ] Repository structural audit passes with skin, animation, and morph-target requirements.
- [ ] Khronos glTF Validator has no errors.
- [ ] All required morph targets are present and visibly deform the intended face regions.
- [ ] Neutral expression and idle animation are stable.
- [ ] Android TTS does not leave the face stuck in a non-neutral pose after stop, error, or completion.
- [ ] Physical Pixel 6 testing records app version, device/OS, asset size, observed frame pacing, memory behavior, and thermal notes.
- [ ] Debug and release builds can load the same asset without missing-resource errors.

## Lip-sync truthfulness

The current Android TTS integration can use text-range callbacks where the installed engine supports them, but these callbacks do not provide phoneme identities or facial weights. The current text-driven mouth shapes remain heuristic. A production-ready phoneme-level claim requires an actual timed viseme source (for example, aligned speech output or a validated forced-alignment pipeline) and perceptual checks against the audio. A good facial rig alone does not make timing accurate.

## Current status

**Pending.** This specification is a handoff and quality gate. No production-quality `assistant.glb`, Khronos validation report, or physical Pixel 6 verification is implied by the existence of this document or by passing CI for the stylized prototype.
