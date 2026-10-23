# Game view: world, map and camera

Lab 0.0.84 connects these existing resource concepts to the recognized Game view
creation path. It is implemented and locally tested; official-runtime, physical
controller and owner acceptance are pending. [Evidence](showcase/0.0.84-en/README.md).

## Make a larger field

Open **World & camera** from Game view. Choose the width and height in screens.
A field can be one screen or several, with no required player or moving object.
D-pad selects a field; A opens it, directions change it, A keeps the value and B
reverts it. **Show map** hides the tile layer without deleting its data.

**Paint map** uses the cartridge's existing 8×8 tiles. Move the cursor with the
D-pad and press A to paint. X opens the visual tile picker; A confirms the tile
and B keeps the previous brush. Tile zero is empty. B opens painting options,
including the eraser. Touch selects/paints a cell; controller access is complete
at the portable action layer, with physical validation still pending.

To draw a new tile today, use the existing sprite-sheet tool in **All tools**, save
it, then return to this picker. Direct new-tile drawing inside this map workflow
is unfinished and next in the creation queue. The connected map brush does not
create a sprite placement or a hero. Tile painting adds scenery, not collision.
Existing rectangle/fill, region and shared-memory operations remain in All tools.

## Choose how the camera behaves

- **Fixed view:** choose its starting X/Y offset, in 8-pixel steps.
- **Follow an element:** choose any named element; its center guides scrolling,
  clamped to the edges of the field. The element need not have player controls.
- **Change rooms:** choose an element; the view switches between 128-pixel screens
  when its center crosses a boundary. This is not a door/trigger transition.

The map draws first, then the objects. Score, switch instructions and result
messages draw after resetting the camera, so they keep their screen positions.
Moving elements stop at the world boundary. Their interactions still use world
coordinates. Removing the camera's active target asks the user to change the
camera first; deleting unrelated elements adjusts the target reference.

Game view's **Starting layout** is an editor viewport, not a running simulation.
Selecting an off-screen element or moving a placement pans it into view. Touch
selection and placement account for that offset. The viewport may differ from the
runtime camera; the World settings preview shows the camera's initial view.

## One change to save or discard

Map cells, field dimensions and camera wiring share one draft. Start/Test launches
an isolated candidate. Review/Save commits them together, and session Undo/Redo
reverses the complete change. Map painting/settings also have up to 32 draft Undo
steps. B from a changed settings page offers Save, Keep editing or Discard.
Recovery retains the cursor, picker, field revert and draft history and requires
an exact match with the saved source. Failed saves keep the draft for retry.

Shrinking or hiding the field preserves cells outside the visible area. Animation,
audio, names and behaviors survive the world edit. Unfamiliar Lua is not rewritten;
use the independent map/camera tools for those cartridges. World state lives in
ordinary Lua; `.pikoos/` metadata is not required by the exported game.

## Console contract and current editor limits

The [official PICO-8 0.2.7 manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html)
describes the base 128×32 map and its optional shared lower half. Its map values
reference 8×8 sprites; tile zero is empty under the default draw state. `map()`
uses tile coordinates for its source and pixel coordinates for its destination.
[`camera()`](https://www.lexaloffle.com/dl/docs/pico-8_manual.html)
shifts drawing coordinates and resets when called without arguments.

This connected editor currently offers **1–8 screens across and 1–2 down**,
starting at map cell 0,0. Only upper map cells are changed. Lower map/shared-gfx
bytes remain intact, and all 256 tile references can be selected. These are
PikoNest workflow limits, not PICO-8 capacity rules. Custom memory layouts, layer
masks, parallax, camera easing, door triggers and collision authoring are not
silently inferred. The older independent tools and source data remain available.
