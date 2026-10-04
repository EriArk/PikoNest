# PikoNest Architecture

## Status

This document defines architectural boundaries and the intended shape of the system. It is not a claim that every mechanism described here has already been proven.

Where a detail is still experimental, it is marked as such.

Current inventory (2026-10-03, lab 0.0.30 / adapter 4): see [STATUS](STATUS.md).
The subsequent 0.0.31 [editor decision](EDITOR_FOUNDATION.md) introduces portable
LuaDraft/CartEdit, a compare-before-write storage port and a controller editing view.
Implementation order and tracked gaps live in [ROADMAP](ROADMAP.md) and
[BACKLOG](BACKLOG.md). Dated lab subsections below retain the scope of those
versions; they are not a claim that later work is still absent or that the
proposed architecture has been completed.

Current shipping target is the Android APK. [Android app requirements](ANDROID_APP.md)
define adaptive device coverage and the external-launch contract. That entry must
map validated storage/format/origin into the shared application launch workflow;
Android intent parsing belongs in the platform adapter. Play, workshop and external
return destinations must remain distinct across lifecycle restoration. No external
production entry-point API is frozen yet; a tested explicit Android lab entry
exists since 0.0.25. Linux implementation is deferred.

### Android lab 0.0.24: isolated runtime audio lifecycle fix

The Android adapter prefers the installed `art.pikoos.runtimeexperiment` package,
falling back to `io.wip.pico8`. A pinned, locally rebuilt upstream wrapper fixes
the bootstrap race between opening an old audio directory and asynchronous
removal/recreation. Cleanup precedes server launch; readiness checks the socket
with liveness/timeout; exit reaps the owned audio child. Core/project/UI code does
not own these processes. The user's official runtime is still imported separately.

This is a two-APK development arrangement, not a final packaging decision. Both
wrappers share public runtime data; original private settings stay separate. The
original fallback remains unfixed. Intent acceptance still cannot observe full
runtime success/failure, and production import/diagnostics remain open.
[Experiment and rollback](../experiments/runtime-restart/README.md).

### Android lab 0.0.23: PNG Play transport and covers

`CartridgeFormat` crosses the portable runtime port explicitly. Android stages
exact source bytes to separate fixed read-only `run.p8` / `run.p8.png` URIs, with
matching display name and MIME type. Existing text-only backends reject the PNG
overload by default. Workshop editing remains canonical text `.p8`.

Portable `P8Png` validates a bounded PNG envelope: 160×205, RGBA8, non-interlaced,
chunk CRCs, exact inflated size and scanline filters. It extracts a 128×128 cover
at (16,24) for display only, preserving RGB and ignoring encoded alpha bits.
Labels from both formats reach the UI as ARGB. The original file is never
re-encoded. This is not a compressed-Lua decoder or a cart compatibility verdict;
PNG dependencies are not inspected. Runtime acceptance remains authoritative.
Other PNG variants have an explicit unsupported-format error.
[Verification and current wrapper lifecycle limitation](design/android-png-22/README.md).

### Android lab 0.0.22: separate Play library

`PlaySession` owns selection, all/recent/favorite filters and semantic actions.
It has no ProjectStore/WorkshopCartridge dependency. `PlayCartridge` inspects
text framing and decodes standard 128×128 base-palette labels; it is not a Lua
validator or a full dependency analyzer. A conservative pattern blocks include
directives and direct load/save/cstore/reload(...) references except `reload()`.
Comments/strings/shadowed names may cause false positives; indirect file calls
may evade the hint. Do not claim general multicart/dependency compatibility.

Android `GameFolder` lists one SAF tree level, up to 128 games/2,048 entries,
with a 2 MiB per-file lab read budget. Limits and subdirectories are surfaced.
Unreadable and unsupported entries remain visible with an explanation.
Indexing retains labels/metadata, not all cartridge payloads. Launch re-reads and
rechecks a fresh snapshot before handing exact bytes to the existing backend.
Only that runtime snapshot and Play metadata are written; originals are untouched.
Generation checks reject stale scans/launch reads after leaving the screen.

