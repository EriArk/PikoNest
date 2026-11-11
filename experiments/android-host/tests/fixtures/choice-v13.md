# Original lab 0.0.95 interaction-menu fixture

Generated 11 November 2026 using retained, unchanged lab 0.0.95 compiled classes,
before pressure-plate support. Real version 13 output, not a modified version header.
Base: `docs/showcase/0.0.95-en/switch-passage.p8`.
Base SHA-256: `244969c473354818033859f73f55d52e36053b949cc370eaacf365dd314e308c`.
Fixture SHA-256: `8a2d04c80ac707cf89b9560ee43106f445a782c56299655ef0d9ddbcfca9a26d`.
Writer GameCanvas.class SHA-256: `d9c17d7958cbcf8f7c475e8180e1b89fad28cc7f9b1bf3bb99cc71a70dcc4caf`.

Construct GameCanvas from the base, select element 1, call `configure(true)`, focus
row 1 and confirm its interaction picker. Focus index 5 (old Cancel), assert the
label and version integer 13, then save `encode()`. PlateWorkflowTest restores this
journal in version 14 and confirms Cancel still cancels, preserving the toggle,
instead of selecting the newly inserted Pressure plate item. Only original test
data is present.
