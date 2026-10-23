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
| Android baseline | **0.0.83** built locally; **0.0.77** remains the last complete native creation journey |
| Latest work | Music composition and shared phrase editing implemented; device acceptance pending |
| Current focus | Maps/camera in the simpler creation path; device journeys when permitted |
| Platforms | Android first; Linux later |

## Latest implementation: compose music for your game (0.0.83)

Arrange musical phrases into patterns, set where a section loops or stops, and
create or edit its notes without leaving the composition. Phrases can be shared,
or copied independently before changing them. Copying/clearing a pattern asks for
confirmation and keeps its sound data available for reuse.

Attach the composition to the game's start. Choose three musical parts with a
separate channel for effects, or all four parts with possible interruption of the
fourth. Music restarts with the game and stops before result effects on win/loss.
All notes, patterns and the assignment save together, with one Undo and recoverable
nested drafts. Unfamiliar imported Lua remains untouched.
[Scope and validation](docs/showcase/0.0.83-en/README.md).

**Checked locally:** 81 core suite invocations and the integrated APK build passed.
New music checks cover data preservation, shared/copy editing, sequence flow,
channel choices, failed saves, recovery and exact Undo/Redo. Host Lua checks confirm
start/reset/result call order. Compact/tall/wide layouts received host review.

**Still pending:** actual music/SFX playback, Stop/return, mute and exported-cart
audio in official PICO-8; real-device, physical-controller and novice/owner journeys
for labs 0.0.78–83. Console use remains paused. No installation, native screenshot
pack or public release was made. [Issue #17](https://github.com/EriArk/PikoNest/issues/17)
remains open for visual journey acceptance.

This is basic composition. Range rearrangement, music/SFX library reuse, advanced
sound settings and further music events remain unfinished. Game-start wiring and
automatic phrase allocation require recognized Game view code; explicit resource
editing remains available for other projects.

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

Still required: animation/audio device acceptance and broader event clips, music
range editing and reuse, advanced SFX tools, maps and
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

1. Connect map/camera tools to the simpler Game view creation path. When console
   use is authorized, verify the named collection/switch journeys, animation and
   audio, then fix the observed UX findings.
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

PikoNest now has a basic music composition workflow. Arrange phrases into patterns,
write their notes right there, and choose where the tune loops or stops. Shared
phrases can be copied before editing, and one Undo reverses the whole composition
change. Music can start with your game and leave a separate channel for sound
effects. Local tests and the Android build pass; real handheld playback and
beginner review are still ahead. Maps and camera are next in the simpler workflow.
