# Development runtime archives

At the repository owner's explicit request on 2026-10-02, this private
repository stores the supplied, unmodified PICO-8 0.2.7 archives for development
and runtime integration experiments.

| Archive | Target |
| --- | --- |
| `pico-8/0.2.7/pico-8_0.2.7_i386.zip` | Linux x86, 32-bit |
| `pico-8/0.2.7/pico-8_0.2.7_amd64.zip` | Linux x86-64 |
| `pico-8/0.2.7/pico-8_0.2.7_raspi.zip` | Raspberry Pi, including `pico8_64` |

The archives retain the original license and documentation. `SHA256SUMS` beside
them records the SHA-256 hashes verified against the supplied attachments.

These files are private development inputs. Do not include this directory in
application packages or public releases. PikoNest still requires each end user
to import their own official PICO-8 runtime, as specified in
[`PICO8_COMPATIBILITY.md`](../docs/PICO8_COMPATIBILITY.md).

Storing these archives does not establish that the Android runtime path works;
the launch/exit proof in [`ROADMAP.md`](../docs/ROADMAP.md) remains pending.
