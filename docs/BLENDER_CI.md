# Blender prototype CI

The `Blender prototype export and GLB audit` workflow installs Blender in a GitHub Actions runner, runs the procedural prototype generator, then requires the exported GLB to contain a skin, animation clip(s), and facial morph targets according to the repository's structural audit.

## Run

- It runs on pull requests to `main`.
- It can also be started manually from the repository's Actions tab using **Run workflow**.

## Artifact

A successful run uploads `nova-prototype-glb-and-audit` with the generated `nova_prototype.glb` and the JSON audit report. This is a **stylized pipeline-test prototype**, not the final Meshy character. The artifact should not be described as photorealistic or production-ready.

## Limits

The audit checks GLB structure and the presence of skin/animation/morph targets. It does not prove the rig deforms correctly, that animation looks good, that facial shapes are semantically accurate, or that Filament can render it on a Pixel 6. The artifact is generated in CI and is not automatically committed into the repository.
