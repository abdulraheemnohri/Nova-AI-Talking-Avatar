# Avatar Asset Manifest Generator

Use this helper after you have selected the exact GLB intended for shipping and reviewed its source and licence/contract terms. It computes the SHA-256 hash and writes the schema expected by `tools/validate_asset_manifest.py`.

From the repository root:

```bash
python3 tools/create_asset_manifest.py \
  --asset app/src/main/assets/models/assistant.glb \
  --output assets/assistant.asset-manifest.json \
  --source-kind generated \
  --source-reference "https://example.com/your-asset-generation-record" \
  --creator "Actual creator or provider" \
  --created-at 2026-10-27 \
  --license-name "Actual licence or contract terms" \
  --license-reference "https://example.com/actual-licence" \
  --redistribution-allowed
```

Replace every example value with facts for the actual asset. The date must be a real `YYYY-MM-DD` date. The `--redistribution-allowed` switch is intentionally explicit: use it only after checking the terms applicable to this particular asset. It records your declaration; it does not independently prove that declaration is legally correct.

If the licence requires attribution, include both flags:

```bash
--attribution-required --attribution-text "Exact required attribution"
```

Safety behavior:
- Computes SHA-256 from the current bytes of the selected asset.
- Rejects absolute paths and paths containing `..`.
- Refuses to overwrite an existing manifest unless `--force` is supplied.
- Requires attribution text when attribution is declared mandatory.
- Does not download, alter, or create the GLB.
- Does not establish ownership, licence validity, redistribution rights, visual quality, GLB conformance, lip-sync quality, or device performance.

After generating the file, review it and run:

```bash
python3 tools/validate_asset_manifest.py assets/assistant.asset-manifest.json --repo-root .
bash tools/validate_release_candidate.sh
```

The combined release gate intentionally fails until the reviewed production GLB and manifest exist at the exact shipping paths.
