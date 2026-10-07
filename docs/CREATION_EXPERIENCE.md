# Creation experience: from an idea to a playable game

8 October 2026. Owner-approved **direction**, with a proposed interaction design.
The review prototype is not an Android build or an implemented feature.
Lab 0.0.75 remains the demonstrated application baseline.

This document develops the creation part of [UX_DIRECTION](UX_DIRECTION.md).
[BACKLOG](BACKLOG.md) owns UX06–UX09 and implementation status;
[ROADMAP](ROADMAP.md) owns sequence. It does not introduce a parallel roadmap.

## 1. The problem to solve

The current lab proves valuable operations, but a newcomer must assemble them.
In [0.0.72](showcase/0.0.72-en/README.md), creating a moving image requires initial
numbers, rules and explicit coordinate bindings. In
[0.0.75](showcase/0.0.75-en/README.md), cross-project graphics reuse includes a
sheet destination and shared-memory review. These are technical achievements,
not evidence that an unassisted beginner can comfortably make a game.

The new completion test is a whole creative task. The user chooses what appears
and what happens; PikoNest prepares the resource placement and ordinary Lua.
Template selection alone does not solve editing, wiring or troubleshooting.

## 2. One workshop, three starting points

- **Blank project:** opens an empty game view with Add. No compulsory name,
  player, genre, movement, collision or sprite.
- **Start from a game:** small playable examples with a picture and a plain
  explanation. Initially validate only Little explorer and Light puzzle.
- **Remix a game:** first create a safe copy. Source inspection determines which
  visual operations are supported; arbitrary Lua is not automatically converted.

All three use the same workshop. Play remains the default product entrance;
creating a project never becomes a requirement for everyday playing.

Inside a project the main view is **the game being made**. The initial viewport
represents its starting composition, not a claim that arbitrary Lua can be
rendered live. Selection opens actions for that element. An accessible Elements
list resolves overlap, invisible elements and controller selection.

**Add** offers Sprite, Tile map, Text and interaction/region actions as supported.
No mandatory hero taxonomy. A sprite is still pixel data; a visible element is
a particular use of that data. Background and HUD describe uses, not new console
resource types. Full tools, library categories/favorites and optional Code remain
reachable through Tools; context explains prerequisites and offers their setup.

## 3. Journey A: Little explorer, created from blank

A small single-screen game: move a chosen figure, collect a star, show a win
message, restart. This is a bounded example, not a platformer physics project.

| Step | The person does | PikoNest prepares | Visible result / undo boundary |
| --- | --- | --- | --- |
| 1 | Choose Blank project | A normal editable cart and optional editor metadata | Empty game view; no player invented |
| 2 | Add → Sprite → Draw; paint and choose Done | A safe resource allocation, name suggestion and draw use | New image attached to placement cursor; cancel rolls back the whole pending add |
| 3 | Place the image with directions; confirm | Coordinates and recognized draw code | Image in game; Add is one project Undo |
| 4 | Select it → Behavior → Move with directions | Ordinary input/update logic and needed values | Working movement with a visible speed setting; no x/y binding exercise |
| 5 | Draw and place a second image | A separate resource/use with the same tools | Star or any other user-created collectible |
| 6 | Select collectible → Rules → On overlap; pick the moving image visually | The selected relationship, an explicit rectangular overlap rule | Summary: once collected, hide this item and show a win message |
| 7 | Add sound/animation through that rule as those tools become available | Dependency selection/allocation and ordinary event calls | Sound/animation attached to this event; no manual slot wiring |
| 8 | Add Restart to the game; Test | Snapshot plus exact editor return context | Play, win, restart, return; preview never overwrites the authored start state |
| 9 | Change image/speed/goal, Undo, reopen and export | Durable save/history and ordinary `.p8` export | Same game independently runs in official PICO-8 |

The initial overlap box is shown and editable. Rectangle overlap is not a claim
of pixel-perfect collision or built-in PICO-8 physics. Restart restores the
defined authored state; calling `_init()` alone is not evidence of resetting all
runtime state. Audio/animation steps stay open until their core workflows exist.

**From a starter:** steps 2–6 begin configured. First tasks are replace the image,
move the collectible, change movement and change the win rule. Deleting all
starter elements must leave a usable blank project. The starter is not an
alternative implementation of movement, selection or rules.

## 4. Journey B: Light puzzle, with no hero

A small board of three lights. Directions select a cell; the game action button
toggles that light. All lights on wins. Restart restores the starting pattern.
The simple independent toggles are intentional: testing authoring does not need
an additional neighbour-propagation recipe.

