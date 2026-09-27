# RESTYLE_PROGRESS.md — Yokai iOS-style restyle

Running log required by the standing rules. **Read this file before touching anything.** It is the
source of truth for "where did the last session stop"; `DESIGN.md` §11–§17 is the *why* behind each
change, and this file is the *what + verified-or-not* ledger.

## How to read the verification column

| Mark | Means |
|---|---|
| `CI ✓` | GitHub Actions `CI Build` (compile + unit tests + lint) passed on that commit. |
| `DEVICE ✗` | **Not** confirmed on the Realme 6. This is the default for everything so far. |
| `DEVICE ✓` | Seen working on the Realme 6, API 30, dark mode, 1080×2400. |

**Nothing in this project has `DEVICE ✓` yet.** CI only ever proves compilation, unit tests and
lint. It cannot prove a radius, a contrast ratio, a blur, or a spring. Treat every visual claim
below as unverified until a device pass says otherwise.

Target device: **Realme 6 (`RMX2001_11.C.18`), Android 11, API 30** — tier `Scrim`. No
`RenderEffect` (31+), no AGSL (33+). `minSdk = 26` (`buildSrc/src/main/kotlin/AndroidConfig.kt`).

## Current position

- **Phase 0 — tokens: BUILT, `CI ✓`, `DEVICE ✗`.**
- **Phase 1 — layout overlaps: BUILT, `CI ✓`, `DEVICE ✗`.** Commit `dad1d91494`.
- **Phase 2 — floating-chrome blur: BUILT, `CI ✓`, `DEVICE ✗`, then REBUILT as the drawn glass
  recipe** (the user ruled the original brief authoritative: no backdrop capture, no BlurView).
  First build was commits `60c8aff606`, `e22abd1e2a`, docs `81f960ce55`; the rebuild supersedes all
  three - see "Phase 2 (rebuilt)" below. **The rebuild is the current state of the tree.**
- **Phase 3 — morphing nav indicator: BUILT, `CI ✓`, `DEVICE ✗`.** Commits `3cd390a34d`,
  `fbdc3413fe`, `760b05942f`, docs `22f4465df0`.
- **Phase 4 — motion pass: NOT STARTED.**
- **Phase 5 — final consistency pass: NOT STARTED.**

**The pipeline is stopped here, on purpose.** Phases 2 and 3 are each gated on the user confirming
the previous phase *on device*; neither has been confirmed. Do not start Phase 4 until both are
confirmed, per "each phase must be visibly correct before starting the next".

---

## Phase 0 — Theme tokens

Built in the pre-Phase-1 work recorded in `DESIGN.md` §11–§14. Central tokens, no per-screen
magic values.

| File | Change | Why | Verified |
|---|---|---|---|
| `presentation/theme/src/main/java/yokai/presentation/theme/Theme.kt` | Palette, `GlassColors`, radii, spacing; `glassTier()` / `glassTierFor(sdkInt)`, `glassTintAlpha(context)`, `glassTintAlphaState()`, `glassBlurTintColor(context)` | One token source for every surface; tier is a function of `Build.VERSION.SDK_INT` so API 30 resolves to `Scrim` | CI ✓ / DEVICE ✗ |
| `presentation/theme/.../Typography.kt` | Inter type scale (large title / title / body / caption) | §4 type scale, geometric sans | CI ✓ / DEVICE ✗ |
| `presentation/theme/.../Constants.kt` | Spacing + radius constants | Same reason | CI ✓ / DEVICE ✗ |
| `presentation/theme/.../GlassSurface.kt` | `View.applyGlass`, `applyGlassDecorators` (specular sheen + 1px mode-aware rim), `applyGlassBackdropPane` | The single reusable glass recipe; squircle outline clip rather than a plain rounded rect | CI ✓ / DEVICE ✗ |
| `presentation/theme/.../GlassMotion.kt` | Spring specs; `performGlassClickHaptic` (`CONTEXT_CLICK`), `performGlassConfirmHaptic` (`CONFIRM` on API 30, else `VIRTUAL_KEY`) | §4.5 motion + §4 haptics; uses the **platform** `View.performHapticFeedback`, no new dependency | CI ✓ / DEVICE ✗ |
| `presentation/theme/.../GlassDialog.kt`, `GlassBackdrop.kt` | Dialog material; tier-1/tier-2 backdrop plumbing | Dialogs are glass surfaces; backdrop supplies tier 1/2 only | CI ✓ / DEVICE ✗ |
| `app/src/main/res/font/inter_{regular,medium,semibold,bold}.ttf` + `inter.xml` | Replaced the eight corrupt TTFs (they were GitHub HTML saved as `.ttf` and would crash on first inflate) with real Inter 4.1 statics | App would not render text at all otherwise | CI ✓ / DEVICE ✗ |
| `app/src/main/res/values{,-night}/colors.xml`, `styles.xml`, `themes.xml` | Token-backed colours/styles, incl. `Widget.Tachiyomi.BottomNavigationView.Glass`, `Widget.Tachiyomi.NavigationRail` | XML chrome needs the same tokens as Compose | CI ✓ / DEVICE ✗ |

