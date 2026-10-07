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
Its home/logs are inside `files/runtime-data`, separate from the old wrapper's
shared directory. Broad external-storage permissions are removed; the workshop
keeps its existing SAF grants. This does not yet migrate shared wrapper saves,
Splore downloads or every upstream setting into the new home. Preserve old data
and independently installed `io.wip.pico8` until that user-data migration is proven.

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

Core recovery tests cover valid/invalid/incomplete censuses, duplicate PIDs,
orphaned children and unrelated applications. Full crash/boot recovery, physical
controller acceptance and durable save migration still require explicit evidence.

Build instructions: [integrated Android package](../experiments/android-integrated/README.md).
