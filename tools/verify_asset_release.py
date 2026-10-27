#!/usr/bin/env python3
"""Verify release metadata and evidence for Nova's production GLB.

This gate verifies file identity and recorded evidence, not visual quality. A passed
status must reference a real evidence artifact or report; it cannot be inferred from
the existence of a GLB.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

REQUIRED_METADATA = ("source", "license", "generated_at")
CHECKS = ("khronos_validator", "blender_roundtrip", "android_runtime", "pixel_device")
VALID_STATUSES = {"pending", "passed", "failed"}


def load_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise ValueError(f"Cannot read valid JSON manifest: {exc}") from exc
    if not isinstance(value, dict):
        raise ValueError("Manifest root must be a JSON object.")
    return value


def verify(manifest_path: Path, require_device: bool = False) -> list[str]:
    errors: list[str] = []
    try:
        manifest = load_json(manifest_path)
    except ValueError as exc:
        return [str(exc)]

    if manifest.get("schema_version") != 1:
        errors.append("schema_version must be 1.")

    asset_value = manifest.get("asset_path")
    if not isinstance(asset_value, str) or not asset_value.strip():
        errors.append("asset_path must be a non-empty path.")
        asset_path = None
    else:
        asset_path = (manifest_path.parent / asset_value).resolve()
        if not asset_path.is_file():
            errors.append(f"Asset file does not exist: {asset_path}")

    for field in REQUIRED_METADATA:
        value = manifest.get(field)
        if not isinstance(value, str) or not value.strip() or value.strip().lower() in {"unknown", "todo", "tbd"}:
            errors.append(f"{field} must contain a recorded, non-placeholder value.")

    expected_hash = manifest.get("sha256")
    if not isinstance(expected_hash, str) or len(expected_hash) != 64 or any(c not in "0123456789abcdef" for c in expected_hash.lower()):
        errors.append("sha256 must be a 64-character hexadecimal SHA-256 digest.")
    elif asset_path is not None and asset_path.is_file():
        actual_hash = hashlib.sha256(asset_path.read_bytes()).hexdigest()
        if actual_hash.lower() != expected_hash.lower():
            errors.append(f"SHA-256 mismatch: manifest={expected_hash.lower()} actual={actual_hash}")

    checks = manifest.get("checks")
    if not isinstance(checks, dict):
        errors.append("checks must be an object containing validation evidence.")
        checks = {}

    required_checks = list(CHECKS[:3])
    if require_device:
        required_checks.append("pixel_device")
    for name in CHECKS:
        item = checks.get(name)
        if not isinstance(item, dict):
            if name in required_checks:
                errors.append(f"Missing required check record: {name}.")
            continue
        status = item.get("status")
        evidence = item.get("evidence")
        if status not in VALID_STATUSES:
            errors.append(f"{name}.status must be one of: pending, passed, failed.")
            continue
        if name in required_checks and status != "passed":
            errors.append(f"{name} must be passed for this release gate (currently {status}).")
        if status == "passed" and (not isinstance(evidence, str) or not evidence.strip()):
            errors.append(f"{name} is marked passed but has no evidence reference.")
        if status == "failed":
            errors.append(f"{name} records a failed validation.")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path, help="Path to the asset release manifest JSON")
    parser.add_argument("--require-device", action="store_true", help="Require passed Pixel-device validation")
    args = parser.parse_args()

    errors = verify(args.manifest, require_device=args.require_device)
    if errors:
        print("ASSET RELEASE: BLOCKED")
        for error in errors:
            print(f"- {error}")
        return 1
    print("ASSET RELEASE: PASS")
    print("Metadata, file hash, and required evidence records are consistent.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
