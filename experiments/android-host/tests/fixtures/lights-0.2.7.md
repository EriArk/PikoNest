# Official PNG export fixture

`lights-0.2.7.p8.png` is our own `../../assets/lights.p8`, exported by the user's
official Raspberry Pi PICO-8 0.2.7 running through the experimental Android wrapper
on 2026-10-03. No runtime binary or third-party game code is included.

Run the cart, capture its clean game screen with Ctrl+7, enter the console with
Shift+Escape, then `export /pikoos-tests/lights-clean.p8.png`. The command preserves
the source cart name. Pull the generated PNG without any image conversion.

SHA-256: `11bb25685b0811ae858c9a8f7265c96a83a0f1d253008c0e10d6b485ab8f791e`.
PNG dimensions 160×205, RGBA8, data version 43. Label starts at (16,24).
`P8PngTest` compares every cover RGB pixel against JDK ImageIO, independently
checks all five filter types, and rejects corrupt/unsupported/budget-exceeding data.
The PNG was also launched unchanged via PikoNest on the device and played.

The [official manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html)
documents PNG carts, label capture and EXPORT. The
[PNG specification](https://www.w3.org/TR/png-3/) defines chunks, CRCs and filters;
[picotool's formatter](https://github.com/dansanderson/picotool/blob/main/pico8/game/formatter/p8png.py)
is a reference for encoded channel ordering/version location. No formatter code
is copied. We do not decode compressed Lua or certify general cartridge behavior.
