# Original lab 0.0.94 journal fixture

Generated on 10 November 2026 using the **already compiled, unchanged lab 0.0.94
classes**, before compiling passage support. It is a real journal v12, not a v13
journal with its version header modified. It contains only original test data.

Base: `docs/showcase/0.0.94-en/maze.p8`, SHA-256 `abd2cf266865197ae360a99e87e6aad52a36c4d8d8e56251028e0cb8e6049c10`.
Fixture SHA-256: `4eb92265235589a9674556974299a609f68e9063d8928109932dfb8f9272828e`.

The generator opens GameCanvas/World, selects map cell (4,2), opens Wall / walkable
here, confirms Make walkable, undoes that change, and reopens the wall review.
It stores `GameCanvas.encode()`, after asserting the first integer is 12.
The fixture retains the original wall, pending Redo and the focused wall review.
`PassageWorkflowTest` verifies restored walls, empty passage list, Redo/Undo and
byte-exact return to the saved cart.

Writer class SHA-256 (lab 0.0.94):

- GameCanvas.class: `23571bfbdb774f359d7bd26d1b5a7f5484b1bcc80a19c9b21c6eadbd9c57dc66`
- WorldEditor.class: `b97644a28fc99b8b206bb07f34bd11832ef2455266f473a761cf300ddc5fb55e`

See [lab 0.0.94 evidence](../../../../docs/showcase/0.0.94-en/README.md) for its
source identity and validation. No runtime binary or purchased content is present.
