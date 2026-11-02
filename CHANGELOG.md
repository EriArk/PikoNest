# PikoNest — changelog

Plain-language development updates for posts and the website, newest first.
Version numbers below identify **development builds**, not public APK releases.
There is no public APK release as of 2 November 2026.

## 2026-11-02 — Native dependency sources and distribution checks

- Matched 44 native files to official Ubuntu, Termux and Godot artifacts. Collected
  153 pinned source archives/patches and additional original component notices.
- Added a repeatable APK/source audit and an allowlisted source-material ZIP builder.
  Unexpected libraries, corrupted sources and unsafe output paths are rejected;
  incomplete source coverage is reported explicitly and fails the completion check.
- The next beta package must include the new notices and pass native drift checks.
  No native binaries, application behavior or installed app changed in this batch.
- Validation: 10 focused boundary tests passed; all 79 current native files,
  44 official binary matches and all 153 source files checked. The local bundle is
  explicitly partial: 33 Termux files and the remaining distribution review are
  unresolved. No full app rebuild, handheld access or publication.
  [Evidence and next work](docs/release/beta1/DEPENDENCIES.md).

## 2026-11-02 — Early beta preparation, backup and Splore (lab 0.0.93)

- Named the first milestone an early beta (`0.1.0-beta.1`), with Lua still needed
  outside supported visual workflows. No public APK release has been created.
- Added checked backup ZIPs for active projects, reusable assets, persisted drafts,
  settings and runtime saves/downloads. Staged restore retains original private
  data, detects source conflicts and can resume or reverse an interrupted swap.
- Connected official Splore to Play. Cached cartridges join the shelf after return,
  with artwork, available names, favorites/recents and direct cached-file launch.
  A Games folder or Workshop project is not required. Real network/offline tests
  and external Downloads-folder synchronization remain open.
- Prepared non-debug candidate packaging with the existing signing identity,
  setup/data instructions, release text and a fresh labeled website preview pack.
- Inventoried 77 native bootstrap files and added Godot/Press Start 2P notices.
  Exact matching sources and remaining dependency notices still block publication.
- Validation: 91 core suites and integrated beta packaging passed, including 154
  backup checks, 16 cache checks, signing continuity and 77 native inventory hashes.
  [Evidence](docs/showcase/0.0.93-en/README.md).
  No handheld access, installation or new native captures. Device/provider/runtime,
  physical/novice and owner acceptance are separate pending checks.

## 2026-11-02 — Alpha scope and flexible runtime setup (lab 0.0.92)

- Set the first milestone to alpha, with Lua allowed beyond supported visual
  workflows. Full no-Lua creation remains planned for later updates.
- Removed the required readme and fixed inner-folder layout from runtime import.
  A single ARM64 executable and its sibling data file are discovered by content;
  outer filenames and version numbers are not whitelisted.
- Connected discovered entries to both installation and isolated runtime probes.
  Kept damaged/unsafe, ambiguous and unsupported-platform archives blocked.
- Replaced fixed-version setup guidance and added Open creation folders to the
  first-run guide, with optional continuation using internal storage.
- Validation and limits: [local checks and host layout review](docs/showcase/0.0.92-en/README.md).
  No console use, native install, old/new runtime execution matrix or public release.
  Independent wizard acceptance and full backup/restore remain open.

## 2026-11-02 — External working library (lab 0.0.91)

- Connected sprites, parameter presets and the shared sprite buffer to an independent
  external library location with checked activation, reconnect and internal originals.
- Routed naming, categories, favorites, resource creation and reuse through shared
  typed persistence. Interrupted saves recover before resource enumeration.
- Preserved original resource bytes during migration; historical preset labels
  now allow explicit metadata edits without false source conflicts.
- Local validation: 89 core suites, new library repository checks (288 assertions,
  including 256 pixel comparisons), and integrated single-APK packaging passed.
- No handheld use, installation, screenshots or release. Actual Android provider,
  lifecycle, visual/controller and owner acceptance remain open. Full reinstall
  backup/restore, merge and additional reusable asset types are unfinished.

