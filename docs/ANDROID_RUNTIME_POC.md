# Android runtime proof: execution plan

Date: 2026-10-02. Status: official runtime baseline and experimental host
edit/run/return loop demonstrated on hardware. The owner confirmed the smoke
test's controls and sound. Roadmap 0.0A remains incomplete: production runtime
ownership, reliable warm restart and detailed input acceptance remain open.

## Scope

Establish an official PICO-8 runtime baseline on the owner's Retroid Pocket
Classic using the upstream Android wrapper. This is the first investigation
within roadmap 0.0A, followed by an isolated PIKOOS host using that wrapper.
Success does not establish a production backend or choose the application's
framework.

The Classic is the available physical test device. Core interfaces and future
layouts must remain portable to other Android handhelds and Linux.

## Plan and acceptance

1. **Inventory and provenance.** Confirm ADB authorization, model, Android/ABI,
   screen, storage and charging. Record APK version, source and SHA-256; verify
   the owner-provided runtime archive against its stored checksum.
2. **Install and import.** Install the upstream wrapper and use its normal
   runtime import flow. Pass when the official runtime visibly starts and its
   process is observed. Preserve existing device data.
3. **Known cartridge.** Create a small ordinary `.p8` diagnostic cartridge with
   movement, separate O/X counters, frame animation and a short sound. Launch
   that exact file. Pass when it runs, input is distinguishable and the owner
   confirms physical controls and audible sound.
4. **Lifecycle.** Check exit, repeated cart launch, Home/background/resume and
   screen sleep/wake. Record crashes, input loss, wrong orientation, stretching
   or failure to return. Programmatic input does not establish physical-button
   acceptance; an audio stream does not establish audible output.
5. **Evidence and next boundary.** Record observed results and remaining manual
   gates. Identify how a PIKOOS host could launch a specified cart and restore
   its context. Keep upstream-wrapper success separate from an integrated
   PIKOOS backend proof.

## Initial device observations

- Retroid Pocket Classic; stock build
  `RPClassic_V1.0.0.84_20250618_122028_user`.
- Android 14 / API 34; ABI list `arm64-v8a,armeabi-v7a,armeabi`.
- Native portrait screen reported by ADB: 1080 x 1240; 320 dpi.
- About 94 GiB available on `/data` at initial inspection.
- Firmware exposes an input device named `Xbox Wireless Controller`; the owner
  subsequently confirmed basic controls in the smoke test.
- Battery 4%, charging at the start of this plan. No firmware changes planned.

## Inputs

- Upstream: <https://github.com/Macs75/pico8-android>
- Pinned release: <https://github.com/Macs75/pico8-android/releases/tag/1.6.6>
- APK: `pico8-frontend.apk`; GitHub release asset SHA-256:
  `916330f7f4a8349c3630d207be73b402f3161a2717a8d1884228ca2c0f94da2b`.
- Owner runtime: `dev-runtime/pico-8/0.2.7/pico-8_0.2.7_raspi.zip`;
  SHA-256 `5e64187b50c470b2e345ea0ccccb04a9c098660cc8b9974672f6587741893295`.
- Downloads, research checkout, raw logs and screenshots stay in ignored
  `.local/`. Reproducible test carts and concise findings belong in Git.

## Results

### Installation and cold launch: observed pass

- Installed `io.wip.pico8` version 1.6.6. Downloaded APK SHA-256 matched the
  GitHub asset digest. APK metadata reports min SDK 24, target SDK 28 and
  `arm64-v8a`; the upstream README's stated minimum differs (Android 9/API 28).
- Granted the wrapper's requested all-files permission through Android's UI.
  Its working directory is `/sdcard/Documents/pico8`.
- Copied the owner's Raspberry Pi archive there and verified SHA-256 on device.
  The wrapper detected and unpacked it through its normal startup flow.
- Observed Splore, a `proot` process and `pico8_64`; the shim log identifies
  official PICO-8 0.2.7.
- Created and deployed `experiments/runtime-smoke/runtime_smoke.p8` to
  `/sdcard/Documents/pico8/data/carts/pikoos-tests/runtime_smoke.p8`.
- Cold launch of this exact cart works. The process arguments contain
  `-run /home/public/data/carts/pikoos-tests/runtime_smoke.p8`.
- Captured a complete square border, legible text, moving timer/bar and the
  initial six zeroed input counters. Rendering is 1024 x 1024 (8x the 128 x 128
  viewport) within a 1240 x 1080 landscape screenshot. Native portrait layout
  remains a separate ergonomics check.

### Launch interface and unresolved behavior

The externally callable entry point in this APK is
`io.wip.pico8/com.godot.game.GodotAppLauncher`. Directly addressing `GodotApp`
fails with Android's "not exported" permission denial.

Verified cold launch command (replace the serial with the selected device):

```powershell
adb -s SERIAL shell am start -W -a android.intent.action.VIEW `
  -d file:///sdcard/Documents/pico8/data/carts/pikoos-tests/runtime_smoke.p8 `
  -t text/plain -n io.wip.pico8/com.godot.game.GodotAppLauncher
