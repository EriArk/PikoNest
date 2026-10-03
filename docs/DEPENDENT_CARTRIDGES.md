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

## Include preparation (introduced in Android host 0.0.27)

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
- In 0.0.27, statements needing runtime files retained conservative rejection.
  Host 0.0.28 adds the bounded file-set path below, including calls in included code.

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
The include hint can still refuse unsupported `#include` text in comments or
strings. This is not a complete Lua parser. Include expansion changes
runtime line numbers; source-mapped diagnostics are future work. Nested include
directives are explicitly refused rather than recursively expanded.

Direct-launch and PIKOOS-prepared captures match for the owned three-form fixture
in the user's official 0.2.7 runtime. [Device/test evidence](design/android-includes-27/README.md).

## Read-only multicart experiment: host 0.0.28 / adapter revision 3

Play and external entry now stage direct `load("chapter.p8", ...)` and
`reload(dest, source, length, "data.p8")` dependencies from the selected cart's
own folder. Calls and filenames remain ordinary PICO-8 code. Reloading the
current ROM with zero to three arguments does not need an external file.

`RuntimeDependencies` follows literal calls through each cart's prepared Lua,
deduplicates cycles, and shares snapshots across includes and carts. The Android
source also caches named reads during initial include inspection. No source file
is rewritten. `RuntimeFileSet` carries immutable ordinary file bytes through the
portable runtime boundary. Limits are 32 carts, 2 MiB per cart and 8 MiB per set;
source reads also have an 8 MiB bound. These are lab limits, not PICO-8 limits.

The separate revision-3 adapter receives an internal `PIKOSET1` envelope through
one fixed read-only provider URI. This transport is **not** a new project or
distribution format. The adapter validates the entire envelope before extracting
into a fresh private session folder, then supplies that folder as PICO-8's root.
It preserves upstream's persistent runtime home for ordinary cartdata/dset.
Files are read-only (444), directory 555. After monitored native exit, only the
recorded files in that session directory are removed, without recursive deletion.
Unexpected/crashed sessions are retained; a 32-directory quota bounds accumulation.

### Deliberate current limits

- Only sibling text `.p8` files, ASCII basename letters/digits/underscore/hyphen/
  dot, at most 120 characters; lowercase extension; no `..`, case collisions,
  spaces, Unicode, directories or `.p8.png` dependencies.
- Filenames must be literal strings in direct calls. Detected computed names,
  escaped filenames, aliases, `save` and `cstore` stop preparation. Durable game
  file writes need a separate ownership/recovery design, not disposable copies.
- This is a conservative lexical collector, not a complete Lua parser or a
  sandbox. Reflection such as `_ENV["load"]` is not certified; unrelated custom
  identifiers may cause refusals. Unknown dynamic access is not general support.
- Requires the connected Games tree and Runtime Test revision 3. A single-file
  grant cannot authorize sibling reads. The old adapter still handles single carts,
  but file-set dispatch asks for an update before changing its staged snapshot.
- Crash cleanup/recovery UI, transactional source reads, provider timeouts and
  persisted cartdata regression across multicart sessions remain future checks.

The owned Petal Gate fixture exercises two-way load, parameter transfer and a
third data cart in official PICO-8 0.2.7. Native and prepared chapter captures
match. [Device/test evidence](design/android-multicart-28/README.md).

## Next boundary

Integrated runtime setup is next. Broader multicart paths, computed filenames,
PNG code inspection, durable file writes and stale-session recovery remain explicit
compatibility work; this experiment does not claim arbitrary linked-cart support.

Library/project import and export of a linked source set are separate from this
read-only Play launch workflow. They still require preservation, missing-file
recovery and round-trip checks before being advertised as supported.