Play is the default home (pending import/export flows may resume first).
R opens the project shelf, L there returns to Play. Folder setup returns to its
originating screen. Stable document URIs identify favorites, selected cart and
recent launch timestamps; rename/move identity migration is not implemented.
Recent means accepted runtime launch, not observed play completion or a save state.
Persisted runtime origin restores the correct shelf after Activity/process loss;
editor launches still return to the editor. Runtime setup remains experimental.

### Android lab 0.0.21: persistent folder intentions

Portable `FolderSetup` owns four roles, semantic selection, access states and
publish-after-verification behavior. Locations are opaque identifiers. Failed
replacement/persistence retains the previous entry; denied rechecks preserve
the original handle for reconnect. Cold restoration starts UNKNOWN, not READY.
Cancellation ignores late results and never redirects a project store.

Android `FolderAccess` uses persisted SAF tree grants, directory queries and an
owned UUID probe for writable roles. Probe bytes are read back; only the created
probe URI is deleted. Games require read access only. Provider calls run on a
worker; destroyed/stale Activities cannot commit results. A process killed during
the probe may leave its uniquely named temporary file; no cleanup scans delete
unknown files. Folder preferences and pending picker role survive recreation.
Existing project/asset stores and external runtime paths are unchanged: these
are prepared locations, not active migration or Splore synchronization.

Controller-first Play is now an explicit near-term workflow: its library/index
must be separate from editable ProjectStore. Browsing/launching external games
must not call editor import or rewrite originals. See PRODUCT_VISION and ROADMAP.

### Android lab 0.0.20: pixel-fragment movement

Portable `SpriteMove` owns three-stage selection/placement intent within an
opened `SpriteRegion`. Endpoints are pixel coordinates; destination clamping keeps
the entire fragment in scope. It computes a preview against the current cart.
`P8Graphics.move` captures the original source, clears it to index 0, then writes
the captured pixels into the destination. It writes the union through the existing
lossless gfx writer, preserving unrelated data and handling overlap without smear.

`WorkshopSession` traps the workflow in `MOVE`; confirm calls the save port before
publishing/history. Cancel never writes; failures retain the draft for retry.
The UI journal stores only validated selection/placement intent and return mode,
not duplicate cart bytes. Restored state requires explicit confirmation. General
map-reference relocation, clipboard interoperability and wrapping sprite shifts
remain separate work. [Device evidence](design/android-move-19/README.md).

### Android lab 0.0.19: oval rasterization

`P8Graphics.withOval` validates bounds/color, normalizes corner order and builds
a local indexed-pixel mask before passing values to the existing lossless gfx
writer. Radii are integer halves of coordinate spans; mirrored centers preserve
symmetry and inclusive bounds for even widths/heights. Both axes are sampled to
cover steep and shallow parts; filled ovals join the extreme pixels of each row.
Point, one-row and one-column cases use the same path. This portable operation
does not evaluate Lua, modify palette state or depend on an Android drawing API.

Two appended `DrawTool` values reuse the pending-stroke transaction, recovery,
touch/controller, failure and history paths. Existing menu indices remain stable.
The owned runtime diagnostic produces reference masks from official PICO-8 0.2.7;
their independent SHA-256 corpus guards pixel semantics in the core test suite.
It is a sampled compatibility check, not a general runtime/API certification.
[Evidence and fixture provenance](design/android-oval-18/README.md).

### Android lab 0.0.18: reversible cartridge history

Portable `WorkshopSession` keeps undo/redo deques of immutable whole-cartridge
snapshots, with at most 32 retained edits in total. An effective new edit calls
the save port before pushing the previous cart and clearing redo. Undo/redo saves
the target before moving either stack or publishing it; failures preserve the
current cart and both stacks. No-ops do not write or branch history.

The semantic `REDO` action is exposed through the paired menu row and an Android
R2 mapping. Modal drafts consume it without changing their base cart. Existing
contextual Y actions remain intact. History does not include asset-store naming,
exports, project creation or UI-only selection/color changes. Runtime launches
receive exactly the currently saved snapshot. Current selection/cursor are not
rewound by cartridge history.

The existing Activity session cache preserves per-project history on navigation
when loaded canonical bytes still match; new Activity/process sessions start
with empty history. This remains a bounded lab implementation, not a persistent
command journal or final memory-budget design. No history data enters `.p8`.
[Device evidence](design/android-history-17/README.md).

