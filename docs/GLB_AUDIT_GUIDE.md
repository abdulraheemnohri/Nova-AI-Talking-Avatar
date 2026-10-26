# GLB audit: how to use it

The repository's `tools/audit_glb.py` uses only the Python standard library. It inspects the GLB header and chunks, parses the JSON document, and reports counts for meshes, skins, animation clips, and morph-target primitives.

## Basic report

```bash
python3 tools/audit_glb.py /path/to/nova.glb --json-out nova-audit.json
```

## Require minimum avatar features

```bash
python3 tools/audit_glb.py /path/to/nova.glb \
  --require-skin \
  --require-animation \
  --require-morph-targets \
  --json-out nova-audit.json
```

The command exits non-zero when parsing fails or when requested requirements are missing. Review the report and warnings rather than relying on a single pass/fail result.

## Scope and limitations

This is a structural preflight, not a full glTF conformance validator. It cannot establish whether a rig deforms correctly, whether animation is visually good, whether facial morphs correspond to phonemes, or whether the Android renderer can load the file. For release acceptance, also run the Khronos glTF Validator, inspect the asset in Blender, and test it in the Android app on a physical target device.

## Tests

Run the standard-library tests without installing third-party packages:

```bash
python3 -m unittest discover -s tests -p 'test_*.py' -v
```
