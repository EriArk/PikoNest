# From Blank to two playable games — lab 0.0.77

Draw sprites, give them behavior, choose a goal, test, reopen and export without
opening Lua. This batch connects a small game-making loop to native Game view.
It does not mark the complete creator or public beta ready.

Eleven original English device captures, taken on Retroid Pocket Classic with
official PICO-8 0.2.7 on 18 October UTC / 19 October local time, 2026.
[Capture provenance and hashes](capture.json) distinguish the authoring/runtime
build from the final installed build. No APK release was published.

## What was made through the app

**Collection game:** a hand-painted heart moves with the D-pad. Touching the
red hazard loses; going around it and collecting the gem wins. X restarts.
The app generated the ordinary movement, contact, score and outcome Lua.

**Switch game:** three hand-painted smiley switches, no hero or movement.
Left/right selects; O toggles. The first switch also toggles the second.
All on wins; X restores the initial state. The first and third switches solve it.
Both games began with New project / Blank, not a pre-authored cart or genre template.
No project Lua or pixel data was injected during the native authoring journey.

- [Exact exported collection game](collection.p8?raw=true)
- [Exact exported switch game](switches.p8?raw=true)
- [Tool guide and precise semantics](../../GAME_BEHAVIORS.md)

These are original ordinary `.p8` files. They need no PikoNest library, metadata
or runtime extension. Collection export was also opened through an independent
official-runtime wrapper outside PikoNest: start, win and reset were observed.
That old wrapper was only a validation fixture; the product remains one APK.
The switch export was byte-verified; its execution was checked inside PikoNest.

## Connected editing and verification

Behavior uses contextual movement, speed, contact target, toggle state and link
fields. Game rules choose a goal and clear color. Unused settings stay hidden.
The selected sprite has a yellow outline; its contact/link target has a blue one.
Screen titles and target numbers remain readable in the final captured layout.

Both projects were saved, exported through Android's picker and read back exactly.
The collection behavior draft survived closing the app without modifying saved
bytes. Undo and Redo each restored exact cartridge bytes. The updated UI build reopened
both projects, ran an unsaved switch-state draft in official PICO-8, returned to
the same form and discarded it without modifying the saved game. The switch
reset frame exactly matched its initial frame. Two sampled frames during/after
a 2.2-second input hold were identical; the recipe tracks true input edges.

The full local build ran 73 core suites. Final focused tests passed 16,504 existing
GameCanvas checks and 96 behavior checks. These include field revert/recovery,
failure/retry, stale source refusal, ordinary-source reopening, comment safety,
target deletion/rebinding, independent appearance, duplication and continued
adding/editing after behavior. Subsequent builds addressed observed labels and
focused recovery/parser cases and old-menu focus migration; final packaging and installation succeeded, with both projects reopened unchanged. The final touch-field check also
verified that Decrease changes the value without opening Test.
All 59 pre-existing project/library/save files checked stayed byte-identical.
The final device media state was verified muted. These two games contain no audio.

Input was injected controller/keyboard events and touch. Android export Save
used touch. This is not physical-controller, controller-only picker, novice or
owner acceptance. Runtime screenshots prove the stated observed states, not
general compatibility with all cartridges or devices.

## Images and captions

| Image | Caption / context |
| --- | --- |
| [01 Game view](01-game-view.png) | The collection game's starting layout in the handoff APK |
| [02 Movement](02-movement.png) | Give the selected sprite D-pad movement and a speed |
| [03 Contact rule](03-contact-rule.png) | Collect this sprite when the blue-outlined sprite touches it |
| [04 Game rules](04-game-rules.png) | Choose the switch goal and background color |
| [05 Switch game](05-switch-game.png) | A separate Blank project with no hero |
| [06 Linked switches](06-linked-switches.png) | One switch also changes another; both remain ordinary sprites |
| [07 Collection win](07-collection-win.png) | Official PICO-8 after collecting the gem; authoring build |
| [08 Switches running](08-linked-switches-running.png) | One press turned on the first two switches; authoring build |
| [09 Switch win](09-switch-win.png) | Official PICO-8 after solving the switch game; authoring build |
| [10 Draft Test](10-draft-test.png) | Updated UI build runs an unsaved starting-state edit; saved game stays unchanged |
| [11 Export saved](11-export-saved.png) | Android export read back and verified; authoring build |

## Remaining limits

This is partial UX07/UX08 and technical evidence toward ACC-01/ACC-09. The form
supports a fully recognized recipe with up to 16 sprites, bounded speed,
rectangular contact, one rule and optional switch link per placement. Unknown
Lua stays untouched and uses All tools. The canvas shows initial positions,
not live Lua simulation; toggle outlines and outcome UI are fixed generated art,
not a completed animation/state editor. Undo history remains session-local.

General map/camera integration, animation/audio events, richer game rules,
multi-type library reuse, unassisted setup, modern Android target, broader device
testing and owner acceptance are still required work. This pack is not a claim
that sound, animation, learning or the full creator have been completed.
