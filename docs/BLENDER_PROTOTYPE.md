# Nova Blender Prototype Generator

This generator is a **procedural pipeline smoke-test asset**, not the final Meshy character. It creates a stylized low-detail humanoid made from separate rigid-weighted mesh parts, a basic armature, simple named animation actions, and basic head/mouth/eyelid shape keys, then attempts to export a GLB.

## Requirements

- Blender 3.6 LTS or a compatible Blender 4.x build with the glTF 2.0 exporter enabled.
- Run from the repository root so the output path is predictable.

## Generate

```bash
blender -b --python blender/build_nova_prototype.py
```

Expected output path:

`app/src/main/assets/models/nova_prototype.glb`

The script prints the resolved output path. Confirm the file exists before running the repository's structural audit:

```bash
python3 tools/audit_glb.py app/src/main/assets/models/nova_prototype.glb \
  --require-skin --require-animation --require-morph-targets \
  --json-out nova-prototype-audit.json
```

If the Blender exporter rejects an option or the audit fails, record the Blender version and fix the exporter/API mismatch; do not mark the asset as validated based on the script's exit message alone.

## Included prototype actions

- `Nova_Idle`
- `Nova_Listening`
- `Nova_Thinking`
- `Nova_Greeting`
- `Nova_Speaking`

The generator defines these as Blender actions with keyframes. Exporter behavior can vary by Blender version, so verify that the GLB actually contains all five clips and that each clip plays after import.

## Known limitations

- This is a geometric placeholder character, not a photorealistic or Meshy-generated model.
- Body parts use rigid one-bone weights; this does not demonstrate production-quality smooth skin deformation.
- Head shape keys deform a simplified spherical head and the mouth keys deform a separate simple mouth object; this is not a realistic facial rig or phoneme-accurate lip-sync system.
- Eye blink keys flatten the prototype eye meshes; they are placeholders rather than anatomically accurate eyelids.
- Hands, clothing, eyes, hair, topology, materials, and anatomy are deliberately simplified.
- The script has only been syntax-checked with Python in the authoring environment. Blender execution, GLB export, Khronos validation, Android Filament rendering, and Pixel 6 playback remain unverified until run in those environments.

## Production path

Use the generated GLB only to smoke-test import, animation discovery, and basic renderer wiring. For the release character, generate/export Nova from Meshy, import that asset into Blender, repair or create the humanoid rig and skin weights, add actual facial morph targets, test each animation, then export and validate the production GLB. Keep the Meshy source export unchanged for provenance.
