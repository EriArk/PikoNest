# PikoNest 0.0.71 - bring your runtime data

Six original English captures from Retroid Pocket Classic, Android 14. Local
one-APK development build; owner-approved interim target SDK 28. No APK release.
[Capture provenance](capture.json) | [Runtime boundaries](../../SINGLE_APK.md).

| Image | Demonstrated behavior |
| --- | --- |
| [Data review](01-data-review.png) | 84 source files, current/imported comparison and default keep-current policy |
| [Conflict choice](02-conflict-choice.png) | Browse the four different files; changing the policy stays a review until A. This review was cancelled |
| [Import complete](03-import-complete.png) | A verified merged copy becomes active; source and prior home are preserved |
| [Save after import](04-save-after-import.png) | Official `cartdata`/`dget`/`dset`: launch count 1 before switching homes, 2 afterward |
| [Clean Play](05-clean-play.png) | Hero-free Little Lights launched after preparation in an empty private installation |
| [Clean Workshop Test](06-clean-workshop-test.png) | Moon Garden rendered by official PICO-8, then normal exit returned to the same project |

## Checks and evidence boundaries

The purchased Raspberry Pi ZIP was selected through Android's document picker in
an isolated `art.pikoos.cleanlab` installation. Its initial Play shelf was empty;
no runtime home, rootfs or preferences were transplanted from the main app. Setup
prepared the support environment and official runtime inside that package. The
legacy helper remained disabled. The independent `io.wip.pico8` app remained
installed and running, so this is **not** a factory-clean-device test with all
external wrappers absent. The temporary validation package was removed only after
its Workshop session reported EXITED; normal delivery still has one APK/icon.

The main installation imported 84 files (3.2 MiB): 79 new, one identical and four
different. Keep-current was selected for all conflicts. A later review found zero
new / 80 identical / four different files; conflict browsing and policy changes
were exercised, then cancelled without changing the active home. Imported-wins
copying is covered by portable tests, not by replacing the owner's config on-device.
All 84 original source files and 48 existing project/library files retained their
SHA-256 hashes; the private import snapshot exactly matched the selected source.
The old runtime home remains. The native save-check cart is our ordinary test
fixture, not a custom PikoNest runtime API. The old source had no saved cartdata;
this check proves retention of the current home save across the merge.

Force-stop was a deliberate fault injection into the disposable validation app
only. Relaunch no longer left setup's dispatched flag permanently busy; subsequent
Play and Workshop Test were available. A second disposable Play interruption
returned to the library and allowed another Test. No live game in the main or
independent app was killed for a passing check. ADB D-pad injection did not reliably
navigate the native game menu, so normal Workshop exit was selected by touch.
Physical controller acceptance is still separate. The console remained muted.

The full existing portable suite passed during implementation; focused migration
(27), boot identity (7), recovery (17) and session (16) checks passed after session
changes. Packaging/signature checks verified all 263 Godot indexed assets and no
purchased runtime files in the APK/bootstrap. Captures 03/04 precede final fixes to
conflict browsing, interrupted setup state and the probe footer; their exact APK
hash differs from 01/02/05/06 and is recorded in capture.json. Private backups,
archives and APKs are not published.

Still open: full four-folder onboarding and ownership, Splore indexing, old
frontend-private settings/themes/shaders outside the selected data folder,
rollback/cleanup UI, storage/provider failure cases, native orphan/boot matrix,
modern Android target, multiple devices, physical controls and owner acceptance.
R09/R03/R05 and issues #9/#10 remain partial. Next implementation work connects
numeric rules to resource coordinates for the first original game from blank.
