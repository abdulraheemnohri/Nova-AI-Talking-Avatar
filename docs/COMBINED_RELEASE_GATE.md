# Combined Avatar Release Gate

Before preparing a Nova release, run the combined gate from the repository root:

```bash
bash tools/validate_release_candidate.sh
```

An optional first argument chooses the structural audit report path:

```bash
bash tools/validate_release_candidate.sh /tmp/nova-avatar-audit.json
```

The gate requires the final asset at `app/src/main/assets/models/assistant.glb` and its provenance manifest at `assets/assistant.asset-manifest.json`. Start with `assets/assistant.asset-manifest.example.json`, but replace all placeholders and set redistribution permission only when supported by the actual licence/terms. The manifest's asset path must point to the exact shipping GLB; its SHA-256 must match the current bytes.

It then runs the strict GLB structural gate (skin, animation, morph targets and canonical names) and provenance/integrity validator. Missing asset or manifest is an intentional hard failure, not a successful prototype fallback.

This combined script does **not** replace the Khronos glTF Validator, visual Blender inspection, facial deformation review, phoneme-alignment tests, physical Pixel 6 performance testing, or release signing checks. Do not ship based on this gate alone.
