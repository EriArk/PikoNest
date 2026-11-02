# First early Android beta

Owner decision, 2 November 2026: call the first public milestone an **early beta**,
with candidate version `0.1.0-beta.1`. This supersedes the alpha name chosen earlier
that day. It does not claim a complete no-Lua creator or waive existing acceptance
and publication gates. The owner has now freed the handheld; bounded candidate checks are recorded below.

## Declared scope

- One Android APK: Play, Workshop, runtime setup and backup/recovery.
- User-imported purchased Raspberry Pi ARM64 runtime. Compatible ZIP contents are
  discovered without pinning a version number or filename; future execution is
  checked separately. [Archive rules](ALPHA_SCOPE.md#runtime-archive-policy).
- Play existing games, open official Splore, and find its downloaded cartridges
  on the same Play shelf after returning. Favorites/recents and direct cached-file
  launch use the existing runtime home; no project import is required.
- Create/remix through supported visual tools and two editable starting games.
  More ambitious gameplay may require ordinary Lua; arbitrary imported code does
  not automatically become a visual project. Tools are independent of templates.
- Save/reopen/Test/return/export for supported paths, with originals preserved and
  errors made explicit. Full no-Lua genre journeys remain later product goals.

[Current device evidence and remaining acceptance](release/beta1/DEVICE_ACCEPTANCE.md).

## Required before publication

1. Complete native dependency notices and corresponding-source provenance for the
   exact shipped binaries. This review is now complete for the refreshed candidate,
   documented in [the inventory](release/beta1/DEPENDENCIES.md).
2. Review the remaining [candidate device checklist](release/beta1/README.md): independent
   setup, update without data loss, controller journeys, third-party launcher,
   external storage/reconnect, backup/restore and interrupted-state recovery.
3. Prove Splore download → return → Play entry → direct offline play on the device.
   Multicart parts have a separate first-download/offline check. Local cache fixtures
   do not prove BBS/network availability or actual runtime cache semantics.
4. Record the tested device/runtime matrix and accepted limits. Minimum API 26 and
   target SDK 28 are manifest settings, not universal Android compatibility proof.
5. Owner reviews the actual candidate, is satisfied with the result and explicitly
   approves its publication. No APK release/prerelease is authorized by a local build.

The local code connects Splore's internal persistent cache to Play. Copying/syncing
that cache to an arbitrary selected external Downloads folder remains unfinished;
do not describe that saved folder selection as an active download destination.
The current cache is included in the checked backup ZIP. Newer runtime versions may
change cache layout; unsupported paths must be reported/preserved rather than deleted.

The [full product roadmap](ROADMAP.md) remains broader: richer visual game logic,
audio/resource libraries, modern-target runtime, novice/physical UX, localization,
later Linux and online experiments. Beta naming does not mark those tasks complete.
