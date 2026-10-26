# Nova production pipeline: Meshy + Blender + Android Filament

This repository is the Android Kotlin/Jetpack Compose application. The production character asset is a separate deliverable and must be validated before the app claims full facial animation or character animation support.

## Current repository state

The source tree does not currently contain `app/src/main/assets/avatar/assistant.glb`. The app can load an imported GLB, but a production Meshy character has not been confirmed in this branch. Do not treat a successful file-read or GLB-header check as proof that a character is rigged or animated.

## 1. Generate a base character in Meshy

Use Meshy to generate an original adult humanoid assistant called Nova:
- Silver-ash hair tied back, blue-gray eyes, white/charcoal futuristic outfit, restrained cyan accents.
- Neutral A-pose, full body, hands separated from torso, fingers clearly modeled.
- Clean topology and symmetrical proportions; no props fused to hands or body.
- Prefer separate eyes, teeth, tongue, hair and outfit meshes where practical.
- Use a mobile-friendly polygon budget and texture sizes suitable for a Pixel 6.
- If Meshy exposes rigging/animation options, record exactly which options were enabled and which clips were exported.

Export the source as glTF 2.0 binary (`.glb`). Keep the original export unchanged as the source artifact.

## 2. Blender prototype and source inspection

For a reproducible pipeline smoke test before a Meshy source is available, see [Blender Prototype Generator](BLENDER_PROTOTYPE.md). It generates a simple placeholder with an armature and named actions. This is **not** the production character and does not prove smooth skinning or a production facial rig.

For the actual Meshy character:

1. Import the source GLB into Blender.
2. Check scale, orientation, origin, mesh normals, material assignments and texture packing.
3. If no usable humanoid skeleton exists, rig the character in Blender (or use a documented compatible auto-rigging workflow); do not assume that a static mesh is rigged.
4. Inspect skin weights by rotating shoulders, elbows, wrists, hips, knees and neck. Correct pinching and collapsing.
5. Add facial shape keys only where they can deform the actual face. Suggested semantic controls include jaw open, mouth close, smile, blink left/right, and phoneme/viseme shapes such as A, E, O, U and M/B/P.
6. Create or import the following actions only if they are genuinely present and testable:
   - `Nova_Idle` (loop)
   - `Nova_Listening` (loop)
   - `Nova_Thinking` (loop)
   - `Nova_Greeting` (one-shot)
   - `Nova_Speaking` (loop or speech-driven facial controls)
7. Play each action in Blender and verify there are no broken joints, foot sliding beyond the intended motion, root jumps or unintended mesh intersections.
8. Pack textures and export glTF 2.0 GLB with skinning, morph targets and animation enabled as applicable.

Important: creating an action name alone does not create animation keyframes. An empty action is not an available animation clip.

## 3. Run the structural GLB audit

The repository includes a Python standard-library preflight tool:

```bash
python3 tools/audit_glb.py /path/to/nova.glb --json-out nova-audit.json
```

For a candidate that must include a skin, at least one animation, and facial morph targets:

```bash
python3 tools/audit_glb.py /path/to/nova.glb \
  --require-skin --require-animation --require-morph-targets \
  --json-out nova-audit.json
```

The tool checks the GLB v2 header, JSON/BIN chunks, mesh/node/skin/animation counts, animation names and the presence of morph-target attributes. It is a structural preflight only. It does not verify skin-weight quality, actual facial semantics, animation quality, GPU compatibility or visual correctness.

## 4. Production validation gates

Before marking the asset as production-ready:

- [ ] Khronos glTF Validator reports no blocking errors.
- [ ] Blender can import and re-export the asset without losing materials, skin, morph targets or animation clips.
- [ ] Skin deforms correctly at shoulders, elbows, hips, knees, neck and wrists.
- [ ] Each required action has keyframes and plays correctly.
- [ ] Facial controls are present on the rendered face and produce the expected movement.
- [ ] Textures are embedded or otherwise resolvable on-device.
- [ ] Android Filament loads the asset, shows no missing materials, and releases GPU/native resources on replacement.
- [ ] Run a sustained Pixel 6 playback test and record frame pacing, memory, thermal behavior and load time.

## 5. Android integration notes

The app's state machine can select among clips only if the runtime actually discovers those clips. Keep a diagnostics view that reports discovered animation names and morph target names. Missing clips must be shown as unavailable and mapped to an explicit fallback; never silently report a missing clip as present.

Speech amplitude can drive approximate jaw motion, but phoneme-accurate lip-sync requires timed viseme data and matching morph targets. A mapper alone does not generate phoneme timings.

## Status labels

- **Generated:** a tool exported the file and the exact output path is recorded.
- **Structurally verified:** the GLB audit and Khronos validator passed.
- **Runtime verified:** the actual asset was loaded and exercised in the Android app.
- **Device verified:** runtime checks were performed on the target Pixel device.
- **Pending:** no evidence of that stage has been recorded yet.

Do not commit a generated binary asset until its provenance, license/usage rights, file size and validation report are recorded.