[Contract](docs/EXTERNAL_LIBRARY.md) · [Evidence](docs/showcase/0.0.91-en/README.md).

## 2026-11-01 — External working projects (lab 0.0.90)

- Added verified activation of a selected Android Projects folder. Original internal
  projects are retained; project create/import/save/reopen use the external workspace.
- Added retained-version saves and recovery of interrupted writes. Unknown changes
  block overwrite and preserve files for review.
- Added reconnect and explicit return to older internal originals, plus automatic
  workshop reopening after a location change and accurate first-run storage wording.
- Local validation: 88 core suites, 61 new recovery assertions and integrated APK
  packaging. Android provider/device, visual/controller and owner acceptance pending.
- Limits: assets, settings, drafts and saves keep their current locations. No merge,
  complete reinstall recovery or automatic version cleanup. No console use or release.

[Contract](docs/EXTERNAL_PROJECTS.md) · [Evidence](docs/showcase/0.0.90-en/README.md).

## 2026-11-01 — Shared project persistence (lab 0.0.89)

- Moved project creation, import, opening and saving behind a portable storage
  contract in preparation for external working folders.
- Added a source recheck at the local write boundary and verification of new cart
  and imported-title bytes before publication. Existing project paths stay intact.
- Validated editor Save/Undo/Redo through the new repository, failed Undo, import
  retry, write failure and source conflicts: 40 new checks; 87 local core suites.
- Working data still lives inside the app. External activation, the library port
  and Android provider/device acceptance remain unfinished. No console use or release.

[Evidence and limitations](docs/showcase/0.0.89-en/README.md).

## 2026-11-01 — Verified copies to selected folders (lab 0.0.88)

- Added Copy projects / Copy library from Folders. Copies use new directories,
  preserve originals and check every file before displaying success.
- Added Resume for interrupted candidates. Existing matching files are rechecked;
  partial or conflicting files are retained and need a new copy.
- Added a checked SHA-256 completion receipt, destination journal and cancellable
  worker with English progress/error guidance and controller/touch actions.
- Local validation: 86 core suites, 36 transfer assertions, 11 actual local-source
  adapter checks and single-APK package verification passed.
- Limits: working storage remains internal. Settings, unfinished editor drafts,
  saves and runtime data are excluded. Actual Android provider, interruption,
  visual and physical-input acceptance remain open; the console was not used.

[Scope and evidence](docs/showcase/0.0.88-en/README.md) ·
[Copy guide](docs/STORAGE_COPY.md).

## 2026-11-01 — First-start guide and optional runtime test (lab 0.0.87)

- Added a skippable English setup guide with saved progress and re-entry from
  Folders. It connects the games folder to Play and explains current data storage.
- Choosing a valid purchased Raspberry Pi ZIP now continues into PICO-8 preparation.
  Trying a game is a separate action. Failed preparation can retry the saved archive;
  a cancelled or rejected folder replacement retains the earlier choice.
- Updated the folder and archive screens in English. Reserved external destinations
  are distinguished from active storage; long setup messages can be scrolled.
- Local validation: 85 core suite invocations passed, including 40 new guide/runtime
  state checks; the single APK compiled, signed and passed package verification.
  Compact/tall/wide production-view host renders were reviewed.
- Limits: projects, assets and Splore still use app-private storage. No new device,
  official-runtime, physical-input or owner acceptance was performed. No APK release.

[Scope and host previews](docs/showcase/0.0.87-en/README.md) ·
[First-run setup and remaining work](docs/FIRST_RUN_SETUP.md).

This file was started on 11 October 2026 and backfilled from documented work
from 5 October onward. It is a curated history, not a complete commit log.
Earlier work is recorded in [the technical status](docs/STATUS.md) and
[versioned evidence](docs/showcase/README.md). See
[PROJECT_STATUS.md](PROJECT_STATUS.md) for the current overall picture.

## 2026-11-01 — Start from a small game and make it yours (0.0.86)

