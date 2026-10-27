"""Unit tests for the avatar provenance manifest validator."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

import sys
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import validate_asset_manifest  # noqa: E402


class AssetManifestTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.asset = self.root / "assistant.glb"
        self.asset.write_bytes(b"test asset bytes")
        self.manifest = {
            "schema_version": 1,
            "asset_path": "assistant.glb",
            "sha256": hashlib.sha256(self.asset.read_bytes()).hexdigest(),
            "source": {
                "kind": "generated",
                "reference": "https://example.invalid/task/123",
                "creator": "Nova project maintainer",
                "created_at": "2026-10-27",
            },
            "license": {
                "name": "Example redistribution licence",
                "reference": "https://example.invalid/licence",
                "redistribution_allowed": True,
                "attribution_required": False,
                "attribution_text": "",
            },
        }

    def test_valid_manifest_and_matching_hash_pass(self):
        self.assertEqual(validate_asset_manifest.validate_manifest(self.manifest, self.root), [])

    def test_hash_mismatch_fails(self):
        self.manifest["sha256"] = "0" * 64
        errors = validate_asset_manifest.validate_manifest(self.manifest, self.root)
        self.assertIn("sha256 does not match the asset file", errors)

    def test_missing_asset_fails(self):
        self.manifest["asset_path"] = "missing.glb"
        errors = validate_asset_manifest.validate_manifest(self.manifest, self.root)
        self.assertTrue(any("asset file does not exist" in e for e in errors))

    def test_path_traversal_fails(self):
        self.manifest["asset_path"] = "../assistant.glb"
        errors = validate_asset_manifest.validate_manifest(self.manifest, self.root)
        self.assertTrue(any("must not be absolute or contain" in e for e in errors))

    def test_redistribution_must_be_allowed(self):
        self.manifest["license"]["redistribution_allowed"] = False
        errors = validate_asset_manifest.validate_manifest(self.manifest, self.root)
        self.assertIn("license.redistribution_allowed must be true for a shippable asset", errors)

    def test_attribution_text_required_when_flagged(self):
        self.manifest["license"]["attribution_required"] = True
        self.manifest["license"]["attribution_text"] = "  "
        errors = validate_asset_manifest.validate_manifest(self.manifest, self.root)
        self.assertIn("license.attribution_text is required when attribution_required is true", errors)

    def test_non_object_root_fails_cleanly(self):
        self.assertEqual(
            validate_asset_manifest.validate_manifest(["not", "an object"], self.root),
            ["manifest root must be a JSON object"],
        )


if __name__ == "__main__":
    unittest.main()
