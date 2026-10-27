import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from tools.verify_asset_release import verify


class AssetReleaseGateTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.asset = self.root / "assistant.glb"
        self.asset.write_bytes(b"test-glb-bytes")
        self.manifest_path = self.root / "asset-manifest.json"
        self.manifest = {
            "schema_version": 1,
            "asset_path": "assistant.glb",
            "sha256": hashlib.sha256(self.asset.read_bytes()).hexdigest(),
            "source": "Test fixture generated for unit tests",
            "license": "CC0-1.0",
            "generated_at": "2026-10-27",
            "checks": {
                "khronos_validator": {"status": "passed", "evidence": "reports/khronos.json"},
                "blender_roundtrip": {"status": "passed", "evidence": "reports/blender.txt"},
                "android_runtime": {"status": "passed", "evidence": "reports/android.txt"},
                "pixel_device": {"status": "passed", "evidence": "reports/pixel6.txt"},
            },
        }

    def tearDown(self):
        self.temp.cleanup()

    def write_manifest(self):
        self.manifest_path.write_text(json.dumps(self.manifest), encoding="utf-8")

    def test_valid_manifest_passes(self):
        self.write_manifest()
        self.assertEqual(verify(self.manifest_path), [])

    def test_hash_mismatch_blocks_release(self):
        self.manifest["sha256"] = "0" * 64
        self.write_manifest()
        self.assertTrue(any("SHA-256 mismatch" in e for e in verify(self.manifest_path)))

    def test_placeholder_license_blocks_release(self):
        self.manifest["license"] = "TBD"
        self.write_manifest()
        self.assertTrue(any("license" in e for e in verify(self.manifest_path)))

    def test_missing_evidence_for_passed_check_blocks_release(self):
        self.manifest["checks"]["android_runtime"]["evidence"] = ""
        self.write_manifest()
        self.assertTrue(any("no evidence reference" in e for e in verify(self.manifest_path)))

    def test_pixel_check_required_only_when_requested(self):
        self.manifest["checks"]["pixel_device"]["status"] = "pending"
        self.manifest["checks"]["pixel_device"]["evidence"] = ""
        self.write_manifest()
        self.assertEqual(verify(self.manifest_path), [])
        self.assertTrue(any("pixel_device must be passed" in e for e in verify(self.manifest_path, require_device=True)))

    def test_missing_asset_blocks_release(self):
        self.asset.unlink()
        self.write_manifest()
        self.assertTrue(any("Asset file does not exist" in e for e in verify(self.manifest_path)))


if __name__ == "__main__":
    unittest.main()
