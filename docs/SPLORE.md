# Splore and the Play shelf

Source lab 0.0.93 connects the official browser and its downloaded games to Play.
Real download, shelf return, favorites across restart and offline launch were checked on Retroid Pocket Classic with PICO-8 0.2.7 on 2 November 2026. See [device evidence](release/beta1/DEVICE_ACCEPTANCE.md).

From Play, open **Menu → Splore: discover games**. Wide layouts also have a Splore
button beside Workshop. It uses the purchased runtime already prepared in PikoNest.
No game, project or connected Games folder is required. Runtime errors return to a
retryable Play state; active/unknown sessions and ongoing restoration block a new
launch. Select/Back uses the same runtime exit menu and session-return contract.

After returning, Play refreshes both the chosen Games folder and the active runtime's
`data/bbs` tree. Downloaded `.p8.png` files appear as **Splore downloads**, with their
cartridge labels and a title from sibling `.nfo` metadata when available. The official runtime also writes `temp-<name>.nfo`; its title is used only when its `lid` exactly matches the cached cart revision. Metadata
without a downloaded cart and incomplete-file suffixes are not playable entries.
An unreadable Games folder does not hide otherwise available Splore downloads.

Favorites and recents use a stable relative cache ID, including across backup/restore.
Launching a download passes a validated relative path to the existing runtime home.
It neither imports the cart into Workshop nor moves it into an isolated temporary
directory. The original cart, cache, metadata and other cached parts remain in place.
The complete current runtime home is included in [backup](BACKUP_RESTORE.md).

The official manual documents built-in `-splore` startup and persistent downloaded
cartridges. It also explains that multicart parts need a connection on their first
load. [PICO-8 manual, Splore](https://www.lexaloffle.com/dl/docs/pico-8_manual.html).
The cache placement and temporary metadata were checked against the actual purchased runtime on the device. No alternative BBS scraper/downloader is introduced.

## Current bounds

- Indexing is read-only: Splore allows 512 carts / 8,192 entries / depth 16,
  with at most 32 MiB of label pixels. External providers retain their separate
  128-cart / 2,048-entry budget. A limit warning keeps other cached games
  accessible through Splore; it does not delete them or claim a complete index.
- Current cache paths use safe ASCII directory/filename components under `data/bbs`.
  Unsupported path/layout, malformed PNG or inaccessible data is not rewritten.
- Cached content is not a promise that every later multicart dependency is already
  available offline. Test first-load and offline continuation separately.
- An arbitrary external Splore Downloads destination is not active yet. Its saved
  folder selection must not be confused with the working cache. External folder
  synchronization/conflict review remains R07 work.
- Do not redistribute downloaded third-party carts/assets with the app. Their
  authors' terms still apply; the shelf is a view of the user's runtime data.

## Further acceptance

The previously uncached Ghoulish Garden 1.0 journey passed. Still test a multicart's next part, backup/restore of cache and recents, missing
metadata, a lost external Games grant and source preservation. Record actual
network/runtime results separately from the local synthetic-cache tests.
