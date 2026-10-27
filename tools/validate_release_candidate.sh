#!/usr/bin/env bash
# Combined structural + provenance gate for the repository's shipping avatar path.
# Does not replace Khronos glTF validation, visual inspection, or physical-device tests.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
asset="$repo_root/app/src/main/assets/models/assistant.glb"
manifest="$repo_root/assets/assistant.asset-manifest.json"
report="${1:-$repo_root/assistant-audit-report.json}"

if [[ ! -f "$asset" ]]; then
  echo "ERROR: production avatar is missing: app/src/main/assets/models/assistant.glb" >&2
  echo "Add the reviewed, properly licensed GLB before running the release gate." >&2
  exit 66
fi
if [[ ! -f "$manifest" ]]; then
  echo "ERROR: provenance manifest is missing: assets/assistant.asset-manifest.json" >&2
  echo "Start from assets/assistant.asset-manifest.example.json and fill it with verified details." >&2
  exit 66
fi

python3 - "$manifest" <<'PY'
import json
import sys
from pathlib import Path

manifest_path = Path(sys.argv[1])
try:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
except (OSError, json.JSONDecodeError) as exc:
    print(f"ERROR: cannot parse provenance manifest: {exc}", file=sys.stderr)
    raise SystemExit(2)
expected = "app/src/main/assets/models/assistant.glb"
if not isinstance(manifest, dict) or manifest.get("asset_path") != expected:
    print(f"ERROR: manifest asset_path must be exactly {expected!r}", file=sys.stderr)
    raise SystemExit(1)
PY

echo "Step 1/2: strict GLB structural validation"
bash "$repo_root/tools/validate_production_avatar.sh" "$asset" "$report"

echo "Step 2/2: provenance and SHA-256 validation"
python3 "$repo_root/tools/validate_asset_manifest.py" "$manifest" --repo-root "$repo_root"

echo "Combined release gate: PASSED"
echo "This does not certify licence claims, visual quality, full glTF conformance, lip-sync accuracy, or device performance."
