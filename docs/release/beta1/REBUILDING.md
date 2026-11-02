# Source, rebuilding and replacing support libraries

The release APK contains open-source Android/Linux support software. It does not
contain PICO-8. Obtain the corresponding source-material ZIP from the **same
release** as the APK, together with this repository at that release's tag. Keep
both: the ZIP holds original sources and distribution patches; the repository
holds PikoNest's integration changes and build scripts. Individual component
licenses apply; the project's MIT license does not replace them.

## Source map

`native-source-lock.json` maps every shipped ELF to its binary evidence and source.
`sources/ubuntu/` contains complete Debian source packages (`.dsc`, original tar,
Debian/Ubuntu patch tar or diff). On a suitable Linux build host, unpack with
`dpkg-source -x <package>.dsc`, install the build dependencies from `debian/control`,
and use `debian/rules` / `dpkg-buildpackage` for ARM64. Preserve the recorded package
revision and distribution patches. This includes glibc and its dynamic loader.

Termux's build framework and package patches are in the full recipe archives:

- `sources/termux/`: the January 2025 bootstrap tree and base-library inputs.
- PulseAudio's separately pinned tree and upstream source: use its `packages/pulseaudio/`
  recipe, including the Android audio modules.
- `sources/termux-refresh/build-recipes/`: snapshot
  `e96e5b38357d314b3696df051d93cacc982d8730`; refreshed packages use this tree.
  `libltdl` is the `libtool` subpackage. Android execinfo source is in its recipe
  directory. SELinux source is supplied separately, alongside its Android recipe.
- `busybox.config` records the configuration embedded in the actual shipped
  BusyBox library, including the original Termux build paths. The matching recipe
  supplies Android patches and configuration substitutions.

Use each snapshot's own build instructions and `build-package.sh -a aarch64 <name>`
on its supported Linux build host. This is Android native code, not GNU/Linux
ARM64 code. Upstream source checksums and every retained input are recorded in the
lock. Build tools/SDKs and system dependencies may require separate installation;
a complete offline rebuild has not been demonstrated.

SDL and the shim retain their identified source revisions and permissive notices.
Their original local compiler options have not been recovered. Godot/C++ libraries
match the official AAR; its engine and dependency notices are included. Android
Java/Kotlin/plugin notices and Mozilla certificate source are also supplied.

## Build the same support refresh

Follow `experiments/runtime-restart/README.md` for the pinned upstream support APK,
Apktool and scripts, then `experiments/android-integrated/README.md`. No purchased
runtime is needed to build the application.

```powershell
.local/dependency-audit/venv/Scripts/python.exe tools/dependencies/audit.py --evidence .local/dependency-audit/evidence
.local/dependency-audit/venv/Scripts/python.exe tools/dependencies/runtime_support.py
./experiments/android-host/build.ps1 -JdkRoot 'C:\Program Files\Android\Android Studio\jbr'
```

The Python environment needs `zstandard==0.25.0` for Debian payloads. The refresh
replaces only the 20 allowlisted files; existing lifecycle/audio startup patches
are then retained by APK preparation. Verification rejects unexpected drift.

## Modify or replace a library

GPL/LGPL sources and modifications are available for study, modification and
redistribution under their own terms. PikoNest adds no restriction on reverse
engineering to debug modifications to LGPL components.

The Linux libraries remain separate shared objects in `assets/package.dat`.
Runtime startup loads them from the prepared private support directory; no
cryptographic vendor signature or runtime library hash prevents a compatible
modified library from loading. The release's build hashes are reproducibility
checks in editable source, not a restriction on user modifications.

1. Build the desired ABI-compatible replacement with the matching distribution
   recipe. Keep its SONAME, ARM64 ABI and required exports; include new dependency
   libraries and their sources/notices if needed.
2. In a private source checkout, replace the corresponding file in the support
   tar. For refreshed files, update the records in `tools/dependencies/runtime-support.json`
   and `docs/release/beta1/native-source-lock.json` to your actual bytes. Update the
   support tar output hash/size and preserve every unrelated member. These files
   are deliberately editable; do not describe a modified build as the official one.
3. Run the APK preparation/build scripts with your own signing key. Keep all
   required notices. The new support identity causes preparation to use the new
   payload while retaining the selected purchased archive and game data.
4. Android requires the same signing certificate for an in-place update. A user's
   differently signed build must use a separate application identity or be installed
   after backing up and uninstalling the official application. Restore a copy of
   user data only after verifying the custom build. Never uninstall valuable data
   merely to experiment with a library.

The complete application/integration source is supplied, so a custom APK can be
rebuilt and signed without access to the project's private signing key. Standard
Android installation is supported; the product does not require a locked appliance
or proprietary installation authorization. PICO-8 itself remains an independent,
user-supplied proprietary program and is not relicensed by this source offer.

Full terms and copyright notices: `NATIVE-NOTICES.txt`, `GODOT-COPYRIGHT.txt`,
font notices, and the original source files. This guide describes the engineering
path, not a claim that every replacement has been built or tested.
