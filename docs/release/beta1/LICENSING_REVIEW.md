# Distribution review: obligations and provenance

Research update, 2 November 2026. This records findings and remaining engineering
work; it is not a completed distribution clearance. The purchased PICO-8 runtime
is outside these support materials and remains user-imported.

## Do not make every dependency follow the same rule

- GPL/LGPL components that we distribute require the applicable corresponding
  sources and notices. A later upstream version is not a substitute for the source
  used in a shipped modified build. For downloaded binaries, retained source
  packages/patches beside the download are the intended route.
- Byte matching is our provenance technique. GNU explicitly distinguishes the
  actual corresponding source from a requirement to reproduce an identical binary
  hash. Timestamps and compiler differences can change the latter.
- MIT/zlib components need their applicable notices/terms preserved. An unproven
  local rebuild of SDL or the MIT shim is a provenance follow-up, not by itself a
  reason to require a complete runtime rewrite before distribution.
- Review actual linking and component terms. Separate executables in one APK do
  not automatically put every file under the same license. Conversely, the
  wrapper's MIT license does not license the bundled GPL/LGPL libraries.

Sources: [GNU FAQ: corresponding source and exact hashes](https://www.gnu.org/licenses/gpl-faq.en.html#MustSourceBuildToMatchExactHashOfBinary),
[unchanged redistributed binaries](https://www.gnu.org/licenses/gpl-faq.en.html#UnchangedJustBinary),
[LGPL static/dynamic distribution](https://www.gnu.org/licenses/gpl-faq.en.html#LGPLStaticVsDynamic).
Preserved actual component terms are in `NATIVE-NOTICES.txt` and source archives.

The completion checker now permits an explicitly reviewed permissive component
or a reviewed source build without requiring an official binary match. Unknown
statuses, unresolved builds, source-only findings and recorded open gates still
fail completion. Dependency review and owner/device release approval are separate.

## PulseAudio finding: no replacement needed to establish origin

The main Termux endpoint no longer served the probed `17.0-1` package, but the
[official mirror list](https://github.com/termux/termux-packages/wiki/Mirrors)
identified `mirrors.sau.edu.cn`. Its historical catalog is dated **23 January 2026**.
It is used solely as retained evidence, not configured as a system package source.

The verified chain is:

1. Existing pinned official Termux bootstrap ZIP supplies the public repository
   key. Its SHA-256 was already checked against the official release checksum list.
2. GnuPG validates the mirror's `InRelease` with fingerprint
   `CC72CF8BA7DBFA0182877D045A897D96E57CF20C`, in an isolated temporary keyring.
3. The signed SHA-256/size matches the ARM64 `Packages` index.
4. That index supplies the exact package name/version/filename/size/hash for
   `pulseaudio_17.0-1_aarch64.deb`.
5. **All 16 shipped PulseAudio files** match the contents of that package.

The upstream v17.0 source resolves to
`1f020889c9aa44ea0f63d7222e8c2b62c3f45f68`. The complete Termux recipe tree at the
17.0-1 package change, `0ab554db227f55a6ce8144f6f1e071d650c8ddae`, is collected.
All eight files in its PulseAudio recipe folder also match the previously retained
January 2025 recipe tree. This includes the Android AAudio/OpenSL ES module source.
The upstream source and recipe archive are pinned in the source-material lock.

Termux's short package label says GPL-2.0; upstream's
[LICENSE](https://github.com/pulseaudio/pulseaudio/blob/1f020889c9aa44ea0f63d7222e8c2b62c3f45f68/LICENSE)
describes LGPL with exceptions and effects from optional dependencies. Both the
upstream terms and relevant module headers are retained. Do not infer the final
license of every linked component from the short package label. The wider library
review, including the remaining audio dependencies, stays open.

## Remaining 17 native files

| Group | Files | Next proof needed |
| --- | ---: | --- |
| PRoot and loaders | 3 | Exact package/source patches, or a pinned source build |
| Android BusyBox executable/library | 2 | Package revision and build configuration |
| talloc | 1 | Exact package/source provenance |
| Opus, Vorbis, FLAC, Ogg, SoXR, SpeexDSP | 7 | Package/source provenance; inspect statically included code as well |
| D-Bus, libltdl, android-execinfo, libsndfile | 4 | Exact package/source provenance and applicable notices |

Current/newer mirror packages were not accepted merely because their version
names looked close. No binary was replaced and no device test was performed.
Future replacement should be restricted to the components whose provenance
cannot be recovered, with the existing candidate retained for comparison.

## Android and other content

[APK metadata evidence](android-metadata.json) records 22 Java/Kotlin version
markers. Six contain literal Gradle task descriptions instead of versions.
The other 16 are only hints, not proof of the complete resolved dependency graph.
Do not claim that native inventory covers AndroidX/Kotlin, resources, scripts or
certificate data. The pinned Godot Java build configuration also excludes generic
`META-INF/LICENSE` and `META-INF/NOTICE` files, so absence of such files in the
input APK does not mean there are no notice obligations.

Still required: finish those notices, document the applicable source and library
replacement path, package the updated material in the exact final candidate,
and perform the separately authorized runtime/device and owner acceptance.
