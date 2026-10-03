# Color replacement — Android lab 0.0.16

2026-10-03. Actual Retroid Pocket Classic captures at 1240×1080. This is a bounded
tool addition; the full editor, maps, animation and audio tools remain unfinished.

## Interaction and scope

Select a sprite/region → sprite tools → Заменить цвет (entry 7). The menu scrolls
with D-pad or a touch swipe. In the preview, D-pad moves through a numbered 8×2
palette, X switches source/target, A applies and B cancels. Touch selects the field
and swatch. Y after applying undoes the entire operation. The source initially
comes from the pixel under the cursor; target comes from the brush color. The
active field is highlighted. Count and before/after pictures update immediately.

Replacement affects all matching pixels in the selection, including disconnected
parts. Browsing never accumulates temporary replacements. Existing brush, cursor,
selection and zoom survive. Start/menu/tabs cannot bypass the draft. Color 0 is
supported in both fields; the UI explains its usual in-game transparency.

The lab still edits only the upper 128×64 sheet. Larger/rectangular selections
within that area use exactly the same operation. All supported carts have the
tool, regardless of templates or hero bindings. This edits stored gfx indices;
it neither changes the display palette nor generates Lua. The preview uses the
standard colors/checkerboard and does not execute the cart's `pal`/`palt` calls.
The official runtime remains the authority for actual in-game appearance.

The [official PICO-8 manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html)
describes color search/replace in the sprite editor and index 0 transparency
under `spr`/`palt`. The UI approach here is handheld-specific; stored data remains
ordinary PICO-8 gfx with indices 0–15.

## Device evidence

- [Tool selected by controller](tools.png)
- [Source 8 → target 12 preview](preview.png)
- [Same pair and active source field restored after process death](restored.png)
- [Official PICO-8: puzzle completed with blue details](runtime.png)
- [Seventh menu entry reached with a touch swipe](touch-menu.png)
- [Replacing blue with index 0](zero-preview.png)

Installed version `0.0.16`, versionCode `16`. APK SHA-256:
`7c0bc4f8f25cbd6819581fe9acd56703c44f600362859552a9322d42d77f218b`.

Created `remix-0005` (Копия 5) through the shelf from `remix-0004`. All ten
pre-existing project hashes matched before/after; no library record was edited.

1. Controller menu and palette produced an 8 → 12 preview; saved file stayed
   unchanged. X switched the active field; source selection round-trip restored 8.
   Start, Y, R1 and Select could not leave or save the preview.
2. Home → `am kill` → absent PID → cold reopen restored the pair, active field,
   selected region and preview. B cancelled with byte-identical source.
3. Applied 8 → 12 once. Pulled actual cart matched the portable operation exactly:
   18 gfx bytes changed from `8` to `c`; all other sections stayed byte-exact.
4. Y returned the entire file to its original SHA-256. Reapplied, launched the
   user's official PICO-8 0.2.7 and completed Little Lights in two moves. All nine
   cells showed the blue details. Ctrl+Q returned to the same project/editor.
5. Swiped the menu to its seventh entry, tapped it and tapped target index 0.
   The preview showed transparent replacement. A applied; Y restored the exact
   blue cart. Device is left in the editor with this saved blue variant.

| `remix-0005/game.p8` state | SHA-256 |
| --- | --- |
| Source / cancel / undo blue replacement | `ff13fa507c3084cf692f92893627a80524e8a8efd6a1b59e2a5fca04b229bcd3` |
| 8 → 12 / undo replacement with 0 | `ad8c6aaf1a4acfda33ee045e6898a2b859926227e0a6c2c2e71706456b629448` |
| 12 → 0 | `f8d49dd30f6871950d891e25d60cc7548c626190dcc25af7372b6a60fdf4eb48` |

Main workflow uses injected semantic controller buttons through ADB. Touch is
additionally tested through injected swipe/taps. Physical ergonomics and owner
acceptance remain distinct from these automated device checks.

## Tests and recovery

`RecolorWorkflowTest`: 738,035 assertions, chiefly exhaustive pixel checks over
blank, Lights and Moon Garden fixtures; 24×24, 32×24 and 128×64 regions; zero as
source/target, identity/absent-color no-ops, disconnected matching pixels, exact
changed counts, untouched outside pixels/shared sheet half, CRLF/unknown sections,
uppercase gfx and mixed row endings. Also covers modal action isolation, default
colors, palette row wrapping, non-cumulative previews, context preservation,
failed-save retry, one-operation undo, launch bytes and invalid restore metadata.
All existing core suites passed, as did build and APK signature verification.

Write failure is tested with a failing save port, not by filling device storage.
Restoration recomputes a read-only candidate from current canonical bytes and
requires confirmation. Undo history remains in memory, as with other lab tools.
No-op edits cause neither file writes nor additional undo entries.
