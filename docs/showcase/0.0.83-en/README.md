# Lab 0.0.83 — music composition and shared audio editing

An **implementation/local-validation record**. Console use remains paused: no
installation, native audio journey or new official-runtime execution occurred.
There is no new native screenshot pack and no public APK release.

## Implemented path

Music opens from Game view and All tools. Select a pattern and part, create or
choose a phrase, edit its notes, set sequence loop/stop flags, copy/clear a pattern
with confirmation, choose a start binding/channel policy, review and save once.
Independent phrase copies preserve the original; pattern copies share SFX until
explicitly changed. Unknown Lua supports explicit resource editing, not guessed
allocation or wiring. Nested Listen auditions the phrase; sequence Listen plays
from the selected pattern; Test uses the game candidate without saving.

Three-part music explicitly mutes part 4 along the reachable track, preserving its
SFX references/data. Four-part music keeps it and discloses interruption by effects.
Start/reset and win/loss use ordinary music()/sfx() calls, with music stopped before
result effects. A single Undo covers notes, patterns and assignment; recovery keeps
the nested note field and its revert value. [Tool contract](../../MUSIC_EDITOR.md).

## Actual validation

- **81 core suite invocations**, Android compilation and integrated APK packaging
  passed. Package checks verified one launcher, 263 required Godot assets and no
  bundled official PICO-8 binaries.
- **MusicDataTest: 548 assertions.** LF/CRLF, missing defaults, exact no-op/targeted
  edits, all SFX/channel values, flags, muted-reference retention, backward loop
  reachability, stop precedence, fourth-part conflicts, advanced/invalid data safety.
- **MusicWorkflowTest: 218 assertions.** Nested phrase creation/editing/copy,
  field recovery/revert, shared data preservation, pattern flow/copy/clear,
  policy selection and conflict rejection, isolated audition/Test, atomic save,
  failed-save retry, exact Undo/Redo, discard, unknown Lua, source checks,
  optional names, behavior/animation retention and All tools return.
- Existing focused checks passed: GameCanvas 16,504; PlayModel 96; GameAnimation
  80,199; GameRelationships 81; GameIdentity 101; GameRulesOverview 100;
  AssetNaming 53; SfxData 2,222; SoundWorkflow 200. Counts include repeated assertions,
  not separate user journeys.
- Actual v6/v7 drafts generated with previous lab81/82 classes recovered, including
  old nested animation paint checkpoints after re-encoding, behavior forms and v7
  sound assignments. Fixture coverage also retains v1–5 readers. Journal is now v8.
- **Seven host Lua assertions** used Lupa 2.6 with `+=` normalized and audio calls
  stubbed: start/reset ordering, no per-frame restart, music stop before contact and
  win/loss SFX, and held restart behavior. Traces are recorded in validation.json.
  This proves call order only, not official audio timing, fidelity or channel behavior.
- Production GameSurface draw calls were reviewed with temporary Java2D adapters
  at 360×480, 360×800, 620×540 and 960×540 logical sizes. Clipped channel-choice labels
  were shortened. These approximate host layouts are not Android screenshots or
  proof of physical-controller ergonomics.

Final **local-only, not installed** APK SHA256:
`38a8e57b5db0f3d078cc5fdfb347a3f8e3cd22c7dd4435662bb266d0a5ab065b`

[Machine-readable record](validation.json). The standard local build runs the
committed Java suites. MusicWorkflowTest accepts an optional output directory for
composition and event-call fixtures; these are generated host tests, not native
authored-game evidence.

## Remaining scope

S01–S05 remain partial: range insertion/reordering, cross-project music/SFX reuse,
advanced SFX data, playback position and further music-event bindings are unfinished.
Native official playback, loop/timing comparison, mute/Stop/return, independent
exported-cart audio and physical/novice/owner acceptance remain open. Issue #17
also remains open. The last complete native creation journey is [0.0.77](../0.0.77-en/README.md).

Next connect maps/camera to the simpler Game view workflow. Once device access is
authorized, complete the outstanding visual/animation/audio journeys and repair
observed issues. Existing acceptance and the separate owner release gate remain.
