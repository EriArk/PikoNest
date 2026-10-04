# PIKOOS

**Play PICO-8 games. Open them up. Change them. Make your own — on the handheld itself.**

PIKOOS is a controller-first home and workshop for **PICO-8** on Android gaming handhelds.  
It gives playing and creating their own space: open **Play** when you just want your games, or step into the **Workshop** when you want to draw, change, learn, remix or build something new.

PIKOOS stays close to the real thing. Projects remain ordinary `.p8` cartridges, generated code is ordinary Lua, and games run through the user's official PICO-8 runtime.

> **Play → Peek Inside → Remix → Create → Learn → Test → Share**

<p align="center">
  <img src="docs/showcase/0.0.61/01-play-library.png" width="720" alt="PIKOOS Play library">
</p>

## Play first

PIKOOS is meant to be useful even when you do not want to make anything.

The Play library treats cartridges like games rather than files: covers, folders, favorites, recent carts and direct launch. Leaving a game should bring you back where you started, whether it came from PIKOOS itself or a supported external launcher.

The Workshop is separate on purpose. Playing a game should never turn into a tutorial, project setup or editor unless you ask for it.

## A workshop made for handheld controls

A small gaming handheld is a great place to draw pixels and tweak a game. It is a terrible place to pretend you have a desktop keyboard.

PIKOOS is built around that fact.

You can work with sprites, sprite sheets, maps, animations, camera behavior, rooms, transitions and background layers using the controller. Code tools can insert and edit common Lua/PICO-8 structures through readable forms, while the normal source stays visible and editable whenever you want it.

<table>
  <tr>
    <td width="50%"><img src="docs/showcase/0.0.61/02-sprite-editor.png" alt="PIKOOS sprite editor"></td>
    <td width="50%"><img src="docs/showcase/0.0.61/03-sprite-sheet.png" alt="PIKOOS sprite sheet selection"></td>
  </tr>
  <tr>
    <td align="center"><sub>Draw and edit directly on the handheld.</sub></td>
    <td align="center"><sub>Select real regions of the PICO-8 sprite sheet.</sub></td>
  </tr>
</table>

There is no separate PIKOOS scripting language and no block system that traps a project inside the app. If a tool adds a condition, movement rule, draw call or animation, it becomes normal Lua in the cartridge.

<table>
  <tr>
    <td width="50%"><img src="docs/showcase/0.0.61/04-tool-catalogue.png" alt="PIKOOS Lua tool catalogue"></td>
    <td width="50%"><img src="docs/showcase/0.0.61/05-background-layers.png" alt="PIKOOS background layers"></td>
  </tr>
  <tr>
    <td align="center"><sub>Build rules without typing every character.</sub></td>
    <td align="center"><sub>Compose and reorder moving background layers.</sub></td>
  </tr>
</table>

## Learn by changing real games

PIKOOS is meant to make the distance between **player** and **creator** very small.

A beginner should be able to change something visible first — a color, a value, a rule, a sprite, a jump, a condition — and only then discover the Lua behind it. Explanations live next to the thing being changed instead of turning the app into a course that must be completed before anything fun happens.

Templates and reusable mechanics are there to give you useful starting points, not to define what kind of game you are allowed to make. A platformer may begin with movement and jumping; a puzzle may begin with a board and rules. The underlying tools remain available to both.

The long-term goal is simple: someone can arrive because they like tiny games, start modifying them out of curiosity, and eventually become comfortable writing real PICO-8 code.

## Make things once, use them again

PIKOOS has a shared asset library for things worth keeping between projects. Today that already includes reusable sprites and parameter presets; the finished library is intended to cover sprites, animations, backgrounds, SFX, music and other reusable pieces while keeping their source and dependencies understandable.

<table>
  <tr>
    <td width="50%"><img src="docs/showcase/0.0.61/06-asset-library.png" alt="PIKOOS asset library"></td>
    <td width="50%"><img src="docs/showcase/0.0.61/07-game-runtime.png" alt="Game running in official PICO-8"></td>
  </tr>
  <tr>
    <td align="center"><sub>Keep useful pieces for the next game.</sub></td>
    <td align="center"><sub>Test the result in the official PICO-8 runtime.</sub></td>
  </tr>
</table>

## Where PIKOOS is going

The complete Android version is meant to cover the whole handheld creation loop: browse and play cartridges, start from a blank cart or a lightweight template, draw sprites and maps, build rules and mechanics, animate things, work with cameras and backgrounds, add effects, SFX and music, test and diagnose the game, reuse pieces across projects, remix existing carts, handle larger multicart projects, and export a normal PICO-8 project that still works outside PIKOOS.

The edit → test loop is deliberately central. Change something, press a physical shortcut, run it in official PICO-8, exit, and return to the same place in the Workshop.

After the local Android product is solid, the architecture is intended to grow toward **Linux handhelds**, experimental **Link Play** between devices, and optional PIKOOS-aware online cartridges. Those are later directions, not requirements for the core editor and player.

## Current state

PIKOOS is under active development. **0.0.61 is a working Android development lab, not a public release.**

The current build already has a Play library, project creation/import/export, sprite and map editing, controller-first Lua editing and structured tools, conditions and branch actions, reusable presets and sprites, tile flags and collision helpers, movement helpers, animations, camera/room tools, transitions, background layers, session undo/redo, runtime diagnostics, and the edit → official PICO-8 → return loop.

There is still substantial work before the Android product is considered finished: sound and music creation, VFX, broader mechanics and event tools, deeper learning/reference features, full remix and project/dependency workflows, clean first-run/runtime setup, stronger recovery, controller-only edge cases, device coverage and final UX acceptance.

For the detailed implementation state and the deliberately picky acceptance rules, see:

- [Product vision](docs/PRODUCT_VISION.md)
- [Current capability audit](docs/STATUS.md)
- [Roadmap](docs/ROADMAP.md)
- [End-to-end acceptance](docs/ACCEPTANCE.md)
- [Current showcase](docs/showcase/0.0.61/README.md)

## PICO-8 compatibility

PIKOOS does **not** bundle PICO-8. The user supplies their own official PICO-8 installation.

The cartridge is the source of truth. PIKOOS metadata may live alongside a project, but removing that metadata must not turn the game into a proprietary PIKOOS-only format. Ordinary projects should remain understandable and runnable in normal PICO-8.

Android handhelds are the first target. Retroid Pocket Classic is the main development device today, but PIKOOS is not intended to depend on one screen shape, control layout or manufacturer.

---

PIKOOS is an independent project and is not affiliated with Lexaloffle Games. PICO-8 must be obtained separately from its official publisher.
