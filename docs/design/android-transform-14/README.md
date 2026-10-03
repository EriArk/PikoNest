# Sprite transformations — Android lab 0.0.15

2026-10-03. Actual Retroid Pocket Classic captures, 1240×1080. This is a bounded
sprite-tool slice, not completion of the editor or new visual-design acceptance.

## Interaction

Open a sprite/region → tool selector → Отразить / повернуть. Left/right (or up/down)
browses horizontal mirror, vertical mirror, clockwise quarter turn and half turn.
Each preview derives from the same saved source. A applies, B cancels, Y afterwards
undoes the entire change. The existing A/B swap applies. Touch arrows and footer
actions call the same portable session. No keyboard is needed for this workflow.
Start, menu, tabs and unrelated actions cannot bypass the preview. Selected brush,
cursor, region and zoom stay intact. Changes affect all uses of those gfx pixels.

The lab edits only the upper 128×64 sprite sheet. Mirrors and half turns work on
any supported rectangle, including regions larger than 16×16. Quarter turns require
a square: a rectangular selection displays an explanation and cannot apply 90°.
These are tool limits, not console limits. Arbitrary-angle rotation, resized
destination placement and animation editing are not implemented by this slice.

## Evidence

- [Tool entry](tools.png)
- [16×16 quarter-turn preview](rotate-preview.png)
- [Preview restored after process death](restored-preview.png)
- [24×16 mirror preview](rectangle-preview.png)
- [Explicit rectangular quarter-turn limit](rectangle-quarter-turn.png)
- [Official PICO-8: transformed puzzle completed](runtime.png)
- [Final installed build](final-preview.png)

Device input was injected through ADB using semantic controller buttons. Physical
ergonomics remain for owner testing. The main device scenario needed no touch;
the final build additionally received taps on both preview arrows.

A new `remix-0004` (Копия 4) was created through the shelf from `remix-0003`.
The nine pre-existing projects were hashed before/after and all stayed unchanged.
No library asset or external exported file was edited.

1. Opened tools and selected clockwise 90°. Saved cart remained byte-exact.
2. Sent Start/Y/R1/Select while previewing. No write or runtime escape occurred.
3. Home → `am kill` → confirmed absent PID → cold reopen. Same operation/region
   restored; canonical file unchanged. B cancelled with the same source hash.
4. Applied 90° once. Pulled actual cart matched the portable transformation byte
   for byte: exactly 88 changed gfx bytes, every other section unchanged.
5. Y restored the original SHA-256 exactly. Reapplied 90°, launched the official
   user PICO-8 0.2.7 through the existing runtime adapter, completed Little Lights
   in two moves. All nine cells displayed the rotated cat. Returned with Ctrl+Q.
6. Selected 24×16. Mirror preview included the entire selection. On 90°, A made
   no write. Returned to horizontal mirror, applied once, then Y restored exactly
   the pre-mirror rotated cart.
7. Undid the remaining quarter turn and reopened a 16×16 90° preview. Installed
   the final APK in place; it restored this preview with the original saved hash.
   The device is left at that uncommitted preview in Копия 4.

Final installed version: `0.0.15`, versionCode `15`. APK SHA-256:
`d712f48b3663fb8e9eab6c6d5f73f83e42f9b5c829a94b0d1180472728a0c8bf`.

Hashes for `remix-0004/game.p8`:

| State | SHA-256 |
| --- | --- |
| Source / cancelled preview / undo quarter turn | `ff13fa507c3084cf692f92893627a80524e8a8efd6a1b59e2a5fca04b229bcd3` |
| 16×16 clockwise turn / undo rectangle mirror | `29a809e0f1f68d02dee8c4a6be3022557c15c4dccd824d71d4be3ebd3f187d62` |
| 24×16 horizontal mirror after turn | `35f93bdc9bf0f32e4500358f15937fa91d8a81b85e210c7ce52c0bff436f5af4` |

## Portable checks and recovery boundaries

`TransformWorkflowTest`: 557,588 assertions, mostly exhaustive pixel checks across
blank, puzzle and Moon Garden fixtures. Covers 24×24, 32×24 and 128×64 regions,
all supported operations, independent coordinate mapping, untouched outside
pixels/shared map half, unknown sections/CRLF, no-op missing gfx, uppercase hex,
whole-edit undo, launch bytes, unsafe rectangular 90°, invalid restore metadata,
pending lines, action isolation, failed-save retry and read-only restoration.
All existing Android-host core suites passed; build and APK signature validation
passed. Save failure is tested with a failing port, not by filling device storage.

Only the operation/selection context persists. On restoration the preview is
recomputed from current canonical pixels and must be confirmed. Completed-edit
undo history remains in memory as in existing lab tools; this step does not add
persistent history. No-op edits do not write or consume an undo entry.

The transform writes ordinary gfx data. It does not add a Lua helper or custom
runtime dependency. Separately, standard `spr`/`sspr` support drawing-time flips;
those do not edit the sheet. See the [official PICO-8 manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html#SPR)
for that distinct runtime API. Collision bindings and code are not rewritten by
pixel transformations.
