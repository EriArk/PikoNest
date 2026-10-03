# Play-first launcher — Android lab 0.0.22

2026-10-03. Retroid Pocket Classic, actual app/runtime captures. Play is the
default home; Workshop projects have a separate entrance. No editor import is
needed to play a supported text cartridge from the connected Games folder.

Installed versionCode 22 / 0.0.22. Final APK SHA-256:
`f896c7845403b94f9f3a81191c12e847d2cfe49b77b6c07c27b935c268be585a`.

## Screens

- [Play home, actual Jelpi label and neutral covers for unlabeled carts](home.png)
- [Jelpi in the user's official runtime](jelpi-runtime.png)
- [Return to the selected cartridge](returned.png)
- [Recent launches](recent.png) and [favorites](favorites.png)
- [Separate workshop project shelf](workshop.png)
- [Missing file detected at launch](missing.png)
- [Selection after cold restoration](restored.png)
- [720×1080 two-column layout](narrow.png) and [1080×720 wide layout](wide.png)

The square-ish normal screenshot is 1240×1080. Temporary Android size overrides
exercised narrow/wide layouts and Activity recreation; original display size was
restored with `wm size reset`. This is layout evidence on one device, not device-
matrix acceptance. Favorite markers are drawn with palette pixels, not emoji.

## Verified workflow

Prepared three files in the previously empty `Documents/PIKOOS/Games` folder:
owned Little Lights and Moon Garden samples, plus the user's official Jelpi demo
already available in the local round-trip corpus. The demo/runtime binaries are
not added to this source repository. The launcher only read these originals.

1. Opened Play, marked Little Lights favorite and launched with A. Official PICO-8
   displayed the correct puzzle. While it was running, `am kill` removed the host
   process (PID absent). Exiting runtime restored Play, the selected cart and
   favorite. No editor project was created.
2. Added Jelpi, used L to rescan, selected it and launched. Its ordinary `reload()`
   works from the staged current cartridge. The actual 128×128 `__label__` also
   appeared on the shelf. The initial overly broad dependency hint was corrected
   to allow empty `reload()` and covered by a regression check.
3. Opened the runtime pause menu and selected SHUTDOWN. Returned to the same game.
   Y showed recent launches in descending order, then favorites. X metadata
   survived recreation. Recency means accepted launch, not an observed game result.
4. R entered the existing project shelf; L returned to Play with its filter and
   selection. A missing Moon Garden file (temporary rename of this turn's own
   fixture) produced a read error before launching. Restored it in a `finally`
   block; L rescanned successfully. Launch therefore does not trust cached bytes.
5. Home/kill/relaunch and installation updates preserved selection and favorites.
   All fifteen pre-existing project hashes matched before/after. All three source
   game files matched their local originals, including after runtime testing.
6. Opened folders from Workshop, killed/recreated the host twice, then returned
   to the project shelf and Play. This guards against reusing a missing View after
   restoration; returning to Workshop reconstructs its shelf when necessary.

Host controls were injected semantic key events; runtime pause/selection used
held keyboard equivalents (Enter/arrows/Z, and Ctrl+Q for the first return).
Complete physical-controller exit ergonomics and button mappings remain for
device acceptance; these checks do not claim a new runtime input implementation.

| External game | SHA-256, unchanged after play |
| --- | --- |
| Little-Lights.p8 | `99a38a85fa19403c715e61ddee8c68ec19db269cfbeee0a3a9c75308eb258903` |
| Moon-Garden.p8 | `1afbcdf44e942ab54fb6afd9daa52fdaf470adca02ff56430aa8c7616e08af76` |
| Jelpi.p8 | `008bfd81b11296fb3c131f8a152bb6195b770bbcac582a1ffdf4983959561813` |

## Scope and checks

`PlayWorkflowTest`: 16,411 checks covering ordinary/no-gfx/hero-free text framing,
input preservation, all label palette pixels, unsupported PNG/dependency hints,
selection, favorites with failed persistence, recent ordering, filtered removal,
double-launch guards, escape routes and empty lists. All prior core suites, APK
build and signing verification pass. Portable tests do not certify all Lua/game
behavior; official runtime remains authoritative.

One folder level, up to 128 games/2,048 entries, 2 MiB per file. The screen reports
subdirectories and truncation. PNG launch is not yet connected. Includes and
direct file-call hints are conservative (can match comments/strings or shadowed
functions); indirect calls are not fully analyzed. General dependency/multicart
staging remains open. Unsupported/unreadable entries stay visible. A failed scan
does not delete favorite records or original files. Remote providers and very
large folders have not been device-tested.

Cover behavior and single-file limitations follow the [official manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html):
cartridge labels, relative includes and runtime file-loading APIs are ordinary
PICO-8 features. Our launcher limitations are not console restrictions.

Next: format-aware `.p8.png` launch/cover support, then nested libraries,
dependencies and runtime setup. See [completion plan](../../ROADMAP.md#completion-path).
