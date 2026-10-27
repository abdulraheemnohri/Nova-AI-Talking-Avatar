#!/usr/bin/env bash
# Run the repository's strict, dependency-free acceptance gate for a candidate avatar.
# This is structural validation only; it does not replace Khronos validation or device testing.
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 CANDIDATE.glb [REPORT.json]" >&2
  exit 64
fi

candidate="$1"
report="${2:-}"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
audit="$repo_root/tools/audit_glb.py"

if [[ ! -f "$candidate" ]]; then
  echo "ERROR: candidate GLB not found: $candidate" >&2
  exit 66
fi

required_morphs="jawOpen,A,E,O,U,MBP,F,L,WQ,smile,blinkLeft,blinkRight"
required_animations="Nova_Idle,Nova_Listening,Nova_Thinking,Nova_Greeting,Nova_Speaking"
args=(
  "$audit"
  "$candidate"
  --require-skin
  --require-animation
  --require-animation-names "$required_animations"
  --require-morph-targets
  --require-morph-names "$required_morphs"
)
if [[ -n "$report" ]]; then
  args+=(--json-out "$report")
fi

echo "Nova production avatar structural gate"
echo "Candidate: $candidate"
echo "Required named morphs: $required_morphs"
echo "NOTE: Passing this gate does not verify deformation quality, licence, rendering, or Pixel device performance."
exec python3 "${args[@]}"
