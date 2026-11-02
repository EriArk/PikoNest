# Back up and restore your workshop

Implemented in source lab 0.0.93; local checks passed as recorded in
[the evidence](showcase/0.0.93-en/README.md). Android provider/lifecycle and real
restore acceptance remain pending. Do not use the unaccepted candidate as the
only copy of valuable work.

## Create a backup

Open **Folders**, then press **Y** (or tap the backup hint). Choose **Create backup
ZIP**, then select a destination outside PikoNest through Android's picker. Finish
any game, runtime preparation or folder migration first. Backup temporarily blocks
new saves/launches; cancellation leaves current work unchanged.

The app checks the current files twice, writes a manifest of file sizes and SHA-256
hashes, then reads the saved document back. Only an exact checked copy is reported
as verified. Keep that ZIP somewhere you can still access after uninstalling the app.
This ZIP is not encrypted and may contain private game data, source or tokens.

| Included | Not included |
| --- | --- |
| Active projects, `.pikoos` metadata and included source files | Original Games-folder collection outside the active runtime home |
| Active sprite/preset library and shared sprite buffer | Purchased PICO-8 executable/data and compatibility-layer installation |
| Persisted editor drafts, settings, favorites and unfinished import/export records | Android folder grants, transient runtime sessions or a running game's memory |
| Current runtime-home data: saves, downloaded carts and configuration | Logs, caches, inactive workspace/history directories and session-only Undo stacks |

If external project/library writes were interrupted, recovery must first resolve
them. An unreadable source stops backup instead of silently omitting it. Changed
source content/listing refuses success. The archive records the selected workspace;
old inactive copies are not silently merged into it. Empty directories are recreated
where needed, not archived as files.

## Restore a backup

1. Open **Restore from backup ZIP** and choose the ZIP. Validation and staging happen
   before any active data is changed. Bad paths, extra/missing/duplicate files,
   mismatched hashes, unsupported settings and oversized input are refused.
2. Read the review. **Keep current work** cancels activation. **Restore and close** rechecks the current files, records the request and closes the app.
3. Open PikoNest. The recovery screen applies the verified data. If interrupted,
   the next opening resumes; conflicting changes stop it with both versions kept.
4. On completion choose **Finish and close**, then open it again. This
   second restart makes Android reload the restored settings and drafts.
5. Reconnect Games and other Android folders through the guide. Import your purchased
   runtime if it is not installed. Restored projects/assets initially use internal
   storage; activate a new external copy explicitly if desired.

Restoration replaces the active private workspace, not the original external
folders. It does not merge two projects or re-enable saved provider IDs. Existing
private directories, pointer sidecars and preferences are retained under a separate
recovery transaction. A pending interrupted restore offers Retry or Restore previous
data. There is not yet a general post-success rollback/cleanup browser.

Settings refer to their archived cart data; drafts are not automatically committed.
Reconnecting an old external workspace instead can expose different source content,
so source-conflict checks still apply. Unfamiliar data is preserved, not converted
to the newest editor subset.

## Bounds and recovery limits

- At most 20,000 files and 512 MiB uncompressed; ZIP transfer cap 520 MiB.
- A local checked archive, readback/staged copy and retained old files need extra
  free space. A full disk is an error, not permission to delete original work.
- Interrupted/failed candidates are retained; cleanup is currently manual and must
  preserve any pending restore and original data. Repeated backups can use space.
- Hashes detect inconsistent/corrupted files; they do not authenticate a ZIP's author.
- Blocking Android document-provider I/O can delay cancellation. Native provider
  timing, process death and power-loss durability remain acceptance work.
- Do not uninstall/clear app data to recover a failed restore: private retained
  versions would be lost. Preserve the external ZIP and report the displayed error.

## Implementation evidence

`BackupArchive` and `BackupPreferences` provide portable bounded formats.
`BackupSwap` checks immutable old/new fingerprints before each rename and can resume
or reverse a partially applied transaction. Its fixed allowlist includes AtomicFile
`.bak`/`.new` siblings so stale pointers cannot reactivate old external locations.
Normal activity entry and runtime launch are gated while recovery is pending.
File application runs off the UI thread; no work is considered restored until the
transaction finishes. Local tests inject failures after each rename and exercise a
real unfinished Game view world draft. These tests are not Android acceptance.
