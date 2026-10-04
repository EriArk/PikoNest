# Include preparation — Android host 0.0.27

2026-10-03. Installed in place on Retroid Pocket Classic, Android 14. Runtime
adapter revision 2 and the user's official PICO-8 0.2.7 are unchanged.
Host APK SHA-256:
`447741f3da16e08ad0004593fb97cdf8b19d87d065fdf330c033ed466c0b6b51`.

## Actual runtime comparison

The owned fixture at
`experiments/android-host/tests/fixtures/include-garden/Include-Garden.p8`
uses three native include forms:

- `code/palette.lua`: ordinary Lua from a descendant directory;
- `shared.p8:1`: only tab 1; tab 0 deliberately asserts if included;
- `behavior.p8`: all code, split into two tabs in the final fixture.

Copied that directory into `Documents/PIKOOS/Games/include-garden` for device
testing. First launched the unchanged source directly through the wrapper's raw
file entry, so official PICO-8 performed its own includes. Then launched through
PikoNest's external LaunchActivity and through Play, exercising SAF preparation.
The temporary snapshot contained ordinary Lua and retained no include directives.

The initial direct, external-entry and Play screenshots were byte-identical. After
splitting behavior into two tabs, repeated direct/Play runs still matched exactly.
Both final 1240×1080 captures have SHA-256:
`12e467c18661baeadcff654a802b4fecb8f53c87f7ebfc3c23b7bffcac8eecec`.

- [Original sources, official native include processing](direct.png)
- [Prepared cart through PikoNest](prepared.png)
- [One input changes the flower and move counter](recovered.png)
- [Missing dependency reported before runtime launch](missing.png)
- [Return to selected Play card](returned.png)

This is fixture-level compatibility evidence, not a general Lua equivalence or
budget validator. External entry was exercised using an explicit VIEW intent with
the same scoped file path contract as the installed launchers; their catalog UIs
were not rescanned/reconfigured in this step. Runtime input used held keyboard
equivalents, host navigation used injected D-pad/Start events.

## Failure, recovery and preservation

After a successful Play run, temporarily renamed only this test's `palette.lua`.
Start displayed the missing relative filename on the shelf. No native process
started and the previous staged `run.p8` hash was unchanged. Restored the file in
a finally block, retried Start without recreating the project, and played normally.

All 15 existing editable project hashes matched their baseline. The four root
games and the two pre-existing nested copies were unchanged. All four include
fixture files matched their repository sources after testing; the intentional
two-tab fixture update was recopied and hash-checked. No project import, data
reset, runtime reinstall or launcher profile change occurred. Device was left
on Play after a normal exit, with the external dispatch journal cleared.

## Automated checks and boundaries

All portable host suites, compilation and APK signature verification passed.
`PicoIncludesTest` has 43 checks covering input preservation, non-code/unknown
sections, raw Lua, all tabs, selected tabs, missing files/tabs, framing injection,
path restrictions, bounded reads/output, repeated dependencies, fresh subsequent
reads, comments/strings, nonrecursive includes and rejection of runtime file
calls in included code. The two final extra budget/fresh-read checks were compiled
and run after the full build; they change tests only, not installed app behavior.

The bounded launch contract and pending syntax/path support are in
[DEPENDENT_CARTRIDGES.md](../../DEPENDENT_CARTRIDGES.md). Workshop linked import,
PNG code inspection, runtime file sets, multicart and persistent file writes are
not implemented by this step. Next is a file-set runtime contract, not a rewrite
of `load()` into flattened Lua.
