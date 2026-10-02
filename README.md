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

Android 0.0.10 adds sprite/rectangle copying in every project, with explicit
placement, replacement preview, cancel and exact undo. The product rule requires
general tools to remain available independently of genre/template.
[Device evidence](docs/design/android-copy-09/README.md).

Android 0.0.9 separates Moon Garden bindings from basic cart/resource editing.
New projects can also start with a blank cart or the hero-free Lights puzzle;
their workshop offers sprite editing and Lua viewing without character controls.
See [device/runtime evidence and limitations](docs/design/android-herofree-08/README.md).

Android 0.0.8 uses precise sprite terminology in the existing editor; see
[device capture](docs/design/android-terminology-07/README.md). The reusable asset
library and first-run folder/runtime wizard are accepted requirements for future
implementation, described in the documents above.

The owner accepted the [PICO-8-style visual baseline](docs/UX_DIRECTION.md)
after rejecting the original Android form, and authorized its implementation
with controller-first interaction. The installed
[Android workshop experiment](experiments/android-host/README.md) now edits
real speed/jump parameters and sprite pixels in an ordinary `.p8`, with
controller navigation, explicit commit/cancel, undo and runtime launch.
Its sprite sheet now supports choosing, drawing a new image, safely copying
an existing image and explicitly assigning a different hero, using the same
controller workflow. See [device captures](docs/design/android-sprites-02/README.md).
Drawing now includes a controller-operated tool chooser, connected fill,
line preview/confirmation and a picker, with whole-operation undo. See
[drawing captures](docs/design/android-drawing-04/README.md).
Rectangular sheet areas now support images beyond 16×16, the same drawing tools
and cursor-following zoom. The current selector covers the upper half of the
standard sheet; shared-map editing remains a next step.
See [region editing and runtime evidence](docs/design/android-regions-05/README.md).
Large images can now be assigned as the owned template's hero after previewing
their collision rectangle. Image placement, platform landing and screen bounds
use the same saved body dimensions in ordinary Lua, with one-step undo.
See [hero assignment and physics checks](docs/design/android-hero-06/README.md).
The native project shelf now opens separate games, creates a fresh small-game
template and makes independent copies, retaining each project's editing context.
See [library device captures](docs/design/android-library-03/README.md).
The [interactive study](experiments/ux-study/README.md) remains the visual
baseline. Physical ergonomics and the complete product remain unaccepted.

Owner-provided PICO-8 0.2.7 archives for private development are stored in
[`dev-runtime/`](dev-runtime/README.md). They must stay outside application
packages and public releases; end users still import their own runtime.

Early foundation experiments. An installed Android host has demonstrated
editing a real `.p8`, launching it in official PICO-8 and restoring its saved
context after exit, including host process recreation. See the
[device results and remaining limitations](docs/ANDROID_RUNTIME_POC.md) and
[reproducible host experiment](experiments/android-host/README.md).

A [portable `.p8` reader/writer proof](experiments/p8-roundtrip/README.md)
now preserves complete source bytes and supports section-scoped edits, tested
against the official demo corpus and used by the Android lab.

The production framework remains undecided. The first milestone is proving
three foundations:

1. reliably launching the user's official PICO-8 runtime from Android and returning to PIKOOS;
2. loss-safe parsing and writing of real `.p8` projects;
3. a portable core that does not bake in Android or one handheld model.

---

PIKOOS is an independent project and is not affiliated with Lexaloffle Games. PICO-8 must be obtained separately from its official publisher.
