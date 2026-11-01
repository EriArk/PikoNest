# Lab 0.0.90 — external working projects

Local implementation, 1 November 2026. No console access, installation, screenshots
or public APK release. APK SHA-256: `4be20a47b755e31becdf746f53b30db757c861b60f61cb8f9a677807dd36e04c`.

[Contract and user path](../../EXTERNAL_PROJECTS.md). Projects now have SAF-backed
create/import/read/save, fresh-workspace activation and reconnect/older-internal
workspace controls. The library remains private. Existing internal data is retained.

- Full local core suite: 88 invocations passed. RecoverableReplaceTest adds 61
  assertions: provider failures before/after create, journal, both renames and
  clear; whole old/new recovery; idempotence; retained originals; partial candidates;
  stale source; unknown external changes; missing backup; unsafe journals; real
  WorkshopSession Save/Undo/Redo and ordinary-cart reopen through repository plus
  simulated provider. First-run split-storage wording is checked too.
- Existing setup, repository and transfer suites pass (40 / 40 / 36 assertions).
- Android source compilation, signing and integrated package verification are
  recorded for the final APK: one launcher, private runtime process, expected 263
  Godot assets and no purchased PICO-8 binaries in APK/bootstrap.
- Initial packaging was stopped after a prolonged quiet phase; final platform
  compilation/packaging ran with private diagnostic tracing and skipped the already
  passed core suites. Location-record durability and activation guards are included
  in that final platform build; unrelated core checks were not repeated.

The provider test model implements named-file operations and injected failures;
it does not execute Android DocumentsProvider calls. Activation/location-record/lifecycle,
rename behavior, permissions/power loss, native create/Undo/reopen/Test/export,
controller/touch/visual and novice/owner acceptance remain unperformed. Interface
compilation and portable tests are not those evidence layers. No beta acceptance.

Saved carts remain ordinary `game.p8`; old `.p8` versions are retained beside them.
Recovery journals/selected location are app-private. No full reinstall recovery,
external-to-external migration, newer-external-to-internal merge or version cleanup.
Providers must support the checked rename workflow; flags alone do not authorize
activation. A copy receipt describes migration time, not future edits.

Next: library persistence/activation; full backup/restore/Splore ownership. Native
journeys wait for owner authorization. ACC-06 and the release gate remain open.
