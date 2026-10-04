# AGENTS.md — PikoNest contributor instructions

This repository contains a working Android lab and product/architecture documents;
it is not yet a complete creator. Treat this file as the primary implementation
brief for Codex and other AI coding agents.

Current planning entry points: `docs/STATUS.md` records demonstrated scope,
`docs/ROADMAP.md` orders complete user workflows, `docs/BACKLOG.md` owns task IDs
and status, and `docs/ACCEPTANCE.md` defines end-to-end acceptance. Historical lab
version notes are evidence, not current task order or completed-product claims.

Owner naming decision (2026-10-04): the product and repository are **PikoNest**.
Technical identifiers will be migrated later with data preservation. Keep existing
package IDs, `.pikoos/` metadata and integration strings until that coordinated
migration; see `docs/PROJECT_NAME.md`. Historical screenshots retain their original
branding, and documented installed-app labels must remain accurate.

## 1. Product in one sentence

PikoNest is a **cute, handheld-first PICO-8 workshop** for playing, creating, remixing, learning, testing and eventually sharing PICO-8 games, using the user's **official PICO-8 runtime** as the authoritative execution engine.

The intended emotional experience is closer to a creative toy than to a desktop IDE.

## 2. Platform strategy

### Android first

The first production target is Android gaming handhelds with physical controls.

Owner clarification (2026-10-03): the current deliverable is an installable Android
APK application. Linux handhelds are occupied by TrainerOS work; Linux porting and
device validation are deferred and must not block Android. Keep portable boundaries.

Owner release gate (2026-10-03): publish an APK as a GitHub Release only after
the owner explicitly confirms they are 100% satisfied with the result and approves
that release. Continuing development, passing tests, installing local test APKs,
or pushing source commits is not release approval. Keep development builds local;
do not create/upload a GitHub APK release or prerelease before that confirmation.

Support both the PikoNest Play shelf and game launch from third-party Android
launchers through PikoNest. External play must not require editor/project import.
Adaptive screen/controller support and external-launch acceptance are part of the
Android release scope; see `docs/ANDROID_APP.md` for requirements and pending design.

Do **not** architect PikoNest around the Retroid Pocket Classic or any other single device. Devices will vary in:

- display aspect ratio and resolution;
- touch availability;
- D-pad / stick / button layouts;
- shoulder buttons;
- Android version;
- storage model;
- chipset and GPU;
- whether they expose keyboard/mouse peripherals.

Retroid Pocket Classic is an excellent design/reference target, not a platform contract.

### Linux later

A Linux port is a planned product direction, including ARM handhelds such as RG DS-class devices.

Anything that is not inherently platform-specific should live outside Android-specific code. The editor, project model, mechanics library, teaching system, PICO-8 parser/writer, cartridge library model and UI state should be portable.

Use a replaceable platform/runtime boundary from the beginning.

## 3. Non-negotiable compatibility rules

1. **`.p8` is the canonical editable project format.**
2. Standard PikoNest projects must remain valid ordinary PICO-8 projects.
3. Do not invent custom Lua syntax for normal projects.
4. Do not silently depend on a custom runtime API in normal projects.
5. Mechanics/templates must emit ordinary PICO-8 Lua/data.
6. The user-supplied official PICO-8 runtime is the authoritative compatibility test.
7. A project that passes normal verification should continue to run when opened outside PikoNest in ordinary PICO-8.
8. Optional PikoNest extensions (network bridge, achievements, etc.) must be explicit and separable from standard compatibility.
9. PikoNest must never bundle proprietary PICO-8 binaries. The user imports their own purchased runtime.
10. **The base product follows the real PICO-8 rules.** Its resource model,
    editing operations, generated code and validation must respect the official
    PICO-8 semantics and limits for the targeted runtime version.

### Real console rules vs prototype scope

Owner-confirmed requirement (2026-10-02): design the base around PICO-8 itself.

- Before designing or extending a tool, check the relevant official manual/API,
  resource layout and limits. Verify implemented behavior with focused data tests
  and the user's official runtime where applicable; document remaining uncertainty.
- Do not turn a temporary PikoNest restriction into a claimed PICO-8 restriction
  or a permanent domain-model invariant. For example, the experiment's eight
  16×16 images are a bounded editor slice, not PICO-8's sprite size or capacity.
  Larger and rectangular images must fit the standard sprite-sheet model.
