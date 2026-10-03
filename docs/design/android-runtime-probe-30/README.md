# Isolated runtime test — host 0.0.30 / adapter 4

2026-10-03. Retroid Pocket Classic, Android 14, user-supplied PICO-8 0.2.7.
Both local development APKs installed in place. No GitHub release was created:
the owner explicitly reserves APK release approval until fully satisfied.

| Artifact | SHA-256 |
| --- | --- |
| Host 0.0.30 | `857540758b7b88e295d5b1f081b70ec4319847a0d71f1f3cb3f42cfe60b83bcd` |
| Runtime Test 1.6.6-pikoos.4 | `1f8aa756db5af11a96013d4b8820d9e3f4b053c7674da52ce82505f6d524ce34` |

## Delivered path

Folders → R PICO-8 → verified purchased ZIP → A → background preparation →
official runtime test cart. X selects another archive, Y revalidates the saved
copy, B cancels host preparation or returns. A/B follows the existing preference.
Once dispatched, A resumes the same test and archive replacement is blocked.

- [Test cart after movement and two button presses](probe.png)
- [Home return offers the same test](resume.png)
- [Official menu has Shutdown](shutdown.png)
- [Normal return to setup](returned.png)
- [Abrupt runtime termination reported with retry](interrupted.png)
- [Existing multicart still runs through Play](play-regression.png)

These are physical-device captures at 1240×1080, without a display override.
The runtime screen is our ordinary `.p8`, not a mock. Native events were injected
held keyboard equivalents; host actions were injected controller events. Physical
controller ergonomics, audio listening and owner product acceptance remain distinct.
The console's media stream was muted at the owner's request and left muted.

## Isolation and runtime evidence

The retained ZIP was the same verified 16,613,018-byte archive from 0.0.29,
SHA-256 `5e64187b50c470b2e345ea0ccccb04a9c098660cc8b9974672f6587741893295`.
The host rechecks that hash, emits a bounded `PIKORUN1` transport and temporarily
grants read access to its fixed private provider URI. The adapter gate/provider
require the same signing identity. An adb shell start of the gate was rejected
by its manifest signature permission. This is not a complete upstream security audit.

The receiving gate validates all three sizes and SHA-256 digests, the ARM64 ELF
header and cart header before dispatch. Fixed output names, a fresh UUID directory
and no archive-controlled extraction paths keep extraction separate from the
working installation. Candidate executable/data are mode 500/400 respectively;
the test cart is 400. No purchased binaries are committed or bundled in either APK.

`/proc/<native PID>/maps` showed the executable mapped from the private
`files/pikoos-probes/<session>/pico8_64`, with the existing shim mapped separately.
The first whole-directory bind broke `../picoshim.so` resolution and left a spinner.
Replacing it with two per-file binds restored graphics and input without changing
the extracted baseline launcher scripts. Isolated `probe-home` contains the
candidate's own configuration/activity/backup/BBS directories.

Home/resume kept native PID 27065, moved position and press count; another O press
worked after return. Start/menu → Shutdown produced EXITED, stopped native PICO-8,
returned to setup and deleted the exact candidate binary/data/cart files.
Lifecycle states deliberately do not say that graphics/audio passed or that the
owner accepted the runtime.

Active lifecycle records include process PID/start time. Android Godot FileAccess
did not read procfs reliably in this experiment; the script obtains that one record
using fixed `/system/bin/cat` argv. The Java provider compares current identity,
including zombie/dead detection. Force-stopping the runtime adapter during a test
removed its native process and showed the interrupted message on the host. Retrying
then ran a fresh candidate. No arbitrary process is killed by this status query.

Completed sessions retain isolated diagnostics and a closed marker. At eight
directories, starting another probe removed the oldest marked completed session
and kept the count at eight; interrupted sessions and their files were retained.
Traversal does not follow symlinks. A cleanup UI for interrupted sessions is still
needed before release. Power-loss, storage exhaustion and hostile-filesystem fault
coverage are not claimed by this device check.

## Preservation and automated checks

All 15 pre-existing project hashes matched their baseline. The working official
runtime files retained these hashes:

```text
ad56e8ed1ad812cab57a7e2679b6731eba367cdd626cbf40a12b514060b597aa  pico8_64
91212d55b540ef2abf9d5df7bb46fb87f41c35f3e9d108ebd8680debab020be2  pico8.dat
```

The shared runtime config stayed
`f9f9722da44cb0f212fc8f38fee64079827a4c51a0cebdebb3d818ac37a951bf`
through the isolated probe tests. Ordinary Play mapped the original working
binary, not a candidate. No uninstall, data reset, folder migration or launcher
profile change occurred.

All portable host suites, Android compilation, APK signature verification and all
262 sparse-index asset checks passed. `RuntimeProbeTest` adds 16 checks: exact
binary/data/cart bytes, source preservation, bad magic/length/hash/payload,
truncation/trailer, occupied target, changed source hash, cancellation and controller
action gating. Fixtures are synthetic, without proprietary runtime data.

## Remaining work

This requires the already initialized separate wrapper/rootfs. Clean-install
environment delivery, permanent activation with rollback, full first-run routing,
folder activation and Splore synchronization remain open. Interrupted-session
cleanup, missing/unreadable lifecycle identity and wrapper-only death with a live
native child need broader recovery handling. A missing identity remains
conservatively pending; it never becomes a fabricated success.

The next slice should make installation activation/recovery transactional, then
remove the need to prepare the separate wrapper manually. Product UI acceptance
and publishing permission must remain explicit owner decisions.
