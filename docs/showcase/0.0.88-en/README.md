# Lab 0.0.88 — verified external copies

Local implementation, 1 November 2026. Commit source and checks do not constitute
native/owner acceptance. No console use, installation, new screenshots or APK release.
APK SHA-256: `85619c370c454dccf0fc71ca4e8cac8c14b77251596bf9444cf6ae9ec9e10c65`.

Folders → Projects / Asset library → Start connects new-copy and resume actions to
the portable transfer engine and Android SAF adapter. See [scope](../../STORAGE_COPY.md).

- Full local build: 86 core suite invocations passed, including VerifiedTransferTest
  (36 assertions). Scenarios include exact binary/text/metadata preservation,
  cancellation between files/resume, provider truncation, mismatched existing files,
  lost access, changed source contents/file list, invalid paths/case collisions,
  empty library and failed receipt publication. Sources are retained throughout.
  The final focused transfer run passed 36 assertions (35 in the preceding full
  run); a reserved receipt-name case guard and viewport fill were then recompiled
  and repackaged without repeating unaffected suites.
- Eleven supplemental JVM checks ran the actual StorageCopy local-source adapter
  against temporary files: project/library separation, on-disk metadata, exact bytes,
  exclusion of unrelated runtime files and path-escape refusal. These do not execute
  Android DocumentsProvider calls.
- Single APK compiled, signed and package-verified: one launcher, private runtime
  process, 263 expected Godot assets and no purchased PICO-8 binaries in APK/bootstrap.

The native UI/SAF adapter is compiled; provider permissions, stream durability,
recreation/process death, touch/controller navigation and visual layout still need
authorized device checks. No seeded UI image is used as evidence of file transfer.
Existing state tests do not prove physical buttons or Android provider behavior.

Copies exclude app preferences, unfinished editor drafts, saves, runtime and Splore.
Partial files stay untouched and require New copy. Candidate directories are kept.
Working storage is still private; external activation and full restore are unfinished.
Next: shared persistence port and journaled activation/reopen/access-loss recovery.
