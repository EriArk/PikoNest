# External working library

Lab 0.0.91, 2 November 2026. Implemented and checked locally; native acceptance is
pending. The owner reaffirmed that the handheld must not be used.

## User path and data

Choose a writable Asset library folder in Folders. Press Start / F5 on its row,
or tap the left half of the bottom hint. Use selected folder creates a fresh
`PikoNest-library-<UUID>` workspace, copies the internal library and buffer, checks
raw hashes plus typed IDs/metadata, exercises saves/renames on the candidate, then
commits the location. The Projects location is independent. Cancel/failure before
selection preserves the current location and candidate files. Failed selection
acknowledgement reports uncertainty rather than promising an unchanged selection.

Currently supported reusable data: sprites (`asset-library/sprites/*.pksp`), tool
parameter presets (`asset-library/parameters/*.pkpr`) and `sprite-buffer.pksp`.
This adds no new music/SFX/animation/particle library type. Unknown unrelated files
in the copied tree are preserved; unsupported/corrupt known records block activation.
An empty library can be activated and populated afterward.

All resource operations use one typed repository over location-neutral storage:
create/read/list/update, sprite naming/categories/favorites, preset metadata and
the persistent cross-project sprite buffer. The copy snapshot preserves original
encoded bytes, including historical preset field labels. Only explicit edits may
rewrite metadata; matching historical labels do not cause a false conflict.
Sprite pixels, source hash and provenance remain intact when metadata changes.
Insertion still materializes ordinary PICO-8 data/settings in a cartridge; normal
games do not depend on this external library at runtime.

## Safe save, enumeration and activation

The local adapter retains existing paths and AtomicFile writes, compares expected
bytes at the write boundary, and gates library mutations while activation copies
data. The SAF adapter stages first creation, verifies bytes, then renames to the
resource name. Updates reuse the retained-version/journal protocol from external
projects. Resource versions are binary `.bin` files; they are not labelled `.p8`.
Old versions and incomplete candidates are retained without automatic cleanup.

Pending save journals are recovered before listing resources as well as before a
direct read. This matters when interruption left the canonical name temporarily
absent: the resource must not disappear from a successful-looking empty library.
Complete old/new versions can be recovered; unknown changed data or missing backups
block rather than overwrite. Read errors are not converted into an empty library.
Source comparison is repeated in the adapter after the repository's initial read.
Readback failures report uncertain stored state; they do not imply guaranteed rollback.

Activation shares the existing project storage controls, with separate role/location
records and data-specific text. Only one activation runs at once; its relevant local
store is gated. The candidate is fully rechecked after provider exercises. Returning
to the workshop recreates repositories after either location changes. Startup load
errors offer separate project and library recovery entries. The first-run guide
explains all combinations of internal/external project and library locations.

## Recovery and remaining limits

Reconnect requires the SAME original Android tree. A different tree is rejected.
Use older internal originals explicitly selects the retained internal library;
newer external changes remain separate and are not merged. External-to-external
migration and automatic external-to-internal copy/merge are not implemented.
The old copy-only path refuses to represent stale internal data as a backup of the
newer external library. Failed/conflicting saves may require reopening the library
or inspecting retained versions. Providers must support the checked rename workflow.

Keep the app installed: selected locations, preferences, unfinished editor drafts,
runtime/save data and interrupted-write journals still include app-private state.
This is not a complete reinstall backup/restore. Splore ownership/indexing and
full backup/restore remain required. No proprietary runtime binaries are copied
into the library or packaged in the APK.

## Evidence limits

[Local evidence](showcase/0.0.91-en/README.md): portable repository tests, existing
recovery/transfer/editor suites and single-APK compilation/package checks. The new
tests use a simulated file port; they do not execute Android AtomicFile or SAF.
Real provider grants, process death/power loss, native activation/reopen/reuse,
touch/controller/visual and novice/owner acceptance are unperformed. No handheld
use, installation, captures or public APK release occurred in this batch.
