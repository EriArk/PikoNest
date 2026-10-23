# Game view: animation frames and state

Lab 0.0.78 connects a selected sprite to an editable sequence without a Lua cursor.
This extends the recognized Game view recipe. The separate animation editor in
All tools remains available for its existing supported draw blocks.

## Draw and connect a sequence

1. Select a sprite in Game view and open **Animation**.
2. **Copy frame** makes an independent copy of the selected frame and selects it.
3. **Edit this frame** opens the existing brush, palette and eraser. Done returns
   to the frame list; it does not save the project yet.
4. Select frames using the **Frame** field or the thumbnail strip. Change each
   duration, move frames earlier/later, or remove a sequence reference.
5. Choose **Loop**, **When moving**, or **When on**. Moving requires a configured
   movement; On requires the Toggle rule. The form explains missing prerequisites.
6. **Start / Test** runs the entire unsaved sequence in official PICO-8.
7. **Save animation** writes the sequence, pixels and ordinary Lua together.
   One Undo restores the previous cartridge; Redo restores the saved result.

A enters a field, directions change it, A keeps it, and B reverts it. B outside
fields discards the whole animation draft. In the drawing menu, Discard changes
returns to the frame list and restores that frame's pre-edit pixels/allocation.
No physical keyboard is needed. The editor preview shows the selected pose;
Test provides authoritative playback, rather than a simulated Lua execution.

## Timing and state

The recipe uses the ordinary `_update` callback, at 30 updates per second.
Durations are stored as whole updates and shown as rounded milliseconds.
Frame 1 appears on entry to a playing state. Every frame has its own duration;
the bounded counter wraps at the sequence duration rather than accumulating
unbounded elapsed time.

- **Loop:** repeats continuously while the game is running.
- **When moving:** plays only when the sprite position actually changes. Holding
  a direction at a screen edge returns to frame 1, just like standing still.
- **When on:** plays while the switch is on. Linked switches update before their
  animation states are evaluated; switching off returns to frame 1.
- Restart resets animation state. The existing game outcome freezes gameplay
  and animation until restart. Collection hides the whole animated placement.

## Resource and source safety

Frames refer to ordinary `sspr` rectangles. The `.p8` contains all pixels and Lua;
no sidecar or external library is required to play it. Existing lab 0.0.77 source
is recognized and round-trips exactly; opening it does not migrate or rewrite it.
Recognition compares the complete reconstructed source, including frame playback.
A modified or unfamiliar recipe stays intact and is available through All tools.

Allocation reserves every frame reference, including transparent regions, plus
existing pixels, placements, map references, flags and tile zero. New frames use
the upper sheet. Editing a frame shared by another placement/frame or map creates
an independent copy first, so unrelated uses retain their appearance. Lack of
safe space refuses the operation without overwriting resources. Removing a frame
keeps its pixels on the sheet; automatic garbage collection is not implemented.

Source-checked recovery retains frame order, duration fields and their revert
values, painted pixels and the in-progress drawing checkpoint. Stale recovery
is refused. Save failure retains the draft. Test and Cancel preserve saved bytes.

## Current editor scope and open acceptance

This Game view sequence supports **up to eight equally sized rectangular frames
per sprite**, with durations of 1-150 updates (about 33 ms-5 s). These are current
PikoNest editor limits, not PICO-8 limits. The first frame supplies the placement's
base appearance and collision rectangle. Reordering frames updates that reference.
A one-frame sequence is visually still. There is no separate sequence-removal
command yet; remove extra frames to return to a single pose.

Multiple named states, one-shot event clips, variable frame sizes within this
Game view sequence, frame library reuse, mirroring, audio and a live editor
playback timeline remain unfinished. Existing All tools operations are retained.
Physical-controller ergonomics, novice use and owner acceptance are separate
from automated input and data tests. This work does not close beta acceptance.

References: [PICO-8 manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html)
(`_update`, `sspr`, sprite sheet), [behavior contract](GAME_BEHAVIORS.md),
[development evidence](showcase/0.0.78-en/README.md).
