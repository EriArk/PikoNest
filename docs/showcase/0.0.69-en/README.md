# Rules & state — lab 0.0.69

From **In the game**, press X for **Rules & state**. Start with a number, choose
when it changes, add a condition, and show its live value in the game. The same
tools work without a hero, sprite or genre template. Lua inspection remains optional.

This is the first connected C02.4 slice, not completion of M1 or ACC-01.

## What the forms edit

- **Starting value:** an ordinary numeric assignment in `_init`.
- **Rule:** an `if` in `_update`, evaluated in source order. Choose every update,
  `btnp` or `btn`, an optional value/number comparison, then add, set or reset.
- **Readout:** an ordinary `print(value,x,y,color)` in `_draw`.
- **Reset:** call `_init()` to restore all recognized starting values. This does
  not reset the cartridge clock, audio, random generator or other runtime state.

Forms are reconstructed from `.p8` source; no private game format, marker comments
or runtime library is required. Existing Lua insertion/parameter primitives,
cartridge transactions, Undo and shared English controls are reused. Draft journal
v6 adds the rules state; v1–v5 remain readable.

Create, reopen, change, duplicate, remove and reorder are explicit operations.
Removal refuses a still-referenced variable. Reordering refuses to cross a comment
between rules. Field A/Done keeps the change; B/Revert restores the previous value,
including after process recreation. Apply saves one history step. Start tests an
isolated candidate without saving the project.

## Demonstration

The numeric scenario starts from the app's **New project → Blank** flow:

1. Add `score=0` and `phase=0` as starting values.
2. On O, while `phase==0`, add 1 to `score`.
3. Each update, if `score>=5`, set `phase=1`.
4. On X, reset starting values.
5. Show `score` and `phase` at separate positions.

PICO-8's `btnp` includes auto-repeat: holding O also advances the number. The UI
names that behavior explicitly. This is a small authored numeric interaction, not
a newly added game template, resource-binding system or polished finished game.

## Images

Original ADB PNGs from the regular 0.0.69 APK on the dedicated Android 15 x86_64
PikoNest emulator. No capture-only translation build or image edits.

| Image | Contents |
| --- | --- |
| [Starting value](01-starting-value.png) | Name and initial number. |
| [Input rule](02-input-rule.png) | Button input and a state condition. |
| [Rule review](03-rule-review.png) | Readable behavior before Apply. |
| [Reset](04-reset-review.png) | Reset action using the same rule workflow. |
| [Readout](05-readout.png) | Live number position and palette color. |
| [Rules list](06-rules-list.png) | Starting values, ordered rules and readouts. |
| [Compact rule](07-compact-rule.png) | The same rule at 720 × 960. |
| [Export](08-export.png) | Android export completed and read back. |

[Saved ordinary cartridge](final.p8) · [Capture and verification record](capture.json) ·
[Download all eight images](../PikoNest-rules-en-0.0.69.zip?raw=true).

## Verification and open acceptance

Full local domain suite and APK build passed. `GameRulesTest` adds 111 focused
checks for the connected numeric workflow, supported line endings, exact no-op
round trips, preserved resource sections/comments, invalid callbacks, unresolved
references, variable collisions, export/readback, old journals, draft recovery,
save failure, isolated Test, Apply and Undo/Redo. Existing editor tests also pass.

The official manual supplied with the owner's 0.2.7 archive was consulted for
callback order, `btn`/`btnp` (including repeat), `print` and draw state. This batch
does not yet prove execution in official PICO-8 or independent runtime acceptance.
The emulator has no official runtime. Its Test verifies failure/return and draft
preservation; the runtime port's exact candidate bytes are checked in core.
Retroid's pre-existing unknown runtime session is not bypassed or stopped.

The demonstration's state/rules/readouts were authored through the Android UI,
without editing project files or opening Lua. Shelf entry/New used touch; form
operations used injected controller events. This is not physical button-feel or
owner acceptance. See capture.json for the exact native checks and installation.

The native journey also covered missing-runtime Test/return, field recovery and
revert after process recreation, Apply/Undo/Redo, duplication/removal, rule order,
and the compact layout. Android's document picker saved a new `.p8` in Downloads;
the exported 348 bytes exactly match the saved project. Picker Done/Save used touch.
The regular APK was installed on Retroid with a data-preserving update; all 48
pre-existing project/library files kept their SHA-256 hashes. Music volume is zero.

Current editing limits are explicit: integer initial/action/comparison values,
one optional comparison and one action per rule, player 0's six PICO-8 buttons,
30 Hz `_update`, and recognized line-delimited callbacks. New variable names use
suggestions and numbered values; arbitrary new names/renaming linked values need
later UI work. Unsupported Lua, `_update60`, includes, local/shadowed state and
unfamiliar initialization/update code are preserved and refused by this editor.
These are editor limitations, not PICO-8 restrictions.

Readouts are appended after existing draws and use their current camera. They
are not included in the resource-only camera/background sketch; Test is needed
for the actual screen. Sprite/map/animation/background editing remains available.
Dynamic sprite positions, multi-action/nested rules, animation/audio event binding,
and the complete controller-only ACC-01/export/runtime journey remain open.

Next: connect the starting values and rules to resource coordinates and events,
then carry the shared UX into the remaining graphics/audio tools. Keep templates
and preset expansion behind those core workflows.
