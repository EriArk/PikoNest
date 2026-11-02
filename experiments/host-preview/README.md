# Host-rendered UI previews

`py -3 experiments/host-preview/render.py` compiles the current portable core and
production View/Surface files against small Java2D drawing adapters. It renders
original demo cartridges and configured editor states at compact, square-ish,
tall and wide sizes. Output stays under `.local/` unless explicitly copied to a
documented showcase. Run from the repository with a JDK and Python 3.

The stubs supply drawing/input types only; they do not emulate Android, SAF,
Activity lifecycle, controller hardware or official PICO-8 execution. Fixtures
configure sample states directly. Always label the resulting files **host previews**.
Do not use these images as evidence that an Android journey passed.
