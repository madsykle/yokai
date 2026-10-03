# Build & device loop (CI builds, Termux drives the phone)

Builds happen in **GitHub Actions**; `adb` runs **on the phone itself**, in Termux, talking to that
same phone over Wireless debugging. There is no local build host and no usable local Gradle: the CI
artifact *is* the APK, and the phone is the only place a visual change can be verified. CI compiles,
unit-tests and lints, but it never sees the screen.

```
GitHub Actions — .github/workflows/ci.yml (CI Build)
├── ./gradlew assembleStandardDebug --no-daemon
├── artifact: yokai-madsykle-debug   (7 days; ABI splits + universal)
└── ./gradlew testStandardDebugUnitTest, lintStandardDebug
Termux on the phone
├── gh / curl to download that artifact
└── adb (wireless debugging, to the phone itself)
    ├── install the APK
    └── logcat, uiautomator, screenshots, bugreport
```

Local `./gradlew` is **not** a usable build oracle in this Termux/proot environment — treat CI as
the compiler. Do not try to build the APK on the phone.

## Build (GitHub Actions)

`ci.yml` runs on **push / pull_request to `master`**, or a manual **workflow_dispatch** (Actions →
*CI Build* → *Run workflow*, then pick the branch). A feature branch that is merely pushed does
**not** build on its own — open a PR against `master`, or dispatch it manually.

| Workflow (name) | Trigger | Produces | Installable? |
|---|---|---|---|
| `ci.yml` — *CI Build* | push / PR to `master`, or manual dispatch | `assembleStandardDebug`, artifact `yokai-madsykle-debug` (debug-signed) | **Yes — this is the one** |
| `build_check.yml` — *Build PR* | PRs | `assembleStandardRelease`, artifact `arm64-v8a-<sha>` | No — release has no signing config on this fork |
| `build_push.yml` — *Build app* | `master` / manual | signed release + nightly | No — signing secrets absent on the fork |
| `mirror.yml` — *Mirror Repository* | `master` | mirror push | n/a |

The `yokai-madsykle-debug` artifact holds `app/build/outputs/apk/standard/debug/*.apk`. ABI splits
are enabled with `isUniversalApk = true`, so expect `*-universal.apk` **plus** one APK per ABI —
install the universal one unless you are chasing an ABI-specific problem.

`dev` (en-only, no Firebase/Google Services) and `nightly` flavours are **not** built by `ci.yml`.
If you need one, add a task to the workflow or use `build_push.yml`'s manual dispatch; do not build
it locally.

## Get the APK

```bash
gh auth status                                         # see "auth" below if this fails
gh workflow run ci.yml --ref <branch>                  # only if you can't wait for a push/PR
gh run list --workflow ci.yml --limit 5                # find the run for your commit
gh run watch <run-id>                                   # wait for "Build Debug APK" to go green
gh run download <run-id> -n yokai-madsykle-debug -D "$PREFIX/tmp/yokai-apk"
```

`gh run download` preserves the artifact's relative paths, so the APK lands under
`$PREFIX/tmp/yokai-apk/app/build/outputs/apk/standard/debug/`. There is no `/tmp` in Termux — use
`$PREFIX/tmp` for scratch space.

### Auth

`gh` on this phone has been unauthenticated. Either run `gh auth login`, or recover the token that
is embedded in the `origin` remote URL:

```bash
TOKEN=$(git remote get-url origin | sed -n 's|https://[^:]*:\([^@]*\)@github.com/.*|\1|p')
```

**That personal access token is still embedded in the remote and has not been rotated.** Rotate it
and switch to SSH or a credential helper. Without `gh` auth, fetch the artifact through the GitHub
API/UI with that token instead of `gh run download`:

```bash
curl -L -H "Authorization: Bearer $TOKEN" \
  -o "$PREFIX/tmp/yokai-apk.zip" \
  https://api.github.com/repos/madsykle/yokai/actions/artifacts/<artifact-id>/zip
```

CI logs are CRLF-heavy and ANSI-coded; read them with `tr '\r' '\n'` + `grep -a`.

## Wireless debugging to the phone itself

`adb` reaches the phone over Wi-Fi. It does **not** work on mobile data or while the phone is
serving its own hotspot.

Developer options › Wireless debugging › *Pair device with pairing code*. Pair **immediately** after
reading the code — a stale code fails with a protocol fault. Split-screen with Termux makes reading
the code and typing it survivable.

```bash
adb pair 127.0.0.1:<pairing-port> <code>      # pairing port is on the pairing dialog
adb connect 127.0.0.1:<port>                  # the main Wireless debugging screen, a *different* port
```

Pairing survives reboots; after a reboot only `adb connect` is needed, and the connect port changes
every time.

**Always pass `-s`.** `adb devices` can list a phantom `emulator-5554` next to the real phone, and on
this device the phone itself can show up *as* `emulator-5554` (a realme RMX2001, `ro.serialno`
`6T9PMNG6OZ7PQKV4`). Identify the target by asking it, not by trusting the list:

```bash
adb devices -l
adb -s <serial> shell getprop ro.product.model     # confirm it is the phone, not an emulator
export ANDROID_SERIAL=<serial>                     # or set this once and drop -s
```

## Install and launch

```bash
adb -s $ANDROID_SERIAL install -r \
  "$PREFIX/tmp/yokai-apk/app/build/outputs/apk/standard/debug/app-standard-debug-universal.apk"
adb -s $ANDROID_SERIAL shell am start -n \
  eu.kanade.tachiyomi.madsykle.debugYokai/eu.kanade.tachiyomi.ui.main.MainActivity
```

The activity lives in the library namespace `eu.kanade.tachiyomi`; only the **application id** carries
the suffix — `<base>.debugYokai` for debug, `<base>.yokai` for release, `<base>.nightlyYokai` for
nightly, base being `eu.kanade.tachiyomi.madsykle`. Launch with `am start -n`, never `monkey`.

## Look at it

```bash
adb logcat                                     # for what the app's own log misses
adb shell uiautomator dump && adb shell cat /sdcard/window_dump.xml   # coordinates of a view
adb shell input tap X Y
adb exec-out screencap -p > $PREFIX/tmp/shot.png
adb bugreport bugreport.zip                    # includes the Bluetooth HCI log
```

A test that rotates the screen must put auto-rotate back:

```bash
adb shell settings put system accelerometer_rotation 0
```

`setprop` debug switches persist across reboots too — this repo uses
`adb shell setprop debug.glass.ramp_only 1` to isolate the glass ramp/rim from the shadow (see
`RESTYLE_PROGRESS.md`), and `0` resets it.

## Commit hygiene

`ref/` and `currentappss/` hold local reference material and device screenshots; they stay untracked.
Never plain `git add -A`:

```bash
git add -A ':(exclude)ref' ':(exclude)currentappss'
```
