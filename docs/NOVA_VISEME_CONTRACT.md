# Nova GLB facial animation contract

Nova uses a stable semantic contract between the 3D character asset and the speech animation layer. The exact exported names must be read from the candidate GLB; never assume names from the Blender source survive export unchanged.

## Required talking controls

| Semantic group | Suggested target names | Purpose |
|---|---|---|
| AA | AA, viseme_AA, mouth_AA, jawOpen | Open jaw / broad vowel |
| EE | EE, viseme_EE, mouth_EE | Spread lips |
| IH | IH, viseme_IH, mouth_IH | Narrow-open vowel |
| OH | OH, viseme_OH, mouth_OH | Rounded open lips |
| OO | OO, viseme_OO, mouth_OO | Tight rounded lips |
| FV | FV, viseme_FV, mouth_FV | Teeth-to-lower-lip approximation |
| MBP | MBP, viseme_MBP, mouth_MBP, lipsClosed | Closed lips |
| L | L, viseme_L, mouth_L | L-like tongue/lip pose |
| CHSH | CHSH, viseme_CH, viseme_SH, mouth_CH | CH/SH approximation |

The Blender inspection script recognizes aliases for audit convenience. The Android renderer should map actual GLB morph target names to these semantic groups at load time, log missing mappings, and continue with neutral/available shapes instead of crashing.

## Animation clips

Preferred clip names: Idle, Listening, Thinking, Speaking, Gesture_Explain, Gesture_Greet. Names may differ; enumerate clips from the loaded asset and keep missing optional gestures non-fatal.

## Runtime rules

- Keep viseme values normalized to 0..1 and clamp before applying.
- Blend neighboring visemes smoothly; avoid abrupt one-frame transitions.
- Clear the mouth pose when speech finishes, is interrupted, or the assistant enters an error/paused state.
- Do not use body animation to drive facial visemes.
- A text heuristic is only an approximation; true phoneme alignment needs timed phoneme/viseme data from the speech pipeline.
- Avoid logging raw microphone audio or private conversation text in diagnostics by default.

## Acceptance checks

- Each available viseme visibly changes the mouth in a preview.
- A full speech timeline transitions without snapping or stuck-open lips.
- Idle/listening/thinking/speaking clips do not freeze the face.
- The GLB opens in Blender and passes the Khronos glTF Validator.
- The app renders the asset on a physical Pixel 6 without crashes and with acceptable memory/thermal behavior.
