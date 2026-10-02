# Sprite selection and creation — Retroid, 2026-10-02

Actual 1240×1080 device captures from Android lab 0.0.3:

- [Sheet](sheet.png): original red-scarf cat, independent blue-scarf copy
  assigned to the hero, and a new yellow star drawn in an empty region.
  Yellow indicates selection; pink indicates hero usage.
- [Assigned image editor](assigned-editor.png): opened through the workshop's
  Hero image link, even while the sheet had selected the star.
- [Blank new image](new-blank.png): New opens an empty region for drawing;
  opening it does not change the hero or write pixels.
- [Official PICO-8](official-runtime.png): the assigned blue-scarf copy actually
  running, before the star was drawn. This is the official runtime, not the
  workshop's scene illustration.

The workflow was operated through injected Android controller key codes.
Physical controller ergonomics remain separate acceptance work. Final installed
APK SHA-256: `ed1261ff10c7f14e8c6c7be8659e7228d393336c377ddeded48bde771fdca51d`.

The device's original hero pixels, parameters and unrelated data were retained.
The final `.p8` differs from its pre-test copy at 122 byte positions, exclusively
within the two newly drawn regions and the hero's sprite-number literal.

See [runtime report](../../ANDROID_RUNTIME_POC.md) for validation and limitations.
