# Large hero binding / Android lab 0.0.7

Real Retroid Pocket Classic captures at 1240×1080, 2026-10-02. Editor captures
use the installed final build; game captures use the owner's official PICO-8
0.2.7 through the existing external wrapper. No mockups or compositing.

- [Assignment preview](preview.png): source image 32×24, proposed rectangular
  body 26×16, yellow collision outline and green ground line.
- [Workshop](workshop.png): illustration uses the actual assigned image and
  body offset; the hero link opens its 32×24 source rectangle.
- [Library](library.png): the copied game's cover now shows its cloud hero.
- [Official game](official-game.png): the saved game runs with the cloud;
  its bottom visible pixels meet the ground without the transparent margin.
- [Runtime physics checks](physics-checks.png): the separate diagnostic cart
  completed eleven assertions and draws its diagnostic body overlay.

## Observed device workflow

All three carts were backed up first. Changes were made only in `Копия 1`
(`remix-0001`), reusing the cloud drawn in 0.0.6.

1. Reached **Герою** with injected D-pad/Confirm. The preview did not write.
   Cancel retained the exact original SHA-256. Reopened the preview, backgrounded
   and killed host PID 19336, checked it absent, then reopened as PID 19477.
   The uncommitted image/body preview was reconstructed and still wrote nothing.
2. Confirmed assignment. Its image rectangle is `(64,24,32,24)`, with body
   `(3,3,26,16)` relative to that image. Y restored the complete original file,
   including the prior `spr(4,x,y,2,2)` binding and movement code, in one action.
3. Reopened preview and used Start. The saved cart and `run.p8` snapshot had
   identical hashes. Official PICO-8 displayed the cloud standing on the ground.
   The gfx section was byte-identical before/after assignment; only owned Lua
   fragments and the new ordinary variables changed.
4. Exited with injected Ctrl+Q and launched the separate physics fixture.
   The first diagnostic launch returned without an observed game frame; the
   cause was not established. A second launch showed the success banner.
   This does not fix or mask the previously documented wrapper startup issue.
5. The fixture executes the generated movement code and checks: binding values,
   spawn, floor contact, left and right screen bounds, platform landing, upward
   passage, missing the platform at its right edge, respawn, a taller body and
   a narrower body. All eleven assertions completed in official PICO-8. The
   changed height/width are temporary diagnostic values, restored before play.
6. Returned to the editor. The workshop link opened the actual large region.
   Opened the library using controller actions and observed the cloud cover.
   Killed the background host again; new PID 20057 reopened the saved binding
   and shelf. Left the device in the copied game's workshop without a draft.

The original `moon-garden` and `garden-0001` remain byte-identical to their
backups. No projects were removed or replaced, and the drawing itself was not
rewritten to assign it. A synthetic face-button jump event did not yield an
observed jumping frame; hardware/runtime input acceptance is not claimed here.

| Artifact/state | SHA-256 |
|---|---|
| Before assignment / after cancel / after undo | `e931122b81cb3cab91d500e02576366a094db03065acfe0d4c2b2658e6707b02` |
| Assigned saved cart and runtime snapshot | `e0ea70bce657dd98155be51d32dd2beafb9922a562457e3bfa686371b8c9c64d` |
| Diagnostic fixture | `d9e4bbef6687e8347e61ecf46d7bb31d8baf1692688366a9c5921ace3b97ab09` |
| Installed APK | `471daca98c6898497370a39bd4383e07e00c112dd4307f4c009237a1dd224845` |

## Automated validation and scope

LabCartridgeTest passed; WorkshopTest 799 checks; SpriteWorkflowTest 2804;
LibraryWorkflowTest 28; DrawingWorkflowTest 4269; RegionWorkflowTest 16422;
HeroBindingTest 41. Compilation, APK packaging and signature validation passed.

The new checks cover transparent margins, body bounds, generated Lua use of
body dimensions, repeated binding, parameter editing after insertion, resource
and CRLF preservation, rejection of changed movement code/name conflicts,
empty images, reservation of all intersecting hero cells, modal navigation,
failed-save retry, commit-before-test and byte-exact undo of the migration.
Restoring stale preview preferences over a restored legacy cart is read-only;
it cannot trigger the older immediate-assignment path. This last persistence
guard was added after the lifecycle checks; the final APK was installed and
its workshop, preview and shelf captures refreshed with the saved cart intact.

The rectangle is our template's collision convention, not PICO-8 physics or
pixel-perfect collision. Default color-0 transparency is assumed for this owned
template. Body editing, animation anchors and arbitrary/custom game inference
are not implemented. Painting after assignment keeps the saved body fixed;
reassignment explicitly proposes a new one. The eight old cards still work,
and existing projects are never migrated merely by opening them.

The data and workflow remain portable Java; Android handles rendering, input,
storage and external runtime launch. Physical controller ergonomics, wider
viewport acceptance and runtime reliability remain separate gates. See the
[host contract](../../../experiments/android-host/README.md),
[runtime fixture](../../../experiments/runtime-smoke/hero_physics_checks.p8)
and [runtime report](../../ANDROID_RUNTIME_POC.md).
