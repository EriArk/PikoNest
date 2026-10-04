# PNG cartridges on Play — Android lab 0.0.23

2026-10-03. Actual Retroid Pocket Classic captures, user-supplied official PICO-8
0.2.7 via the existing external experimental wrapper. Installed versionCode 23.
APK SHA-256: `229e586fab988d04953712a2a91fa260d0da62f720126382ef1b50204eb1acef`.

## What works

- [Play shelf](home.png): actual PNG label alongside text carts; format captions
  distinguish identical titles. Existing controller actions are unchanged.
- [PNG running and responding to input](runtime.png): Little Lights, move count 1.
- [Return after host process death](returned.png): same PNG selected, Play restored.
- [Corrupt file rejected before launch](corrupt.png): fresh bytes checked even when
  the shelf still has the previously loaded cover.
- [Text cart after PNG](text-after-png.png): Moon Garden starts from the separate
  text snapshot, after restarting the external wrapper (see limitation below).

Created our own PNG fixture from `assets/lights.p8` using the official runtime's
Ctrl+7 label capture and EXPORT command. The file is also in tests/fixtures with
provenance. No PICO-8 binaries or third-party cart code were added to the repo.
Games now contains this new file alongside the three previous text samples.

The original PNG, fixed provider snapshot and runtime intake have the same binary
transport path; device SHA-256 of source and `files/run.p8.png` both equals
`11bb25685b0811ae858c9a8f7265c96a83a0f1d253008c0e10d6b485ab8f791e`.
The explicit send intent uses `image/png`, `pikoos-lab.p8.png`, read-only stream
permission and a matching ClipData URI. PikoNest never re-encodes the image.

During PNG play, `am kill` removed the background host process (PID absent).
Ctrl+Q returned to a recreated Play screen with the same selection and recency.
Temporarily replaced only this turn's new owned PNG fixture with four invalid
bytes; A produced the error without changing the staged valid PNG. Restored the
fixture in `finally`; L rescanned. All fifteen pre-existing project cartridge
hashes and the three previous Games text files matched before/after.

## Evidence limits and next gate

Repeated testing exposed a **warm-launch failure in the external wrapper**:
`SDL Error: Could not setup connection to PulseAudio` followed by
`FATAL ERROR: Unable to initialize SDL`. This happened with ordinary text carts
as well as during the preparation sequence. Force-stopping/relaunching the wrapper
restored audio initialization and game launch. The PNG Play launch itself passed
without that intervention after installation; subsequent text launch needed it.
After exiting Moon Garden through the native pause menu's SHUTDOWN, a subsequent
PNG launch succeeded without force-stop. Compare that path with Ctrl+Q during
the next lifecycle investigation; the failure is intermittent, not yet isolated.
This is not fixed by the PNG change, and accepted Android intent delivery is not
proof of game execution. Prioritize a reproducible launch/exit/audio lifecycle
fix before calling the launcher reliable or completing first-run runtime setup.

Input was injected semantic controller actions for the host and held keyboard
equivalents for native PICO-8. This does not replace physical-controller owner
acceptance. Layout evidence here uses the normal 1240×1080 device display.

Only the PNG envelope and cover are decoded: 160×205 RGBA8 non-interlaced, all five
filters, CRC validation, exact bounded inflation. Other variants are explicitly
unsupported. An ordinary image with this envelope is not proven to be a valid
game; the official runtime remains authoritative. Compressed Lua, external file
dependencies, linked carts, PNG editor import and Splore folder binding remain
open. Existing one-folder-level / 128 carts / 2 MiB per file lab limits remain.

`P8PngTest`: 98,361 checks, including independent JDK pixel comparisons, synthetic
filter fixtures, official export version 43, immutable input, CRC/truncation,
dimensions/depth/type/interlace errors, inflated over/underflow and old-backend
PNG rejection. All existing portable suites, APK build and signing checks pass.
The fixture was refreshed to a clean label and its targeted suite rerun afterward.

See [format sources/provenance](../../../experiments/android-host/tests/fixtures/lights-0.2.7.md)
and the [overall completion path](../../ROADMAP.md#completion-path).