### Android lab 0.0.17: two-point rectangle brushes

`P8Graphics.withRectangle` changes indexed pixels within inclusive integer corners,
in either direction, with a border-only or solid mask. It uses the existing gfx
writer, so outside pixels and unrelated sections remain unchanged. The cart
wrapper exposes the operation without platform or gameplay-role dependencies.

`WorkshopSession.pendingStroke` covers lines and both rectangle brushes. Anchor,
cursor, selected color, region and brush define a recomputed immutable preview.
Confirm saves once before publication/history; Cancel/Y clears the draft; Start
saves before launching, preserving the existing line workflow. Failure leaves
the draft available for retry. No-op writes/history are suppressed as before.

The old `lineX`/`lineY` preference keys now hold the first point for every two-point
brush, while `drawTool` continues to persist its enum name. Portable `restoreStroke`
checks mode, bounds, tool and the lab shared-map restriction before restoring a
read-only draft. Menu indices are mapped explicitly so existing transform/recolor
entries retain positions 5/6 and newly appended brushes cannot collide with them.
No Lua shape calls or PikoNest-specific runtime data are generated.
[Device evidence](design/android-rectangle-16/README.md).

### Android lab 0.0.16: selected-region color replacement

Portable `P8Graphics.replaceColor` changes matching palette indices only within
the selected rectangle through the existing byte-preserving gfx writer.
`WorkshopSession` owns the source/target pair, active field, changed-pixel count,
immutable candidate and return context. Color selection recomputes from the
canonical cart; it never chains temporary replacements. One confirmation uses
the durable save port and creates one in-memory undo entry. No-ops do neither;
failed saves retain the candidate. Other resources and code stay byte-exact.

Android persists only optional operation context and recomputes the preview
against current canonical bytes after process recreation. It never applies the
restored choice automatically. The base palette/checkerboard preview is an asset
view, not execution of `pal`/`palt` or game Lua. Index 0 is a real editable color
index, not an out-of-band alpha channel. Existing lab restrictions on editing
map-shared sheet rows remain explicit. [Evidence](design/android-recolor-15/README.md).

### Android lab 0.0.15: sprite transformations

Portable `SpriteTransform` describes permutations within an unchanged selection
footprint. `P8Graphics.transform` snapshots source pixels before writing only
changed gfx values, preserving unrelated section bytes and row conventions.
`WorkshopSession` owns a modal, immutable candidate, selected operation and return
mode. Browsing operations always recomputes from the canonical cart, never from
the previous preview. Confirm uses the existing durable save port before publishing
state and adding one undo entry. No-ops do neither. Failed saves keep the candidate.

Android stores only selection/operation/return context in optional UI preferences.
Restoration recomputes a read-only candidate from current canonical bytes and
requires confirmation; no automatic application. Undo history is still in-memory.
The transform changes actual indexed pixels, not `spr`/`sspr` flip arguments,
sprite flags, bindings, collision dimensions or any runtime API. Square-only
quarter turns and the upper-half sheet restriction are explicit lab boundaries.
[Device evidence and tests](design/android-transform-14/README.md).

### Android lab 0.0.14: export with verified persistence

Portable `CartridgeExport` holds the selected project's saved bytes, display
title and source ID. Its versioned PKE1 journal records preview/writing/saved/
uncertain states. `LibrarySession` controls preview, file selection, explicit
retry and acknowledgement through ports. It reads the canonical cart again at
preview creation, not a thumbnail or the active project's editor draft. Bytes
remain fixed after preview; export never normalizes data or edits the project.

Android `ACTION_CREATE_DOCUMENT` selects a new document. `ExportJob` writes on
a worker, closes the output, reopens the URI and compares bytes, then journals
the result atomically. It reports saved only after verification and journal
completion. I/O/verification failure retains an uncertain snapshot; the external
document may be partial and is not automatically deleted or overwritten.
Retry invokes a fresh system selection. No persistable URI grants are needed.
The worker owns the journal while running; Activity recreation retains it and
reattaches the result listener without reading AtomicFile concurrently. After
process death, a writing journal restores as uncertain and never restarts a write.
Preview and verified results also restore without side effects. The 2 MiB budget
is experimental host policy. Includes, multicart siblings, editor metadata and
library records are not packed into the exported `.p8`.

