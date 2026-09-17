# PIKOOS Online Cartridges

## Status

Experimental product direction. The user-facing behavior is defined here; transport/bridge details still require proof-of-concept work.

## 1. Idea

A PIKOOS online cartridge is still a `.p8` cartridge, but PIKOOS provides an optional bridge between that cart and an online game server.

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

The cart can therefore act as a small client for a much larger persistent game.

## 2. What lives where

A practical split could be:

### Cartridge/client

- rendering;
- controls;
- UI;
- local effects;
- client-side animation;
- compact local rules;
- basic assets;
- protocol handling.

### Server

- accounts;
- characters;
- persistent world state;
- inventory;
- quests;
- NPC state;
- economy;
- events;
- authoritative online game rules;
- validation of important actions.

The goal is not to upload arbitrary Lua from the server and execute it. The client should remain a predictable PICO-8 program driven by server data/messages.

## 3. Clean cartridge

A downloadable/shareable online cart begins unbound.

Conceptually:

```text
ONLINE CART
state: CLEAN
account token: empty
```

The clean cart can be copied/shared safely.

On first online use it offers something like:

```text
NEW PLAYER
RETURNING PLAYER
```

## 4. First-time account binding

Possible flow:

1. user launches a clean online cart in PIKOOS;
2. cart/PIKOOS requests account creation or login;
3. user creates/selects a character;
4. server creates a unique high-entropy **cartridge token**;
5. user chooses a short PIN for routine launches;
6. PIKOOS writes the cartridge token into a reserved area of this copy of the `.p8`;
7. the cart is now personalized.

Result:

```text
ONLINE CART
state: PERSONALIZED
token: present
```

This is intentional: the user's cartridge file itself becomes a personal object.

## 5. Authentication model

Routine login should use:

```text
cartridge token + PIN
```

The token identifies the personalized cartridge/account binding.

The PIN is verified by the server and attempts are rate-limited.

Important consequence:

> Possessing a copied personalized cart/token alone should not immediately authenticate the account without the PIN.

The account's longer password/primary credential remains available for:

- account recovery;
- changing the PIN;
- revoking cartridges;
- managing characters/account settings;
- binding a fresh cartridge after loss/reset.

## 6. Why the PIN is server-side

Do not treat a short PIN as strong local encryption.

A short PIN stored or checked entirely inside a readable cartridge is easy to brute-force offline.

Instead, use it as a server-validated second component:

```text
random token in cart + memorized PIN + server rate limiting
```

This preserves the desired simple handheld UX without pretending a 4-6 digit PIN is cryptographically strong by itself.

## 7. Token requirements

A cartridge token should be:

- random/high entropy;
- unique;
- revocable;
- scoped to the intended game/service;
- replaceable after compromise/reset;
- treated as sensitive data.

Do not encode account password, e-mail address, or unnecessary personal data into the cart.

## 8. Token storage inside `.p8`

Product requirement:

> The personalized token is physically written into the cartridge file itself.

The exact encoding is still TBD.

Possible approaches must be evaluated against:

- standard `.p8` validity;
- no accidental use by ordinary game assets/code;
- reliable round-trip through PIKOOS;
- predictable capacity;
- ability to identify the reserved block;
- safe clean/reset operation;
- behavior when opened/saved in official PICO-8;
- avoiding unnecessary code token cost if possible.

Do **not** choose a final location before testing real carts and official-runtime behavior.

Potential storage families to investigate include:

- a deliberately reserved standard data range owned by this online cart;
- a clearly marked encoded block in a standard cart section;
- another standard-file-safe representation.

The online cart template itself must reserve the chosen area and never treat it as ordinary game data.

## 9. Binding metadata

The reserved block may need to carry more than the raw token, for example:

```text
binding format version
service/game id
token length
token
small flags/check value
```

Keep this compact.

Do not store server-authoritative character state in the binding block.

## 10. Personalized launch UX

A personalized cart may start like:

```text
WELCOME BACK, MOPPY

PIN
● ● ○ ○
```

On successful authentication the server returns the current character/world state.

The cart itself is not the authoritative save file for an online persistent game.

## 11. Factory Reset Cartridge

A required action:

```text
CARTRIDGE SETTINGS
  → FACTORY RESET CARTRIDGE
```

After confirmation PIKOOS:

- clears the token/binding block;
- clears cart-local binding metadata;
- returns the file to the clean/unbound state;
- does **not** delete the remote account or character unless the user separately requests that through account management.

The server may mark/revoke the old token as part of reset when online.

If reset happens offline, queue/offer token revocation on the next authenticated account-management session where possible.

## 12. Share Clean Cartridge

A personalized cart must never be casually shared by default.

