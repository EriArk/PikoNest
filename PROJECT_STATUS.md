# PikoNest — project status

Updated **2 November 2026**. Plain-language material for posts and the website.
Historical batches are in [CHANGELOG.md](CHANGELOG.md).

## At a glance

PikoNest is a PICO-8 player and game-making workshop for Android handhelds.
Play, Workshop and purchased-runtime setup live in one APK. The first early-beta
**[0.1.0-beta.1 is the first public early beta](https://github.com/EriArk/PikoNest/releases/tag/v0.1.0-beta.1)**. It is installed and
technically checked on the Retroid. The owner explicitly approved this exact
candidate with the recorded limitations; untested journeys remain open.

| Item | Current position |
| --- | --- |
| Version | 0.1.0-beta.1, source lab 0.0.93, Android code 93 |
| Device | Retroid Pocket Classic / Android 14 / official PICO-8 0.2.7 |
| Latest work | Authenticated runtime-support refresh, completed dependency materials, actual device journeys and seven native screenshots |
| Current focus | Collect early-beta feedback; finish the deferred physical/audio/provider matrix |
| Platforms | Android first; Linux later |

## What changed

The old unknown runtime-support files now have authenticated replacements with
matching sources and notices. Eighty of 82 native files match official packages;
the two permissive components have identified source/notice records. The prepared
source bundle holds 185 pinned materials plus rebuild instructions and configuration.
The purchased PICO-8 runtime stays out of both APK and source downloads.

Splore downloads now show their actual available titles and the shelf handles
more than the old 128-cart limit. The Retroid shows 190 cached games. Fixed sprite
thumbnails that could repeat the last image, and moved archive import errors into view.

## What ran on the handheld

- Updated the main app in place; all 56 previous project files and four library
  files matched their pre-update hashes. A private rollback backup is retained.
  Disabled the obsolete separate runtime prototype without deleting its data;
  only the unified PikoNest application remains enabled.
- In a separate empty validation install, completed setup, rejected a desktop
  archive, accepted a renamed ARM64 archive, prepared it and ran the runtime probe.
  That temporary validation app was removed afterwards.
- Downloaded a new game through Splore, found it in Play, kept it as a favorite,
  restarted without Wi-Fi and launched it directly to its title screen.
- Made artwork and D-pad movement from Blank through the UI, tested and exported
  exact cartridge bytes. Both editable starters reached their win states; the
  lamp puzzle also demonstrated state-driven frames and restart.
- Edited/saved a sound, used Undo, saved a music pattern/game binding and opened
  both runtime previews. The device remained muted, so listening quality is unverified.
- Created and restored a clean-project backup through Android's document picker;
  restored game bytes matched. The final main-app backup separately verified 355 files.
- External Android launch, background/resume and exit worked with a VIEW intent.

All 91 core suites passed, plus 20 dependency-audit tests, final package checks and
the strict dependency/source gate. Two later UI-only fixes were repackaged and
visually checked on-device. [Exact evidence and build identities](docs/release/beta1/DEVICE_ACCEPTANCE.md).

## What is still unfinished

This is not a complete no-code creator. Arbitrary Lua and broader genre logic
remain outside supported visual paths. Physical controls, audible playback, novice
usability, other devices/runtime versions, fresh launcher-app UI checks and the
remaining external-storage/interrupted-restore matrix still need acceptance.
Multicart offline continuation and arbitrary external Splore download folders are open.

More no-Lua logic, richer asset reuse, modern Android targeting and complete
localization are later product work. A successful technical test does not substitute
for physical or novice acceptance. [Recorded limits](docs/release/beta1/README.md).

## Pictures and next step

[Seven current native English screenshots and PNG pack](docs/showcase/beta1-en/README.md)
cover Play, Game view, sprite/SFX/music editing and both games in official PICO-8.
The root README now uses these actual captures. Earlier host previews retain their labels.

Next: collect early-beta feedback and complete deferred physical-controller, audible
audio, provider/interruption and wider-device checks. Then prioritize the next
complete no-Lua creation workflow. Keep broader product goals separate from this release.
