# Verified copies to user folders

Lab 0.0.88, 1 November 2026. Local implementation; native acceptance is pending.

## User path

1. Open Folders, select Projects or Asset library, and choose a writable Android
   folder. Existing selected locations remain available.
2. Press Start / F5, or tap the left half of the bottom setup hint, to open Copy.
3. Choose New verified copy. The app creates a unique `PikoNest-projects-<UUID>`
   or `PikoNest-data-<UUID>` directory beneath that tree. No existing destination
   file is overwritten. Progress shows checked files.
4. On success, the destination contains `pikonest-copy-complete.txt` with SHA-256
   values and relative paths. The receipt itself is read back and checked.
5. Cancel retains originals and copied files. Resume last copy uses the saved tree
   and candidate document ID and rechecks existing files against the current source.
   A partial/mismatched file is retained and requires New copy. Reconnect a lost
   grant through Folders first. Changing destinations requires New copy.

Up/down and Confirm navigate actions; Back cancels active work before allowing
exit. Touch and keyboard use the same actions. The worker survives Activity
recreation; process loss leaves a durable candidate record for explicit Resume.
Physical input and actual Android provider/process-loss behavior are unverified.

## Included data and limits

Projects copy the files below `projects/`, including `game.p8`, import names and
on-disk `.pikoos` notes/metadata. Library copies `asset-library/` (current sprites
and parameter presets) and `sprite-buffer.pksp` when present. Empty directories
are not reproduced. Byte contents are copied without cart normalization.

Application preferences, unfinished editor drafts stored in preferences, runtime
archives/binaries, runtime homes, game saves and Splore downloads are excluded.
This is a verified file copy, not a full application backup or restore workflow.
Working storage stays inside PikoNest. Do not uninstall the app assuming this
operation preserved settings, saves or unfinished work. No external destination
is labelled active and the canonical working project does not move yet.

## Transfer contract

The portable VerifiedTransfer engine uses source/target streams and cancellation,
not Android URIs or inferred Linux paths. The SAF adapter treats tree/document IDs
as opaque handles. Relative paths reject escapes, control characters and ambiguous
case collisions. The local adapter anchors to a canonical root and refuses linked
descendants. A copy never deletes files, sources or earlier candidates.

Each file is hashed before copying, read back, and compared with a fresh source
read. A final pass compares all source/destination hashes and the source file list
before receipt publication. Interruption during a file may leave a partial candidate;
Resume refuses to overwrite it. Completed copies with missing files are rejected.
The destination and source should remain untouched during copying; this is not a
filesystem snapshot or a lock against another process modifying files afterward.

The journal records the destination before copying files; a failed journal commit
keeps any newly created empty folder. Old candidates are not cleaned automatically.
Provider durability after power loss is not implied by stream close/readback.

## Next storage work

ProjectStore, asset stores and save callbacks still use app-private File/AtomicFile.
Introduce a shared persistence port covering create/read/compare-and-save/list,
source conflicts and metadata. Then support staged external activation and reopening,
with durable switching, lost-access recovery and exact ordinary-cart preservation.
Do not implement activation as URI-to-path conversion or silent background mirroring.
Retain old data until complete verified activation. ACC-06/P08/R08 remain open.
