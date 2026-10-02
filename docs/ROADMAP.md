# PIKOOS Roadmap

## Purpose

This roadmap is deliberately staged around technical risk and product value.

The project should not begin by building a beautiful full UI around assumptions that have not been proven. The first versions must validate the official runtime path, cartridge round-tripping and the handheld interaction model.

Version numbers are approximate planning markers, not release promises.

---

# Phase 0 — Foundations / proofs

## Goal

Prove that the core idea is technically sane before committing to a large application architecture.

## 0.0A — Official PICO-8 on Android proof

The first physical-device investigation is tracked in
[`ANDROID_RUNTIME_POC.md`](ANDROID_RUNTIME_POC.md), including its execution
plan, evidence and remaining acceptance gates.

Current proof: an [isolated Android host](../experiments/android-host/README.md)
can edit its fixture, launch official PICO-8 and restore its context after
exit/process recreation. Basic controls and sound have owner confirmation.
Warm restart and production runtime ownership remain unresolved; 0.0A is not
complete. The single-field fixture editor exists only to exercise the runtime
handoff and is not the product editor.

Build the smallest possible Android proof that can:

- import/select the user's official ARM64/Raspberry Pi PICO-8 files;
- validate that the expected binary/resources are present;
- launch official PICO-8 through a Linux/proot-style environment;
- launch a specified `.p8` directly;
- receive handheld controls correctly;
- produce working video/audio;
- exit cleanly back to the host application.

Test on at least one real Android handheld as early as possible.

### Do not add yet

- library UI;
- project editor;
- online systems;
- mechanics;
- tutorials.

The only question is: **can PIKOOS reliably treat official PICO-8 as its runtime on Android?**

## 0.0B — `.p8` round-trip proof

The [portable section reader/writer experiment](../experiments/p8-roundtrip/README.md)
now passes byte-preservation tests over 12 official 0.2.7 demos, both owned
fixtures and synthetic edge cases. It also backs the Android lab's speed edit.
This establishes a first framing/targeted-edit proof; resource decoding, Lua
understanding and broader import compatibility remain open.

Create a parser/writer test tool.

Requirements:

- open real `.p8` files;
- identify all standard sections;
- save without corruption;
- preserve unchanged carts as closely as possible;
- build an automated corpus of round-trip tests.

Success target:

> Opening and saving a cart without edits should not damage or semantically alter it.

## 0.0C — Platform abstraction skeleton

Before product code grows, define interfaces for:

- runtime;
- storage;
- input;
- lifecycle/platform services.

Android gets the first implementation.

Create placeholders/tests that demonstrate the core does not depend directly on Android APIs.

## 0.0D — Handheld UI interaction proof

The owner accepted the [interactive UX study](../experiments/ux-study/README.md)
as the visual baseline and emphasized controller-first operation. The native
Android proof now implements workshop/code/sprite tools for one owned cart:
real byte-scoped changes, semantic input, explicit commit/cancel, undo and
runtime launch. Physical ergonomics, arbitrary cart editing, the library and
production framework are still open. See [device results](ANDROID_RUNTIME_POC.md).

Build a tiny mock editor screen to test:

- controller focus/navigation;
- touch selection;
- responsive layouts;
- square and widescreen displays;
- on-screen text input;
- shoulder-button modifiers;
- fast `Test` shortcut.

Do not over-design visuals yet. Validate ergonomics.

---

# 0.1 — PIKOOS Shell

## Goal

A useful launcher around the official runtime.

Features:

- first-run runtime import;
- runtime validation/status;
- local cart import;
- basic cartridge library;
- cover/screenshot metadata where available;
- Play;
- Favorites;
- Last played;
- launch Splore if practical through the imported runtime;
- clean return from PICO-8 to PIKOOS;
- input mapping/settings;
- adaptive layout for multiple handheld aspect ratios.

### Definition of done

A user can install PIKOOS, provide official PICO-8, import carts and comfortably use the device as a PICO-8 handheld without touching a terminal or filesystem manager.

---

# 0.2 — First Creator

## Goal

Make and edit a small real PICO-8 game entirely on a handheld.

Current experimental slice: the owned Moon Garden project has a controller
sprite sheet, blank-image entry, safe copy and explicit hero assignment. This
connects resource selection to editing and official-runtime testing; it does
not complete project creation or arbitrary cartridge editing.

Features:

