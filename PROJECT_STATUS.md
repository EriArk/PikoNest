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
| Android baseline | Development build **0.0.75**, recorded on 7 October 2026 |
| Latest product work | Creation-flow design and interactive prototype, 8 October 2026 |
| Current focus | Making the whole creation process easier, starting with editing directly from the game view |
| Distribution | One development APK; no public APK release yet |
| Platforms | Android first; Linux later |

The 11 October update adds public progress documentation. It does not introduce
a new application build. The proposed creation interface is not in the APK yet.

## What already works in the development build

- **Play and Workshop in one app.** Browse local games or work on projects, then
  run them using the imported official PICO-8 runtime. Launch and return have
  been demonstrated on Retroid Pocket Classic.
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

## The next experience: designed, not yet implemented

The proposed Workshop opens on the game being made. Select an element and change
its appearance, position, behavior, rules or sound from nearby actions. PikoNest
should prepare safe resource locations and code connections automatically.

Two journeys guide this design: a small exploration/collection game and a light
puzzle without a hero. Both have a blank-project path and an editable starter
path. Templates provide a starting point; they never lock access to tools.

The browser prototype explores selection, drawing, placement, rules and a
simulated play/return loop. It is **design material**, with no cartridge writing,
resource allocator or official PICO-8 execution. Specific screens still await
owner review. [Design and prototype](docs/design/creation-flow-2026-10-08/README.md).

## What still needs to be finished

- The complete, unassisted beginner journey from Blank to an original game.
- Automatic safe resource allocation and the remaining sprite/map operations.
- General SFX/music editors, event-driven animation/audio and connected effects.
- Dependency-aware reuse of more asset types across projects.
- Complete folder setup, migration, recovery and modern Android compatibility.
- Full English coverage, physical-controller acceptance and broader screen/device
  testing. Additional languages follow the English version.

Ordinary `.p8` files remain the game source. Proprietary runtime binaries are
never bundled. Publishing an APK requires separate owner approval after product
acceptance; development progress is not release approval.

## What comes next

1. Review the proposed creation screens and interactions.
2. Implement Blank → draw a sprite → place it → edit it → Test and return, with
   safe automatic resource placement in supported new projects.
3. Connect selected-element behaviors, rules, goals and restart, including the
   hero-free puzzle path.
4. Add the required animation/audio connections and validate two small starters
   through the same tools.
5. Finish the remaining editors, reuse/remix and runtime/storage work, then run
   full Android and owner acceptance.

[The roadmap](docs/ROADMAP.md) owns the sequence; [the backlog](docs/BACKLOG.md)
owns task status. This summary does not create a second plan.

## Images for posts and the website

| Material | Use it for | Required context |
| --- | --- | --- |
| [0.0.75 screenshots](docs/showcase/0.0.75-en/README.md) | Drawing strokes, copying a sprite between projects, testing edits | Real development-build captures; not a public release |
| [0.0.72 workflow](docs/showcase/0.0.72-en/README.md) | A small original game made from Blank through the tools | A limited demonstrated workflow; the complete creator remains unfinished |
| [Full screenshot index](docs/showcase/README.md) | Play, maps, animation, camera and backgrounds | Keep each image's build and capture notes; packs show different stages |
| [Creation-flow study](docs/design/creation-flow-2026-10-08/README.md) | Discussing the proposed simpler Workshop | Label as a **design prototype**, not an implemented Android screen |

## Short update to adapt for a post

PikoNest now combines playing and editing in one Android development app. Recent
work added continuous sprite drawing, copying graphics between projects and safer
handling of unfinished edits. A small game has also been created from a blank
project through the tools and tested in official PICO-8. The next focus is making
creation much easier: selecting things directly in the game and letting the app
handle more of the technical setup. That interface is currently a prototype;
the full creator and public APK release are still ahead.

## How this file stays current

Update this snapshot after each completed meaningful work batch, together with
CHANGELOG.md. Keep implemented behavior, demonstrated results, prototypes and
plans distinct. Link the relevant evidence, retain useful limitations, and update
the date only when reviewing the snapshot. Technical detail stays in `docs/`.
