# Lab 0.0.80 — element names and explicit behavior choices

This is an **implementation/local-validation record**, not a new native screenshot
pack. Console use remains paused by the owner. No device actions, installations
or official-runtime executions occurred in this batch. This is not an APK release.

## What changed

- Name a selected element using a controller/touch keyboard. Names are optional;
  clearing one restores its visual description. They label placements rather than
  sprite-sheet resources, so shared appearances can have independent names.
- See those names in the Elements list and relationship picker. Duplicate names
  receive distinct suffixes, including when an explicit name resembles a suffix.
- Names follow supported movement, duplication/removal and animation frame
  changes. A renamed copy does not rename its original. Save/Undo, source-checked
  recovery, draft Test and byte export use the existing cart transaction.
- Choose movement and interaction from explicit lists. A contact action immediately
  asks which element touches it. Cancelling the target step restores the previous
  action and returns to the choices; saving commits the completed behavior once.
- Names travel in optional ordinary Lua comments. Annotation bindings are checked
  against the source rectangle/initial position; stale or malformed annotations
  cannot silently attach a name to another element. Existing title/author header
  lines are kept clear of encoded metadata.

[Interaction contract](../../UX_DIRECTION.md#named-elements-and-explicit-choices--lab-0080)
· [name storage and compatibility](../../GAME_BEHAVIORS.md#element-names-0080).

## Actual checks

- Full local build: **76 core suite invocations**, Android compilation and
  integrated APK packaging passed. One launcher, 263 required Godot assets, and
  no official PICO-8 runtime binaries in the package were checked.
- Focused checks: GameCanvasTest 16,504; PlayModelTest 96; GameAnimationTest 80,199;
  GameRelationshipsTest 81; GameIdentityTest 101; AssetNamingTest 53. Counts include
  repeated data/timing assertions, not that many separate user journeys.
- Name coverage: controller letter/save access, English length feedback, cancel,
  draft Test, keyboard-state recovery, save failure, exact Undo/Redo, ordinary cart
  reopen, duplicate/suffix collisions, layout deletion/remapping, and source checks.
- Named animation reorder → further allocation → recovery → Test generation →
  save passed. This caught and fixed an intermediate allocation path that still
  carried the old first-frame name binding. No animation generator semantics changed.
- Unicode names use ASCII-encoded comment bytes. Control/formatting characters,
  invalid UTF-16 names, duplicate annotations and mismatched external layouts are
  refused. Unrelated cartridge sections remain exact. Leading title/author lines
  and byte-exact repeated application are covered. Legacy journal checks pass.
- Production GameSurface drawing was reviewed with temporary host-only Java2D
  adapters at 360×480, 360×800, 620×540 and 960×540 logical sizes. This is approximate
  layout inspection, not Android touch/input, rasterization or usability proof.
  The diagnostic images are not presented as native website screenshots.

Final **local-only, not installed** APK SHA256:
`4f3b3d800499239b46b704bf33fe0e8aa784ac4360effcea7a4a2f5088eb122c`

[Machine-readable record](validation.json).

## Limits and next acceptance

Names have this editor's 24-character limit. They are not game roles, variables,
mandatory scene objects or a custom runtime dependency. Name annotations add
source size; complete character/compressed-budget checks remain separate work.
All tools placement edits do not yet update name bindings automatically: a
mismatch preserves the source and stops automatic Game view edits until that
external edit/annotation is reviewed or reverted. No unsafe identity guessing.

Issue #17 remains open. Next clarify goals and the configured-rule overview,
including return/edit/remove behavior. Once the owner authorizes console use,
complete named collection and hero-free switch journeys, the pending animation
checks, official draft Test/return, independent export, and protected-file comparison.
Then capture real English screens and obtain physical-controller, novice and owner
acceptance. Existing [0.0.77 native evidence](../0.0.77-en/README.md) retains its
original version and scope; it is not proof of these new features.
