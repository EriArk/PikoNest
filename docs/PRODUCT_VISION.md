# PIKOOS Product Vision

## 1. Product idea

PIKOOS is a handheld-first environment for the full PICO-8 creative loop:

> **Play → inspect → remix → create → learn → test → share**

It should feel like a small, delightful game-making device rather than a conventional IDE.

The product is intended for Android gaming handhelds first, then Linux handhelds later. It must not be designed around one screen size, one chipset, one control layout, or one manufacturer.

The key promise is simple:

> A person should be able to start by playing with games and end up learning real Lua and real PICO-8 almost by accident.

## 2. What makes PIKOOS different

PIKOOS is not only a launcher and not only an editor.

The useful distinction is that the same object — a PICO-8 cartridge — can move smoothly between different modes of interaction:

- play it;
- look inside it;
- change one thing;
- remix it;
- learn what a piece of code does;
- reuse a mechanic;
- build a new game;
- test it immediately;
- share it.

The boundary between **player** and **creator** should feel intentionally thin.

## 3. Emotional target

PIKOOS should feel:

- cute;
- compact;
- tactile;
- playful;
- friendly;
- immediate;
- curious;
- non-intimidating.

It should not feel:

- corporate;
- dense;
- like a desktop IDE shrunk onto a 4-inch screen;
- like an educational app giving homework;
- like a phone app awkwardly mapped to gamepad buttons.

A useful mental model is **a toy workshop that happens to contain a real programming environment**.

## 4. Cute, but not childish

The interface may celebrate progress and react to the user:

- first successful run: `It lives! ✨`
- first function: `Nice — you made your first function.`
- syntax problem: `I think this end got lost.`
- cartridge getting full: `This cart is getting pretty packed.`

But the user must always be able to reach the real technical information.

For example, a friendly error explanation can appear first, with `Show raw PICO-8 error` immediately available.

The product should respect both a complete beginner and an experienced PICO-8 developer.

## 5. The beginner path

A beginner should not need to study before making anything.

A good first-session flow is:

1. choose `Create`;
2. choose something like `Tiny Platformer`;
3. immediately see a character and a tiny room;
4. press `Play`;
5. change one visible parameter such as speed or jump height;
6. play again;
7. PIKOOS reveals the tiny piece of Lua responsible for that behavior.

The first reward is the changed game, not a badge for completing a lesson.

## 6. Learn by doing

Learning is contextual.

Example: the user adds health.

PIKOOS can say:

> We need somewhere to remember the player's health. Let's make `hp`.

Then it shows the real code:

```lua
hp=3
```

and briefly explains:

> `hp` is a variable — a named place that stores a value.

The user can change `3` to `5`, run the game, and see the result.

That single action teaches more usefully than an isolated lecture about variables.

## 7. Optional deeper learning

The default experience should never force a curriculum.

A user who wants more explanation can enable a stronger Learn mode. It can add:

- contextual explanations;
- code highlighting;
- tiny challenges;
- `why does this work?` actions;
- alternative implementations;
- reminders of previously encountered ideas.

Examples:

> This jump uses `vy=-3`. Try making the jump higher.

or:

> This code repeats once for every enemy. Want to see how the loop works?

## 8. Skill Book

PIKOOS may maintain a lightweight record of programming/game-making concepts the user has actually encountered.

Example:

```text
YOUR LUA BOOK

✓ values
✓ variables
✓ conditions
✓ buttons
✓ functions
○ loops
○ tables
○ callbacks
```

This is not a score and should not gate features.

A concept becomes familiar because the user used it in a real project.

## 9. Main product areas

A minimal top-level model could be:

### PLAY

The user's cartridge library.

Games should feel like objects on a shelf, not files in a directory.

Useful actions:

- Play
- Continue
- Favorite
- View source
- Remix
- Screenshots
- Details

### CREATE

Projects and creation tools.

A new project should be startable from:

- blank cart;
- tiny platformer;
- top-down adventure;
- shooter;
- puzzle;
- another lightweight starting point.

These are normal `.p8` projects, not proprietary templates that require a PIKOOS runtime.

### DISCOVER

A place to discover PICO-8 games and ideas.

Long-term this may integrate with publicly available PICO-8 discovery sources where technically and legally appropriate.

The key product action is not only `Play`; it is also:

- `Peek Inside`
- `Remix`

### MY STUFF

Projects, saved cartridges, experiments, screenshots, notes and possibly learning progress.

The exact navigation can change; the important part is keeping the top level small.

## 10. Cartridge library

A cartridge card can show:

