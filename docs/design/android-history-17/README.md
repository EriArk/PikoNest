# Undo and redo — Android lab 0.0.18

2026-10-03. Actual Retroid Pocket Classic captures, 1240×1080. This is a bounded
history improvement, not completion or owner acceptance of the full editor.

## Interaction

Select opens the workshop menu with Undo focused. Left/right selects Отменить
or Вернуть in the first row; A confirms, following the existing A/B mapping.
Both actions show their available-step counts. Empty actions are muted and
report that there are no edits to undo/redo. Touch uses the same two buttons.
Y undoes; optional R2 redoes. Both shortcuts also work while the menu is open.
All remaining menu rows retain their prior positions and navigation.

History covers saved cartridge edits, including whole shapes, recoloring,
transformations, copies, inserted resource pixels and known template parameters.
It retains up to 32 edits per project in the current in-memory session. The menu
states this boundary. Project navigation and runtime return preserve the cached
history while the Activity survives and loaded cart bytes match. Activity/process
recreation starts at saved bytes with empty history; persistent history is future
work. Asset names/library saves, project creation/import and exports are separate
operations and are not undone by cartridge history. Cursor/selection are not rewound.

A successful effective edit replaces the redo branch. No-op drawing, cancelled
previews and failed saves retain it. Undo/redo save their target before publishing
the cart or moving either stack. Drafts consume redo without changing their base
cart, while Y keeps existing contextual behavior such as cancelling a pending line.
Only ordinary cart bytes are saved: no custom Lua or runtime metadata is added.

## Device evidence

- [Undo and redo both available](menu.png)
- [Restored saved sprite](restored.png)
- [Official PICO-8 puzzle completed after redo](runtime.png)
- [New process: empty history, saved sprite retained](empty.png)
- [Final build: controller focus on Redo](final-menu.png)

Created `remix-0007` (Копия 7) from `remix-0006` through the project shelf.
All twelve pre-existing project SHA-256 values matched before/after. Inputs were
ADB-injected controller key events and one touch check; physical ergonomics remain
for owner acceptance.

1. Recolored the selected 16×16 sprite from index 12 to 11, then its frame from
   10 to 9. Y undid the second operation. The menu showed one step in each
   direction; Right + A returned the frame edit with the original committed hash.
2. Two Y presses returned the exact copied source file. Two R2 presses replayed
   both edits, yielding the exact final file. No Lua, map or other sections changed.
3. Start launched the user's official PICO-8 0.2.7 runtime with the restored
   green sprites/orange frames. Little Lights completed in two moves; Ctrl+Q
   returned to the editor. Undid one edit, opened another project, then returned
   and redid it. Its saved bytes matched; the other project stayed unchanged.
4. Final-build review added Y/R2 handling inside the menu to match its displayed
   shortcuts. Rebuilt and installed in place. The new process showed zero counts
   and retained the final saved cart. Recolored 11→12 then 12→11; menu + Y undid
   the latter. Captured the final menu with counts 1/1. A tap on Redo returned the
   saved final file. Menu + Y, then menu + R2 also returned exactly those bytes.
5. Pulled actual source/result carts. The result matched the two expected portable
   recolor operations byte-for-byte: 58 gfx bytes changed and every unrelated
   section preserved. The device remains in Копия 7 with both final colors saved.

| State of `remix-0007/game.p8` | SHA-256 |
| --- | --- |
| Source / two undos | `b4fe208433fd5b4438b36d0f7fcf59786bfcfea1579d36eb8fbd0b4eb02b0996` |
| Green sprite, original yellow frame | `a0fc5311069903d0a819b4d445cba88d451b09055424f4ac62d1e0a418a62d54` |
| Green sprite, orange frame / full redo / final | `b25643e0987c37c938877a4ba74a97aab129a46ee800015dc2ecac8fbbf0e4e5` |
| Final-build temporary blue sprite, orange frame | `6476a822492dd113f487500b70bda51bea74009fd9b221cad52dde3fc8c3acba` |

Installed version `0.0.18`, versionCode `18`. Final APK SHA-256:
`cb5bf32139d8632379770a798e26c3bf00d7028db3c4197f8105928b262598e1`.

## Automated checks

`HistoryWorkflowTest`: 397 assertions over blank, Lights and Moon Garden carts,
with CRLF and an unknown section containing opaque bytes. Mixed-operation
sequences cover brush, line, outline/filled rectangles, recolor, rotation, copy
and template parameters; full backward/forward replay, runtime bytes, both menu
routes, empty history, no-op/cancel preservation, draft guards, failed undo/redo
and branch writes, retry, replacing the future, project/session isolation and
the 32-edit boundary. Failed writes use an injected port error, not device storage
exhaustion. All existing core suites, APK build and signature verification passed.
