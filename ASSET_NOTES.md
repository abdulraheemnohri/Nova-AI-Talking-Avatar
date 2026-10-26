# ASSET NOTES — AI Talking Avatar (Nova)

## Production asset status

**The repository tree currently has no committed `app/src/main/assets/avatar/assistant.glb`.** The app supports loading an imported GLB, but the default production character asset must be added and verified before a bundled avatar can be claimed.

Use the [Meshy + Blender production pipeline](docs/MESHY_BLENDER_PIPELINE.md) and run `tools/audit_glb.py` on the exported asset. A successful header/readability check is not proof of rigging, working animations or facial controls.

## Model specification
- **Format:** glTF 2.0 Binary (.glb)
- **Target platform:** Google Pixel 6 and Android 12+ (API 31+); the app's current minimum SDK may support older devices.
- **Renderer:** Google Filament (`ModelViewer`, `FilamentRendererManager`, `GltfAssetLoader`)
- **Preferred asset location:** `app/src/main/assets/avatar/assistant.glb`, once a reviewed asset is actually committed. Imported assets can also be selected from app storage where supported.

## Rigging and animation acceptance criteria
- **Skeleton:** humanoid hierarchy with root, pelvis, spine/chest, neck/head, arms/hands and legs/feet.
- **Skinning:** usable weights and verified joint deformation.
- **Morph targets:** facial controls for jaw/mouth, blinks and useful visemes; exact names must be discovered from the asset.
- **Animation clips:** preferred names are `Nova_Idle`, `Nova_Listening`, `Nova_Thinking`, `Nova_Greeting`, and `Nova_Speaking`. These names are targets, not a claim that clips are currently present.

## Import instructions
1. Open **Settings → Character Asset**.
2. Choose **Import .glb** and select a mobile-optimized glTF 2.0 binary character.
3. Run structural validation before import.
4. Confirm actual animation playback, facial controls, textures and deformation on the target device.
5. Keep diagnostics honest: missing clips or morph targets must be shown as unavailable.
