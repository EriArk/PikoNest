# Persistent folder selection — Android lab 0.0.21

2026-10-03. Actual Retroid Pocket Classic, 1240×1080. Entry: Select on
the project shelf or its Папки header button. Four roles share a portable
`FolderSetup` workflow and an Android SAF adapter; no storage migration occurs.

Installed versionCode 21, versionName 0.0.21. Final APK SHA-256:
`010c748110a554b73edd8cc27fd58dab478dd510ef01f0fc45a13383e324fcc2`.

## Device evidence

- [Empty choices](empty.png)
- [Four verified folders](ready.png)
- [Cancelled replacement retains the previous location](cancelled.png)
- [Cold process restoration: saved locations, unverified access](restored.png)
- [Missing folder retains its recovery handle](unavailable.png)

Prepared empty `Documents/PIKOOS/{Games,Downloads,Projects,Data}` directories
through ADB for this check, then selected each through the actual Android picker
and its grant dialog. This does not prove wizard-driven automatic directory setup.
Games kept a persistent read grant; the three writable destinations passed
create/write/read-back/delete of the adapter's uniquely named probe. No files
remained in the four directories after checks. They are prepared destinations;
projects/assets still live in app storage and Splore still uses its prior backend.

Injected controller actions exercised shelf entry, role selection, recheck and
return. Touch selected directories in Android's picker and approved its grants.
Back navigates parent directories inside that picker before cancelling; one Back
is not always cancellation. Full physical-controller picker ergonomics remains
unverified. Cancelling a replacement retained the prior Games URI and name.

Home → `am kill` → absent PID → relaunch reported `LaunchState: COLD`.
All four choices and the selected role returned. They correctly showed Проверить,
not a stale green status. X verified all four persisted grants without another
picker. Reopening the screen also starts with unverified statuses.

The newly created empty Data directory was temporarily renamed to the checked,
unused `Data-access-check` sibling. X reported Нет доступа while retaining Data's
URI/name. A `finally` block restored the original directory; X then succeeded.
This tests disappearance/recovery, not actual SD ejection or permission revocation.

All fifteen existing project SHA-256 hashes matched before/after. No project or
asset migration, game launch, runtime import or Splore redirection was performed.

## Automated checks and limits

`FolderSetupTest`: 15 focused checks for cold/unknown status, picker non-mutation,
busy actions, failed replacement, denied recheck, reconnect, failed persistence,
shared tree roles, cancellation and ignored late completion. All existing core
suites, Android build and signature verification pass. Save failure/revocation
are simulated by the portable port, not claimed as device fault injection.

Provider calls run in a worker. Back can leave a slow check; late/stale Activity
results cannot publish settings. A hard kill during the probe could leave its
uniquely named `.tmp`; no generic cleanup scans delete unknown files. Remote
providers, read-only cards and reboot were not tested. Grant retirement, full
folder management/migration and first-run/runtime orchestration remain open.

This follows the [Android document-tree/persistent-permission contract](https://developer.android.com/training/data-storage/shared/documents-files).
URI handles are not converted into runtime filesystem paths. The displayed path
for Android's external-storage provider is only a user-facing label.

Owner clarified that everyday play is a primary use case. The next integrated
workflow is a dedicated Play shelf fed by the connected Games location, with a
separate Workshop entrance. See the [whole-product completion path](../../ROADMAP.md#completion-path).
