# Shared animation and camera editors — lab 0.0.67

Regular development APK, English interface. Captured on 7 October 2026 in an
isolated Android 15 x86_64 emulator. These are original ADB screenshots, without
image edits or a capture-only translation build. No APK release was published.

[Download the seven images and captions](../PikoNest-editors-en-0.0.67.zip?raw=true).

## Screens

| Image | What it shows |
| --- | --- |
| [Animation](01-animation.png) | Frame list, duration, resource preview and shared actions. |
| [Field edit](02-field-edit.png) | Explicit edit mode: keep or revert the duration. |
| [Camera](03-camera.png) | Fixed camera offset, with an explicit preview action. |
| [Camera preview](04-camera-preview.png) | Resource sketch before Apply; Test is needed for gameplay. |
| [Frame region](05-frame-region.png) | Choose a sprite-sheet region using controls or touch. |
| [Compact camera](06-compact-camera.png) | 720 × 960 layout; action hints wrap instead of being cut off. |
| [Missing runtime](07-runtime-required.png) | Test failure keeps the unsaved draft and provides a return action. |

The normal captures are 1240 × 1080. Compact and 1920 × 1080 wide layouts were
also inspected. The wide image is verification evidence, not an additional website
hero image. The fixture is the existing [0.0.65 camera demonstration](../../design/android-camera-uses-65/final.p8),
loaded into the isolated emulator's Moon Garden project. It contains no mandatory
hero. The displayed project title does not imply the original Moon Garden game
was rebuilt through these tools. The fixture and restored initial editor state
were prepared before the UI journey; subsequent edits used controller events.

## Verified in this batch

- Full local domain suite and APK build passed. `UsesEditorTest` adds 32 checks for
  exact draft snapshots, saved-cart/history preservation, process recovery,
  field rollback, launch failure and older journals. Focused existing checks:
  GameUses 90, AnimationUses 81, CameraUses 76, WorldCamera 69.
- On the emulator, controller events exercised animation duration editing,
  cancellation after process recreation, missing-runtime Test and return,
  Apply, one Undo and Redo. Saved cartridge bytes matched before/after Undo/Redo.
- Camera offset editing, field cancellation, draft recovery, Apply/Undo/Redo and
  native/compact/wide layouts were exercised with the same byte checks.
- Draft Test creates an ordinary `.p8` candidate without saving the project.
  Core tests compare the exact bytes passed to the runtime port. An unfinished
  region/position selection gives a prerequisite message instead of ignoring Start.
- Apply is a separate saved operation. Journal v4 preserves the original field
  value for B/Revert; journals v1–v3 remain readable. No identifiers were migrated.
- The official 0.2.7 manual supplied with the owner's archive was checked for
  `camera`, `sspr` and `time`. Generated Lua/resource semantics are unchanged.

## Limits and remaining acceptance

The emulator has no official runtime installed. Its Test exercise verifies the
failure/return path, not successful PICO-8 execution. The exact candidate launch
and retained context are additionally tested through the core runtime port;
physical successful Test/return for this new UI remains pending.

Retroid's existing unknown runtime session was not bypassed or stopped. The local
APK update preserves app data, and all 48 pre-existing project/library files were
compared by SHA-256. The console's music stream remains muted. Physical button
feel and owner visual acceptance remain separate gates.

This is a bounded UX03 implementation, not the complete creator or full English
localization. It still recognizes supported flat draw blocks; complex Lua is
preserved and refused by this editor. Animation timing starts at game launch,
not a gameplay event. The sprite/map placement form retains its existing direct
D-pad adjustment behavior; the new explicit field edit mode applies to animation
and camera. Rare platform/domain errors and other editors still need localization.
Unknown-session recovery remains issue #10. Next: UX04, connect existing background
layers to this Workshop path, then the connected blank-game workflow.

Machine-readable capture hashes and verification scope: [capture.json](capture.json).