- project creation;
- blank cart;
- one or two simple starting points;
- Code editor v1;
- Sprite editor v1;
- Map editor v1;
- Project screen;
- Save;
- Test in official PICO-8;
- return to previous editor context after test;
- basic undo/redo;
- standard cartridge budget display;
- project metadata under `.pikoos/`;
- open/import existing `.p8` as project.

### Code editor v1

Sprite/map tool planning must also account for the owner-confirmed future
requirements for frame animation, background layers, parallax, independent
scrolling and repeating backgrounds. See the detailed interaction and
compatibility requirements in [UX direction](UX_DIRECTION.md). These are not
implemented by the current sprite sheet; release placement remains TBD.

Focus on keyboard-less usability:

- controller cursor movement;
- touch cursor placement where available;
- syntax highlighting;
- Lua/PICO-8 keyword suggestions;
- PICO-8 API suggestions;
- project symbol suggestions;
- structural insertion for common blocks;
- on-screen keyboard when free text is necessary;
- external keyboard support when present.

### Definition of done

A user can build and test a tiny game that remains a normal `.p8` and can be opened in official PICO-8 elsewhere.

---

# 0.3 — Mechanics + Learn by Doing

Owner discussion (2026-10-02): later design genre-specific starting points and
tools as complete creation journeys. Explore, for example, platformers,
top-down adventures, shooters and puzzles after the shared resource/create/test
workflow is usable. These are research examples, not an accepted genre list or
separate engines. Keep mechanics ordinary editable PICO-8 Lua/data.

## Goal

Make PIKOOS genuinely different from a small-screen code editor.

## Mechanics Library v1

Start with a deliberately small set of excellent mechanics, for example:

- 4-way movement;
- 8-way movement;
- platformer movement;
- jump;
- variable jump;
- coyote time;
- jump buffer;
- simple projectile;
- health/damage;
- patrol enemy;
- chase enemy;
- room transition;
- basic dialogue;
- screen shake.

Each mechanic should include:

- friendly setup UI;
- configurable parameters;
- preview if practical;
- generated ordinary Lua/data;
- explanation of generated code;
- learning concepts.

## Learning system v1

Features:

- contextual first-time explanations;
- `How does this work?` on mechanics/code;
- tiny optional challenges;
- concept tracking;
- simple Skill Book;
- contextual and independently browsable PICO-8 API reference;
- optional task-based guides, from a first game to animation, maps and sound;
- controller-first reading and return to the original editor context;
- friendly translation of a small set of common errors;
- raw technical error always available.

Reference and guide requirements, offline direction, version checks and the
initial source catalogue are recorded in
[PICO-8 learning resources](PICO8_LEARNING_RESOURCES.md). These are planned
capabilities; the current workshop does not yet provide this library.

### Definition of done

A beginner can create a recognizable tiny game mainly by manipulating understandable mechanics, while gradually seeing and changing the real Lua underneath.

---

# 0.4 — Sound, Remix and Better Projects

## Goal

Complete the core creative loop.

Features:

- SFX editor v1: creation from scratch, editable presets, note parameters,
  phrase speed/loop, audition and undo with controller input;
- Music editor v1: note/phrase editing, channel parts, pattern sequencing and
  loops, with shared-resource awareness and official-runtime sound checks;
- stronger project screen;
- screenshots;
- notes/TODO;
- project duplication;
- Remix workflow;
- `Peek Inside` for library carts;
- easy remix suggestions such as:
  - change player sprite;
  - change speed;
  - change music;
  - add a mechanic;
- improved history/undo;
- optional simple version snapshots.

### Definition of done

A player can discover a cart locally, inspect it, make a remix, understand some of its construction and turn the remix into their own project.

Sound acceptance also requires creating an effect and a short looping piece
with a controller, testing both together in official PICO-8 and returning to
the selected editing step. See [UX direction](UX_DIRECTION.md) for the planned
sound workflows, preservation rules and unresolved preview implementation.

---

# 0.5 — Larger Projects / Multicart / Debugging

## Goal

Make PIKOOS useful beyond tiny first projects.

Features:

- multicart project model;
- beginner-facing `Add Chapter/Region/Dungeon` workflows;
- explicit view of underlying carts for advanced users;
- shared project metadata;
- better token/resource analysis;
- compatibility status;
- lightweight symbol browser;
- jump-to-definition where practical;
- basic variable watch/log tools if runtime integration permits;
- room/test launch shortcuts;
- bug/test capture experiments;
- input replay experiment.

