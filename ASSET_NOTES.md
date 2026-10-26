# ASSET NOTES — AI Talking Avatar (Nova)

## Model Specification
- **Format:** glTF 2.0 Binary (.glb)
- **Target Platform:** Google Pixel 6 & Android 12+ (API 31+)
- **Renderer:** Google Filament (`ModelViewer`, `FilamentRendererManager`, `GltfAssetLoader`)
- **Default Asset:** `assistant.glb` bundled in app assets and copied to internal storage on first run.

## Rigging & Animation Standards
- **Skeleton:** Standard humanoid skeleton hierarchy (`Root`, `Pelvis`, `Spine`, `Chest`, `Neck`, `Head`, `Clavicles`, `Arms`, `Hands`, `Legs`, `Feet`).
- **Morph Targets (Blend Shapes):** Supports facial blend shapes for jaw open/close, smiles, blinks, and visemes.
- **Animation Clips:** `Nova_Idle`, `Nova_Listening`, `Nova_Thinking`, `Nova_Greeting`, `Nova_Speaking` with automatic fallback mapping.

## Import Instructions
1. Navigate to **Settings** -> **Character Asset**.
2. Tap **Import .glb** to select a mobile-optimized, rigged humanoid glTF 2.0 binary asset.
3. The app validates asset readability and registers the mesh into the Filament scene.
