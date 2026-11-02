# PikoNest — project status

Updated **2 November 2026**. A plain-language snapshot for posts and the
website. Dated changes are in [CHANGELOG.md](CHANGELOG.md).

## At a glance

PikoNest is a PICO-8 player and game-making workshop for Android handhelds.
Users bring their purchased official PICO-8 runtime. Play, Workshop and runtime
setup live in one development APK; there is no public APK release yet.

| Item | Current position |
| --- | --- |
| Stage | First early beta preparation; Lua remains necessary outside supported visual paths |
| Android baseline | **0.0.93 / 0.1.0-beta.1** candidate (see evidence); **0.0.77** remains the last complete native creation journey |
| Latest work | Confirmed PulseAudio through a signed catalog; 60 native files now have official matches |
| Current focus | Resolve 17 remaining Termux files and Android notices, finish distribution materials, then authorized device journeys |
| Platforms | Android first; Linux later |

## Dependency preparation — 2 November 2026

We traced 60 of the application's 79 native files to exact official packages,
including the Godot engine, most Linux support libraries and part of the Android
support layer. Their source materials now have fixed versions and checksums;
155 source inputs and two repository catalogs are collected locally, with licenses.
A repeatable check catches changed or unexpected native files and verifies the
source-only archive without including the purchased PICO-8 runtime or private data.

The latest research recovered all 16 PulseAudio files from a historical Termux
package and verified the repository signature, index and package hashes. This
avoids replacing those binaries just to establish their origin. License review
also now distinguishes required notices/sources from optional identical rebuilds.

This work is **not complete**: 17 Android-side files still need source/notice
provenance or a controlled replacement. Remaining Android notices and final
distribution review also stay open; six APK version markers were invalid, so we
do not treat them as proof of resolved versions. Fifteen focused audit tests
passed; all current native hashes, 60 official package matches and the collected
sources were checked. The existing APK was not replaced or installed, and new
notices enter its next packaging pass. [Details](docs/release/beta1/DEPENDENCIES.md).

Next: prioritize the remaining PRoot/BusyBox/support-library sources and Android
notices, replacing only components whose provenance cannot be recovered. Test the
resulting candidate on the handheld only when the owner frees it.

## First early beta preparation (0.0.93)

The owner chose **early beta** as the first public milestone. This replaces the
alpha name from earlier today, while keeping honest limits: some creation still
needs Lua, and the full no-code product is later work. [Scope](docs/BETA_SCOPE.md).

The new backup flow checks an external ZIP containing current projects, assets,
persisted drafts/settings and runtime saves/downloads. Restoration first verifies
and stages a copy, then retains previous private data while switching versions.
It can resume or reverse interrupted renames. Android/provider acceptance remains
open; the new guide explains exclusions and both required reopen steps.

Splore now has an entry in Play. Its persistent downloaded cartridges are indexed
on the same shelf after return, with labels, available names, favorites and recents.
Direct launch uses the existing cache. Local fixtures are tested; actual downloading,
offline launch and multicart continuation still need the handheld. Arbitrary external
Downloads-folder synchronization remains unfinished.

All 91 core suites and final integrated package verification passed locally.
The local candidate uses one icon/APK, disabled debugging and the same signing
certificate for an in-place update. README, setup/recovery instructions, draft release
notes and 13 current host-rendered website previews are prepared. These are not new
Android screenshots. [Images and build evidence](docs/showcase/0.0.93-en/README.md).

**Still blocking publication:** native dependency matching-source/notice materials,
authorized device/setup/update/restore/controller journeys, and the owner's actual
candidate review/publication approval. The updated 79-file native audit makes the
remaining source work explicit. [Checklist](docs/release/beta1/README.md).

Next: resolve the native distribution sources while the console remains occupied;
when freed, test the exact candidate and replace useful previews with native captures.

## Previous decision: alpha direction and setup changes (0.0.92)

The first milestone is now an alpha: it will offer the existing player and tools,
with Lua still needed for gameplay beyond the supported visual paths. Complete
no-code creation for more genres will follow in updates. This is not publication
approval. [Scope and remaining gates](docs/ALPHA_SCOPE.md).

Runtime setup accepts supported Raspberry Pi ARM64 contents without requiring a
particular ZIP name, version number, enclosing folder or readme. Multiple runtimes
and incomplete/wrong-platform archives are refused. The guide explains the archive
type without a fixed-version example and opens creation-folder settings directly.