The UI does not call this Share Clean Cartridge: personalized-cart binding
removal is a separate future workflow, and byte-exact export cannot remove it.
Sources: [Android create-document behavior](https://developer.android.com/training/data-storage/shared/documents-files),
[ordinary PICO-8 carts](https://www.lexaloffle.com/dl/docs/pico-8_manual.html).
[Device evidence](design/android-export-13/README.md).

### Android lab 0.0.13: single-file import boundary

Portable `CartridgeImport` holds a read-only cart snapshot, display filename and
stable random destination ID. Its bounded versioned record persists a preview;
it is editor metadata, never an alternate cartridge format. `LibrarySession`
owns confirm/cancel/error/retry states and delegates selection/publication through
ports. Successful parsing proves editor readability, not valid Lua or runtime
compatibility. A 2 MiB intake budget protects the lab reader; it is not a console
limit. No dependency resolver or `.p8.png` decoder is implied.

Android uses `ACTION_OPEN_DOCUMENT`, reads the selected URI on a worker thread
without a write grant or persistent source dependency, and saves the preview via
AtomicFile. Cancellation ignores late provider results. Killing/recreating the
activity during an unfinished provider read requires selecting again; a completed
preview survives process death and never confirms itself. New projects publish
from a staged directory after `game.p8` and optional `.pikoos/import-name` are
complete. Retries compare bytes and title at the same destination; they cannot
overwrite an edited project. Clearing a completed/cancelled preview writes an
empty atomic record. Interrupted staging files are retained, hidden from the shelf.

User-facing source labels use project names; asset provenance still includes
the source-cart SHA256 and coordinates. Previously saved asset records are not
rewritten. Sources: [Android SAF](https://developer.android.com/training/data-storage/shared/documents-files),
[PICO-8 formats and includes](https://www.lexaloffle.com/dl/docs/pico-8_manual.html).
[Evidence](design/android-import-12/README.md).

## 1. Architectural goals

PikoNest must satisfy several constraints that pull in different directions:

1. use the user's official PICO-8 runtime as the authoritative execution engine;
2. provide a much friendlier handheld-first creation environment around it;
3. keep ordinary projects compatible with real PICO-8;
4. work first on Android handhelds;
5. remain portable to Linux handhelds later;
6. support devices with different screens and control layouts;
7. function without a physical keyboard;
8. keep optional PikoNest-only experiments from contaminating the standard project path.

The architecture should therefore be modular, with the PICO-8 project model and editor logic above a replaceable platform/runtime layer.

## 2. High-level layers

```text
┌──────────────────────────────────────────────┐
│                 PikoNest UI                  │
│ Library · Create · Learn · Editors · Play   │
└──────────────────────────────────────────────┘
                       │
┌──────────────────────────────────────────────┐
│            Application workflows             │
│ Open · Save · Remix · Test · Import · Run   │
└──────────────────────────────────────────────┘
                       │
┌──────────────────────────────────────────────┐
│              Portable core/domain             │
│                                              │
│ P8 model/parser/writer                       │
│ Project model                                │
│ Mechanics library                           │
│ Learning/context model                       │
│ Budget/compatibility analysis                │
│ Cartridge/library metadata                  │
│ Multicart model                              │
└──────────────────────────────────────────────┘
                       │
┌──────────────────────────────────────────────┐
│                 Port interfaces               │
│ Runtime · Input · Storage · Bridge · Network │
└──────────────────────────────────────────────┘
             │                       │
┌──────────────────────┐   ┌──────────────────────┐
│   Android adapters   │   │    Linux adapters    │
│       first          │   │       later          │
└──────────────────────┘   └──────────────────────┘
```

Platform code must not leak upward unnecessarily.

## 3. Suggested top-level modules

Exact source directories depend on the chosen framework/language, but the conceptual modules should remain recognizable.

### `core/p8`

Responsibilities:

- parse `.p8` text cartridges;
- represent cartridge sections;
- write `.p8` safely;
- preserve unknown/unmodified material where possible;
- calculate code/token/resource budgets;
- inspect symbols and basic Lua/PICO-8 structure;
- eventually support `.p8.png` import/export if feasible and useful.

This is one of the most important correctness-sensitive modules.

### `core/project`

Responsibilities:

- represent a PikoNest project;
- map canonical carts to optional `.pikoos/` metadata;
- represent multicart relationships;
- manage project identity without changing the cart unnecessarily;
- create remix/project copies.

The generic project/cart contract has no mandatory hero, character, movement
fields, collision body or scene graph. Resources and gameplay uses are separate:
a sprite may serve multiple objects or no object at all. Role-specific bindings
belong to optional template/mechanic adapters over ordinary Lua/data; unsupported
bindings do not make an otherwise valid cart invalid.

Android lab 0.0.9 separates those bindings into optional `MoonGardenBinding`.
`WorkshopCartridge` opens/edits ordinary supported gfx independently and retains
unrecognized Lua; missing gfx is materialized on the first effective pixel edit.
`HeroBinding` still describes only the Moon Garden character. The generic view
offers resources and code without guessing gameplay roles. This proves a small
hero-free path, not a complete generic project model, Lua editor or importer.
Automatic free-slot allocation remains restricted to the owned platformer;
arbitrary code may use visually empty pixels. Other carts use explicit selection.

Android lab 0.0.10 adds a template-independent explicit `copyRegion` operation
and placement/preview/commit states in portable `WorkshopSession`. It copies
pixels only; no role assignment, reference rewrite or claim of free allocation.
The UI currently covers the upper 128×64 with an 8-pixel placement grid. Occupied
destinations require a before/after preview; overlapping source/destination is
rejected to retain the original (an editor policy, not a PICO-8 limitation).
Durable save precedes publishing state/history. Cancel writes nothing; undo
restores exact bytes. Restored placement always requires reviewing the preview
again, even when the app was closed on the confirmation screen.

Tool registration/availability must not depend on template ID, genre or hero
binding. Bindings provide optional contextual parameter panels. General tools
and the mechanics catalogue remain discoverable for blank/imported projects;
resource/target/binding prerequisites are resolved inside the operation.

### `core/mechanics`

Responsibilities:

- mechanic definitions;
- parameters;
- dependencies;
- preview metadata;
- code/data generation;
- human explanations;
- learning concepts associated with each mechanic.

A mechanic must ultimately produce normal PICO-8-compatible code/data.

### `core/learning`

Responsibilities:

- known/encountered concepts;
- contextual explanation state;
- optional tiny challenges;
- skill-book progress;
- mappings from editor actions/mechanics/errors to learning concepts.

Learning state is PikoNest metadata, not part of the game cart unless explicitly chosen for a game feature.

### `core/library`

Responsibilities:

- installed/local carts;
- favorites;
- play history;
- screenshots;
- metadata/cache;
- relationship between an original cart and local remixes/projects.

### `core/assets`

The separate proposed `core/assets` module owns reusable asset snapshots,
dependencies, provenance, revisions and insertion plans across projects/carts.
The application owns save/extract/preview/insert/update workflows and transactions;
storage adapters persist the library. Keep this distinct from `core/library`'s
cartridge catalogue. See [asset requirements](ASSET_LIBRARY.md). Exact package
formats and revision/update implementation are TBD.

Android lab 0.0.11 proves a sprite-only slice with immutable portable `SpriteAsset`
snapshots and the existing placement transaction in `WorkshopSession`. Export
uses a storage port and never writes the source cart. Insert materializes color
indexes through `P8Graphics`; canonical state/history advance only after durable
cart save. Import drafts persist their own snapshot, rather than just an asset ID.
Neither the source cart nor the library record is needed to finish that draft or
run the inserted cart. Errors retain the candidate and return to preview for retry.

`SpriteAssetStore` is an Android adapter using atomic app-private files outside
individual projects. Its temporary v1 `.pksp` record format and limits are
documented in [the slice evidence](design/android-assets-10/README.md). Listing
records reconstructs the catalogue; there is no separately authoritative index.
Replacing this adapter with user-selected storage remains required by first-run
setup. The binary library record never becomes a cartridge/runtime dependency.

Lab 0.0.12 adds portable `NameEditor` drafts and metadata-only rename transactions.
Display titles are not Lua identifiers or filenames; record UUIDs stay stable.
The Android adapter compares the stored record with the expected snapshot before
an atomic title replacement, rejecting stale edits. Identical completed retries
are accepted. Catalogue selection follows UUID through sorting/reopening.
Pending text, alphabet, case, key focus and selection state are optional UI
preferences. Restoring them never commits a name. New-export naming returns to
the export preview for a separate save. Cart bytes and cart undo stay untouched.

### `core/compatibility`

Responsibilities:

- standard vs PikoNest-enhanced capability classification;
- required extensions;
- resource warnings;
- verification results;
- compatibility badges/status.

## 4. Runtime boundary

The official PICO-8 process must be treated as an external runtime controlled through a replaceable backend.

Conceptual interface:

```text
PicoRuntimeBackend
  detectRuntime()
  importRuntime(source)
  validateRuntime()
  getRuntimeInfo()
  launchCart(cartPath, launchOptions)
  launchEditor(optionalCartPath)
  stop()
  getCapabilities()
```

The application should not care whether the implementation is Android/proot or native Linux.

### Runtime capabilities

Backends may expose capabilities such as:

```text
canLaunchCart
canLaunchSplore
canLaunchEditor
canReturnExitCode
canCaptureLogs
canInjectInput
canUseBridge
canUseLinkPlay
```

Experimental features should check capabilities rather than assume them.

## 5. Android runtime backend

### Intended direction

The user imports their own official ARM64/Raspberry Pi PICO-8 distribution.

PikoNest stores/validates the imported runtime in app-controlled storage and launches it through a small Linux compatibility environment, likely using a proot-style setup and any required shim/adaptation.

Conceptually:

```text
PikoNest Android app
        │
        ▼
Android runtime adapter
        │
        ▼
minimal Linux/proot environment
        │
        ▼
official user-provided PICO-8 ARM64 binary
```

### Important rule

Do not bury application logic inside the wrapper.

The wrapper's job is to make official PICO-8 execute correctly and map platform facilities. Project/editor logic remains outside it.

### Research tasks

Before depending on this architecture, prove:

- runtime import;
- reliable launch on target Android versions;
- graphics/audio/input behavior;
- gamepad mappings;
- filesystem paths;
- launching a specified cart;
- clean exit/return to PikoNest;
- suspend/resume behavior;
- lifecycle when Android backgrounds the app;
- whether an LD_PRELOAD/native shim is necessary;
- whether the wrapper can later expose a safe PikoNest bridge.

## 6. Linux runtime backend

The future Linux backend should reuse the same runtime interface.

On compatible handhelds it may be able to launch a user-provided official PICO-8 Linux/ARM build with much less indirection.

Conceptually:

```text
PikoNest Linux app
      │
      ▼
LinuxRuntimeBackend
      │
      ▼
official PICO-8
```

Do not assume a desktop Linux environment. Targets may use game-console-style sessions, unusual compositors, controller-only input and read-only/system-managed filesystems.

## 7. Platform services boundary

Additional abstractions should exist where platform differences are expected.

### Storage

```text
StorageProvider
  appDataRoot()
  projectsRoot()
  gameSourceRoots()
  sploreDownloadsRoot()
  userDataRoot() // persistent asset library and metadata
  runtimeRoot()
  importFile(...)
  exportFile(...)
```

Android scoped storage and Linux filesystem access are different; core logic should not know the details.

These roots are logical storage handles, not necessarily native filesystem paths.
The platform/runtime adapter resolves runtime-visible paths and any required
working-copy synchronization. Persistent user data is separate from disposable
caches and executable/runtime storage. First-run setup and settings use the same
workflow to select/reconnect roots, validate access and prepare a runtime.
See [first-run requirements and unresolved Splore mapping](FIRST_RUN_SETUP.md).

### Input

```text
InputProvider
  actions
  devices
  bindings
  textInput
  touch/pointer availability
```

Map physical controls to semantic actions such as:

- confirm;
- cancel;
- menu;
- tool primary/secondary;
- modifier left/right;
- next/previous tab;
- run/test;
- undo/redo;
- directional navigation.

Never make core/editor logic depend on a Retroid/Anbernic key code.

### Platform lifecycle

Abstract:

- foreground/background;
- suspend/resume;
- runtime process state;
- low-memory events if relevant;
- external controller connect/disconnect.

## 8. Responsive handheld UI

PikoNest must support very different display shapes.

Examples may include:

- near-square handheld screens;
- 4:3;
- 16:9;
- taller Android displays;
- dual-screen Linux/Android devices in the future.

Avoid layouts that only work because one reference device is approximately square.

### Layout strategy

Prefer adaptive composition based on available space, for example:

- compact single-pane layout;
- wide split-pane layout;
- optional secondary-pane layout;
- future dual-screen layout.

The PICO-8 128×128 viewport is naturally square, but editor chrome should adapt around it.

## 9. Project layout

Default conceptual layout:

```text
my-game/
  my-game.p8
  .pikoos/
    project.json
    notes/
    screenshots/
    tests/
    history/
```

### `my-game.p8`

Canonical game content.

### `.pikoos/project.json`

May contain information such as:

- display title overrides;
- project UUID;
- editor state;
- multicart relationships;
- learning/help preferences;
- mechanic provenance metadata;
- test definitions;
- PikoNest extension declarations.

Do not store data in `project.json` when it is part of the actual game and belongs in the cart.

## 10. P8 parser/writer design

The current [section reader/writer proof](../experiments/p8-roundtrip/README.md)
keeps an immutable byte source plus section spans and rejects targeted edits
that alter framing. The Android lab consumes the same JDK-only implementation.
This is an experimental Java toolchain reuse, not a production language choice
or a complete Lua/resource parser.

The parser/writer must be conservative.

Requirements:

- recognize standard PICO-8 sections;
- preserve section order where practical;
- preserve unmodified raw text where practical;
- avoid reformatting all Lua just because one field changed;
- preserve comments;
- avoid destructive normalization;
- round-trip existing carts through tests;
- handle line endings safely;
- keep backups/history before risky transformations.

### Editing strategy

Where possible, retain both:

- parsed semantic representation;
- original/raw spans.

This makes targeted edits safer than regenerating the entire file from a normalized AST/data model.

## 11. Lua understanding

PikoNest does not need a full compiler to deliver its first editor, but it benefits from gradually understanding more structure.

Possible stages:

1. lexer/tokenizer;
2. symbol extraction;
3. block matching (`if/end`, functions, loops);
4. lightweight syntax tree;
5. basic diagnostics/context;
6. safe structured insertion/refactoring.

The official runtime remains the authority for execution semantics.

## 12. Mechanics representation

Android lab 0.0.53 demonstrates a bounded paired-region transition through the
shared Lua insertion catalogue: fields, code review, exact-block reopening,
undo and recovery. It is a slice of mechanics tooling, not the completed M01/M05
system. Its only game prerequisites are explicit coordinate variables and an
independent latch; hero, sprite, map, camera and genre bindings are not required.
Door/portal is an application of this recipe, not a mandatory domain object.
Non-spatial conditions, score and state workflows remain separate M02 work.
Do not model all events as movement/region entry, or treat more spatial recipes
as completion of generic game creation. See [scope and evidence](design/android-transitions-53/README.md).

A mechanic definition should conceptually contain:

```text
id
name
category
summary
learningConcepts[]
parameters[]
requirements[]
conflicts[]
preview
codeGenerator
optionalDataGenerator
explanation
```

Example:

```text
id: movement.variable_jump
parameters:
  jumpForce
  gravity
  holdFrames
  coyoteFrames
  bufferFrames
```

The generator produces normal Lua/data inserted into the project.

### Dependency behavior

If `variable_jump` needs a velocity variable and grounded detection, the tool should either:

- detect existing compatible structures;
- offer to create the missing pieces;
- clearly show what it is adding.

Do not silently generate a second incompatible player system.

## 13. Learning integration

Learning should observe meaningful application events rather than inspect everything after the fact.

Example events:

```text
VariableCreated
ConditionInserted
FunctionCreated
MechanicAdded
RawCodeEdited
PicoErrorObserved
LoopUsed
TableUsed
```

The learning service can decide whether a contextual explanation is useful.

This keeps learning logic out of individual widgets.

## 14. Editor state and undo

All creation tools should participate in a shared command/history system where practical.

Benefits:

- undo/redo across controller/touch operations;
- history visualization later;
- safer mechanic insertion;
- potential teaching explanations such as “this action changed these lines.”

Do not make every UI component mutate files directly.

Use application commands/transactions around edits.

## 15. Test/run workflow

Conceptual sequence:

```text
Editor
  │
  ├─ save/flush project safely
  │
  ├─ ask RuntimeBackend to launch cart
  │
  ▼
Official PICO-8
  │
  └─ exit
       │
       ▼
PikoNest restores previous editor context
```

Persist enough editor state that a test run does not feel like leaving the application.

## 16. Verification

`Verify in PICO-8` should mean something concrete:

- save current standard project;
- run it through the official runtime;
- present runtime errors/logs when available;
- show compatibility/resource analysis;
- distinguish PikoNest-enhanced features from standard PICO-8 support.

Do not label a project “compatible” merely because a custom preview engine accepted it.

## 17. Multicart project model

PikoNest may represent several `.p8` carts under one project.

Example:

```text
adventure/
  boot.p8
  town.p8
  forest.p8
  castle.p8
  .pikoos/
    project.json
```

`project.json` can describe human-facing structure and relationships, while the actual carts use standard PICO-8 mechanisms.

Do not hide the underlying files from advanced users.

## 18. Standard and enhanced capability model

Each project/cart can declare or be detected as one of:

### Standard

No PikoNest runtime extensions required.

### Enhanced

Uses explicit PikoNest services such as an online bridge.

Example metadata:

```text
extensions:
  - online.v1
```

The exact declaration mechanism is TBD and must not break the cart when opened in ordinary PICO-8.

## 19. Bridge architecture (experimental)

Desired conceptual shape:

```text
official PICO-8
      ↕
platform shim / bridge endpoint
      ↕
PikoNest BridgeService
      ↕
network / second handheld / services
```

Potential uses:

- online-cart communication;
- Link Play;
- achievements;
- diagnostic hooks.

This mechanism is **not yet proven** for the native official runtime path.

Keep bridge work isolated behind an interface such as:

```text
BridgeBackend
  available()
  openChannel(...)
  send(...)
  receive(...)
```

Standard projects must work when `available() == false`.

## 20. Online services architecture

If online cartridges are implemented, separate:

- PikoNest application account/services, if any;
- game-specific server/account data;
- cartridge token binding;
- routine PIN authentication.

The server must remain authoritative for persistent online state.

See `ONLINE_CARTRIDGES.md`.

## 21. Security boundary

Never treat client/cart data as trusted server authority.

For online games:

- cartridge tokens must be random/high entropy;
- tokens must be revocable;
- PIN attempts must be rate-limited server-side;
- game-critical persistent state should be validated server-side;
- a factory reset only removes local/cart binding — it does not erase the account unless explicitly requested through an account-management flow.

## 22. Data migration/versioning

Version all PikoNest-owned metadata formats from the beginning.

Examples:

```text
projectSchemaVersion
mechanicsSchemaVersion
onlineBindingVersion
```

Do not version the PICO-8 format itself; it is external.

## 23. Framework/language decision

This architecture intentionally does not freeze the UI framework yet.

The final choice should be evaluated against:

- Android support;
- Linux handheld support;
- controller/gamepad input quality;
- touch/pointer handling;
- animation performance;
- process/runtime integration;
- native bridge/shim interoperability;
- filesystem portability;
- packaging size/complexity;
- maintainability.

Whichever framework is chosen, maintain the core/platform separation described above.

## 24. First architecture proof

Before building a large UI, create small proofs for:

1. official PICO-8 launch/exit on Android;
2. `.p8` round-trip parsing and writing;
3. controller abstraction across at least two differing input layouts if available;
4. responsive square/wide editor layout concept;
5. one tiny mechanic inserted as real Lua and verified in official PICO-8.

If these work cleanly, the rest of the product has a solid base.
