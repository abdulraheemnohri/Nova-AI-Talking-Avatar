"""Regression tests for production avatar provenance validation."""
from __future__ import annotations

import hashlib
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "tools" / "validate_asset_manifest.py"
SPEC = importlib.util.spec_from_file_location("validate_asset_manifest", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
VALIDATOR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VALIDATOR)


class AssetManifestValidationTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.asset = self.root / "app" / "src" / "main" / "assets" / "models" / "assistant.glb"
        self.asset.parent.mkdir(parents=True)
        self.asset.write_bytes(b"reviewed-test-glb")
        self.manifest = {
            "schema_version": 1,
            "asset_path": "app/src/main/assets/models/assistant.glb",
            "sha256": hashlib.sha256(self.asset.read_bytes()).hexdigest(),
            "source": {
                "kind": "generated",
                "reference": "https://example.invalid/source",
                "creator": "Test creator",
                "created_at": "2026-10-27",
            },
            "license": {
                "name": "Test licence",
                "reference": "https://example.invalid/licence",
                "redistribution_allowed": True,
                "attribution_required": False,
                "attribution_text": "",
            },
        }

    def tearDown(self) -> None:
        self.temp.cleanup()

    def errors(self, manifest: dict) -> list[str]:
        return VALIDATOR.validate_manifest(manifest, self.root)

    def test_accepts_valid_manifest_and_matching_asset_hash(self) -> None:
        self.assertEqual(self.errors(self.manifest), [])

    def test_rejects_hash_mismatch(self) -> None:
        self.manifest["sha256"] = "0" * 64
        self.assertIn("sha256 does not match the asset file", self.errors(self.manifest))

    def test_rejects_path_traversal(self) -> None:
        self.manifest["asset_path"] = "../assistant.glb"
        self.assertIn(
            "asset_path must not be absolute or contain '..' path segments",
            self.errors(self.manifest),
        )

    def test_requires_explicit_redistribution_permission(self) -> None:
        self.manifest["license"]["redistribution_allowed"] = False
        self.assertIn(
            "license.redistribution_allowed must be true for a shippable asset",
            self.errors(self.manifest),
        )

    def test_requires_attribution_text_when_attribution_is_mandatory(self) -> None:
        self.manifest["license"]["attribution_required"] = True
        self.manifest["license"]["attribution_text"] = "  "
        self.assertIn(
            "license.attribution_text is required when attribution_required is true",
            self.errors(self.manifest),
        )

    def test_rejects_non_object_manifest(self) -> None:
        self.assertEqual(
            VALIDATOR.validate_manifest([], self.root),
            ["manifest root must be a JSON object"],
        )


if __name__ == "__main__":
    unittest.main()
