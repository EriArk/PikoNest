# PikoNest — project status

Updated **23 October 2026**. A plain-language snapshot for posts and the
website. Dated changes are in [CHANGELOG.md](CHANGELOG.md).

## At a glance

PikoNest is a PICO-8 player and game-making workshop for Android handhelds.
Users bring their purchased official PICO-8 runtime. Play, Workshop and runtime
setup live in one development APK; there is no public APK release yet.

| Item | Current position |
| --- | --- |
| Stage | Early development; a small connected creation loop is demonstrated |
| Android baseline | **0.0.85** built locally; **0.0.77** remains the last complete native creation journey |
| Latest work | Tile art creation/editing inside the map; device acceptance pending |
| Current focus | Two small editable starters using the general tools; device journeys when permitted |
| Platforms | Android first; Linux later |

## Latest implementation: make your own map tiles (0.0.85)

Draw a new tile directly from the map, review it and start painting scenery with
it. No separate object or Lua editing is needed. When changing an existing tile,
choose whether to update all its uses or make an independent copy for one cell.
Copies keep the tile's flags; other graphics and map data stay intact.

Test unfinished art without saving. Tile pixels, map placement and camera
settings save together, with one Undo. Palette selection, menus and interrupted
touch strokes recover with the draft. [Scope and validation](docs/showcase/0.0.85-en/README.md).

**Checked locally:** 83 core suite invocations and the integrated APK build passed.
New checks cover drawing, copying, shared resources, failed saves, recovery and
exact Undo/Redo. Actual drafts from the previous version still restore. Compact,
tall and wide layouts received host review; two labeled host previews are linked below.

**Still pending:** official-runtime tile/world/camera/audio/animation execution,
real-device controls, exported-cart playback and novice/owner journeys for labs
0.0.78–85. Console use remains paused. No installation, new native screenshot pack
or public release. [Issue #17](https://github.com/EriArk/PikoNest/issues/17) remains
open for visual journey acceptance.

New tiles use safe upper-sheet storage in recognized projects. In-place editing
of lower/shared tiles remains in All tools. Collision, door triggers, parallax
and broader map operations remain unfinished. General tools stay accessible in
every project; automatic wiring and allocation require recognized code.

## Previously demonstrated in the development app

- **Play and Workshop in one app.** Browse local games, launch using imported
  official PICO-8 and return. External-launch and storage work also has bounded
  demonstrated paths; broader device acceptance remains open.
- **Draw directly from Game view.** Add a sprite, paint it and place it. Select
  it to move, duplicate, remove or change its appearance. Shared graphics offer
  edit-all or an independent copy; the app handles safe resource placement.
- **Give sprites behavior.** Choose directional movement and speed, contact
  collection/win/loss, or switches with optional links to another switch.
  Settings appear when relevant. There is no mandatory hero.
- **Give the game a goal.** Collect all items or turn all switches on, with
  ordinary PICO-8 win/loss and restart logic generated automatically.
- **Keep editing after adding rules.** Appearance, position, duplication and
  safe removal preserve behavior. Rule targets survive unrelated deletions;
  referenced sprites cannot be removed silently.
- **Test, recover, undo and export.** Supported drafts survive app closure and
  can run separately from saved work. Changes use one Save/Undo transaction.
  Both new games exported exactly; the collection export also ran independently
  in official PICO-8 outside PikoNest.
- **Compose audio.** Create effects, bind supported events, arrange music patterns,
  edit shared or copied phrases and choose how music shares channels with effects.
  This is locally implemented; official audio and device acceptance remain open.
- **Use the existing tools.** Sprite-sheet/map editing, shared-memory review,
  continuous strokes, a persistent sprite buffer, asset reuse and recognized
  animation/camera/background/code forms remain available through All tools.

The native evidence covers a collection game with a hazard and a separate
linked-switch game, both authored through the app. It includes official runtime
outcomes/reset, reopening, draft Test/return, exact export and preservation of
59 existing files. Tests passed, but injected input is not physical-controller,
novice or owner acceptance. [Images, carts and limits](docs/showcase/0.0.77-en/README.md).

## What is still unfinished

Game view currently recognizes a bounded ordinary-Lua recipe: up to 16 sprite
placements, simple movement/contact/toggle rules and fixed outcome text. Unknown
Lua is preserved and uses All tools. These are editor limits, not PICO-8 limits.
The preview shows initial positions, not simulated game execution.

Still required: animation/audio device acceptance and broader event clips, music
range editing and reuse, advanced SFX tools, direct tile creation in the map,
collision/door/parallax workflows, broader rules, library insertion and
multi-type reuse, remaining sprite/map operations, complete onboarding/storage
and recovery, modern Android compatibility, English coverage and wider device
and physical-controller validation. Undo history is session-local. The Android
export picker still used touch in this batch.

The browser creation-flow prototype remains design material. Native work now
covers the two small blank-project journeys above; editable starter content,
learning and the complete unassisted beginner experience remain unfinished.
No public beta or release readiness is claimed.

## What comes next

1. Connect two small editable starters using the same general tools and contextual
   guidance. Templates must not lock tools or replace the Blank creation path.
2. When console use is authorized, verify the named collection/switch journeys,
   animation, audio and world/tile/camera editing, then repair observed UX findings.
   Complete remaining core-tool/library operations and contextual learning.
3. Finish independent setup, storage/backup/recovery and Android compatibility;
   run both journeys on supported screen/control configurations.
4. Have a novice and the owner complete and review the intended paths, fix the
   findings, then obtain separate approval for any public APK release.

[ROADMAP](docs/ROADMAP.md) owns the sequence; [BACKLOG](docs/BACKLOG.md) owns task
status. This summary does not replace either plan or waive acceptance gates.

## Images for posts and the website

| Material | Use it for | Required context |
| --- | --- | --- |
| [0.0.85 tile editor previews](docs/showcase/0.0.85-en/README.md) | Tile painting and review | Host-rendered production UI; not native screenshots or device acceptance |
| [0.0.77 screenshots and two games](docs/showcase/0.0.77-en/README.md) | Movement, contact, goals, linked switches, official play and export | Native development build; bounded creation path, not a public beta |
| [0.0.76 screenshots](docs/showcase/0.0.76-en/README.md) | Drawing/placement and shared/independent appearance | Earlier static Game view slice |
| [Full screenshot index](docs/showcase/README.md) | Play, maps, animation, camera and other tools | Keep each pack's version and provenance |
| [Creation-flow study](docs/design/creation-flow-2026-10-08/README.md) | The broader proposed experience | Label as design, not an Android feature |

## Short update to adapt for a post

PikoNest now lets you draw new tiles right inside the map workflow. Paint a tile,
review it, then use it across your scenery. Change every use or make a separate
copy for one cell. Art, map and camera save and undo together, and unfinished
strokes recover with the draft. Local tests and the Android build pass; handheld
and beginner review are still ahead. Next: two small editable starting games
using the same tools.
