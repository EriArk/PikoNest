# Lab 0.0.78 — Game view animation implementation

This is a **validation record, not a new screenshot pack or completed native
journey**. The owner requested no further console use while another project was
using the Retroid. Code and documentation work continued locally.

## Implemented

Game view now opens Animation for the selected sprite. It supports independent
frame copies, painting, per-frame duration, ordering/removal and Loop / When
moving / When on bindings. A frame strip complements controller fields. Ordinary
PICO-8 Lua/data remain authoritative; old recipe source round-trips unchanged.

Save/Undo covers the whole sequence and pixels. Draft Test generation, field
revert, painting cancellation, source-checked recovery, save failure and safe
shared-frame copies have local regression coverage. Recovery also handles a new
graphics section and refuses pixel changes outside the recorded edit footprint.
See the [tool contract](../../GAME_ANIMATION.md) for exact scope and limitations.

## Actual verification

- Full local build: **75 core suite invocations**, successful Android compilation
  and integrated APK packaging. No official runtime binaries are bundled.
- After focused recovery corrections: GameCanvasTest **16,504**, PlayModelTest
  **96**, GameAnimationTest **80,199** checks passed; final packaging passed.
  The large animation count includes repeated timing-boundary checks, not that
  many different user scenarios. The same generator still needs runtime proof.
- The initial APK installed on Retroid Pocket Classic. Play/Workshop opened,
  and the native Copy project flow created/opened a separate editable copy.
- Input stopped when another app took focus. The owner then explicitly requested
  no further console use. No device actions followed that instruction.
- The last observed media stream was muted at effective volume zero. No audio
  setting was changed. A private pre-install backup/inventory covers 61 existing
  project/library/save files; a post-run preservation comparison remains pending.

## Package provenance

Final **local-only, not installed** APK SHA256:
`e63a589500abeae02e4696b0c9cee257a1f68d31e99a71912e83454991236d6d`

Initial installed APK SHA256:
`63703b17302ce7f1ba0c085014cc410479783b0cdeb401e794bff2b2c324d037`

Both are development build 0.0.78. The final local package additionally contains
recovery handling for initially absent gfx sections and recorded pixel-edit
footprints. Do not identify the initially installed app as that final artifact.
No APK is committed or published as a GitHub release.

[Machine-readable record](validation.json).

## Deferred checks, in order

1. Obtain owner permission to use the console again. Check no active runtime or
   pending owner edit, preserve current data, then install the final local APK.
2. Work on a copy of the collection game: Animation → copy frame → draw → choose
   When moving → change duration/order → Test the draft → return/cancel. Save,
   close/reopen, recover a paint draft, and verify exact Undo/Redo.
3. Do the same on a copy of the linked-switch game using When on. Check linked
   switches, stopping, outcome and restart. Export the ordinary saved cart and
   run it independently. Compare protected pre-existing files.
4. Run the [prepared synthetic official-runtime fixture](../../../experiments/android-host/tests/fixtures/game-animation-0.2.7.p8).
   It checks 13 assertions including frame pixels, movement at the screen edge,
   linked-switch state entry, restart and 40,000 updates. **It has not run yet.**
   It substitutes button input and does not validate physical controls. Its
   intended success message is `animation checks passed`.
5. Capture the English animation form/frames/gameplay and record exact artifact
   hashes. Then obtain visual, physical-controller and novice/owner acceptance.

No new animation screenshots are supplied. Use the
[previous 0.0.77 pack](../0.0.77-en/README.md) for the already demonstrated behavior
workflow, with its original version/context. Animation runtime, UX08, N01 and
full beta acceptance remain open. General SFX/music editing is the next code block.
