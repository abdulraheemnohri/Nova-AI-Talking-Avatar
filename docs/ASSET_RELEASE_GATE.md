# Production GLB release gate

Nova must not label an avatar as production-ready just because a .glb file exists or its header parses. The release gate records the exact asset hash, provenance/license, and validation evidence.

## 1. Keep the manifest beside the asset

Create `app/src/main/assets/avatar/asset-manifest.json` beside `assistant.glb`:

```json
{
  "schema_version": 1,
  "asset_path": "assistant.glb",
  "sha256": "REPLACE_WITH_64_CHARACTER_SHA256",
  "source": "Meshy task URL/ID or Blender source project and commit",
  "license": "Exact license or explicit commercial-use terms",
  "generated_at": "YYYY-MM-DD",
  "checks": {
    "khronos_validator": { "status": "pending", "evidence": "" },
    "blender_roundtrip": { "status": "pending", "evidence": "" },
    "android_runtime": { "status": "pending", "evidence": "" },
    "pixel_device": { "status": "pending", "evidence": "" }
  }
}
```

Do not commit placeholders as a passing record. Keep task IDs, license terms, and source-project references that are safe to publish; never commit API keys or signed download URLs.

## 2. Run the gate

From the repository root:

```bash
python3 tools/verify_asset_release.py app/src/main/assets/avatar/asset-manifest.json
```

The default gate requires:
- The asset exists and its SHA-256 matches the manifest.
- Source, license, and generation date are recorded.
- Khronos glTF Validator, Blender round-trip, and Android runtime checks are marked passed with evidence references.
- No validation check is recorded as failed.

To require a real-device check before a device-specific release:

```bash
python3 tools/verify_asset_release.py app/src/main/assets/avatar/asset-manifest.json --require-device
```

Evidence references should point to retained reports, such as validator JSON, Blender inspection notes, and device test logs. A status flag without evidence is rejected.

## 3. Validation is layered

- **Structural preflight:** `tools/audit_glb.py` checks container structure and reports meshes, skins, clips, and morph-target data.
- **Khronos validation:** checks glTF specification conformance.
- **Blender round-trip:** visually inspect deformation, facial controls, materials, and real keyframed actions.
- **Android runtime:** load the exact asset in Filament and test state/clip discovery, morph reset, and resource release.
- **Pixel 6:** capture app version, Android version, load time, sustained frame behavior, memory, and thermal observations.

The release gate validates metadata and hash consistency; it does not itself prove that evidence is genuine or that an animation looks correct. Human/device validation remains required.
