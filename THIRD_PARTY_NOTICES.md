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

The experimental runtime build consumes separately downloaded tools and binaries.
Pinned inputs appear in the [runtime build instructions](experiments/runtime-restart/README.md#reproducible-local-build).
A full component/notice audit of that payload remains required before production
redistribution. This table is an identified-materials inventory, not a completed
audit of every transitive dependency.

Third-party cartridges and extracted assets retain their authors' terms. Import
or extraction does not grant redistribution permission. Screenshot captions
identify demo content and distinguish previews from official-runtime execution.
