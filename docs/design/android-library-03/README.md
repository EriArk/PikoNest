# Project shelf / Android lab 0.0.4

Real ADB captures from Retroid Pocket Classic, 2026-10-02, 1240×1080.
No generated mockups or compositing. Shelf covers are intentionally drawn
illustrations using each project's assigned sprite, not runtime screenshots.

- [Shelf](shelf.png): original Moon Garden, a new template and its independent copy.
- [Creation](create.png): description and controller confirm/cancel before writing.
- [Official runtime](official-runtime.png): the copied project running in user-owned PICO-8 0.2.7.
- [Return](returned-editor.png): copied project restored to image 3, cursor 8,7.

Verified with injected Android controller keys (not physical ergonomics acceptance):

1. Existing project backed up before update. Cancelled New; no directory created.
2. Created `garden-0001` from the clean template. Its initial bytes matched the
   fixture; changed speed to 3 without touching the original.
3. Copied Moon Garden to `remix-0001`: initially byte-identical, including both
   cats and the star. Changed only its speed to 4.
4. Reopened projects with independent selections and cursor positions. Original
   returned to its third sprite-sheet cell; copy returned to the canvas at 8,7.
5. Started the copied cart in official PICO-8; this attempt succeeded. Launch
   snapshot hash matched the copy. Killed background host PID 16558; Ctrl+Q
   returned to a recreated host PID 16797 with the correct project and cursor.
6. Browsed another cart without opening it, focused New, backgrounded/killed
   host and reopened (PID 16898). Shelf selection and action focus returned;
   the original remained the active editor project.
7. Installed final build preserving data and checked shelf/create rendering.
   Left the device on the shelf with the original selected.

Final device cart SHA-256 values:

| Project | SHA-256 |
|---|---|
| Original, unchanged from pre-update backup | `e9c5161d8c52d9dc3648ff018c336ca0a85a219c369a375958e9f0ab7682f59b` |
| New game, speed 3 | `df4bf0636f39d1621983eb4561056b7a12547a07d4c3c95aad950b343a920d8c` |
| Copy, speed 4 | `a2af77fb51d6627ef3b6130af6ab60eb416aa0f8405d06175bc51fa556d5d59c` |

Portable validation: LabCartridgeTest passed; WorkshopTest 799 checks,
SpriteWorkflowTest 2804, LibraryWorkflowTest 28. Library tests include source
isolation, clean template vs copy, cancellation, failed creation/open, corrupt
carts, unsafe IDs, fresh disk reads, CRLF/unknown section preservation and
controller navigation. APK compilation, packaging and signature verification passed.

Final installed APK SHA-256:
`51fead1bb7c6ec599e4b53fa75c597d1129a5e3b37d1a71cd2b5f8825d280a7c`.
Runtime/return captures precede the final shelf-only selection-marker/cover
rendering adjustment; runtime and workshop code were unchanged by that adjustment.

Limitations: one starter template; automatic names; no arbitrary import,
blank-project editor, export or persistent undo. UI metadata remains app-private
preferences. All-new viewport and physical held-button checks remain open.
One successful runtime launch does not resolve the previously observed wrapper
startup failures or prove audio quality. See [runtime report](../../ANDROID_RUNTIME_POC.md).
