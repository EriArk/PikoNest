# PIKOOS Android host experiment

Version 0.0.15 adds sprite transformations. Open a sprite/region, leave the canvas
with B if drawing, move to the tool selector, then choose Отразить / повернуть.
Left/right selects horizontal mirror, vertical mirror, clockwise 90° or 180°.
The before/after preview always starts from the saved pixels; browsing operations
does not stack them. A applies once, B cancels, Y afterwards undoes the whole edit.
Touch uses the same operation selector and confirmation. Brush, cursor, region
and zoom survive the operation. Start and tab switching cannot commit a preview.
Only selected ordinary gfx pixels change; Lua, flags, map and role bindings stay
unchanged. A sprite used multiple times changes everywhere it is referenced.
No effective change means no write and no extra undo entry. Failed writes retain
the preview; process recreation restores it for explicit confirmation against
current saved pixels. Undo history remains in-memory, as for other lab edits.
The upper 128×64 lab sheet restriction still applies. Quarter turns currently
require square selections so the tool never silently expands its footprint;
this is a lab limit, not a PICO-8 limit. Rectangular quarter turns with destination
selection, arbitrary angles and animated transforms remain future work.
[Evidence](../../docs/design/android-transform-14/README.md).

Version 0.0.14 adds external `.p8` export. On My games, choose a cart and press Y,
or navigate to Save .p8. The preview captures that project's current saved bytes;
Confirm opens Android's create-document dialog, Cancel discards the preview.
Cancelling the system dialog returns to the preview. The worker closes its output,
reopens it and compares every byte before reporting success. Neither the project
nor an existing chosen source URI is rewritten. A partial/unverified destination
may remain after failure; retry explicitly chooses a new document.
Preview/result state survives process death. An interrupted write restores an
uncertain result, never an automatic rewrite or success. An in-process worker is
retained across Activity recreation. Export is one text cart, not a project archive,
dependency bundle, library backup or account-token-cleaning operation. 2 MiB is
the lab snapshot budget, not a PICO-8 limit. System picker navigation can require
touch; host preview/result actions use controller or touch.
[Evidence](../../docs/design/android-export-13/README.md).

Version 0.0.13 imports a single text `.p8` through Android's document picker.
My games → Down → Down → Confirm opens file selection. Preview shows source
sprites and filename; Confirm adds an independent project, Cancel adds nothing.
Start cannot bypass the preview. The original URI is opened read-only; imported
bytes are preserved exactly, including unknown sections. A durable preview and
stable destination ID survive process death; publication retries are idempotent.
The filename supplies the shelf/workshop title via optional `.pikoos/import-name`.
Import does not certify Lua or runtime compatibility and never executes the cart.
All existing generic sprite/library tools remain available after opening it.
The lab intake budget is 2 MiB, not a PICO-8 format limit. `.p8.png`, archives,
includes/multicart dependencies and folder setup remain pending. Single-file
project export was added in 0.0.14.
Android's picker is system UI; on Retroid, changing its root required a touch
during this check. Its file list and the host confirmation work with injected
controller buttons. This is not full physical-controller acceptance.
[Evidence and limitations](../../docs/design/android-import-12/README.md).

Version 0.0.12 adds names for sprite-library records. Y in the library renames
the selected record; X on the export preview names the pending resource.
The controller alphabet grid supports Russian/Latin, case, digits and symbols.
Confirm types, X erases, Y changes case, L/R changes alphabet, Select toggles
select-all/end editing, Start finishes, Cancel discards. Touch uses the same
keys. The complete draft restores after process death without an automatic write.
Rename atomically changes only the title and rejects a stale source record.
Catalogue focus follows the resource ID through sorting and reopening.
[Evidence](../../docs/design/android-names-11/README.md).

Version 0.0.11 adds the first shared sprite library. In any project, open the
menu's Resources entry; with a sprite/region selected, X previews saving it.
Confirm stores independent indexed pixels and source metadata. Select a record
in another project, place and preview it, then confirm insertion. Cancel and
whole-operation undo preserve the target exactly. The library and insertion
draft survive process recreation; inserted carts run without library access.
Storage is currently app-private, separate from project folders. Folder selection,
deletion, `.p8.png` import, flags/animation/audio dependencies and other
resource types remain pending. [Evidence](../../docs/design/android-assets-10/README.md).

