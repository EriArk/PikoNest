# PikoNest 0.1.0-beta.2

11 November 2026 · source lab 0.0.96 · Android version code 96.
[Download beta 2](https://github.com/EriArk/PikoNest/releases/tag/v0.1.0-beta.2).
The owner explicitly authorized finishing, device validation and publication on
11 November. This release includes the wall and passage work from labs 0.0.94–95.

Create a small game from Blank: paint a moving sprite, a pressure plate, a wall
with a linked passage, and a goal. Step onto the plate, cross the passage, win and
restart. This complete journey was authored through the native UI and ran in the
user's official PICO-8 on Retroid. Export remains ordinary `.p8` Lua/data.

## Install

Update the existing app **in place**, without uninstalling. Package and signing
identity match beta 1. Keep a backup outside the app before uninstall/reset.
New users import their purchased Raspberry Pi ARM64 PICO-8 ZIP; it is not bundled.
One APK and launcher, Android API 26 minimum / interim target 28. Retroid Pocket
Classic / Android 14 / official PICO-8 0.2.7 is the tested combination.
[Setup instructions](../beta1/README.md#install-and-start) remain applicable.

## New functionality

- Paint wall types with an all-use review; copy a tile for an independent placement.
- Bind up to 32 individual wall cells to a toggle or pressure plate, opening when
  On or Off. Closing waits for moving objects to leave the cell.
- Choose **Behavior → On contact / press → Pressure plate → Pressed by**. The
  plate is On while that selected element overlaps it. No global switch selection
  is required. State animation, press SFX and the all-switches-on goal can use it.
- Save, Undo/Redo, isolated draft Test and recovery preserve the authored links.
  Old journals remain readable, including the previous interaction-menu Cancel.

[Device journeys and limitations](DEVICE_ACCEPTANCE.md) ·
[Native screenshots](../../showcase/beta2-en/README.md) ·
[Technical behavior contract](../../WORLD_EDITOR.md).

This remains an early beta: bounded visual recipes, rectangular contact/movement,
one activator per plate, no general physics or arbitrary-Lua conversion. Manual
switches still share left/right selection with movement. Physical controls,
audible sound, novice usability, broader devices and the remaining provider,
launcher and multicart matrix are not newly accepted by this release.

## Artifacts and sources

- `PikoNest-0.1.0-beta.2.apk`: 52,114,641 bytes, exact installed/tested candidate.
- `PikoNest-0.1.0-beta.2-support-sources.zip`: 193,164,522 bytes. Byte-identical to
  beta 1's reviewed support-source bundle because the runtime payload is unchanged.
  Its internal beta-1 names identify that original dependency review.
- [SHA256SUMS](SHA256SUMS), matching source/tag and [build record](BUILD.md).

No purchased runtime, downloaded third-party cart, private backup or signing key
is included. The sample carts and artwork here are original project test material.
