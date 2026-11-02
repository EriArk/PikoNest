# PikoNest early beta 1 preparation

Prepared 2 November 2026. **Local candidate, not a published release.**
Candidate version: `0.1.0-beta.1`, source lab `0.0.93`, Android version code `93`.
The local build passed all 91 core suites and package verification.
[Exact candidate hash and open gates](../../showcase/0.0.93-en/candidate.json).
The APK remains outside Git. No release tag, download or publication is implied.

Dependency follow-up: 60 of 79 native files now match official artifacts, with 155
source inputs and two signed-catalog files collected. The existing candidate is unchanged;
additional notices and native drift checks enter its next packaging pass. Seventeen
Termux files and the remaining distribution review still block completion. See
[dependency evidence](DEPENDENCIES.md); the local source-material ZIP is partial.

## What this beta is for

Play PICO-8 cartridges, remix supported resources, and make small games with a
mixture of visual tools and ordinary Lua. Play, Workshop and purchased-runtime
setup are in one APK. The bounded Game view can connect artwork, movement,
collection/switch rules, animation, sound, music and a tile world. Its two starting
games are editable examples using the same tools available in Blank.

This is an early workshop, not a complete no-code creator for every genre.
Unknown Lua is retained; visual actions may require recognized code. All tools
remains available, with explicit limits rather than guessed rewrites. More complete
no-Lua creation, advanced audio/library work and modern Android targeting follow.

## Installation and first opening

1. Use the eventual owner-approved APK from this repository's Releases page.
   **None has been published yet.** Do not uninstall an existing PikoNest to update.
2. Install one APK. It supplies one PikoNest icon; there is no helper to install.
3. In the guide, select an existing Games folder, or skip it for now. Android's
   folder/document picker belongs to the system and may need touch on some devices.
4. Download your purchased **PICO-8 Raspberry Pi ZIP containing `pico8_64`
   (ARM64)** from your Lexaloffle purchase page. Select the ZIP in PikoNest.
   Desktop Windows/macOS/Linux x86 archives and ARM32-only archives will not work.
5. Preparation inspects and imports the files. ZIP filename, version number and
   enclosing folder can vary. Use **Test** explicitly to check this runtime; a
   successful import alone does not prove that an older or future release runs.
6. Keep projects and assets inside PikoNest, or open creation folders to activate
   separate external workspaces through a verified copy. Games browsing, Projects,
   reusable assets and runtime saves/downloads have distinct storage roles.
7. Enter Play for games, or Workshop to create/import. Save a
   [backup ZIP](../../BACKUP_RESTORE.md) outside the app before an uninstall/reset.

## Requirements and known limits

| Area | Early beta position |
| --- | --- |
| Android | Minimum API 26 / Android 8; ARM64 runtime backend. This is a package requirement, not proof of support on every Android 8+ device |
| Compatibility mode | Target SDK 28, explicitly approved for this first integration; modern-target execution remains unfinished |
| Device evidence | Retroid Pocket Classic is the reference device; lab 0.0.77 is the last complete native creation journey. The early beta candidate has not been installed/tested yet |
| PICO-8 | Purchased separately. Supplied 0.2.7 Raspberry Pi archive has structural checks; no universal old/new runtime matrix |
| Controls | Controller-first plus touch. Physical inputs, system pickers, rotation and narrow/wide layouts require candidate acceptance |
| Creation | Bounded visual workflows; advanced/custom logic can require Lua and external editing. Unsupported editing must preserve source |
| Language | Main shelves, setup and Game view are English. Some legacy tools/help remain incompletely translated |
| Data | Backup/restore implemented and host-tested; Android document-provider, interrupted restore and update acceptance still open |
| Downloads | Splore cache is connected to Play locally; real download/return/offline acceptance remains open. Arbitrary external Downloads-folder synchronization remains unfinished |
| Distribution | [Native dependency notices and matching source provenance remain a publication blocker](DEPENDENCIES.md) |

## Candidate acceptance, in order

Device work starts only after the owner frees the handheld. Keep system audio muted
unless the owner allows an audio check. Preserve current data and record all evidence
against the APK hash, Android version, runtime version and actual controller.

- [ ] Complete the dependency/source package in [DEPENDENCIES](DEPENDENCIES.md).
- [ ] Update the installed app with the same certificate; verify projects, drafts,
  library favorites, saves and runtime configuration. Do not clear/uninstall it.
- [ ] Test independent first-run setup on an empty authorized validation install:
  skip/cancel/reopen, wrong desktop archive, valid ARM64 archive, preparation error
  and retry, explicit Test and return, Games access and lost-grant reconnect.
- [ ] Splore: download a previously uncached cart, return and see it on Play, favorite, restart without network and launch directly; test multicart parts separately.
- [ ] Play an existing cart, return to the shelf, and launch it from a third-party
  launcher without importing it into Workshop. Check suspend/resume and exit.
- [ ] Blank and both starters: edit sprite/tile, behavior and world; animation,
  SFX and music drafts; Test/return, Cancel/Keep, Save/Undo/reopen and `.p8` export.
  Run the exported cart in official PICO-8 independently. Separate silent logic
  checks from actual audible acceptance.
- [ ] Projects and library: activate checked external copies, edit/reopen/reuse,
  revoke/reconnect access, cancel a copy and resume. Verify originals retained.
- [ ] Backup with a real unfinished draft and save. Restore into isolated validation
  data, complete both reopen steps, compare files/draft candidate, then test process
  interruption and rollback. Verify external originals stay unchanged. Only then
  consider restoration of valuable owner data, with a separate retained copy.
- [ ] Check physical D-pad/sticks/buttons, A/B convention, Android picker boundaries,
  compact/wide layouts, backgrounding, storage-full and clear error/retry paths.
- [ ] Review current English screens with the owner, resolve blocking findings and
  replace host previews with native screenshots where useful.
- [ ] Owner accepts these beta limits, is satisfied with the actual candidate and
  explicitly approves publishing that APK. Only then create a GitHub prerelease.

## Prepared materials

- [Local evidence and fresh image pack](../../showcase/0.0.93-en/README.md)
- [Draft release text](RELEASE_NOTES.md)
- [Backup and recovery guide](../../BACKUP_RESTORE.md)
- [Candidate build/signing instructions](../../../experiments/android-integrated/README.md#local-beta-candidate)
- [Native component inventory](DEPENDENCIES.md)

If code changes after acceptance, rebuild and repeat the affected journeys. Never
attach a newer, unreviewed APK under the hash of an accepted candidate.
