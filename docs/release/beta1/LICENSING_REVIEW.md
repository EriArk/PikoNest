# Distribution review and remaining provenance limits

2 November 2026. The original PikoNest MIT scope is unchanged. Third-party
components keep their own terms; the purchased PICO-8 runtime remains outside
the distribution.

We retained corresponding GPL/LGPL sources, exact distribution patches/build
recipes and applicable notices alongside the binary download. We also retained
the BusyBox configuration and documented shared-library replacement and custom
APK signing in [REBUILDING.md](REBUILDING.md). Permissive SDL/shim/Android notices
were reviewed separately. Matching an official package is evidence of origin,
not a universal requirement to produce a byte-identical source rebuild.

The final support refresh replaces the old secondary-archive PRoot and the 14
previously unidentified Termux files with authenticated exact packages. It adds
their three required libraries. The signed historical catalog authenticates 17
package records, including unchanged PulseAudio. Sources are checked against the
matching recipes; a newer source archive was not substituted for an older binary.

Actual source terms matter: talloc's library is LGPLv3; FLAC's C library carries
BSD terms even though its broader source tree also includes GPL/LGPL material.
PulseAudio's upstream terms and module headers are retained, rather than treating
the package's short GPL label as the license of every linked library. SoXR's
statically included LLVM OpenMP has its own notice. Full original texts, including
source-family notices that cover unshipped tools, remain clearly attributed.

The Android DEX inventory identifies AndroidX/support, Kotlin/coroutines,
JetBrains/IntelliJ annotations, Guava ListenableFuture, AOSP expansion/licensing,
Godot, file-picker and app-links code. Original project notices were added where
the upstream APK's packaging excluded generic META-INF notices. Invalid Gradle
version markers were not treated as actual versions. An exact historical Gradle
resolution graph and SDL local compiler options remain provenance follow-ups.

Bootstrap OpenSSL scripts/configuration match their official Ubuntu artifact.
Both CA stores are subsets of the collected Mozilla/Ubuntu certificate source;
MPL attribution and source are included. No hidden executable was found in the
empty nested tar. The user-supplied proprietary runtime is never part of the
source bundle, APK or public test artifacts.

A complete offline rebuild has not run. Ubuntu signatures were not independently
verified; its source/binary indexes used official dated HTTPS. These limits remain
explicit instead of being described as stronger evidence.

Primary references: [GNU corresponding-source FAQ](https://www.gnu.org/licenses/gpl-faq.en.html#MustSourceBuildToMatchExactHashOfBinary),
[unchanged binary redistribution](https://www.gnu.org/licenses/gpl-faq.en.html#UnchangedJustBinary),
[LGPL linking guidance](https://www.gnu.org/licenses/gpl-faq.en.html#LGPLStaticVsDynamic),
[Godot license guidance](https://docs.godotengine.org/en/latest/about/complying_with_licenses.html).
The preserved component terms, not these summaries, govern their reuse.
