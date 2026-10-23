# Sound effects — lab 0.0.82

A general SFX resource editor is available from **Game view → Sound effects**
and **All tools → Menu → Sound effects**. It works in Blank, hero-free and imported
projects. Automatic placement and event wiring require a recognized Game view
project; explicit resource edits preserve unfamiliar Lua unchanged.

This is a bounded implementation, not completion of S01–S05. Local data/workflow,
host layout and package validation are recorded in the
[0.0.82 evidence](showcase/0.0.82-en/README.md). Official playback, native return,
physical controls, novice and owner acceptance are still pending.

## Make a sound

1. Choose **Create sound**. A safe unused slot is chosen for recognized projects.
   For unfamiliar Lua, choose an explicit SFX slot; the editor cannot infer whether
   arbitrary code will use it later.
2. Choose one of 32 notes. Turn Sound on, then adjust pitch, volume, instrument
   and effect. A opens a field, arrows change its value, A keeps it, B reverts it.
   Moving the cursor never writes notes. Touching the graph selects a note.
3. Copy a note to the next step, make a rest, or open **Speed and loop**. You can
   transpose all sounding notes, set ticks per note and loop boundaries. Menu
   jumps to the sequence options; shoulder navigation moves eight rows at a time.
4. **X / Listen** launches an isolated audition cart through the user's official
   runtime. PICO-8 O stops and X replays once per press; exit returns through the
   existing runtime path. **Start / Test** runs the game with the draft instead.
   Neither action saves the project. There is no automatic cursor audition or
   system-volume change. Actual playback/Stop/return still needs device validation.
5. **Review and save** shows event, music and custom-instrument references. Saving
   changes every use of that resource. **Make independent copy** keeps the original;
   it does not silently redirect existing uses. In an event-creation draft only
   that selected event is assigned the copy. Back from the sequence discards it.

Y / R2 undo/redo note edits inside the draft. Saving creates one session Undo
transaction for resource data and optional event wiring together. Recovery retains
notes, the active field's original value, draft undo/redo and return context, and
requires the exact saved source. Global Undo/Redo is available in the sound bank
and event list; it is suspended in a pending slot chooser.

## Put it in the game

**Sounds in the game** lists game start, win, loss and configured element
interactions. Choose an existing SFX, create one for that event, or choose No sound.
A selected element's **Sound** action opens its interaction binding directly;
configure an interaction in Behavior first. Save/Cancel returns to that element.
Removing a binding leaves the SFX data intact.

Generated code uses ordinary `sfx(slot,3)` calls:

- Start plays at `_init`, including restart. Channel 3 is stopped before reset
  whenever this recipe contains sound bindings.
- Collect/contact win/contact lose plays once when the interaction happens.
- Toggle plays on the selected switch's button edge. A linked neighbor does not
  cause a second sound; holding the button does not replay it every frame.
- Global win/loss plays once on the outcome update. Finished-game updates return
  early, so the result sound does not restart every frame.
- All these events share channel **3**. Later events in the same update replace
  earlier sounds; the global result follows contact sounds. This first policy is
  explicit, not a mixer or automatic music/SFX arbitration. Channels 0–2 are not
  changed by these calls. Existing music resources are preserved, not played by
  this recipe.

Removing an interaction retains its now-dormant sound binding. It stays visible
in Sounds in the game for explicit removal/reuse. Appearance, movement, names,
behavior and animation edits retain bindings. Duplicated elements intentionally
share the SFX until explicitly changed. Sound bindings are managed through the
sound list; the earlier rule overview does not yet add sound rows.

## PICO-8 data and preservation

The text-cart model covers 64 SFX slots with 32 notes each. Editable note fields
are stored pitch 0–63, instrument 0–15 (8–15 reference custom instruments 0–7),
volume/effect 0–7. Speed is 1–255 ticks. Loop end is exclusive; start >= end disables
the loop, except start > 0 with end 0 sets playback length. The UI's note numbers
are 1–32; loop offsets use PICO-8's 0-based values.

Ordinary rows are 168 hex digits: four header bytes followed by 32 five-digit
notes. A no-op preserves exact bytes; a targeted edit changes only differing
nibbles. LF/CRLF, unchanged hex case, unrelated rows and all other sections stay
intact. Missing rows/sections can be materialized using the standard defaults.

Automatic allocation reserves slots 0–7, all known event bindings, every music
reference including muted channels, custom-instrument references and any authored
nonempty SFX data. Silence alone never proves a slot is unused. Full-source
recognition, not a comment marker or empty slot, authorizes automatic wiring.

The current editor accepts ordinary mode headers 0/1, speed 1–255 and loop offsets
0–32. A wholly blank zero-speed row is explicitly initialized to speed 16 when
edited. Advanced filter/waveform headers and other unsupported settings remain
preserved and are refused for editing; supported slots in the same cart can still
be changed. These are PikoNest limits, not claims that those cartridges are invalid.
Malformed/ambiguous resource framing is refused without mutation.

## Still required

Basic [music composition and a three/four-part policy](MUSIC_EDITOR.md) are now
implemented locally in 0.0.83. Remaining work includes richer range copy/editing
and multi-type library reuse; advanced filters/waveform editing;
actual audio comparison and complete physical-controller/novice acceptance.
Neither local tool implementation proves a beta-ready journey.

References checked for this implementation:
[official PICO-8 0.2.7 manual — SFX editor, sfx and music](https://www.lexaloffle.com/dl/docs/pico-8_manual.html),
[picotool SFX text serialization](https://github.com/dansanderson/picotool/blob/main/pico8/sfx/sfx.py),
[PicoSynth's original waveform implementation and instrument names](https://notb.itch.io/picosynth).
The official runtime remains authoritative; host Lua execution is only a logic check.
