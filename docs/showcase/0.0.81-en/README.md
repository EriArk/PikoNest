# Lab 0.0.81 — goals and a connected rules overview

This is an **implementation/local-validation record**. Console use remains paused:
no device action, installation or new official-runtime execution occurred. There
is no native screenshot pack for this batch and no public APK release.

## What changed

- Game rules opens **How the game works**: the saved goal/background and named
  movement, interaction and animation entries. Elements without rules offer Add
  behavior. The overview is derived from the recognized cart, never a second rule
  database. Select an entry to edit the corresponding existing form.
- Save/Discard returns to the originating rule or another row for the same element
  if it was removed. Undo/Redo retains the overview. Recovery retains this context,
  including nested target choices and animation forms; failed saves keep the draft.
- Goals use explicit choices: no automatic win, collect every configured item,
  or turn all switches on. Outlines identify participants, counts describe what
  contributes, and missing-rule hints explain the next setup step. Empty goals can
  be saved as unfinished work. Contact win/lose still applies with no automatic win.
- Start tests the highlighted goal without saving. Cancel keeps the prior form
  value; Save rules commits once. Removing movement/interactions cannot silently
  break a state-bound animation or linked switch; messages name the dependency.
- Compact/tall/wide layout adjustments give the overview readable two-line entries,
  thumbnails and contextual descriptions. All tools remains reachable from Game view.

[Tool contract](../../GAME_BEHAVIORS.md#goals-and-rule-overview-0081)
· [interaction direction](../../UX_DIRECTION.md#goals-and-connected-rule-overview--lab-0081).

## Actual validation

- **77 core suite invocations**, Android compilation and integrated APK packaging
  passed locally. One launcher, 263 required Godot assets, and no official PICO-8
  binaries in the package were verified by the existing build checks.
- Focused checks: GameCanvasTest 16,504; PlayModelTest 96; GameAnimationTest 80,199;
  GameRelationshipsTest 81; GameIdentityTest 101; GameRulesOverviewTest 100;
  AssetNamingTest 53. These counts include repeated data assertions, not that many
  separate user journeys. Both LF and CRLF fixtures are covered.
- Connected checks cover goal preview/confirm/cancel, empty prerequisites,
  participant highlighting, named rule navigation, movement/interaction removal,
  linked-switch and animation dependency failures, explicit unlinking, source-
  checked recovery, failed saves, exact Undo/Redo, reopening and cart preservation.
  A null draft-model access in overview relationship highlighting was caught and
  fixed before the completed build.
- Journal v6 adds return context; legacy v1–5 compatibility remains tested.
  Invalid return bindings and stale source snapshots are refused. The ordinary
  Lua generator and supported execution semantics are unchanged.
- Production GameSurface draw calls were reviewed with temporary Java2D adapters
  at 360×480, 360×800, 620×540 and 960×540 logical sizes, including long names and
  empty goal requirements. This approximates layout, not Android rasterization,
  physical input or novice usability. Private diagnostics are not native website images.

Final **local-only, not installed** APK SHA256:
`dcac21a4daa4ac762e63182b045516bd260c0a30eef2152a7bce7a33b095bf03`

[Machine-readable record](validation.json).

## Limits and next work

Issue #17 and all complete acceptance gates remain open. The overview covers only
recognized Game view recipes; unfamiliar Lua stays protected in All tools. It adds
no arbitrary event graph, new goal type or automatic dependency cascade. Animation
rows open the existing editor, which still retains at least one frame; this batch
does not add whole-sequence removal. Existing name-binding/budget limitations remain.

Continue independent code work with general SFX creation and event binding. When
the owner authorizes console use, complete named collection and hero-free switch
journeys, animation playback, overview edits/removals/recovery, official Test/return,
independent export and protected-file comparison. Capture real English screenshots
and obtain physical-controller, novice and owner acceptance. The last complete
native journey remains [0.0.77](../0.0.77-en/README.md).
