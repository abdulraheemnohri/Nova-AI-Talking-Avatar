#!/usr/bin/env python3
"""Create a SHA-256-bound avatar provenance manifest from explicitly supplied facts.

This tool records user-provided declarations. It does not verify the truth of
source or licence claims; only enable redistribution after reviewing the actual
licence/contract for the specific asset.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from datetime import date
from pathlib import Path

DEFAULT_ASSET = "app/src/main/assets/models/assistant.glb"
DEFAULT_MANIFEST = "assets/assistant.asset-manifest.json"
KINDS = ("original", "generated", "purchased", "downloaded", "commissioned")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


def repo_relative_file(repo_root: Path, value: str) -> tuple[str, Path]:
    rel = Path(value)
    if rel.is_absolute() or ".." in rel.parts:
        raise ValueError("asset path must be repository-relative and contain no '..' segments")
    resolved_root = repo_root.resolve()
    candidate = (resolved_root / rel).resolve()
    if candidate != resolved_root and resolved_root not in candidate.parents:
        raise ValueError("asset path resolves outside the repository")
    if not candidate.is_file():
        raise ValueError(f"asset file does not exist: {value}")
    return rel.as_posix(), candidate


def build_manifest(args: argparse.Namespace, repo_root: Path) -> dict:
    asset_path, asset = repo_relative_file(repo_root, args.asset)
    try:
        created_at = date.fromisoformat(args.created_at).isoformat()
    except ValueError as exc:
        raise ValueError("--created-at must be a real date in YYYY-MM-DD format") from exc

    attribution_text = args.attribution_text or ""
    if args.attribution_required and not attribution_text.strip():
        raise ValueError("--attribution-text is required when --attribution-required is set")
    if not args.redistribution_allowed:
        raise ValueError(
            "redistribution permission was not confirmed; review the asset's actual licence "
            "and pass --redistribution-allowed only if shipping this asset is permitted"
        )

    return {
        "schema_version": 1,
        "asset_path": asset_path,
        "sha256": hashlib.sha256(asset.read_bytes()).hexdigest(),
        "source": {
            "kind": args.source_kind,
            "reference": args.source_reference,
            "creator": args.creator,
            "created_at": created_at,
        },
        "license": {
            "name": args.license_name,
            "reference": args.license_reference,
            "redistribution_allowed": True,
            "attribution_required": args.attribution_required,
            "attribution_text": attribution_text,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path.cwd())
    parser.add_argument("--asset", default=DEFAULT_ASSET, help="repo-relative asset path")
    parser.add_argument("--output", default=DEFAULT_MANIFEST, help="repo-relative output path")
    parser.add_argument("--source-kind", required=True, choices=KINDS)
    parser.add_argument("--source-reference", required=True, help="verifiable source URL, order, or project reference")
    parser.add_argument("--creator", required=True)
    parser.add_argument("--created-at", required=True, help="YYYY-MM-DD")
    parser.add_argument("--license-name", required=True, help="actual licence or contract terms")
    parser.add_argument("--license-reference", required=True, help="URL or stable reference to the applicable terms")
    parser.add_argument("--redistribution-allowed", action="store_true",
                        help="explicitly confirm the reviewed licence permits redistribution")
    parser.add_argument("--attribution-required", action="store_true")
    parser.add_argument("--attribution-text", default="")
    parser.add_argument("--force", action="store_true", help="replace an existing output manifest")
    args = parser.parse_args()

    root = args.repo_root.resolve()
    try:
        output_rel = Path(args.output)
        if output_rel.is_absolute() or ".." in output_rel.parts:
            raise ValueError("output path must be repository-relative and contain no '..' segments")
        output = (root / output_rel).resolve()
        if output != root and root not in output.parents:
            raise ValueError("output path resolves outside the repository")
        if output.exists() and not args.force:
            raise ValueError(f"output already exists: {output_rel}; use --force to replace it")
        manifest = build_manifest(args, root)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    except (OSError, ValueError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    print(f"Manifest written: {output_rel.as_posix()}")
    print(f"Asset: {manifest['asset_path']}")
    print(f"SHA-256: {manifest['sha256']}")
    print("Reminder: verify source and licence claims independently; this tool cannot establish legal permission.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
