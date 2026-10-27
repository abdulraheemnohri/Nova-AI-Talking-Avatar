#!/usr/bin/env python3
"""Inspect a glTF 2.0 binary (.glb) using only the Python standard library.

This is a structural preflight, not a complete glTF validator and not a proof
that a rig deforms or that animations look correct. Use Khronos glTF Validator
and Blender for production validation.
"""
from __future__ import annotations

import argparse
import json
import struct
import sys
from pathlib import Path

GLB_MAGIC = 0x46546C67
JSON_CHUNK = 0x4E4F534A
BIN_CHUNK = 0x004E4942


def inspect(path: Path) -> dict:
    report = {
        "file": str(path),
        "exists": path.is_file(),
        "valid_glb_header": False,
        "structurally_usable_candidate": False,
        "version": None,
        "declared_length": None,
        "actual_length": path.stat().st_size if path.is_file() else 0,
        "json_chunk_present": False,
        "binary_chunk_present": False,
        "mesh_count": 0,
        "node_count": 0,
        "skin_count": 0,
        "animation_count": 0,
        "animation_names": [],
        "missing_required_animation_names": [],
        "morph_target_mesh_primitives": 0,
        "morph_target_names": [],
        "named_morph_targets": [],
        "missing_required_morph_targets": [],
        "morph_name_count_mismatches": [],
        "material_count": 0,
        "embedded_image_count": 0,
        "warnings": [],
        "errors": [],
    }
    if not path.is_file():
        report["errors"].append("File does not exist.")
        return report

    data = path.read_bytes()
    if len(data) < 12:
        report["errors"].append("File is shorter than the 12-byte GLB header.")
        return report

    magic, version, declared_length = struct.unpack_from("<III", data, 0)
    report.update(
        valid_glb_header=(magic == GLB_MAGIC and version == 2),
        version=version,
        declared_length=declared_length,
    )
    if magic != GLB_MAGIC:
        report["errors"].append("Incorrect GLB magic; expected glTF binary magic.")
        return report
    if version != 2:
        report["errors"].append(f"Unsupported GLB version {version}; expected 2.")
    if declared_length != len(data):
        report["errors"].append(
            f"Declared GLB length ({declared_length}) differs from actual length ({len(data)})."
        )

    offset = 12
    document = None
    chunk_index = 0
    while offset + 8 <= len(data):
        chunk_length, chunk_type = struct.unpack_from("<II", data, offset)
        offset += 8
        end = offset + chunk_length
        if chunk_length % 4:
            report["errors"].append(
                f"Chunk {chunk_index} length ({chunk_length}) is not 4-byte aligned."
            )
        if chunk_index == 0 and chunk_type != JSON_CHUNK:
            report["errors"].append("The first GLB chunk must be the JSON chunk.")
        if end > len(data):
            report["errors"].append("Chunk extends beyond end of file.")
            break
        chunk = data[offset:end]
        if chunk_type == JSON_CHUNK:
            if document is not None:
                report["errors"].append("Multiple JSON chunks found; GLB must contain one JSON chunk.")
            else:
                try:
                    document = json.loads(chunk.decode("utf-8").rstrip(" \t\r\n\0"))
                    report["json_chunk_present"] = True
                except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                    report["errors"].append(f"Could not parse JSON chunk: {exc}")
        elif chunk_type == BIN_CHUNK:
            report["binary_chunk_present"] = True
        else:
            report["warnings"].append(f"Unknown chunk type 0x{chunk_type:08x}.")
        offset = end
        chunk_index += 1

    if offset != len(data):
        report["errors"].append(
            f"Trailing incomplete GLB chunk header or bytes at offset {offset}."
        )

    if document is None:
        report["errors"].append("No readable JSON chunk found.")
        return report

    if not isinstance(document, dict):
        report["errors"].append("GLB JSON chunk root must be an object.")
        return report

    def collection(name: str) -> list:
        value = document.get(name, [])
        if not isinstance(value, list):
            report["errors"].append(f"GLB JSON field '{name}' must be an array.")
            return []
        return value

    meshes = collection("meshes")
    nodes = collection("nodes")
    skins = collection("skins")
    animations = collection("animations")
    materials = collection("materials")
    images = collection("images")
    for name, entries in (("meshes", meshes), ("nodes", nodes), ("skins", skins),
                          ("animations", animations), ("materials", materials),
                          ("images", images)):
        for index, entry in enumerate(entries):
            if not isinstance(entry, dict):
                report["errors"].append(
                    f"GLB JSON field '{name}[{index}]' must be an object."
                )

    report["mesh_count"] = len(meshes)
    report["node_count"] = len(nodes)
    report["skin_count"] = len(skins)
    report["animation_count"] = len(animations)
    report["animation_names"] = [
        item.get("name") or f"animation_{i}"
        for i, item in enumerate(animations) if isinstance(item, dict)
    ]
    report["material_count"] = len(materials)
    report["embedded_image_count"] = sum(
        1 for image in images if isinstance(image, dict) and "bufferView" in image
    )

    # Human-readable morph names are conventionally stored in mesh.extras.targetNames.
    # Primitive target keys (POSITION/NORMAL/TANGENT) are accessor semantics, not names.
    for mesh_index, mesh in enumerate(meshes):
        if not isinstance(mesh, dict):
            continue
        extras = mesh.get("extras", {})
        target_names = extras.get("targetNames", []) if isinstance(extras, dict) else []
        if not isinstance(target_names, list):
            report["errors"].append(
                f"GLB JSON field 'meshes[{mesh_index}].extras.targetNames' must be an array."
            )
            target_names = []
        for name in target_names:
            if isinstance(name, str) and name not in report["named_morph_targets"]:
                report["named_morph_targets"].append(name)
        primitives = mesh.get("primitives", [])
        if not isinstance(primitives, list):
            report["errors"].append(
                f"GLB JSON field 'meshes[{mesh_index}].primitives' must be an array."
            )
            continue
        for primitive_index, primitive in enumerate(primitives):
            if not isinstance(primitive, dict):
                report["errors"].append(
                    f"GLB JSON field 'meshes[{mesh_index}].primitives[{primitive_index}]' must be an object."
                )
                continue
            targets = primitive.get("targets", [])
            if not isinstance(targets, list):
                report["errors"].append(
                    f"GLB JSON field 'meshes[{mesh_index}].primitives[{primitive_index}].targets' must be an array."
                )
                continue
            if target_names and len(target_names) != len(targets):
                mismatch = {
                    "mesh": mesh.get("name") or "unnamed_mesh",
                    "primitive_index": primitive_index,
                    "named_morph_count": len(target_names),
                    "primitive_target_count": len(targets),
                }
                report["morph_name_count_mismatches"].append(mismatch)
            if targets:
                report["morph_target_mesh_primitives"] += 1
                for target_index, target in enumerate(targets):
                    if not isinstance(target, dict):
                        report["errors"].append(
                            f"GLB JSON field 'meshes[{mesh_index}].primitives[{primitive_index}].targets[{target_index}]' must be an object."
                        )
                        continue
                    for attribute in target:
                        if attribute not in report["morph_target_names"]:
                            report["morph_target_names"].append(attribute)

    if report["morph_name_count_mismatches"]:
        report["errors"].append(
            "Named morph target count does not match primitive target count for one or more meshes."
        )

    if not meshes:
        report["warnings"].append("No meshes found.")
    if not skins:
        report["warnings"].append("No skins found: this may be a static, unrigged model.")
    if not animations:
        report["warnings"].append("No animation clips found.")
    if not report["morph_target_mesh_primitives"]:
        report["warnings"].append("No morph-target primitives found; facial morph lip-sync may be unavailable.")
    if not report["binary_chunk_present"] and not document.get("buffers"):
        report["warnings"].append("No BIN chunk or buffer declarations found; inspect asset packaging.")
    report["structurally_usable_candidate"] = (
        report["valid_glb_header"]
        and report["json_chunk_present"]
        and bool(meshes)
        and not report["errors"]
    )
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("glb", type=Path, help="Path to the input .glb file")
    parser.add_argument("--require-skin", action="store_true", help="Fail if no skin is present")
    parser.add_argument("--require-animation", action="store_true", help="Fail if no animation clips are present")
    parser.add_argument(
        "--require-animation-names",
        type=str,
        default="",
        help="Comma-separated list of named animation clips required in the GLB",
    )
    parser.add_argument("--require-morph-targets", action="store_true", help="Fail if no morph-target primitives are present")
    parser.add_argument(
        "--require-morph-names",
        type=str,
        default="",
        help="Comma-separated list of named morph targets required in mesh.extras.targetNames",
    )
    parser.add_argument("--json-out", type=Path, help="Optional path to write the full JSON report")
    args = parser.parse_args()

    report = inspect(args.glb)
    if args.require_skin and report["skin_count"] == 0:
        report["errors"].append("Required skin is missing.")
    if args.require_animation and report["animation_count"] == 0:
        report["errors"].append("Required animation clips are missing.")
    if args.require_morph_targets and report["morph_target_mesh_primitives"] == 0:
        report["errors"].append("Required morph targets are missing.")
    required_animation_names = [
        name.strip() for name in args.require_animation_names.split(",") if name.strip()
    ]
    report["missing_required_animation_names"] = [
        name for name in required_animation_names if name not in report["animation_names"]
    ]
    if report["missing_required_animation_names"]:
        report["errors"].append(
            "Required named animation clips are missing: "
            + ", ".join(report["missing_required_animation_names"])
        )
    required_names = [name.strip() for name in args.require_morph_names.split(",") if name.strip()]
    report["missing_required_morph_targets"] = [
        name for name in required_names if name not in report["named_morph_targets"]
    ]
    if report["missing_required_morph_targets"]:
        report["errors"].append(
            "Required named morph targets are missing: "
            + ", ".join(report["missing_required_morph_targets"])
        )

    output = json.dumps(report, indent=2, ensure_ascii=False)
    print(output)
    if args.json_out:
        args.json_out.write_text(output + "\n", encoding="utf-8")
    return 0 if report.get("structurally_usable_candidate") and not report["errors"] else 1


if __name__ == "__main__":
    sys.exit(main())