Version 0.0.10 makes sprite/rectangle copying available in every supported cart.
Choose destination → review before/after → confirm, using controller or touch.
Occupied areas can be explicitly replaced; source overlap is blocked. No Lua
or role bindings are rewritten. Failed saves retain the preview for retry;
cancel writes nothing and undo is exact. Process recreation restores placement
and requires reviewing the preview again. Current placement covers upper 128×64
in 8-pixel steps; shared map-half editing remains pending.
See [device evidence](../../docs/design/android-copy-09/README.md).

Version 0.0.9 adds blank and Lights puzzle creation, plus a generic resource
workshop without required hero/speed/jump fields. Moon Garden parameters and
hero assignment are conditional on its optional binding. Missing gfx is created
only by an effective edit and undo restores the original cart exactly.
See [hero-free creation/runtime evidence](../../docs/design/android-herofree-08/README.md).
The general path has sprite editing and code viewing; free-form Lua editing,
arbitrary file import and resource types beyond sprites are still unimplemented.
A new blank cart does not automatically display its sprites.

Version 0.0.8 aligns resource names with PICO-8: sprite labels replace generic
"drawing" labels in the existing editor. See [device capture and checks](../../docs/design/android-terminology-07/README.md).
The [cross-project asset library](../../docs/ASSET_LIBRARY.md) now has the bounded
sprite workflow above. The [first-run setup wizard](../../docs/FIRST_RUN_SETUP.md)
remains a documented requirement, not an implemented feature of this experiment.

An isolated 0.0A/0.0C/0.0D proof: a controller-operated workshop using the
owner-approved PICO-8 visual baseline. Edit speed/jump or the hero's sprite in
a real `.p8`, select/create/copy a sprite and explicitly assign it to the hero,
send a read-only snapshot to the installed official-runtime
wrapper, and restore the workshop when the user returns.
Version 0.0.4 adds a project shelf, creation from the small-game template and
independent copies, with separate editing context for each project.
Version 0.0.5 adds a drawing-tool chooser, connected fill, previewed lines and
a color picker, all reachable through the same semantic controller actions.
Version 0.0.6 adds rectangular sheet selection, drawing beyond 16×16 and a
16-pixel zoom window that follows the controller cursor.
Version 0.0.7 adds large-image hero assignment with a collision-body preview,
ordinary Lua dimensions and coordinated placement/platform/screen bounds.

The visual direction is approved; this implementation remains an experiment,
not a decision to use Java/Android Views for the production application.
The existing SDK-only host avoids introducing another framework while testing
input, byte edits, Android intents, URI grants and activity lifecycle.
Rendering uses a native Canvas; cartridge edits and interaction state are
JDK-only and the Android input adapter emits semantic actions.
The experiment has no library dependencies and includes no PICO-8 binary.

## Boundaries

- `core/`: JDK-only `LibrarySession`, `WorkshopCartridge`, `WorkshopSession`, original
  `LabCartridge` regression fixture and `PicoRuntimeBackend` contract.
- `../p8-roundtrip/core/`: shared byte-preserving section reader/writer proof.
- `src/`: Android activity, atomic persistence, read-only URI provider and
  external-app runtime adapter.
- `assets/moon-garden.p8`: original ordinary PICO-8 Lua and a 16×16 hero.
  Each parameter/pixel edit changes exactly one byte via the shared P8 document.
  Legacy card assignment edits only the sprite number in the owned
  `spr(n,x,y,2,2)` call; large-image binding also explicitly updates the known
  movement functions and adds ordinary Lua dimensions. Copying changes only
  destination image pixels.
  This deliberately handles the owned template, not arbitrary imported carts.
- `assets/workshop.p8`: original diagnostic fixture, retained for regression.
- `assets/Tiny5-Regular.ttf` and `OFL.txt`: Tiny5, SIL Open Font License;
  same Cyrillic-capable pixel font as the approved study, from google/fonts.
- `tests/`: executable byte-preservation/validation checks with no Android SDK.

The host requests no storage, network or privileged permissions. A scoped
`content://` read grant lets the wrapper copy the test cart to its own cache.
Canonical files are `files/projects/<id>/game.p8` in private app storage.
The existing `moon-garden/game.p8` is retained without migration or rewriting.
The previous `files/game.p8` is preserved. UI preferences are optional app
metadata and are not needed to run the cartridge elsewhere.
`files/run.p8` is a launch snapshot; the runtime cannot mutate the original.