**Implemented and locally checked; official runtime and device acceptance remain open.**

- Added Little explorer and Light puzzle beside Blank, with original art, a field
  preview, plain descriptions and controls. Previous examples remain available.
- Both starts use the existing tools: movement/collecting or linked switches,
  goals, animation, event sounds and scenery. There is no template-locked editor.
- X / Help explains common tasks and opens the relevant tool. It works in Blank
  too and offers sprite setup when needed. Unknown Lua keeps the All tools path.
- New projects and copies keep separate saved files. Editing, Undo, recovery and
  ordinary export preserve the game's other resources and behavior. Older project
  IDs and drafts remain readable; opening help never modifies the game.

**Checked:** 84 local core suite invocations and integrated APK packaging passed.
The final starter workflow suite passed 135 assertions; 15 host Lua checks covered
winning/restarting both games, linked switches, animation and event calls. Four
actual older-journal checks passed. Compact/tall/wide host layout review led to
larger starter sprites and shorter labels. A final help-return correction passed
focused checks and refreshed Android packaging.

**Limits:** this is a starter pair and optional help, not completed novice acceptance,
learning progress or a full genre catalogue. Console use remains paused: no
installation, new official execution, native screenshots or public APK release.
Two ordinary example carts and three labeled host previews are in the repository.
Next: independent first-run folder/runtime setup and recoverable errors.
[Guide](docs/STARTER_GAMES.md) · [evidence and previews](docs/showcase/0.0.86-en/README.md).

## 2026-10-23 — Draw new tiles without leaving the map (0.0.85)

**Implemented and locally checked; official runtime and device acceptance remain open.**

- Create tile art from the map, paint with controls or touch, choose colors and
  erase, then review and place it. The new tile becomes the brush for more cells.
- Change every use of a shared tile or make an independent copy for one cell.
  Copies keep flags; other objects, animation frames and shared memory stay intact.
- Test an unfinished tile without saving. Keep adds it to the world draft;
  one Save/Undo covers art, map and camera. Discard leaves no unused new tile.
- Recover unfinished strokes, palette selection, menus and drawing history.
  Previous world drafts remain readable. No Lua editing or placed object is required.

**Checked:** 83 local core suite invocations and the integrated APK build passed.
The new workflow suite passed 33,004 assertions, mostly protected-pixel comparisons;
actual previous-version drafts passed six recovery checks. Compact/tall/wide
layouts received host review. Two labeled host previews are in the repository.

**Limits:** new tiles use safe upper-sheet slots in recognized projects; lower
shared tiles can be copied, while their in-place editing stays in All tools.
Broader map operations, collision and door/parallax tools remain unfinished.
No installation, native screenshots, official execution or public release occurred.
Next: two small editable starters using these same general tools.
[Guide](docs/WORLD_EDITOR.md) · [evidence and host previews](docs/showcase/0.0.85-en/README.md).

## 2026-10-23 — Build a larger world from Game view (0.0.84)

**Implemented and locally checked; official runtime and device acceptance remain open.**

- Choose field dimensions, paint/erase map tiles and choose a fixed, following or
  room-changing camera without editing Lua. No hero or moving object is required.
- Place and select objects beyond the first screen; the editor pans to keep them
  in view. Movement uses world bounds, while scores/messages stay on the screen.
- Map cells and camera settings share one Save/Undo. Draft history, picker,
  field revert and cursor recover together. Failed saves keep the draft.
- Camera targets are protected from accidental deletion. Existing animation,
  music, sound and shared map/graphics data remain intact. Unknown Lua uses the
  existing independent tools without guessed wiring.

**Checked:** 82 local core suite invocations and integrated APK packaging passed.
The new workflow suite passed 16,496 assertions (including repeated byte/pixel
preservation checks). Seventeen host Lua assertions covered movement bounds,
room edges, restart and drawing order. Actual older v8 drafts also recovered,
including a nested animation paint checkpoint; journal readers retain v1–8.
Production drawing code received compact/tall/wide host layout review. A final
relationship-viewport correction passed focused checks and refreshed packaging.

