# P8 round-trip proof

Portable JDK-only framing and targeted byte-edit experiment for roadmap 0.0B.
No Android imports, external libraries, runtime binaries or network access.
Java reuses the existing host experiment's toolchain; this does not decide the
production framework/language.

## Contract

- `P8Document.parse(bytes).bytes()` is byte-identical, including line endings,
  comments, UTF-8 or opaque bytes, section order, unknown sections and EOF.
- Recognizes `lua`, `gfx`, `gff`, `label`, `map`, `sfx`, `music`, `meta`.
- Keeps each section's header/body byte offsets. Raw bodies remain uninterpreted.
- `edit(sectionIndex, from, to, replacement)` replaces only that body's byte
  range; all bytes outside it remain unchanged. It reparses and refuses edits
  that insert, merge, remove or relocate section boundaries unexpectedly.
- Duplicate sections survive no-op saves. Named lookup rejects ambiguity.
- Reads LF, CRLF, lone CR and an optional UTF-8 BOM without normalization;
  preserving these forms does not claim the official runtime accepts each one.
- Requires the exact P8 signature and a numeric `version` line. Unknown version
  numbers remain intact and do not imply compatibility.

Exact whole-line lowercase `__name__` markers delimit sections. Colon-prefixed
metadata subsection markers such as `__meta:other_tool__` stay inside `meta`.
There is no Lua lexing: a standalone section marker inside a Lua long string
is still framing here. Noncanonical marker spelling/spacing is opaque data.
Those edge cases require runtime investigation before general import support.

This is a section reader/writer, not a Lua compiler, cartridge resource decoder,
token counter, importer, or runtime verification result. It does not expand
`#include`, reinterpret shared gfx/map memory, or support `.p8.png`. Application
metadata remains outside the cartridge even though existing `meta` is retained.

## Run tests

```powershell
.\experiments\p8-roundtrip\test.ps1
# Optional private corpus from the owner's official runtime:
.\experiments\p8-roundtrip\test.ps1 -Corpus .local\p8-corpus\demos
```

Tests cover all standard section names, empty sections, duplicate/unknown
sections, mixed newlines, BOM, missing final newline, Unicode/opaque bytes,
comments/compact Lua/includes/long strings, a large synthetic file, 200 seeded
random byte cases, defensive copying and boundary-changing edits. Synthetic
data tests preservation, not validity of the data as a playable game.

Every corpus cart is checked for byte-identical no-op/body saves and reversible
length-changing edits with identical prefix/suffix bytes. A SHA-256 inventory
is printed. Corpus sources are read-only. Real demos remain in ignored `.local`
and are not redistributed with the tool or host APK.

The script prints the compiled classes directory for the CLI:

```powershell
java -cp CLASSES art.pikoos.p8.P8Tool inspect INPUT.p8
java -cp CLASSES art.pikoos.p8.P8Tool copy INPUT.p8 NEW_OUTPUT.p8
```

`copy` refuses existing output paths, including the input itself. The portable
document model does no filesystem work; atomic persistence remains the storage
adapter's responsibility. The Android lab uses it for its owned speed fixture.

## Observed results (2026-10-02)

- 228 document cases / 1609 assertions passed, including 12 official demos and
  the two owned carts. The demo inventory is in `official-demos.sha256`.
- Demos were produced by `INSTALL_DEMOS` in the owner's official 0.2.7 runtime
  on the Retroid, then copied from `/sdcard/Documents/pico8/data/carts/demos`
  into `.local/p8-corpus/demos`. No demo source is tracked or packaged.
- The CLI produced an 81,186-byte copy of `jelpi.p8` with matching SHA-256
  `008bfd81b11296fb3c131f8a152bb6195b770bbcac582a1ffdf4983959561813`.
  That exact copy launched in official PICO-8 on the Retroid; evidence:
  `.local/evidence/39-jelpi-roundtrip.png`. This is observed startup, not a
  complete playthrough or automated compatibility verdict.
- CLI copy to an existing destination was rejected without changing that file.
- Updated Android lab saves speed 4 through this core; its private `game.p8`
  differs from the fixture by exactly one byte at offset 124. Official PICO-8
  displayed speed 4 (`42-parser-runtime-settled.png`).
  Normal exit restored the host with speed 4 (`43-parser-host-return.png`).

Remaining corpus work: additional independently authored carts, valid files at
resource/token limits, real multicart/include workflows and runtime probes of
unusual framing. Resource/data decoding and semantic editing are future work.

## Reference

The [official FAQ](https://www.lexaloffle.com/pico-8.php?page=faq) describes text
editing and includes. The runtime author's
[`__meta__` announcement](https://www.lexaloffle.com/bbs/?tid=47063) describes
that block and proposed colon subsection markers. The owner's 0.2.7 manual and
runtime-generated demo files are the local execution/format reference.
