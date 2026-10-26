# Nova Avatar — Production Readiness

## Current status

The repository contains a source-level Blender prototype generator, a scene-audit script, a Meshy prompt, and workflow documentation. These files are a starting point for creating and validating the avatar asset; they are not evidence that a final character asset has been generated or tested on a device.

## Confirmed in source control

- Blender prototype generator: `blender/build_nova_prototype.py`
- Blender scene audit: `blender/audit_nova_scene.py`
- Meshy prompt and hybrid pipeline documentation
- Lightweight GitHub Actions checks for Python syntax and required pipeline files

## Not yet verified

- A production-quality, fully rigged Nova character
- A final exported `assistant.glb` / `.glb` asset with facial morph targets
- Successful execution of the generator in a supported Blender version
- GLB import and rendering in the Android application
- Real-time audio-driven lip sync and viseme timing on a Pixel device
- Performance, thermal, memory, and battery measurements on target hardware

## Acceptance gates before calling this production-ready

1. Generate or obtain a properly licensed character model with a humanoid armature and facial controls.
2. Open the source scene in the documented Blender version and run the scene audit.
3. Export a GLB and run the repository's GLB inspection/validation tools.
4. Verify required nodes, animations, skinning, and morph targets in the exported asset.
5. Integrate the asset with the Android renderer and confirm graceful error handling when the asset is missing or invalid.
6. Test on a physical Android device, recording frame rate, load time, memory use, and thermal behavior.
7. Validate viseme mapping against actual speech audio; unit tests alone do not prove perceptual lip-sync quality.

## CI scope

The GitHub Actions workflow checks Python syntax and required files. It intentionally does not claim to run Blender, build the Android app, validate a GLB, or run on-device tests. Those require additional dependencies, a real asset, and suitable test infrastructure.