## Run

Prerequisites: JDK 21, Android platform 34, Build Tools 36.0.0, and a configured
Macs75 `io.wip.pico8` wrapper (tested separately at 1.6.6 with official 0.2.7).

```powershell
.\experiments\android-host\build.ps1
adb -s SERIAL install -r .local/artifacts/pikoos-runtime-lab.apk
adb -s SERIAL shell am start -n art.pikoos.runtimelab/.MainActivity
```

The build first runs portable core tests, then compiles, packages and signs a
debuggable lab APK. Generated files and its development-only signing key stay
in ignored `.local/`. Paths can be supplied through build-script parameters.

## Controller workflow

| Input | Action |
| --- | --- |
| D-pad / stick | Choose a field; move a code line or pixel cursor |
| A / B | Confirm / back (exchangeable in Select menu) |
| Left / Right inside a value | Change the draft; A saves, B cancels |
| L1 / R1 | Previous / next tool, retaining selection |
| X | Context explanation, sprite color palette, or Copy on the sprite sheet |
| Y | Undo last edit |
| Start | Save a pending parameter and test in official PICO-8 |
| Select | Menu, including A/B mapping |

All implemented editor actions are reachable without touch. Touch uses the
same session actions: fields, tabs, palette and single-pixel taps. The code
view displays actual Lua and edits the two owned parameters; arbitrary text
entry, autocomplete, map and audio editors are not implemented yet.

### Sprite selection and creation (0.0.3)

The sprite tab initially opens a 4×2 sheet of eight 16×16 regions. These are
ordinary gfx pixels in the first 16 rows of the existing cart; the underlying
PICO-8 sprite numbers are 0, 2, …, 14. This is a bounded resource slice for the
owned template, not the entire PICO-8 sprite/map editor. UI names use one-based
image numbers so users do not need to understand tile addressing to start.

- D-pad selects an image. A opens its editor; A again enters pixel drawing.
- Below the grid: New, Copy and Assign to hero, all reachable with D-pad.
- New opens the first empty unassigned region. Leaving it untouched writes
  nothing. Painting makes it an ordinary sprite in the cartridge.
- Copy (also X on the sheet) writes one undoable duplicate into an empty
  region and opens it for editing. It leaves the original and hero binding alone.
- Assign to hero explicitly changes which image the game draws. The pink
  Hero marker and workshop preview follow this binding. Merely browsing or
  editing another image does not assign it. Empty images cannot be assigned.
- B leaves the canvas for tools, then returns to the sheet. The workshop's
  Hero image link opens the assigned image directly. Sheet/editor selection
  and the pixel cursor survive runtime return and process recreation.
- A full sheet cannot overwrite an occupied region; the UI reports no free
  cells. An erased but still assigned hero is reserved from automatic allocation.

Existing 0.0.2 Moon Garden files open without migration or normalization,
including their saved speed/jump/pixel changes. As before, the adapter refuses
ambiguous or manually changed owned code rather than guessing a binding.

### Project shelf and creation (0.0.4)

- Select menu → **Мои игры** opens the shelf. Left/right selects a cart;
  down reaches New / Copy, up returns to carts. A opens/activates, B resumes
  the active workshop, X copies the selected project. A/B mapping is global.
- **Новая игра** explains the starter (hero, jump, platforms). Confirm creates
  a clean copy of the bundled original Moon Garden template; cancel writes
  nothing. Names are automatic (`Новая игра 1`, `Копия 1`), requiring no keyboard.
- Copy reads the selected project's saved `.p8` again and preserves all bytes,
  including unknown sections. Browsing does not switch the active workshop.
- Each project gets its own directory and preferences. New directories are
  staged, written and synced before publication; existing destinations are
  refused. Failed/interrupted hidden staging directories are retained, not
  presented as completed projects. There is no deletion operation.
- The shelf's selected cart/focus and each project's tool, sprite, cursor and
  parameter draft survive process recreation. Switching projects within one
  process also retains their independent undo histories; undo is not durable.
- A damaged/unsupported project remains visible with an error; other projects
  remain available. Opening/copying it never replaces it with a template.
- Covers are illustrations using the actual assigned hero, not game captures.
  Touch opens cards/buttons; horizontal swipes browse beyond the current page.

