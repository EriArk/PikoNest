# PIKOOS UX study 01

**Accepted visual baseline; browser behavior remains a simulation.** Created
after the owner rejected the native runtime lab's appearance, then approved
this direction and its transfer to Android with controller-first operation.
See [`UX_DIRECTION.md`](../../docs/UX_DIRECTION.md) and the
[working native slice](../android-host/README.md).

Open `index.html` locally or run with Node (no dependencies):

```powershell
node experiments/ux-study/serve.cjs
```

Then open <http://127.0.0.1:8765>. The server binds loopback only.

## Reviewable behavior

- Cartridge shelf, empty state and lightweight creation/remix flow.
- Workshop speed -> corresponding Lua value -> simulated Test -> same tool.
- Explicit parameter editing, cancel and undo.
- Optional explanation tied to the real variable being edited.
- Sprite/color selection, single-pixel drawing and undo.
- Runtime-unavailable example and return to preserved workshop.
- Compact/square, widescreen and portrait layout presets.
- Keyboard, pointer and standard browser gamepad mapping to semantic actions.

All changes live in memory. This page does not edit any real project, import
files, play actual PICO-8, detect a runtime or save to disk. It clearly labels
this outside the app viewport. Pixel art is original illustration for this
proposal; it is not a screenshot from a running PICO-8 game. The runtime lab
and proprietary PICO-8 archives are not part of this page.

Not implemented here: free-form code editing, real symbol binding, continuous
brush strokes, zoom/pan, map/music editors, long-term persistence or runtime
integration. Proposed product behavior must still be evaluated and implemented
against the portable core and runtime boundary.

## Inputs

Arrow keys: spatial focus; value editing when active; cursor on sprite canvas.
Enter: Confirm. Escape: Cancel/Back. Q/E: previous/next tool. Space: Test.
Z: Undo. X: context help / next color. Tab: native focus; trapped inside dialogs.
Gamepad API uses standard mapping provisionally, not a Retroid-specific layout.
O/X hints represent semantic Confirm/Cancel, pending real device remapping.

## Evidence

On 2026-10-02, browser checks exercised edit -> code -> Test -> return, undo,
discarding a pending edit, drawing, creation, dialogs and horizontal overflow
at all three layouts. Screenshots are in ignored `.local/evidence/ux-study/`.
Code scroll restoration was also checked. Curated review images are tracked in
[`docs/design/ux-study-01/`](../../docs/design/ux-study-01/).
The study was inspected in CodexWeb's built-in browser. No physical-controller,
small-screen readability or owner design acceptance is claimed.

## Assets

- `app.js` scene/sprite graphics and inline pixel icons: original PIKOOS study.
- PICO-8 palette: [official FAQ](https://www.lexaloffle.com/pico-8.php?page=faq).
- `assets/Tiny5-Regular.ttf`: Google Fonts `ofl/tiny5`, downloaded 2026-10-02
  from <https://github.com/google/fonts/tree/main/ofl/tiny5>.
- Font SHA-256: `cb8168f80cfee2f47f6db59f2a7afbde31cdcdcdcf262e7a993e4d468a5bf4b0`.
- Font license is included as `assets/OFL.txt` (SIL Open Font License).

Tiny5 is a Cyrillic-capable pixel-font candidate, not a production typography
decision. The official PICO-8 logo and proprietary runtime assets are not used.