### Definition of done

The user can manage a moderately complex PICO-8 project without the project structure becoming confusing on a handheld.

---

# 0.6 — Discover / Community integration

## Goal

Make the play → inspect → remix loop feel natural.

Potential features:

- richer discovery/catalog integration;
- author/title/tag metadata;
- local collections;
- source/remix actions;
- share/export workflows;
- clean project/cart packaging;
- maybe community mechanic packs later, but only if compatibility and trust are handled cleanly.

External integrations must be implemented only after checking current APIs/terms and should not become required for offline use.

---

# 0.7 — Link Play prototype

## Goal

Test the idea of transparent handheld-to-handheld multiplayer for ordinary local multiplayer carts.

Research tasks:

- determine what runtime/input interception is possible without modifying official PICO-8;
- transport remote player input;
- frame/timing model;
- desync behavior;
- latency measurement;
- local discovery/pairing UX.

Desired user experience:

```text
PLAY
  → LINK PLAY
  → HOST / JOIN
```

The cart should ideally continue to behave as an ordinary local multiplayer PICO-8 cart.

### Important

Do not commit the core architecture to this feature until the prototype proves it is practical.

---

# 0.8 — Online Bridge prototype

## Goal

Prove that an official PICO-8 runtime launched by PIKOOS can exchange small structured messages with a host bridge/server without modifying the proprietary PICO-8 binary.

Prototype requirements:

- capability detection;
- small bidirectional message channel;
- graceful no-bridge behavior;
- one tiny demo cart;
- one simple server;
- latency/bandwidth measurements;
- robust process/lifecycle behavior.

Do not build an MMO first.

A trivial online shared object/room is enough to prove the transport.

---

# 0.9 — Personalized Online Cartridge prototype

## Goal

Prove the unusual cartridge-binding model.

Features/tests:

- clean online cart;
- reserved binding block;
- server-created high-entropy token;
- token written into the actual `.p8` copy;
- server-side PIN authentication;
- personalized launch;
- Factory Reset Cartridge;
- Share Clean Cartridge;
- token revocation;
- move/copy personal cart between devices;
- official PICO-8 still opens/runs the cart without corruption;
- bridge-unavailable behavior.

### Definition of done

A clean cart can genuinely become a personal cart, then be reset to a clean cart again without damaging ordinary PICO-8 compatibility.

---

# 1.0 — Android release target

## Goal

A coherent, polished version of the core PIKOOS experience on Android handhelds.

Must-have qualities:

- stable official-runtime installation/launch;
- strong local library;
- responsive controller-first UX;
- useful touch support;
- creator with Code/Sprites/Map/Sound;
- several polished Mechanics Library categories;
- contextual learning;
- Remix;
- compatibility verification;
- good project management;
- reliable import/export;
- friendly onboarding;
- settings/input remapping;
- recovery/backups for user projects;
- no dependency on one Android handheld model.

Experimental Link/Online features may remain clearly marked beta if they are not yet mature.

---

# 1.x — Linux handheld port

## Goal

Bring the same PIKOOS core and product experience to Linux handhelds.

Reference future targets may include devices in the RG DS class and other ARM Linux gaming handhelds.

Expected work:

- Linux runtime backend;
- Linux filesystem/storage adapter;
- controller/input backend;
- packaging/update path;
- compositor/window/fullscreen behavior;
- suspend/resume;
- optional dual-screen adaptations where relevant;
- device-profile quirks only at the adapter/config level.

The following should **not** need to be rewritten:

- P8 parser/writer;
- project model;
- mechanics library;
- learning engine;
- cartridge library model;
- most UI/application logic;
- compatibility analysis.

If the Linux port requires rewriting those areas, the earlier architecture has failed its portability goal.

---

# Later ideas — not commitments

These are intentionally outside the near-term core:

- community mechanics library;
- collaborative project sharing;
- cloud project sync;
- richer achievements;
- advanced debugger;
- profiler overlays;
- full persistent online worlds;
- online trading/social spaces;
- public PIKOOS game services;
- specialized dual-screen editing layouts;
- desktop companion/editor mode.

They should not distract from making the handheld creation loop excellent.

---

# Development priority rule

When choosing between two features, prefer the one that improves this loop:

> **make a tiny change → test it immediately → understand what happened → want to change something else**

That loop is the heart of PIKOOS.