Known open item from §14: whether the lifted dark veil is *too* present at the 95% stop
(95% × `GLASS_DARK_BASE_ALPHA` 0.50 ≈ 48% white). Unresolved — **not** verified either way.

---

## Phase 1 — Layout overlap fixes (commit `dad1d91494`)

Flat colours only, no blur. On-device screenshots (`currentappss/`, 9 shots) proved the concrete
overlaps; the fixes below are the result. **The user has not yet confirmed zero overlaps**, which
is Phase 1's own exit criterion.

| File | Change | Why | Verified |
|---|---|---|---|
| `app/src/main/java/eu/kanade/tachiyomi/ui/main/FloatingNavInsets.kt` | New pure helper: `topInsetFor(systemTop, appBarHeight, margin)`; clamped bottom-inset arithmetic | Top-bar gap had drifted per screen (Library +12dp ad-hoc, Recents +48dp); one helper + one 12dp margin token replaces both | CI ✓ (unit tested) / DEVICE ✗ |
| `app/src/main/java/eu/kanade/tachiyomi/ui/source/browse/BrowseSourceController.kt` | Popular/Latest/Filter bar reserves only `8dp + system inset` | It was reserving `bottom_nav_total_height` on a screen where the nav is *not shown* (a pushed controller hides the pill), floating the bar ~76dp into the cover grid | CI ✓ / DEVICE ✗ |
| `app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryController.kt` | `updateFilterSheetY()` reserves pill height **+ margin** | It reserved only the height, so the segmented control sat under the pill's top edge by exactly the 8dp margin | CI ✓ / DEVICE ✗ |
| `app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerTransitionHolder.kt` | Reserve the reader chrome height as bottom padding | The page-seekbar pill overlaid the chapter-transition text | CI ✓ / DEVICE ✗ |
| `app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/WebtoonTransitionHolder.kt` | Same | Same bug on the webtoon reader | CI ✓ / DEVICE ✗ |
| `app/src/main/java/eu/kanade/tachiyomi/util/view/ControllerExtensions.kt` | Content padding routed through `FloatingNavInsets` | Consistency; kills per-screen magic numbers | CI ✓ / DEVICE ✗ |
| `app/src/main/res/values{,-night}/colors.xml` | `content_top_margin = 12dp` | The single top gap token | CI ✓ / DEVICE ✗ |
| `app/src/test/java/eu/kanade/tachiyomi/ui/main/FloatingNavInsetsTest.kt` | New; incl. "only ever *increase* a screen's bottom padding" | The inset arithmetic is the part that is invisibly wrong when wrong | CI ✓ / DEVICE ✗ |

---

## Phase 2 — Glass on the floating chrome (commits `60c8aff606`, `e22abd1e2a`)

Applies to the four floating surfaces only: top card, bottom nav pill, circular search button and
the w720dp rail. List items, cover cards, sheets and the reader page are content and stay flat —
per the brief's own rule. Full rationale in `DESIGN.md` §16.

> **✅ Resolved 2026-09-27.** The original brief (no backdrop capture, no BlurView, no RenderEffect)
> was confirmed as the spec of record, so the BlurView build below was **replaced** by the drawn
> glass recipe. The table is kept as the record of what §16 described and why; do not restore it.
> `DESIGN.md` §16 now carries a "superseded by §18" note and §18 documents the rebuild.

