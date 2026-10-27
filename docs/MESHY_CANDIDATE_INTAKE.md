# Meshy candidate intake (Blender)

This script provides the first reproducible handoff from a Meshy-exported GLB to the Blender review stage. It does **not** make a static model production-ready: it imports and reports what is present, preserves the scene in a .blend, and returns a failing status when required structure is absent.

## Requirements

- Blender 3.6 LTS or Blender 4.x with the glTF 2.0 importer enabled.
- An exported GLB whose licence permits your intended use.
- Enough disk space for the source GLB, editable .blend, and JSON report.

## Run

From the repository root:

```bash
blender -b --python blender/prepare_meshy_candidate.py -- \
  /path/to/meshy-export.glb \
  /path/to/nova-meshy-review.blend \
  /path/to/nova-meshy-review.json
```

The script imports the GLB into a clean scene, then saves the editable Blender file and report. It records the source file's SHA-256, mesh/material data, armatures/bones, shape keys and action names. It does not modify the original GLB.

## Structural production gate

The report expects all canonical morph names:

jawOpen, A, E, O, U, MBP, F, L, WQ, smile, blinkLeft, blinkRight

It also expects the five action names:

Nova_Idle, Nova_Listening, Nova_Thinking, Nova_Greeting, Nova_Speaking

If these are missing, it still saves the review files but exits with code 2 and reports REVIEW_REQUIRED. Do not add fake shape keys or empty actions merely to satisfy the gate. The facial controls must deform the intended face region, and each action must contain actual useful keyframes.

The current gate is intentionally conservative and structural. An artist must still inspect the model, rig weights, shape-key combinations, and animation playback. Run the repository GLB audit, Khronos glTF Validator, and Android Filament / physical Pixel 6 tests separately before release.

## Hybrid production sequence

1. Generate the original character in Meshy and retain its licence/provenance details.
2. Export a source GLB; do not overwrite it.
3. Run this intake script and inspect the .blend and JSON report.
4. Repair topology/materials and skin weights in Blender; create genuine facial shape keys and keyframed actions.
5. Export a separate candidate GLB.
6. Run tools/validate_production_avatar.sh CANDIDATE.glb REPORT.json.
7. Run Khronos validation and test the exact candidate in the Android Filament app.
8. Only after all gates pass, place the candidate at app/src/main/assets/models/assistant.glb and record its licence, SHA-256, validation reports, and Pixel test results.

A successful script exit is not proof of animation quality or runtime compatibility.
