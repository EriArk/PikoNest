# Game view: behavior and game rules

Lab 0.0.77 adds a bounded creation path to the existing native Game view.
This is an ordinary PICO-8 Lua recipe, not a new runtime, universal entity
model or promise to visually edit arbitrary Lua. All tools remains reachable.

## Make a small collection game

1. Create a **Blank** project. Add, draw and place a sprite.
2. Select it, open **Behavior**, choose **D-pad** movement and a speed, then save.
3. Add another sprite. Give it the **Collect** rule and choose the moving sprite
   under **By**. The collectible disappears once when their rectangles overlap.
4. Open **Game rules**, choose **Collect all**, then save. Duplicate or draw more
   collectibles as desired. Each placement has its own collected state.
5. Optionally add a **Lose** contact rule to a hazard, or a **Win** rule to a goal.
6. **Start / Test** runs the game in the imported official PICO-8. PICO-8 **X**
   restarts it, including after an outcome. Exit returns to the editing context.
7. Continue editing, use Undo/Redo, reopen the project and export its saved `.p8`
   from the project shelf. Export contains the game, not an editor dependency.

## Make a game without a hero

Draw and place several sprites, then give each the **Toggle** rule. Left/right
selects a switch and PICO-8 **O** toggles it. **Also** optionally links another
already-configured switch: one press changes both. **Starts** sets its initial
state. **All on** supplies a completion condition. Design the links and
starting states so the puzzle is solvable; this version does not solve it for you.
Direction buttons also move any configured moving sprites, so mixing movement
and switch selection deliberately shares those controls.

## Editing contract

- Navigate to a field, A edits, directions adjust, A keeps the value and B
  reverts that field. Save commits the whole behavior or rule change as one
  Undo transaction. B outside a field discards this form's changes.
- Speed appears only for moving sprites; contact targets only for contact rules;
  initial state and links only for switches. The preview shows the starting
  layout, not a simulation of Lua or a live execution state.
- Draft Test uses a separate cart. Test does not save the form. Source-checked
  recovery retains pending fields and their revert values; a failed save retains
  the draft. A changed source refuses stale recovery.
- Appearance, position, duplication, removal and independent graphics continue
  to work after adding behavior. Removing a referenced sprite is refused until
  its dependent rule is changed. Removing another sprite preserves target identity.
- Linked switch dependencies are checked too. A referenced switch cannot silently
  become an unrelated rule. Duplicated placements share graphics initially, but
  have independent position and game state.

## Ordinary PICO-8 output and bounded scope

The authoritative data is the `.p8`: numeric initial values, ordinary `_init`,
`_update`, `_draw`, `btn`, `sspr`, rectangle overlap and drawing calls. Input edges
are tracked explicitly because `btnp` has held-button repeat. Restart calls
`_init`; it resets this recipe's game state, not every possible PICO-8 subsystem.
Movement runs at `_update`'s 30 updates per second, stops at screen edges, and
does not implement tile collision, physics or continuous collision detection.
Contacts run after all movement, in placement order; the first outcome wins.

Reopening verifies the complete generated source by reconstructing and comparing
it. A comment marker, empty graphics or metadata cannot authorize a rewrite.
Unknown or manually changed code is preserved and routed to All tools. Converting
a recognized static layout retains its comments; subsequent edits preserve the
generated source structure. No PikoNest metadata is required to run the cart.

Lab 0.0.78 adds [editable frame sequences and movement/switch-state bindings](GAME_ANIMATION.md). Local code/tests exist; native animation acceptance is still pending.

Current editor limits: 16 placements; speed 1–4 pixels per update; one contact
or toggle rule per placement; one optional linked switch; fixed rectangular
graphics; a solid background; fixed outcome text. These are PikoNest limitations,
not limits of PICO-8. Maps, audio events, richer conditions,
score/lives customization and library insertion still need connections to this
creation path. Existing advanced tools remain available, but arbitrary code
changes can take a project outside the recognized Game view subset.

## Completion gates

Technical evidence for this batch belongs in
[the 0.0.77 showcase](showcase/0.0.77-en/README.md).
Native input injection and automated checks do not close physical-controller,
novice or owner acceptance. The full product is not beta-ready merely because
these two small games work. Next verify the new animation path on device and connect audio,
then complete onboarding/storage, recovery and device acceptance. Public APK
publication still requires the owner's explicit release approval.

API reference: [official PICO-8 manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html).