| File | Change | Why | Verified |
|---|---|---|---|
| `gradle/libs.versions.toml` | `blurview = "version-2.0.6"` + `com.github.Dimezis:BlurView` | 3.x requires a breaking `BlurTarget` wrapper; 2.0.6 is the last line with `setupWith(ViewGroup)`, which auto-picks `RenderEffectBlur` on 31+ and `RenderScriptBlur` below. Jitpack tags are `version-<semver>`. | CI ✓ / DEVICE ✗ |
| `app/build.gradle.kts` | `implementation(libs.blurview)` | Same | CI ✓ / DEVICE ✗ |
| `app/src/main/java/eu/kanade/tachiyomi/ui/main/GlassBlurChrome.kt` | New. Builds/sets up the four BlurView panes, applies the 40–55% black legibility veil (`setOverlayColor`), the rounded transparent pane + rim/sheen, and mirrors nav `translationY`/`alpha`/`isVisible` via `OnPreDrawListener`; `updateTint()`, `detach()` | A sibling BlurView does not follow hide-on-scroll's `translationY`, so it has to be copied every frame. Blur source is `controller_container`, not the decor view, so the chrome is never sampled into its own backdrop | CI ✓ / DEVICE ✗ |
| `app/src/main/res/layout/main_activity.xml` | `card_blur` (margins 10/4/10/4) above now-transparent `card_view`; `bottom_nav_blur` (0dp×0dp, constrained to all 4 edges) declared **before** `bottom_nav`; `bottom_nav_search_blur` inside the search button | Each BlurView must be a *sibling behind* its surface — nesting samples the surface's own drawing | CI ✓ / DEVICE ✗ |
| `app/src/main/res/layout-w720dp/main_activity.xml` | Same `card_blur` + transparent `card_view`; new `side_nav_blur` before `side_nav`; `bottom_nav_search_blur` in the rail's search button | Tablet parity | CI ✓ / DEVICE ✗ |
| `presentation/theme/.../GlassSurface.kt` | New `View.applyGlassBackdropPane(cornerRadiusDp, circle)` — transparent rounded `GradientDrawable` + outline provider + `clipToOutline` | BlurView is a `FrameLayout` and supplies no rounded clipping; it carries the geometry, `applyGlassDecorators` carries the rim/sheen | CI ✓ / DEVICE ✗ |
| `presentation/theme/.../Theme.kt` | `GLASS_BLUR_TINT_MIN/MAX_ALPHA` (0.40/0.55) in `GlassColors`; `glassBlurTintColor(context)` | Maps the §2.2 transparency slider into a fixed veil band — more transparent ⇒ stronger veil, so legibility does not swing with the slider | CI ✓ / DEVICE ✗ |
| `app/src/main/res/values{,-night}/colors.xml` | `glass_blur_radius = 14dp` | Single radius token | CI ✓ / DEVICE ✗ |
| `app/src/main/java/eu/kanade/tachiyomi/ui/base/ExpandedAppBarLayout.kt` | `cardFrame` no longer gets `applyGlass(24f)` / decorators; `backgroundColor = null` | The flat "fake glass" fill had to go or it would sit under the blur as a grey wash | CI ✓ / DEVICE ✗ |
| `app/src/main/java/eu/kanade/tachiyomi/util/view/ControllerExtensions.kt` | `setAppBarBG` — both `cardView` branches now `setCardBackgroundColor(Color.TRANSPARENT)`; dropped the now-unused `ColorStateList` import | Same; the scroll-blend had no card colour left to animate | CI ✓ / DEVICE ✗ |
| `app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt` | `glassBlurChrome` created after `setContentView`, attached to `binding.controllerContainer`; `refreshGlassChrome()` → `updateTint()`; detached + nulled in `onDestroy`; dropped the `FloatingGlassNavController` / `glassTier` imports and `NAV_CORNER_RADIUS_DP` | Wiring, and removal of the dead path | CI ✓ / DEVICE ✗ |
| `presentation/theme/.../FloatingGlassNavController.kt` | **DELETED** (`e22abd1e2a`) | Its `applyGlass` fill *was* the banned fake glass; `GlassBlurChrome` replaced it | CI ✓ / DEVICE ✗ |
| `app/src/main/res/values/styles.xml` | `Widget.Tachiyomi.NavigationRail` comment repointed at `GlassBlurChrome` | Stale reference | CI ✓ / DEVICE ✗ |
| `DESIGN.md` | New §16; cleaned two stale §13 mentions | Record | n/a |

