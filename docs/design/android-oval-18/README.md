# Oval brushes — Android lab 0.0.19

2026-10-03. Actual Retroid Pocket Classic captures, 1240×1080. This is a bounded
sprite-tool addition, not completion or owner acceptance of the full editor.

## Interaction and PICO-8 behavior

Овал and Овал с заливкой are entries 10/11 in the scrolling sprite-tool menu.
Choose the color first. A sets one corner of the bounding rectangle; D-pad moves
the opposite corner with live dimensions/pixels; A applies. B/Y while drawing
cancels the draft. Start saves the draft then tests the saved cart. Two touch taps
use the same endpoints. Undo/redo works on the whole shape. Color 0, reverse corner
order, odd/even sizes and degenerate points/rows/columns are supported. Drafts trap
palette, history, tab and menu actions and recover after process recreation without
writing. Circle snapping is future work; the lab upper 128×64 editing boundary
remains a prototype limit, not a PICO-8 sprite capacity rule.

The [official manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html#Sprite_Editor)
describes outlined/filled oval tools and bounding-rectangle `oval`/`ovalfill` calls.
PIKOOS stores normal indexed gfx pixels; it does not insert calls or introduce
runtime dependencies. The reference output below targets the owner's version 0.2.7.

## Independent raster reference

The owned [diagnostic cart](../../../experiments/runtime-smoke/oval_oracle.p8)
ran in the user-supplied official PICO-8 0.2.7 RasPi runtime on the Retroid.
It draws each oval on a zeroed screen and reads its pixels through `pget`.
Widths: 1–32, 63, 64, 65, 95, 96, 127, 128. Heights: 1–32, 47, 48, 63, 64.
Both outline and fill are sampled at the origin and with reversed endpoints
translated to the bottom-right corner of the upper 128×64 area: **5,616 masks**.

The reference hashes in
[`oval-0.2.7.sha256`](../../../experiments/android-host/tests/fixtures/oval-0.2.7.sha256)
are derived from runtime pixels, independently of the Java implementation. Each
record is `filled,width,height,variant SHA256(mask)`, where mask is row-major
ASCII `0`/`1`, with no newline included in the hash. Variant 1 uses offset
`(128-width, 64-height)` and reverses both corners. `OvalWorkflowTest` creates
the same geometry in an ordinary blank cart and compares each pixel-mask hash.
All 5,616 comparisons pass. This samples the supported range; it is not an
exhaustive assertion about arbitrary coordinates or future runtime versions.

To reproduce, launch the diagnostic through the existing external runtime adapter
or the documented VIEW entry point. The current wrapper captures `printh` in
`/sdcard/Documents/pico8/logs/shim.log`. Wait for `pikoos oval oracle complete`,
pull that log before another runtime launch, select lines matching
`^[01],\d+,\d+,[01]:[01]+$`, verify 5,616 unique keys, then hash only the text
after the colon as ASCII. The cart prints no project data and edits no project.
Proprietary runtime files are neither part of the fixture nor bundled in the APK.

## Editor and device evidence

- [Controller chooses the filled oval](tools.png)
- [14×12 filled preview](filled-preview.png)
- [Draft recovered after process death](restored-draft.png)
- [White outline over the blue fill](outline-preview.png)
- [Saved result after touch endpoints](saved.png)
- [Official PICO-8 puzzle completed with the new oval sprite](runtime.png)

Created `remix-0008` (Копия 8) through the shelf from `remix-0007`. All thirteen
pre-existing project hashes matched before/after. Verification used injected
controller key events plus two touch taps; physical ergonomics remain for owner
acceptance. Existing assets and external exports were not edited.

1. Cleared only the copied first 16×16 sprite using the existing filled rectangle
   and color 0, leaving the rest of the cart intact.
2. Selected the new filled oval and color 12. Set (1,2), moved to (14,13), and
   observed a 14×12 preview. R1/Select/R2/X did not change the draft or saved file.
3. Home → `am kill` → absent PID → cold launch restored the selected brush,
   color and both corners. B cancelled with the exact cleared hash. Committing
   the same shape in reverse order saved it once; Y returned the cleared hash,
   and R2 restored the filled version.
4. Selected outline and color 7 with the same bounds. Start committed the white
   outline and launched official PICO-8. Little Lights completed in two moves
   with all nine instances showing the new oval sprite; Ctrl+Q returned to editor.
5. Y removed only the outline. Two taps at reversed corners recreated exactly
   the saved outlined version. Pulled cart bytes matched clear + fill + outline
   through the portable model; all other sprite pixels and non-gfx sections were
   byte-exact. Device remains in Копия 8 with the outlined blue oval saved.

| `remix-0008/game.p8` state | SHA-256 |
| --- | --- |
| Cleared sprite / cancelled preview / undo fill | `02f60e41dd1b90dcc67d30f65a92739afee620af02445d8ff3e3a82f83ea7b32` |
| Blue filled oval / undo outline | `b22ab9ea38a68b9e4b51d938705c11d723b71da55ccd9e497cd819e3b3abf1ff` |
| Blue fill + white outline / touch result | `f199c40b36ac6d724dc68ecbd4dfae7460631b77bb9ee6d6cecc287ff895aff1` |

Installed version `0.0.19`, versionCode `19`. APK SHA-256:
`0736de9c0ee9d55bdca17cfeec2dbb0cedf1c8fb50b536631367222b7c36db53`.

## Automated checks

`OvalWorkflowTest`: 100,573 assertions, including all 5,616 official masks plus
workflows across blank, Lights and Moon Garden carts. Covers scrolled controller
selection, translated 32×24 regions, preview isolation, trapped navigation/redo,
cancel, restored anchors, injected save failure/retry, Start launch bytes, unknown
sections/CRLF, untouched surrounding pixels/shared map half, whole-operation
undo/redo, reversed touch corners, picker return, no-op zero and invalid inputs.
All existing core suites, build and APK signature verification passed. No failure
was induced by filling device storage. History remains in memory; optional draft
preferences restore intent against current saved pixels.