**Checked locally:** 89 core suites passed; integrated APK produced and its signature/content verified. Host previews were reviewed; no device use.

Evidence and command outcomes are recorded in
[0.0.92 evidence](docs/showcase/0.0.92-en/README.md). No handheld was used. The
wizard still needs independent Android/provider/controller/novice acceptance;
accepting archive structure is not proof that every old/new version runs.
Full backup/recovery remains unfinished and is the next local block.

## Previous implementation: your reusable library in its own folder (0.0.91)

The library can now use a selected Android folder for sprites, tool settings and
the shared sprite buffer. Naming, categories, favorites and reuse follow that
location. Activation checks a fresh copy before switching and retains the internal
originals. Lost access has a reconnect path. Projects keep their independent location.

Old preset field labels remain usable during explicit metadata edits. Transfer
preserves resource files byte for byte. This adds no new music or SFX library type.

**Checked locally:** 89 core suite invocations, including new library checks, and
integrated single-APK packaging passed. Provider/device, visual/controller and
novice/owner acceptance remain open. The owner reaffirmed no handheld use on
2 November; it was not accessed. [Evidence](docs/showcase/0.0.91-en/README.md).

Full reinstall backup/restore is still unfinished. Keep the app installed: settings,
unfinished edits and recovery records include private state. Returning to internal
originals does not merge newer external edits. [Guide](docs/EXTERNAL_LIBRARY.md).
Next complete a bounded backup/restore workflow; native journeys wait for permission.

## Previous implementation: projects in your own folder (0.0.90)

Projects can now use a selected Android folder as their working location. Activation
creates and checks a fresh workspace before switching; the internal originals stay
available separately. Save keeps the previous version, and interrupted saves have
a recovery path. Lost folder access offers reconnection instead of silently opening
older copies. Library storage and game saves remain in their current locations.

**Checked locally:** 88 core suites, including 61 new recovery assertions, and
integrated APK packaging. Simulated provider failures were exercised before and
after each save step, alongside real editor Save/Undo/reopen through the domain.
Android folder/provider, visual/physical-input and owner acceptance remain open;
the console was not used. [Scope and evidence](docs/showcase/0.0.90-en/README.md).

This does not complete full backup/restore. Keep the app installed: settings,
unfinished drafts and interrupted-save records remain private. Returning to internal
originals does not merge newer external edits. [User guide](docs/EXTERNAL_PROJECTS.md).
Next connect external library storage, then complete authorized device journeys.

## Previous implementation: preparing projects for external storage (0.0.89)

Project creation, import, opening and saving now share one storage interface.
Saving checks that the source still matches the edit, and new projects are checked
before appearing on the shelf. Existing project folders and cart bytes are preserved.
This is internal groundwork for external storage, with no new screen or external
working location enabled yet. The verified-copy workflow remains available.

**Checked locally:** 87 core suites passed, including 40 new persistence assertions
covering real editor Save/Undo/Redo, failed writes and source conflicts. Android
compilation and integrated APK packaging are recorded in the evidence below.
Device/provider acceptance remains pending; console use stays paused.
[Evidence](docs/showcase/0.0.89-en/README.md).

Next: external project adapter, safe activation and reopening after access loss;
then the corresponding library path. Settings, saves and unfinished drafts still
need their separate complete-backup workflow.

## Previous implementation: checked copies of your creations (0.0.88)

Projects and reusable assets can now be copied to their selected external folders.
Each copy gets a new directory. PikoNest rereads every file and checks its contents
before reporting success. Cancelled copies keep all files; Resume checks the last
candidate and continues when existing files match. A damaged partial copy stays
available and requires a fresh copy.

**Checked locally:** 86 core suite invocations and single-APK packaging passed,
including 36 new transfer checks. Eleven additional checks exercised the actual
local source adapter with ordinary cart bytes, metadata and library files.
Android providers, process death, physical controls and visual/device acceptance
remain pending. Console use remains paused.

Working storage still lives inside the app. This copies files, not settings,
unfinished editor drafts or game saves. Activating external working storage is
the next block. [User path and limits](docs/STORAGE_COPY.md) ·
[Evidence](docs/showcase/0.0.88-en/README.md).

## Previous implementation: a gentler first start (0.0.87)

A short English guide connects a games folder, explains where creations live and
opens setup for the user's purchased Raspberry Pi PICO-8 ZIP. It remembers the
current step, can be skipped, and can be reopened from Folders. PICO-8 preparation
now follows archive import without automatically opening a game. The optional test
is a separate action; a preparation failure can retry the saved archive.

