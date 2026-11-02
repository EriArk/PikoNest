# PikoNest 0.1.0-beta.1 — little worlds, made here

**The first public early beta, approved with the limitations below.**

The first early beta brings a PICO-8 player and game-making workshop to Android
handhelds. One APK, one icon: Play when you want a game, Workshop when you want
to change something or make something of your own.

## Inside this beta

- Browse cartridges, favorites and recent games. Open official Splore and find
  downloaded games back on the Play shelf, ready for direct launch.
- Start from Blank, a tiny collection game or a hero-free lamp puzzle. Edit artwork,
  movement and supported rules, animation, sound, music and tile worlds.
- Keep ordinary `.p8` cartridges underneath. Test in official PICO-8 and export;
  supported visual actions generate ordinary Lua/data.
- Reuse sprites/tool settings, choose project/library folders and make checked
  backup ZIPs of creation data, settings, saves and downloaded cartridges.

## Before installing

You need **ARM64 Android (minimum Android 8)** and your purchased **PICO-8 Raspberry
Pi ZIP containing `pico8_64`**. Download it from your Lexaloffle purchase page and
import it through setup. Desktop x86/Windows/macOS and ARM32-only builds do not work.
PICO-8 is never included. Compatible archive names/versions can vary; preparation
checks contents and **Test** checks execution.

Update PikoNest in place. Do not uninstall it to update. Keep an external backup
before uninstall/reset; runtime installation and Android permissions must be
re-established separately. Some system folder pickers require touch.

## Tested scope

Retroid Pocket Classic, Android 14, official PICO-8 0.2.7 Raspberry Pi ARM64.
Verified in-place preservation, independent setup, Splore download/favorite/offline
title launch, blank sprite/movement/export, both starter wins, audio preview launches,
external Android intent/resume and clean-project backup/restore.

91 core suites and 20 dependency audit tests passed, followed by final package
and affected native UI checks. Corresponding dependency sources, recipes,
configuration and notices accompany the APK.

## Early means early

Custom gameplay may need Lua; unfamiliar Lua cannot automatically become visual
controls. Modern Android targeting, wider reusable asset types, external Splore
folder sync and complete localization remain future work. The backend uses target SDK 28.

Physical/novice controls, audible sound (tests were muted), wider device/runtime
coverage and remaining provider/interruption/multicart journeys are not claimed
passed. The owner accepted this bounded first-beta scope; those checks remain
follow-up work, not completed tests.

## Downloads

- `PikoNest-0.1.0-beta.1.apk` — Android application.
- `PikoNest-0.1.0-beta.1-support-sources.zip` — corresponding support sources,
  patches, recipes, configuration, notices and rebuild instructions.
- `SHA256SUMS` — checksums for both files.
- Repository source at the matching tag — PikoNest code and integration patches.

APK SHA-256: `e496a14cc5751232850fa76b393f99ce9b5fb88668c9dbb6a8317ffc2cda224f`.

Original PikoNest code is MIT; dependencies and third-party games retain their own
terms. PICO-8 is by Lexaloffle Games. PikoNest is independent.

[Setup and known limits](https://github.com/EriArk/PikoNest/blob/v0.1.0-beta.1/docs/release/beta1/README.md) · [Actual screenshots](https://github.com/EriArk/PikoNest/tree/v0.1.0-beta.1/docs/showcase/beta1-en) · [Device evidence](https://github.com/EriArk/PikoNest/blob/v0.1.0-beta.1/docs/release/beta1/DEVICE_ACCEPTANCE.md).
