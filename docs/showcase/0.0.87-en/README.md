# Lab 0.0.87 — optional first-start setup

Local implementation and evidence, 1 November 2026. This is not a release.
The console was not accessed; the last complete native creation journey remains
0.0.77. APK SHA-256: `d5f9e00330c80ac48035089c6d60a880d6901be45f271222ca6f28332cb72b24`.

The guide joins Games, storage explanation and purchased-runtime setup. It can
be skipped or reopened from Folders → Select. Archive import automatically
continues into preparation; an actual test launch needs a separate user action.
Folder and archive errors retain their recovery paths. Detailed limits and
data-preservation behavior are in [FIRST_RUN_SETUP](../../FIRST_RUN_SETUP.md).

## Host previews, not device screenshots

| Image | What it shows |
| --- | --- |
| [Games](01-games-host-preview.png) | Initial folder connection and skippable steps |
| [Your PICO-8](02-runtime-host-preview.png) | Correct purchased archive type |
| [Optional test](03-optional-test-host-preview.png) | Prepared UI state; explicit Test action |
| [Folder destinations](04-folder-intentions-host-preview.png) | Connected Games versus reserved project destination |

These render the production FirstRunView, RuntimeSetupView and FolderView through
temporary Java2D Android stubs with the repository's Monocraft font. State is seeded
for layout review; the prepared screenshot is **not** evidence that runtime
preparation executed. Reviewed layouts: 360×480, 360×800, 620×540 and 960×540 logical
sizes. Images are rendered at 2× and must be labelled host previews if reused.
Original cartridge/nest artwork uses the PICO-8 palette. No purchased assets.

## Checks performed

- Full local suite: 85 invocations passed. FirstRunSetupTest adds 40 assertions
  covering entry deferral, saved progress, skip/re-entry, failed persistence,
  cancellation, stale folder results, retained prior choices and the separation
  between archive verification, preparation and an explicit test request.
- Existing folder/archive/probe suites passed (15 / 37 / 16 assertions), including
  source-byte preservation, invalid ABI/ZIP paths, CRC/size checks and cancellation.
- Integrated APK compiled, signed and passed package checks: one launcher,
  internal runtime process, 263 expected Godot assets and no official PICO-8
  binaries in APK/bootstrap. Final UI/launch-guard changes were recompiled and
  repackaged after the full suite; unrelated core tests were not repeated.
- Production-view compact/tall/wide host inspection caught header crowding and
  clipped hints; long guide/runtime text now has controller and touch scrolling.

## Not yet demonstrated

No native Android picker/grant or process-death run; no actual new runtime
preparation/launch; no physical controller, fresh-install, novice or owner review.
The Android workers are compiled and reviewed, while the automated tests exercise
portable state/data behavior. Those layers must not be conflated.

Projects, assets and Splore still live inside the app. Three external folder choices
remain reserved intentions; this guide performs no migration. Full external
storage activation, Splore indexing, backup/update and return to interrupted
external Test/setup workflows remain required. ACC-06 and R08 stay open.

No APK was installed, uploaded to GitHub or released. Next: journaled, verified
project/library copy-and-activation with cancellation, lost-access recovery and
original preservation. Device checks wait for owner authorization.
