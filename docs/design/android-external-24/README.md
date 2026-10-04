# External launcher play — Android lab 0.0.25

Follow-up: the interrupted-startup failure below is reproduced and fixed in
[runtime adapter revision 2](../android-resume-25/README.md). This page records
the original 0.0.25 checks before that adapter update.

2026-10-03. Actual Retroid Pocket Classic, Android 14, 1240×1080 landscape.
User-supplied official PICO-8 0.2.7 through the separate Runtime Test adapter.
Installed host versionCode 25; final APK SHA-256:
`e28ae9c5718126c519bb3b3ac3cf4f280314b2c6d1986ea7515cbff79657393b`.

## Real launcher acceptance

| Entry | Observed result |
| --- | --- |
| Beacon 1.8.10, custom PikoNest platform, Little-Lights (png) | Official game appears; Ctrl+Q returns to Beacon with the same PNG selected. Repeated on the final APK. |
| Retroid Launcher beta 1.16-2025-0618-1139, PikoNest platform, Little-Lights text cart | Safely Open reaches official game; Ctrl+Q returns to Retroid with the same text cart selected. Repeated on the final APK. |
| VIEW of a path outside the authorized Games tree | Clear access message; no runtime dispatch. A opens the system picker; choosing the PNG reaches official PICO-8. |
| Host process loss during that recovered game | PID 13961 disappears after `am kill`; runtime continues. Exit recreates host PID 14268, clears the dispatch journal and returns to Retroid without launching again. |
| A second, different external request during an active PNG game | Existing runtime PID 11220 and staged PNG hash remain unchanged; existing game remains visible. Tested before the final recovery-label/background-read refinements. |
| Empty request → picker → cancel → B | Returns to caller; journal stays false. No project import. |
| Normal PikoNest Play regression | Text cart runs, one injected move changes the board, exit restores the selected Play card. |
| External request while Play's runtime is still booting | New request is rejected and both snapshot hashes stay unchanged. Returning to the wrapper exposed the startup-interruption failure described below. |

Screenshots:

- [Game launched from Beacon](beacon-runtime.png), [return to Beacon](beacon-return.png).
- [Game launched from Retroid](retroid-runtime.png), [return to Retroid](retroid-return.png).
- [Access recovery screen](access-error.png).

The two launcher profiles are separate additions. Existing Beacon platforms,
including its PICO8/RetroArch configuration, remain untouched; Beacon remains
default HOME. A Beacon backup was saved before profile changes to
`/storage/7E6D-FA36/PikoNest-Launcher-Backups/beacon_backup_2026-10-03_11_47.zip`
(808,631,622 bytes). Retroid was initially empty; its new platform indexes four
sample carts. Metadata matching failed for the homebrew samples, so third-party
launcher covers remain empty. PikoNest's own Play shelf already uses cartridge
labels. No launcher APKs, backups or proprietary runtime files enter this repo.

See [exact profile settings](../../ANDROID_APP.md#tested-lab-entry-0025).
Retroid's suffix list needs `p8` and `png`, without dots. Its Safely Open prompt
reflects a running host process, not proof of an active native game.

## Preservation and validation

All fifteen pre-existing project cartridge hashes match the pre-test baseline.
The existing editor selection remains `remix-0009`; external entry never creates
a MainActivity/project session. All four source games remain unchanged:

| File | SHA-256 |
| --- | --- |
| Jelpi.p8 | `008bfd81b11296fb3c131f8a152bb6195b770bbcac582a1ffdf4983959561813` |
| Little-Lights.p8 | `99a38a85fa19403c715e61ddee8c68ec19db269cfbeee0a3a9c75308eb258903` |
| Moon-Garden.p8 | `1afbcdf44e942ab54fb6afd9daa52fdaf470adca02ff56430aa8c7616e08af76` |
| Little-Lights.p8.png | `11bb25685b0811ae858c9a8f7265c96a83a0f1d253008c0e10d6b485ab8f791e` |

`LauncherPathTest` covers internal aliases, removable storage, nested/unicode
paths, sibling-prefix confusion, traversal and malformed segments. All portable
host suites, APK compilation/signing and installation pass. The file picker
exercises a real permission-bearing content URI; real launchers exercise URI and
string-path transport. The exported activity grants no arbitrary filesystem or
runtime-command access. Shared format validation keeps the existing 2 MiB limit.

## Remaining boundaries

This is single-cart external play through the existing experimental backend,
not finished runtime packaging. There is no integrated first-run runtime importer
yet. Text dependency checks remain conservative; PNG code/dependencies are not
decoded. Multicart/include staging and nested Play indexing are separate work.

The wrapper has no reliable process/exit-result observation. A returned Activity
means only that control returned, not that a game completed successfully. A stale
dispatch journal offers explicit resume rather than overwriting a possible live
game; crashes, missing/uninstalled runtime and arbitrary task manipulation still
need a production recovery design. Repeated requests resume the existing game,
not the newly requested cart. Both entry origins guard the fixed runtime snapshot.

An additional failure was observed when interrupting Play's native startup with
an external request: after dismissing the rejection, the wrapper displayed the
game with a spinning cartridge overlay and stopped forwarding keyboard input.
The native/audio processes remained alive, so this is distinct from the previously
fixed audio socket startup race. Restarting only the test wrapper recovered it;
the following ordinary Play run accepted a move and returned normally. Cause is
not established. This interrupt-during-boot path does **not** pass acceptance and
must be investigated before broad launcher reliability is claimed. The existing
game was not overwritten by the rejected request. User files were preserved.

An in-flight read is invalidated when its Activity pauses; delayed-provider timing
was reviewed in code, not exercised with a synthetic slow provider on device.
External SEND is implemented but not tested with a separate sharing app.
Missing-runtime text is implemented; the installed runtime was not removed to
simulate it. Input here uses injected controller actions and native keyboard
equivalents; physical-controller ergonomics and other Android devices remain
separate acceptance gates.

Next: isolate/fix the interrupted-startup recovery, then nested game libraries,
dependency-aware staging and complete first-run runtime setup.
The [full completion path](../../ROADMAP.md#completion-path) remains
the product plan.
