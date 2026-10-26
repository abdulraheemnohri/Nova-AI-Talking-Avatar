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
        "morph_target_mesh_primitives": 0,
        "morph_target_names": [],
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
    while offset + 8 <= len(data):
        chunk_length, chunk_type = struct.unpack_from("<II", data, offset)
        offset += 8
        end = offset + chunk_length
        if end > len(data):
            report["errors"].append("Chunk extends beyond end of file.")
            break
        chunk = data[offset:end]
        if chunk_type == JSON_CHUNK:
            if document is not None:
                report["warnings"].append("Multiple JSON chunks found.")
            try:
                document = json.loads(chunk.decode("utf-8").rstrip(" \\t\\r\\n\\0"))
                report["json_chunk_present"] = True
            except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                report["errors"].append(f"Could not parse JSON chunk: {exc}")
        elif chunk_type == BIN_CHUNK:
            report["binary_chunk_present"] = True
        else:
            report["warnings"].append(f"Unknown chunk type 0x{chunk_type:08x}.")
        offset = end

    if document is None:
        report["errors"].append("No readable JSON chunk found.")
        return report

    meshes = document.get("meshes", [])
    nodes = document.get("nodes", [])
    skins = document.get("skins", [])
    animations = document.get("animations", [])
    report["mesh_count"] = len(meshes)
    report["node_count"] = len(nodes)
    report["skin_count"] = len(skins)
    report["animation_count"] = len(animations)
    report["animation_names"] = [
        item.get("name") or f"animation_{i}" for i, item in enumerate(animations)
    ]
    report["material_count"] = len(document.get("materials", []))
    report["embedded_image_count"] = sum(
        1 for image in document.get("images", []) if "bufferView" in image
    )

    for mesh in meshes:
        for primitive in mesh.get("primitives", []):
            targets = primitive.get("targets", [])
            if targets:
                report["morph_target_mesh_primitives"] += 1
                for target in targets:
                    for attribute in target:
                        if attribute not in report["morph_target_names"]:
                            report["morph_target_names"].append(attribute)

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
    parser.add_argument("--require-morph-targets", action="store_true", help="Fail if no morph-target primitives are present")
    parser.add_argument("--json-out", type=Path, help="Optional path to write the full JSON report")
    args = parser.parse_args()

    report = inspect(args.glb)
    if args.require_skin and report["skin_count"] == 0:
        report["errors"].append("Required skin is missing.")
    if args.require_animation and report["animation_count"] == 0:
        report["errors"].append("Required animation clips are missing.")
    if args.require_morph_targets and report["morph_target_mesh_primitives"] == 0:
        report["errors"].append("Required morph targets are missing.")

    output = json.dumps(report, indent=2, ensure_ascii=False)
    print(output)
    if args.json_out:
        args.json_out.write_text(output + "\\n", encoding="utf-8")
    return 0 if report.get("structurally_usable_candidate") and not report["errors"] else 1


if __name__ == "__main__":
    sys.exit(main())
