# PikoNest: current scope and development map

**Lab 0.0.72 · Android first · No public APK release.** The complete creator is
still in development. Original code uses the [MIT License](../LICENSE).

## What exists today

| Area | Demonstrated scope | Still open |
| --- | --- | --- |
| Play/projects | Local library, favorites/recent games, shared English shelves, safe copy | Settings migration, long text and broader device acceptance |
| Sprites/maps | Pixel/region operations, map brushes, region review and sprite reuse | Shared-memory painting, allocation and complete cross-tool workflows |
| Animation/camera | Shared English forms, create/reopen/reorder and draft Test for recognized uses | Event bindings and wider lifecycle acceptance |
| Backgrounds | Sprite-strip layers through the common Workshop path, ordering, parameters and recovery | Broader layer representations and complete scene workflows |
| Rules/code | Numeric state, input/conditions, add/set/reset, readouts and sprite/map X/Y bindings; a UI-authored Blank game tested in official PICO-8 and exported | Animation/audio events, broader bindings and complete ACC-01 with physical controls and independent export execution |
| Storage/runtime | One APK; verified data merge, official save retention, isolated clean setup, Play/Test/return and interrupted-setup retry on Retroid | Complete onboarding/frontend migration, rollback, modern target and crash/boot matrix |
| Audio/reuse | Audio scope planned; bounded sprite/parameter reuse exists | SFX/music editors, event bindings and dependency-aware reuse |

Unsupported editor operations preserve unfamiliar source. Templates do not lock
tools, games need not have a hero, and ordinary `.p8` stays authoritative.
Technical PIKOOS IDs remain until the [coordinated migration](PROJECT_NAME.md).

## Next implementation work

1. Complete the basic sprite/map editors, including shared-memory painting and
   resource allocation; connect them to the existing placement workflow.
2. Continue state/event animation, camera/background and other connected workflows
   before expanding genre presets.
3. Continue runtime/storage acceptance alongside creation: frontend migration,
   rollback, full onboarding and crash/boot matrix. Owner-approved target SDK 28
   remains temporary; modern-target execution is mandatory before release.

[Current creation evidence and exported game](showcase/0.0.72-en/README.md) ·
[One-APK setup evidence](showcase/0.0.71-en/README.md) ·
[The ordered 14-package plan](ROADMAP.md).

The full roadmap is **foundations → an original playable game → world/movement →
audio/rules/effects → reuse/remix/multicart → independent setup → Android acceptance**.
Detailed planning records own task status; this is an English entry point, not a
second backlog. Learning/languages accompany the tools. Linux/online work is later.

## Where to go

- [Public issue map](ISSUES.md): follow-up scenarios linked to backlog IDs.
- [Build and contribute](../CONTRIBUTING.md): prerequisites, commands, repository map.
- [Status](STATUS.md), [roadmap](ROADMAP.md), [backlog](BACKLOG.md),
  [acceptance](ACCEPTANCE.md): detailed planning, currently mostly in Russian.
- [Core tools](CORE_TOOLS.md): supported operations and missing connections.
- [Architecture](ARCHITECTURE.md), [compatibility](PICO8_COMPATIBILITY.md),
  [UX direction](UX_DIRECTION.md): implementation contracts.
- [Screenshots](showcase/README.md): capture versions and limitations.
- [Third-party notices](../THIRD_PARTY_NOTICES.md): identified materials.

Historical `docs/design/` reports record bounded evidence, not completed-product
claims. English editor images from 0.0.65 use a presentation build; English shelves
from 0.0.66 are in the regular lab. Full localization is unfinished.

GitHub Actions are disabled by owner decision. Validation is local; a release
requires explicit owner approval after product acceptance.
