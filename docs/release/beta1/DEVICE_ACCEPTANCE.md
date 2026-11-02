# Early beta 1: device evidence

Recorded 2 November 2026 after the owner made the handheld available. This is a
bounded technical check, not physical-controller or novice acceptance. The owner
subsequently confirmed satisfaction and explicitly approved publication of this
exact candidate with the listed limitations. Release: `v0.1.0-beta.1`.

## Candidate identity

- Product APK: `PikoNest-0.1.0-beta.1.apk`, version code 93, 52,106,465 bytes.
- SHA-256: `e496a14cc5751232850fa76b393f99ce9b5fb88668c9dbb6a8317ffc2cda224f`.
- Retroid Pocket Classic, Android 14, ARM64, 1240 × 1080.
- User-supplied official PICO-8 0.2.7 Raspberry Pi ARM64 runtime.
- One product launcher; retained package ID and signing identity; debugging disabled.
- Public signing-certificate SHA-256:
  `b8a72962518f59c2585f68c9e0a189743068e0a76f6d7933f35cad53d4a8e008`.
- The obsolete separate `art.pikoos.runtimeexperiment` prototype was disabled for
  user 0 after independent integrated-runtime checks. Its private data remains
  intact; `adb shell pm enable art.pikoos.runtimeexperiment` reverses this. It was
  not uninstalled or treated as migrated/deleted data. Only the unified PikoNest
  package remains enabled; the temporary clean-validation package was removed.
- Android API 26 minimum / target 28. Other OS versions, chips and screen shapes
  have not passed this candidate's device matrix.
- System media volume remained zero. Inputs were ADB-injected keys and taps.

## Demonstrated journeys

| Journey | Evidence and scope |
| --- | --- |
| In-place update | Existing installation updated without uninstalling. A private pre-update backup was retained. A final UI-created backup verified 355 files; all 56 pre-existing project files and four library files match their pre-update SHA-256 values. Settings/runtime remained usable. This is not an exhaustive comparison of every preference/cache byte. |
| Runtime support refresh | Newly authenticated support files prepared from the retained purchased archive. Official runtime probe, games and Splore ran. Old prepared installation was retained. |
| Independent first opening | An empty, isolated validation application walked the four-step guide, skipped optional folders, rejected an x86 desktop ZIP, imported a renamed ARM64 ZIP and prepared/Tested it. The temporary validation app was then removed; it is not a product helper. |
| Error recovery | Final product APK visibly rejects a wrong desktop archive above the instructions. The previously prepared runtime still passes Test afterwards. |
| Splore to Play | A previously uncached **Ghoulish Garden 1.0**, by Pixelody, downloaded through official Splore. On return it appeared with its title and cover; 190 cached carts were indexed. Favorite survived app restart, then direct shelf launch reached its title screen with Wi-Fi disabled. Wi-Fi was restored. Multicart continuation was not tested. |
| External play and resume | Android VIEW intent launched an existing Jelpi `.p8` through LaunchActivity without editor import. Home → PikoNest → Resume returned to the game; the exit menu returned successfully. This used an ADB intent, not a fresh Pegasus/Daijishou UI run. |
| Blank authoring | In the clean validation app, created a blank cart, painted a 16 × 16 sprite, placed it, saved and added D-pad movement through UI. Test moved the sprite in official PICO-8. SAF export matched the saved cart exactly (2,949 bytes). No Lua editing was used. This is an art/movement loop, not a full win/loss game authored from Blank in this batch. |
| Collection starter | Created Little explorer in the final APK. Movement, collecting three stars, win and return worked. Export was read back and verified by the application. A diagnostic copy confirmed independent visibility flags/rendering; the suspected disappearing-star issue was a misread screenshot, not a defect. |
| Hero-free starter | Created Light puzzle in the final APK. Linked toggles changed lamp frames, all-lit win and restart worked in official PICO-8; returned to the editor. |
| Sound and music | Edited an SFX pitch, reviewed shared uses, saved and used Undo. Runtime SFX preview returned to the editor. Assigned a shared phrase to music pattern 0, set Stop, bound three music parts to game start, saved and ran its music preview. Audio remained muted; audible quality is unverified. |
| Backup and restore | Real Android document picker wrote and verified a 14-file clean-project backup. Restored in the isolated validation app, completed both reopen steps; restored cart bytes matched export. Final main-app backup separately verified 355 files. No destructive restore was performed over owner data. |
| Presentation fixes | Final native captures show distinct sprite thumbnails. Wrong-archive error is visible immediately. Seven new unmodified native screenshots are in the [gallery](../../showcase/beta1-en/README.md). |

Blank export SHA-256:
`c5ea3254b58eb56160f89b962e07e1b1ec0dfd1ac67f55e6d2ed77c0bc484658`.
Private backups, purchased archives and downloaded third-party cartridges stay
outside Git and release assets.

## Evidence boundaries

The 91 core suites passed before the last two Android-only rendering corrections.
Final packaging/inventory checks passed after those corrections; the affected
native views were inspected. The core suite was not rerun merely for UI packaging.

Splore/offline checks used candidate `7386342e715b4c1e3a8cde833bafd78f6c3cc4c79275593ece4c9689e43b5132`.
Direct cached-title launch was also repeated on the final APK with Wi-Fi disabled
and the obsolete helper disabled; it succeeded and returned to Play. Wi-Fi was restored.
External resume used `4c70bc69` (local intermediate). The final APK differs only in
the two view fixes: archive error placement and sprite thumbnail bitmap ownership.
Clean-install validation used the same core/runtime with an isolated debug namespace,
SHA-256 `4a0eb10cde9304fe95bf9d8a04a7685f4f737d82226e2dfa423cac5ae062d436`.
Final starter/audio checks, error recovery, main backup and gallery use the full
final product hash above. This does not claim every journey was repeated on every build.

## Still needs explicit acceptance or further testing

- Owner publication approval is complete for this exact early beta; the checks
  below remain deferred/unverified, not implicitly passed.
- Physical buttons/sticks, novice usability and audible playback.
- Fresh actual third-party launcher UI journey and content-URI variants.
- Other displays/orientations, Android versions and runtime versions.
- Lost document grants, provider disconnection/full storage and process interruption
  during real restore; unfinished-draft restore. Host fault tests are separate evidence.
- Broader world/tile draft edits and external library activation/reconnect on this build.
- Multicart first-download/offline continuation and arbitrary external Splore folder sync.

These open items must not be silently marked passed. The owner may explicitly
accept bounded early-beta coverage; later work stays in the roadmap.
