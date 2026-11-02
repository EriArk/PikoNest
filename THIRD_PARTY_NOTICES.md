# Third-party materials and license status

Owner decision, 6 October 2026: original PikoNest source code (including original
demo Lua) and accompanying documentation use the [MIT License](LICENSE).
This code-license decision does not relicense third-party materials or establish
a separate reuse license for original artwork and screenshot imagery.

| Material | Attribution and source | License / scope |
| --- | --- | --- |
| Monocraft, native UI font | Idrees Hassan, [Monocraft](https://github.com/IdreesInc/Monocraft) | [SIL OFL 1.1](experiments/android-host/assets/Monocraft-OFL.txt) |
| Tiny5, prototype font | The Tiny5 Project Authors, [font_tiny5](https://github.com/Gissio/font_tiny5) | [SIL OFL 1.1](experiments/android-host/assets/OFL.txt); [UX study copy](experiments/ux-study/assets/OFL.txt) |
| Upstream Android wrapper source | Unmatched Bracket; [Macs75/pico8-android](https://github.com/Macs75/pico8-android) | [Preserved MIT text](experiments/runtime-restart/UPSTREAM-LICENSE.txt); does not relicense PikoNest or all wrapper binary dependencies |
| Official PICO-8 | Lexaloffle Games | Proprietary; each user supplies it separately; never part of a PikoNest APK |
| Godot 4.6, integrated engine | [Godot Engine](https://godotengine.org/license/); pinned APK contains `4.6.stable.official` version strings | [MIT notice](experiments/android-host/assets/GODOT-LICENSE.txt) and [third-party copyright file](experiments/android-host/assets/GODOT-COPYRIGHT.txt), preserved from `godotengine/godot` tag `4.6-stable` |
| Press Start 2P, upstream frontend font | CodeMan38; notice preserved from the pinned upstream wrapper source | [SIL OFL text](experiments/android-host/assets/PressStart2P-LICENSE.txt) |

The experimental runtime build consumes separately downloaded tools and binaries.
Pinned inputs appear in the [runtime build instructions](experiments/runtime-restart/README.md#reproducible-local-build).
A full component/notice audit of that payload remains required before production
redistribution. This table is an identified-materials inventory, not a completed
audit of every transitive dependency.

The 2 November dependency follow-up covers 79 ELF files (77 bootstrap and two APK
libraries). Forty-four match exact official Ubuntu/Termux/Godot artifacts; 153
source archives and patches are collected with pinned URLs/hashes. See
[the distribution evidence](docs/release/beta1/DEPENDENCIES.md) and preserved
[native component notices](experiments/android-host/assets/NATIVE-NOTICES.txt).
These include Ubuntu package copyright texts, full referenced common licenses,
the identified Termux source notices, SDL/wrapper notices and LLVM C++ runtime
licenses. The latter are separate from the Godot engine copyright list.

This remains incomplete: 33 Termux files need exact source/build provenance or
replacement, and non-native dependencies and final distribution requirements still
need review. New notices enter the next APK packaging pass. The candidate stays
local until the corresponding-source/notice package is complete. Original PikoNest
MIT licensing is unchanged and does not cover those separate component terms.

Third-party cartridges and extracted assets retain their authors' terms. Import
or extraction does not grant redistribution permission. Screenshot captions
identify demo content and distinguish previews from official-runtime execution.
