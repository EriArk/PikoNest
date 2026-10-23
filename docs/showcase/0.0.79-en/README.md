# Lab 0.0.79 — visual element and relationship selection

This is an **implementation and local-validation record**, not a screenshot pack
or completed native journey. The owner has paused console use. No device actions,
installations, new official-runtime executions or native captures occurred here.

## Implemented first slice of issue #17

- The Elements list shows sprite thumbnails and color-based descriptions.
  Identical descriptions get A/B/C suffixes. Labels describe appearance, not a
  guessed hero/object role or a persistent name. They can change after repainting
  or reordering. Empty/transparent and off-field placements remain selectable.
- A canvas selection opens contextual actions. Contact targets and switch links
  open a visual picker with a controller-accessible list. Self is excluded;
  switch links only offer configured switches, plus a no-link choice.
- The field outlines candidates, highlights the selected target and draws its
  connection to the source. A sentence explains the relationship. The display
  explicitly says Starting layout and does not execute a second game simulation.
- D-pad or touch selects a candidate; A confirms and B cancels. Test can preview
  a candidate in an isolated cart without confirming it. Recovery retains the
  choice and originating field; saving remains one ordinary-Lua change/Undo.
- Better two-line behavior/goal labels and compact-screen description placement
  use the existing Java/Canvas UI and font. All tools and source checks remain.

[Issue #17](https://github.com/EriArk/PikoNest/issues/17) remains open. Custom names,
broader direct contextual actions, full visual journeys and owner acceptance are
not completed by this slice. The recipe still has its documented editor limits;
no general scene system or PICO-8 extension was introduced.

## Actual verification

- Full local build completed **75 core test invocations** listed in
  `experiments/android-host/build.ps1`. Focused results: GameCanvasTest 16,504;
  PlayModelTest 96; GameAnimationTest 80,199; GameRelationshipsTest 82.
  These counts include data/timing assertions, not that many user journeys.
- Relationship coverage includes contact and switch filters, overlapping and
  invisible/off-field placements, controller navigation, Test/cancel/recovery,
  save failure, one Undo/Redo, reopening and stale/unknown source refusal.
  Legacy v1/v2/v3 journal checks passed. No Lua generator behavior changed.
- Production GameSurface draw calls were inspected through temporary host-only
  Java2D drawing adapters at 360×480, 360×800, 620×540 and 960×540 logical sizes. This caught
  a narrow footer label and guided compact/tall description placement. Font metrics,
  Android rasterization, touch dispatch and device ergonomics still need native
  review; these private diagnostics are not screenshots for the website.
- After those UI-only adjustments, final Android compilation and integrated APK
  packaging passed. Core tests were not repeatedly rerun for rendering labels.
  Packaging verified one launcher and no proprietary PICO-8 binaries.

Final **local-only, not installed** APK SHA256:
`ed45df302bd9adf10cea667cf9e4e706adca432c4d4c55aedc50ff081e3832eb`

[Machine-readable record](validation.json). No APK is committed or released.

## Remaining acceptance

1. Continue #17's element identity and contextual action flow on the existing
   operations. Keep names/resources/roles separate; keep All tools reachable.
2. When the owner permits console use, preserve existing data and install the
   final development build. Complete collection and hero-free switch journeys:
   select from field/list; link/cancel; draft Test/return; save/reopen/Undo/export.
3. Include the pending [animation checks](../0.0.78-en/README.md), independent
   official-runtime execution, compact/wide physical controls and data comparison.
4. Capture real English native before/after screens, then obtain novice and owner
   feedback. Issue #17, UX07/UX08 and beta acceptance remain open.

Use the [0.0.77 native images](../0.0.77-en/README.md) with their original version
and scope. The current [roadmap](../../ROADMAP.md) puts contextual UX before sound;
the broader core tools, setup and Android acceptance remain required.
