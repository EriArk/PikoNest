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
