#!/usr/bin/env python3
"""Audit a Blender character scene and optionally export it as a Nova GLB.

Usage:
  blender -b "Tuff Girl.blend" --python tools/prepare_blend_avatar.py -- --report build/avatar_report.json
  blender -b "Tuff Girl.blend" --python tools/prepare_blend_avatar.py -- --export build/nova_avatar.glb --report build/avatar_report.json

The script never saves over the source .blend file.
"""
from __future__ import annotations
import argparse
import json
import os
import sys
from pathlib import Path
import bpy

VISEME_ALIASES = {
    "AA": {"aa", "viseme_aa", "mouth_aa", "jawopen", "jaw_open", "open"},
    "EE": {"ee", "viseme_ee", "mouth_ee", "smile"},
    "IH": {"ih", "viseme_ih", "mouth_ih"},
    "OH": {"oh", "viseme_oh", "mouth_oh"},
    "OO": {"oo", "viseme_oo", "mouth_oo", "pucker"},
    "FV": {"fv", "viseme_fv", "mouth_fv", "teeth_lip"},
    "MBP": {"mbp", "viseme_mbp", "mouth_mbp", "closed", "lips_closed"},
    "L": {"l", "viseme_l", "mouth_l"},
    "CHSH": {"ch", "sh", "chsh", "viseme_ch", "viseme_sh", "mouth_ch"},
}

def parse_args():
    argv = sys.argv
    argv = argv[argv.index("--") + 1:] if "--" in argv else []
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", default="nova_avatar_inspection.json")
    parser.add_argument("--export", help="Optional output GLB path; omitted means inspect only")
    parser.add_argument("--require-rig", action="store_true")
    parser.add_argument("--require-visemes", action="store_true")
    parser.add_argument("--require-actions", action="store_true")
    return parser.parse_args(argv)

def normalize(value):
    return "".join(ch for ch in value.lower() if ch.isalnum())

def inspect_scene():
    objects, meshes, armatures, shape_key_names, actions, materials = [], [], [], [], [], []
    viseme_matches = {key: [] for key in VISEME_ALIASES}
    for obj in bpy.data.objects:
        objects.append({
            "name": obj.name, "type": obj.type,
            "visible_in_viewport": not obj.hide_viewport,
            "visible_in_render": not obj.hide_render,
            "parent": obj.parent.name if obj.parent else None,
            "animation_data": bool(obj.animation_data),
            "modifiers": [mod.type for mod in obj.modifiers],
        })
        if obj.type == "ARMATURE":
            armatures.append({"name": obj.name, "bone_count": len(obj.data.bones),
                              "bones": [bone.name for bone in obj.data.bones]})
        elif obj.type == "MESH":
            block = obj.data.shape_keys
            keys = [key.name for key in block.key_blocks] if block else []
            meshes.append({
                "name": obj.name, "vertices": len(obj.data.vertices),
                "polygons": len(obj.data.polygons),
                "material_slots": [slot.material.name if slot.material else None for slot in obj.material_slots],
                "armature_modifiers": [mod.object.name for mod in obj.modifiers
                                       if mod.type == "ARMATURE" and mod.object],
                "shape_keys": keys,
                "has_basis_shape_key": bool(keys and keys[0].lower() == "basis"),
            })
            for name in keys:
                if name.lower() == "basis":
                    continue
                shape_key_names.append(name)
                normalized = normalize(name)
                for canonical, aliases in VISEME_ALIASES.items():
                    if normalized in {normalize(alias) for alias in aliases}:
                        viseme_matches[canonical].append({"mesh": obj.name, "shape_key": name})
        for slot in getattr(obj, "material_slots", []):
            if slot.material and slot.material.name not in materials:
                materials.append(slot.material.name)

    for action in bpy.data.actions:
        actions.append({
            "name": action.name,
            "frame_range": [float(action.frame_range[0]), float(action.frame_range[1])],
            "fcurve_count": len(action.fcurves) if hasattr(action, "fcurves") else None,
        })

    scene = bpy.context.scene
    report = {
        "schema_version": 1,
        "blender_version": bpy.app.version_string,
        "source_file": bpy.data.filepath or None,
        "scene": {"name": scene.name, "frame_start": scene.frame_start,
                  "frame_end": scene.frame_end, "fps": scene.render.fps,
                  "render_engine": str(scene.render.engine)},
        "counts": {"objects": len(objects), "meshes": len(meshes),
                   "armatures": len(armatures), "actions": len(actions),
                   "materials": len(materials), "non_basis_shape_keys": len(shape_key_names)},
        "objects": objects, "meshes": meshes, "armatures": armatures,
        "actions": actions, "materials": materials, "shape_keys": shape_key_names,
        "viseme_matches": viseme_matches, "warnings": [], "errors": [], "export": None,
    }
    if not meshes:
        report["warnings"].append("No mesh objects found in the scene.")
    if not armatures:
        report["warnings"].append("No armature found; the exported character may be static.")
    if not shape_key_names:
        report["warnings"].append("No shape keys found; morph-based lip-sync may be unavailable.")
    missing = [name for name, matches in viseme_matches.items() if not matches]
    if missing:
        report["warnings"].append("No matching shape key found for viseme groups: " + ", ".join(missing))
    if not actions:
        report["warnings"].append("No actions found; idle/talking gestures may need to be authored.")
    return report

def export_glb(output_path):
    output_path = os.path.abspath(output_path)
    os.makedirs(os.path.dirname(output_path) or ".", exist_ok=True)
    operator = bpy.ops.export_scene.gltf
    supported = {prop.identifier for prop in operator.get_rna_type().properties}
    requested = {
        "filepath": output_path, "export_format": "GLB",
        "export_animations": True, "export_skins": True, "export_morph": True,
        "export_morph_normal": True, "export_materials": "EXPORT",
        "export_cameras": False, "export_lights": False, "export_yup": True,
        "export_apply": False, "export_selected": False, "export_visible": True,
        "export_extras": True,
    }
    kwargs = {key: value for key, value in requested.items() if key in supported}
    result = operator(**kwargs)
    if "FINISHED" not in result or not os.path.isfile(output_path):
        raise RuntimeError("Blender glTF exporter did not produce the requested GLB.")
    return {"path": output_path, "bytes": os.path.getsize(output_path),
            "exporter_options_used": sorted(kwargs), "exporter_result": sorted(result)}

def main():
    args = parse_args()
    report = inspect_scene()
    if args.require_rig and not report["armatures"]:
        report["errors"].append("--require-rig requested but no armature exists.")
    if args.require_visemes and not any(report["viseme_matches"].values()):
        report["errors"].append("--require-visemes requested but no recognized viseme shape keys were found.")
    if args.require_actions and not report["actions"]:
        report["errors"].append("--require-actions requested but no actions exist.")
    if args.export and not report["errors"]:
        try:
            report["export"] = export_glb(args.export)
        except Exception as exc:
            report["errors"].append(f"GLB export failed: {type(exc).__name__}: {exc}")
    report_path = Path(args.report)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"\nInspection report: {report_path.resolve()}")
    if report["errors"]:
        raise SystemExit(1)

if __name__ == "__main__":
    main()
