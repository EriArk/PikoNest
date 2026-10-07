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
