# Rectangular sprite areas / Android lab 0.0.6

Real ADB captures from Retroid Pocket Classic, 1240×1080, 2026-10-02.
All tracked editor captures use the final installed build; no compositing.

- [Selection](selection.png): choosing a 32×24 rectangle around the saved cloud.
- [Editor](editor.png): that rectangle at its natural aspect ratio, with the
  existing line/fill/picker tools and controller-accessible zoom.
- [Restored preview](restored-preview.png): zoom and an unfinished line after
  the host process was killed and recreated. The pink diagonal is still a draft.
- [Project runtime](project-runtime.png): the saved game still runs with its
  existing comet hero. Editing a sheet area does not automatically bind it.
- [Official rendering check](official-runtime.png): a separate ordinary test
  cart draws the exact saved cloud via `spr` and `sspr`, in official PICO-8 0.2.7.

## Device checks

Backed up all three projects before updating. Drawing used `Копия 1`
(`remix-0001`); the original `moon-garden` and `garden-0001` stayed byte-identical.

1. Reached **Область листа** using injected controller keys; chose corners at
   cell 8,3 and cell 11,5, selecting pixels 64,24 through 95,47. Confirming this
   selection and changing color left the original cart hash unchanged.
2. Drew the cloud using two-touch-endpoint lines, a connected fill and palette
   selection. The resulting file has 43 gfx rows rather than 16. Its 336 new
   nonzero pixels are all inside the selected rectangle. Original gfx rows,
   Lua and header are byte-identical; blank intervening rows are ordinary zeros.
3. Reached zoom with D-pad/Confirm. Started a line at 24,13 and moved the
   endpoint to 29,18, beyond the old 16-pixel boundary. Backgrounded and killed
   host PID 18743, checked it absent, then reopened as PID 18870. Region, tool,
   color, zoom, cursor and draft anchor survived. Saved bytes still matched the
   cloud before the uncommitted preview.
4. Confirmed that restored line, then pressed Y once. The full cart hash
   returned to its pre-line value. Returned to full-image view via controller.
5. Start launched the expanded game successfully; its read-only `run.p8`
   snapshot matched the canonical saved cart. Exited using injected Ctrl+Q.
6. Pushed the separate [rendering fixture](../../../experiments/runtime-smoke/sprite_regions.p8)
   into the wrapper's test-cart directory. The first VIEW intent was sent while
   the previous activity was exiting; no diagnostic game frame appeared. After
   exit, a new VIEW launch displayed the cloud at native size with `spr` and
   at 2× with `sspr`. This is a rendering/data check, not a runtime-reliability fix.
7. Returned to the workshop, captured selection over the actual saved artwork,
   and left the full cloud canvas open without a pending edit.

| Artifact | SHA-256 |
|---|---|
| Copy before / after selection only | `4c047e1f5960c0c3585bc701c1ca98464fadfff2b87139a5a79c5cb001f4da19` |
| Cloud / preview not committed / after undo | `e931122b81cb3cab91d500e02576366a094db03065acfe0d4c2b2658e6707b02` |
| Rendering fixture | `fed46cbb1593a1cd8f38ab3da10e2f87a3cf83a1f60e27050494f337f28f5037` |
| Installed APK | `0f4bb15e89af87b4100224bc7a81e60e51af289c03aed730bfb51b6b59de0905` |

## Portable checks and remaining scope

LabCartridgeTest passed; WorkshopTest 799 checks; SpriteWorkflowTest 2804;
LibraryWorkflowTest 28; DrawingWorkflowTest 4269; RegionWorkflowTest 16421.
Build, packaging and APK signature verification passed.

Region checks cover every outside pixel, CRLF and unknown-section preservation,
omitted-row extension, an unterminated final row, full-sheet corner addressing,
non-square line reversal, bounds rejection, selection without writes, modal
input isolation, failed-save retention, complete undo including appended rows,
controller selection/zoom and expanded runtime snapshots.

The data model covers the ordinary 128×128 sheet. The current selector exposes
only upper 128×64 pixels with corners on 8×8 cells; this is PikoNest scope, not a
PICO-8 limit. Map-aware lower-half editing, large-image hero binding/hitboxes,
named resources and safe copy placement are future steps. A selection is not
an allocation and can overlap other selections. The eight old 16×16 cards
remain shortcuts with their existing New/Copy/Assign workflow.

Injected keys establish reachable actions, not physical controller ergonomics
or owner acceptance. Only this device viewport was checked in this slice.
No general cart import, persistent undo or production framework decision is
implied. See the [host details](../../../experiments/android-host/README.md) and
[runtime limitations](../../ANDROID_RUNTIME_POC.md).
