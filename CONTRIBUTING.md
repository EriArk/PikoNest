# Working on PikoNest

PikoNest is an Android development lab with a growing portable core, not yet a
complete creator or a public APK release. Start with the [current scope](docs/README.md),
[open tasks](docs/ISSUES.md) and [contributor brief](AGENTS.md).
Original PikoNest code uses the [MIT License](LICENSE); third-party licenses keep
their own scope. See [notices](THIRD_PARTY_NOTICES.md).

## Local build

The documented path uses Windows PowerShell, a JDK with `javac` available,
Android SDK Platform 34 and Build Tools 36.0.0. The lab documents JDK 21;
Android Studio's JBR is also used locally. There is no Gradle wrapper.
The default integrated build also needs Python 3 and the hash-pinned open-source
wrapper inputs listed in [android-integrated](experiments/android-integrated/README.md).
It does not need the purchased PICO-8 archive to build.
From the repository root:

```powershell
.\experiments\android-host\build.ps1
# Or supply your actual locations:
.\experiments\android-host\build.ps1 -JdkRoot 'C:\path\to\jdk' -SdkRoot 'C:\path\to\Android\Sdk'
```

The script runs the host's portable tests, then compiles, packages, signs and
verifies `.local/artifacts/pikoos-runtime-lab.apk`. A local development key is
created in `.local/artifacts/`; keep it to update your own installation. Building
does not install or publish. Never commit keys or APKs.

Standalone cartridge framing tests need only the JDK:

```powershell
.\experiments\p8-roundtrip\test.ps1
```

Both paths were checked on 6 October 2026 against a clean source snapshot of
`9c900e4` using the existing Windows toolchain. This was not a fresh-machine setup
test or runtime-wrapper rebuild. A PowerShell 5.1 combined-stream redirection
wrapper can promote native compiler warnings to errors; use the direct commands
above when diagnosing that behavior.

## Optional device work

The APK builds without proprietary PICO-8. Import the purchased Raspberry Pi ZIP
through runtime setup; 0.0.71 prepares and launches it within the main application.
No separate helper is required by the integrated path. Old data-folder import and
isolated clean private setup are checked; factory-clean onboarding and full
migration remain unfinished. Preserve old apps/data while validating.
The optional [clean validation variant](experiments/android-integrated/README.md#isolated-clean-install-validation)
uses its own private namespace; it is not a second deliverable APK.
The separate adapter build is only a legacy migration/research option.

```powershell
adb devices
adb -s YOUR_DEVICE_SERIAL install -r .local/artifacts/pikoos-runtime-lab.apk
adb -s YOUR_DEVICE_SERIAL shell am start -n art.pikoos.runtimelab/.MainActivity
```

Preserve projects and saves before device work. A signature mismatch is not a
reason to uninstall or clear data. The integrated label is PikoNest; existing package IDs remain
intentional until the [coordinated migration](docs/PROJECT_NAME.md).

## Repository map

| Path | Purpose |
| --- | --- |
| `experiments/android-host/core/` | Portable editing models and interaction workflows |
| `experiments/android-host/src/` | Android UI, persistence and runtime adapters |
| `experiments/android-host/tests/` | Portable data/workflow checks and fixtures |
| `experiments/p8-roundtrip/` | Byte-preserving `.p8` framing and edits |
| `experiments/runtime-restart/` | Separate experimental runtime integration |
| `experiments/ux-study/` | Historical browser UX simulation, not the Android app |
| `docs/` | Product contracts, planning and acceptance |
| `docs/design/` | Historical evidence for bounded lab changes |
| `docs/showcase/` | Screenshots with version/provenance notes |
| `tools/showcase/` | Isolated English presentation-build tooling |
| `.local/` | Ignored builds, private inputs and device evidence |

## Choosing and submitting work

- Use the [issue map](docs/ISSUES.md). Detailed IDs/status live in
  [BACKLOG](docs/BACKLOG.md); old lab notes do not set current priorities.
  Discuss substantial scope or architecture changes in the relevant issue first.
- Keep ordinary `.p8` and official PICO-8 authoritative. Unsupported editor
  operations preserve source data. General tools must not require a hero/template.
  Do not change technical IDs as incidental cleanup.
- Describe the checks actually run. Distinguish model tests, emulator input,
  physical controls and official-runtime results. Docs-only edits need link and
  consistency checks, not an APK rebuild.
- Include versioned screenshots for meaningful UI changes and describe the
  verified interaction. Preserve the PICO-8 style and controller-first operation.
- Keep purchased binaries, credentials, device serials and personal paths out of
  submissions. Share a minimal owned cart instead of another author's full game.

Use the bug template for expected/observed behavior, build/device and reproduction
steps. Feature requests should describe a user workflow and related backlog IDs.

## Validation and release policy

Owner decision, 6 October 2026: **GitHub Actions are disabled; checks run locally.**
Do not introduce Actions workflows, bot updates, automated merges or automated
releases without a new owner decision. Issue/PR templates are static forms.
APK publication needs explicit owner approval of a particular release after
product acceptance. Passing tests or pushing source is not that approval.

## Clones from before the history cleanup

On 6 October 2026, purchased runtime archives were removed from the published Git
history. If your clone predates that cleanup, preserve any uncommitted work and
clone the repository again before contributing. Do not merge or push the old
history back. Coordinate unpublished commits with the maintainer for selective
reapplication. Historical evidence may still name pre-cleanup commit hashes.
