# Pixel-fragment movement — Android lab 0.0.20

2026-10-03. Actual Retroid Pocket Classic captures, 1240×1080.

Tool entry 12, Перенести фрагмент, uses first corner → opposite corner → new
position inside the opened sprite/region. D-pad steps one pixel, A confirms each
stage. Touch sets position and the footer A confirms it. B moves back one stage;
Y cancels the whole draft. Other tools, palette, runtime, menu and redo are trapped.
The preview shows source pink, destination yellow, dimensions and displacement.
All source pixels are captured before clearing to index 0 and replacing the
destination, including its zero pixels. Overlap is safe. No clipping or wrapping
occurs at scope edges. The operation changes gfx only, with one-step undo/redo.

The [official manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html#Sprite_Editor)
supports pixel selections and cut/paste, and distinguishes them from looping
sprite shifts. This bounded cut/place workflow does not implement wrapping,
cross-sprite relocation, map-reference updates or clipboard interoperability.
The upper 128×64 editor limit remains. General tools do not require a hero.

## Evidence

- [Controller selects the operation](tools.png)
- [Overlapping 14×12 preview, shifted by (1,1)](preview.png)
- [Same draft after cold process recreation](restored.png)
- [Official PICO-8 puzzle completed with the moved sprite](runtime.png)

Created `remix-0009` (Копия 9) from `remix-0008` using the shelf. All fourteen
pre-existing project hashes matched before/after. Input was injected controller
events plus touch positioning; physical ergonomics remain for owner acceptance.

1. Selected local pixels (1,2)–(14,13), a 14×12 fragment, and moved it one pixel
   right and down. Source and target overlap substantially. Preview showed the
   whole future sprite; no bytes had been written.
2. Start/R1/R2/X/Select did not commit, leave or change the draft. Home →
   `am kill` → absent PID → cold launch restored source, destination and phase.
   Y cancelled; canonical bytes retained the source hash.
3. Reopened and selected the same source/destination by touch, confirming each
   step with A. Save produced the expected overlapping move. Y restored the
   whole original file; R2 restored the moved file. Start launched the saved cart;
   Little Lights completed in two moves. Ctrl+Q returned to the editor.
4. Pulled actual source/result carts and compared to the portable move operation:
   byte-exact match, with all non-gfx sections unchanged. The device remains in
   Копия 9 with the moved sprite saved.

| `remix-0009/game.p8` state | SHA-256 |
| --- | --- |
| Source / cancelled draft / undo | `f199c40b36ac6d724dc68ecbd4dfae7460631b77bb9ee6d6cecc287ff895aff1` |
| Moved / redo / runtime input | `381ffa84a4df87f43099928667db2c6aeb40ecfbb63f6f7527c600a584e2bba9` |

Installed `0.0.20`, versionCode `20`. APK SHA-256:
`9b995976f9acf766b567e22ccd68eab5d2a618383f9f0b56397fe769c59c21ea`.

## Automated checks and limits

`MoveWorkflowTest`: 393,345 assertions across blank, Lights and Moon Garden.
Independent per-pixel expectations cover disjoint, overlapping, same-place and
four-direction moves with nonuniform data containing zeros; untouched pixels,
CRLF and opaque sections; controller stages, trapped actions, cancellation,
recovery, injected save failure/retry, runtime bytes, undo/redo, no-op preservation,
boundary clamping and invalid/shared-map recovery. All previous suites, APK build
and signature verification passed. Failed saves use an injected port error.

History remains session-local. Only draft intent is persisted, and restoration
never writes automatically. Persistent project folders, the first-run runtime
wizard and the remaining editors are part of the [completion path](../../ROADMAP.md#completion-path),
not completed by this graphics slice.
