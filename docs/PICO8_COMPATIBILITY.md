# PIKOOS — PICO-8 Compatibility Rules

## Purpose

PIKOOS should make PICO-8 easier and more playful without quietly creating a different fantasy console.

This document defines the compatibility contract.

## 1. Canonical format

For normal projects, the canonical game artifact is a standard PICO-8 cartridge:

```text
game.p8
```

PIKOOS may keep optional metadata beside it, but that metadata must not be required for the standard cart to open and run in ordinary PICO-8.

## 2. Standard project invariant

A standard PIKOOS project must be able to follow this path:

```text
Create/edit in PIKOOS
        ↓
Save game.p8
        ↓
Open game.p8 in official PICO-8 outside PIKOOS
        ↓
Continue editing/running normally
```

And the reverse should also be supported as safely as practical:

```text
Existing PICO-8 cart
        ↓
Import/open in PIKOOS
        ↓
Edit
        ↓
Save
        ↓
Still works in official PICO-8
```

## 3. Official PICO-8 is authoritative

PIKOOS may implement analysis, syntax help, preview tools and diagnostics, but the user-provided **official PICO-8 runtime** is the authoritative compatibility target.

A future alternative preview engine must never redefine compatibility.

`Verify in PICO-8` should always mean testing with the official runtime.

The Android 0.0.27 Play experiment prepares supported `#include` source sets into
a temporary ordinary `.p8`, without rewriting their editable sources. This is a
bounded launch operation, not a change to the canonical project format or support
for runtime multicart/file writes. See the [linked-file contract](DEPENDENT_CARTRIDGES.md)
and its direct-versus-prepared official-runtime checks.

The later 0.0.28 slice also launches bounded read-only sibling carts through
ordinary `load`/`reload`. Neither slice is a full project/dependency editor.
Current gaps and planned verification are tracked in [STATUS](STATUS.md) and
[BACKLOG](BACKLOG.md); the compatibility rules below remain authoritative.

### The base follows PICO-8's actual model

Owner-confirmed rule, 2026-10-02: the base tools follow the real PICO-8 rules,
including resource organization, API semantics and version-specific limits.
This applies to graphics, maps, sound/music, Lua, timing and cartridge handling,
not just whether the resulting file has a `.p8` extension.

