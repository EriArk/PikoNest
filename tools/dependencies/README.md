# Native dependency evidence and source materials

This is a **partial source-material bundle**, not an APK release or a declaration
that distribution requirements are complete. Read the `open_gates` in
`native-source-lock.json` and `docs/release/beta1/DEPENDENCIES.md` before publishing.
The original MIT license does not replace the individual component licenses.

The lock records 79 native files: 77 bootstrap files and two APK libraries. Of
these, 44 match official binary artifacts byte for byte (36 Ubuntu, six Termux,
two Godot). SDL reports a source revision, and the wrapper has a pinned source
tree; these are weaker evidence than a binary match. Another 33 Termux files
still need exact source/build provenance or replacement from known sources.

## Verify locally

From the repository root, using Python 3.12 or later:

```powershell
py tools/dependencies/audit.py --apk .local/artifacts/PikoNest-0.1.0-beta.1.apk
py tools/dependencies/audit.py --materials .local/dependency-audit/source-materials
py tools/dependencies/audit.py --materials .local/dependency-audit/source-materials --offline --bundle .local/artifacts/PikoNest-beta1-partial-source-materials.zip
```

The first command checks all native filenames, lengths and hashes. Missing,
changed or newly added ELF files fail; an ordinary successful audit does **not**
mean the source package is complete. `--require-complete` exits with code 2 while
there are unresolved records/gates. There is no publication command.

`--materials` downloads only pinned HTTPS sources and verifies every size/SHA-256.
`--offline` permits no downloads. A corrupt existing file is retained and rejected.
Downloads are never installed or executed. The bundle contains only allowlisted
source archives/patches, this guide, the lock and notices; no evidence APK/DEB,
purchased runtime, projects, signing keys or arbitrary adjacent files are included.

To independently repeat the binary comparison, use an isolated Python environment
with `zstandard==0.25.0` installed, then:

```powershell
.local/dependency-audit/venv/Scripts/python.exe tools/dependencies/audit.py --evidence .local/dependency-audit/evidence
```

The optional dependency only reads Ubuntu `.deb` compression. It is not bundled
in the Android application. Input indexes came over official HTTPS; this audit
does not claim to have verified APT OpenPGP signatures. Package and source hashes
are pinned, and the official Godot/Termux release checksum lists were checked.

## What the materials contain

- Ubuntu: 28 exact source packages for 31 binary packages, including upstream
  archives, Debian/Ubuntu patches, `.dsc` files and packaging/build rules.
- Termux: Bash 5.2.37, Readline 8.2.13, libiconv 1.17, libandroid-support 29 and
  ncurses 6.5.20240831-2. Bash's 37 and Readline's 13 upstream patches are present.
  The complete recipe tree is pinned to the matching bootstrap release tag's
  commit `66a7206e299d7836687dfee2130e1e2967734915`. The tag packages prebuilt
  binaries: it is not proof that every component was compiled at that commit.
- SDL revision `cd0cf8b855d1edd0cd30fe1edaabced23ea39e9a`, identified by its
  embedded version, plus the pinned wrapper source and their notices.
- Original component copyright texts, full common license texts and LLVM C++
  runtime notices. Godot engine/font notices are separately included in the app.

These are retained original source inputs, not a claim of a successful offline
rebuild. On a suitable Linux build host, Debian source packages can be unpacked
with `dpkg-source -x <package>.dsc`; their `debian/rules` and `debian/control`
describe package-specific builds and build dependencies. Termux patches/options
are under `packages/<name>/` in its recipe archive, with the build framework and
toolchain setup alongside them. Auxiliary ncurses terminfo sources, complete
offline build dependencies and source/replacement instructions still need review.

Byte matching establishes provenance here; bit-for-bit reproducibility is not
being asserted as a universal license obligation. Keep the required corresponding
source, notices and replacement/relink information available alongside the final
owner-approved distribution, under the actual applicable component terms.
