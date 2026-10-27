# Timed viseme timeline integration

## What is implemented

`SpeechVisemeTimeline` accepts frames with relative millisecond timestamps and semantic morph weights. It normalizes common labels (`jawOpen`, `viseme_A`, `viseme_O`, `viseme_MBP`, blink and smile aliases), clamps weights to 0..1, sorts frames, keeps the last frame at duplicate timestamps, and linearly interpolates between frames.

The Filament renderer exposes:

- `playVisemeTimeline(frames: List<VisemeFrame>)` — starts speaking state and samples frames against a monotonic clock.
- `clearVisemeTimeline()` — removes the supplied timeline.
- During timeline playback, matching morph targets are driven by sampled weights. Without a supplied timeline, the existing jaw-wave fallback remains approximate.

## Example

```kotlin
renderer.playVisemeTimeline(
    listOf(
        VisemeFrame(0, mapOf("jawOpen" to 0.0f, "viseme_M" to 0.8f)),
        VisemeFrame(90, mapOf("jawOpen" to 0.6f, "viseme_A" to 0.9f)),
        VisemeFrame(180, mapOf("jawOpen" to 0.25f, "viseme_O" to 0.7f)),
        VisemeFrame(280, mapOf("jawOpen" to 0.0f))
    )
)
```

Only morph targets present in the imported GLB can move. Use the semantic names supported by `SpeechVisemeTimeline.canonicalName` or extend the alias map for the rig.

## Important limitation

The renderer does **not** derive phoneme timestamps from generated text or Android TTS audio. The caller must provide timed viseme frames from a TTS engine that exposes alignment, a forced-aligner, or an external animation authoring step. The current Android TTS flow still uses approximate procedural jaw movement unless a timeline is explicitly supplied. A timeline unit test proves interpolation logic, not perceptual lip-sync.

## Validation

Unit tests cover interpolation, clamping, duplicate timestamps, unknown morph labels and aliases. Run:

```bash
gradle --no-daemon :app:testDebugUnitTest
```

Then test with the actual rigged GLB and speech audio on a physical device. Confirm each morph target is visible on the face and reset to neutral when speech ends.