**Perf lever on this device:** the downsample factor (currently BlurView's default, 6) — **not** a
lower radius. If frames drop on scroll, raise the downsample factor first.

**Also unresolved in §16:** whether `fakeBottomNavView` (a solid `colorPrimaryVariant` View drawn
during `PUSH_EXIT`, `ControllerExtensions.kt` ~470–484) fights the material. Unverified.

---

## Phase 2 (rebuilt) — the drawn glass recipe

The user ruled the original Phase 2 wording authoritative, so the BlurView build above was ripped
out and replaced by a component the app draws itself. Rationale and platform constraints are in
`DESIGN.md` §18; this is the ledger.

| File | Change | Why | Verified |
|---|---|---|---|
| `presentation/theme/.../GlassRecipe.kt` | **New.** The single recipe: ramp alphas (18/8 dark, 14/6 light), noise (4%, 64px tile, fixed seed), rim (55/40% white, top 60%), glow/shadow params, sweep params, `baseFillAlpha` / `baseFillColor`, `fillGradientColors`, `sweepBandAlpha`, `noisePixels` | One set of numbers for both the View and Compose hosts. Deliberately **free of `android.*` types** and packs ARGB by hand: the JVM unit tests have no Robolectric and no `returnDefaultValues`, so a single `android.graphics.Color.argb` call would make the whole file untestable | CI ✓ (13 unit tests) / DEVICE ✗ |
| `presentation/theme/.../Superellipse.kt` | **New.** `points` (parametric superellipse, exponent 4), `path`, and `rimPoints` — the contiguous top-edge run | A rounded rect built from arcs has a curvature discontinuity where the arc meets the edge; the iOS continuous curve does not. `rimPoints` exists as a pure function because the naive "filter the outline by `y`" bug makes the rim stroke jump across the surface | CI ✓ (8 unit tests) / DEVICE ✗ |
| `presentation/theme/.../GlassPane.kt` | **New.** `FrameLayout` custom view drawing the whole recipe in `onDraw`; assigns `circularCornerRadiusDp` / `circle` at runtime; `refreshMaterial()`, `playSpecularSweep()` | This *is* the Phase 2 component. The glow and the drop shadow are rasterised into one cached software `Bitmap` because `setMaskFilter()` is unsupported with hardware acceleration at **every** API level and `setShadowLayer()` (non-text) only from API 28, while minSdk is 26 — they are silently dropped otherwise, which is how glass renders correct on one device and flat on another | CI ✓ / DEVICE ✗ |
| `presentation/theme/.../GlassSurface.kt` | Compose host now draws the same recipe via `drawWithCache` (base fill, ramp, noise `BlendMode.Softlight`, shared rim path, `Modifier.shadow`); `applyGlassBackdropPane` deleted; the private `View.isNightMode()` made public | One recipe, two hosts — an XML screen and a Compose screen cannot drift. The outer shadow uses the platform because a Compose surface cannot draw outside its own bounds | CI ✓ / DEVICE ✗ |
| `presentation/theme/.../Theme.kt` | `GLASS_BLUR_TINT_MIN/MAX_ALPHA` and `glassBlurTintColor` **deleted** | The 40–55% veil existed only to make a blur legible; there is no blur now, and the fill carries legibility instead | CI ✓ / DEVICE ✗ |
| `app/src/main/java/.../ui/main/GlassChrome.kt` | **New**, replacing the deleted `GlassBlurChrome.kt`. Assigns radius/shape per pane, registers the nav-mirror pre-draw listener, `updateTint()`, `playSpecularSweep()`, `detach()` | The capture, the RenderScript context, the overlay veil and three of the four BlurView setups are gone; only what XML cannot express is left | CI ✓ / DEVICE ✗ |
| `app/src/main/res/layout/main_activity.xml`, `layout-w720dp/main_activity.xml` | `card_blur` → `card_glass`, `bottom_nav_blur` → `bottom_nav_glass`, `side_nav_blur` → `side_nav_glass`, `bottom_nav_search_blur` → `bottom_nav_search_glass`, all `BlurView` → `yokai.presentation.theme.GlassPane` | Same geometry and same z-order (each pane stays a sibling *behind* its surface); only the class and the name change | CI ✓ / DEVICE ✗ |
| `app/src/main/java/.../ui/main/MainActivity.kt` | `glassBlurChrome` → `glassChrome`, `attach()` with no arguments; `playSpecularSweep()` on nav item select | Wiring; the sweep is the beat that acknowledges a tab switch | CI ✓ / DEVICE ✗ |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | `blurview` version + library **removed** | No backdrop capture and no new dependency (the brief's whole point). Jitpack stays in `settings.gradle.kts` — other deps use it | CI ✓ / DEVICE ✗ |
| `app/src/main/res/values{,-night}/colors.xml` | `glass_blur_radius` **removed** | Nothing references it | CI ✓ / DEVICE ✗ |
| `app/src/main/res/values/styles.xml`, `ControllerExtensions.kt`, `ExpandedAppBarLayout.kt` | Comment/doc references repointed from the BlurView ids and `GlassBlurChrome` to `*_glass` and `GlassPane` | Stale references | CI ✓ / DEVICE ✗ |
| `app/src/test/java/yokai/presentation/theme/GlassRecipeTest.kt`, `SuperellipseTest.kt` | **New**, 13 + 8 tests | The ramp direction, the sweep reaching exactly zero at both ends, the scrim ignoring the slider, the noise being deterministic, the points lying on the superellipse, and the rim being one contiguous run — all invisible in CI and all "looks almost right" on a screenshot when wrong | CI ✓ / DEVICE ✗ |

**Deliberate behaviour change to flag on device:** the material is now *more opaque* than the
BlurView build was. That is §3's tier-3 rule plus the brief's legibility requirement, and it is a
visible difference, not a bug — check it over a bright cover before deciding to thin it.

**Still outstanding from the Phase 2 brief (not yet migrated to the component):**
- Bottom sheets — `GlassBottomSheetContainer` still paints an *opaque* container colour.
- Dialogs / popup menus — `GlassAlertDialog`, `GlassAlertDialogBuilder`,
  `MaterialAlertDialogExtensions` still paint opaque containers with a manual rim.
- The search bar — the `search_toolbar` inside the top card sits on the card's glass but is not
  itself a glass surface; the global-search screen's own bar is untouched.
- The reader's page-slider control — still the old treatment.
- The collapsed `mainToolbar` and the manga-details FAB still use `applyGlass` /
  `applyGlassDecorators` (the *older* fill path), not `GlassPane`. They look consistent today
  because both read the same tier and rim tokens, but they are two implementations of one recipe.

---

## Phase 3 — Morphing spring tab indicator (commits `3cd390a34d`, `fbdc3413fe`, `760b05942f`)

One pill that springs between the bottom nav's items, replacing Material's static per-item
indicator. Pure geometry, no blur dependency. Full rationale in `DESIGN.md` §17.

| File | Change | Why | Verified |
|---|---|---|---|
| `app/src/main/java/eu/kanade/tachiyomi/ui/main/MorphingNavIndicator.kt` | New `internal object`: `translationXFor`, `indexFor`, `stretchScalesFor`, `isLayoutUsable`; `MAX_STRETCH_X = 1.15f`, `MIN_STRETCH_Y = 0.9f`, deadband 350 px/s, full stretch 9000 px/s | The parts that are visibly wrong when wrong (a pill between two items, one that never relaxes) are only unit-testable if they are pure and Android-free | CI ✓ (12 unit tests) / DEVICE ✗ |
| `app/src/main/java/eu/kanade/tachiyomi/ui/main/MorphingNavIndicatorController.kt` | New. Single `SpringAnimation` over a `FloatValueHolder`; `dampingRatio = 0.6`, `STIFFNESS_MEDIUM`; update listener writes `translationX` + squash-stretch from reported velocity; `onItemSelected`, `snapToSelection`, layout listener, pre-draw `mirror`, `detach()` | The holder is the source of truth, not the view: `skipToEnd()` is a no-op on a non-running spring, so "snap" must write holder **and** view or the next spring launches from a stale position | CI ✓ / DEVICE ✗ |
| `app/src/main/res/drawable/glass_active_indicator.xml` | New capsule shape, corner `@dimen/bottom_nav_active_indicator_corner` (16dp), solid `@color/bottom_nav_active_indicator` (`#59FFFFFF` day / `#42FFFFFF` night) | Mode-aware for the same reason the §2.1 rim is: a dark pill cannot separate from a dark page | CI ✓ / DEVICE ✗ |
| `app/src/main/res/layout/main_activity.xml` | New `nav_tab_indicator` (56×32dp, `visibility="invisible"`), constrained top/bottom/start to `bottom_nav`, declared before `bottom_nav_blur` | Must sit *behind* the §16 blur veil so the pill reads as lit glass; `invisible` so a cold start never flashes it at the start edge | CI ✓ / DEVICE ✗ |
| `app/src/main/res/values/styles.xml` | `Widget.Tachiyomi.BottomNavigationView.Glass` now points `itemActiveIndicatorStyle` at new `Widget.Tachiyomi.EmptyIndicator` (parent `""`, 0dp, transparent); the w720dp rail keeps the real `ActiveIndicator` | Without this Material draws its **own** per-item pill underneath ours — two indicators. The rail keeps the Material one deliberately: a horizontal pill springing between columns of a vertical menu reads worse than the static one | CI ✓ / DEVICE ✗ |
| `app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt` | `morphingNavIndicator`; attached in `onCreate`; `nav.setOnItemSelectedListener` drives `onItemSelected`; `doOnLayout { snapToSelection() }`; detached + nulled in `onDestroy` | Wiring | CI ✓ / DEVICE ✗ |
| `app/src/test/java/eu/kanade/tachiyomi/ui/main/MorphingNavIndicatorTest.kt` | New, 12 tests. `TOLERANCE = 0.0001f` | Covers even distribution, inverse mapping, clamping, degenerate counts/widths, rest = exact capsule, deadband, peak = spec | CI ✓ / DEVICE ✗ |
| `DESIGN.md` | New §17 | Record | n/a |

Two CI failures were hit and fixed inside this phase — both worth remembering:
1. `fbdc3413fe` — Kotest's `shouldBeBetween(min, max)` needs a **third** `tolerance` argument
   (`No value passed for parameter 'tolerance'`).
2. `760b05942f` — with a zero-width nav, `translationXFor` returned *half a pill before the start
   edge*. It now returns `0f` when `navWidth == 0` (test "degenerate widths and counts fall back to
   the start edge", `MorphingNavIndicatorTest.kt:54`).

---

## Phase 4 — Motion pass: NOT STARTED

Nothing below has been done. Partial groundwork exists from Phase 0: `GlassMotion.kt` already
holds the spring specs and the three haptic helpers, and `ReaderActivity` / `Pager` /
`WebtoonRecyclerView` / `LibraryFastScroll` etc. already call `performHapticFeedback` with platform
constants — so the haptics wording ("if a haptics API is already available/used elsewhere in this
codebase — check before adding a new dependency") is satisfied *without a new dependency*.

Remaining Phase 4 work, from the brief:
- Replace remaining linear/tween-eased transitions app-wide (screen transitions, sheet open/close,
  dialog appear/dismiss, list-item press states) with springs matching Phase 3's
  damping/stiffness, so motion is consistent.
- Haptics on tab switch, sheet open and toggle switches (audit which of those three are still
  missing — the reader and fast-scroll paths already have theirs).
- Large collapsing title on Library / Recents / Browse: big → into the bar on scroll, with scroll
  performance verified (no layout pass per scroll pixel, no recomposition thrashing). Note §12
  already made the Compose content top inset *sticky at the expanded height*; that interaction
  needs checking on device as part of this.

## Phase 5 — Final consistency pass: NOT STARTED

Walk Library, Recents, Browse, source list, source grid/AllManga, manga detail, reader + reader
controls, popup menu, search/global search, settings; confirm one radii family, spacing scale,
glass recipe and type scale. Use **manga detail** (already close to correct in the original
screenshots) as the baseline quality bar. Then write the final changelog summary into this file,
listing screens confirmed on-device and explicitly flagging anything untouched.

---

## Blockers and environment notes for the next session

- **CI cannot prove visuals.** Compile + unit tests + lint only. Every visual claim above is
  `DEVICE ✗`.
- **Local Gradle is not a usable build oracle** in this Termux/proot environment. Verification is
  GitHub Actions only. `gh` is **not** authenticated; the token can be recovered with
  `TOKEN=$(git remote get-url origin | sed -n 's|https://[^:]*:\([^@]*\)@github.com/.*|\1|p')`.
  `jq` is at `/data/data/com.termux/files/usr/bin/jq`. CI logs need `tr '\r' '\n'` + `grep -a`.
- **CI shape:** `ci.yml` → `CI Build` with jobs `Build Debug APK` (`assembleStandardDebug` +
  `testStandardDebugUnitTest`) and `Lint & Type Check` (`lintStandardDebug`); plus
  `Mirror Repository`. Poll
  `https://api.github.com/repos/madsykle/yokai/actions/runs?per_page=6`, jobs at `/runs/$RID/jobs`,
  logs at `/actions/jobs/$JID/logs`. The workflow *name* for a commit is its subject line.
- **Signing secrets still absent on the fork** (`SIGNING_KEY`, `ALIAS`, `KEY_STORE_PASSWORD`,
  `KEY_PASSWORD`, `NIGHTLY_PAT`) ⇒ sign/publish/nightly steps skip. Expected, not a failure.
- **🔴 The Personal Access Token is still embedded in the `origin` remote URL and has NOT been
  rotated.** It has been used for every push this session. Rotate it and switch to a credential
  helper or SSH. This has been flagged repeatedly and is unresolved.
- `ref/` and `currentappss/` are untracked and must stay that way. Every commit uses
  `git add -A ':(exclude)ref' ':(exclude)currentappss'`.
- `currentappss/` holds 9 device screenshots (1080×2400, dark mode) from the Phase 1 pass;
  `/tmp/map.py` is an ASCII screenshot mapper used to analyse them.

## Verification queue (the user's next two device checks)

**Phase 2 (rebuilt)** — on a library scroll over a bright cover: does the top card and the pill
read as a *material* (ramp, lit top rim, a glow on its own edge, a shadow under it) rather than as a
flat translucent fill; are the labels legible over the busiest cover in the library; does the glass
stay glued to the pill through hide/show; and does the surface look smooth rather than banded (which
is what the noise layer is for)? Also check the sweep fires once on a tab switch and leaves nothing
behind. The material is deliberately more opaque than the BlurView build was — judge it, don't
assume it is a regression.

**Phase 3** — tap across tabs: does a single pill spring with a visible overshoot, stretch while
fast and relax to round, is there any second pill, and does it survive rotation and inset changes?

Only after both are confirmed may Phase 4 begin.

## Commit log (this branch, most recent last)

| Commit | What |
|---|---|
| `dad1d91494` | Phase 1 layout overlaps |
| `60c8aff606` | Phase 2 backdrop blur on the floating chrome |
| `e22abd1e2a` | Delete the unused `FloatingGlassNavController` |
| `81f960ce55` | `DESIGN.md` §16 (Phase 2 as-built) |
| `3cd390a34d` | Phase 3 morphing spring tab indicator |
| `fbdc3413fe` | Fix: Kotest `shouldBeBetween` tolerance |
| `760b05942f` | Fix: zero-width nav falls back to the start edge |
| `22f4465df0` | `DESIGN.md` §17 (Phase 3 as-built) |
| `215ebd4134` | Added this ledger |
| `abc0b8688e` | Phase 2 rebuilt as the drawn glass recipe (`GlassPane`/`GlassRecipe`/`Superellipse`, BlurView removed) — `DESIGN.md` §18 |
| `43ddff2ff9` | Fix: import `BitmapShader`; use `RoundRect`'s four-coordinate constructor |
| `74039e8d4d` | Fix: assert the noise grey with a range check (no Int matcher imported) |
| `c88b4f0ac4` | Fix: pin the sweep's ends to exactly zero; walk the rim left-to-right |

**Phase 2 rebuild status: `CI ✓` on `c88b4f0ac4`** (Build Debug APK incl. 21 new unit tests, and
Lint & Type Check). Four CI rounds were needed, and three of the four failures were real bugs the
tests caught rather than test-only noise:
1. `BitmapShader` was not imported (compile).
2. Compose has no `RoundRect(Offset, Size, CornerRadius)` constructor (compile).
3. `sin(PI)` in Float is `-1.2e-7`, not zero, so the specular band left a permanent trace of
   alpha behind after its pass — `sweepBandAlpha` now pins both ends to exactly `0f`.
4. `rimPoints` assembled the run right-to-left, so the lit rim started on the right-hand side.
   The "starts left of the middle" test caught it; the run is now walked `-reach..reach`.

One failure was a test bug: the superellipse test indexed the flat interleaved `x,y` array with a
*sample* index, so it read sample 24 instead of the topmost sample.