- cover/screenshot;
- title;
- author;
- short description;
- play time;
- last played;
- favorite state;
- local project/remix relationship;
- compatibility/extension indicators where needed.

The primary action stays obvious: **Play**.

## 11. Creation workspace

The editor must retain PICO-8's visual identity: its palette, pixel language and
direct relationship between game material and tools. A generic native Android
form does not meet this direction. Readability, controller focus and usable
touch targets must survive the pixel styling. See the
[accepted visual baseline](UX_DIRECTION.md). The owner approved its transfer
to Android and reiterated that almost everything must be controller-operated;
physical ergonomics still need their own evaluation.

The editor should expose familiar PICO-8 areas in a handheld-friendly form:

- Play/Test
- Code
- Sprites
- Map
- Sound
- Project

The interface does not need to copy the stock PICO-8 editor. It should improve ergonomics while preserving the real underlying format.

## 12. Code without a physical keyboard

Most handhelds have excellent game controls and poor text input.

PIKOOS should treat that as a design constraint rather than an inconvenience.

The code editor can know:

- Lua keywords;
- PICO-8 API functions;
- symbols already declared in the project;
- common syntactic structures;
- context around the cursor.

Instead of typing every character, the user can insert structures such as:

- `if ... then ... end`
- `for ... do ... end`
- `function ... end`
- `btn()`
- `spr()`
- `map()`
- `sfx()`

Free-form keyboard input is still available for variable names, strings, comments and unrestricted editing.

An external keyboard should work well when present, but PIKOOS must remain useful without one.

## 13. Mechanics Library

The Mechanics Library is one of the core reasons PIKOOS can work on a keyboard-less handheld.

It contains compact, understandable implementations of common game mechanics.

Examples:

### Movement

- 4-way movement
- 8-way movement
- acceleration/friction
- simple platformer movement

### Platforming

- jump
- variable jump
- double jump
- coyote time
- jump buffer
- wall jump
- dash
- ladders

### Combat

- health/damage
- melee hit
- projectile
- knockback
- invulnerability frames
- hit flash

### Enemies

- patrol
- chase
- turret
- simple flying behavior
- simple boss phases

### World

- doors
- switches
- checkpoints
- moving platforms
- room transitions

### Polish

- particles
- screen shake
- fades
- dialogue box
- hearts/UI

The implementations are written specifically for PIKOOS, informed by useful patterns found throughout PICO-8 development culture.

## 14. A mechanic is also a lesson

Adding a mechanic should be a small interactive experience.

Example:

```text
JUMP

Height          [■■■■□□]
Gravity         [■■■□□□]
Variable jump   ✓
Coyote time     ✓

[ TRY IT ]
```

The user can preview it before adding it.

After insertion, the mechanic becomes normal Lua inside the cart.

The user can choose:

- `Use it`
- `How does it work?`

The second option teaches the generated code piece by piece.

The mechanic library therefore acts as:

- starter code;
- a game-design toolbox;
- an interactive code cookbook;
- a learning system.

## 15. Real code remains visible

Learning also includes a browsable PICO-8 reference and optional task-based
guides, available both from the current editor context and independently.
Controller navigation, readable explanations, version-aware examples and a
return to the same working context are required. See
[PICO-8 learning resources](PICO8_LEARNING_RESOURCES.md) for the owner-requested
direction, initial sources and future acceptance scenario.

PIKOOS should never trap users inside generated blocks.

If a mechanic generates:

```lua
if btn(0) then
  x-=1
end
```

that is exactly what the code editor should eventually show.

The user may rewrite it however they like.

This matters because the end goal is not dependence on PIKOOS. The end goal is confidence with PICO-8 itself.

## 16. Sprites and maps

Touch-enabled handhelds are especially good for sprites and map editing.

Possible controller/touch division:

- touch: drawing, selection, direct placement;
- D-pad/stick: precise movement;
- face buttons: tool actions;
- shoulders: tool/page/layer switching, undo/redo or modifiers.

Do not require touch; provide controller workflows for devices without it.

## 17. Sound and music

The sound tools should initially make common actions simple rather than expose every tracker concept at once.

A beginner may start with playful controls and presets, then reveal the real PICO-8 SFX/music structure as they go deeper.

As elsewhere, PIKOOS should progressively expose the underlying system rather than replace it.

## 18. Edit → Test loop

The edit/test cycle must be extremely short.

A user should be able to:

1. change something;
2. press a physical shortcut;
3. run through the official PICO-8 runtime;
4. exit;
5. return to the same editor/context.

