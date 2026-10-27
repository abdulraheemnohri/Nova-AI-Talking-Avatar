# Tuff Girl → Nova Talking Avatar: inspection and export

This workflow prepares the supplied Blender project for evaluation as Nova's 3D talking character. It does not assume the file already has a compatible rig, facial shape keys, or animation clips. The original file is never overwritten.

## 1. Requirements

- A desktop/laptop installation of Blender with the glTF 2.0 exporter enabled. Blender 3.6 LTS or newer is a sensible first attempt.
- The uploaded file header indicates it was saved by Blender 2.81. Open a copy with a compatible Blender version and check for version-dependent changes.
- The repository checked out locally.
- Keep the original Tuff Girl.blend backed up.

## 2. Inspect first — do not export blindly

Place the supplied file in the repository root and run:

```bash
blender -b "Tuff Girl.blend" --python tools/prepare_blend_avatar.py -- --report build/tuff_girl_inspection.json
```

Read the JSON report before making changes. It lists mesh objects, armatures and bones, shape keys, actions, materials, and recognized viseme aliases. Warnings are findings, not automatic proof that the character is broken.

## 3. Export a candidate GLB

```bash
blender -b "Tuff Girl.blend" --python tools/prepare_blend_avatar.py -- --export build/nova_tuff_girl_candidate.glb --report build/tuff_girl_export.json
python tools/audit_glb.py build/nova_tuff_girl_candidate.glb --json-out build/glb_audit.json
```

For a strict talking-avatar candidate, add `--require-rig --require-visemes --require-actions` to the Blender script. Strict mode exits non-zero if any required feature is missing. This is a release check, not an automatic rigging tool.

The exporter adapts its options to the installed Blender version and writes a new GLB. It does not save changes back to the original Blender file.

## 4. Recommended facial shape keys

Create or rename shape keys on the rendered face mesh so the exported GLB has stable, expressive controls. The inspection script recognizes common aliases for these groups:

- AA: open jaw / broad vowel
- EE: spread lips / smile vowel
- IH: narrow-open vowel
- OH: rounded open lips
- OO: tight rounded lips
- FV: upper teeth touching lower lip
- MBP: lips closed for M/B/P
- L: tongue-to-alveolar / L approximation
- CHSH: CH/SH approximation

Exact naming is not a substitute for testing. Some GLB exporters store morph target names in mesh extras. The repository's GLB audit checks structural presence but cannot prove that a shape deforms correctly or is synchronized to speech.

## 5. Animation and mobile integration

Useful named clips are Idle, Listening, Thinking, Speaking, Gesture_Explain, and Gesture_Greet. Keep gestures subtle and avoid a body loop that conflicts with mouth movement. Speech visemes should be driven separately from body gestures.

After export:
1. Open the candidate GLB in Blender and verify materials, skeleton, facial shape keys, and animations.
2. Run the Khronos glTF Validator.
3. Preview every required clip and each facial shape key.
4. Integrate the candidate into Nova's Filament renderer and map the GLB morph targets to the app's viseme timeline.
5. Test on a physical Pixel 6 for render stability, frame time, heat, and memory.
6. Only then fill in the asset manifest using actual hashes and evidence from the real tests.

## Important limitations

- This script audits the opened Blender scene and exports a candidate; it does not automatically create a rig, sculpt facial expressions, repair broken materials, or generate accurate phoneme timing.
- If no armature or facial shape keys are present, use Blender rigging/shape-key work or a suitable character authoring tool before claiming talking-avatar support.
- The GLB audit is a structural preflight only. It does not certify visual quality, deformation, Filament compatibility, or Pixel performance.
- Do not commit private/licensed character assets to a public repository unless you have permission to redistribute them.