Before implementing a tool, use the relevant sections of the
[official manual](https://www.lexaloffle.com/dl/docs/pico-8_manual.html), matched
to the targeted runtime version. Document how friendly editing operations map
to ordinary PICO-8 data/code. Test the affected data and behavior, including
official-runtime checks where applicable; do not claim unobserved verification.

Temporary prototype restrictions are implementation scope, not console rules.
For example, the first editor slice exposed eight 16×16 images. PICO-8 uses 8×8
sprite cells and supports drawing larger or rectangular regions with `spr()`
or pixel-addressed regions with `sspr()`. Future editor sizes must use that
underlying model and account for occupied sheet regions and shared map memory.
Do not bake the experiment's fixed image slots into the production resource model.

Android lab 0.0.6 now also selects rectangular areas in the upper 128×64 pixels,
while its portable gfx model addresses all 128×128. The UI's 8-pixel selection
step and exclusion of shared-map rows are temporary editing scope. The selected
rectangle is a view of ordinary pixels, not a custom cartridge resource format.
See [implementation and observed runtime evidence](design/android-regions-05/README.md).

The 0.0.7 owned-template hero binding stores source rectangle, drawing offset
and rectangular body dimensions as ordinary Lua variables. `sspr` reads the
selected pixels; ordinary platformer code handles collisions. PICO-8 does not
gain a custom physics/resource API. Deriving an initial box from nonzero pixels
is a workshop convenience for the template's default palette, not a console
rule or pixel-perfect collision promise. The box stays fixed during subsequent
drawing edits until explicitly reassigned. Changed owned movement functions are
refused by the binding adapter rather than rewritten speculatively.

An operation may remain unsupported while the editor grows. Label that as a
PIKOOS editing limitation, preserve the cartridge and avoid destructive fallback
or conversion. Lack of editor support alone does not make a valid cart
`Incompatible / Error`. Conversely, do not hide extra resources or altered
runtime behavior behind a standard-project label.

The interface may offer different layouts, controller workflows, names and
high-level tools. Their game output must retain ordinary PICO-8 semantics;
optional PIKOOS extensions remain explicit and separable.

## 4. No custom Lua dialect for standard carts

Standard carts must use ordinary PICO-8 Lua syntax and APIs.

Do not introduce syntax such as:

```text
@component PlayerMovement
@network server.foo
```

if ordinary PICO-8 cannot parse it.

Friendly editors may generate ordinary code, but the stored result must remain ordinary code.

## 5. Mechanics are code generators, not runtime components

The Mechanics Library can present a high-level configuration UI, for example:

```text
JUMP
Height: 4
Coyote time: on
```

But `Add to game` should produce understandable normal Lua/data.

Once inserted, the game does not require a hidden PIKOOS movement engine.

The generated code should remain editable by the user.

## 6. PIKOOS metadata stays outside the cart by default

Normal editor/workshop data belongs in:

```text
.pikoos/
```

Examples:

- notes;
- project UUID;
- editor cursor/layout state;
- learning progress;
- screenshots;
- test definitions;
- mechanic provenance;
- history.

Do not put editor metadata into the cart merely because there is spare cart space.

## 7. Preserve imported carts conservatively

PIKOOS should avoid damaging or unnecessarily rewriting existing carts.

Priorities:

1. preserve semantics;
2. preserve all recognized PICO-8 data;
3. preserve comments and formatting where practical;
4. preserve unknown text/sections where safe;
5. minimize unrelated diffs.

Opening and saving a cart without changes should ideally produce no content change.

## 8. Round-trip testing

Build a corpus of real `.p8` carts for parser/writer tests.

At minimum test:

- tiny carts;
- large carts near limits;
- unusual formatting;
- comments;
- tabs/spaces;
- long strings;
- all standard sections;
- carts with shared map/gfx memory use;
- multicart projects;
- carts that rely on compact PICO-8 syntax/features.

Round-trip failures are release-blocking for the editor portions that touch those structures.

## 9. `.p8.png`

PIKOOS should eventually support convenient cartridge image import/export if implementation effort is reasonable.

However, do not delay the core editor architecture on `.p8.png` support. Text `.p8` is the first-class editable format for early milestones.

When `.p8.png` support is added, decoded data must pass through the same canonical project model and compatibility checks.

## 10. Resource limits

PIKOOS should teach and display real PICO-8 limits rather than pretending they do not exist.

The UI can show a friendly summary first, but advanced details should expose actual resource usage.

Examples include:

- code/token budget;
- character/code size where relevant;
- sprite/map storage relationship;
- SFX usage;
- music usage;
- memory/CPU observations where available.

Do not invent a “PIKOOS standard cart” with larger hidden limits and call it PICO-8 compatible.

## 11. Multicart

Large projects should use normal PICO-8-compatible multicart techniques.

PIKOOS may simplify this concept in the beginner UI as:

- Add Region;
- Add Chapter;
- Add Dungeon;
- Add Episode.

Underneath, the project should remain composed of valid carts and standard transitions/data handling.

## 12. Project verification states

Suggested states:

### `Standard / Verified`

Runs successfully through the imported official runtime and has no declared PIKOOS-only feature dependency.

### `Standard / Unverified`

No known PIKOOS-only dependency, but has not been verified after the latest changes.

### `Enhanced`

Valid `.p8`, but intentionally uses optional PIKOOS bridge/services for part of its behavior.

### `Incompatible / Error`

Known parse/resource/runtime issue that prevents ordinary PICO-8 behavior.

Avoid overclaiming automatic verification if PIKOOS cannot observe the full runtime result.

## 13. Enhanced carts

PIKOOS-enhanced carts are allowed, but must be visibly different from standard projects.

Examples of optional enhanced capabilities:

- online bridge;
- PIKOOS Link Play-specific support;
- external service integration;
- PIKOOS achievement hooks;
- personalized online-cart binding.

A cart may still be syntactically valid PICO-8 while its enhanced features are unavailable outside PIKOOS.

Where practical, enhanced carts should degrade gracefully:

```text
Online features require PIKOOS.
[Play Offline]
```

instead of simply crashing.

## 14. Online-cart exception

Personalized online cartridges deliberately store a PIKOOS account-binding token **inside the cartridge file**.

That is a product feature rather than ordinary editor metadata.

Rules:

- the cart must explicitly reserve a binding area;
- the encoding must remain valid within a normal `.p8` file;
- the clean/unbound version contains no user token;
- `Factory Reset Cartridge` removes the binding;
- `Share Clean Cartridge` exports a token-free copy;
- the exact storage method must be proven safe before standardizing it.

See `ONLINE_CARTRIDGES.md`.

## 15. Never bundle PICO-8

PIKOOS must not redistribute the official proprietary PICO-8 runtime.

The installation flow should ask the user to import/select a runtime they obtained legitimately.

PIKOOS may validate files/version/capabilities after import.

## 16. Runtime versions

Do not assume only one PICO-8 version will ever exist on user devices.

Track imported runtime metadata where possible:

```text
runtimeVersion
runtimeArchitecture
runtimePlatformBuild
```

Compatibility warnings can then distinguish:

- a PIKOOS bug;
- an unsupported runtime version;
- a cart feature requiring a newer PICO-8 version.

## 17. Compatibility over cleverness

If a convenience feature has two possible designs:

- clever proprietary representation;
- slightly less magical implementation that writes normal PICO-8 code/data;

prefer the second for standard projects.

The value of PIKOOS is making the real system approachable, not hiding a different engine behind a PICO-8 skin.

## 18. Temporary linked-file launch preparation

Host 0.0.28 can prepare simple includes and a bounded set of sibling text carts
for ordinary `load`/`reload`. The source remains ordinary `.p8` and linked files;
runtime calls are not flattened or replaced by custom APIs. Internal adapter
transport is not an editable project format. Official-runtime fixture comparisons
are evidence for that limited path, not proof of arbitrary Lua compatibility.
See [linked-file contract and pending support](DEPENDENT_CARTRIDGES.md).