```

The initial warm launch from Splore using the equivalent
`file:///storage/emulated/0/...` path left a loading indicator over the old
frame despite a new PICO-8 process. Force-stopping only `io.wip.pico8` and cold
launching with `/sdcard/...` recovered it.

Reproduced the warm-restart failure with `/sdcard/...` as well: changed the
same cart's visible label from `v1 / standard .p8` to `v2 / reload check`,
pushed it to the same device path and delivered another VIEW intent. The
wrapper created a new runtime process but kept the old image under a loading
indicator. Cold-starting the same path then displayed the new v2 label.
Thus the failure is not exclusive to the `/storage/emulated/0` alias; its
precise cause is still unproven. Warm restart does not pass acceptance.

Nonfatal diagnostics seen so far: a `proot` warning about `/proc/self/fd/7`,
and Godot messages about a missing default `bg_color` setting and duplicate
`size_changed` connection. These did not prevent the observed cold cart launch.

### Lifecycle: observed automated results

- **Home/background/resume:** `proot` and `pico8_64` entered stopped states;
  bringing the task forward resumed the same PIDs and visible animation.
- **Input after resume:** injected keyboard events held for approximately
  400 ms moved the dot and independently incremented O and X. Very brief ADB
  key events did not register reliably; this is not a physical-input test.
- **Short screen sleep/wake while charging:** Android reported Dozing and
  then Awake; the runtime stopped/resumed, the cart kept its position/counters,
  and held keyboard input worked afterwards. This does not establish long
  suspend behavior or battery drain while unplugged.
- **Graceful exit:** injected Ctrl+Q closed the official runtime and wrapper;
  no matching PICO-8/proot processes remained, and Android focus returned to
  `com.radikal.gamelauncher`. This was repeated successfully. The later host
  experiment also demonstrated return into PIKOOS; see below.
- **Cold relaunch after editing:** displayed the changed v2 cart successfully.
  Restored the canonical v1 cart and relaunched it successfully.

Local evidence is under `.local/evidence/`: `07-cold-cart.png`,
`11-held-input.png`, `14-wake-input.png`, `16-warm-sdcard.png`,
`17-edited-cold.png`, `19-final-test.png` and the captured initial/warm-restart
logs. Screenshots distinguish successful cold loading from the warm failure.

### Owner-confirmed baseline and remaining physical checks

The owner initially deferred manual checks, then reported on 2026-10-02 that
they had tried the smoke test and everything was present. Record this as broad
confirmation of basic physical controls and audible sound, not an exhaustive
per-button or lifecycle test. Remaining checks:

- Exact per-button O/X mapping, simultaneous presses and shoulders.
- Audio behavior after sleep/resume and long unplugged suspend.
- Comfortable handheld orientation and controller-based exit.

### PIKOOS host: edit/run/return demonstrated

Source and build instructions: [`../experiments/android-host/README.md`](../experiments/android-host/README.md).

Installed `art.pikoos.runtimelab` version 0.0.1 alongside the existing wrapper.
The host changes a speed field in its own ordinary `.p8` fixture and saves it
atomically in private storage. It sends a separate snapshot using `ACTION_SEND`
and a scoped, read-only `content://` grant. The host requests no storage or
network permission and bundles no proprietary runtime. The wrapper copies the
snapshot through its content resolver and launches official PICO-8 with
`-run /home/custom_mount/pikoos-lab.p8`.

Observed device results:

- Speed 2 appeared in the actual running cartridge after a touch edit.
- Normal runtime exit returned to the host with speed 2 retained; a second
  complete exit/relaunch cycle loaded the same cartridge successfully.
- Left/Right actions change speed and Start launches; verified using injected
  Android key events. Speed 3 appeared in the official runtime.
- Killing the background host process while PICO-8 ran did not terminate the
  game. On normal runtime exit Android recreated the host with a new PID;
  speed 3 and the pending-return state survived.
- Saved project bytes differ from the source fixture by exactly the edited
  speed byte. Portable tests cover no-op and edit/revert preservation, CRLF,
  unknown bytes, buffer ownership and malformed or ambiguous speed fields.

The native Java/Android Views screen is a disposable SDK-only experiment for
intents, URI access and lifecycle. It does not settle the production UI or
language choice. `core/` has no Android imports; the runtime interface reports
unsupported stop, import and result-observation capabilities explicitly. The
field editor only handles the owned fixture, not general `.p8` projects.

Returning to the host is not proof that the runtime exited successfully. The
external backend cannot observe runtime exit status or stop another package.
Complete exit/relaunch works; this does not fix the upstream warm-restart bug.
Returning Home with the runtime still alive can still expose that limitation.

Additional local evidence: `22-host-launched-cart.png`,
`24-host-return-settled.png`, `26-speed3-runtime.png`,
`27-process-restored.png`, `28-final-host-runtime.png` and
`29-final-host-return.png`. Initial host milestone APK SHA-256:
`3b3de075306c57df5d99b650df4cc538855c54305ba36da84f336241f86ba990`.

### Portable P8 integration follow-up (same date)