Games folders connect to Play. Projects, assets and Splore downloads still live
inside the app: other saved folder choices are explicitly labelled reserved.
Activating external project and library storage is the next implementation block.

**Checked locally:** 85 core suite invocations and integrated APK packaging passed;
the new guide's state checks cover skipping, progress, cancellation, late callbacks,
failed saves and preparation-versus-testing. Compact, tall and wide host layouts
were reviewed. Android folder grants, actual runtime preparation, interruptions,
physical controls and a novice first-run journey remain untested for this batch.
The console was not used. [Evidence and host previews](docs/showcase/0.0.87-en/README.md).

## Previous implementation: a helpful place to start (0.0.86)

New project now offers Blank, Little explorer and Light puzzle with a picture,
a short explanation and play controls. Collect stars in the explorer game, or
light linked lamps in a puzzle with no moving character. Both are separate editable
games; changing one never changes the starting example or another project.

The same tools remain available in Blank and both starts. Press X / Help to read
an explanation and jump to drawing, behavior, animation, tiles, sounds, music or
goals. Missing elements have a setup action. Export guidance leads back to Projects.
Older examples and existing projects remain available.
[Scope, carts and host previews](docs/showcase/0.0.86-en/README.md).

**Checked locally:** 84 core suite invocations and integrated APK packaging passed.
The new creation/edit/recovery/export journey passed focused checks. Both games
won and restarted in host Lua checks; actual previous-version tile drafts restored.
Compact/tall/wide layouts received host review. These checks do not replace
playing in official PICO-8 or using real handheld controls.

**Still pending:** official-runtime execution, installation, physical controls,
independent exports and novice/owner journeys for labs 0.0.78–91. Console use
remains paused. No native screenshot pack or public APK release was produced.
[Issue #17](https://github.com/EriArk/PikoNest/issues/17) remains open.

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
range editing and reuse, advanced SFX tools,
collision/door/parallax workflows, broader rules, library insertion and
multi-type reuse, remaining sprite/map operations, complete onboarding/storage
and recovery, modern Android compatibility, English coverage and wider device
and physical-controller validation. Undo history is session-local. The last demonstrated Android export picker used touch.

The browser creation-flow prototype remains design material. Native work now
covers the two small blank-project journeys above. Editable starting content and
optional help are now implemented locally; learning progress and the complete
unassisted beginner experience remain unfinished.
No public beta or release readiness is claimed.

## What comes next

1. Finish the alpha's data-preservation path: complete backup/recovery alongside
   the current setup and external storage controls.
2. When console use is authorized, prove independent setup and the declared
   Play/create/edit/Test/export journeys; fix observed usability problems.
3. Document supported devices/runtime versions and remaining alpha limits, then
   have the owner review the build and separately authorize any APK publication.
4. Expand general rules, world/collision, animation/audio and reuse in updates
   toward complete no-Lua creation. Full-product acceptance remains open.

[ROADMAP](docs/ROADMAP.md) owns sequence; [BACKLOG](docs/BACKLOG.md) owns task status.

## Images for posts and the website

| Material | Use it for | Required context |
| --- | --- | --- |
| [0.0.86 starting games and help](docs/showcase/0.0.86-en/README.md) | Starter selection, optional guidance, ordinary example carts | Host-rendered production UI and generated carts; not native acceptance |
| [0.0.85 tile editor previews](docs/showcase/0.0.85-en/README.md) | Tile painting and review | Host-rendered production UI; not native screenshots or device acceptance |
| [0.0.77 screenshots and two games](docs/showcase/0.0.77-en/README.md) | Movement, contact, goals, linked switches, official play and export | Native development build; bounded creation path, not a public beta |
| [0.0.76 screenshots](docs/showcase/0.0.76-en/README.md) | Drawing/placement and shared/independent appearance | Earlier static Game view slice |
| [Full screenshot index](docs/showcase/README.md) | Play, maps, animation, camera and other tools | Keep each pack's version and provenance |
| [Creation-flow study](docs/design/creation-flow-2026-10-08/README.md) | The broader proposed experience | Label as design, not an Android feature |

## Short update to adapt for a post

We are aiming for a PikoNest alpha first. It will include the existing player and
workshop, while more advanced game logic may still need Lua. Runtime setup now
checks supported Raspberry Pi contents without tying the import to one ZIP name
or version. The guide also leads directly to creation-folder settings. Local
checks are recorded; handheld and beginner setup review are still ahead.