Reducing friction in this loop matters more than adding dozens of advanced editor panels.

## 19. Cartridge limits as a teaching tool

PICO-8 constraints are part of its identity.

PIKOOS should show them in a friendly form first, with detailed numbers available when requested.

Example:

```text
CART SPACE
████████░░

Still some room!
```

Expanded view can show real budgets such as tokens, map/sprite use, SFX and music.

When the project approaches a limit, PIKOOS should explain what is consuming space and introduce optimization/multicart concepts gently.

## 20. Multicart without unnecessary jargon

Large projects can still use normal PICO-8 multicart behavior.

A beginner-facing project tree may call carts:

- Chapter
- Region
- Dungeon
- Episode

PIKOOS can handle the underlying file relationships while still allowing an advanced user to see the real carts and code.

## 21. Remix as a first-class workflow

Remixing is a powerful bridge from player to developer.

A useful flow:

1. play an interesting cart;
2. press `Remix`;
3. PIKOOS creates a working copy;
4. offer easy entry points:
   - change hero;
   - make movement faster;
   - change music;
   - add dash;
   - change a room;
5. user eventually sees how each change maps to real code/data.

Remix should feel like opening a toy to see what is inside.

## 22. Debugging can teach too

Raw Lua/PICO-8 errors can be confusing to beginners.

PIKOOS should translate common failures into contextual explanations while keeping the real error visible.

Example:

> `enemy.x` failed because `enemy` does not currently contain an object. Want to jump to where it is created?

Then offer:

- Fix/help action
- Go to code
- Show raw error

## 23. Debug/test experiments

Potential later tools:

- variable watch;
- collision/hitbox overlay;
- CPU usage visualization;
- sprite/map budget view;
- quick room test;
- recorded input replay;
- bug capture.

Bug Capture could save enough test context to replay the same input sequence after a fix.

These are useful, but they are not prerequisites for the first usable creator.

## 24. Link Play

A long-term experiment is transparent handheld-to-handheld play for carts that already behave as local multiplayer PICO-8 games.

The cart sees normal player input; PIKOOS transports one player's input from another handheld.

The goal is to add a handheld capability without creating a new Lua language.

The feasibility and exact synchronization design still require proof-of-concept work.

## 25. PIKOOS-enhanced online cartridges

A special cart may opt into PIKOOS network features.

Conceptually:

```text
PICO-8 cart
    ↕
PIKOOS bridge
    ↕
Internet
    ↕
Game server
```

The cart remains a standard `.p8` file, but its online features may only work when a PIKOOS bridge is present.

A server-driven persistent game could keep most persistent world/account state on the server while the cart contains the renderer, controls, UI, local effects and client rules.

See `ONLINE_CARTRIDGES.md`.

## 26. Personalized cartridges

For an online game, a clean cartridge can become a user's personal cartridge.

Initial flow:

1. create/login to account;
2. create/select character;
3. server returns a unique cartridge token;
4. PIKOOS writes that token into a reserved part of the cart;
5. user chooses a short PIN for routine authentication.

Subsequent use can feel like inserting a personal game cartridge:

```text
WELCOME BACK, MOPPY

PIN: ● ● ○ ○
```

A `Factory Reset Cartridge` action clears the binding and returns the cart to a clean/unbound state.

A `Share Clean Cartridge` action produces a copy without the personal token.

## 27. Product scope discipline

PIKOOS can eventually contain many powerful features, but the first usable product must stay focused.

The core experience is successful if it can do these things well:

- import the official runtime;
- browse local cartridges;
- play them;
- create a normal PICO-8 project;
- edit it comfortably on handheld controls;
- add a few understandable mechanics;
- teach what those mechanics are doing;
- test instantly in official PICO-8;
- export/open the same cart elsewhere.

Everything else should grow from that foundation.

## 28. The intended progression

A user might progress like this:

### Day 1

Change a character and movement speed.

### Later

Build a tiny platformer using ready mechanics.

### Later

Modify generated Lua directly.

### Later

Write a custom function.

### Later

Open an unfamiliar cart and understand a meaningful part of it.

At that point PIKOOS has succeeded: a person who arrived to play with little games has become someone who can actually create with PICO-8.

## 29. Summary

PIKOOS is simultaneously:

- a console;
- a workshop;
- an editor;
- a remix tool;
- a Lua/PICO-8 learning environment;
- a mechanics cookbook;
- an experimental platform for handheld PICO-8 features.

But it should never *feel* like eight separate tools.

It should feel like one small, happy machine for playing with games.
