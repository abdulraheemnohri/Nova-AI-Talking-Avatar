"""Unit tests for the dependency-free GLB structural preflight."""
import json
import struct
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import audit_glb  # noqa: E402


def make_glb(document: dict, *, magic: int = audit_glb.GLB_MAGIC, version: int = 2,
             declared_length_delta: int = 0) -> bytes:
    payload = json.dumps(document, separators=(",", ":")).encode("utf-8")
    payload += b" " * ((4 - len(payload) % 4) % 4)
    chunk = struct.pack("<II", len(payload), audit_glb.JSON_CHUNK) + payload
    total_length = 12 + len(chunk)
    header = struct.pack("<III", magic, version, total_length + declared_length_delta)
    return header + chunk


class AuditGlbTests(unittest.TestCase):
    def write_temp(self, data: bytes) -> Path:
        tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".glb")
        tmp.write(data)
        tmp.close()
        self.addCleanup(Path(tmp.name).unlink, missing_ok=True)
        return Path(tmp.name)

    def test_valid_glb_with_mesh_is_candidate(self):
        doc = {"asset": {"version": "2.0"}, "meshes": [{"primitives": [{"attributes": {"POSITION": 0}}]}],
               "nodes": [{"mesh": 0}], "animations": [{"name": "Nova_Idle"}],
               "skins": [{"joints": [0]}]}
        report = audit_glb.inspect(self.write_temp(make_glb(doc)))
        self.assertTrue(report["valid_glb_header"])
        self.assertTrue(report["json_chunk_present"])
        self.assertEqual(report["mesh_count"], 1)
        self.assertEqual(report["animation_names"], ["Nova_Idle"])
        self.assertTrue(report["structurally_usable_candidate"])
        self.assertEqual(report["errors"], [])

    def test_invalid_magic_is_rejected(self):
        report = audit_glb.inspect(self.write_temp(make_glb({"meshes": [{}]}, magic=0)))
        self.assertFalse(report["valid_glb_header"])
        self.assertFalse(report["structurally_usable_candidate"])
        self.assertTrue(any("magic" in e.lower() for e in report["errors"]))

    def test_declared_length_mismatch_is_reported(self):
        report = audit_glb.inspect(self.write_temp(make_glb({"meshes": [{}]}, declared_length_delta=4)))
        self.assertTrue(any("length" in e.lower() for e in report["errors"]))
        self.assertFalse(report["structurally_usable_candidate"])

    def test_morph_targets_are_reported(self):
        doc = {"meshes": [{"primitives": [{"targets": [{"POSITION": 3, "NORMAL": 4}]}]}]}
        report = audit_glb.inspect(self.write_temp(make_glb(doc)))
        self.assertEqual(report["morph_target_mesh_primitives"], 1)
        self.assertCountEqual(report["morph_target_names"], ["POSITION", "NORMAL"])

    def test_short_file_is_rejected(self):
        report = audit_glb.inspect(self.write_temp(b"glTF"))
        self.assertFalse(report["structurally_usable_candidate"])
        self.assertTrue(report["errors"])

    def test_missing_file_is_reported(self):
        missing = Path(tempfile.gettempdir()) / "nova-test-file-definitely-missing.glb"
        report = audit_glb.inspect(missing)
        self.assertFalse(report["exists"])
        self.assertTrue(report["errors"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
