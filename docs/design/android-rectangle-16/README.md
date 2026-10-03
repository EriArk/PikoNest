# Rectangle brushes — Android lab 0.0.17

2026-10-03. Actual Retroid Pocket Classic captures, 1240×1080. A bounded sprite
creation tool, not completion of the full graphics editor or new design acceptance.

## Interaction and compatibility

Tool entries 8/9 are Прямоугольник (outline) and Прямоуг. с заливкой (solid).
Choose a color before starting; A sets the first corner, D-pad positions the other
corner and previews pixels plus width×height, A commits. B/Y while drawing cancels
the draft; Y after commit undoes the whole shape. Start saves the pending shape
then launches those bytes, matching the existing line tool. Two touch taps set
the corners and commit. Palette, tabs, menu and other tools cannot discard a draft.

Corners are inclusive and order-independent. A single point, row or column is
valid. Outline preserves its interior; solid fills the entire rectangular area.
Index 0 can be drawn like every other stored palette index. Operations stay inside
the current selection. All supported carts and region sizes use the same tools;
the lab's upper 128×64 sheet boundary remains explicit. No gameplay role is needed.
Ovals, square snapping and persistent undo history remain future work.

The [official manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html)
describes rectangle/filled-rectangle shape tools and `rect`/`rectfill`. This tool
materializes ordinary gfx pixels; it does not insert drawing API calls, execute
game Lua or add a custom runtime dependency. Code and other resources stay intact.

## Device evidence

- [Controller selects outline](tools.png)
- [Initial outline preview](outline-preview.png)
- [Restored two-corner outline](restored.png)
- [12×2 filled preview](filled-preview.png)
- [Saved result after two touch corners](saved.png)
- [Official PICO-8: puzzle completed with the edited sprites](runtime.png)
- [Final build: dimensions remain visible during a draft](final-preview.png)

`remix-0006` (Копия 6) was created through the shelf from `remix-0005`.
All eleven pre-existing project hashes matched before/after. No library assets
or external exported carts were edited. Main input was injected semantic
controller actions through ADB, plus a two-tap touch check. Physical ergonomics
remain for owner acceptance.

1. Selected outline, chose color 10, set (1,1), moved to (14,14). The 14×14
   preview showed the yellow frame; saved cart retained its original hash.
2. R1/Select/X did not leave the draft or change its color. Home → `am kill` →
   absent PID → cold launch restored both corners, color, selection and outline
   tool without writing. B cancelled with the original hash.
3. Drew the same outline in reverse corner order and committed. Pulled cart
   matched expected pixels exactly: 52 changed gfx bytes. Y restored the entire
   original file byte-for-byte. Redrew in forward order with the same result.
4. Selected solid rectangle, color 14, corners (2,14) and (13,15). Preview showed
   12×2; saved file still contained only the outline. Start committed 24 changed
   gfx bytes, then launched the user-supplied official PICO-8 0.2.7. Little Lights
   completed in two moves with all nine edited sprites. Ctrl+Q returned to editor.
5. Y restored the outline-only hash. Taps at the same two corners recreated the
   exact filled result. Pulled final cart matched the composition of both portable
   operations; all non-gfx sections stayed byte-exact. Device remains in Копия 6.

| `remix-0006/game.p8` state | SHA-256 |
| --- | --- |
| Source / cancelled outline / undo outline | `ad8c6aaf1a4acfda33ee045e6898a2b859926227e0a6c2c2e71706456b629448` |
| Outline / undo solid rectangle | `09ef962fae039ed279fa96fe3e1ae5a3f068c987d8ed8626ea06559f61969b8a` |
| Outline + solid rectangle | `b4fe208433fd5b4438b36d0f7fcf59786bfcfea1579d36eb8fbd0b4eb02b0996` |

Device review found the initial instruction toast covered dimensions while moving
the cursor. Final UI suppresses transient toasts during all two-point drafts;
corner/size and A/B instructions remain visible. The final APK was installed in
place and a reverse 12×2 draft was captured immediately: no toast covered its
dimensions. B cancelled, retaining the saved final hash. Earlier captures retain
evidence of the original check.

Final installed version `0.0.17`, versionCode `17`. APK SHA-256:
`fb0f25060277d2e6a255009122be6e7aa6ecc0c3e0e156ed52d58f0e4dff1948`.

## Automated checks

`RectangleWorkflowTest`: 2,949,812 assertions, mostly exhaustive pixel masks over
blank, Lights and Moon Garden; 24×24, 32×24 and 128×64 selected regions, both styles,
colors 0/10, reversed corners, points, rows, columns, untouched interiors/outside
pixels/shared sheet half, CRLF and unknown sections. Workflow checks cover menu
mapping, previews, cancellation, restored drafts, invalid anchors, failed-save
retry, Start launch bytes, whole-operation undo, touch equivalence and picker
return to the selected rectangle brush. All existing core suites, build and
APK signature verification also passed.

Failure uses an injected save-port error, not device storage exhaustion. Undo
history remains in memory; optional persisted draft context reconstructs a preview
against current canonical pixels and never writes automatically. No-op drawing
does not materialize missing gfx rows or add history.
