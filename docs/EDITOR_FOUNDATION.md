# Editor foundation decision — A01/A02, first slice

2026-10-03, implemented in Android lab 0.0.31. This is a bounded decision for the
next creator slices, not closure of A01/A02 or a permanent Linux UI choice.

## Decision and tradeoff

Continue the installed Java/Canvas host while moving new editing behavior into
portable Java models/workflows. Preserve the accepted PICO-8 pixel UI, existing
projects, runtime adapter and controller action vocabulary.

| Option | Benefit | Cost / current decision |
| --- | --- | --- |
| Extend Canvas + portable core | Reuses tested device input, styling, storage and runtime; direct control over controller focus | Text selection, accessibility, IME and layout require deliberate implementation. Choose for this bounded slice |
| Native Android text widget as primary editor | Mature keyboard/IME/selection | Its default interaction and styling do not solve controller input; Android-specific. Consider as an optional input adapter, not the domain model |
| Replace app framework now | Potential shared desktop/Linux UI | Would migrate proven platform behavior before measuring the actual editor limits. Defer; revisit on measured text/render/accessibility constraints |

Linux should reuse cart operations, text state and workflows; a future Linux view
need not reuse Android Canvas. The wrapper remains behind PicoRuntimeBackend.
Production wrapper packaging/clean setup is R02, still unproven and not silently
settled by continuing the existing two-APK laboratory setup.

## Operation contract

`LuaDraft` owns literal text, cursor/selection, internal clipboard, temporary
undo/redo and recovery data. It has no Android or runtime dependency. ASCII entry
is available through controller character pages and optional keyboard. Valid UTF-8
already in source is preserved; this is not a full P8SCII input/render implementation.

`CartEdit` holds the original cart and immutable candidate. Applying to another
cart is refused. `WorkshopSession` writes through its storage port before publishing
the new cart or changing history. Android compares current stored bytes with the
expected bytes before its atomic write. A failed code save leaves the draft open,
does not add history and does not launch the previous saved version.

Code and graphics commits share the existing session history: one saved code draft
is one whole-cart undo entry. Draft undo is separate from committed project undo.
Cancel/discard never writes a cart; dirty exit defaults to continuing the edit.
Test explicitly saves first. Unrelated sections and original line endings survive;
section-marker injection is refused by the framing writer. When necessary, the
changed final Lua line gets a separator before the next existing resource section.

## Demonstrated slice and open boundaries

See [device evidence](design/android-code-31/README.md) and `LuaEditorTest`.
The core compiles with the JDK alone; the same controller actions drive device UI.

- Draft recovery is app-private, versioned and persisted with project UI metadata.
  It keeps original bytes to detect a stale draft; no automatic rebase or overwrite.
- Draft/project undo stacks remain bounded to 32 in-memory snapshots. Persistent
  history, backup, multi-file transactions and Save Copy conflict resolution are P03/P04.
- The storage comparison is not a cross-process filesystem lock or a complete
  external-edit protocol. SAF project activation is still P02.
- Character selection is a fallback for free text. C02.1 now supplies structural
  insertions and parameter proposals; context-aware completion remains C03.
- Code tabs remain literal source, without a tab/include project editor (C04).
- The viewer's existing syntax colors are cosmetic; the draft does not claim a Lua
  parser or validation. Errors still come from official PICO-8; C06 remains open.
- Physical-controller ergonomics, full input remapping, full Unicode/IME and
  accessibility semantics are not accepted by this device automation pass.

Reference: official [PICO-8 manual, Code Editor / Program Structure](https://www.lexaloffle.com/dl/docs/pico-8_manual.html).
The new tool edits ordinary Lua and keeps runtime authority unchanged.

## C02.1 continuation — 0.0.32

`LuaInsert` holds a versioned proposal with 17 catalogue entries, field kinds,
ordinary Lua preview and controller text entry. `LuaDraft` inserts before the
current line, preserves its indentation/newline convention, places the caret in a
new block and adds one undo entry. No runtime binding or source annotation is added.
`LuaContext` masks strings/comments to refuse insertion inside them and detect
literal duplicate function definitions/assignments conservatively. It is not a
scope resolver: callback placement, conflicting update variants and arbitrary
expressions still need user judgment and official runtime testing.

Recovery schema 2 includes an unfinished proposal and still reads schema 1.
Opening/reviewing/cancelling a proposal does not save the cartridge. The same
storage operation commits the resulting text; failed writes keep the draft.
See [evidence and limitations](design/android-insert-32/README.md).

## C03.1 continuation — 0.0.33

`LuaSymbols` indexes simple assignments and named/assigned zero-argument functions
from the current draft. Strings retain a non-name lexical barrier while their
contents and comments are masked. Table members/constructor keys are excluded.
Any name used by a local declaration, function parameter or loop variable anywhere
in the draft is conservatively omitted. This sacrifices coverage rather than
claiming a scope resolver. No includes or external files are indexed yet.

`LuaInsert` offers a modal source/name picker only for applicable fields; choosing
an entry changes the proposed field, not the source or saved cart. Expression
fields also offer eight ordinary API calls with editable sample arguments. Simple
assignment/declaration collisions hide matching built-ins. Dynamic bindings are
not inferred. Recovery format 3 persists group/selection, reconstructs the index
from the recovered draft and still reads formats 1/2. Index/signature bounds fail
to manual input with an explicit empty state, not a partial-context claim.

See [device evidence](design/android-symbols-33/README.md). Existing-source forms,
scope-aware inline completion, source navigation and diagnostics remain open.

## C02.2 continuation — 0.0.34

`LuaCall` recognizes standalone single-line cls/print/circfill/rectfill/spr with
supported explicit arities and records exact argument spans. Balanced nested
delimiters and masked strings/comments determine boundaries; the model does not
compile expressions or infer behavior. A declaration collision refuses a built-in
form. Existing source, separators, indentation and trailing comments are preserved;
only fields actually changed replace their original spans. No-op Apply creates no
history. Replacing delimiters that escape the call is refused before mutation.

The same `LuaInsert` fields/chooser act as a proposal; editing colors accepts Lua
expressions and retains ordinary palette stepping for base color literals. Simple
string literals get text fields; complex literals remain expressions. A separate
PARAMETERS panel prevents changing the call's catalogue identity. Format 4 rebuilds
the original binding from recovered text/cursor and validates its identity; formats
1–3 remain readable. Source is checked again before applying, and storage retains
the existing expected-bytes comparison. See [evidence](design/android-parameters-34/README.md).

## C03.2 continuation — 0.0.35

`LuaNavigation` indexes named function declarations and simple function assignments
in source order, including local/parameterized declarations and duplicate names.
Strings/comments are masked. The outline is independent of callable symbol choices:
a declaration can be a navigation destination without being safe to insert as a call.
It does not resolve scopes, includes, table-index expressions or anonymous callbacks.
The 100,000-token scan guard marks a partial outline; line navigation remains available.

The NAVIGATION panel traps editing, save and Test. Preview does not move the cursor;
Confirm jumps, Cancel preserves cursor/selection, and Back-location restores both.
Up to 32 locations follow the unchanged prefix/suffix through text changes. Positions
inside the replaced span collapse to its start, including multi-argument edits; this
is positional tracking, not semantic identity. UTF-16/CRLF boundaries remain valid.
Return history is independent of edit Undo/Redo and lasts for the current draft.
Recovery format 5 stores navigation and return locations, validates bounds and rebuilds
the outline. Ordinary drafts without navigation state still encode as format 4;
formats 1–4 remain readable. See [evidence](design/android-navigation-35/README.md).
