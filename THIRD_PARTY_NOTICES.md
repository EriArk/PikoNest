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

The early beta support payload has a pinned inventory of **82 native files**.
Eighty match official Ubuntu, Termux and Godot artifacts. SDL and the shim have
separate permissive source/notice review. Unknown historical Termux files were
replaced with signed packages and their corresponding sources, rather than
distributing unidentified builds. The source-material bundle contains 185 pinned
inputs plus notices, build instructions and the exact BusyBox configuration.

[Distribution review](docs/release/beta1/DEPENDENCIES.md) ·
[Full component notices](experiments/android-host/assets/NATIVE-NOTICES.txt) ·
[Rebuilding and library replacement](docs/release/beta1/REBUILDING.md).

Notices cover native packages, LLVM runtimes/OpenMP, SDL, AndroidX, Kotlin,
coroutines, JetBrains annotations, Guava ListenableFuture, the Android file picker
and app-links plugin, and Mozilla certificate data. Godot/AOSP and font notices
remain separate. Exact SDL build flags and the original Gradle dependency graph
were not recovered; a complete offline rebuild is not claimed. GPL/LGPL source
and replacement obligations are distinct from permissive notice requirements.
Original PikoNest MIT licensing is unchanged.

Third-party cartridges and extracted assets retain their authors' terms. Import
or extraction does not grant redistribution permission. Screenshot captions
identify demo content and distinguish previews from official-runtime execution.