- Partial implementation is allowed. Identify unsupported editing operations
  as PikoNest limitations, preserve their source data, and refuse unsafe edits.
  A valid PICO-8 feature missing from our editor is not an incompatible cartridge.
- Do not silently expand or shrink console limits, change API behavior, or invent
  a different graphics, map, audio or execution model for standard projects.
- Improve the controller-first interface freely within that contract. Friendly
  objects, templates and tools must map to ordinary PICO-8 Lua/data; matching the
  original desktop editor's layout or keyboard workflow is not required.
- PikoNest-only behavior remains an explicit, separable extension, never an
  accidental requirement of the base creation workflow.

See `docs/PICO8_COMPATIBILITY.md`.

## 4. UX principles

### Cute, not childish

The UI should feel warm, playful and alive, but it must never talk down to the user.

Good:

- small celebratory reactions;
- clear icons and large actions;
- gentle animation;
- plain-language explanations;
- friendly error handling;
- visual cartridge/project metaphors;
- immediate feedback.

Bad:

- baby-talk;
- excessive mascots/dialogue blocking work;
- fake gamification that slows down experienced users;
- hiding real errors forever;
- turning every action into a tutorial.

### Simplicity before completeness

A beginner should be able to create something playable quickly.

Prefer progressive disclosure. Advanced controls should appear when useful rather than occupying the first screen.

### Tools are independent of templates and genres

All general tools must be available in every project, including blank and
imported cartridges. A template supplies initial content and configured mechanics;
it must not enable, hide or lock whole tools by genre or by the presence of a hero.
This applies to sprites, animation, maps/backgrounds, code, SFX/music, effects,
particles, asset reuse and the mechanics catalogue. Genre filters are suggestions,
not capability restrictions. Context may require selecting a resource, object,
event or compatible code binding; explain that prerequisite and offer its setup.
Never invent a hero or silently rewrite unfamiliar Lua to satisfy an action.
Keep technical limitations of an unfinished tool explicit and separate from this
product rule. A template-only parameter adapter is not the general tool itself.

### Controller-first

Owner clarification (2026-10-03): everyday play is a primary use case. The
default Play library must support attractive cartridge browsing, recent launches,
favorites and direct controller launch/return. Workshop is a separate entrance.
Playing must not require editor import, project creation, remixing or a tutorial.
Build this workflow early, alongside the runtime/storage foundations.

Important actions must be possible using handheld controls. Touch should make drawing, selection and editing better but should not become a hard architectural dependency.

Input must use abstract actions, not hard-coded Android key codes or one device layout.

### Keyboard is optional

The product exists partly because these handhelds often lack keyboards.

Code editing should therefore emphasize:

- autocomplete;
- known PICO-8 API symbols;
- existing project symbols;
- structural insertions (`if`, `for`, `function`, etc.);
- parameter editors;
- mechanics insertion;
- controller navigation;
- contextual touch keyboard only when free text is actually required.

External Bluetooth/USB keyboards may be supported, but never make them the required primary workflow.

## 5. Learning is part of creation

PikoNest should teach Lua/PICO-8 **through real work**, not by forcing a course before creation.

Pattern:

1. user wants to do something;
2. PikoNest helps them do it;
3. PikoNest reveals the real Lua/PICO-8 concept behind it;
4. user can inspect and edit the generated code;
5. a tiny optional challenge can reinforce the idea.

Example:

- user adds player health;
- PikoNest introduces `hp` as a variable;
- it briefly explains what a variable is;
- user changes the value and immediately sees the result.

Keep a lightweight skill/progress model based on concepts the user has genuinely encountered, not quiz scores.

See `docs/PRODUCT_VISION.md`.

## 6. Mechanics library

The mechanics library is a major product feature.

A mechanic is a compact reusable implementation of a game behavior, for example:

- 4/8-way movement;
- platformer movement;
- variable jump;
- coyote time;
- jump buffering;
- dash;
- wall jump;
- health/damage;
- melee/projectiles;
- patrol/chase enemies;
- camera follow;
- room transitions;
- dialogue;
- particles;
- screen shake.

Mechanics should be:

