# Runtime support sources and verification

This material accompanies PikoNest early beta 1. It contains pinned original
sources, distribution patches, notices and build/replacement instructions. It
contains no purchased PICO-8 runtime, private data or signing keys. Component
licenses remain separate from PikoNest's MIT license.

The lock covers **82 native files**: 80 exact official Ubuntu/Termux/Godot binary
matches and two permissive components with source/notice review (SDL and the shim).
Seventeen unidentified or secondarily archived files were replaced by exact signed
Termux packages; three required libraries were added. PulseAudio itself is unchanged.
The 185 downloaded material inputs include source packages, complete recipe trees,
Android/plugin notices, certificate source and the signed repository catalog.

## Verify or reconstruct the bundle

Use Python 3.12+ from the repository root:

```powershell
py tools/dependencies/audit.py --apk .local/artifacts/PikoNest-0.1.0-beta.1.apk --require-complete
py tools/dependencies/audit.py --materials .local/dependency-audit/source-materials
py tools/dependencies/audit.py --materials .local/dependency-audit/source-materials --offline --bundle .local/artifacts/PikoNest-0.1.0-beta.1-support-sources.zip --require-complete
```

`--require-complete` fails on unreviewed native statuses or recorded distribution
gates. Package verification also checks the notice asset inside the APK. These
checks do not replace device testing or owner release acceptance.

To repeat binary comparisons and the signed Termux catalog chain, install
`zstandard==0.25.0` in an isolated Python environment, then:

```powershell
.local/dependency-audit/venv/Scripts/python.exe tools/dependencies/audit.py --evidence .local/dependency-audit/evidence --gpg /absolute/path/to/gpg
.local/dependency-audit/venv/Scripts/python.exe tools/dependencies/runtime_support.py
```

Use your own absolute GnuPG path. `--offline` prevents downloads once files are
cached. Verification uses a temporary public keyring, not personal keys or system
APT configuration. The January 2026 mirror is historical evidence, not a system
package source. Seventeen package records are authenticated through its signature,
index and package hashes. Ubuntu inputs came from official dated HTTPS snapshots;
their OpenPGP signatures have not been checked. Official Godot/Termux release
checksum lists were checked separately.

Missing/changed/extra ELF files, altered sources, duplicate entries and unsafe paths
are rejected. Existing corrupt files are retained for inspection. Downloads are
never installed or executed. The deterministic source ZIP includes only locked
materials and explicit support files, and is read back after creation.

## Build and modify

Read **REBUILDING.md** in this bundle (repository copy:
`docs/release/beta1/REBUILDING.md`). It maps Debian/Ubuntu source packages and the
Termux recipes, supplies the shipped BusyBox configuration, and explains rebuilding,
replacing shared libraries and installing a personally signed APK. PikoNest's
integration source and patch scripts are in the repository at the matching release
tag; download its source archive alongside this bundle.

Byte matching establishes provenance; it is not a claim of byte-reproducible builds
or a universal license requirement. A complete offline native rebuild, exact SDL
compiler options and the original resolved Gradle dependency graph have not been
reconstructed. MIT/zlib/Apache notices and GPL/LGPL corresponding source have
different requirements; original full terms are retained in the supplied files.
