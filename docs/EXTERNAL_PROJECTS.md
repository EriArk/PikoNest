# External working projects

## Related update — 0.0.91

Library storage now has its own external activation/reconnection path using the same
controls. [External library](EXTERNAL_LIBRARY.md). Project location remains independent.
The earlier private-only library statements below describe 0.0.90, not current scope.
Full backup/restore and native acceptance remain unfinished; handheld use stays paused.

Lab 0.0.90, 1 November 2026. Implemented locally; native acceptance is pending.

## Start and switch

Choose a writable Projects folder in Folders, then press Start / F5 on Projects
(or tap the left half of the bottom hint). Use selected folder copies the current
internal project files into a fresh `PikoNest-workspace-<UUID>` directory. The app
checks file hashes, project IDs/titles/cart bytes, tests the full save protocol on
the candidate, and tests file/directory rename support. It performs a final source
comparison before committing the location. Cancel or a failed check keeps the old
location; candidates and original files are retained.

Project mutations through the local adapter are gated during activation. This is
a process-level guard, not a filesystem snapshot against other applications.
Changing an already active external workspace to another external workspace is
not implemented: activation starts from internal projects only. Library storage,
preferences/drafts, game saves and runtime remain in their existing locations.

The location is one checked AtomicFile record. Interrupted publication uses its
prior complete record; after commit MainActivity opens the selected workspace.
A failed acknowledgement reports an uncertain selection and asks the user to reopen
storage controls; both complete workspaces remain available. Existing edit journals retain
their project IDs/source checks. Recreating MainActivity after a location change
prevents cached editor sessions from continuing against the previous backend.

## Normal editing

The SAF adapter implements the same ProjectRepository contract as local storage:
list/read/create/import/compare-and-save. Canonical saved carts are ordinary
`projects/<id>/game.p8`, with copied metadata/dependencies alongside. Creating a
project first checks files in a hidden staging directory, then renames that whole
directory to its final ID. Interrupted staging is retained and excluded from IDs.
No custom Lua, API or PICO-8 resource model is introduced.

Each changed save creates and reads back a complete candidate, persists a private
journal, renames the current cart to a unique `.pikonest-before-<UUID>.p8`, then
renames the candidate to `game.p8`. Old versions are retained; there is no cleanup
policy yet. Save readback must succeed before the editor publishes new state.
An error after provider commit can leave a complete new file despite a failed
acknowledgement; reopen the project for recovery rather than overwriting it.

Queries resolve current document IDs after renames. Providers lacking rename
support, or returning a different name than requested, are rejected. Rename flags
alone are not treated as successful activation: the candidate is exercised too.
Android does not grant this application POSIX filesystem transactions. The journal
provides recoverable complete versions within the app; provider/cloud durability
after power loss still needs real-device testing.
[Android document flags](https://developer.android.com/reference/android/provider/DocumentsContract.Document)
and [rename contract](https://developer.android.com/reference/android/provider/DocumentsContract#renameDocument(android.content.ContentResolver,%20android.net.Uri,%20java.lang.String)).

## Interruption and lost access

Before reading a project, recovery checks its pending journal. A complete expected
old or next cart is accepted and the journal retired. If `game.p8` is absent, a
verified retained old version is renamed back. Unknown current bytes, missing or
changed backups, inaccessible storage or failed verification block the operation;
versions and journal remain intact. This preserves unfamiliar user changes.

The load-error screen opens Project storage / reconnect. Choose the **same**
original Android tree to restore its durable grant. Back retries opening Projects.
Selecting a different tree is rejected. There is no silent fallback to stale data.
Use older internal originals explicitly returns to the retained internal workspace;
external projects and newer edits remain separate and are not merged or copied back.
Export external edits before returning if you need to carry them across.

Keep the app installed: workspace selection and interrupted-save journals remain
private. Uninstall/reinstall recovery, full backup/restore, automatic conflict
resolution, external-to-external migration and external-to-internal merge remain
unfinished. Some conflicts require inspecting retained files. The original copy
receipt describes migration-time bytes, not later edited contents.

## Acceptance still required

No console use was authorized. Native activation, permissions/revocation, SD loss,
provider failures/power loss, process recreation, actual save/Undo/reopen/Test/export,
visual/controller navigation and novice/owner review are unperformed for this batch.
Local tests exercise portable recovery and real editor/domain composition with a
simulated provider. Compilation is not device proof; ACC-06 stays open.
