# Lab 0.0.84 — world, map and camera in Game view

An **implementation/local-validation record**. Console use remains paused: no
installation, new official-runtime execution or native screenshot pack occurred.
No public APK release was created. [Workflow guide](../../WORLD_EDITOR.md).

## Implemented path

From a recognized Game view project, choose field dimensions, paint/erase existing
tiles, choose fixed/follow/room camera behavior and an explicit target, Test an
isolated candidate, review and save the world as one change. Game view pans for
placement/selection outside the first screen and converts touch coordinates into
world positions. Relationship selection pans to its target and returns to the
edited element on confirm/cancel. Removing an active camera target is refused
until its camera is changed; unrelated deletions reindex the target.

Movement uses field bounds; map drawing precedes objects, and HUD uses a reset
camera. Animation, sounds, music, optional names and other cart resources survive.
Map/settings have bounded draft history, field revert, picker/cursor recovery and
one session Save/Undo. Hidden/outside map cells and lower/shared gfx bytes remain.
Unknown Lua is preserved and uses the independent tools without automatic wiring.

## Actual validation

- **82 local core suite invocations** and the integrated Android APK build passed.
  The final small relationship-viewport correction then passed focused world and
  relationship checks; Android compilation/packaging was refreshed without rerunning
  all core suites. Package checks retain one launcher, 263 required Godot assets
  and no official PICO-8 binaries.
- **WorldWorkflowTest: 16,496 assertions** in the final focused run (16,492 in the
  earlier full run). LF/CRLF, hero-free map creation, controller field/picker,
  paint/erase infrastructure, draft Undo/Redo, isolated Test, failed-save retry,
  one commit/Undo, shared-memory preservation, off-screen placement/picking,
  camera targets, map ordering/HUD reset, audio/animation retention, source refusal,
  hidden-cell preservation and exact journal recovery. Most repeated assertions
  compare protected pixels; this is not 16,496 separate user journeys.
- Existing full-run checks included GameCanvas 16,504; PlayModel 96; GameAnimation
  80,199; GameRelationships 81; GameIdentity 101; GameRulesOverview 100;
  AssetNaming 53; SfxData 2,222; SoundWorkflow 200; MusicData 548; MusicWorkflow 218.
- Actual v8 home, behavior and nested animation-paint journals were generated with
  the preceding lab's classes and restored with the new code. Four explicit checks
  cover old All tools focus, exact behavior/animation candidates and nested paint
  cancellation. Existing synthetic fixtures retain v1–5 and versioned readers
  retain v1–8. Current journal version is 9. World data in unrelated form/animation
  drafts is restored from their source-checked base, not another editable copy.
- **17 host Lua assertions**, Lupa 2.6: normalize `+=`, stub PICO APIs, then check
  initial follow/room offsets, map/object order, HUD reset, far-edge clamping,
  world movement bounds, restart, room-boundary crossing and a hero-free fixed map.
  [Traces](validation.json) do not prove official rendering, timing or input.
- Production GameSurface draw calls were reviewed through temporary Java2D
  adapters at 360×480, 360×800, 620×540 and 960×540 logical sizes. Long compact
  hints were replaced with selected-action explanations. These are approximate
  host layouts, not Android captures or physical-controller acceptance.

Final local-only APK SHA256 is recorded in [validation.json](validation.json).
It was **not installed**. `WorldWorkflowTest` accepts an optional output directory
for fixed/follow/room `.p8` fixtures. These are generated host-test fixtures, not
native-authored export evidence. The normal local build runs the committed suite.

## Remaining scope and next work

The connected brush currently uses existing tiles, one map origin and the upper
32 rows, with dimensions in full screens. The existing independent map tool keeps
its broader region/fill/shared-memory operations. This is a PikoNest workflow
slice, not a PICO-8 capacity claim. Drawing new tile art inside the map context is
next, followed by two small editable starters using the same general tools.
Collision, door/trigger transitions, parallax/easing, map layers, broader resource
reuse and advanced audio remain unfinished.

Official fixed/follow/room execution, Test/return, independent exported-cart
playback, physical compact/wide controls, recovery on a device and novice/owner
review remain open, together with the earlier visual/animation/audio journeys.
Issue #17 is not closed. [0.0.77](../0.0.77-en/README.md) remains the last complete
native creation journey. The separate owner APK release gate remains in force.
