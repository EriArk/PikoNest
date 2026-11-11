# Beta 2: bounded device evidence

Recorded 11 November 2026 after explicit owner authorization to use the Retroid,
finish the work and release this beta. APK SHA-256:
`321a9342d6e564b1e38f7639b5efefd0ba7b17a54f7571fbd7f18b399501c3a5`.
Version code 96; 52,114,641 bytes; same package/signing identity as beta 1.

Retroid Pocket Classic, Android 14, ARM64, 1080 × 1240 capture orientation.
User-supplied official PICO-8 0.2.7 Raspberry Pi ARM64 runtime. All input was
ADB-injected taps, swipes and keys, including held direction/X keys. Audio remained
muted. These checks are not physical-controller, audible or novice acceptance.

| Journey | Observed result |
| --- | --- |
| Preserved update | UI-created pre-update backup verified 355 files. Installed beta 2 with replacement, without uninstall/clear. New verified backup also had 355 files. No old entry missing; all 58 project files and five library files matched SHA-256. Preferences/runtime changed through navigation and probe execution; those categories are not claimed byte-identical. |
| Blank creation | Created Blank project 17 through native UI: painted three 8×8 images, placed a D-pad mover at (8,8), a plate at (24,8), and a touch-win goal at (72,8). Chose the mover as the plate activator. Painted 16 green wall cells in column 4, reviewed their wall rule and linked cell (4,1) to the plate. No Lua editing/import was used for authoring. |
| Draft Test and return | Tested before saving world changes. Official PICO-8 showed plate activation, crossing and win; exit returned to the same world draft. Review showed 16 map cells, one tile, one wall type and one passage. |
| Save / Undo / Redo | Saved world changes; Undo visibly removed the wall/map batch; Redo restored it. The final saved project survived app force-stop/reopen and ran again. |
| Export and independent play | Android document picker exported 9,406 bytes; the app reported readback equality with the saved project. Copied that exact file into the configured Games tree and launched through Android VIEW, without editor import. Reached win and used held X to restart to the original layout; exit returned successfully. This is an injected VIEW intent, not a third-party launcher UI test. |
| Official semantic probe | Generated-code diagnostic cart passed 13 assertions in official PICO-8 on the final APK: initial map, exact overlap edge, opening, held/O behavior, leaving, occupied-cell delay, close after crossing, blocked return, win and restart. Its callbacks are renamed for a small assertion harness; this is separate from the unmodified UI-authored export. |

The verified post-update backup was completed before new scenario creation so its
project/library comparison has an unchanged baseline. An earlier post-update
attempt was cancelled before completion and is not counted as a verified backup.
Private backups are retained locally and excluded from Git/releases. No restore
was performed over owner data.

UI-authored export: [native-blank-plate.p8](native-blank-plate.p8), SHA-256
`7981969ba7d9ee390a0b0e16d3bd1a1a60154e769839e5c81ac9b8226a692659`.
Probe: [plate-probe.p8](plate-probe.p8); original generated input [plate.p8](plate.p8).
[Unmodified native captures and provenance](../../showcase/beta2-en/README.md).

## Owner confirmation — 11 November 2026

After publication, the owner confirmed that physical buttons and audible sound
work on the Retroid. Record these as owner-confirmed for this device and beta.
The automated checks above still used injected input and muted audio; this later
confirmation is a separate source of evidence.

## Limits carried forward

- Novice usability and a broader ergonomics/screen review remain open. Basic
  physical-button operation and audible sound are owner-confirmed above.
- Other hardware, Android/runtime versions, orientation changes and maximum
  world/object/token/CPU loads need broader validation.
- Beta 1 setup/Splore/restore/audio evidence remains historical; the entire matrix
  was not repeated for beta 2. The runtime/native payload is unchanged.
- Real provider/grant loss, full storage, interrupted or unfinished-draft restore,
  fresh launcher UI/content-URI cases and multicart continuation remain open.
- Native creation covers one plate/one passage. Multiple movers, inverted links,
  initial overlaps and audio/animation bindings have focused host coverage; do not
  describe every variant as device-tested.

Release authorization covers this beta, not future releases or completion of
these deferred acceptance items. [Previous evidence](../beta1/DEVICE_ACCEPTANCE.md).
