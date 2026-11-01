# Lab 0.0.89 — portable project persistence

Local source/evidence, 1 November 2026. No console access, install, screenshots or
APK release. APK SHA-256: `035682b28620e8b016ad7a2816073285332d08149157e24392b57d538eab57cb`.

All project operations in MainActivity now route through ProjectRepository rather
than using Android filesystem objects. The local ProjectStore implements that
contract while retaining existing project layout, IDs and atomic write mechanism.
The shared process lock protects read/replace/publication between local adapter
instances; it is not an OS-wide compare-and-swap against unrelated processes.
Staged cart/title readback precedes publishing a new project directory.

- Full local core run: 87 suite invocations passed. ProjectRepositoryTest has 40
  assertions covering create/read/save ownership, duplicate/failed creation,
  no-op saves, stale sources, storage-boundary race rejection, failed writes,
  exact import retry and changed metadata, unsafe IDs/listings, readback mismatch,
  and real WorkshopSession Save/Undo/Redo plus failed Undo through the repository.
- Android compilation initially caught a missing adapter method; it was added.
  Android compilation and integrated APK packaging were rerun without repeating
  unaffected core suites. The final package passed signing/package verification.
- Package checks confirm one launcher, private runtime process, expected 263 Godot
  assets, and no purchased PICO-8 binaries in APK/bootstrap.

Portable tests exercise repository/editor behavior with a simulated storage port;
they do not execute Android AtomicFile or SAF providers. New local-adapter behavior
is compiled and reviewed; real device read/write/recovery and owner acceptance
remain pending. Source/runtime/physical acceptance are separate evidence layers.

This batch adds no external activation or new UI. Library stores and pending
import/export journals retain their existing private implementations. Readback
failure reports uncertain stored state; it is not represented as a guaranteed
rollback. Do not retry by silently overwriting a changed source.

Next: SAF project storage satisfying the same publication/conflict contract,
journaled activation, reopening and lost-access recovery, then the library port.
See [storage contract](../../STORAGE_COPY.md). Console use stays paused.
