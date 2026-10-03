## Agent skills

### Build & device loop

Builds run on **GitHub Actions**, not locally. Push to `master` (or open a PR against it) and let
`CI Build` (`.github/workflows/ci.yml`) produce the APK as the `yokai-madsykle-debug` artifact;
download that artifact and `adb install -r` it on the phone over Wireless debugging. CI compiles
and unit-tests but never sees the screen, and local `./gradlew` is not a usable build oracle on the
Termux/proot phone — so the CI artifact is the compiler and the phone is the only way to verify
anything visual. See `docs/agents/device-loop.md` before building or driving a device.

### Issue tracker

Issues live in this repo's GitHub Issues (`github.com/madsykle/yokai`, the active working fork), read/write via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

This repo uses the five default canonical triage role-strings — `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout: one root `CONTEXT.md` plus ADRs under `docs/adr/`. See `docs/agents/domain.md`.