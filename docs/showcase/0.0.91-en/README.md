# Lab 0.0.91 — external working library

Local implementation, 2 November 2026. No handheld access, installation, screenshots
or public APK release. Owner reaffirmed the device pause. APK SHA-256: `ee64ff07e313b3b2aaae4834af1100ba0ac63f339d75e825fd7f27d8691b57e6`.

[Library contract](../../EXTERNAL_LIBRARY.md). Sprites, parameter presets and the
cross-project buffer use one typed repository, local/SAF storage and independent
checked activation/reconnection controls. Internal originals and versions are kept.

- Full local core run: 89 suite invocations passed. LibraryRepositoryTest: 288
  assertions, including 256 pixel comparisons; create/retry, metadata/category/
  favorites, source conflicts and failed writes, buffer replacement, source-cart
  preservation, historical labels and raw snapshots, independent copied library
  reopen, invalid paths/IDs, lost-access refusal, CAS race and corrupt/truncated
  data. Mixed storage descriptions are checked. These are not 288 native journeys.
- Existing recovery (61), repository (40), setup (40) and transfer (36) suites
  passed, along with prior editor/asset/preset checks.
- Android source compiled before the full suite. Integrated APK compiled, signed
  and passed package checks: one launcher, private runtime process, expected 263
  Godot assets and no purchased PICO-8 binaries in APK/bootstrap.
- A final UI-role status guard was recompiled/repackaged after the full suite;
  unaffected core tests were not repeated. This prevents a completed project
  activation's status from appearing as the current library's status, or vice versa.

New automated tests use a simulated storage port. Android AtomicFile/SAF providers,
actual activation/recreation/recovery, native resource reuse and physical/touch/
visual/novice/owner acceptance are unperformed. Compilation is not those proofs.
No before/after screenshots are claimed. Current `.pksp`/`.pkpr` paths are retained;
external recovery versions use `.bin`, not cartridge `.p8` names.

Full reinstall backup/restore, merge, external-to-external migration, version cleanup
and additional reusable asset types remain unfinished. App-private location/journal/
preference/draft state is still required. Next: bounded complete backup/restore.
