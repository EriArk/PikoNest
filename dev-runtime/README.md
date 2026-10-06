# Local development runtime inputs

Each developer supplies their own purchased official PICO-8 runtime. Keep it in
ignored `.local/` or `dev-runtime/pico-8/`; never commit, upload or include it in
an application package. Host builds and portable data tests need no runtime ZIP.

For the Android ARM64 experiment see the backend-specific archive guidance in
[first-run setup](../docs/FIRST_RUN_SETUP.md) and
[runtime integration](../docs/ANDROID_RUNTIME_POC.md). Linux x86 archives are not
substitutes for the ARM64 runtime used on the handheld.

Earlier private development commits stored owner-supplied archives here.
Housekeeping on 6 October 2026 removes them from the current tree while keeping
local originals. **Older Git history still contains them pending a separately
approved cleanup.** Ignoring or untracking a file does not purge its history.

See [compatibility rules](../docs/PICO8_COMPATIBILITY.md). Local experiments do not
replace the end user's license or prove independent clean-device onboarding.
