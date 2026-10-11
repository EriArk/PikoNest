# PikoNest 0.0.76 — draw and place from Game view

Six unedited English screenshots from Retroid Pocket Classic, Android 14,
captured on 11 October 2026. This is a local single-APK development build,
not a release or owner acceptance. Original mushroom art was painted through
the native interface; no cartridge code or graphics were injected into the app.

## A connected first step

New Blank projects open in **Game view**. Add a sprite, draw its appearance,
then put it on the game canvas. PikoNest chooses a safe sprite-sheet location
and writes an ordinary `sspr()` call. No sprite number, sheet address, callback
or coordinate variable needs to be entered.

The Elements list and canvas select the same placement. Its actions are
Appearance, Move, Duplicate and Remove. Duplicate initially shares the image;
editing shared pixels offers **Edit all uses** or **Make independent**. A map
reference also prompts this choice. Removing a placement retains its pixels.
All tools remains accessible, including code, maps and the existing libraries.

Drawing and placement are one pending change. Done saves them together; one
Undo removes both. Appearance edits and moves also save as whole operations.
Back from placement keeps the drawing; Discard changes abandons it. Test runs
the current draft separately from the saved cartridge and returns to its context.
Draft recovery checks the exact source before restoring pixels and position.
Storage failures retain the pending operation and do not consume Undo history.

Directions navigate or move the cursor; A paints/selects/confirms; B returns to
drawing options or cancels placement; Select exposes options; Start tests.
Undo and Redo are also focusable list actions, without requiring shoulder buttons.
Swapped A/B labels follow the existing controller preference. Touch painting and
placement are optional. During an interrupted touch gesture the current appearance
remains an unsaved draft; Discard abandons the whole appearance operation.

## Demonstrated on the device

- Created Blank project 14 through the normal project shelf. Drew and placed
  the pink mushroom; tested it while the saved `.p8` was still the untouched Blank.
- Exited official PICO-8 to the exact placement context. Closed the app process,
  reopened the project and recovered byte-identical draft data. Saved once,
  undid the whole addition, and restored it byte-for-byte with Redo.
- Duplicated the mushroom and made the second appearance independent. Painted
  it blue; the pink sprite remained unchanged. The saved pair ran in official
  PICO-8: all 16,384 sampled frame pixels matched the ordinary cartridge data.
- Installed the final label/pointer/recovery correction. Reopened the same
  project, painted with injected controller actions and discarded that edit.
  Duplicated, inspected shared-use choices, and undid the duplicate exactly.
- All 52 previous project/library files remained byte-identical. Media stayed
  muted; the runtime session ended normally. Existing independent apps were kept.

72 local core suite invocations passed. Final focused GameCanvas tests passed
16,504 checks, covering source refusal, resource reservations, all four offered
sizes, recovery, storage failure, linked/independent edits and shared-map safety.
The integrated APK has one launcher and contains no purchased runtime binaries.
See [capture.json](capture.json) for artifact hashes and capture provenance.
The runtime image predates the final small UI/input/recovery corrections;
the cartridge generation and rendered content are identical.

## Scope still open

This is **partial UX07**, not the complete beginner creator. Game view currently
recognizes a whole-source subset: one flat `_draw()`, one numeric `cls()`, and
static supported `spr()`/`sspr()` placements. Unknown Lua, state/rules, maps,
camera and animation use All tools; adding those connections to Game view is
the next slice. The static subset is a PikoNest editing limitation, not a PICO-8 rule.

Automatic allocation uses only the upper sprite sheet and reserves drawn
regions (including transparent ones), nonzero pixels, map references, flags and
tile zero. It does not infer availability from blank pixels alone. Newly drawn
sizes are 8×8, 16×16, 32×16 and 32×32; the underlying resource model is broader.
Direct lower-sheet appearance changes stay in the existing shared-memory editor;
Game view can instead create an independent upper-sheet copy. General allocation,
library-to-Game-view insertion, richer painting, names, behaviors, rules, audio,
compact-device validation and broader layouts remain unfinished.

Undo/Redo history lasts for the current editor session; draft recovery survives
process closure. ADB key/touch evidence is not physical-controller or novice
acceptance. The ordinary saved cart below was read back from the app; this batch
did not repeat SAF export or independent execution outside PikoNest.

## Images and cartridge

| File | What it shows |
| --- | --- |
| [01-game-view.png](01-game-view.png) | Two independently colored sprite placements |
| [02-sprite-actions.png](02-sprite-actions.png) | Actions beside the selected element |
| [03-appearance.png](03-appearance.png) | Direct appearance editing without a sheet address |
| [04-shared-appearance.png](04-shared-appearance.png) | Shared editing or an independent copy |
| [05-place-sprite.png](05-place-sprite.png) | A duplicate positioned before saving |
| [06-official-runtime.png](06-official-runtime.png) | The saved pair running in official PICO-8 |
| [two-mushrooms.p8](two-mushrooms.p8) | Exact ordinary saved cartridge, original artwork |

Compatibility reference: [official PICO-8 manual, graphics and memory](https://www.lexaloffle.com/dl/docs/pico-8_manual.html).
