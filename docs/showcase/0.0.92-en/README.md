# Lab 0.0.92 — alpha scope and flexible runtime setup

2 November 2026. Local source/tests and host layout evidence. **No handheld use,
installation, runtime execution or release.** Owner has selected an alpha milestone
with Lua allowed outside the supported visual paths. [Scope](../../ALPHA_SCOPE.md).

## Implemented

- Archive discovery accepts one ARM64 `pico8_64` and sibling `pico8.dat` in a safe
  root/nested/renamed directory. No readme or fixed version/ZIP name is required.
- Installation and probe packaging consume the inspected paths; destinations stay
  fixed. Multiple runtimes, mismatched data, unsupported ABI, damaged/unsafe ZIPs
  and oversized input remain refused.
- Setup guidance explains architecture versus version and preparation versus Test.
- Creations opens Folders without dismissing the guide; Right/Next can skip that
  action. Existing folder activation/reconnect controls are reused.

## Local checks

- Full core suite: 89 invocations passed.
- `RuntimeArchiveTest`: 44 synthetic assertions. Includes optional readme, four
  layouts, mismatched siblings, multiple runtimes, ARM32-only rejection, ELF fields,
  CRC damage, unsafe paths, limits, cancellation and source preservation.
- Repeated the archive suite with the three owner-supplied **0.2.7** ZIPs: Raspberry
  Pi accepted structurally; amd64/i386 refused (46 assertions plus real archive
  inspection). No purchased bytes are included here or in the APK.
- `RuntimeProbeTest`: 19 assertions, including byte-exact prepare/unpack from three
  alternate layouts into fixed output filenames. These use synthetic binaries.
- `FirstRunSetupTest`: 42 assertions, including creation-folder navigation without
  dismissal/runtime launch, skipping, ABI/version guidance and prior recovery cases.
- Android source compilation and integrated packaging completed. The outer
  PowerShell logging command returned 1 after treating javac's obsolete Java 8
  stderr warnings as `NativeCommandError`; no test/compiler failure was recorded.
  Separate signature and APK/bootstrap verification passed with exit 0: one
  launcher, private runtime process, 263 expected Godot assets and no purchased
  runtime binaries. Hash and command outcome are recorded in `checks.json`.

## Host layout review

The images below use the production View rendering code with local Java2D Android
drawing adapters and Monocraft. They are **host previews, not Android screenshots**.
Compact and wide layouts were inspected for readable guidance and reachable actions.
Scrollable content does not certify actual Android focus/touch/controller behavior.

| Image | State |
| --- | --- |
| [Archive setup, compact](archive-compact.png) | Version-independent archive guidance, fixed Choose ZIP action, scroll hint |
| [Creation folders, compact](folders-compact.png) | Internal storage default and direct folder action |
| [Creation folders, wide](folders-wide.png) | Same operation and information on a wide layout |

## Still open

- Actual clean-install wizard and provider grants; navigate from guide to Folders,
  activate/reconnect projects/library and resume setup on Android.
- Physical controls, scroll behavior, compact/wide native readability and novice review.
- Actual preparation, launch/return/audio/input on every claimed supported runtime.
  Synthetic older/future folder names do not prove execution of another version.
- Unsupported ABI/old-only packages, preparation interruption and replacement on device.
- Full backup/reinstall recovery and the owner's separate release approval.

The last full native creation journey remains 0.0.77. Existing no-Lua product
acceptance scenarios stay open; the alpha decision does not mark them completed.
