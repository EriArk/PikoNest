# Music composition — lab 0.0.83

Open **Music** from Game view or **All tools → Menu → Music**. The same tool works
in Blank, hero-free and imported cartridges. It edits ordinary PICO-8 patterns
and SFX data. Unknown Lua remains unchanged; automatic phrase allocation and
start-music wiring require a completely recognized Game view project.

## Create and use a composition

1. Open a pattern in the sequence. Its four parts correspond to PICO-8's four
   audio channels. Choose a part, select an existing SFX or **Create phrase**.
2. Edit notes using the [SFX editor](SFX_EDITOR.md). **Keep phrase** returns to
   the part and retains an unsaved composition draft. It does not save the cart.
   **Edit shared phrase** changes every use of that SFX; **Edit a copy** allocates
   an independent SFX and changes only this part's reference. Imported unfamiliar
   code can edit an explicitly selected SFX but cannot automatically allocate one.
3. Set Loop start, Loop back and Stop after this. Patterns play in numbered order.
   **Copy to next pattern** copies references/flags, with explicit overwrite
   confirmation. It shares SFX until you choose an independent copy. **Clear this
   pattern** clears references/flags after confirmation, retains SFX data and does
   not renumber other patterns. Copy at slot 63 is refused.
4. **X / Listen** auditions from the focused pattern, including its sequence flow.
   Inside the phrase chooser it auditions the highlighted SFX; inside the note
   editor it auditions that draft phrase. Official PICO-8 O stops and X replays.
   These are isolated temporary cartridges. Start tests the game candidate instead.
5. **Use in the game** sets the selected pattern as the start of the soundtrack.
   Choose three or four musical parts explicitly, as described below. Music starts
   with the game, restarts on reset, and stops at win/loss. Remove game music keeps
   all patterns and SFX. This first binding does not select different tracks for
   rooms or results; the existing event-SFX bindings still work.
6. Menu opens the composition review. **Save composition** commits all changed
   patterns, phrases and start binding together. Keep editing returns to the bank;
   Discard changes restores the saved source. Leaving a changed bank opens review.

D-pad navigation selects rows; shoulders move eight rows. A chooses actions;
B returns or reverts an active note field. The sequence grid is also touchable.
Y/R2 undo/redo composition edits; inside the note editor they undo/redo note edits.
After saving, bank Undo/Redo uses the existing session history. Recovery retains
composition edits, nested note drafts and the field's original value. Source
changes invalidate recovery without replacing the saved cart.

## Music and effects

**Use 3 music parts** explicitly mutes part 4 throughout the reachable track,
including backward loop targets. Underlying SFX references and notes are retained;
other tracks are not changed unless they share those same patterns. The review
reports changed patterns. Parts 1–3 use channels 0–2; event effects use channel 3.
The generated call is `music(game_music,0,7)`.

**Use all 4 parts** preserves the fourth part and uses mask 15. Event effects can
interrupt it on channel 3; this tradeoff appears in the choice. The editor does
not promise automatic ducking, mixing or musical resumption behavior. Playback
must still be checked in the user's official runtime.

If you later enable part 4 in a track bound to the three-part policy, Save/Test
explains the conflict. Explicitly choose the three-part policy again to mute it,
or switch to four parts. Neither option deletes the phrase. On an outcome, music
stops before the contact/result SFX calls. Repeated finished-game updates do not
restart either music or result sounds. Reset stops prior playback before starting
music and then the start SFX. Games without music keep their prior generated Lua.

## Ordinary data and exact edits

The model uses 64 patterns of four channel references. Text rows contain flags,
a space and four hex bytes. Flags are loop-start, loop-back and stop. A muted
channel retains its low six-bit SFX reference; changing mute does not replace it.
Missing patterns use `00 41424344`. No-op saves preserve exact bytes. Targeted
edits preserve unchanged hex case, LF/CRLF, unrelated rows and sections. Unknown
flags or channel encodings are preserved but refused for editing, not labelled
incompatible cartridges.

The editor follows pattern links to review channel conflicts; it does not simulate
music timing. The official runtime determines progression, including the leftmost
non-looping SFX duration, SFX loop/length settings and differing speeds. A stop flag
ends the sequence; loop-back searches for loop-start or pattern zero. Silent
patterns terminate the reviewed path. These rules need device comparison as part
of audio acceptance, especially unusual loop combinations.

References: [official PICO-8 0.2.7 manual, Music Editor and music()](https://www.lexaloffle.com/dl/docs/pico-8_manual.html)
and [picotool's music serialization implementation](https://github.com/dansanderson/picotool/blob/main/pico8/music/music.py).
Implementation is original PikoNest code; no runtime or library dependency is added.

## Scope and remaining work

This is basic composition, not completion of all S01–S05 work. Range insertion,
shifting/reordering many patterns, cross-project music/SFX library reuse, advanced
SFX filters/waveforms, a playback-position display and room/result music events
remain open. Unknown Lua is never guessed or rewired. Native playback, mute,
Stop/return, independent exported-cart audio, physical controls, novice and owner
review remain unperformed while console use is paused.

[Local evidence](showcase/0.0.83-en/README.md). Journal v8 retains v1–7 readers,
including old animation paint checkpoints and SFX drafts. This is a development
build, not a public APK release or beta-readiness claim.
