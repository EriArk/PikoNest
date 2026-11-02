# Runtime support distribution evidence

Updated 2 November 2026. The dependency review is complete for the recorded
payload; device acceptance and owner release authorization are separate.

## What is shipped

The APK contains **82 native files**. Eighty match official binary packages byte
for byte: 36 Ubuntu, 42 Termux and two Godot/AAR libraries. SDL and the shim have
their identified source revisions and permissive notices retained; their exact
original compiler options have not been recovered. There is no purchased PICO-8
binary or data file in the APK or source bundle.

Seventeen unidentified/secondary-archive Termux files were replaced with exact
packages authenticated through the signed January 2026 repository catalog. Three
dependencies were added: Android SELinux, PCRE2 and LAME. The refresh therefore
changes 20 allowlisted paths. PRoot is now 5.1.107-70; BusyBox is 1.37.0-3.
The existing PulseAudio 17.0-1 binaries are unchanged. Startup/audio lifecycle
patches are preserved, and an installation identity ensures existing users prepare
the updated support payload while retaining their purchased archive and game data.

The [lock](native-source-lock.json) and [inventory](runtime-components.csv) map every
file to its evidence/source. The [refresh manifest](../../../tools/dependencies/runtime-support.json)
records old/new hashes and exact package members. The earlier partial bundles and
original APK remain local rollback/evidence artifacts, not release downloads.

## Sources and notices

- **185 pinned material files**: original source packages, distribution patches,
  complete Termux recipe snapshots, signed catalog/index, Android notices and CA source.
- Exact shipped [BusyBox configuration](busybox.config), recovered independently
  from the matching binary, accompanies its source and Android patches.
- Full native notices include static LLVM OpenMP in SoXR, the C++ runtime,
  AndroidX/Kotlin/coroutines, annotations, Guava ListenableFuture and both Android
  plugins. Godot/AOSP and font notices remain separately packaged.
- OpenSSL helper scripts/configuration exactly match the recorded Ubuntu package.
  Both certificate stores contain 146 trusted certificates, all present in Ubuntu
  ca-certificates 20240203; no additional certificates/private keys were found.
  Mozilla source, attribution and MPL terms are supplied. The nested bootstrap tar
  is empty. Wrapper scripts/configuration retain their original MIT terms.
- [Rebuilding and replacement instructions](REBUILDING.md) explain the source map,
  shared-library replacement and installation of a personally signed APK.

The source ZIP is allowlisted and read back after creation. It excludes evidence
APKs/DEBs, purchased archives, projects, backups, signing keys and adjacent local
files. Download the repository's matching tag source too: it contains PikoNest's
integration patches. Full offline recompilation and byte-identical builds are not
claimed. The original resolved Gradle dependency graph remains unavailable;
notice review covers the observed DEX class families, not invented exact versions.

## Validation

All 82 native hashes are checked against the actual APK. Eighty official binary
matches were independently repeated. Seventeen Termux package records have a
verified GnuPG signature/index/package chain, anchored in the official bootstrap
public key. Ubuntu provenance uses official dated HTTPS snapshots; Ubuntu OpenPGP
verification was not performed. Twenty focused audit boundary tests pass.
All 185 source inputs verify offline; the strict completion gate passes.

The APK verifier checks the packaged notice hash, one launcher, disabled debugging,
SDK 26/28, retained startup fixes and absence of proprietary runtime files.
[Candidate and source bundle evidence](dependency-evidence.json).

[Verification commands](../../../tools/dependencies/README.md) ·
[Review rationale](LICENSING_REVIEW.md) · [Device record](DEVICE_ACCEPTANCE.md).
