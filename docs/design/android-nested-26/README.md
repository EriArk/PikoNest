# Nested Games library — Android lab 0.0.26

2026-10-03. Installed in place on Retroid Pocket Classic (Android 14); existing
runtime adapter revision 2 and user-supplied official PICO-8 0.2.7 retained.
Host APK SHA-256:
`13d70fa31692fdbe31b53909a27e45fc30c6411839bf1809dfb496994362629e`.

## Behavior

Play reads the selected Games folder and its subfolders into one shelf. Each card
shows its relative folder (or “Корень папки”) and cartridge format. Same-name
carts sort by title, folder, format and opaque ID; selection, favorites and recent
launches use the unchanged document URI, never the display name or list position.
Moving/renaming a document may change a provider's ID; metadata migration for that
case is not implemented.

The portable `GameTree` walks breadth-first, deduplicates opaque document IDs and
closes each listing before opening the next. Limits apply to the whole scan:
128 carts, 2,048 entries and 16 nested directory levels. A limit shows a partial
result notice. An unreadable child increments a visible warning count while other
folders remain available; an unreadable root offers reconnection. Source files and
stored favorite records are never deleted by a scan. Each cart read remains capped
at 2 MiB; launch rereads and validates the selected source.

Leaving Play/replacing its scan/destroying its Activity invalidates the generation.
The worker checks cancellation between cursor rows, folder opens and cart reads.
This is cooperative cancellation, not an interrupt/timeout for a blocked provider
query or stream. Remote providers and large real-world libraries remain untested.

## Device checks

Added copies of the repository's owned Little Lights fixtures at
`Games/Puzzles/Little-Lights.p8` and
`Games/Puzzles/Chapters/Little-Lights.p8.png`. The four existing root files remain.
Six carts appeared, including same-name root and nested variants.

1. Selected nested text using D-pad events, favorited it with X and rescanned
   with L. Selection and favorite stayed on the same nested document.
2. Start launched that text cart in official PICO-8. Made one puzzle move and
   exited with Ctrl+Q; returned to its selected shelf card.
3. Launched the two-level PNG, made one move, then killed the background host
   using `am kill`. Its PID was absent. Runtime exit recreated the host at the
   selected nested PNG, retaining the separate nested-text favorite.
4. Y's favorites filter displayed root and nested Little Lights as independent
   entries. PNG and text recency persisted under separate document IDs.
5. Temporary size overrides checked 720×1080 and 1080×720 layouts and Activity
   recreation. Original physical 1080×1240 size restored, no override remains.
6. All 15 existing project hashes and all four original Games hashes matched
   their baselines. The two new nested copies also remained byte-identical to
   the repository fixtures. No project import, runtime replacement or launcher
   reconfiguration was required.

These use injected controller actions and held keyboard runtime equivalents;
they do not replace physical-button ergonomics acceptance. An earlier Home/kill
attempt did not remove the still-foreground host, so only the confirmed
background PID-loss check above counts as process-restoration evidence.

## Screens

- [Selection restored after host process loss](restored.png)
- [Same-name favorites from distinct folders](favorites.png)
- [Nested PNG running after one move](png-runtime.png)
- [Narrow layout](narrow.png) and [wide layout](wide.png)

## Verification and next boundary

All portable host suites, APK compilation and signature verification passed.
`GameTreeTest` adds 15 checks for nesting, case-insensitive extensions, duplicate
IDs/cycles, missing folders, global entry/cart/depth budgets, cancellation/closing,
stable same-name ordering, selection and independent favorite/recent filtering.

This adds discovery, not dependent-file staging. Includes, multicart and other
linked files are still a separate runtime/storage workflow; conservative text
hints remain unchanged, and PNG Lua dependencies are not decoded. Next work is
dependency-aware staging, then integrated first-run runtime setup. The limits here
belong to the PIKOOS experiment, not to PICO-8.
