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

Product UX and integration contract (lab coverage and remaining gates below):

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
Lab 0.0.25 implements the explicit entry and scoped path adapter described below.
Do not assume every launcher supplies the same request or that an absolute path
grants access. Scope any path adapter to existing authorized storage. Do not
register PIKOOS indiscriminately for all pictures merely because carts use PNG.

## Tested lab entry (0.0.25)

Explicit component: `art.pikoos.runtimelab/art.pikoos.runtimelab.LaunchActivity`.
It is separate from the app's Play/editor task. It accepts VIEW/MAIN with a
`content://` or `file://` data URI, SEND with a read-granted EXTRA_STREAM, or the
`rom` string extra used by path-oriented frontends. Prefer a permission-bearing
content URI. Raw paths only map to documents beneath the already authorized
Games folder; they never grant broad filesystem access. Internal storage aliases
and removable-volume paths are supported. No generic image intent filter.

The same bounded format/dependency checks and runtime backend as Play are used.
Source bytes are copied unchanged into the read-only runtime snapshot. External
play never opens/imports an editor project. Missing access offers the system file
picker, retry, Play fallback and cancel. A/B follow the host's swap setting.
Backgrounded reads are invalidated. A persisted dispatch journal prevents an
automatic second launch after process loss; a duplicate/new request during a
dispatched game resumes its wrapper instead of replacing the snapshot.

### Beacon 1.8.10

Create a Custom platform, choose PIKOOS Runtime Lab as player and the same Games
folder. Enable custom launch; use this exact command:

```text
am start -n art.pikoos.runtimelab/art.pikoos.runtimelab.LaunchActivity -a android.intent.action.VIEW -d {file_uri}
```

The device profile is named **PICO-8 - PIKOOS**, short name **PIKOOS**. Existing
PICO8/RetroArch and other profiles remain. Beacon's placeholder syntax follows
the wrapper's [frontend documentation](https://github.com/Macs75/pico8-android/wiki/Frontends-Integration).

### Retroid Launcher beta 1.16 (2025-0618-1139)

Create a custom PIKOOS platform/configuration:

| Field | Value |
| --- | --- |
| Package | `art.pikoos.runtimelab` |
| Activity | `art.pikoos.runtimelab.LaunchActivity` |
| Action | `android.intent.action.VIEW` |
| DataType / DataFilePathType | Leave empty |
| Extra key / value | `rom` / `{file.path}` |
| File suffixes | `p8`, `png` (without dots) |

Add the Games directory, select it in Synchronize and scan. These fields were
confirmed against the installed launcher and actual launches. Retroid extracts
the final extension, so `p8.png` is not a suffix here; unrelated PNGs can appear,
and PIKOOS still validates their format. Unknown homebrew metadata may report
matching failures even though the game files were indexed. Choose **Safely Open**
when Retroid reports the PIKOOS process already running. Force Open is unnecessary
and is not part of the tested workflow.

Both real launcher shelves now reach official PICO-8 and return to their selected
cart on the reference device. See [device evidence](design/android-external-24/README.md).

## Status and order

Lab 0.0.25 proves the external-launch slice for single `.p8`/`.p8.png` carts in
Beacon and Retroid Launcher. This is an experimental package/entry contract;
production naming and distribution are not frozen. Missing-runtime setup still
explains the requirement rather than completing an integrated import wizard.
Linked carts/files, runtime crash observation and reliable stale-session recovery
remain open. Interrupting native startup with another entry also exposed a
wrapper hang; see the evidence above. The wrapper cannot report a verified game result; returning is only
an Activity lifecycle event. Existing narrow/wide screenshots are layout checks
on one device, not a supported-device matrix.

The reproduced audio startup race is fixed in a separate test runtime adapter;
this two-APK development setup is not final product packaging. See the
[runtime experiment](../experiments/runtime-restart/README.md).

Next: fix interrupted-startup recovery, then nested libraries and dependency-aware
staging, followed by complete first-run runtime setup. Device/layout checks accompany each UI slice.
These are Android release requirements, not optional post-release Linux work.
