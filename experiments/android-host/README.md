# PIKOOS Android host experiment

An isolated 0.0A/0.0C/0.0D proof: a controller-operated workshop using the
owner-approved PICO-8 visual baseline. Edit speed/jump or the hero's sprite in
a real `.p8`, send a read-only snapshot to the installed official-runtime
wrapper, and restore the workshop when the user returns.

The visual direction is approved; this implementation remains an experiment,
not a decision to use Java/Android Views for the production application.
The existing SDK-only host avoids introducing another framework while testing
input, byte edits, Android intents, URI grants and activity lifecycle.
Rendering uses a native Canvas; cartridge edits and interaction state are
JDK-only and the Android input adapter emits semantic actions.
The experiment has no library dependencies and includes no PICO-8 binary.

## Boundaries

- `core/`: JDK-only `WorkshopCartridge`, `WorkshopSession`, original
  `LabCartridge` regression fixture and `PicoRuntimeBackend` contract.
- `../p8-roundtrip/core/`: shared byte-preserving section reader/writer proof.
- `src/`: Android activity, atomic persistence, read-only URI provider and
  external-app runtime adapter.
- `assets/moon-garden.p8`: original ordinary PICO-8 Lua and a 16×16 hero.
  Each parameter/pixel edit changes exactly one byte via the shared P8 document.
  This deliberately handles the owned template, not arbitrary imported carts.
- `assets/workshop.p8`: original diagnostic fixture, retained for regression.
- `assets/Tiny5-Regular.ttf` and `OFL.txt`: Tiny5, SIL Open Font License;
  same Cyrillic-capable pixel font as the approved study, from google/fonts.
- `tests/`: executable byte-preservation/validation checks with no Android SDK.

The host requests no storage, network or privileged permissions. A scoped
`content://` read grant lets the wrapper copy the test cart to its own cache.
The canonical file is `files/projects/moon-garden/game.p8` in private app storage.
The previous `files/game.p8` is preserved. UI preferences are optional app
metadata and are not needed to run the cartridge elsewhere.
`files/run.p8` is a launch snapshot; the runtime cannot mutate the original.

## Run

Prerequisites: JDK 21, Android platform 34, Build Tools 36.0.0, and a configured
Macs75 `io.wip.pico8` wrapper (tested separately at 1.6.6 with official 0.2.7).

```powershell
.\experiments\android-host\build.ps1
adb -s SERIAL install -r .local/artifacts/pikoos-runtime-lab.apk
adb -s SERIAL shell am start -n art.pikoos.runtimelab/.MainActivity
```

The build first runs portable core tests, then compiles, packages and signs a
debuggable lab APK. Generated files and its development-only signing key stay
in ignored `.local/`. Paths can be supplied through build-script parameters.

## Controller workflow

| Input | Action |
| --- | --- |
| D-pad / stick | Choose a field; move a code line or pixel cursor |
| A / B | Confirm / back (exchangeable in Select menu) |
| Left / Right inside a value | Change the draft; A saves, B cancels |
| L1 / R1 | Previous / next tool, retaining selection |
| X | Context explanation, or sprite color palette |
| Y | Undo last edit |
| Start | Save a pending parameter and test in official PICO-8 |
| Select | Menu, including A/B mapping |

All implemented editor actions are reachable without touch. Touch uses the
same session actions: fields, tabs, palette and single-pixel taps. The code
view displays actual Lua and edits the two owned parameters; arbitrary text
entry, autocomplete, map and audio editors are not implemented yet.

Parameters are drafts until confirmation; switching tools cannot silently
discard an active draft. Writes use AtomicFile and update the in-memory model
only after persistence succeeds. Undo holds the last 32 edits for the current
process; it is not persistent history. Project bytes and the current tool,
selection, cursor, color and pending parameter draft survive process recreation.

The scene is explicitly labelled an illustration, not an official runtime
frame or a simulator. It uses the actual editable sprite. Start runs the
saved cart in the official runtime. A/B mapping affects the workshop only;
the external wrapper currently owns in-game mapping and exit controls.

During automated runtime-return checks Ctrl+Q is injected over ADB. The upstream
wrapper maps physical Select to intent-session exit, but synthetic Select
events did not prove that path; physical exit ergonomics remain to be checked.

On 2026-10-02 the Retroid Pocket Classic demonstrated repeated complete
launch/exit cycles with speeds 2 and 3, return to the host, and restoration
after its background process was killed while the runtime continued. The
saved cartridge differed from the fixture by one byte. See
[device evidence and open work](../../docs/ANDROID_RUNTIME_POC.md).

## Honest limitations

- Detection establishes wrapper installation, not a valid imported runtime.
- External backend cannot force-stop the wrapper or observe its exit code;
  returning to the host does not mark a cartridge verified.
- Upstream 1.6.6 warm restart failed in the baseline. Initially test complete
  exit/relaunch cycles. Returning Home while leaving the runtime alive may
  still trigger that upstream failure on the next launch.
- Runtime import remains upstream's flow. This experiment does not choose a
  production bootstrap or establish broad Android/Linux compatibility.
- Two cold launches on 2026-10-02 failed during PulseAudio connection, before
  a game frame; a later attempt ran. See the runtime report. No automatic retry
  or success indication hides this backend limitation.
- Physical ergonomics, held axes and hardware mapping need real controller
  acceptance. Injected Android key checks do not establish those properties.
- Canvas accessibility semantics and broader viewport/device coverage remain
  future work; this is not yet a production accessibility implementation.
