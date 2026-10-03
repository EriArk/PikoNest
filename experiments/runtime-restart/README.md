# Runtime startup, resume and exit experiment

2026-10-03. Bounded bootstrap fix for the upstream Android wrapper
([Macs75/pico8-android](https://github.com/Macs75/pico8-android), release 1.6.6).
This remains a separate Android research adapter, not a production runtime
architecture or a new framework choice for PIKOOS.

## Controller game menu (adapter revision 6)

`1.6.6-pikoos.6` / host 0.0.37 adds `RuntimeControls` in classes2.dex. The existing
provider registers public Activity lifecycle callbacks; only the pinned Godot
Activity receives a delegating `Window.Callback`. Host dispatch explicitly opts
into the menu and supplies its A/B preference. Unhandled gameplay input and other
window callbacks retain the upstream route. Direct wrapper setup/probe are unchanged.

Select/Android Back opens a native Canvas menu with the host's Tiny5 font/OFL and
PICO-8 palette. The portable `RuntimeMenu` owns safe selection and exit intent.
Native transport sends ordinary Ctrl+Q through Godot after the dialog releases
focus. It never kills processes or force-finishes the Activity. A four-second
unconfirmed exit offers Continue/retry; backgrounding clears delayed work and
held menu keys. The existing process monitor still owns final cleanup and return.
This does not resolve arbitrary crash/session recovery or prove game completion.

Native font/license assets bypass Godot's resource loader. Upstream classes.dex,
Godot payloads and the 262-entry sparse resource index retain revision 5's contents.
Only our Java adapter and native assets change. [Checks and limits](../../docs/design/android-exit-37/README.md).

## Bounded draft diagnostics (adapter revision 5)

`1.6.6-pikoos.5` / host 0.0.36 adds a signature-protected `DiagnosticActivity`
in classes2.dex. It accepts only the host's fixed read-only diagnostic URI, copies
at most 2 MiB, and runs the already installed user-supplied ARM64 PICO-8 with `-x`.
The upstream classes.dex and Godot launch payloads remain intact.

The separate UUID session has private home/work/tmp paths and dummy SDL drivers.
Android timeout owns the four-second process group, with a one-second kill grace;
the Activity has a seven-second wait bound. Output is drained with a 32 KiB cap.
B/Back cancels the result (finishing may take the remaining timeout). Token and
SHA-256 bind the response to the host request and exact draft snapshot. Results
are textual observations, not a compiler protocol or whole-game validation.

Only this session tree is removed after process closure; interrupted directories
are retained and the eight-directory quota stops further trials. Cleanup UI is
pending. This is not a general sandbox. It requires an already prepared wrapper;
clean-device runtime activation remains R01/R02. No proprietary binary is bundled.
[Evidence, official manual and limits](../../docs/design/android-diagnostics-36/README.md).

## Isolated purchased-runtime probe (adapter revision 4)

`1.6.6-pikoos.4` / host 0.0.30 adds a signature-protected Java import Activity
and lifecycle provider in `classes2.dex`; upstream `classes.dex` stays intact.
Only the host's fixed read-only probe URI is accepted. `PIKORUN1` contains three
fixed outputs: ARM64 `pico8_64`, `pico8.dat`, and our ordinary `probe.p8`, with
bounded lengths and SHA-256 digests. It is internal transport, not a project format.
The portable writer revalidates the saved archive hash before preparing it.

Each candidate gets a fresh private UUID directory. Two individual file binds
overlay executable/data for this launch; a separate parent-directory bind supplies
the cart and isolated `-home`/`-desktop`. Binding the whole executable directory
breaks upstream's relative `../picoshim.so` preload under proot: keep its original
working directory and bind the two files. Existing runtime/configuration stay intact.

The lifecycle journal records PREPARING/READY/RUNNING/EXITED/FAILED. Active states
include the responsible process PID/start time to reject stale process identities.
Monitored exit discards exact binary/cart files and marks diagnostics closed.
At the eight-session limit, the gate prunes oldest closed app-owned UUID sessions
without following symlinks; interrupted sessions are preserved and can exhaust
the quota. A user-facing interrupted-session cleanup flow remains pending.
Exit is not proof of graphics, audio or user acceptance.

This probe requires an already initialized wrapper/rootfs. It neither replaces
the working installation nor proves a clean-device onboarding path. The permission
boundary is specific to executable import, not a full audit/sandbox of the upstream
wrapper. [Evidence and remaining gates](../../docs/design/android-runtime-probe-30/README.md).

## Read-only runtime file sets (adapter revision 3)

`1.6.6-pikoos.3` (versionCode 3), paired with host 0.0.28, adds an internal
bounded file-set transport. `PatchLaunch.ps1` appends `file-set.gd` to the pinned
launch script and routes `.pikoset` inputs through complete validation, private
extraction and `-root_path /home/custom_mount`. The existing parent-directory
bind supplies ordinary sibling `.p8` files to official PICO-8. The user's global
custom-root setting is unchanged. Revisions 1 and 2 remain included.

No custom Lua or PICO-8 binary patch is involved. Files use mode 444 and the
session directory 555; chmod uses a fixed Android executable and argument array.
Normal monitored exit removes only the exact recorded session files. Interrupted
or unexpected sessions remain for diagnostics, with a 32-directory quota; recovery
UI is not implemented. Persistent runtime home remains the existing upstream
home, but durable `save`/`cstore` file ownership is not implemented and detected
calls are refused by the host. This is not a sandbox for arbitrary Lua.

Envelope: `PIKOSET1`, big-endian 32-bit entry-name length + ASCII name, file count,
then name length/name/data length/raw bytes for each cart. Reject invalid names,
case collisions, missing entry, truncated or trailing data, >32 files, >2 MiB per
file and >8 MiB aggregate before extraction. It is an internal adapter transport,
not an editable cartridge format. Error confirmation/cancel returns to the caller.

Install with `adb install -r`; revision 3 changes APK assets, leaving previously
imported official runtime/settings and extracted audio scripts intact. Host file
sets require this adapter revision; old fallback remains usable for single carts.
[Contract](../../docs/DEPENDENT_CARTRIDGES.md) ·
[Device evidence](../../docs/design/android-multicart-28/README.md).

## Resume fix (adapter revision 2)

`1.6.6-pikoos.2` (versionCode 2) also fixes replay of the cold launch request on
the first Activity resume. Upstream `_get_target_path()` consumes `get_data()`
without updating either `Applinks.last_data` or `last_received_data`. The resume
observer subsequently treats the same URI as a new request and starts a restart.
On the device, an interruption during boot changed the native PID and left the
video/input FIFO connection stuck. This was not an audio-server crash.

`PatchLaunch.ps1` sets both consumed-request markers before decode/await/launch.
The source is pinned to upstream 1.6.6, commit
`562662e35727ae7706fe97fed3390db249a35263`; revision 2 added only those two assignments
and their comments. The APK maps the existing script resource to this patched
plain GDScript, retaining its upstream MIT notice as an asset. No PICO-8 code or
binary is modified. Later distinct app links keep their original handling.

Godot 4.6's sparse asset index bounds reads by recorded sizes even for loose APK
assets; replacing files alone is insufficient. `PatchSparsePack.java` refreshes
the bootstrap/remap size and MD5 entries and adds the script/license entries,
preserving the other 258 entries byte-for-byte. `VerifySparsePack.java` verifies
all 262 indexed payloads against the final APK. This follows the
[Godot sparse PCK reader](https://github.com/godotengine/godot/blob/4.6/core/io/file_access_pack.cpp).

Device evidence includes unchanged PID after interrupted boot, working input,
retained move count after Home/resume, and Beacon/Retroid launch/return:
[resume checks](../../docs/design/android-resume-25/README.md).

## Audio cause and change (revision 1, retained)

The release's `start_pico_proot.sh` starts `pulsar.sh` in the background, then
waits for the **directory** `tmp/pulse` before opening it as FD 7. On a warm start,
that directory can still be left from the previous run. Meanwhile `pulsar.sh`
kills PulseAudio and removes the directory. The host can therefore bind an old,
unlinked directory into proot while the new server creates its socket elsewhere.
PICO-8 then fails SDL audio initialization. Waiting only for a directory also
does not prove that the server has created its socket.

`audio-session.sh`, inserted into the host script, performs cleanup synchronously
before launching PulseAudio. It waits for the current Unix socket, checks child
liveness, and bounds the wait to 250 × 20 ms sleeps (plus scheduling overhead).
Failure exits before proot starts. The host traps exit/signals to terminate and
reap its own audio child. `pulsar.sh` no longer removes the directory and uses
`exec` so the recorded child PID is the server, not a shell parent.

This readiness gate follows the existing wrapper's local Unix-socket transport;
it does not invent a PICO-8 API. [PulseAudio server strings](https://www.freedesktop.org/wiki/Software/PulseAudio/Documentation/User/ServerStrings/)
document that transport. Sound output quality still needs physical listening.

## Reproducible local build

Inputs, downloaded separately into ignored `.local/`:

- [Upstream 1.6.6 APK](https://github.com/Macs75/pico8-android/releases/tag/1.6.6),
  SHA-256 `916330f7f4a8349c3630d207be73b402f3161a2717a8d1884228ca2c0f94da2b`.
- [Apktool 3.0.3](https://github.com/iBotPeaches/Apktool/releases/tag/v3.0.3),
  SHA-256 `dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423`.
  Resource decode/rebuild uses its CLI; dex and native libraries are retained.
- [Pinned run_pico_cmd.gd](https://raw.githubusercontent.com/Macs75/pico8-android/562662e35727ae7706fe97fed3390db249a35263/frontend/run_pico_cmd.gd),
  SHA-256 `71ef40406dc66f8f590542cdbbc37263734141ae337ae9b3d2297ec5f42cd2cc`.
  Save unchanged as `.local/downloads/run_pico_cmd-1.6.6.gd` or pass `-RunCmdSource`.
- JDK (tested Android Studio JBR), Android build-tools 36.0.0, local debug key
  generated by the android-host build.

Run `./experiments/runtime-restart/build.ps1`. It verifies the three input hashes,
uses a fresh work directory, patches two bootstrap tar members and the cold-intent/
file-set script/resource index, assigns
`art.pikoos.runtimeexperiment` and distinct provider authorities, labels the app
**PIKOOS Runtime Test**, then builds/signs/verifies the APK and indexed assets. Output:
`.local/artifacts/pikoos-runtime-test.apk`. The local test APK is debuggable.
`PatchBootstrap.java` rejects unexpected script contents and does not extract
the tar onto the Windows filesystem or traverse its symlinks.

No official PICO-8 binary is an input to this build or bundled into the APK.
The new app uses the existing wrapper import flow to prepare the user's purchased
archive in its own private storage. It retains upstream's target SDK 28 and
all-files storage approach; those are experiment limitations, not final APK policy.
Retained third-party components still require a full license/build audit before
production redistribution. Upstream source license is preserved in
[UPSTREAM-LICENSE.txt](UPSTREAM-LICENSE.txt). Generated binaries stay untracked.

## Device use, isolation and rollback

Install the test APK alongside `io.wip.pico8`; do not uninstall or replace the
original wrapper. Grant storage access and import the user's archive. Both use
the existing `Documents/pico8` public data directory, so run only one at a time.
The original app's private settings are not migrated or replaced. First-run
copies/preparation are still upstream behavior, not the PIKOOS setup wizard.
Upstream caches bootstrap version 26 in private storage. Updating this APK alone
does not guarantee replacement of previously extracted scripts: verify their
hashes when developing a later patch. This build was first installed under a new
package ID; test injections were explicitly restored to the packaged scripts.
Do not clear user data as an implicit bootstrap-update mechanism.
Revision 2 changes the launch script inside APK resources and takes effect with
`adb install -r`; revision 1's extracted audio scripts remain identical. The
already prepared user runtime/settings remain in place. Keep the previous signed
debug APK if needed; `adb install -r -d` can roll that package version back without
uninstalling its data. The original `io.wip.pico8` app remains unchanged.

Host 0.0.24 prefers the test package when installed, otherwise uses upstream.
The original fallback **does not have this fix**. For rollback, exit the test
runtime and uninstall only `art.pikoos.runtimeexperiment`; this removes its
private prepared runtime/settings, not the public games or original wrapper.
Do not remove either app's data as part of ordinary update/testing.

`test-startup.ps1` requires the test app to be initialized and closed. It backs up
the test app's pulsar script, injects an immediate exit and a no-socket sleeping
child, checks refusal before proot and child cleanup, and restores in `finally`.
It refuses to overwrite an existing recovery backup. Logs go under `.local/`.

For the race comparison, seed a stale `tmp/pulse` by playing/exiting once. In
the isolated test package only, delay pulsar by 0.5 seconds: original host+
pulsar reproduces the SDL failure, patched host+pulsar starts successfully.
Restore the exact two patched scripts after injection. Keep this distinct from
ordinary repeated launch and real gameplay checks.

See [device evidence](../../docs/design/android-restart-23/README.md).
