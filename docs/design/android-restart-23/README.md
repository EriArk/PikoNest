# Runtime restart — Android lab 0.0.24

2026-10-03, Retroid Pocket Classic / Android 14. Actual device screenshots:
[PNG game after a repeated launch](runtime.png), [text game](text-runtime.png),
[same text game after Home/resume](resumed.png), and
[restored Play after native shutdown and host process loss](returned.png).
Normal display size, 1240×1080 screenshots; no layout change in this slice.

Installed artifacts:

| App | SHA-256 |
| --- | --- |
| PikoNest host 0.0.24, versionCode 24 | `48c8678e2e431472b0d7019bf1220f64b15786614343a5404a63016fa5c7ea12` |
| PIKOOS Runtime Test, rebuilt upstream 1.6.6 | `6592e6911cc9087475d4efd5dfd4b6614c6164302b0dc34cd0772e24c6f146f4` |

The helper is installed as `art.pikoos.runtimeexperiment` beside the unchanged
`io.wip.pico8`. Host 0.0.24 prefers it. User-supplied official PICO-8 0.2.7 is
prepared through the helper's existing import path; no PICO-8 binary is bundled.
Both wrappers use the existing public data folder, not each other's private
settings. The source wrapper was stopped before the experiment. APKs stay local.

## Reproduced cause

Observed the original wrapper's PulseAudio connection failure again after
Ctrl+Q/relaunch. In the pinned release bootstrap, a background script removes
`tmp/pulse` while the foreground script waits only for that directory and opens
it as FD 7. A stale directory can satisfy that wait and then be unlinked, so the
runtime is bound to a directory different from the new server's socket.

Controlled comparison in the isolated package: a 0.5-second delay before the
original audio script's cleanup reliably gave `Could not setup connection to
PulseAudio` / `FATAL ERROR: Unable to initialize SDL`. With the same delay and
patched scripts, the current socket became ready and the official game ran.
Injected scripts were restored; installed script hashes match the patched build.

The patch cleans before spawning, waits for the socket with liveness and bounded
retry checks, and reaps its owned server on host-script exit. Detailed source,
build inputs, upstream license and rollback are in the
[experiment](../../../experiments/runtime-restart/README.md).

## Verification

- Three consecutive short runtime startup/Ctrl+Q cycles passed the video-connection
  and process checks. Each returned to Play; no `pulseaudio`, `proot` or `pico8_64`
  remained. These include boot time; they are not three completed play sessions.
- Then waited for actual PNG gameplay, changed the board and selection, and
  captured move count 1. Killed the background PikoNest host (PID absent). Held
  Enter opened the native menu; four Down presses and Z selected SHUTDOWN.
  PikoNest recreated and restored the PNG selection; runtime/audio processes exited.
- Launched the text Little Lights cart afterward without force-stop, played a
  move, pressed Home, and resumed the existing runtime task. Same native/audio
  PIDs were retained; move count advanced from 1 to 2 after resume. Ctrl+Q cleaned
  up and returned to Android's launcher in this manually foregrounded task path;
  reopening PikoNest restored Play. Automatic focus return after this path remains
  distinct from the verified direct Play → game → Play path.
- `test-startup.ps1` ran against the closed test package. An `exit 7` audio child
  failed before proot with an explicit diagnostic; a sleeping no-socket child
  hit the bounded timeout and was reaped. Both returned exit code 1. The script
  restores the original pulsar script in `finally` and retains a backup if needed.
- All 15 existing project cartridge hashes and three original Games text hashes
  matched the pre-change baseline. The Games PNG remained
  `11bb25685b0811ae858c9a8f7265c96a83a0f1d253008c0e10d6b485ab8f791e`.
- Full host portable suites, APK build/signing and the separate reproducible
  helper build/signing passed. No core cartridge behavior changed.

The first automation used a fixed three-second launch delay; a later screenshot
showed startup frames rather than the game. Checks were corrected to observe
process/video readiness and separately inspect settled gameplay. A short Enter
event was also missed by native input; held input plus visible menu confirmation
was used for the actual SHUTDOWN check. Do not infer play from intent delivery.

## Remaining gates

This fixes the reproduced audio startup race in the **test adapter**. The original
fallback is still unfixed. It does not certify every wrapper lifecycle path or
provide production runtime setup, launch-result reporting, a complete timeout UI,
external launcher entry, or a final one-APK packaging design. Physical sound
quality and controller ergonomics need listening/owner acceptance; process/audio
initialization and injected keys alone cannot establish those. Storage/runtime
requirements retain the upstream experiment's constraints.

Next: external launcher entry/return through PikoNest; keep the corrected lifecycle
as a regression case. See the [overall plan](../../ROADMAP.md#completion-path).
