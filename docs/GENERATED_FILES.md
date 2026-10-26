# Generated Nova prototype files

This folder contains source assets and guides produced during the Nova hybrid Meshy + Blender prototyping process.

- `blender/build_nova_prototype.py`: creates a low-detail procedural placeholder with an armature and candidate animation actions; it is not the final Meshy character.
- `blender/audit_nova_scene.py`: reports mesh, armature, action, vertex-group and shape-key presence from a Blender scene.
- `docs/MESHY_PROMPT.md`: starting prompt and production steps for creating Nova's base character.
- `docs/BLENDER_PROTOTYPE.md`: generation instructions and explicit limitations.
- `docs/MESHY_BLENDER_PIPELINE.md`: end-to-end import, rigging, export and validation plan.

The standalone Android scaffolding ZIPs generated during the conversation are starter packages, not the repository's canonical Android source tree. The repository's existing Android app remains under `app/`; do not overwrite its files with the separate starter module.

## Asset status

No production Meshy GLB is included in this generated source set. The procedural prototype has not been run in Blender in the authoring environment. Do not mark the character as generated, structurally validated, runtime verified or Pixel-device verified until the relevant checks are actually completed.
