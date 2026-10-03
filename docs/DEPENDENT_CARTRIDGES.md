# Linked cartridge files: launch preparation

## Contract

The editable source remains the original ordinary `.p8` plus its ordinary linked
files. PIKOOS must not turn temporary preparation into an in-place source rewrite,
custom Lua syntax, or an enhanced-cart dependency. Official PICO-8 remains the
authority for interpretation and resource limits.

The [official manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html) describes
`#include` as a boot-time insertion of a Lua text file, all code tabs of another
cartridge, or one selected tab. Paths are relative to the current cart; expansion
is not recursive. The normal code/token limits still apply. This differs from
runtime `load()`/`reload()` and file writes, which need a set of files available
throughout execution.

## Implemented experiment: Android host 0.0.27

Play and the external launcher entry can prepare a text cart with simple
`#include file.lua`, `#include file.p8` and `#include file.p8:N` directives.
`PicoIncludes` is portable and reads dependencies through a port. Its output is a
temporary ordinary `.p8`; the existing runtime port still receives ordinary cart
bytes. No wrapper update or PIKOOS runtime API is needed.

- Resolve the selected cart's parent using opaque document IDs inside the
  persisted Games tree. A single incoming file grant is insufficient to read its
  siblings; the game must be found inside that connected tree.
- Read only named dependencies, retaining one snapshot per distinct file within
  this preparation. A later launch rereads them. Neither listing nor preparing
  writes source files or imports an editor project.
- Substitute only directive lines in the unique Lua section. Resource sections,
  unknown sections and unrelated bytes stay intact. Selected tabs are zero-based;
  including a whole cart copies its code, not its graphics/audio resources.
- Validate the resulting cart before dispatch. Missing files, unsupported paths,
  duplicate names, unavailable tabs, ambiguous sections, framing changes and
  bounded-read failures abort preparation. No partially prepared cart is sent.
- Statements that may need files at runtime retain the previous conservative
  rejection, including when they occur in included code.

### Deliberate current limits

The preparer accepts lowercase `#include` on its own line, optional leading and
trailing horizontal whitespace, and a single filename with an optional tab suffix.
Inline directive comments and quoting are not yet supported. Filenames currently
use ASCII letters/digits, underscores, hyphens, dots and `/`; case is preserved.
Only `.lua` and `.p8` includes are handled, in the cart's own folder or descendants.
Absolute paths, `.`/`..`, spaces and Unicode paths are refused by this adapter.
These are **PIKOOS limitations**, not restrictions of ordinary PICO-8 projects.

Bounds: 32 directives, 2 MiB per input/output cart, 8 MiB aggregate input, and
16 path components. Parent discovery uses the existing Play index limits; path
resolution has a separate total budget of 2,048 directory entries. These limits
bound work but do not replace PICO-8's own token/character/resource verification.
Scanning and preparation cancel cooperatively between provider operations;
blocked provider calls still lack timeouts. Multiple source files cannot be read
as an atomic filesystem transaction, so concurrent edits during preparation are
not covered.

The lexical masker prevents comments and strings from causing include reads.
The final conservative launch hint can still refuse `#include`/file-call text in
comments or strings. This is not a complete Lua parser. Include expansion changes
runtime line numbers; source-mapped diagnostics are future work. Nested include
directives are explicitly refused rather than recursively expanded.

Direct-launch and PIKOOS-prepared captures match for the owned three-form fixture
in the user's official 0.2.7 runtime. [Device/test evidence](design/android-includes-27/README.md).

## Next boundary

Multicart `load()`, data `reload()`, computed filenames, PNG code inspection and
file-writing/save ownership remain open. They need a bounded file-set runtime
port and isolation of per-game working files, preserving directory layout and
original names. Do not flatten runtime loads or silently discard durable writes.
The current two-fixed-URI external adapter cannot provide this contract.

Library/project import and export of a linked source set are separate from this
read-only Play launch workflow. They still require preservation, missing-file
recovery and round-trip checks before being advertised as supported.