**Limits:** the connected brush uses existing tiles and upper map rows. Direct
new-tile drawing in context is next. Collision, door triggers, parallax and broader
map operations remain unfinished. No console actions, installation, new native
screenshots, official execution or public APK release occurred.
[World guide](docs/WORLD_EDITOR.md) · [exact evidence](docs/showcase/0.0.84-en/README.md).

## 2026-10-23 — Compose music inside your game project (0.0.83)

**Implemented and locally checked; official audio and device acceptance remain open.**

- Added a general Music tool for patterns, four parts, loop/stop flags and confirmed
  pattern copy/clear. No Lua editing is needed for this resource workflow.
- Parts open the existing note editor. Create a phrase, edit a shared one or make
  an independent copy without leaving the composition. Notes, pattern references
  and game-start assignment save and undo together; nested drafts recover.
- Choose three musical parts plus a separate effects channel, or four parts with
  possible interruption of the fourth. Muting retains the original phrase data.
  Music restarts on reset and stops before contact/result effects at win/loss.
- Listen uses isolated audition carts. Unknown code remains unchanged and uses
  explicit resource selection; automatic allocation/wiring requires a recognized game.

**Checked:** 81 local core suite invocations and integrated APK packaging passed.
New data/workflow suites passed 548 and 218 assertions; seven host Lua assertions
checked start/reset/result audio call order with stubbed audio. Actual older v6/v7
animation/behavior drafts recovered. Compact/tall/wide host layouts were reviewed.
No console actions, installation, new native screenshots or official playback occurred.

**Limits:** range rearrangement, multi-type audio library reuse, advanced SFX data
and room/result music events remain unfinished. Real playback, Stop/return, exported
cart audio and physical/novice/owner acceptance remain open. No public APK release.
[Music guide](docs/MUSIC_EDITOR.md) · [exact evidence](docs/showcase/0.0.83-en/README.md).

## 2026-10-23 — Create sounds and attach them to events (0.0.82)

**Implemented and locally checked; official audio and device acceptance remain open.**

- Added a general sound-effects editor: 32-note sequences, pitch, volume,
  instruments, effects, rests, next-note copy, transpose, speed and loops.
  Controller navigation selects notes without changing them accidentally.
- Recognized games can create a sound for starting, collecting, toggling, winning
  or losing. One Save/Undo covers both sound data and its assignment. Unfamiliar
  imported Lua remains unchanged; it uses explicit sound-slot selection.
- Shared-use review includes events, music and custom instruments. An independent
  copy keeps the original and does not silently change other uses. Automatic
  placement reserves referenced slots, including muted music references.
- Listen prepares a separate audition cartridge; Test uses the game draft. Neither
  saves the project or changes system volume. Notes, field revert and draft Undo
  recover after closure; older animation/behavior drafts still restore.

**Checked:** 79 local core suite invocations and integrated APK packaging passed.
The new data/workflow suites passed 2,222 and 200 checks. A separate host Lua trace
passed 16 event/stop/restart checks; this used stubbed audio calls, not official
playback. Compact/tall/wide host layouts and real lab-0.0.81 draft migration passed.
No device actions, installation or new official-runtime execution occurred.

**Limits:** basic event sounds share channel 3; later sounds replace earlier ones.
Music composition/channel handling, richer range edits and sound-library reuse are
unfinished. Advanced imported filters/waveforms remain preserved but read-only.
Physical-controller, novice and owner review remain open. No public APK release.
[Sound tool guide](docs/SFX_EDITOR.md) · [exact evidence](docs/showcase/0.0.82-en/README.md).

## 2026-10-23 — See and edit how your game works (0.0.81)

**Implemented and locally checked; device and owner acceptance remain open.**

- Game rules now shows the goal, background, movement, interactions and animations
  together. Named entries lead to their settings; saving or cancelling brings you
  back to the same rule or element. Undo also keeps you in the overview.
