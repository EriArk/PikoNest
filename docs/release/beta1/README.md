# PikoNest early beta 1 candidate

Updated 2 November 2026. **[First owner-approved early beta](https://github.com/EriArk/PikoNest/releases/tag/v0.1.0-beta.1).**
Version `0.1.0-beta.1`, source lab `0.0.93`, Android version code `93`.
[Exact APK and source-bundle hashes](SHA256SUMS).

Play, Workshop, runtime setup and backups are in one APK. Game view connects a
supported subset of artwork, movement, collection/switch rules, animation, audio
and tile-world editing. Blank and two examples share the same tools. More ambitious
gameplay may require Lua; this is not a complete no-code creator.

## Install and start

1. Install the APK from the [GitHub release](https://github.com/EriArk/PikoNest/releases/tag/v0.1.0-beta.1).
   Update existing PikoNest **in place**, without uninstalling. One icon, no helper app.
2. Choose Games in the guide, or skip it. Android's system picker may need touch.
3. Import your purchased **PICO-8 Raspberry Pi ZIP containing `pico8_64` (ARM64)**
   from your Lexaloffle purchase page. Windows/macOS/Linux x86 and ARM32-only
   archives are unsupported. ZIP names, versions and enclosing folders can vary.
4. Prepare, then press **Test**. Successful inspection alone does not prove every
   older/future runtime executes correctly.
5. Keep creation data internal or activate checked project/library copies in selected
   folders. The external Splore folder is not yet an active download-sync destination;
   current downloads live in persistent runtime storage and appear in Play.
6. Open Play to play or Workshop to create. Save a [backup ZIP](../../BACKUP_RESTORE.md)
   outside the app before uninstall/reset. Purchased runtime files and Android
   permissions must be re-established separately after restoration.

## Demonstrated result

- Dependency review complete: 82 native records, 80 official matches, 185 pinned
  materials and notices/configuration/rebuild map. [Evidence](DEPENDENCIES.md).
- 91 core suites, 20 audit tests, final packaging and strict distribution checks.
  Final Android-only view corrections were inspected on-device.
- Retroid update preserved 56 prior project files and four library files byte-for-byte.
  Final main-app backup verified 355 files.
- Independent first-run import/Test, Splore download/favorite/offline title launch,
  blank art/movement/export, both starter wins, SFX/music previews, external VIEW
  intent/resume and clean-project restore demonstrated within recorded bounds.
- Fixed repeated sprite thumbnails, visible archive errors and Splore's old 128-cart cap.

[Actual journeys and build identities](DEVICE_ACCEPTANCE.md) ·
[Seven native English screenshots](../../showcase/beta1-en/README.md).

## Requirements and known limits

| Area | Position |
| --- | --- |
| Android | ARM64, API 26 minimum, interim target 28. Checked on Retroid Pocket Classic / Android 14, not every Android 8+ device. |
| PICO-8 | Purchased separately. Execution checked with 0.2.7 Raspberry Pi ARM64; import does not whitelist this version. |
| Creation | Bounded visual paths. Custom Lua is preserved; advanced logic can require code. |
| Controls/audio | Injected keys/taps and muted runtime previews are not physical ergonomics or audible acceptance. |
| Data | Clean-project SAF restore passed; unfinished-draft, interrupted/provider-failure restore remains open. |
| Splore | Single-cart download/shelf/offline title passed; multicart continuation and external-folder sync remain unfinished/unverified. |
| Language | Main shelves/setup/Game view are English; some legacy tools/help are incompletely translated. |

## Deferred acceptance in the first early beta

The owner explicitly approved this exact APK with the stated limits. These
remaining checks are deliberately deferred; they are not marked passed:

- [ ] Physical controls, audible playback, novice/owner review.
- [ ] Fresh third-party launcher UI and content-URI variants; VIEW/resume already passed.
- [ ] Broader world/tile draft and external library activation/reconnect journeys.
- [ ] Real grant/provider loss, full storage, unfinished-draft restore and interrupted
  rollback; host fault tests remain a separate evidence layer.
- [ ] Wider devices/displays/orientations/runtime versions; multicart offline parts.
- [x] Owner confirmed satisfaction with the installed candidate, accepted the
  early-beta limits above and explicitly approved publication.

The owner accepted this bounded first-beta matrix. Deferred work remains tracked
and must not be called tested; release approval does not close broader product tasks.

## Publication materials

- `PikoNest-0.1.0-beta.1.apk` — exact tested product, 52,106,465 bytes.
- `PikoNest-0.1.0-beta.1-support-sources.zip` — 193,164,522 bytes; corresponding
  materials, recipes, notices and configuration. No purchased runtime.
- `SHA256SUMS` and matching repository source/tag.
- [Prepared release text](RELEASE_NOTES.md), [rebuild map](REBUILDING.md) and
  [build/signing guide](../../../experiments/android-integrated/README.md).

This approval applies only to these exact artifacts. Future code changes require
new builds and affected checks; never substitute another APK under these hashes.
