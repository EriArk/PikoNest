# Drawing tools / Android lab 0.0.5

Real ADB screenshots from Retroid Pocket Classic, 1240×1080, 2026-10-02.
All four tracked captures use the final installed build; no mockups/compositing.

- [Tools](tools.png): controller chooser, Fill highlighted.
- [Line preview](line-preview.png): orange star plus an uncommitted yellow tail.
- [Finished image](finished.png): committed image assigned as the copied game's hero.
- [Official runtime](official-runtime.png): that comet running in user-owned PICO-8 0.2.7.

## Device checks

All three existing carts were backed up first. Tests used injected controller
keys inside `Копия 1` (`remix-0001`), not the original project.

1. Opened the tool chooser through controller navigation, selected Fill,
   picked orange and recolored the connected 23-pixel star with one confirmation.
   Y restored the entire original cart hash in one undo; repeated the fill.
2. Selected Line, anchored 11,4 and moved to 15,0. Preview changed on screen
   while the saved cart retained its post-fill hash.
3. Backgrounded and killed host PID 17191, then reopened (PID 17378).
   Tool, color, both endpoints and pending preview survived. Cancel retained
   the exact post-fill bytes. This recovery check preceded the final picker
   return/label refinements, which did not alter line persistence.
4. Repeated the line in reverse, confirmed, then undid it as one action.
   Hash returned exactly to the post-fill state. Drew and committed it again.
5. Picked the yellow endpoint: no file change, returned to Line. Erased that
   endpoint, then Y restored the full line's hash.
6. Assigned the finished image as hero. First runtime attempt returned without
   an observed game frame; the second showed the comet in official PICO-8.
   Cause of this attempt's early return was not established. The previously
   observed wrapper startup instability remains open.
7. Exited with injected Ctrl+Q and returned to the same copied project.
   Left the device on its completed sprite canvas with no pending edit.

The original `moon-garden` and `garden-0001` remained byte-identical to their
pre-update backups. The copy remained 3505 bytes; only 29 byte positions changed:
23 recolored star pixels, 5 tail pixels and the hero sprite number (`2` → `4`).
Other Lua, both cat images and other data remained unchanged.

| State of copy | SHA-256 |
|---|---|
| Before drawing / after undoing fill | `a2af77fb51d6627ef3b6130af6ab60eb416aa0f8405d06175bc51fa556d5d59c` |
| Fill only / after cancelling or undoing line | `349251c45da140342952b3687899124a441ee00bead66f21aab41f3aff56a067` |
| Fill and committed line | `8e5cd8131761fe422a31ee915f77e496598b801b2ef245c62951eb0640a9b4ac` |
| Final, assigned hero; also matches runtime snapshot | `4c047e1f5960c0c3585bc701c1ca98464fadfff2b87139a5a79c5cb001f4da19` |

## Portable validation and limits

LabCartridgeTest passed; WorkshopTest 799 checks; SpriteWorkflowTest 2804;
LibraryWorkflowTest 28; DrawingWorkflowTest 4269. Drawing checks cover connected
regions, diagonal separation, line endpoints/octants/reversal, invalid input,
all seven initially blank regions, CRLF/unknown data preservation, modal routing,
no-op operations, whole-operation undo, failed-write retry, picker return,
two touch endpoints and commit-before-runtime semantics.

APK compilation, packaging and signature verification passed. Installed SHA-256:
`0442dcbf0860094be8e81c63efa628b605554f75f10d1ae2eb3be2b541b7a7fb`.

This does not establish physical held-button ergonomics or broad viewport
acceptance. Freehand strokes, zoom/pan, full sprite/map coverage, persistent
undo and the later animation/audio tools are still absent. Runtime reliability
is separate from editor/data correctness; see the [runtime report](../../ANDROID_RUNTIME_POC.md).