- Goals have clear choices and highlight the items or switches that count.
  Missing-rule hints explain what to add. You can try a highlighted goal before
  saving, or cancel without changing the game.
- Removing a movement or interaction now names linked switches or animations that
  depend on it. Those connections remain protected until explicitly changed.
- Draft recovery includes the return location, even through nested target choices.
  Compact/tall/wide layouts received host review, including long element names.

**Checked:** 77 local core suite invocations and integrated APK packaging passed.
The new connected overview/goal suite passed 100 checks alongside existing Game
view, name, relationship and animation tests. No device actions, installation or
new official-runtime execution occurred; host previews are not native screenshots.

**Limits:** the overview covers recognized Game view rules, not arbitrary Lua.
Complete device journeys and physical/novice/owner acceptance remain open under
issue #17. General sound effects/music are still unfinished. No public APK release.
[Tool guide](docs/GAME_BEHAVIORS.md#goals-and-rule-overview-0081) ·
[exact evidence](docs/showcase/0.0.81-en/README.md).

## 2026-10-23 — Name elements and choose behavior clearly (0.0.80)

**Implemented and locally checked; device and owner acceptance remain open.**

- Added optional element names with a controller/touch keyboard. The visual list
  and relationship picker show those names. Shared graphics can have independent
  names; duplicates get distinct labels. Empty names restore visual descriptions.
- Names survive supported moves, copies, deletion and animation-frame changes,
  and travel with an exported `.p8`. Save/Undo and recoverable draft Test use the
  same protected workflow. Existing title/author header lines stay intact.
- Movement and interaction now open explicit choice lists. Contact actions lead
  directly to the visual target picker; cancelling it restores the previous
  interaction instead of leaving an unfinished change behind.
- Fixed named animation allocation after changing the first frame, so users can
  continue copying frames and recover/test that draft before saving.

**Checked:** 76 local core suite invocations and integrated APK packaging passed.
Focused name/choice coverage passed 101 checks, alongside the existing Game view,
behavior, relationship, animation and asset-name checks. Host-only layout previews
covered compact, tall and wide sizes; they are not native screenshots or input proof.
No console actions, installations or new official-runtime executions occurred.

**Limits:** names currently have this editor's 24-character limit. External layout
edits can make name bindings stale; the source is preserved and automatic edits
stop until reviewed/reverted. General tools do not yet maintain those bindings
in every placement operation. Goal/overview ergonomics, native journeys and novice/
owner review remain open under issue #17. No public APK release.
[Tool guide](docs/GAME_BEHAVIORS.md#element-names-0080) ·
[exact evidence](docs/showcase/0.0.80-en/README.md).

## 2026-10-23 — Pick game relationships visually (0.0.79)

**Implemented and locally checked; device and owner acceptance remain open.**

- Added sprite thumbnails and color descriptions to the Elements list. Canvas
  selection opens the selected element's actions; the list also reaches
  overlapping, transparent and off-field placements.
- Contact targets and linked switches now use a visual picker, candidate
  outlines and a connection line, with a short explanation of the relationship.
  Switch links offer other configured switches and an explicit no-link option.
- Confirm keeps the selection; Cancel returns to the original field unchanged.
  Draft Test can try the highlighted candidate without saving or confirming it.
  Recovery retains candidate focus, and Save/Undo remains one complete edit.
- Added clearer behavior/goal labels and an explicit Starting layout label.
  The existing official runtime remains the execution authority.
- Updated the UX audit and existing roadmap around issue #17: the contextual
  creation experience takes priority over the next sound block.

**Checked:** full local core suite (75 invocations), Android compilation and
integrated APK packaging; 82 focused relationship checks cover candidate filters,
controller selection, overlap/off-field access, Test/cancel/recovery, save failure,
Undo/reopen and stale-source refusal. Host-only layout inspection informed compact
labels and description placement; it is not native screenshot or touch evidence.

**Limits:** no device actions, new runtime pass or native captures in this batch.
Custom names, broader contextual actions and complete novice/owner acceptance
remain unfinished. Color descriptions may change when appearance/order changes.
Issue #17 and beta acceptance remain open. No public APK release.
[Interaction contract](docs/UX_DIRECTION.md#visual-relationships--lab-0079) ·
[exact evidence](docs/showcase/0.0.79-en/README.md).

## 2026-10-23 — Animation from the selected sprite (0.0.78)

**Implemented and locally tested; native playback acceptance is pending.**

- Added Game view Animation: copy/draw frames, select a pose, adjust individual
  durations, reorder/remove frames and save the sequence without opening Lua.
- Added Loop, When moving and When on bindings. Movement uses actual position
  changes; state entry, stopping, switching off and restart return to frame one.
- Shared/map-backed frames get independent copies before painting. Allocation
  reserves all frame references, including transparent ones; removed frames keep
  their sheet pixels. Existing games remain byte-exact without implicit migration.
- A complete animation saves as one Undo transaction. Draft recovery retains
  pixels, field revert and drawing cancellation; Test prepares an isolated cart.
  New graphics sections recover too, and recovery rejects unrelated resource edits.
- Added a controller form and touch frame strip using the existing pixel UI.

**Checked:** the full local suite (75 invocations) and Android build passed;
final focused suites passed 16,504 Game view, 96 behavior and 80,199 animation
checks. An initial APK was installed and a copy of an existing project opened.
The owner then requested no further console use. Final APK installation, native
animation authoring/recovery/Test/export, official playback and screenshots are
still open. No new runtime pass or post-run device-preservation result is claimed.

**Limits:** up to eight same-size frames in this Game view sequence; one sequence
per sprite, loop/moving/on bindings only. These are editor limits. Sound effects,
music and broader event animation remain unfinished. No public APK release.
[Tool guide](docs/GAME_ANIMATION.md) · [exact evidence](docs/showcase/0.0.78-en/README.md).

## 2026-10-18 — Playable games from Game view (0.0.77)

**Implemented and demonstrated in the Android development app.**

- Added selected-sprite movement, speed, collect/win/lose contact rules and
  switches that can also toggle another switch. No mandatory hero or Lua cursor.
- Added game goals, restart and background color. Forms hide irrelevant fields;
  linked targets are visible in the preview, and long headings no longer hide
  the tool name or target number in the checked layout. Older saved menu focus
  migrates, and touch field adjustment keeps its own action area.
- Appearance, move, duplicate and safe removal continue to work after adding
  behavior. Dependent rules protect targets; removing unrelated sprites keeps
  references connected to the same elements.
- Behavior and game rules reopen from ordinary `.p8` Lua. Full-source checks
  refuse unfamiliar code; comments survive conversion. Draft recovery, field
  revert, isolated Test and one-step Undo/Redo cover these settings.
- Created a collection game and a hero-free switch game through the native UI
  from Blank. Added eleven English screenshots and both exact exported carts.

**Checked:** 73 local core suite invocations; final focused suites of 16,504 and
96 checks. Native creation, behavior draft recovery, exact Undo/Redo, official
PICO-8 movement/contact loss/collection win/switch goal/restart, draft
Test/return/cancel and final-APK reopening, reopening and byte-exact SAF export. Collection export ran
independently outside PikoNest. All 59 checked pre-existing project/library/save
files stayed unchanged. The final installed device was verified muted.

**Still limited:** a recognized recipe with 16 sprite placements and simple
rules, not an arbitrary visual Lua editor. No new animation/audio/map connections,
custom outcome text or general physics. Export Save used touch; physical controls,
novice and owner acceptance remain open. Final UI/parser corrections have separate
artifact provenance from the initial authoring captures. No public APK release.

[Images, exported games and full evidence](docs/showcase/0.0.77-en/README.md) /
[Behavior tool guide](docs/GAME_BEHAVIORS.md).

## 2026-10-11 — Draw and place from Game view (0.0.76)

**Implemented in the Android development app; first bounded part of the new UX.**

- New Blank projects open on the game canvas. Draw a sprite and place it without
  entering sprite-sheet slots, callback locations or coordinate bindings.
- Select a placed sprite to change its appearance, move, duplicate or remove it.
  Shared images offer edit-all or an independent copy; All tools stays available.
- Drawing and placement save together as one undoable change. Test can run the
  unsaved version, and source-checked drafts recover after closing the app.
- Added safe upper-sheet placement for recognized static sprite projects. Existing
  pixels, transparent references, map references and sprite flags reserve space.
- Added six native English screenshots and the ordinary cartridge painted in
  the app. Refined action labels after checking them on the handheld.

**Checked:** 72 local core suite invocations; final focused recovery/workflow
checks; native Blank authoring, process-close recovery, byte-exact Undo/Redo,
independent coloring, and official PICO-8 Test/return. All 16,384 sampled runtime
frame pixels matched. The 52 previous project/library files were unchanged;
console audio remained muted. The final APK was installed and reopening checked.

**Still limited:** Game view currently handles static sprite placements. Behavior,
rules, maps/camera/animation/audio connections, wider allocation and library
insertion remain work ahead. Other cartridges retain All tools. Undo history is
session-local; physical-controller, beginner and owner acceptance remain open.
This is one development APK, with no public release.

[Native images, cartridge and precise scope](docs/showcase/0.0.76-en/README.md).

## 2026-10-11 — Public progress documents

**Earlier documentation-only update; the app was still at development build 0.0.75 at that point.**

- Added a readable project snapshot covering what works, what is being designed,
  what remains and where to find screenshots.
- Added this dated changelog and backfilled recent milestones for use in posts.
- Added contributor instructions to keep both files current after completed work.
- Aligned the documentation entry point with the revised creation-workflow plan.

[Current snapshot](PROJECT_STATUS.md) · [Contribution guidance](CONTRIBUTING.md).

## 2026-10-08 — A simpler way to create games

**Design and browser prototype; no new Android build.**

- Designed a Workshop centered on the game view: select an element and edit its
  appearance, position, behavior, rules or sound nearby.
- Described which technical setup the app should handle automatically and where
  a shared-resource change still needs a clear user choice.
- Planned both blank and starter journeys for an exploration game and a puzzle
  without a hero. All general tools remain available in either case.
- Added an interactive browser study and eight images, including compact and
  wide layouts. Updated the roadmap and beginner acceptance criteria.

**Checked:** browser interactions and six screens at four viewport sizes.
**Still open:** review of the proposed screens and Android implementation. The
browser's simulated Test does not run PICO-8 or save cartridges.

[Prototype and images](docs/design/creation-flow-2026-10-08/README.md) ·
[Creation journeys](docs/CREATION_EXPERIENCE.md).

## 2026-10-07 — Drawing, reuse and a connected Android workflow

These builds extend the development app. The complete creation workflow and
physical-controller acceptance remain open.

### 0.0.75 — Continuous strokes and a reusable sprite buffer

- Draw a connected brush or eraser stroke using touch or the controller workflow.
  Cancel or undo the whole stroke as one operation.
- Copy sprite pixels into a persistent buffer and paste them into another project.
- Recover unfinished stroke/paste work after an editor interruption; keep the
  draft available if writing fails.
- Review shared sprite/map consequences once for the complete stroke.

**Checked:** a native draw → copy → paste → Test/return workflow on Retroid,
Undo/Redo, recovery and preserved existing project/library files. The edited
draft ran in official PICO-8. Device input was automated, not owner acceptance.
**Still open:** richer zoom/pan, resource allocation and non-sprite buffers.

[Screenshots and evidence](docs/showcase/0.0.75-en/README.md).

### 0.0.74 — Safer copying and transforming graphics

- Connected sprite copy, library insertion, move, flip, rotation and recoloring
  to shared sprite/map consequence review.
- Kept operation drafts through Back, recovery and isolated Test, with one
  completed operation to undo.

**Checked:** native editing, recovery, Undo/Redo and official-runtime Test on
Retroid. These are bounded operations, not a complete resource allocator.

[Screenshots and evidence](docs/showcase/0.0.74-en/README.md).

### 0.0.73 — Painting across shared sprite and map memory

- Added brush/shape editing and map brush/rectangle/fill workflows with a
  before-and-after view of shared-memory effects.
- Kept Cancel and draft Test separate from saving; unfinished edits can recover.

**Checked:** native editing and official-runtime results with previous data
preserved. Complete map tools and the broader beginner workflow remain open.

[Screenshots and evidence](docs/showcase/0.0.73-en/README.md).

### 0.0.72 — A small game made from Blank through the tools

- Connected sprite and map positions to the game's values through editing forms.
- Created a small game with a drawn heart, movement, a goal and reset without
  opening Lua in that demonstrated workflow.
- Tested it in official PICO-8 and exported the saved cartridge exactly.

**Still open:** the complete no-code creation journey, event-linked animation
and sound, physical controls and independent execution of the exported game.

[Screenshots and exported game](docs/showcase/0.0.72-en/README.md).

### 0.0.71 — Bringing existing PICO-8 data into the app

- Added reviewed, verified copying of existing runtime data while preserving
  the original files.
- Improved recovery from interrupted runtime preparation.
- Checked setup in an isolated empty test installation and verified that game
  save data survived the runtime-data move.

**Still open:** complete first-run setup, all settings migration and the full
interruption/recovery matrix. This was not full clean-device acceptance.

[Screenshots and evidence](docs/showcase/0.0.71-en/README.md).

### 0.0.70 — One Android app for playing and creating

- Combined Play, Workshop and the runtime adapter into one APK and one PikoNest
  launcher icon. The purchased official runtime is still imported by the user.
- Demonstrated game launch, Workshop Test, return and Home/resume on Retroid.

**Still open:** complete migration/recovery and modern Android compatibility.
The first integrated build uses the owner-approved temporary compatibility mode.

[Screenshots and evidence](docs/showcase/0.0.70-en/README.md).

### 0.0.67–0.0.69 — More connected Workshop tools

- **0.0.67:** shared English animation/camera forms, reversible field edits and
  testing a draft without saving it over the project.
- **0.0.68:** background strips in the common Workshop path, with movement,
  order, duplication, visibility and draft recovery.
- **0.0.69:** starting numbers, input, conditions, value changes and readouts in
  Rules & state, without finding a Lua insertion point.

These work with supported code structures; they do not establish complete
animation, camera, background or general game-creation support.

[Animation/camera](docs/showcase/0.0.67-en/README.md) ·
[Backgrounds](docs/showcase/0.0.68-en/README.md) ·
[Rules & state](docs/showcase/0.0.69-en/README.md).

## 2026-10-06 — Preparing the repository for contributors

**Documentation and repository work.**

- Added clearer public scope, contribution guidance and an issue map for
  unfinished work.
- Established MIT licensing for original code, with separate third-party terms.
- Kept validation local; GitHub Actions remain disabled by owner decision.

This did not publish an APK or complete the distribution/license audit.

[Contribution guide](CONTRIBUTING.md) · [Issue map](docs/ISSUES.md) ·
[License](LICENSE) · [Third-party notices](THIRD_PARTY_NOTICES.md).

## 2026-10-05 — English Play and project shelves, 0.0.66

- Added shared English shelves and actions for Play and Workshop projects.
- Made copying a project explicit and improved navigation back to its parent.
- Captured the regular development app for the website; older tools and
  settings were not yet fully translated.

[Screenshots and captions](docs/showcase/0.0.66-en/README.md).

## Entry guidelines

Add an entry for each completed meaningful batch using its actual date. Lead
with what changed for the user, then state the checks and important remaining
limits. Label design-only or documentation-only work explicitly. Link usable
screenshots/evidence; retain their build and prototype provenance. Do not turn
plans into completed changes or call a development build a release. Correct old
entries transparently when evidence changes; keep old dates intact.
