"""Unit tests for the dependency-free GLB structural preflight."""
import json
import struct
import subprocess
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

    def test_non_object_json_root_is_rejected_without_crashing(self):
        report = audit_glb.inspect(self.write_temp(make_glb(["not", "a", "glTF document"])))
        self.assertFalse(report["structurally_usable_candidate"])
        self.assertTrue(any("root must be an object" in e for e in report["errors"]))

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

    def test_named_morph_targets_are_reported(self):
        doc = {
            "meshes": [{
                "extras": {"targetNames": ["jawOpen", "A", "blinkLeft"]},
                "primitives": [{"targets": [{"POSITION": 3}, {"POSITION": 4}, {"POSITION": 5}]}],
            }]
        }
        report = audit_glb.inspect(self.write_temp(make_glb(doc)))
        self.assertCountEqual(report["named_morph_targets"], ["jawOpen", "A", "blinkLeft"])
        self.assertEqual(report["missing_required_morph_targets"], [])

    def test_named_morph_count_mismatch_is_rejected(self):
        doc = {
            "meshes": [{
                "name": "Face",
                "extras": {"targetNames": ["jawOpen", "A", "blinkLeft"]},
                "primitives": [{"targets": [{"POSITION": 3}, {"POSITION": 4}]}],
            }]
        }
        report = audit_glb.inspect(self.write_temp(make_glb(doc)))
        self.assertEqual(len(report["morph_name_count_mismatches"]), 1)
        self.assertEqual(report["morph_name_count_mismatches"][0]["named_morph_count"], 3)
        self.assertEqual(report["morph_name_count_mismatches"][0]["primitive_target_count"], 2)
        self.assertFalse(report["structurally_usable_candidate"])
        self.assertTrue(any("count does not match" in e for e in report["errors"]))

    def test_cli_fails_when_required_morph_name_is_missing(self):
        doc = {
            "meshes": [{
                "extras": {"targetNames": ["jawOpen", "A"]},
                "primitives": [{"targets": [{"POSITION": 3}, {"POSITION": 4}]}],
            }]
        }
        glb = self.write_temp(make_glb(doc))
        result = subprocess.run(
            [sys.executable, str(ROOT / "tools" / "audit_glb.py"), str(glb),
             "--require-morph-names", "jawOpen,A,blinkLeft"],
            capture_output=True, text=True, check=False,
        )
        self.assertEqual(result.returncode, 1)
        report = json.loads(result.stdout)
        self.assertEqual(report["missing_required_morph_targets"], ["blinkLeft"])
        self.assertIn("blinkLeft", report["errors"][-1])

    def test_cli_passes_when_all_required_morph_names_exist(self):
        names = ["jawOpen", "A", "E", "O", "U", "MBP", "F", "L", "WQ",
                 "smile", "blinkLeft", "blinkRight"]
        doc = {
            "meshes": [{
                "extras": {"targetNames": names},
                "primitives": [{"targets": [{"POSITION": i} for i in range(len(names))]}],
            }]
        }
        glb = self.write_temp(make_glb(doc))
        result = subprocess.run(
            [sys.executable, str(ROOT / "tools" / "audit_glb.py"), str(glb),
             "--require-morph-targets", "--require-morph-names", ",".join(names)],
            capture_output=True, text=True, check=False,
        )
        self.assertEqual(result.returncode, 0, msg=result.stdout + result.stderr)
        report = json.loads(result.stdout)
        self.assertEqual(report["missing_required_morph_targets"], [])

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