Primary sharing action:

```text
SHARE CLEAN CARTRIDGE
```

PIKOOS creates a copy with the binding area returned to its clean state.

The user's personal cart remains unchanged.

Exporting the personalized file itself can exist as an advanced/explicit action, but should clearly warn that it contains an account token.

## 13. Lost or copied cartridges

Account management should eventually support:

- list active cartridge bindings;
- revoke a token;
- rename/identify a binding if useful;
- bind a fresh clean cart;
- change PIN;
- recover access using the primary account credential.

A token must not be permanent and irrevocable.

## 14. Multiple cartridges/accounts

Do not assume one PIKOOS device equals one account.

Because identity lives partly in the personalized cart, the system should allow:

- multiple users' carts on one device;
- multiple characters/carts for one account if the game allows it;
- copying a personal cart between the user's own devices, subject to PIN/server rules.

Do not bind the fundamental identity model to Android device IDs.

This also supports future Linux handhelds naturally.

## 15. Persistent world protocol

Keep the network protocol compact and data-driven.

Example client messages:

```text
move(direction)
interact(targetId)
attack(actionId, targetId)
enter(areaId)
use(itemId)
```

Example server state/events:

```text
player state
nearby entity states
area/chunk data
inventory delta
quest/event delta
world event
message/dialogue data
```

Do not send or execute arbitrary remote code as the normal content-update mechanism.

## 16. World size

A server-driven cart is not limited to storing an entire world inside one cartridge.

The cart can contain reusable rendering/game systems and request compact world/chunk/entity data as needed.

That allows a tiny PICO-8 client to represent a persistent world far larger than the cart's static map storage.

The actual bridge bandwidth/latency limits must be measured before setting gameplay expectations.

## 17. Real-time vs asynchronous online games

The first online-cart prototype does not need to be an MMO.

Good early candidates are systems tolerant of modest latency/bandwidth:

- asynchronous shared worlds;
- small social spaces;
- turn-based games;
- trading/mail;
- ghost data;
- world events;
- simple co-op with low message volume.

Fast action networking should only be attempted after bridge performance is measured.

## 18. Offline behavior

An online cart should declare its intended offline behavior.

Possible modes:

### Online required

```text
This cartridge needs PIKOOS Online.
```

### Offline demo

A small local demo or tutorial remains playable.

### Cached/offline

Some read-only or limited play continues using cached state where game design permits.

The cart should fail clearly rather than silently behaving incorrectly.

## 19. Standard PICO-8 behavior

The cart file should remain syntactically valid standard PICO-8.

When opened outside PIKOOS, the bridge will not exist.

The program should detect/unambiguously handle that situation where the chosen bridge design allows it.

Preferred user experience:

```text
PIKOOS ONLINE NOT FOUND

This cart uses online features.
[OFFLINE MODE]
```

rather than crashing.

## 20. Bridge transport is unresolved

The desired product behavior does **not** prove the transport mechanism yet.

Research is required to determine how the official PICO-8 runtime launched through PIKOOS can communicate with the host without modifying the official binary.

Potential investigation areas include the runtime wrapper/shim and standard PICO-8 host I/O facilities.

Keep all transport work behind a `BridgeBackend` abstraction.

Do not design the game protocol around a particular hack until a reliable Android proof exists.

## 21. Security rules

For online game servers:

- server is authoritative for persistent state;
- never trust coordinates/currency/items simply because a client sends them;
- validate actions;
- rate-limit PIN attempts;
- rate-limit abusive network calls;
- support token revocation;
- use encrypted transport provided by the host/network layer;
- avoid putting primary account secrets in `.p8`;
- log/audit account binding changes where appropriate.

## 22. Factory-reset guarantee

The clean version of an online cart should be deterministic enough that PIKOOS can confidently answer:

> Does this file currently contain a personal binding?

and perform:

> Reset only the binding, not the rest of the player's modified/remixed game data.

This means the binding block must be explicitly structured, not found by fragile string searching.

## 23. Versioning

The binding format must be versioned from its first prototype.

Example:

```text
PKO1
version=1
service=...
token=...
```

The exact encoding is TBD; the important point is that future PIKOOS versions can identify and migrate/reset older bindings safely.

## 24. First prototype success criteria

Before building a full game, prove:

1. a clean `.p8` can declare/reserve a binding area;
2. PIKOOS can write a random token into that area;
3. official PICO-8 can still open/run the cart;
4. PIKOOS can read the same token back after normal cart use;
5. Factory Reset restores the binding area without damaging the cart;
6. Share Clean creates a token-free copy;
7. a bridge POC can exchange at least small messages with a server while the official runtime is being used.

Only after these pass should the online game protocol become a major implementation focus.
