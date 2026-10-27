"""Import and audit a Meshy-exported GLB without silently inventing a rig.

Usage:
  blender -b --python blender/prepare_meshy_candidate.py -- SOURCE.glb OUTPUT.blend REPORT.json

The script preserves the imported geometry, shape keys, armatures and actions as Blender
imports them. It does not auto-rig, fabricate facial deformations, or claim that names
alone prove the quality of a rig. It saves the .blend and report even if the candidate
fails the production gate, then exits non-zero so automation can detect the failure.
"""
import bpy
import hashlib
import json
import os
import sys
from pathlib import Path

REQUIRED_MORPHS = [
    "jawOpen", "A", "E", "O", "U", "MBP", "F", "L", "WQ",
    "smile", "blinkLeft", "blinkRight",
]
REQUIRED_ACTIONS = [
    "Nova_Idle", "Nova_Listening", "Nova_Thinking",
    "Nova_Greeting", "Nova_Speaking",
]


def parse_args():
    args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    if len(args) != 3:
        raise SystemExit(
            "Usage: blender -b --python blender/prepare_meshy_candidate.py -- "
            "SOURCE.glb OUTPUT.blend REPORT.json"
        )
    return [os.path.abspath(value) for value in args]


def sha256_file(path):
    digest = hashlib.sha256()
    with open(path, "rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main():
    source, blend_output, report_output = parse_args()
    if not os.path.isfile(source) or os.path.getsize(source) == 0:
        raise SystemExit("ERROR: source GLB is missing or empty: " + source)
    if os.path.abspath(source) == os.path.abspath(blend_output):
        raise SystemExit("ERROR: source GLB and output .blend paths must differ")

    os.makedirs(os.path.dirname(blend_output) or ".", exist_ok=True)
    os.makedirs(os.path.dirname(report_output) or ".", exist_ok=True)

    # Empty scene: this is a review workspace, not a destructive modification of source.
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for datablocks in (bpy.data.meshes, bpy.data.armatures, bpy.data.materials, bpy.data.actions):
        # Keep no stale datablocks from a default startup file.
        for block in list(datablocks):
            if block.users == 0:
                datablocks.remove(block)

    bpy.ops.import_scene.gltf(filepath=source)
    meshes = [obj for obj in bpy.context.scene.objects if obj.type == "MESH"]
    armatures = [obj for obj in bpy.context.scene.objects if obj.type == "ARMATURE"]
    actions = sorted({action.name for action in bpy.data.actions})

    morph_names = set()
    mesh_report = []
    for obj in meshes:
        keys = []
        if obj.data.shape_keys:
            keys = [key.name for key in obj.data.shape_keys.key_blocks if key.name != "Basis"]
            morph_names.update(keys)
        mesh_report.append({
            "name": obj.name,
            "vertices": len(obj.data.vertices),
            "polygons": len(obj.data.polygons),
            "materials": [slot.material.name if slot.material else None for slot in obj.material_slots],
            "shape_keys": keys,
            "armature_modifiers": [
                modifier.object.name
                for modifier in obj.modifiers
                if modifier.type == "ARMATURE" and modifier.object
            ],
            "vertex_groups": [group.name for group in obj.vertex_groups],
        })

    missing_morphs = [name for name in REQUIRED_MORPHS if name not in morph_names]
    missing_actions = [name for name in REQUIRED_ACTIONS if name not in actions]
    report = {
        "tool": "prepare_meshy_candidate.py",
        "source_file": os.path.basename(source),
        "source_size_bytes": os.path.getsize(source),
        "source_sha256": sha256_file(source),
        "blender_version": bpy.app.version_string,
        "saved_blend": os.path.basename(blend_output),
        "mesh_count": len(meshes),
        "armature_count": len(armatures),
        "meshes": mesh_report,
        "armatures": [
            {
                "name": obj.name,
                "bones": [bone.name for bone in obj.data.bones],
            }
            for obj in armatures
        ],
        "actions": actions,
        "morph_names_found": sorted(morph_names),
        "required_morphs": REQUIRED_MORPHS,
        "missing_required_morphs": missing_morphs,
        "required_actions": REQUIRED_ACTIONS,
        "missing_required_actions": missing_actions,
        "checks": {
            "has_mesh": bool(meshes),
            "has_armature": bool(armatures),
            "has_actions": bool(actions),
            "has_required_morphs": not missing_morphs,
            "has_required_actions": not missing_actions,
        },
        "status": "STRUCTURAL_GATE_PASSED" if (
            meshes and armatures and actions and not missing_morphs and not missing_actions
        ) else "REVIEW_REQUIRED",
        "limitations": [
            "Names and counts do not prove that facial controls deform the intended regions.",
            "This script does not auto-rig, create shape keys, or repair topology or weights.",
            "Animation keyframes, skin deformation, licence, Khronos validation, Filament rendering, and device performance need separate checks.",
        ],
    }

    # Save both artifacts before returning a failing gate status, so artists can inspect it.
    bpy.ops.wm.save_as_mainfile(filepath=blend_output)
    with open(report_output, "w", encoding="utf-8") as stream:
        json.dump(report, stream, indent=2, ensure_ascii=False)
        stream.write("\n")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print("NOVA_MESHY_CANDIDATE_REPORT=" + report_output)

    if report["status"] != "STRUCTURAL_GATE_PASSED":
        print("ERROR: candidate does not satisfy the structural production gate.")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
