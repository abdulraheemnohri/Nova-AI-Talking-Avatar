"""Contract tests for the Blender-only Meshy intake script.

These tests run without Blender installed. They validate the script's source-level
safety contract; they do not pretend to execute Blender's glTF importer.
"""
import ast
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "blender" / "prepare_meshy_candidate.py"
SOURCE = SCRIPT.read_text(encoding="utf-8")
TREE = ast.parse(SOURCE, filename=str(SCRIPT))


def assigned_literal(name):
    for node in TREE.body:
        if isinstance(node, ast.Assign) and any(
            isinstance(target, ast.Name) and target.id == name
            for target in node.targets
        ):
            return ast.literal_eval(node.value)
    raise AssertionError(f"top-level assignment not found: {name}")


def function_source(name):
    for node in TREE.body:
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)) and node.name == name:
            return ast.get_source_segment(SOURCE, node)
    raise AssertionError(f"function not found: {name}")


class MeshyCandidateScriptContractTests(unittest.TestCase):
    def test_required_facial_controls_are_complete_and_unique(self):
        morphs = assigned_literal("REQUIRED_MORPHS")
        expected = {
            "jawOpen", "A", "E", "O", "U", "MBP", "F", "L", "WQ",
            "smile", "blinkLeft", "blinkRight",
        }
        self.assertEqual(set(morphs), expected)
        self.assertEqual(len(morphs), len(set(morphs)))

    def test_five_canonical_actions_are_required(self):
        actions = assigned_literal("REQUIRED_ACTIONS")
        self.assertEqual(
            actions,
            ["Nova_Idle", "Nova_Listening", "Nova_Thinking",
             "Nova_Greeting", "Nova_Speaking"],
        )

    def test_sha256_reads_file_in_bounded_chunks(self):
        source = function_source("sha256_file")
        self.assertIn("hashlib.sha256()", source)
        self.assertIn("1024 * 1024", source)
        self.assertIn('open(path, "rb")', source)

    def test_source_and_blend_paths_must_not_be_same(self):
        source = function_source("main")
        self.assertIn("os.path.isfile(source)", source)
        self.assertIn("os.path.getsize(source) == 0", source)
        self.assertIn("os.path.abspath(source) == os.path.abspath(blend_output)", source)

    def test_review_artifacts_are_saved_before_gate_failure(self):
        source = function_source("main")
        save_blend = source.index("bpy.ops.wm.save_as_mainfile")
        save_report = source.index('json.dump(report, stream, indent=2, ensure_ascii=False)')
        gate = source.index('if report["status"] != "STRUCTURAL_GATE_PASSED"')
        self.assertLess(save_blend, gate)
        self.assertLess(save_report, gate)
        self.assertIn("return 2", source[gate:])

    def test_script_does_not_claim_to_generate_missing_rig_controls(self):
        self.assertIn("does not auto-rig", SOURCE)
        self.assertIn("does not auto-rig, create shape keys", SOURCE)
        source = function_source("main")
        self.assertIn("missing_required_morphs", source)
        self.assertIn("missing_required_actions", source)

    def test_report_keeps_runtime_and_quality_checks_as_separate_gates(self):
        source = function_source("main")
        for phrase in (
            "Khronos validation",
            "Filament rendering",
            "device performance",
        ):
            self.assertIn(phrase, source)


if __name__ == "__main__":
    unittest.main()
