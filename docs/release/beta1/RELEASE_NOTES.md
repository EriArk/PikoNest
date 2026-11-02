# PikoNest 0.1.0-beta.1 — draft release text

**Unpublished draft. Remove this preparation banner only after every beta gate
is met and the owner approves the actual APK.**

PikoNest is a small PICO-8 home and game-making workshop for Android handhelds.
Use Play when you want a game, and Workshop when you want to change something.

This first beta brings the player, editor and runtime setup together in one APK.
You supply your own purchased PICO-8 Raspberry Pi ARM64 ZIP; PICO-8 itself is not
included. The setup guide accepts compatible contents without tying import to a
particular ZIP filename or version number.

Play also connects to official Splore. Downloaded carts appear on the Play shelf
after return, with labels, available names, favorites and direct launch using the
existing persistent cache. External download-folder synchronization is still later
work; record actual network/offline acceptance before publishing this text.

The workshop includes sprite and map editing, animation, cameras, backgrounds,
rules, sound and music tools. Game view connects a supported subset into small
editable games. Start from Blank or one of two examples, use the same general
tools, Test in official PICO-8 and export an ordinary `.p8` cartridge.

Projects and reusable assets can use their own selected folders. A checked backup
ZIP covers active projects, reusable assets, persisted drafts/settings and runtime
saves/downloads. Restore uses a fresh internal copy and retains previous private
data for recovery; Android folder permissions and purchased runtime installation
must be re-established separately.

This is an **beta**. More ambitious gameplay can still require Lua. It is not a
complete no-code creator for every genre, a universal visual editor for arbitrary
Lua, or a promise of compatibility with every Android device/PICO-8 version.
Some legacy UI remains incompletely translated. Modern Android targeting, broader
library types and more complete genre workflows are planned after this milestone.

Before publication, insert the actual tested device/runtime matrix, SHA-256,
accepted known issues and matching dependency/source bundle links here. Do not
replace missing acceptance with old screenshots or host-test results.

See the [setup and candidate checklist](README.md) and
[data guide](../../BACKUP_RESTORE.md). Update an existing installation in place;
do not uninstall it to update. Keep your backup ZIP outside the app.
