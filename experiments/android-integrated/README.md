# Integrated Android development APK

The default `experiments/android-host/build.ps1` runs the portable core suite and
packages Workshop + the pinned runtime adapter as one APK. The installed package
remains `art.pikoos.runtimelab`; its launcher label is PikoNest. Godot runs in
`:runtime`. One APK does not include the user's proprietary PICO-8 runtime.

Use the JDK/SDK and pinned upstream APK, Apktool and run script documented in
[runtime-restart](../runtime-restart/README.md#reproducible-local-build). Also save
[boot.gd at the same pinned commit](https://raw.githubusercontent.com/Macs75/pico8-android/562662e35727ae7706fe97fed3390db249a35263/frontend/boot.gd)
as `.local/downloads/boot-1.6.6.gd`:

`3841e28ef49e53e88b09d55110ff4587f39a8e1c9f71fcebffad4982ab68da58` (SHA-256).

Python 3 (`py -3`), Java and Android build-tools 36.0.0 are required. Inputs are
validated before preparation. `prepare.py` composes the manifest, private runtime
home, boot script/class registry and sparse PCK. `verify.py` checks indexed
payloads and rejects an APK/support tar containing official PICO-8 binaries/data.

```powershell
./experiments/android-host/build.ps1 -JdkRoot 'C:\Program Files\Android\Android Studio\jbr'
```

Local result: `.local/artifacts/pikoos-runtime-lab.apk`. No GitHub APK release is
authorized. `-HostOnly` retains the old lightweight UI/core research build for
environments without the ARM64 adapter; it is not the delivery product.
`-SkipCoreTests` is only for an Android/package-only correction after the core
suite has already passed; it does not constitute a test run.

The owner approved target SDK 28 for this first integration. See
[the decision, boundaries and remaining gates](../../docs/SINGLE_APK.md).
Upstream MIT and native font notices are packaged. Complete transitive binary
licensing/source obligations remain a pre-release task; source MIT does not
relicense Godot, bootstrap libraries or third-party artwork.

## Isolated clean-install validation

For an empty private installation without clearing the owner's current app:

```powershell
./experiments/android-host/build.ps1 -ValidationOnly -JdkRoot 'C:\Program Files\Android\Android Studio\jbr'
adb -s YOUR_DEVICE_SERIAL install .local/artifacts/pikonest-clean-validation.apk
adb -s YOUR_DEVICE_SERIAL shell am start -n art.pikoos.cleanlab/.MainActivity
```

This opt-in variant changes the host namespace and launcher label only; portable
core, packaged backend and setup/Play/Test code stay the same. It does not copy
preferences, rootfs or purchased files from the main package. Select your purchased
Raspberry Pi archive and games folder through the normal UI, then verify setup,
Play/Test and return. Do not install if an unknown package with that ID exists.
After preserving evidence and confirming its game has exited, uninstall only
`art.pikoos.cleanlab`. Do not clear the main app or uninstall independent wrappers.
The default build/output remains the single product APK; the validation variant
is not a product delivery or release. `-ValidationOnly` cannot be combined with
`-HostOnly`.

The initial private runtime home remains `files/runtime-data`. Verified old-data
import may activate `files/rh/<key>` via AtomicFile; the boot script resolves only
a validated pointer and ready marker. SAF sources are never used as executable
paths. See [migration and acceptance boundaries](../../docs/SINGLE_APK.md).
