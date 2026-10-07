# One Android application — R09

Owner decision, 2026-10-07: one APK and one PikoNest launcher icon. The first
integrated development build may retain the runtime's target SDK 28; moving to a
modern target remains mandatory work, not a completed compatibility claim.

## Bounded implementation decision

Retain the Java/Canvas workshop and portable core. Package the pinned upstream
Godot runtime frontend, native support and our adapter in the same Android app,
with Godot in a private `:runtime` process. Its exit must not terminate the editor.
The existing `art.pikoos.runtimelab` package/signing identity remains stable so an
update preserves projects and library. This is packaging, not a framework rewrite
or the separate technical naming migration.

The main icon opens Play. Godot and adapter activities/providers are internal;
there is no second launcher icon or public shortcut around the host launch checks.
The runtime port selects the internal adapter in an integrated build. Legacy
session records still query their original adapter until resolved. The signature
permission retained in the manifest is for that lab migration, not for new games.

The current proot bootstrap executes support files from app-private storage.
[Android's target-29 execution restriction](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission)
prevents treating a target SDK change as a manifest-only upgrade. The owner chose
an interim target-28 APK for device integration; this is not Play Store readiness
or proof of support for all newer Android devices. A modern execution design must
be researched and tested before raising the target. Do not bundle purchased
binaries or silently weaken this requirement to finish that work.

## Installation and data ownership

The existing archive picker verifies the user's Raspberry Pi ZIP/ARM64 binary.
Preparation expands only the pinned open-source support payload into a new
app-private candidate. From the user ZIP it copies only the two known runtime
files; archive-controlled paths are never extracted. A ready candidate is
activated by an atomic symlink replacement under the archive job's cancel/commit
lock. Previous candidates are retained; a bounded quota refuses more attempts
instead of deleting unknown or interrupted installations. Cleanup and a complete
version rollback UI remain unfinished.

Godot skips the upstream installer and runs only after our readiness marker.
The original home is `files/runtime-data`. Lab 0.0.71 adds verified migration into
`files/rh/<key>`; an atomic `runtime-home.txt` pointer selects a ready home. A
malformed/unready pointer refuses launch rather than silently opening an empty
home. The legacy path remains the default only before a pointer exists. Broad
external-storage permissions are removed; the workshop keeps its SAF grants.

### Data-folder import, 0.0.71

Folders -> R PICO-8 -> Select Data selects the old PICO-8 **data folder** through
SAF, with read permission only. A private snapshot is compared with the active
home. Up/down browses different filenames; left/right selects one conflict policy
for all differences: keep current (default) or use imported. A applies, B cancels.
Projects and the asset library are independent and are not relocated by this step.

The worker rechecks the SAF source before/after copying, preserves the complete
current home, merges selected files under `data/`, checks every file hash and
rechecks the current home before activation. Fsync plus AtomicFile protects the
active pointer; cancellation and activation share a commit lock. Originals and
old homes are retained. A partial read/copy never activates the candidate. Source
changes require another review. Process loss before activation keeps the old home;
the incomplete copy remains for later storage review, not automatic deletion.

The importer refuses unsafe/ambiguous paths, repeated document IDs, local symlinks
and file/folder collisions. Its bounded limits are 512 MiB / 20,000 files / depth
32, plus 16 staged imports and 16 homes. These are PikoNest limits, not PICO-8
limits. Provider reads run off the UI thread but have no absolute timeout yet.
Rollback, cleanup and per-file conflict choice are unfinished. Native disk-full,
power-loss and provider-revocation coverage remain open.

Retroid migration copied 84 source files; both the source and 48 existing
project/library files retained their SHA-256 hashes. The snapshot matched exactly.
An ordinary official `cartdata` save survived the home switch. This covers the
selected folder, including native config/cdata/cstore/carts/BBS cache files when
present; old frontend-private preferences, themes or shaders outside it are not
imported. Keep old apps/data until those separate boundaries are resolved. This
does not activate the four user-folder ownership model or Splore shelf indexing.
The independent `io.wip.pico8` app remains untouched.

No official PICO-8 executable/data enters the APK or Git. Build verification
checks the APK and support tar, all Godot sparse-index sizes/hashes, and keeps
upstream code/resource provenance explicit. A complete redistribution notice/
corresponding-source audit remains issue #15 and a release gate.

## Session recovery

On Retroid, the host's expected session token differed from the adapter's latest
EXITED record. The old single-record observer therefore correctly returned
UNKNOWN, but offered no recovery path. The event that originally replaced the
record is not established by this observation.

The new explicit recovery action preserves the original journal and writes an
INTERRUPTED record for the expected token only after observing an idle runtime:
no open Godot activity, no RUNNING/PREPARING journal, and a complete numeric process
census containing no other process owned by the adapter UID except the observer,
caller and census tool. An orphaned/suspended runtime child, malformed census or
unavailable evidence refuses recovery. It does not kill processes or label the
game successful. The old signed adapter revision 9 provides this operation during
lab migration; ordinary new installations do not need that APK.

Lab 0.0.71 stamps each starting token with the kernel boot UUID separately from
the four-line journal. Missing/mismatched boot evidence returns UNKNOWN, preventing
a reused PID/start tick on another boot from certifying a live session. After the
parent disappears, an open Godot activity or a non-idle/incomplete own-UID census
also returns UNKNOWN; an orphan or suspended child must not be called ended. The
census has bounded output/time and drains its pipe concurrently. Its scanner is
the only process it terminates. Explicit idle recovery stays available.

Core checks cover valid/invalid/incomplete censuses, orphaned children, duplicate
PIDs, unrelated applications and missing/changed boot stamps. On the disposable
clean installation, force-stop/relaunch allowed retry and exposed a stale runtime
setup `dispatched` preference. The host now clears it only after a confirmed-ended
session; an UNKNOWN/live session still blocks new launch. No native device reboot
or orphan fault was performed. Full crash/boot and physical-control acceptance
remain open; game process exit is never called verified game success.

## Isolated clean installation, 0.0.71

The optional validation build changes only the host namespace to
`art.pikoos.cleanlab`; it starts with empty private storage. On Retroid it selected
the purchased archive in the UI, prepared its own support environment/runtime,
then ran Play and Workshop Test with normal return. No prepared rootfs, runtime
home or prefs were copied from the main installation. The old helper was disabled,
but the independent wrapper remained installed/running; absence of all external
packages on a factory-clean device is still an acceptance gate. The validation
package was removed after its final session exited. It is a test tool, not a
second delivered APK. [Captures and artifact hashes](showcase/0.0.71-en/README.md).

Build instructions: [integrated Android package](../experiments/android-integrated/README.md).
