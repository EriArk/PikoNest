# Lab 0.0.82 — sound creation and event binding

This is an **implementation/local-validation record**. Console use remains paused:
no device action, installation or new official-runtime playback occurred. There is
no native screenshot pack for this batch and no public APK release.

## What changed

- General SFX editing from Game view and All tools: 32 notes, pitch, volume,
  instrument/effect, rest, next-note copy, transpose, speed and loop settings.
  Selection never writes notes. Controller field editing has explicit keep/revert.
- Recognized Game view projects allocate safe slots and bind start, interaction,
  win/loss events without Lua. Selected-element Sound retains its return context.
  Unknown Lua supports explicit resource edits, with no automatic allocation/wiring.
- Shared-resource review counts event, music and custom-instrument references.
  Independent copies leave original data and other bindings intact. New sound and
  assignment save as one Undo transaction; removing a binding keeps sound data.
- Listen prepares an isolated cart with explicit Stop/replay; Test launches the
  game candidate. Both preserve saved bytes. Field/draft undo, save-error retry,
  source-checked recovery and legacy animation/behavior draft migration are covered.
- Ordinary Lua uses channel 3 and explicit input edges. Later events replace earlier
  sounds; global results follow contacts. Results/held switches do not retrigger
  sound every update, and restart stops that channel before replaying start.

[Tool contract and data limits](../../SFX_EDITOR.md).

## Actual validation

- **79 core suite invocations**, Android compilation and integrated APK packaging
  passed locally. Existing package checks verified one launcher, 263 required
  Godot assets and no bundled official PICO-8 binaries.
- SfxDataTest: **2,222 checks**. LF/CRLF, no-op exactness, targeted nibble edits,
  unchanged uppercase digits/sections, missing data defaults, pitch/instrument
  ranges, muted music/custom-instrument references and advanced-data refusal.
- SoundWorkflowTest: **200 checks**. Create/edit/listen snapshot/Test/save/reopen,
  atomic assignment Undo/Redo, failed save/retry, shared copies, field cancellation,
  recovery/stale-source rejection, unknown Lua, allocation dependencies, contextual
  return, All tools entry and preservation across behavior/animation/layout edits.
- Existing focused checks passed: GameCanvas 16,504; PlayModel 96; GameAnimation
  80,199; GameRelationships 81; GameIdentity 101; GameRulesOverview 100;
  AssetNaming 53. Counts include repeated assertions, not separate user journeys.
- Actual v6 drafts generated with the previous lab-0.0.81 compiled classes restored
  into v7. Animation painting, cancellation of its nested old checkpoint after a
  v7 re-encode, and a behavior form were checked. Older fixture checks also passed.
- **16 host Lua assertions** used Lupa 2.6, with generated `+=` normalized and
  drawing/audio APIs stubbed. They cover start/reset, collect→win call order,
  one-shot loss, held-button toggle behavior, result silence over 100 later updates,
  and audition Stop/replay. This verifies call logic, not PICO-8 audio semantics,
  sound quality or runtime compatibility. Call traces are in validation.json.
- Production GameSurface draw calls were reviewed using temporary Java2D adapters
  at 360×480, 360×800, 620×540 and 960×540 logical sizes. These approximate layout,
  not Android rasterization or physical input. Host diagnostics are not website
  screenshots; no native images were added.

Final **local-only, not installed** APK SHA256:
`025642afe2021af7be0f33059ade1562f61730293511ab0b7e8c592d21046f7a`

[Machine-readable record](validation.json).
The standard local build runs the committed Java suites. SoundWorkflowTest also
accepts an optional output directory for its generated collection/switch/audition
cart fixtures; those are host test fixtures, not device-authored evidence.

## Limits and next work

S01–S03/S05 remain partial. Advanced filter/waveform data and unsupported speed/loop
variants are preserved but cannot be edited. There is no music composition editor,
channel arbitration, range editing or multi-type sound library yet. The event list
handles bindings; the prior rule overview has no sound rows. Removing an interaction
retains its dormant sound for explicit removal. Unknown Lua gets no guessed wiring.

Next: music patterns/composition, shared-SFX editing and explicit channel handling.
When authorized, validate the named collection and hero-free switch journeys with
animation/audio, Listen/Stop/Test/return, recovery, exact export and protected-file
comparison. Obtain native images and physical-controller/novice/owner acceptance.
Issue #17 remains open. The last complete native creation journey remains
[0.0.77](../0.0.77-en/README.md); public APK publication needs separate owner approval.
