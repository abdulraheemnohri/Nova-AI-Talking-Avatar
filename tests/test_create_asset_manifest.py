"""Tests for the explicit provenance manifest generator."""
from __future__ import annotations

import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "tools" / "create_asset_manifest.py"


class ManifestGeneratorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.asset = self.root / "assets" / "assistant.glb"
        self.asset.parent.mkdir(parents=True)
        self.asset.write_bytes(b"test-glb-payload")
        self.output = self.root / "assets" / "assistant.asset-manifest.json"

    def tearDown(self) -> None:
        self.temp.cleanup()

    def run_generator(self, *extra: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [
                sys.executable, str(SCRIPT),
                "--repo-root", str(self.root),
                "--asset", "assets/assistant.glb",
                "--output", "assets/assistant.asset-manifest.json",
                "--source-kind", "generated",
                "--source-reference", "https://example.invalid/source-record",
                "--creator", "Test creator",
                "--created-at", "2026-10-27",
                "--license-name", "Reviewed test licence",
                "--license-reference", "https://example.invalid/licence",
                *extra,
            ],
            capture_output=True,
            text=True,
            check=False,
        )

    def test_writes_manifest_with_actual_asset_hash(self) -> None:
        result = self.run_generator("--redistribution-allowed")
        self.assertEqual(result.returncode, 0, result.stderr)
        manifest = json.loads(self.output.read_text(encoding="utf-8"))
        self.assertEqual(manifest["asset_path"], "assets/assistant.glb")
        self.assertEqual(manifest["sha256"], hashlib.sha256(self.asset.read_bytes()).hexdigest())
        self.assertIs(manifest["license"]["redistribution_allowed"], True)

    def test_requires_explicit_redistribution_confirmation(self) -> None:
        result = self.run_generator()
        self.assertEqual(result.returncode, 2)
        self.assertIn("redistribution permission was not confirmed", result.stderr)
        self.assertFalse(self.output.exists())

    def test_requires_attribution_text_when_required(self) -> None:
        result = self.run_generator("--redistribution-allowed", "--attribution-required")
        self.assertEqual(result.returncode, 2)
        self.assertIn("--attribution-text is required", result.stderr)
        self.assertFalse(self.output.exists())

    def test_refuses_to_overwrite_existing_manifest_without_force(self) -> None:
        self.output.write_text("{}\n", encoding="utf-8")
        result = self.run_generator("--redistribution-allowed")
        self.assertEqual(result.returncode, 2)
        self.assertIn("use --force to replace it", result.stderr)
        self.assertEqual(self.output.read_text(encoding="utf-8"), "{}\n")

    def test_rejects_path_traversal(self) -> None:
        result = self.run_generator("--asset", "../outside.glb", "--redistribution-allowed")
        self.assertEqual(result.returncode, 2)
        self.assertIn("no '..' segments", result.stderr)


if __name__ == "__main__":
    unittest.main()
