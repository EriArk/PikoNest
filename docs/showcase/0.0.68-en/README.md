# Backgrounds in the Workshop — lab 0.0.68

Background strips now appear in **In the game**, beside sprites, maps, animation
and camera. Choose pixels from the cartridge, adjust their movement, duplicate a
layer, change its order or hide it. No Lua cursor is required for this path.

These are original ADB captures from the regular English development APK, on an
isolated Android 15 x86_64 emulator on 7 October 2026. No image edits, capture-only
translation build or APK release. [Download the image pack](../PikoNest-backgrounds-en-0.0.68.zip?raw=true).

| Image | What it shows |
| --- | --- |
| [Background](01-background.png) | A horizontal strip, its sheet region and speed. |
| [Resource selection](02-strip-region.png) | Choose a rectangular region, with D-pad or touch. |
| [Layer order](03-layer-order.png) | Review a move before saving it. |
| [In the game](04-game-uses.png) | Both background uses and a sprite in their drawing order. |
| [Parallax](05-parallax-field.png) | Explicit field editing, with Keep and Revert. |
| [Scene sketch](06-scene-preview.png) | Resource composition before Apply; Test runs gameplay. |
| [Compact layout](07-compact-background.png) | The same form at 720 × 960. |

The other published images are 1240 × 1080. A 1920 × 1080 layout was inspected
as additional local evidence. The imported project **Cloud garden** was prepared
with blank Lua and the existing sprite-sheet data from the
[0.0.61 demonstration](../../design/android-layers-61/tested.p8). The initial
empty game-use list was restored before the journey. Every layer and the final
sprite placement were then created through Android controller events. This is
an editor workflow demonstration, not a complete game or newly painted artwork.
[Starting cartridge](blank.p8) · [Saved result](final.p8).

## Verification

- Full local domain suite and APK build passed. `BackgroundUsesTest` has 106
  focused checks; existing GameUses 90, AnimationUses 81, CameraUses 76,
  UsesEditor 32, BackgroundLayer 71 and BackgroundLayers 95 checks also passed.
- Native journey: create from blank code, choose the strip, change speed, recover
  an unfinished field edit after process recreation, revert it, Apply, Undo/Redo,
  reopen, duplicate, reorder, hide, cancel removal, remove and Undo. Saved bytes
  were compared after history operations. A sprite was added through the same list.
- Draft Test on the emulator exercised the missing-runtime error and return;
  the exact journal and saved cartridge remained unchanged. Core tests verify
  candidate bytes sent to the runtime port without a project write.
- Core coverage includes LF/CRLF/CR, exact no-op round trips, failed saves,
  journal v1–v4 compatibility and new v5 recovery, ordinary `.p8` export/readback,
  camera restoration and refusal to reorder across code, comments or camera calls.
- Both copies refer to the cartridge's existing pixels. Core checks verify that
  painting their shared source updates both previews and that lower-sheet pixels
  keep their standard shared-map relationship. Background operations write no gfx,
  map or audio data.
- The owner's official 0.2.7 manual was checked for `camera`, `sspr`, transparency
  and `time`. The generated strip formula is unchanged from the runtime-verified
  [0.0.60 experiment](../../design/android-background-60/README.md).

The local 0.0.68 APK was installed on Retroid with a data-preserving update.
All 48 pre-existing project/library files retained their SHA-256 hashes. The
music stream is muted. The unrelated emulator was not used.

## Boundaries

This editor recognizes supported flat draw blocks and the existing horizontal
sprite-strip formula. Complex or manually changed formulas are preserved and
refused. Movement is time-based; this batch adds no vertical/map/animated strip
recipes or gameplay event binding. New layers go before the selected use;
duplicates go immediately after their source. With no recognized use in an
existing supported `_draw`, insertion is at its end. Reorder only swaps adjacent
recognized layers separated by whitespace.

The preview illustrates resources with default palette/transparency; it does not
execute arbitrary Lua, text or shapes. Dynamic camera expressions need Test.
The preview camera X changes only the sketch. A background's Y is screen-based;
parallax uses the incoming camera X and restores the camera for following draws.

Successful official-runtime Test/return for this new UI, native export through
the Android picker, physical controller feel and owner visual acceptance remain
open. The emulator has no official runtime. Retroid's existing unknown runtime
session was neither bypassed nor force-stopped; recovery remains issue #10.
Prior runtime evidence does not substitute for this end-to-end acceptance.

Next: C02.4 / connected M1–M2, joining input, state, conditions/actions and resource
uses from blank. UX05 follows those core tools; N03.3 preset expansion stays deferred.
Capture hashes and scope: [capture.json](capture.json).
