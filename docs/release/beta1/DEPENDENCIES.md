# Native distribution evidence — early beta 1

Updated 2 November 2026. **Partially resolved; publication blocker remains open.**
No runtime binary was replaced, no device was accessed, and no APK was published.
The original candidate remains unchanged. New notice assets enter the next package.

## What is now established

The audit covers **79 ELF files**: 77 inside the bootstrap and two directly in the
APK. **60 match official binary artifacts byte for byte**. This is evidence of
origin, not a requirement that every source rebuild reproduce the same hash.
Three more PRoot files match a secondary archived package; they are counted
separately and are not cleared for distribution by that match alone.

| Group | Evidence and collected materials | Remaining work |
| --- | --- | --- |
| Ubuntu runtime | **36 files**, matched against **31 binary packages / 28 source packages** from official dated Ubuntu snapshots. All 94 source-package files collected: original source archives, distribution patches, `.dsc` and build rules | Review replacement/relink instructions and combined distribution |
| Termux base | **Six files**, matched against official `bootstrap-2025.01.26-r1+apt-android-7`: Bash 5.2.37, Readline 8.2.13, ncurses 6.5.20240831-2, libandroid-support 29, and two copies of libiconv 1.17 | Recipe tree and principal sources collected, including all 37 Bash / 13 Readline patches; auxiliary terminfo inputs and full offline build dependencies remain open |
| Termux PulseAudio | **16 files** match `pulseaudio_17.0-1_aarch64.deb`. Historical mirror signature, index hash and package hash verified. Exact upstream source and package recipe snapshot collected | Finish wider linked-library/source and distribution review; no binary replacement was needed for this evidence |
| Godot / C++ runtime | **Both APK libraries** match official `godot-lib.4.6.stable.template_release.aar`. Official release SHA-512 checked. Engine/font notices already present; LLVM libc++, libc++abi and libunwind notices collected from compiler revision embedded in the C++ runtime | Complete DEX/Android dependency review; engine build-source obligations differ from copyleft components |
| SDL | Embedded revision `SDL-2.33.0-gcd0cf8b` resolves to `cd0cf8b855d1edd0cd30fe1edaabced23ea39e9a`; source and zlib notice collected | Build options/local modifications not established; no byte-match or rebuild claim |
| Shim / wrapper | Source commit `562662e35727ae7706fe97fed3390db249a35263`, source archive, original MIT notice and existing local patch scripts retained | Rebuild/configuration comparison remains a provenance follow-up |
| PRoot and loaders | **Three files** match archived package 5.1.107-65; exact upstream source matches the official recipe checksum; original notice collected | Secondary archive only: official package authentication and source review remain open |
| Remaining Termux components | **14 files**: Android BusyBox, talloc and associated audio/support libraries | Exact package revisions/build sources unresolved; do not substitute latest sources and call them corresponding source |
| Other bootstrap content | Hash inventory now also records **20 non-ELF files and 567 links** | Scripts/configuration, certificate stores and non-native APK dependencies still need full provenance/notice review |
| Official PICO-8 | Absent from support payload and source bundle; each user imports their purchased runtime | Keep proprietary binaries out of every distribution |

The exact machine-readable evidence is in
[native-source-lock.json](native-source-lock.json). The smaller
[runtime-components.csv](runtime-components.csv) retains the original 77-file
bootstrap view with updated statuses. Counts describe files, not independent
projects; PulseAudio contributes multiple modules and libiconv is present twice.
[License requirements, signed catalog chain and remaining groups](LICENSING_REVIEW.md).

## Reusable verification and local source materials

[The audit tool](../../../tools/dependencies/README.md) now:

- verifies every native filename, size and SHA-256 against an actual APK;
- independently repeats the 60 matches against official `.deb`/ZIP/AAR files;
- checks three secondary PRoot matches through separately hashed outer ZIP and
  inner DEB, while retaining their weaker evidence status;
- optionally repeats the Termux catalog signature and full index/package hash chain;
- downloads and verifies **158 material files** (156 source inputs and two catalogs), using
  pinned HTTPS URLs, sizes and hashes;
- makes an allowlisted source-only ZIP with readback verification; adjacent private
  files, purchased runtime, evidence binaries and signing keys are excluded;
- rejects changed/unlisted native files, corrupt sources and unsafe paths;
- reports incomplete distribution review explicitly; `--require-complete` exits
  with code 2 until the recorded gaps are closed.

The collected component notices are preserved in
[NATIVE-NOTICES.txt](../../../experiments/android-host/assets/NATIVE-NOTICES.txt).
They include full common license texts referenced by Debian copyright files and
LLVM C++ runtime notices. This is **partial** coverage, not a statement that every
dependency is MIT or that the entire Android dependency audit is complete.
Godot/font notices remain separate. The next beta package verification also checks
the native inventory and the new notice asset; this does not permit publication.

The local source bundle is deliberately named
`PikoNest-beta1-partial-source-materials-r3.zip`; it is not committed as a large binary
or uploaded as a release. Its lock can recreate the collected materials. Source
recipes have been retained, but a complete offline native rebuild has not run.
Official HTTPS index/checksum evidence is recorded. Termux PulseAudio's catalog
signature was verified with the key from the pinned official bootstrap; Ubuntu
OpenPGP verification was not performed. The earlier partial ZIP is retained locally.

## Next work and publication criteria

1. Resolve the remaining 14 unidentified Android native files and finish the
   separate three-file PRoot source/authentication review. The main Termux endpoint no
   longer served the probed historical revisions, but one listed mirror recovered
   PulseAudio without replacement. Use targeted source recovery or a controlled
   pinned build for the remaining components. Keep the existing candidate as the
   rollback baseline and record every actual replacement.
2. Complete bootstrap certificate/configuration and DEX/Android notice review.
   Finish any missing source/build inputs and LGPL replacement/relink instructions.
   Apply each component's actual terms. MIT/zlib notices and copyleft corresponding
   source have different requirements; byte-identical rebuilding is not a blanket
   license obligation.
3. Assemble the completed materials alongside the exact candidate and PikoNest's
   source/patch snapshot. Repackage notices once this batch is complete. Recheck
   native dependency closure and local tests after any binary replacement.
4. When the owner frees the handheld, test first-run/runtime import, sound/input,
   launch/return and real Splore downloads/offline use against that exact candidate.
   Source evidence does not establish Android/runtime compatibility.
5. Publish only after the separate explicit owner approval. No release/tag/upload
   is authorized by a passing audit, build or device test alone.

Local validation: **17 audit boundary tests passed**, all **79 candidate native
hashes** checked, **60 official and three secondary binary matches** repeated, the Termux signature
chain verified, and all **158 material files** verified offline. Strict completion fails while gaps
remain. The previous 91 core suites belong to the unchanged 0.0.93 candidate;
they were not rerun for this source/material-only batch.
[Recorded candidate/bundle hashes and results](dependency-evidence.json).

Primary references: [Ubuntu snapshot service](https://snapshot.ubuntu.com/),
[Termux bootstrap release](https://github.com/termux/termux-packages/releases/tag/bootstrap-2025.01.26-r1%2Bapt-android-7),
[Godot 4.6 official artifacts](https://github.com/godotengine/godot-builds/releases/tag/4.6-stable),
[Godot license guidance](https://docs.godotengine.org/en/latest/about/complying_with_licenses.html),
[GNU GPL FAQ](https://www.gnu.org/licenses/gpl-faq.en.html).