This 0.0.4 slice was a library of owned-template projects. Version 0.0.9 also
creates blank and hero-free projects; arbitrary cart import, renaming, export,
persistent history and `.pikoos` metadata remain future work. Sound/music and animation
requirements do not imply implemented editors. See
[device evidence](../../docs/design/android-library-03/README.md).

### Drawing tools (0.0.5)

- From the canvas, B returns to tool navigation. Select the current tool name
  (the first control to the right) and A opens the chooser. Up/down and A choose
  Brush, Eraser, Fill, Line or Picker; B leaves the old tool selected.
  Touch can open the chooser directly. A/B hints respect the configured mapping.
- Fill changes only the four-connected area containing the cursor, bounded by
  the selected image/region. Diagonally touching areas remain separate.
- Line: A anchors the start; D-pad moves the other endpoint with a live preview.
  A commits the whole line. B or Y cancels the preview without changing the file
  or earlier undo history. Start commits it before passing saved bytes to runtime.
  Two touch taps choose the same endpoints. Tabs, palette, menus and resource
  actions cannot discard a pending line; complete or cancel it first.
- Picker reads the cursor's color without saving and returns to the prior
  drawing tool (Brush when coming from Eraser). X still opens the palette.
- Each fill or completed line is one undo entry and one atomic save. A no-op
  fill/line creates no history entry. Write failure retains the line draft for
  retry. Line endpoints, color and tool survive process recreation in optional
  UI preferences; only confirmed pixels belong to `game.p8`.
- All drawing operations use the portable cartridge/session core and preserve
  unrelated gfx, Lua, line endings and unknown sections. The eight cards remain
  shortcuts into the owned template; 0.0.6 also exposes rectangular sheet areas.

Held-button brush strokes, touch drags, shapes and persistent undo
remain future work. [Device captures and checks](../../docs/design/android-drawing-04/README.md).

### Rectangular sheet areas (0.0.6)

- Below the eight cards, **Область листа · другой размер** opens the sheet.
  D-pad chooses the first 8×8 cell, A anchors it, D-pad chooses the opposite
  cell, A opens that rectangle. Either corner order works. Touch uses two taps.
  B returns from the second corner to the first, then to the previous screen.
  Selection never resizes/moves pixels or writes a file. Other modal actions
  cannot launch, undo or switch tools while choosing a rectangle.
- Brush, eraser, fill, line preview and picker work on the selected rectangle.
  Undo treats an entire fill/line as one operation, including any appended gfx
  rows. Omitted trailing rows read as zero and are added only when a nonzero
  pixel needs them. Original row bytes, line endings and other sections survive.
- **Приблизить** shows up to 16×16 pixels; the viewport follows the cursor.
  **Весь спрайт** fits the complete rectangle without changing its aspect ratio.
  Both are reachable in normal D-pad tool navigation. **Рамка** selects another
  rectangle; **Лист** returns to the eight existing shortcuts. Region, cursor,
  zoom and a pending line survive process recreation as optional UI preferences.
  An unfinished choice of corners is not restored; the last confirmed area is.
- `SpriteRegion` and `P8Graphics` are portable and cover the full 128×128 sheet,
  including non-tile-aligned rectangles. Lower-half sharing is explicitly
  exposed by `sharesMap()`. This UI deliberately selects only the upper 128×64,
  in 8-pixel increments, until map-aware editing is designed. These are current
  PIKOOS editing restrictions, not PICO-8 limits.
- A rectangle is a view of existing sheet pixels, not a new owned resource or
  reservation. Overlapping selections see the same pixels. Blank margins are
  not automatically allocated. The legacy New/Copy/Assign operations remain
  confined to the eight 16×16 shortcuts in 0.0.6. Version 0.0.7 adds the large
  binding below. Resource naming/copy placement and shared-map editing remain
  future work.
- The gfx decoder currently accepts complete 128-hex-character rows with LF
  or CRLF (and an optional final newline); unsupported row layouts are refused
  without rewriting the source. Version 0.0.9 also accepts absent/empty gfx and
  hero-free projects; general cart import remains pending.

Checked against the [official 0.2.7 manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html):
128×128 sheet, 8×8 tile addressing, lower-half map sharing, `spr` dimensions in
tile units and `sspr` pixel rectangles. The separate ordinary-cart
[`sprite_regions.p8`](../runtime-smoke/sprite_regions.p8) exercises a 32×24
image through both APIs. See [device evidence](../../docs/design/android-regions-05/README.md).

