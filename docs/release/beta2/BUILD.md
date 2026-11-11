# Beta 2 build and validation record

11 November 2026. Local builds only; GitHub Actions remain disabled.

- All **94 core suites** in the host build script passed, then all Android sources
  compiled with JDK 21.0.13, `--release 8`, Android Platform 34. [Suite list](core-suites.json).
- Final focused `PlateWorkflowTest`: **16,442 checks**, including LF/CRLF source
  preservation, selection/cancel, recovery, Test isolation, failed save/retry,
  Undo/Redo, state animation/audio, protected activator references, initial overlap,
  goal and original v13 Cancel migration. The final two fixture assertions were
  added after the full run; that focused suite passed afterwards. No production
  code changed after full compilation/packaging.
- Four generated carts ran in desktop Lua 5.4 with the repository's PICO-8 API shim:
  ordinary plate 10,599, audio 10,601, starts-open 10,599, all-on goal 10,593 checks.
  [Results](lua-checks.json). These include per-coordinate assertions, not that many
  distinct user scenarios. Desktop Lua does not prove official fixed-point/limits.
- Final device evidence is recorded separately in [DEVICE_ACCEPTANCE.md](DEVICE_ACCEPTANCE.md).

## APK assembly

Reused the exact released beta-1 integrated APK as the reviewed runtime/resource
base (SHA-256 `e496a14cc5751232850fa76b393f99ce9b5fb88668c9dbb6a8317ffc2cda224f`).
Compiled current host/core/Android adapters, produced host dex with Build Tools
36.0.0 D8, decoded/rebuilt with Apktool 3.0.3, set version 96 / 0.1.0-beta.2,
replaced `classes2.dex`, aligned to 16 KiB and signed with the existing identity.
The normal source preparation scripts also now select beta 2.

Apktool SHA-256: `dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423`.
Host dex SHA-256: `1517c8ce84c56159785a1a84722cc6fc7554c52026e58ef875ccce4432951e7a`.
Exact compiled dex equals APK `classes2.dex`. The other 385 runtime/resource
entries match beta 1 byte-for-byte. Apktool normalized four adaptive-icon paths
from `mipmap-anydpi-v26` to `mipmap-anydpi`; each XML remains byte-identical and
minimum API remains 26. Manifest/resource version and signatures necessarily differ.

Package checks passed: one launcher, fixed package, version 96, min 26 / target 28,
no debugging or broad-storage permission, Godot asset index, native alignment,
APK signature, retained certificate and reviewed native inventory.
Public certificate SHA-256:
`b8a72962518f59c2585f68c9e0a189743068e0a76f6d7933f35cad53d4a8e008`.
[Package record](package.json) · [Verification](verification.json).

Strict dependency audit passed: 80 official binary matches and two reviewed
permissive components, no recorded open distribution gates. All 185 locked source
materials were rehashed inside the unchanged support-source bundle. Corresponding
sources/notices and [rebuild map](../beta1/REBUILDING.md) still match this payload.
The bundle's SHA-256 stays `9b4580bfe0bd2d7b6007a3a50892123763c529d1ff9c38405e2a6396b42a02bf`.

## Repeating focused checks

Use the JDK/core compilation and suite invocation in
`experiments/android-host/build.ps1`; `PlateWorkflowTest REPO_ROOT` emits four carts
under `.local/beta2-work/evidence`. Then run, for each emitted cart:

```sh
python3 experiments/android-host/tests/test_plate_lua.py CART.p8
python3 tools/dependencies/audit.py --apk APK --require-complete
python3 experiments/android-integrated/verify-beta.py APK AAPT
```

[Source hashes](source-sha256.json) identify the substantive implementation and
verification files in this batch. Private runtime, backups and keys are excluded.
