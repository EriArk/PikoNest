# Official runtime smoke test

`runtime_smoke.p8` is an original, ordinary PICO-8 text cart for the Android
runtime investigation. It uses no PIKOOS APIs and requires no sidecar data.

- The border and animated bar reveal cropping, stretching or a frozen frame.
- The D-pad moves a green dot in the outlined area.
- L/R/U/D/O/X count press edges separately; holding does not auto-repeat counts.
- O plays four notes and X plays the last two, at moderate note volume.
- Timer and counters expose state loss across background/resume or sleep/wake.

For physical acceptance, press each direction and each primary face button
individually. Note the physical labels that produce O and X; one press must
not increment both counters. Confirm audible sound on the device. Then test
Home/resume and sleep/wake and repeat the controls.

The external wrapper is a research tool; this cart does not imply that PIKOOS
has implemented a production runtime backend. See the execution plan and findings in
[`ANDROID_RUNTIME_POC.md`](../../docs/ANDROID_RUNTIME_POC.md).

`sprite_regions.p8` is a separate rendering check for Android lab 0.0.6. Its gfx
is the exact sheet exported from the test copy after drawing a 32×24 cloud at
64,24 through the workshop. Only Lua was replaced with a diagnostic display:
`spr(56,48,27,4,3)` and `sspr(64,24,32,24,32,70,64,48)` draw the same region at
native size and 2×. It uses ordinary PICO-8 data/APIs, with no sidecar or bridge.
Observed in the owner's official 0.2.7 runtime; see
[captures and limits](../../docs/design/android-regions-05/README.md).

`hero_physics_checks.p8` uses the exact saved 0.0.7 hero code from the test copy,
with diagnostic wrappers appended to Lua. At startup it calls the generated
movement function and asserts binding dimensions, spawn, floor contact, both
screen bounds, platform landing, upward passage, edge miss, respawn, a taller
body and a narrower body: 11 checks. Physical controls must be released during
startup. A green success banner appears only after every assertion passes;
the yellow body overlay is diagnostic-only. This cart is separate from the
user's project and the production template. It ran in official 0.2.7; see
[evidence](../../docs/design/android-hero-06/README.md).