### Hero image and collision body (0.0.7)

- On a rectangular area, **Герою** is the last control reachable with D-pad.
  It previews the chosen image, its source size, a yellow collision rectangle
  and the ground line. The initial body encloses all nonzero pixels; an empty
  image cannot be assigned. It is a rectangle, not per-pixel collision.
- A commits image and body in one durable save/undo entry. B cancels without
  writing. Start commits then launches the exact saved snapshot. Other actions
  are trapped while previewing. Failed writes retain the preview for retry.
  Process recreation rebuilds that uncommitted preview from the selected area.
- The first region assignment converts only the exact owned `_init` /
  `_update60` functions and draw call. It adds `hero_sx`, `hero_sy`, `hero_sw`,
  `hero_sh`, `hero_left`, `hero_top`, `hero_w`, `hero_h` to a marked block of
  ordinary Lua. The block is source code, not a sidecar requirement or syntax
  extension. All resource bytes and unrelated Lua text are retained.
- In this version of the template, `x,y` locate the collision body's top left.
  `sspr` draws the image at `x-hero_left,y-hero_top`. Spawn preserves the old
  horizontal centre where possible and places the body on the ground at 101.
  The visible/body edges constrain horizontal movement; platform overlap and
  landing use the saved body width/height. The template still has one-way
  platforms, not solid-wall or arbitrary-shape physics.
- Painting an assigned image changes its pixels but leaves the saved body
  fixed. Reassigning explicitly proposes a fresh box. A small image can be
  assigned again after a large one, using the same preview. Existing carts
  open byte-for-byte unchanged; the older card-to-card assignment retains its
  original behavior until the user enters this new binding workflow.
- The workshop opens the actual assigned region; scene illustrations and shelf
  covers show it with its saved offset. Shortcut cells intersecting an assigned
  region (including empty margins) are unavailable to automatic New/Copy.
- This remains a narrow owned-template adapter. Modified movement functions,
  ambiguous bindings and initial collisions with generated variable names are
  rejected. It does not infer custom palettes, arbitrary Lua, other physics,
  animation anchors or manually adjustable bodies. Color-0 transparency is the
  template convention, checked against the official manual's `palt`/`sspr` API.

See [device captures, portable tests and runtime physics checks](../../docs/design/android-hero-06/README.md).

Parameters are drafts until confirmation; switching tools cannot silently
discard an active draft. Writes use AtomicFile and update the in-memory model
only after persistence succeeds. Undo holds the last 32 edits for the current
process; it is not persistent history. Project bytes and the current tool,
selection, cursor, color and pending parameter draft survive process recreation.

The scene is explicitly labelled an illustration, not an official runtime
frame or a simulator. It uses the actual editable sprite. Start runs the
saved cart in the official runtime. A/B mapping affects the workshop only;
the external wrapper currently owns in-game mapping and exit controls.

During automated runtime-return checks Ctrl+Q is injected over ADB. The upstream
wrapper maps physical Select to intent-session exit, but synthetic Select
events did not prove that path; physical exit ergonomics remain to be checked.

On 2026-10-02 the Retroid Pocket Classic demonstrated repeated complete
launch/exit cycles with speeds 2 and 3, return to the host, and restoration
after its background process was killed while the runtime continued. The
saved cartridge differed from the fixture by one byte. See
[device evidence and open work](../../docs/ANDROID_RUNTIME_POC.md).

## Honest limitations

- Detection establishes wrapper installation, not a valid imported runtime.
- External backend cannot force-stop the wrapper or observe its exit code;
  returning to the host does not mark a cartridge verified.
- Upstream 1.6.6 warm restart failed in the baseline. Initially test complete
  exit/relaunch cycles. Returning Home while leaving the runtime alive may
  still trigger that upstream failure on the next launch.
- Runtime import remains upstream's flow. This experiment does not choose a
  production bootstrap or establish broad Android/Linux compatibility.
- Two cold launches on 2026-10-02 failed during PulseAudio connection, before
  a game frame; a later attempt ran. See the runtime report. No automatic retry
  or success indication hides this backend limitation.
- Physical ergonomics, held axes and hardware mapping need real controller
  acceptance. Injected Android key checks do not establish those properties.
- Canvas accessibility semantics and broader viewport/device coverage remain
  future work; this is not yet a production accessibility implementation.
