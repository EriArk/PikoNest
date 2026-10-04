# Interrupted boot and first resume — runtime adapter revision 2

2026-10-03. Retroid Pocket Classic, Android 14, official user-supplied PICO-8 0.2.7.
Host remains 0.0.25 (`d12a6cd`); its APK/code are unchanged in this fix.
Installed runtime adapter: `art.pikoos.runtimeexperiment`, versionCode 2,
versionName `1.6.6-pikoos.2`. APK SHA-256:
`7b0d854adf57b77532b339fd58e5a4454b616c182e7ad0e3992dcc7c68362a04`.

## Reproduction and cause

Start Little Lights from PikoNest Play. Four seconds later, deliver another game
request to LaunchActivity. The host correctly refuses to overwrite the active
snapshot. Dismiss with B and return to the booting runtime.

Before this patch, native PID **15945** was replaced by **16045**; proot changed
from **15942** to **16042**. The frontend showed a spinning cartridge and stopped
responding to native game input. Its FIFO worker and the new native process were
waiting for pipe partners. Audio/native processes remained alive. A separate
three-second interruption reproduced the same stuck connection.

The pinned upstream `_get_target_path()` consumes the initial intent but does not
record it in the two duplicate-request guards. On first resume, the Applinks
observer delivers that same intent again, triggering an unwanted native restart.
The patch records both guards **before** decoding/awaiting/launching the cart.
It prevents the unintended restart; it does not redesign general FIFO restart
or make intentional hot replacement/crash recovery reliable.

## Device results

| Check | Result |
| --- | --- |
| Same four-second boot interruption, final APK | Native PID **17502** before and after; game loads, accepts a move, exits normally to Play. |
| Earlier functional build of the same fix, boot interruption then Home/resume | Native PID **16436** preserved; move count 1 and board retained after resume. |
| Beacon 1.8.10 → PNG game → one move → Home → select game again | Host recovery screen offers explicit resume. A resumes native PID **16820**, with move count 1 intact. Exit returns to Beacon and clears the dispatch journal. |
| Retroid Launcher beta 1.16 → Safely Open → text game | Game accepts a move; exit returns to the same selected Retroid card. |
| Exit cleanup | No pico8_64/proot/PulseAudio processes left; both host launch journals false. |
| Data preservation | All 15 project cart hashes and all 4 Games cart hashes match the previous baseline; editor selection remains remix-0009. |

Actual screenshots:

- [Input after interrupted boot](boot-resumed.png).
- [Return to PikoNest Play](play-return.png).
- [Beacon resume with preserved move](beacon-resumed.png), [return to Beacon](beacon-return.png).
- [Return to Retroid Launcher](retroid-return.png).

Input was injected controller actions for the host/launchers and held keyboard
equivalents for official PICO-8. This does not replace owner physical-controller
acceptance or prove other Android device compatibility.

## Build validation and scope

The update installs with `adb install -r`, preserving the prepared purchased
runtime, settings and folders. The original `io.wip.pico8` app and launcher
profiles were not modified. No PICO-8 binary is included in this APK or repository.

The build pins upstream APK, Apktool and the upstream launch-script hashes.
An initial test build exposed Godot sparse-index truncation when only the script
remap file was replaced. The final build updates the resource directory too and
verifies every one of its **262** indexed asset sizes and MD5 digests against the
actual APK payloads. A local copy with a replaced remap payload is rejected as
`Invalid sparse asset: run_pico_cmd.gd.remap`; that deliberately invalid APK was
never installed. APK signing verification and installed version verification pass.

No portable editor/domain logic changed, so its unchanged suites were not rerun.
The existing extracted audio bootstrap remains unchanged from adapter revision 1.
The new script executes from APK resources and needs no data reset/re-extraction.
See [reproducible build, source pin and rollback](../../../experiments/runtime-restart/README.md).

General runtime exit/crash observation, stale-journal recovery, production
packaging, integrated first-run import, and linked cart/file staging remain open.
The old wrapper fallback has neither this fix nor the earlier audio fix.

Next: nested game libraries, followed by dependency-aware staging and first-run
runtime setup. See the [overall completion path](../../ROADMAP.md#completion-path).
