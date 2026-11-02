# Earlier alpha scope decision

**Superseded in name later on 2 November 2026:** the owner selected a first early
beta, `0.1.0-beta.1`. See [current scope](BETA_SCOPE.md). This earlier record retains
the runtime archive policy and the distinction between scope and acceptance.

Owner decision, 2 November 2026: target an **alpha**, rather than waiting for a
complete no-Lua creator. This changes the first milestone, not the long-term
product or permission to publish an APK.

## What the alpha promises

- One Android APK with Play, Workshop and setup for the user's purchased runtime.
- Guided runtime import/preparation, optional explicit Test, clear errors and retry.
- Supported visual editing and the two bounded starting games. More ambitious
  rules and genres may require ordinary PICO-8 Lua through the available tools.
- Save/reopen/Test/return/export for the declared supported paths, with source
  preservation and honest explanations of unsupported editing.
- English instructions describing supported devices, runtime archives and known
  limits. No claim that arbitrary cartridges become visually editable.

Manually editing recognized generated Lua may leave the Game view subset; keep
the source and use All tools. The alpha must explain this before users depend on
visual round-tripping. The existing code tool is bounded too; alpha does not mean
an unrestricted desktop IDE has been implemented.

## Required before an alpha can be offered

- [ ] A new user completes setup without developer file copying or shell commands,
  with a valid archive, a wrong archive, cancellation and retry.
- [ ] Games-folder access, optional project/library activation and reconnect behave
  correctly on the supported Android provider; no stale internal copy is silently
  substituted for current external work.
- [ ] Play and a declared supported creation/edit/save/reopen/Test/export journey
  work on the supported physical controls and screens. Return retains context.
- [ ] Updates, interrupted writes and backup/recovery have a verified, documented
  data-preservation path. Backup/restore is now locally implemented in lab 0.0.93; Android acceptance
  remains open. See [the data guide](BACKUP_RESTORE.md).
- [ ] The supported Android/runtime matrix is recorded. The approved interim
  target SDK 28 mode is explicit; modern-target R10 remains required follow-up,
  and alpha status is not evidence of broader Android compatibility.
- [ ] Owner reviews the actual build, accepts its alpha limitations and separately
  authorizes the specific APK publication. This decision is not release approval.

No device checks were performed for this decision. Handheld access remains paused.

## What remains after the alpha milestone

The full [acceptance scenarios](ACCEPTANCE.md) remain open product goals, not a
claim that an alpha must already implement every genre without Lua. Extend shared
rules, collision/world tools, animation/audio, reusable resource types and guidance
in later updates. Keep the tools independent of templates. Complete novice/no-Lua
genre journeys before advertising those capabilities. Linux and online work stay
later. Existing task IDs in [BACKLOG](BACKLOG.md) retain ownership.

## Runtime archive policy

Select the purchased **Raspberry Pi ZIP containing `pico8_64` (ARM64)**. There is
no fixed version-number or outer-filename whitelist. The official publisher lists
separate platform downloads: [PICO-8 downloads](https://lexaloffle.itch.io/pico-8)
and [platform FAQ](https://www.lexaloffle.com/pico-8.php?page=faq), checked 2 November.

Lab 0.0.92 discovers exactly one `pico8_64` and its non-empty sibling `pico8.dat`,
including root-level or renamed/nested folders. A readme is not executable input
and is no longer required. Both installation and isolated probe transport use the
same discovered entries while writing only fixed private destination names.
CRC/size/ELF checks, safe paths, duplicate refusal and cancellation remain in place.
ZIP/entry limits remain 64 MiB, expanded total 128 MiB and 512 entries; these are
PikoNest safeguards, not PICO-8 format limits.

An older ARM32-only archive cannot run in this ARM64 backend. Desktop binaries,
installers, tar archives and standalone exported games are not supported runtime
imports. Ambiguous multiple-runtime ZIPs require a single-version archive.

Structural acceptance is not publisher authentication, version detection or runtime
compatibility. No unknown future version is promised to work: libraries, executable
requirements, command-line options or cartridge APIs may change. Preparation and
official Test are separate evidence. The imported runtime version does not silently
change editor semantics or convert a cartridge to an older format. A failure during
preparation retains the previous installed runtime; restoring an older user archive
is explicit. A full user-facing rollback manager remains unfinished.

Current evidence uses synthetic layout/ABI/error fixtures and the supplied 0.2.7
archives where recorded. It is not a tested old/new runtime version matrix.
