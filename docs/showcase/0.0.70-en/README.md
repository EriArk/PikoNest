# PikoNest 0.0.70 — one Android application

Six original English screenshots from Retroid Pocket Classic. Local development
APK, one PikoNest launcher activity, approved interim target SDK 28. No public APK
release. [Capture metadata](capture.json) · [Architecture and limits](../../SINGLE_APK.md).

| Image | What it demonstrates |
| --- | --- |
| [Runtime setup](01-runtime-setup.png) | Correct purchased archive guidance and preparation in PikoNest |
| [Play](02-play-game.png) | Moon Garden rendered by the official runtime inside the main app |
| [Game menu](03-game-menu.png) | English continue/exit menu |
| [Resume](04-resume.png) | Return to the existing game after Home |
| [Workshop Test](05-workshop-test.png) | Saved Copy 29 resources rendered by official PICO-8 |
| [Workshop return](06-workshop-return.png) | Same project, screen and selected camera after normal exit |

The old helper was disabled during Play and Workshop tests. Its data and the
independent io.wip.pico8 app were preserved. This is an in-place update, not a
clean-device test. All 48 existing project/library files remained byte-identical.
Home/resume preserved the exact session token, PID and process-start identity.
Workshop Test ended with EXITED and returned to the selected camera. These checks
used ADB input; they do not certify physical button feel or game progress/saves.
The console remained muted.

The full portable suite passed before packaging corrections; focused archive,
recovery and session checks passed against the final sources. The APK signature,
263 Godot indexed assets and absence of purchased runtime filenames in both APK
and bootstrap payload were checked. No APK, user archive or private backup is
included here.

The observed old UNKNOWN gate came from a host token different from the adapter's
latest EXITED record. Explicit idle recovery now preserves that journal and marks
the expected token INTERRUPTED. What originally replaced the journal remains
unknown; crash/boot recovery is not claimed complete.

Remaining gates: clean installation, full old runtime-data migration, interruption
and boot matrix, modern Android target, physical controller/owner acceptance and
complete third-party redistribution review. R09 and issue #9/#10 remain open.
