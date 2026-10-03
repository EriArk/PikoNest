# Android APK, device variety and external launch

2026-10-03. Owner-confirmed product requirements; implementation status below.

## Current delivery target

PIKOOS is currently developed and delivered as an installable Android APK.
Retroid Pocket Classic is the available physical reference device. Linux handhelds
are occupied by the owner's TrainerOS development: Linux implementation/testing
is deferred, not an Android completion gate. Portable core and replaceable
runtime/storage/input boundaries remain required.

## Adaptive handheld experience

The same app must accommodate square-ish, 4:3, 16:9 and wider displays, portrait
and landscape, different resolutions/densities and available window sizes.
Use available space to arrange panels, covers and contextual tools; preserve
readable pixel typography, visible focus and reachable primary actions.
Keep the game's square canvas proportional, with unused space or surrounding
controls as appropriate. Never stretch it to fill a different aspect ratio.

Controller actions remain semantic across D-pads, sticks, A/B label conventions,
shoulder-button availability and remapping. Provide reachable menu alternatives
for optional shortcuts. Test touch-enabled and controller-only workflows;
keyboard/mouse are optional. Android version, storage grants, chipset/GPU,
insets, suspend/resume and rotation are also validation dimensions.

Acceptance must cover Play, folders/setup, workshop, dialogs/errors and runtime
launch/return. Layout simulation on Retroid helps detect clipping and focus loss;
it does not prove hardware/runtime compatibility on other devices. Additional
physical Android devices can be validated when available. No Linux device is
needed for this stage.

## External launcher requirement

The owner wants to select a game in a third-party Android launcher and have
PIKOOS run it using the user's official PICO-8. The ordinary PIKOOS app icon still
opens Play. External play must bypass shelf selection and editor import, preserve
the original `.p8` / `.p8.png`, and reuse the same runtime launch workflow.

Proposed UX and integration contract, to prove before calling supported:

1. Receive one explicit game request; validate readability, format and supported
   dependencies using the shared launch workflow. Never forward arbitrary
   commands or caller-supplied runtime arguments.
2. If setup is complete, launch directly. If runtime setup or access is missing,
   explain the necessary action, retain the pending game where permissions allow,
   and continue after setup. Cancel returns without creating an editor project.
3. Track entry origin separately: Play returns to its selection; workshop tests
   return to the editor; external play finishes its entry flow to return to the
   calling launcher when Android task state allows. Offer Play as a fallback.
   Do not invoke an arbitrary caller-provided callback to implement return.
4. Handle cold/warm entry, a new request while the app is open, duplicate delivery,
   process recreation and already-running games without launching twice or
   silently replacing a live game/unsaved editor session.
5. Publish a stable entry-point contract and tested launcher configurations only
   after implementation. Verify at least two real third-party launcher integrations;
   an ADB intent alone is not interoperability acceptance.

Android intents and permission-bearing content URIs are the initial technical
direction, based on [Android intents](https://developer.android.com/guide/components/intents-filters)
and [file sharing](https://developer.android.com/training/secure-file-sharing/share-file).
Exact activity/action/MIME filters and path-based launcher adapters remain TBD.
Do not assume every launcher supplies the same request or that an absolute path
grants access. Scope any path adapter to existing authorized storage. Do not
register PIKOOS indiscriminately for all pictures merely because carts use PNG.

## Status and order

Lab 0.0.23 has a MAIN/LAUNCHER entry and an outgoing runtime share flow. It does
not yet receive external game launch requests. Existing narrow/wide screenshots
are layout checks on one device, not a supported-device matrix.

Next: stabilize the observed runtime warm-launch/audio lifecycle; then implement
and verify external launcher entry/return, followed by nested libraries and the
complete first-run runtime setup. Device/layout checks accompany each UI slice.
These are Android release requirements, not optional post-release Linux work.
