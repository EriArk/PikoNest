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
| Android baseline | **0.0.80** built locally; **0.0.77** remains the last complete native creation journey |
| Latest work | Element names and explicit behavior choices implemented; device acceptance pending |
| Current focus | Clarify goals and configured-rule overview; complete device journeys when permitted |
| Platforms | Android first; Linux later |

## Latest implementation: names and clear action choices (0.0.80)

Give elements their own names, such as Heart, Star or Left switch, using the
controller keyboard or touch. Those names appear in the visual list and target
picker. Two elements can share one graphic but have different names. Names follow
supported placement/frame edits and travel with the exported game; clearing a name
restores its automatic visual label.

Movement and interaction now open explicit choices. Choose Collect when touched,
then select who touches it. Cancelling that selection restores the previous
interaction. Save, Undo, draft Test and recovery keep the same protected workflow.
[Scope and validation](docs/showcase/0.0.80-en/README.md).

**Checked locally:** the full core suite, Android packaging, name/choice recovery
and named animation editing. **Not yet checked on device:** these new journeys,
official Test/return, physical controls or new screenshots. Console use remains
paused by the owner. The previous animation implementation also still needs its
native/runtime acceptance.

This advances [issue #17](https://github.com/EriArk/PikoNest/issues/17), which stays
open. Goal choices, the overview of configured rules and entry/return ergonomics
still need work and novice/owner review. Names are optional editor annotations;
if an external layout edit makes their bindings ambiguous, the app preserves the
cart and asks for review instead of guessing. Sound/music tools remain unfinished.

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

Still required: animation device acceptance and broader event clips, audio bindings,
general SFX/music editing, maps and
camera connected to the simpler path, broader rules, library insertion and
multi-type reuse, remaining sprite/map operations, complete onboarding/storage
and recovery, modern Android compatibility, English coverage and wider device
and physical-controller validation. Undo history is session-local. The Android
export picker still used touch in this batch.

The browser creation-flow prototype remains design material. Native work now
covers the two small blank-project journeys above; editable starter content,
learning and the complete unassisted beginner experience remain unfinished.
No public beta or release readiness is claimed.

## What comes next

1. Clarify game goals, the configured-rule overview and return/edit/remove flow. When console
   use is authorized, verify collection/switch journeys and animation on device;
   then implement general SFX creation and binding to supported events.
2. Complete the required audio/music and map/camera creation workflows, then
   validate a small pair of editable starters through the same general tools.
3. Finish independent setup, storage/backup/recovery and Android compatibility;
   run both journeys on supported screen/control configurations.
4. Have a novice and the owner complete and review the intended paths, fix the
   findings, then obtain separate approval for any public APK release.

[ROADMAP](docs/ROADMAP.md) owns the sequence; [BACKLOG](docs/BACKLOG.md) owns task
status. This summary does not replace either plan or waive acceptance gates.

## Images for posts and the website

| Material | Use it for | Required context |
| --- | --- | --- |
| [0.0.77 screenshots and two games](docs/showcase/0.0.77-en/README.md) | Movement, contact, goals, linked switches, official play and export | Native development build; bounded creation path, not a public beta |
| [0.0.76 screenshots](docs/showcase/0.0.76-en/README.md) | Drawing/placement and shared/independent appearance | Earlier static Game view slice |
| [Full screenshot index](docs/showcase/README.md) | Play, maps, animation, camera and other tools | Keep each pack's version and provenance |
| [Creation-flow study](docs/design/creation-flow-2026-10-08/README.md) | The broader proposed experience | Label as design, not an Android feature |

## Short update to adapt for a post

PikoNest elements can now have names of your choice, and the editor uses those
names when you connect them. Names stay with supported edits and exported games.
Movement and interactions now open clear lists: choose what should happen, then
pick the other element on the field. You can cancel without leaving half a rule
behind. Local tests and the Android build pass; handheld and beginner testing are
still ahead. The complete simpler workshop and public release are not finished.
