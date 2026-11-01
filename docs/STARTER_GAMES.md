# Starting games and optional creation help

Lab **0.0.86**, 1 November 2026. Implemented and locally checked; native,
official-runtime, physical-input and novice/owner acceptance remain open.
[Evidence and host previews](showcase/0.0.86-en/README.md).

## Pick a starting point

Workshop → New now defaults to **Blank**, followed by **Little explorer** and
**Light puzzle**. Left/right selects a starting point; A creates a separate project
and B cancels. The dialog shows its starting field, a short description and play
controls. Creation itself does not launch the game or play sound.

- **Little explorer:** move an original small figure with directions, collect
  three stars, win and restart with PICO-8 X. The map is decorative scenery, with
  no implied walls/collision. Each collectible targets the moving element.
- **Light puzzle:** select lamps with left/right and toggle with PICO-8 O. Lamp 1
  also toggles Lamp 2; Lamp 2 also toggles Lamp 3; Lamp 3 is independent. Turn all
  three on to win. On-state animation pulses the lamps, and event sounds are editable.
  There is no moving character. One solution is Lamp 1, then Lamp 3.
- **Blank:** the same tools, with no preconfigured element or behavior.

O/X above name PICO-8 game buttons; physical mapping remains the runtime/input
adapter's responsibility. The older Moon Garden and Lights examples remain
available after the new starts. Their descriptions explain that some editing
uses All tools; existing saved projects and IDs are preserved.

Each creation makes a fresh ordinary `.p8`. Editing or copying one project never
changes its starting content or another project. The new IDs use `explorer-` and
`switches-`; older namespaces remain valid. Publication is exclusive, so creation
cannot replace an existing project. Existing export and recovery paths accept them.

## Help while making your own game

From Game view press **X / Help**, or choose **Ideas & help**. Select a topic to
read its explanation, then A opens its existing tool. Touch selects a different
topic first; tapping it again or A opens it. B returns to the game without changes.

Topics cover Test, drawing, selected-element appearance/behavior/animation,
world tiles, sounds, music, goals and exporting the saved game. If an element is
needed but the field is empty, the action opens sprite creation. The selected
element's name explains the context. Save/discard uses the tool's usual return
path; this is optional help, not a mandatory course or a completion checklist.

The help also works in Blank and other recognized authored projects. Unknown Lua
is preserved: it explains All tools rather than guessing bindings. The export
topic returns to Projects and explains Actions → Export. Help selection can be
recovered, but opening it does not create a draft or add anything to the cartridge.

## Same general tools and ordinary console data

StarterGames creates original content through PlayModel, PlayAnimation, the map,
sprite and SFX writers. There is no starter-specific behavior engine, capability
lock or runtime API. Appearances, positions, relationships, goals, animation,
sounds, map and camera remain editable through existing tools. All tools remains
reachable, and ordinary exported carts do not require starter metadata.

The [official PICO-8 0.2.7 manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html)
is the console contract: normal callbacks/input, sprite-sheet regions, map cells,
SFX slots and channels. These examples use upper graphics, 16×16 elements, an 8×8
scenery tile, and short ordinary SFX in slots 8/9. They do not expand console limits.

## Still unfinished

This is a small starter pair and shared contextual help. It does not complete a
genre catalogue, concept/progress tracking, an unassisted beginner study, broader
mechanics, collision, door transitions, parallax or asset reuse. Real-device
Test/return, process recovery, independent exported-game playback, physical controls
and owner review remain open. Console use is paused; no public APK release is approved.

The next independent block is the first-run path: folder destinations, correct
purchased runtime archive, preparation and recoverable failures without developer
intervention. Continue remaining core-tool/library work and complete device and
novice acceptance when authorized; see [ROADMAP](ROADMAP.md).
