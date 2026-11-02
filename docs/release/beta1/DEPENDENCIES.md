# Native distribution evidence — early beta 1

Updated 2 November 2026. **Partially resolved; publication blocker remains open.**
No runtime binary was replaced, no device was accessed, and no APK was published.
The original candidate remains unchanged. New notice assets enter the next package.

## What is now established

The audit covers **79 ELF files**: 77 inside the bootstrap and two directly in the
APK. **44 match official binary artifacts byte for byte**. This is evidence of
origin, not a requirement that every source rebuild reproduce the same hash.

| Group | Evidence and collected materials | Remaining work |
| --- | --- | --- |
| Ubuntu runtime | **36 files**, matched against **31 binary packages / 28 source packages** from official dated Ubuntu snapshots. All 94 source-package files collected: original source archives, distribution patches, `.dsc` and build rules | Review replacement/relink instructions and combined distribution |
| Termux base | **Six files**, matched against official `bootstrap-2025.01.26-r1+apt-android-7`: Bash 5.2.37, Readline 8.2.13, ncurses 6.5.20240831-2, libandroid-support 29, and two copies of libiconv 1.17 | Recipe tree and principal sources collected, including all 37 Bash / 13 Readline patches; auxiliary terminfo inputs and full offline build dependencies remain open |
| Godot / C++ runtime | **Both APK libraries** match official `godot-lib.4.6.stable.template_release.aar`. Official release SHA-512 checked. Engine/font notices already present; LLVM libc++, libc++abi and libunwind notices collected from compiler revision embedded in the C++ runtime | Complete DEX/Android dependency review; engine build-source obligations differ from copyleft components |
| SDL | Embedded revision `SDL-2.33.0-gcd0cf8b` resolves to `cd0cf8b855d1edd0cd30fe1edaabced23ea39e9a`; source and zlib notice collected | Build options/local modifications not established; no byte-match or rebuild claim |
| Shim / wrapper | Source commit `562662e35727ae7706fe97fed3390db249a35263`, source archive, original MIT notice and existing local patch scripts retained | Rebuild/configuration comparison remains a provenance follow-up |
| Remaining Termux components | **33 files**: PRoot/loaders, Android BusyBox, talloc, Android PulseAudio/modules and associated audio libraries | Exact package revisions/build sources unresolved; do not substitute latest sources and call them corresponding source |
| Other bootstrap content | Hash inventory now also records **20 non-ELF files and 567 links** | Scripts/configuration, certificate stores and non-native APK dependencies still need full provenance/notice review |
| Official PICO-8 | Absent from support payload and source bundle; each user imports their purchased runtime | Keep proprietary binaries out of every distribution |

The exact machine-readable evidence is in
[native-source-lock.json](native-source-lock.json). The smaller
[runtime-components.csv](runtime-components.csv) retains the original 77-file
bootstrap view with updated statuses. Counts describe files, not independent
projects; PulseAudio contributes multiple modules and libiconv is present twice.

## Reusable verification and local source materials

[The audit tool](../../../tools/dependencies/README.md) now:

- verifies every native filename, size and SHA-256 against an actual APK;
- independently repeats the 44 matches against official `.deb`/ZIP/AAR files;
- downloads and verifies **153 source-material files**, about **132 MiB**, using
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
`PikoNest-beta1-partial-source-materials.zip`; it is not committed as a large binary
or uploaded as a release. Its lock can recreate the collected materials. Source
recipes have been retained, but a complete offline native rebuild has not run.
Official HTTPS index/checksum evidence is recorded; APT OpenPGP verification was
not performed, and is not claimed by the hash checks.

## Next work and publication criteria

1. Resolve the remaining Android native layer. The current official Termux
   repository no longer serves the probed historical package revisions. Avoid
   indefinite mirror hunting: prepare a controlled replacement from a pinned
   source/build tree when exact artifacts cannot be recovered. Keep the existing
   candidate as the rollback baseline and record every replacement.
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

Local validation: **10 audit boundary tests passed**, all **79 candidate native
hashes** checked, **44 official binary matches** repeated, and all **153 collected
source files** verified offline. Strict completion correctly fails while gaps
remain. The previous 91 core suites belong to the unchanged 0.0.93 candidate;
they were not rerun for this source/material-only batch.
[Recorded candidate/bundle hashes and results](dependency-evidence.json).

Primary references: [Ubuntu snapshot service](https://snapshot.ubuntu.com/),
[Termux bootstrap release](https://github.com/termux/termux-packages/releases/tag/bootstrap-2025.01.26-r1%2Bapt-android-7),
[Godot 4.6 official artifacts](https://github.com/godotengine/godot-builds/releases/tag/4.6-stable),
[Godot license guidance](https://docs.godotengine.org/en/latest/about/complying_with_licenses.html),
[GNU GPL FAQ](https://www.gnu.org/licenses/gpl-faq.en.html).
