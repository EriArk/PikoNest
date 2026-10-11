# PikoNest — project status

Updated **11 October 2026**. A plain-language snapshot for project posts and the
website. For dated changes, use [CHANGELOG.md](CHANGELOG.md).

## At a glance

PikoNest is a PICO-8 player and game-making workshop for Android handhelds.
The aim is to make small games with a controller, with optional touch and Lua
editing. Users bring their own purchased official PICO-8 runtime.

| Item | Current position |
| --- | --- |
| Stage | Early development; the complete beginner-friendly creator is unfinished |
| Android baseline | Development build **0.0.76**, installed and checked on 11 October 2026 |
| Latest product work | First native Game view: draw, place and edit sprites, 11 October 2026 |
| Current focus | Making the whole creation process easier, starting with editing directly from the game view |
| Distribution | One development APK; no public APK release yet |
| Platforms | Android first; Linux later |

The first part of the simpler creation interface is now in the development APK.
It works with Blank projects and static sprite placements. The broader design,
including nearby behavior/rule/audio tools, is still being implemented.

## What already works in the development build

- **Play and Workshop in one app.** Browse local games or work on projects, then
  run them using the imported official PICO-8 runtime. Launch and return have
  been demonstrated on Retroid Pocket Classic.
- **Start from the game view.** In a new Blank project, draw a sprite and place
  it without choosing sheet addresses or wiring coordinates. Select it to edit,
  move, duplicate or remove it. Shared graphics can be edited together or copied
  independently. Drawing and placement save as one change with one Undo.
- **Create and edit graphics.** Draw sprites, work with sprite-sheet regions and
  tile maps, copy and transform graphics, and review changes that affect shared
  sprite/map memory.
- **Continuous drawing and reuse.** Touch and controller stroke workflows keep
  a stroke together for cancellation and Undo. A one-item sprite buffer carries
  pixels between projects and survives restarting the editor process.
- **Connect graphics to a game.** Supported sprite/map placements, animation,
  camera and background forms work with recognized cartridge code.
- **Make simple rules without opening Lua.** Starting values, input, conditions,
  changes to values and coordinate connections have been demonstrated. A small
  game was made from Blank through the app, tested in official PICO-8 and exported.
- **Keep work through interruptions.** Supported editing drafts can be restored;
  unfinished changes can be tested separately from the saved cartridge. Verified
  runtime-data copying and interrupted setup recovery also exist.

These are demonstrated, bounded workflows. They do not mean every valid PICO-8
cartridge is fully editable or that the entire creator has passed acceptance.
[Detailed scope and evidence](docs/STATUS.md).

## The broader experience: partly implemented

New Blank projects now open on the game being made. Appearance, position,
duplication and removal are connected in the first static-sprite workflow, with
safe automatic resource placement. Nearby behavior, rules and sound actions are
next. Unknown or more complex cartridge code still uses the existing All tools
path; the app preserves it rather than guessing how to rewrite it.

Two journeys guide this design: a small exploration/collection game and a light
puzzle without a hero. Both have a blank-project path and an editable starter
path. Templates provide a starting point; they never lock access to tools.

The browser prototype explores selection, drawing, placement, rules and a
simulated play/return loop. It is **design material**, with no cartridge writing,
resource allocator or official PICO-8 execution. Specific screens still await
owner review. [Design and prototype](docs/design/creation-flow-2026-10-08/README.md).

## What still needs to be finished

- The complete, unassisted beginner journey from Blank to an original game.
- Broader automatic resource allocation, library insertion into Game view and
  the remaining sprite/map operations.
- General SFX/music editors, event-driven animation/audio and connected effects.
- Dependency-aware reuse of more asset types across projects.
- Complete folder setup, migration, recovery and modern Android compatibility.
- Full English coverage, physical-controller acceptance and broader screen/device
  testing. Additional languages follow the English version.

Ordinary `.p8` files remain the game source. Proprietary runtime binaries are
never bundled. Publishing an APK requires separate owner approval after product
acceptance; development progress is not release approval.

## What comes next

1. Review the first native Game view on the handheld and complete its remaining
   interaction/acceptance work. The static Blank workflow is demonstrated.
2. Connect selected-element behaviors, rules, goals and restart, including the
   hero-free puzzle path. Extend source recognition and previews safely with it.
3. Add the required animation/audio connections and validate two small starters
   through the same tools.
4. Finish the remaining editors, reuse/remix and runtime/storage work, then run
   full Android and owner acceptance.

[The roadmap](docs/ROADMAP.md) owns the sequence; [the backlog](docs/BACKLOG.md)
owns task status. This summary does not create a second plan.

## Images for posts and the website

| Material | Use it for | Required context |
| --- | --- | --- |
| [0.0.76 screenshots](docs/showcase/0.0.76-en/README.md) | Game view, drawing/placement, shared and independent appearances | Real native trial; static-sprite scope, not the complete creator |
| [0.0.75 screenshots](docs/showcase/0.0.75-en/README.md) | Drawing strokes, copying a sprite between projects, testing edits | Real development-build captures; not a public release |
| [0.0.72 workflow](docs/showcase/0.0.72-en/README.md) | A small original game made from Blank through the tools | A limited demonstrated workflow; the complete creator remains unfinished |
| [Full screenshot index](docs/showcase/README.md) | Play, maps, animation, camera and backgrounds | Keep each image's build and capture notes; packs show different stages |
| [Creation-flow study](docs/design/creation-flow-2026-10-08/README.md) | Discussing the proposed simpler Workshop | Label as a **design prototype**, not an implemented Android screen |

## Short update to adapt for a post

PikoNest's simpler creation flow has reached the Android app. Start with a blank
game, draw a sprite and place it directly in the game view. Select it to move,
duplicate or change its appearance; shared images can be edited together or made
independent. The app handles safe sprite-sheet placement and keeps unfinished
work through a restart. The new path was checked on Retroid with official PICO-8.
It currently covers static sprites; easy behavior/rule setup and the complete
beginner game-making journey are next. There is no public APK release yet.

## How this file stays current

Update this snapshot after each completed meaningful work batch, together with
CHANGELOG.md. Keep implemented behavior, demonstrated results, prototypes and
plans distinct. Link the relevant evidence, retain useful limitations, and update
the date only when reviewing the snapshot. Technical detail stays in `docs/`.
