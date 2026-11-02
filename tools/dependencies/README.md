# Native dependency evidence and source materials

This is a **partial source-material bundle**, not an APK release or a declaration
that distribution requirements are complete. Read the `open_gates` in
`native-source-lock.json` and `docs/release/beta1/DEPENDENCIES.md` before publishing.
The original MIT license does not replace the individual component licenses.

The lock records 79 native files: 77 bootstrap files and two APK libraries. Of
these, 60 match official binary artifacts byte for byte (36 Ubuntu, 22 Termux,
two Godot). SDL reports a source revision, and the wrapper has a pinned source
tree; these are weaker evidence than a binary match. Three PRoot files match a
secondary archived package, with recipe-checked sources collected; authentication
and source review remain open. Another 14 Termux files still need exact
source/build provenance or replacement from known sources.

## Verify locally

From the repository root, using Python 3.12 or later:

```powershell
py tools/dependencies/audit.py --apk .local/artifacts/PikoNest-0.1.0-beta.1.apk
py tools/dependencies/audit.py --materials .local/dependency-audit/source-materials
py tools/dependencies/audit.py --materials .local/dependency-audit/source-materials --offline --bundle .local/artifacts/PikoNest-beta1-partial-source-materials-r3.zip
```

The first command checks all native filenames, lengths and hashes. Missing,
changed or newly added ELF files fail; an ordinary successful audit does **not**
mean the source package is complete. `--require-complete` exits with code 2 while
there are unresolved/unreviewed records or distribution gates. An explicitly
reviewed permissive component or source build can pass without an official binary
match; an unknown status cannot. Device/owner release gates remain separate.
There is no publication command.

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

The optional dependency reads `.deb` compression. It is not bundled in the Android
application. Ubuntu input indexes came over official HTTPS, without OpenPGP
verification. For the recovered Termux PulseAudio package, the full signed chain
was verified. Add `--gpg <absolute-path-to-gpg>` to the evidence command to repeat
it, using the public key extracted from the pinned official bootstrap. This uses
a temporary keyring, never personal keys or system APT configuration. Git for
Windows GnuPG is supported. `--offline` also works once evidence is cached.

The mirror catalog is a historical snapshot, not an assertion that its packages
are current or suitable for system installation. Official Godot/Termux release
checksum lists were also checked. See the recorded catalog dates and fingerprints.

The PRoot archive is separate, weaker evidence: both ZIP and nested DEB hashes
are checked before native comparison. Missing/duplicate nested entries and changed
package bytes fail. `archived-package-match` does not clear source review, even if
the open-gate list is accidentally emptied. No downloaded program is executed.

## What the materials contain

- Ubuntu: 28 exact source packages for 31 binary packages, including upstream
  archives, Debian/Ubuntu patches, `.dsc` files and packaging/build rules.
- Termux: Bash 5.2.37, Readline 8.2.13, libiconv 1.17, libandroid-support 29 and
  ncurses 6.5.20240831-2. Bash's 37 and Readline's 13 upstream patches are present.
  The complete recipe tree is pinned to the matching bootstrap release tag's
  commit `66a7206e299d7836687dfee2130e1e2967734915`. The tag packages prebuilt
  binaries: it is not proof that every component was compiled at that commit.
- Termux PulseAudio 17.0-1: upstream source commit
  `1f020889c9aa44ea0f63d7222e8c2b62c3f45f68` and the complete recipe tree at package
  commit `0ab554db227f55a6ce8144f6f1e071d650c8ddae`. All eight PulseAudio recipe
  files match the retained January 2025 tree. Signed repository/index metadata is
  included in the materials.
- Termux PRoot 5.1.107-65: source commit
  `60485d2646c1e09105099772da4a20deda8d020d`, checked against the official recipe's
  source checksum. Its two recipe files are already in the retained January 2025
  tree. This brings the total to **156 source inputs plus two catalog files**.
- SDL revision `cd0cf8b855d1edd0cd30fe1edaabced23ea39e9a`, identified by its
  embedded version, plus the pinned wrapper source and their notices.
- Original component copyright texts, full common license texts and LLVM C++
  runtime notices. Godot engine/font notices are separately included in the app.

These are retained original source inputs, not a claim of a successful offline
rebuild. On a suitable Linux build host, Debian source packages can be unpacked
with `dpkg-source -x <package>.dsc`; their `debian/rules` and `debian/control`
describe package-specific builds and build dependencies. Termux patches/options
are under `packages/<name>/` in its recipe archive, with the build framework and
toolchain setup alongside them. Auxiliary ncurses terminfo sources and a complete
offline build have not been demonstrated; MIT licensing does not itself require
that build proof. Actual corresponding-source/replacement review remains open.

Byte matching establishes provenance here; bit-for-bit reproducibility is not
being asserted as a universal license obligation. Keep the required corresponding
source, notices and replacement/relink information available alongside the final
owner-approved distribution, under the actual applicable component terms.
