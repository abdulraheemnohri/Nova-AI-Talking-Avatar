# Nova Avatar Asset Provenance Manifest

The production GLB must have a provenance manifest alongside the release evidence. The manifest records where the model came from, who created it, the applicable licence, redistribution permission, attribution obligations, and the asset's SHA-256 digest.

## Required file

Use `assets/assistant.asset-manifest.json` for the final manifest. Copy the example at `assets/assistant.asset-manifest.example.json`, then replace every example value with verified facts. Never claim redistribution permission unless the actual licence/terms support it.

## Validate

From the repository root:

```bash
python3 tools/validate_asset_manifest.py assets/assistant.asset-manifest.json --repo-root .
```

The tool fails if required metadata is missing, the asset path is absolute or traverses outside the repository, the GLB is missing, the declared hash differs from the file, or redistribution is not explicitly allowed. Recalculate SHA-256 after every asset change.

Example hash command:

```bash
sha256sum app/src/main/assets/models/assistant.glb
```

## Manifest fields

- `schema_version`: currently `1`.
- `asset_path`: repository-relative path to the actual GLB.
- `sha256`: 64-character SHA-256 hex digest of the exact GLB bytes.
- `source.kind`: `original`, `generated`, `purchased`, `downloaded`, or `commissioned`.
- `source.reference`: source project, receipt, task ID, or public URL that reviewers can inspect; do not include private credentials or secret URLs.
- `source.creator` and `source.created_at`: creator/provider and date in `YYYY-MM-DD`.
- `license.name` and `license.reference`: licence/terms name and a verifiable reference.
- `license.redistribution_allowed`: must be `true` for a shippable asset.
- `license.attribution_required` and `license.attribution_text`: document and satisfy any attribution requirement.

The validator checks consistency and file integrity; it cannot determine whether a licence interpretation is legally correct. A passing manifest is not evidence of visual quality, glTF conformance, working facial deformation, lip-sync accuracy, or Pixel 6 performance. Those remain separate release gates.
