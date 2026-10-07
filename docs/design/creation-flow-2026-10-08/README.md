# PikoNest creation flow study

8 October 2026 · UX06 · **design proposal, not an Android release**.

The owner approved simplifying creation and learning from other game makers.
These particular screens still need review. Native implementation remains lab
0.0.75. [Full journey and decisions](../../CREATION_EXPERIENCE.md),
[current queue](../../ROADMAP.md), [tasks](../../BACKLOG.md).

## Try the proposal

Open [index.html](index.html) locally in a browser, keeping its repository path
so the existing Monocraft font can load. GitHub displays its source; it does not
host this prototype as an application. No install, account or network service is
required by the prototype. The only external-to-this-folder asset is the existing
font at `experiments/android-host/assets/Monocraft.ttf`, with its existing OFL
notice. Illustrations are original code-drawn pixels, not extracted game assets.

- Choose Blank, Little explorer or Light puzzle.
- Select an element on the canvas or through the Elements buttons.
- Edit Appearance, Move, Behavior or the bounded example Rules, then Done/Cancel.
- Undo reverses a completed change. Try Test and Return to editing.
- In the puzzle, left/right selects a light; the action button toggles it.
- Tools remains available in every project, with labelled scope messages for
  tools outside this prototype.

Keyboard: arrows navigate; Enter activates a focused control; Escape returns or
cancels; F5 opens Test; U undoes in the game view. Within Move, arrows move the
selected element. Within the pixel grid, vertical arrows move one row. The pixel
grid is 8×8 **only in this mockup**, not a PICO-8 size limitation. In simulated
Test, arrows move/select; Z or Space toggles the selected puzzle light once per
press. Visible buttons provide the same actions. This study has no gamepad
adapter; the A/B/Start/Select footer depicts the proposed native input vocabulary.

## What is interactive, and what is not

Interactive: starts, selection, pixel edits, move/cancel, duplicate, Undo, a
movement speed draft, one rule recipe per example, shared-appearance choice,
and simulated Test/return. A draft Test uses an isolated browser copy. Browser
memory is discarded on reload; “Done” never writes a cartridge to disk.

The explorer simulation supports one controlled element and one overlap/win
rule. The puzzle uses the starter's simple light-selection/toggle rule. The
generic movement form is illustrated, but the puzzle simulation keeps board
selection rather than applying movement to its lights. This is a mockup limit,
not a proposed genre restriction. The rule card illustrates a future general
editor but is **not** that editor.
Blank → drawing → movement/overlap is interactive; authoring a whole puzzle from
blank, multiple states/actions, animation creation and general grouping are
specified in the journey document, not implemented here. Sound assignment is
illustrative and silent. New drawing is placed at the centre before Move in this
mockup; the proposed native flow keeps drawing and first placement in one draft.

No `.p8` parsing/writing, allocation, real resource-conflict detection, official
runtime, export, durable history or recovery is implemented. In the Android app
these must reuse/extend the existing core and keep ordinary `.p8` authoritative.
The browser simulation does not establish PICO-8 compatibility or performance.

## Screens for review

The PNGs are unedited browser captures of this prototype. They must not replace
the native-build screenshots on the public product page as implemented features.

| Screen | Question to review |
| --- | --- |
| [Starting points](01-starts.png) | Is blank as approachable as a starter? |
| [Game view / selected element](02-game-view.png) | Is the game a useful home for creating and editing? |
| [Appearance](03-appearance.png) | Is the return to the selected element clear? |
| [Rule](04-rule.png) | Is the event, target and consequence understandable? |
| [Hero-free puzzle](05-puzzle.png) | Do the same actions make sense without a player? |
| [Shared appearance](06-shared-appearance.png) | Is the consequence clear without memory terminology? |
| [Compact layout](07-compact.png) | Do actions remain legible when stacked? |
| [Wide layout](08-wide.png) | Is extra space useful without stretching the action panel? |

## Acceptance and limits

See [verification.json](verification.json) for the actual local browser checks.
They cover prototype interaction and layout, not native or physical acceptance.
The live built-in browser was also used for visual inspection.

Owner review is pending for the game-view home, contextual action list and
safe Done/Undo workflow. Hardware controls, long sessions, readability on a real
handheld, resource preservation and the unassisted novice journey remain open.
No application code, APK, package identifier, user data or device volume changed.
