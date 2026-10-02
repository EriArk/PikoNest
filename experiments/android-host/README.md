# PIKOOS Android host experiment

An isolated 0.0A/0.0C proof: edit a speed value in a real `.p8`, save it,
send a read-only snapshot to the installed official-runtime wrapper, and
restore the workshop when the user returns.

This is not the product UI or a decision to use Java/Android Views for the
portable application. Native Java and the existing Android SDK provide the
smallest host for testing Android intents, URI grants and activity lifecycle.
The experiment has no library dependencies and includes no PICO-8 binary.

## Boundaries

- `core/`: JDK-only `LabCartridge` and `PicoRuntimeBackend` contract.
- `src/`: Android activity, atomic persistence, read-only URI provider and
  external-app runtime adapter.
- `assets/workshop.p8`: owned standard cartridge fixture. The field editor
  changes one byte and preserves every other byte. It is not a general parser
  and does not accept arbitrary imported carts.
- `tests/`: executable byte-preservation/validation checks with no Android SDK.

The host requests no storage, network or privileged permissions. A scoped
`content://` read grant lets the wrapper copy the test cart to its own cache.
The canonical file is `files/game.p8` in the host's private app storage.
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

Change speed using buttons or Left/Right. Test using the button or Start.
Exit the runtime normally to return; during automated device checks Ctrl+Q is
injected over ADB. Changes persist through host activity/process recreation.

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
- Controller ergonomics and final visual design require separate acceptance.
