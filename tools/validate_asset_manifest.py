#!/usr/bin/env python3
"""Validate provenance/licensing metadata and SHA-256 for a Nova avatar asset."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from datetime import date
from pathlib import Path
from typing import Any

SHA256_RE = re.compile(r"^[0-9a-fA-F]{64}$")
SOURCE_KINDS = {"original", "generated", "purchased", "downloaded", "commissioned"}


def validate_manifest(manifest: Any, repo_root: Path) -> list[str]:
    errors: list[str] = []
    if not isinstance(manifest, dict):
        return ["manifest root must be a JSON object"]

    if type(manifest.get("schema_version")) is not int or manifest.get("schema_version") != 1:
        errors.append("schema_version must be integer 1")

    asset_path = manifest.get("asset_path")
    if not isinstance(asset_path, str) or not asset_path.strip():
        errors.append("asset_path must be a non-empty repository-relative path")
    else:
        rel = Path(asset_path)
        if rel.is_absolute() or ".." in rel.parts:
            errors.append("asset_path must not be absolute or contain '..' path segments")
        else:
            asset = repo_root / rel
            if not asset.is_file():
                errors.append(f"asset file does not exist: {asset_path}")
            else:
                expected_hash = manifest.get("sha256")
                if not isinstance(expected_hash, str) or not SHA256_RE.fullmatch(expected_hash):
                    errors.append("sha256 must contain exactly 64 hexadecimal characters")
                else:
                    actual_hash = hashlib.sha256(asset.read_bytes()).hexdigest()
                    if actual_hash.lower() != expected_hash.lower():
                        errors.append("sha256 does not match the asset file")

    source = manifest.get("source")
    if not isinstance(source, dict):
        errors.append("source must be a JSON object")
    else:
        kind = source.get("kind")
        if not isinstance(kind, str) or kind not in SOURCE_KINDS:
            errors.append("source.kind must be one of: " + ", ".join(sorted(SOURCE_KINDS)))
        for key in ("reference", "creator", "created_at"):
            value = source.get(key)
            if not isinstance(value, str) or not value.strip():
                errors.append(f"source.{key} must be a non-empty string")
        created_at = source.get("created_at")
        if isinstance(created_at, str) and created_at.strip():
            try:
                date.fromisoformat(created_at)
            except ValueError:
                errors.append("source.created_at must use YYYY-MM-DD format")

    license_info = manifest.get("license")
    if not isinstance(license_info, dict):
        errors.append("license must be a JSON object")
    else:
        for key in ("name", "reference"):
            value = license_info.get(key)
            if not isinstance(value, str) or not value.strip():
                errors.append(f"license.{key} must be a non-empty string")
        for key in ("redistribution_allowed", "attribution_required"):
            if not isinstance(license_info.get(key), bool):
                errors.append(f"license.{key} must be a boolean")
        if license_info.get("redistribution_allowed") is not True:
            errors.append("license.redistribution_allowed must be true for a shippable asset")
        attribution_required = license_info.get("attribution_required")
        attribution_text = license_info.get("attribution_text", "")
        if not isinstance(attribution_text, str):
            errors.append("license.attribution_text must be a string")
        elif attribution_required is True and not attribution_text.strip():
            errors.append("license.attribution_text is required when attribution_required is true")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path, help="JSON provenance manifest")
    parser.add_argument(
        "--repo-root", type=Path, default=Path.cwd(),
        help="repository root used to resolve asset_path (default: current directory)",
    )
    args = parser.parse_args()

    try:
        manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    except OSError as exc:
        print(f"ERROR: cannot read manifest: {exc}", file=sys.stderr)
        return 2
    except json.JSONDecodeError as exc:
        print(f"ERROR: invalid JSON: {exc}", file=sys.stderr)
        return 2

    errors = validate_manifest(manifest, args.repo_root.resolve())
    if errors:
        print("Nova asset provenance validation: FAILED")
        for error in errors:
            print(f"- {error}")
        return 1

    print("Nova asset provenance validation: PASSED")
    print(f"Manifest: {args.manifest}")
    print(f"Asset: {manifest['asset_path']}")
    print(f"SHA-256: {manifest['sha256'].lower()}")
    print("Note: this verifies declared metadata and file integrity, not the legal accuracy of the licence claim.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
