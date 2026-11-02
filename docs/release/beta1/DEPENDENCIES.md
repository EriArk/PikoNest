# Native distribution inventory — early beta 1

Recorded 2 November 2026. **Publication blocker still open.** This document is an
inventory and work queue, not a completed license clearance or legal opinion.

The integrated APK derives from the pinned `Macs75/pico8-android` 1.6.6 frontend.
Its source commit is `562662e35727ae7706fe97fed3390db249a35263`.
The [reproducible input instructions](../../../experiments/runtime-restart/README.md#reproducible-local-build)
record upstream APK/tool hashes. The wrapper's MIT license does not cover all
native components in its bootstrap.

[runtime-components.csv](runtime-components.csv) inventories **77 ELF files** in
the pinned `assets/package.dat`: member path, size, SHA-256 and unresolved matching
source status. It contains no binary, purchased runtime or owner data. Symlinks,
non-ELF assets, engine/font notices and Java dependencies need their own review;
77 is not a claim that the complete APK has only 77 dependencies.

## What is known

| Group | Evidence | Remaining distribution work |
| --- | --- | --- |
| PikoNest changes | Tracked original Java/Lua/scripts, MIT | Include source snapshot of the accepted commit and notices |
| Wrapper / picoshim / scripts | Pinned upstream source and preserved MIT notice | Match built shim to recipe/source and document local patches |
| Godot engine | Binary version string `4.6.stable.official`; engine license and complete copyright text from official tag `4.6-stable` now included in APK assets | Confirm exact export build/third-party components against shipped engine |
| Monocraft / Tiny5 / Press Start 2P | Preserved font license texts, included with assets | Check final archive contains each applicable notice |
| Android-side PRoot / BusyBox / Bash / libraries / PulseAudio | Termux paths/version strings in pinned native payload; BusyBox 1.37.0 and PulseAudio 17.0 identifiable | Resolve exact Termux package revisions, patches, configurations, source packages and notices |
| Linux rootfs libraries | Ubuntu build strings; glibc `2.39-0ubuntu8.4`, BusyBox `1:1.36.1-6ubuntu3.1`, PulseAudio 16.1 identifiable | Recover exact package/source provenance and applicable source/distribution materials for every hash |
| Official PICO-8 | Explicitly absent from built support payload/APK, user-imported | Keep it out of source/APK/source bundle |

The upstream Ubuntu Docker recipe describes harvesting runtime libraries. It is
not a lockfile for the precise Ubuntu and Termux binaries shipped in this APK.
Version strings alone do not establish corresponding sources or all build options.
Simply adding links to latest upstream projects would not close this evidence gap.

## Exit criteria before any APK publication

1. Map every shipped native file to an exact source package/revision, patches,
   build configuration and applicable notices. Include engine and non-ELF inputs.
2. Assemble required source/materials for the selected distribution method; retain
   hashes and a reproducible recipe. Where provenance cannot be recovered, rebuild
   the component from known pinned inputs and retest the resulting runtime.
3. Review the combined source/notice package against the exact candidate APK, with
   the documented component terms. Preserve separate licenses; do not label the
   whole APK or all its binaries as MIT.
4. Keep that material next to the approved APK when publication is authorized.
   A license inventory, passing build or private device test does not close this gate.

Official references checked during preparation:
[Godot license](https://godotengine.org/license/),
[Godot distribution guidance](https://docs.godotengine.org/en/latest/about/complying_with_licenses.html),
[GNU GPL FAQ](https://www.gnu.org/licenses/gpl-faq.en.html),
[GPLv2 FAQ](https://www.gnu.org/licenses/old-licenses/gpl-2.0-faq.html).
Apply the actual component license/version, not a blanket assumption from filenames.