The host now uses the separate
[`p8-roundtrip` proof](../experiments/p8-roundtrip/README.md) for section framing
and targeted edits. Portable tests passed over all 12 official demos, both
owned fixtures and synthetic edge cases. A byte-identical round-tripped Jelpi
copy visibly ran in official PICO-8. The updated host's speed 4 also ran, with
only the speed byte changed in its canonical file.

An initial host launch soon after exiting Jelpi returned without an observed
game frame; the following cold attempt displayed the correct cart. The cause
of that early return is not established. Do not infer reliable launch from
the host's return counter. The external backend still lacks completion/error
observation, and rapid relaunch needs further investigation.

Updated lab APK SHA-256:
`c864f5035932f8775175a215b64b717fb09db6f4c10d9453094977806f93666b`.
Evidence: `39-jelpi-roundtrip.png`, `42-parser-runtime-settled.png`,
`43-parser-host-return.png`, `p8-roundtrip-tests.txt` under `.local/evidence/`.

### Next technical work

1. Isolate the upstream warm-restart failure, especially process/pipe ownership,
   while retaining the working cold launch as a reproducible baseline.
2. Extend the experimental runtime contract with owned start/stop/completion
   and import/validation, based on a backend that can actually implement them.
3. Broaden the portable `.p8` proof's corpus and runtime edge-case probes before
   using it for arbitrary cartridge import or semantic resource editing.

Source inspection was pinned to upstream commit
`562662e35727ae7706fe97fed3390db249a35263` (tag 1.6.6). The upstream wrapper's
old target SDK, broad storage access and bundled bootstrap still need their own
evaluation before choosing a production integration. Using its prebuilt APK
here is a runtime experiment, not a framework or distribution decision.

## Controller workshop implementation — 2026-10-02

The owner approved the PICO-8 visual study and its transfer to Android,
explicitly requiring almost everything to work from the controller. Lab 0.0.2
now uses the accepted palette and Tiny5 pixel font, with native workshop,
actual Lua and sprite tools. The JDK-only session handles semantic actions;
Android keys/hat/stick and touch are adapters. This extends the existing
SDK-only experiment, without choosing the production UI framework.

The new owned `moon-garden.p8` is ordinary Lua/data. Speed, jump and individual
hero pixels use byte-scoped edits through the shared P8 document. The previous
private `files/game.p8` was retained (1298 bytes); the new canonical project is
`files/projects/moon-garden/game.p8`. No runtime binary enters the host APK.

Observed on the same Retroid, 1240×1080, using ADB-injected Android keys:

- D-pad selects before editing; A enters/commits a value, B cancels without
  changing the cartridge. L1/R1 navigate workshop/code/sprites. Start launches.
- X opens the color palette; D-pad selects color and moves the pixel cursor;
  A paints, Y restores the pixel. A/B exchange is reachable in the Select menu,
  and changes the hints. Modal dialogs trap navigation and Start.
- Portable test run: **799 assertions** covering every hero pixel address,
  one-byte parameter edits, CRLF/unknown data preservation, draft cancellation,
  committed launch snapshots, controller paths, focus retention, undo and
  failed-save rollback; original `LabCartridgeTest` also passed.
- The saved new cart was byte-identical to its original asset after reverting
  test changes. Original fixture SHA-256:
  `1afbcdf44e942ab54fb6afd9daa52fdaf470adca02ff56430aa8c7616e08af76`.
- Official PICO-8 displayed the same hero/scene and responded to movement and
  jumping. A speed-3 launch and a subsequent speed-2 launch both ran.
- Killing the background host while PICO-8 ran, then exiting with Ctrl+Q,
  restored the sprite tool, cursor (8,7) and color. Separately, a pending
  speed-3 draft survived host process replacement (PID 14805 → 14951); B then
  cancelled it and retained saved speed 2.
- Touch entry into the contextual explanation also worked. The scene shown
  in the editor is labelled an illustration, not a captured/live runtime frame.

Two earlier cold launch attempts returned before any game frame. This time
the wrapper's `logs/shim.log` supplied specific evidence:

```text
SDL Error: Could not setup connection to PulseAudio
** FATAL ERROR: Unable to initialize SDL
```

`pico_err.txt` also reported that `/proc/self/fd/7` could not be sanitized.
A third attempt worked, and a later complete exit/relaunch also worked. This
isolates these observed early exits to runtime initialization, but does not
prove the root cause of every previous early return or fix warm restart.
The wrapper startup script waits for a directory rather than a ready audio
connection; a startup race is a hypothesis to investigate, not a confirmed fix.

Physical held-input ergonomics, simultaneous key/hat reports and Select exit
still need hardware acceptance. The upstream source maps physical Select to
`IntentExit`, but synthetic Select (including gamepad source) did not establish
that behavior. Automated exit used Ctrl+Q. The host cannot inspect the other
package's exit status and does not label a return as verification.

Final installed lab APK SHA-256:
`fea6df6fb9049efa5a87f4f8485f6840ffbcd14cee8a84a76496cebe79f832c8`.
Actual device captures: [workshop evidence](design/android-workshop-01/README.md).
This is one owned project and parameter/sprite editing, not arbitrary Lua
editing, import, a finished library, persistent undo or production accessibility.