- our own concise implementations inspired by common/interesting PICO-8 patterns;
- parameterized through a friendly editor;
- previewable where practical;
- inserted as ordinary Lua/data;
- editable after insertion;
- understandable by a learner.

Do not create a closed visual language or opaque runtime component system.

## 7. Project model

### Genre-neutral core; no mandatory hero

Owner clarification (2026-10-03): games may contain enemies, items, environment
pieces, puzzle pieces, boards or UI elements, and may have no hero at all.
Creation, resource editing, library reuse and test/run must not require a hero,
player controller, movement/jump fields, collision body or sprite assignment.

A sprite is a graphics resource, not a game object or a mandatory character.
Its uses can be multiple and optional. Gameplay roles and behavior belong to
the particular game/template/mechanic; do not impose a universal entity/component
system, scene graph or fixed hero/enemy/environment taxonomy on ordinary carts.
Offer role-specific actions only for a known applicable binding. Generic tools
must also work for hero-free puzzles and falling-block games.

Android lab 0.0.9 keeps Moon Garden's speed/jump/hero data behind the optional
`MoonGardenBinding` adapter. Shared cart/resource workflows also accept carts
without it, including the blank and Lights samples. `HeroCode` and `HeroBinding`
remain platformer-specific; do not turn them into a universal object model.
Android lab 0.0.13 adds single text `.p8` import with byte-preserving copies.
PNG carts, linked-file import, full Lua editing and general resource allocation remain open.

A normal project should conceptually look like:

```text
project/
  game.p8
  .pikoos/
    project.json
    notes/
    screenshots/
    tests/
    history/
```

`game.p8` is the source of truth for the PICO-8 game.

`.pikoos/` is optional editor/workshop metadata and must not be required by ordinary PICO-8.

When importing an existing cart, preserve data as safely as possible. Prefer lossless round-tripping over aggressive normalization.

Do not rewrite unrelated code/data merely because a file was opened and saved.

## 8. Runtime architecture

Do not couple UI/editor code directly to Android process-launch details.

Design around an interface conceptually similar to:

```text
PicoRuntimeBackend
  detect_runtime()
  import_runtime(...)
  validate_runtime()
  launch_cart(cart, options)
  stop()
  capabilities()
```

Likely first backend:

- Android;
- user imports official ARM64/Raspberry Pi PICO-8 build;
- a small Linux/proot-style compatibility layer launches it;
- Android-specific video/audio/input/interoperation belongs behind the backend/platform layer.

Future backend:

- Linux handheld;
- launch a compatible user-provided official PICO-8 build more directly.

Do not assume the first Android implementation is the permanent architecture.

## 9. Standard vs extended cartridges

PikoNest has two conceptual capability levels.

### Standard cartridge

- ordinary PICO-8;
- no PikoNest-specific runtime dependency;
- must verify in official PICO-8;
- preferred/default project type.

### PikoNest-enhanced cartridge

May optionally use PikoNest bridge features such as:

- network services;
- Link Play infrastructure;
- achievements/integration hooks;
- personalized online cartridge account data.

The file should still remain a valid `.p8`, but enhanced behavior may only function inside PikoNest.

Never make enhanced behavior an accidental dependency of standard projects.

## 10. Personalized online cartridges

The intended online-cart model is unusual and deliberate.

After account/character creation, the server returns a high-entropy **cartridge token**. PikoNest writes that token into a reserved data area of that user's personalized cart.

The exact encoding/location inside a standard `.p8` is **not decided yet**. Do not prematurely lock it to gfx/map/code comments until a round-trip-safe design has been tested.

Authentication concept:

```text
personal cart token + user PIN -> server authentication
```

The PIN is checked server-side and rate-limited. A copied token alone should not be sufficient to log in.

The user's account password remains for account creation/recovery/management rather than routine launches.

Required UX:

- bind a clean cart to an account;
- request PIN on launch;
- `Share Clean Cartridge` creates/exports a copy with account binding removed;
- `Factory Reset Cartridge` removes the token/binding and returns the cart to an unbound state;
- server-side token revocation must be possible.

See `docs/ONLINE_CARTRIDGES.md`.

## 11. Multicart

Use normal PICO-8 multicart mechanisms rather than inventing a different game packaging model.

PikoNest should hide unnecessary complexity for beginners. The UI may present multiple carts as:

- regions;
- chapters;
- dungeons;
- scenes;

