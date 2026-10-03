# Linked runtime files — Android host 0.0.28

2026-10-03. Retroid Pocket Classic, Android 14, user's official PICO-8 0.2.7.
Both development APKs updated in place, preserving imported runtime and settings.

| Artifact | SHA-256 |
| --- | --- |
| Host 0.0.28 | `ffe9fa4e3fd67a7750b11e8262b7bec5b49f220550d6bd1156f6703162f6185d` |
| Runtime Test 1.6.6-pikoos.3 | `fb50446d3b087c93454d6b187635c3a6bd8a4cdf7f1782b9501e422c54f7b050` |

## Official-runtime comparison

Owned fixture: `experiments/android-host/tests/fixtures/petal-gate/`.
`Petal-Gate.p8` loads `night.p8` with a parameter string; that cart asserts
`stat(6)=="petals=3"`, reloads one graphics byte from `colors.p8`, asserts value
14, and can load the garden again. Calls are unchanged ordinary PICO-8 Lua.

The direct baseline used temporary, hash-checked copies of the three owned
files in the runtime's existing cart root. The upstream wrapper otherwise uses
its configured cart root, not the external cart's parent, for runtime loads.
Those three baseline copies were removed after the comparison, leaving only
the pre-existing `demos` and `pikoos-tests` root directories. The PIKOOS Games
sources stayed in `Games/petal-gate/` throughout.

PIKOOS external VIEW entry and Play both reached the garden, loaded the night
chapter/data and returned to the garden. Thus the prepared run could not silently
read the baseline copies. Native and prepared night captures are byte-identical:
SHA-256 `10822ab9ea6f220fc3b9b243af1ca5b5c4ba160237cab0cca1b3af81eb86515e`.

- [Direct official-runtime chapter](direct-night.png)
- [PIKOOS-prepared chapter and linked data](prepared-night.png)
- [Return to garden through native load](prepared-back.png)
- [Return to selected Play card](returned.png)
- [Missing source file reported on Play](missing.png)
- [Malformed adapter transport refused](bad-trailer.png)

Home → resume retained native PID 22107; the subsequent chapter transition
worked and its settled capture matched the baseline. Native input used injected
held keyboard equivalents; host navigation used injected controller actions.
Physical button ergonomics and audio listening are separate acceptance work.
External entry used explicit VIEW with the configured launcher path contract;
Beacon/Retroid catalog UIs were not rescanned/reconfigured in this slice.

## Failure, cleanup and preservation

- Temporarily renamed only fixture `colors.p8`, restored in finally: Play
  reported its missing name, did not launch native PICO-8 and did not replace
  the preceding staged transport. Retrying after restoration worked.
- Valid 1,192-byte transport was mutated to an absolute entry name, impossible
  first payload length and extra trailing byte. All three direct adapter inputs
  showed an error, started no native process and created no extraction directory.
  Faults were bytes[12]=47, big-endian int at offset 46=0x7fffffff, and one
  appended zero byte respectively. Confirming the dialog returned to the caller.
- Runtime session files were mode 444 and directory 555. Normal native exit
  removed that exact session folder; private runtime home was not replaced.
  Two tiny earlier development-session folders remain for diagnostics (one
  interrupted permission experiment, one before cleanup was added). This is not
  evidence of automatic crash recovery; the retained-session quota is 32.
- All 15 existing project hashes, four root games, two nested game copies and
  all four include-fixture sources matched their baselines. All three new
  Petal Gate sources matched the repository after playing. No editor import,
  user-data reset, uninstall or launcher-profile changes occurred.
- After Home/resume followed by an APK update, the host's conservative pending
  session journal required opening Play once before a new external launch. It
  cleared on the normal return path. General stale-session recovery remains open.
- Final host build also launched the existing nested `Little-Lights.p8.png` and
  Include Garden through external entry. Include Garden's settled capture retained
  SHA-256 `12e467c18661baeadcff654a802b4fecb8f53c87f7ebfc3c23b7bffcac8eecec`,
  matching its previous direct/prepared baseline.

## Automated coverage and scope

The complete portable host suites, compilation and APK signature verification
passed. `RuntimeFileSetTest` has 47 checks: literal load/reload, lexical exclusions,
unsupported paths/calls, cycles, source preservation, shared include snapshots,
downstream code, immutable payloads, transport byte fidelity, missing parts,
case collisions and count/byte bounds. Runtime build verified all 262 sparse
asset index entries against the final signed APK.

The transport is internal to the experimental runtime adapter. The editable game
remains ordinary `.p8` files. Current support is read-only sibling text carts
with literal filenames, not arbitrary Lua dependency discovery, PNG dependency
inspection, nested multicart paths or durable file writes. Source reads are not
an atomic filesystem transaction. Existing runtime-home handling is retained;
persistent cartdata across sessions still needs a dedicated regression check.
See [contract](../../DEPENDENT_CARTRIDGES.md).

Next: integrated runtime/archive setup with resumable preparation and clear
readiness states; broaden compatibility and durable-write ownership separately.
