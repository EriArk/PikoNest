# PikoNest — changelog

Plain-language development updates for posts and the website, newest first.
Version numbers below identify **development builds**, not public APK releases.
There is no public APK release as of 11 October 2026.

This file was started on 11 October 2026 and backfilled from documented work
from 5 October onward. It is a curated history, not a complete commit log.
Earlier work is recorded in [the technical status](docs/STATUS.md) and
[versioned evidence](docs/showcase/README.md). See
[PROJECT_STATUS.md](PROJECT_STATUS.md) for the current overall picture.

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
