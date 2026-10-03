# PIKOOS

**A cute, handheld-first PICO-8 workshop for playing, making, learning, remixing, and sharing games.**

PIKOOS is an Android-first shell and creation environment built around the **official PICO-8 runtime**. It is designed for handheld gaming devices with physical controls, with a future Linux port planned for devices such as the RG DS and other ARM handhelds.

PIKOOS is not a replacement fantasy console and not merely a launcher. The goal is a complete, playful loop:

> **Play → Peek Inside → Remix → Create → Learn → Test → Share**

A beginner should be able to make something move almost immediately, then gradually discover that they have been learning real Lua and real PICO-8 along the way.

## Core principles

- **Real PICO-8 first.** Standard `.p8` cartridges remain the canonical project format.
- **Official runtime.** The user supplies their own official PICO-8 installation; PIKOOS does not bundle it.
- **Cute and simple.** The UI should feel like a friendly creative toy, not a desktop IDE squeezed onto a handheld.
- **Learn by doing.** Lua/PICO-8 concepts are taught contextually while the user creates and modifies games.
- **Controller-first.** Everything important should be usable with handheld controls; touch is an enhancement, not a hard dependency.
- **Portable architecture.** Android is the first target, but core logic must not depend on Android or on any one device.
- **Tools for every game.** Templates supply initial content; they do not restrict tools by genre or require a hero.
- **No custom PICO-8 dialect.** Standard projects should remain understandable and runnable in ordinary PICO-8.

## What PIKOOS aims to include

- Cartridge library and launcher
- Reusable asset library across projects and chapters
- First-run folder selection and automatic preparation of the user's runtime
- Import of the user's official PICO-8 runtime
- Source viewing and remixing
- Handheld-friendly code, sprite, map, SFX and music tools
- Smart Lua/PICO-8 code input without requiring a physical keyboard
- A library of reusable game mechanics that produces ordinary Lua code
- Contextual learning and a lightweight Lua/PICO-8 skill book
- Fast edit → test → edit workflow
- Cartridge budget/limit visualization
- Multicart project support presented as one game/project
- Debug/test helpers
- Local Link Play experiments
- Optional PIKOOS online extensions and personalized online cartridges

## Platforms

### First target: Android handhelds

PIKOOS should run on Android gaming handhelds regardless of brand, screen aspect ratio, or exact control layout. Retroid Pocket Classic is a particularly interesting device because of its near-square display, but **it is not the architectural target**.

The current runtime direction is to launch the user's official ARM64/Raspberry Pi PICO-8 build inside Android through a small Linux/proot-style compatibility layer, similar in spirit to existing community wrappers.

### Later: Linux handhelds

The platform/runtime layer must be replaceable so that PIKOOS can later run natively on Linux handhelds such as the RG DS and other ARM Linux devices without rewriting the editor, project model, learning system, mechanics library, or UI logic.

## Project format

The cartridge remains the source of truth:

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

`.pikoos/` contains optional workshop metadata. Removing it must not prevent the standard cartridge from opening in normal PICO-8.

## Documentation

- [`AGENTS.md`](AGENTS.md) — rules and context for Codex/AI contributors
- [`docs/PRODUCT_VISION.md`](docs/PRODUCT_VISION.md) — product and UX vision
- [`docs/PICO8_LEARNING_RESOURCES.md`](docs/PICO8_LEARNING_RESOURCES.md) — PICO-8 reference, guide requirements and source catalogue
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — technical boundaries and proposed architecture
- [`docs/ASSET_LIBRARY.md`](docs/ASSET_LIBRARY.md) — reusable sprites, backgrounds, SFX and music
- [`docs/VISUAL_EFFECTS.md`](docs/VISUAL_EFFECTS.md) — particle/VFX research and proposed tool requirements
- [`docs/FIRST_RUN_SETUP.md`](docs/FIRST_RUN_SETUP.md) — folders, archive choice and automatic runtime preparation
- [`docs/PICO8_COMPATIBILITY.md`](docs/PICO8_COMPATIBILITY.md) — compatibility invariants
- [`docs/ONLINE_CARTRIDGES.md`](docs/ONLINE_CARTRIDGES.md) — personalized online-cart concept
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — staged implementation plan

## Status

As of 2026-10-03, host 0.0.33 / Runtime Test 4 is a working Android lab, not a
complete game creator. It demonstrates Play/external launch, bounded linked-cart
launch, sprite editing/reuse, controller Lua drafts, parameterized insertions and name/API choices, single `.p8` import/export, session undo/redo and an
isolated test of the user's purchased runtime archive.

Structural/API-assisted Lua editing, map/animation/background tools, SFX/music, reusable mechanics,
complete project recovery, production setup and the full creation loop remain open.
The next creator milestone is a real game authored from a blank cart through
PIKOOS with handheld controls, then tested and exported as ordinary PICO-8.

- [Current capability audit](docs/STATUS.md)
- [Whole-product roadmap](docs/ROADMAP.md#completion-path)
- [Task backlog and dependencies](docs/BACKLOG.md)
- [End-to-end acceptance](docs/ACCEPTANCE.md)
- [Historical roadmap and lab milestones](docs/ROADMAP_HISTORY.md)
- [Reproducible Android host and slice history](experiments/android-host/README.md)
- [Latest editor device evidence](docs/design/android-symbols-33/README.md)

The owner approved the PICO-8 visual direction, not the completeness of the
application. APK GitHub releases/prereleases require explicit confirmation that
the owner is fully satisfied and authorizes release. Development APKs stay local.
Linux and optional online research do not block the local Android product.

The [bounded editor decision](docs/EDITOR_FOUNDATION.md) retains Java/Canvas for
the next creator slices and puts editing state/operations in portable core.
Production wrapper delivery and the future Linux UI remain explicit decisions.

Owner-provided PICO-8 archives in [dev-runtime](dev-runtime/README.md) are private
development inputs; they must never enter application packages or public releases.

---

PIKOOS is an independent project and is not affiliated with Lexaloffle Games. PICO-8 must be obtained separately from its official publisher.
