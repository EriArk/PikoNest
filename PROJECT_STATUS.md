# PikoNest — project status

Updated **11 November 2026**. Plain-language material for posts and the website.
Historical batches are in [CHANGELOG.md](CHANGELOG.md).

## At a glance

PikoNest is a PICO-8 player and game-making workshop for Android handhelds.
Play, Workshop and purchased-runtime setup live in one APK.
**[0.1.0-beta.2](https://github.com/EriArk/PikoNest/releases/tag/v0.1.0-beta.2)** is the
second early beta, installed and technically checked on Retroid. The owner
explicitly authorized finishing and publishing it on 11 November.

| Item | Current position |
| --- | --- |
| Version | 0.1.0-beta.2, source lab 0.0.96, Android code 96 |
| Device | Retroid Pocket Classic / Android 14 / official PICO-8 0.2.7 |
| Latest work | Walls, linked passages and pressure plates in the game-making workflow |
| Current focus | Functional game creation first; feedback and remaining device acceptance |
| Platforms | Android first; Linux later |

## What this beta adds

Make a small game from Blank: draw a moving sprite, paint walls, add a pressure
plate and link it to a passage. Add a goal, Test, cross the opening, win and
restart. This entire example was made through the native interface on Retroid,
without editing Lua. Its exported ordinary `.p8` also ran independently.

Wall rules have an all-use review and an independent tile-copy path. A passage
changes only its selected cell and waits for moving objects to leave before it
closes. One toggle or plate can control several cells, opening when On or Off.
A plate stays On while its selected activator overlaps it. Existing state
animation, press sounds and the all-switches-on goal can use that state.

The supported workflow includes Save, Undo/Redo, isolated draft Test and recovery.
Old drafts remain readable. Custom Lua is preserved; this does not turn arbitrary
cartridges into visual projects. [Scope and samples](docs/release/beta2/README.md).

## What was checked

- Updated Retroid without uninstalling: all 58 previous project files and five
  library files matched the retained pre-update backup. The backup UI verified
  355 files before and after the update.
- Authored the complete plate/passage/goal game from Blank, tested the world draft,
  returned, saved, used Undo/Redo and reopened after app restart.
- Exported 9,406 bytes through Android's picker with readback equality, then
  launched that file separately and demonstrated win/restart in official PICO-8.
- A separate generated-code probe passed 13 official-runtime checks for plate
  overlap, safe passage closing, blocking, win and restart.
- All 94 core suites passed; final focused recovery checks and four desktop Lua
  variants passed. Package, signature, exact host dex and native/source audits passed.

[Exact candidate and evidence boundaries](docs/release/beta2/DEVICE_ACCEPTANCE.md).
Automated inputs were ADB-injected and audio muted. After publication on
11 November, the owner separately confirmed that physical buttons and audible
sound work on the Retroid. Novice usability and broader ergonomics remain open.

## Existing Play and setup foundation

One APK contains Play, Workshop, purchased-runtime setup and backup/recovery.
Play supports favorites, recent games, external launch and Splore's persistent
cache with available cartridge titles. The reviewed runtime-support payload has
matching sources/notices and is unchanged from beta 1. PICO-8 itself is never bundled.

Beta 1 demonstrated separate first-run setup, Splore download/favorite/offline
launch, clean-project restore, starter games and audio previews. Those remain
historical checks; the entire matrix was not repeated for beta 2.
[Previous device evidence](docs/release/beta1/DEVICE_ACCEPTANCE.md).

## What is still unfinished

This is a bounded visual creator, not complete no-Lua support for every genre.
General physics, richer event/state composition and broader asset reuse remain
future work. Manual switches still share directional selection with movement.
Novice usability, other devices/runtime versions, fresh third-party launcher UI,
provider/interruption restore cases,
multicart continuation and external Splore-folder synchronization remain open.
Modern Android targeting and complete localization are still required follow-ups.

## Pictures and next step

[Six native beta-2 screenshots](docs/showcase/beta2-en/README.md) show the pressure
plate, passage binding, draft review, export and official-runtime results.
[Beta-1 gallery](docs/showcase/beta1-en/README.md) covers the broader Play/editing UI.

Next: extend connected gameplay scenarios, including richer state and event
actions, alongside beta-2 feedback and the remaining acceptance work.
Keep save-state research and community-link work behind functional creation.
