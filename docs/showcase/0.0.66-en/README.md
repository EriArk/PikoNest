# PikoNest — English interface screenshots, lab 0.0.66

Captured 2026-10-05 for the website. These are original PNG screenshots from the
**regular development APK**, not translated overlays or a special capture build.
No cropping, retouching or resizing was applied.

The pack covers the first shared-interface slice: Play, projects and their actions.
Other editors, settings and some underlying error messages still need migration
and English localization. Use the [0.0.65 editor pack](../0.0.65-en/README.md) for
historical editor images, clearly labelled by version; this pack does not imply
that every tool already has the new interface or complete English support.

## Website images

| File | Suggested caption / alt text |
| --- | --- |
| [01-play-library.png](01-play-library.png) | Browse local PICO-8 games in a focused, controller-first library. |
| [02-favorites.png](02-favorites.png) | Keep favorite games close at hand. |
| [03-workshop.png](03-workshop.png) | Your own projects, with a familiar home in the Workshop. |
| [04-project-actions.png](04-project-actions.png) | Create, copy, import and export from one project menu. |
| [05-new-project.png](05-new-project.png) | Start with a blank project or choose a small starting game. |
| [06-copy-project.png](06-copy-project.png) | Make an independent copy while preserving the original. |
| [07-compact-workshop.png](07-compact-workshop.png) | A compact project list keeps the same controls and readable type. |

Images 01–06 are 1240×1080; image 07 is 720×960. The black strip at the top is the
emulated device's reserved display area, retained in the original capture.
Demo content is our existing Moon Garden, Lights and Little Hearts fixtures.
Project creation and copying were performed through the app; the user's Retroid
projects were not used as disposable demo data.

## What changed

- Shared shelf rendering: quiet blue surface, pink navigation marker, one yellow
  list focus, regular spacing and a preview alongside the list on larger screens.
- One controller-accessible action menu. Up/down selects a game/project; A opens,
  B returns to the parent, Select opens actions. Play retains X favorite/Y filter
  and Start launch. L/R move within the list rather than silently changing products.
- Copy opens an explicit confirmation. Cancel and Start do not publish a new file.
- New projects start on the blank choice when that template exists.
- B on the project home returns to projects; B on projects returns to Play.
- English is part of the shipped development source for these screens/dialogs.
  Generated default project labels are English; imported/user-authored names,
  cartridge code/data and technical IDs remain unchanged.

## Verification and limits

Capture environment: isolated read-only Android 15 x86_64 emulator instance,
320 dpi, ADB controller events. Android's folder picker was operated with taps;
the shelf/create/copy/open/back paths used semantic controller input via ADB.
This is **not** a claim of physical controller acceptance or a fresh official
PICO-8 gameplay test on the emulator.

The same final APK was installed on Retroid Pocket Classic. All 48 existing
project/asset files matched their pre-update SHA-256 hashes. Its unresolved
runtime-session gate from the audit was not bypassed or force-closed; live Retroid
navigation/return was therefore not revalidated here. Device audio was not enabled.

Core verification covered all test programs invoked by `build.ps1`, across the
initial run and focused continuations. Old tests that assumed immediate X-copy
or the former action grid were updated to the explicit menu/confirmation route;
their data-preservation and failure assertions remain. Library workflow: 31
checks; hero-free workflow: 100; import: 29; export: 41;
Play: 16,411. UI-only refinements were recompiled after visual inspection without
repeating unrelated domain tests.

Observed app path: Play/favorite/filter → Workshop → create blank and Lights →
open/back → cancel copy → confirm independent copy → back to project list → Play.
Native-size screens and compact list were inspected. Remaining UX02 scope includes
full long-title/error disclosure, settings migration, broader aspect-ratio tests
and physical/owner acceptance. Existing editor-specific controls are UX03/UX05.
Splore navigation is not shown as a working button before its runtime entry exists.

Build hash, file hashes, preservation result and test counts are in
[capture.json](capture.json). No APK or GitHub Release is published by this change.
