# Game view: world, map and camera

Lab 0.0.84 connects world/map/camera concepts to the recognized Game view creation
path; 0.0.85 adds tile art directly inside the map. This is implemented and locally
tested; official-runtime, physical controller and owner acceptance are pending.
[World evidence](showcase/0.0.84-en/README.md) · [Tile evidence](showcase/0.0.85-en/README.md).

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

The connected map brush does not create a sprite placement or a hero. Tile
painting adds scenery, not collision. Existing rectangle/fill, region and
shared-memory operations remain in All tools.

## Create or change tile art without leaving the map

From the tile picker, press **X / Create → New tile**, or choose **New tile** in
painting options. The editor reserves a free upper-sheet tile automatically. Draw
with D-pad/A or a touch drag, choose a color with X, and use B for eraser, review
or discard. Color zero is transparent. The 8×8 canvas has a pixel cursor and, on
wide screens, a map preview. A connected touch stroke is one local Undo step.

**Review tile → Keep tile** adds the art to the world draft, places it in the
selected map cell and selects it as the brush. It does not save the project yet.
Continue painting with that tile, or create another. Start/Test includes the
unfinished tile in an isolated game candidate. New/copied tiles need at least one
visible pixel before Test/Keep; a shared tile can be erased entirely.

**Edit tile here**, or **Edit tile** from the picker, shows map-cell, object and
animation-frame use counts. **Edit all uses** changes that graphics resource
wherever it is used. **Make a tile copy** changes only the selected map cell;
other uses keep the original. Copies retain ordinary sprite flags. **Copy brush
tile** and **Copy tile** provide the same independent-copy path directly.

New/copied tiles use safely available slots 1–127. Existing pixels, map references
(including hidden rows), flags, sprite placements and animation regions reserve
storage. Slot zero remains empty under this workflow's default draw state. A full
sheet refuses before changing the draft. Tiles 128–255 share lower-map storage:
copy them here into a free upper slot, or use All tools for explicit shared-memory
editing. They are not rejected as invalid PICO-8 tiles.

Discarding a tile preserves the previous world draft and leaves no orphan data.
Keeping a tile is one world-draft Undo step covering pixels, flags and placement.
Palette/menu/review, cursor, local history and interrupted touch strokes recover
against the exact saved source. Earlier version-9 world drafts remain readable.

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

Tile pixels/flags, map cells, field dimensions and camera wiring share one draft. Start/Test launches
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
starting at map cell 0,0. Only upper map cells and upper graphics slots are changed. Lower map/shared-gfx
bytes remain intact, and all 256 tile references can be selected. These are
PikoNest workflow limits, not PICO-8 capacity rules. Custom memory layouts, layer
masks, parallax, camera easing, door triggers and collision authoring are not
silently inferred. The older independent tools and source data remain available.