while retaining standard underlying PICO-8 behavior.

## 12. Keep technical facts separate from experiments

Some ideas are confirmed product principles; others are research tasks.

Confirmed principles include:

- official runtime supplied by user;
- Android first / Linux later;
- `.p8` compatibility;
- playful learning;
- mechanics library;
- controller-first UI.

Research/experimental areas include:

- exact Android official-runtime wrapper implementation;
- native bridge between a running official PICO-8 process and PikoNest;
- transparent Link Play;
- online-cart transport;
- exact reserved token encoding inside `.p8`;
- debugging/state capture possibilities.

When implementing experiments, keep them isolated. Do not reshape the entire core around an unproven trick.

## 13. Architecture preference

Favor clear layers:

```text
UI / interaction
      ↓
Application workflows
      ↓
Portable domain/core
      ├─ P8 project model
      ├─ parser/writer
      ├─ mechanics
      ├─ learning model
      ├─ library/project metadata
      └─ compatibility/budget analysis
      ↓
Ports/interfaces
      ├─ runtime
      ├─ filesystem/storage
      ├─ input
      ├─ network/bridge
      └─ platform services
      ↓
Android adapters now / Linux adapters later
```

The exact framework/language for the app has not been frozen by this document. Do not make a major framework decision solely because it is convenient for a first proof of concept; document the tradeoff if/when such a decision is made.

## 14. Documentation discipline

Owner workflow clarification (2026-10-04): batch development into complete user
scenarios with several related operations. Do not end every tiny tool change with
a full build/install/screenshot cycle. Use focused core checks during implementation,
then build, install and capture evidence once for the completed batch. Repeat only
when a failure, new change or unresolved concern requires it. Preserve meaningful
verification and data safety without repetitive checks.

### Asset library, first-run setup and terminology

Owner requirements: reusable user-created and extracted cartridge assets belong
in a persistent cross-project library; first-run setup chooses existing games,
Splore downloads, own projects and library/data locations, then imports and
automatically prepares the user's correct official PICO-8 archive.
See `docs/ASSET_LIBRARY.md` and `docs/FIRST_RUN_SETUP.md` for scope, dependencies,
storage ownership, backend-specific archive guidance and acceptance gates.
These are required product workflows. Android lab 0.0.11 implements a bounded
sprite-only cross-project library with independent pixels and explicit insertion.
Lab 0.0.13 also imports single text `.p8` files for resource extraction.
Lab 0.0.14 exports a selected saved cart byte-for-byte to a new external `.p8`,
with readback verification. This is not folder setup, dependency/library backup,
or Share Clean Cartridge; raw export preserves any source bindings unchanged.
Other asset types, `.p8.png`/linked-file import and first-run folder/runtime setup remain
unfinished. App-private experimental storage is not the final user-folder model.

Use accurate user-facing names: sprite, animation frame, tile, map, background,
SFX and music. Do not call every visual resource a generic "drawing". Background
describes a role; retain its actual sprite/map/layer representation underneath.
Library insertion must materialize ordinary PICO-8 data/code in the target cart;
standard carts must not require the external PikoNest asset library at runtime.

When a major decision is made:

- update the relevant file in `docs/`;
- distinguish accepted direction from experiment/TBD;
- do not silently contradict these project invariants;
- if a requested implementation would break PICO-8 compatibility or platform portability, flag that explicitly before baking it in.

## 15. Near-term priority order

The foundation-first list below is the original bootstrap order. The launch,
round-trip and bounded controller experiments now exist. Follow the audited
completion path in `docs/ROADMAP.md`: the next creator gap is general Lua editing
and a real blank-cart creation loop (A01/A02 → C01/C02), alongside the remaining
runtime/storage work. Do not keep extending launcher polish or template-specific
controls as a substitute for missing general tools. Close tasks with explicit
scope/evidence, and keep owner acceptance separate from technical test results.

Until the foundations are proven, prioritize:

1. official PICO-8 runtime launch POC on Android;
2. robust `.p8` parse/write round-trip tests;
3. portable project/core model;
4. controller/touch input abstraction;
5. minimal library + launch shell;
6. minimal create/edit/test loop;
7. mechanics + contextual learning;
8. polish, online and advanced experiments later.

See `docs/ROADMAP.md` for more detail.