| Step | The person does | PikoNest prepares | Visible result |
| --- | --- | --- | --- |
| 1 | Blank → add a light image, draw its off/on frames | Resource locations and frame sequence | One editable light, no movement or player object |
| 2 | Duplicate and arrange three lights using a grid | Independent state for each use; shared graphics made explicit | A board assembled with general placement tools |
| 3 | Select the lights → add selection with directions | A bounded selection index/highlight and normal input code | Visible focus can move between cells |
| 4 | Rule: when game action is pressed, toggle selected light | State and frame binding; one change per physical press | On/off changes once; holding does not flicker |
| 5 | Rule: when all selected lights are on, show a win message | Group condition evaluated after toggles; once-on-entry action | Winning state without health, a hero, gravity or a collision body |
| 6 | Add Restart, sound if desired, then Test/reopen/export | The same transaction/runtime/export workflows as Journey A | A complete hero-free game |

**From a starter:** change the starting pattern, replace frames, add/remove a
light, change the winning condition. Rules address the selected group rather
than a hidden fixed count of three. A group is an authoring convenience mapped
to ordinary code; it is not a mandatory entity system for all cartridges.

## 5. Shared screens and controller grammar

The [review prototype](design/creation-flow-2026-10-08/README.md) explores these
surfaces in English. Its illustrated assets and simulated Test are design
material, not screenshots or official-runtime evidence.

| Surface | What the user sees | Main actions |
| --- | --- | --- |
| New project | Blank and two illustrated starts with plain descriptions | Choose, Back |
| Game view | Starting composition, selected element, short contextual actions | Select element, Add, Tools, Test |
| Appearance | The selected image and its frames, with an immediate preview | Paint, frame, Done, Cancel |
| Behavior | A working capability with a few useful parameters | Add/change/remove, Test |
| Rule | When / optional condition / ordered actions, with named visual targets | Choose target, add action, Done |
| Conflict | Concrete affected content and a safe alternative when known | Make independent copy / change shared resource / cancel |
| Test return | Exact previous view/selection/draft | Continue editing; no automatic claim of success |

Abstract actions remain the input contract; labels follow the device mapping.
A opens/confirms, B returns/cancels the current step, Start tests, Select opens
context actions. Undo/Redo are reachable in that menu; Y can remain Undo when
shown. No required shoulders, triggers, chord or touch gesture. A game action
button and an editor confirm button are different contexts: a rule shows both
the portable PICO-8 action identity and the current physical mapping.

Directions move **focus** by default. Move and Paint explicitly enter canvas
editing, change the hints and show the cursor. A finishes that operation, B
cancels it. Selecting another element never silently moves or paints it. The
Elements list provides all canvas selections through discrete controller focus.
Returning from a resource editor restores the exact selected use.

Compact/square screens show the material followed by a short action list; wide
screens put that list beside it. Long forms occupy their own focused view.
Game pixels use nearest-neighbour scaling; text and targets do not shrink with
the game image. Palette and typography follow UX_DIRECTION, not generic Android
forms. Physical readability and input remain acceptance gates.

## 6. Automation with clear ownership

| Task | Automatic in the supported authored path | Explicit choice / boundary |
| --- | --- | --- |
| Add resource | Find known safe space, maintain references and reserve within the pending operation | No space: keep draft and offer resource management; no silent overwrite or reformat |
| Add behavior | Prepare necessary values/callback connections, readable ordinary Lua | Show target and useful parameters; conflicting controls require a decision |
| Draw shared image | Show which uses share it | Change all or make an independent copy; navigation never clones it |
| Duplicate element | Copy placement/settings, assign independent gameplay state | Reuse appearance by default, disclose sharing; duplicate-resource action is separate |
| Add frame/sound | Allocate and connect declared dependencies | Shared SFX/music use and channel conflicts need meaningful review |
| Save | Commit a completed reversible operation atomically | Writing/failed-write states visible; retain draft on error |
| Test | Run current valid draft as a separate snapshot | Invalid draft stays open; never quietly test the old saved version |
| Return | Restore editor context; discard temporary simulation state | Applying game-time positions back into design is not implicit |
| Unknown source | Preserve bytes and known supported operations | No guessed references or promise that empty pixels mean free space |

Allocation requires known ownership and checked references. Fresh authored carts
can maintain these invariants; opening arbitrary Lua cannot establish them from
pixel emptiness. Optional `.pikoos` metadata may accelerate editing but is not a
second executable truth. Reopening after its removal must preserve the cart and
reconstruct only supported bindings. A stale fingerprint invalidates assumptions.

Safe known operations finish with **Done** and one Undo. There is no additional
mandatory diff/review page for every brush or behavior change. Unfinished complex
edits remain drafts. External file changes, unknown memory aliases or known
cross-resource damage keep a meaningful consequence review; automatic saving
does not weaken existing journal/recovery or source-hash safeguards.

## 7. Rules without a second programming language to learn first

Start with named choices tied to selected elements. Expose a general rule editor
for custom combinations. A plain summary remains visible before committing:

- `When this item overlaps Moth → hide this item → show “You did it!”` (once).
- `When action ○ is pressed → toggle selected light` (once per press).
- `When all Board lights become On → show “All lit!”` (once on entering state).

Distinguish **pressed once**, **held**, **repeating navigation** and **state
entered**. Ordered actions and rule ordering remain inspectable. Do not hide an
accidental every-frame sound loop behind a friendly label. “What affects this?”
shows recognized rules, animation and movement with direct edit links.

Only actions applicable to the current target appear in its short menu, with an
All tools path and setup guidance. Genre never gates capability. Optional Help
explains the real generated concept and Lua; no mandatory course or unlock.

## 8. PICO-8 compatibility checks informing the design

Reviewed the official [PICO-8 0.2.7 manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html)
on 8 October 2026, especially Program Structure, Graphics, Input, Audio, Map and
Memory. These are implementation constraints, not extra beginner setup screens:

- Resources/uses compile to standard drawing, map and ordinary Lua state.
- Default gfx/map sharing must be checked when allocating or editing. Advanced
  remapping is a valid console feature even where PikoNest cannot edit it safely.
- `btnp()` has built-in repeat. A true once-per-press authoring action needs a
  correct edge detector or an explicitly scoped repeat policy; changing global
  repeat settings must not unexpectedly change other authored controls.
- SFX and music share resources/channels. Allocation and playback must account
  for both rather than treating each sound as an unlimited independent stream.
- Camera, animation, transitions, groups and collisions are normal Lua/data
  authoring conveniences, not new built-in PICO-8 runtime APIs.

Each implementation slice still needs focused core tests and execution in the
user's official runtime. This design/mockup does not satisfy that requirement.

## 9. Research decisions

Sources were reviewed as documented approaches, not a hands-on usability ranking.
Recommendations below are our adaptation to a small controller-operated editor.

| Source | Adopt | Adaptation / limit |
| --- | --- | --- |
| [Bitsy room tools](https://make.bitsy.org/docs/tools/room/roomEditingTools/index.html) and [exits](https://make.bitsy.org/docs/tools/exitsandendings/index.html) | Select content where it appears; visual links between locations | Also offer a focusable Elements list; rooms are optional ordinary-cart authoring structure |
| [Pulp](https://play.date/pulp/docs/) | Nearby image/frame/interaction/audio editing; useful initial behavior | Do not import its mandatory player/role taxonomy or runtime format |
| [GDevelop behaviors](https://wiki.gdevelop.io/gdevelop5/behaviors/) | Add one working capability, then tune it | Generate explainable PICO-8 Lua; no dependency on GDevelop or an opaque PikoNest engine |
| [Construct families](https://www.construct.net/en/make-games/manuals/construct-3/project-primitives/objects/families) | Reuse rules across user-defined groups | General group logic comes after the first validated single-element path |
| [GB Studio actors](https://www.gbstudio.dev/docs/project-editor/actors/) and [prefabs](https://www.gbstudio.dev/docs/project-editor/prefabs/) | Contextual actions and reusable configured elements | Cross-project insertion must materialize dependencies; live linked updates are deferred until conflict handling exists |
| [LDtk auto-layer rules](https://ldtk.io/docs/general/auto-layers/auto-layer-rules/) | Paint semantic terrain and select edges automatically | Later map improvement after basic map workflows; results become normal tiles |
| [Game Builder Garage](https://www.nintendo.com/us/store/products/game-builder-garage-us/) | Short optional learning steps, edit/play loop, button-based creation | Begin with focusable rule cards; do not require a course or assume wiring graphs are easy on a handheld |
| [RPG Maker event triggers](https://www.rpgmakerweb.com/blog/event-priorities-and-triggers) | Explicit trigger meaning and useful diagnostics | Make once/held/repeat visible; avoid unexplained repeating autorun traps |

## 10. Implementation and review boundary

UX06 delivers this design and review material. UX07 connects blank → safe image
creation → placement → reopen/Test. UX08 connects selected-element behaviors,
rules and the two game journeys using existing operations; unavailable animation
and sound remain tracked dependencies. UX09 validates two small starter paths
and novice acceptance. Resource editors continue inside those complete workflows.

Later additions include terrain automation, richer reusable configured elements
and broader starters. They do not displace missing SFX/music/code tools or
runtime/storage acceptance. See the revised sequence in ROADMAP.

Before coding the new workshop shell, review three concrete choices with the
owner: game view as project home; selected-element action list; Done/Undo for
safe operations with review reserved for actual consequences. Agreement with the
general direction is recorded; acceptance of these particular screens is pending.

The measurable gate is **ACC-09** in [ACCEPTANCE](ACCEPTANCE.md):
complete both blank and starter journeys without developer coaching or manual
resource/callback wiring. Record stumbling points and actual completion time;
there is no invented minutes-to-game or fourteen-turn delivery guarantee.
