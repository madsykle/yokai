Yokai Liquid Glass Redesign

Agent context file. Read this before generating any UI code. This is not documentation; it is a constraint system.

0. Prime Directive

Redesign Yokai's UI to feel like a native iOS 27 app while running on Android 10+ (API 29+). The visual language is Liquid Glass as defined by Apple's Human Interface Guidelines, adapted for Android's rendering stack. Do not generate "generic rounded-corner Material" output. Every component decision must trace back to a rule in this document.

What this project is not:

· Not a Material You app with iOS-ish corners.
· Not a full rewrite. Yokai is a hybrid Views/XML + Compose app. Work within the existing architecture.
· Not a clone of iOS. Glass is a tool for hierarchy, not decoration.

---

1. Design Philosophy (Non-Negotiable Rules)

These rules come from Apple's HIG and override any aesthetic instinct.

1.1 The Functional Layer Rule

Liquid Glass belongs only to navigation, controls, and overlays. Never apply it to content itself — no glass list items, no glass manga cards, no glass table cells.

In Yokai terms:

· ✅ Bottom nav, top app bar, sheets, reader overlay controls, floating action buttons.
· ❌ Library grid items, manga detail rows, chapter lists, reader pages.

1.2 No Glass on Glass

Never stack glass surfaces. If a sheet contains a glass control, the sheet background must be opaque or a standard material. Glass components should be visually separated by content or solid surfaces.

1.3 Restraint

Use Liquid Glass sparingly. Cap at 2–3 glass surfaces per screen. More than that destroys hierarchy and tanks frame rates on mid-range Android devices.

1.4 Content Must Shine Through

Glass adapts its tint to what is behind it. Sample backdrop luminance and switch icon/label color between light and dark automatically. If the underlying content is bright, add a dark dimming layer at ~35% opacity.

1.5 Accessibility Is Default

Liquid Glass must adapt to Reduced Transparency, Increased Contrast, and Reduced Motion. Provide a transparency slider in settings that maps to the user's accessibility preference.

---

2. iOS 27 Liquid Glass Specifics

iOS 27 (codenamed "Golden Gate") refined Liquid Glass from the iOS 26 introduction. The agent must target these refinements, not the 2025 demos.

2.1 What Changed in iOS 27

· Darkened edge: Glass elements now have a distinct darkened rim for visual separation.
· Brighter specular highlights: Light reflections are more pronounced.
· Transparency slider: User-adjustable from ultra clear to fully tinted. Default is less transparent than iOS 26.
· Removed dynamic motion specular: The highlights no longer react to device movement on Home Screen/Control Center.

2.2 Implementation Targets for Yokai

iOS 27 Feature Android Implementation
Darkened edge 1px Color(0x33000000) border on glass surfaces
Brightened specular Subtle white overlay gradient (alpha 0.08–0.12) on top edge
Transparency slider SharedPreferences value mapped to glass tint alpha
Default opacity Frosted-diffuse, not ultra-clear. Base tint alpha ≈ 0.72

---

3. Tiered Implementation Strategy (Android 10+)

Real Liquid Glass refraction requires AGSL, which is API 33+. RenderEffect blur starts at API 31. Android 10 (API 29) has neither. The agent must implement three tiers behind a single API.

Tier 1 — API 33+ (Full Liquid Glass)

· Library: io.github.kyant0:backdrop:2.0.1 (Kyant0's AndroidLiquidGlass / Backdrop).
· Capabilities: AGSL shader-based refraction, lensing, custom shader effects.
· Note: Backdrop is now a Compose Multiplatform library. It records Compose layers as backdrops. For Yokai's XML screens, wrap the backdrop source in AndroidView and test on one screen before rolling out.

Tier 2 — API 31–32 (Blur + Tint)

· Library: dev.chrisbanes.haze:haze for Compose, or RenderEffect directly for Views.
· Capabilities: Hardware-accelerated blur, no refraction. Haze requires workarounds on API 31–32 (progressive effects use masks). Blur must be manually invalidated on API 31.
· Fallback behavior: Haze uses a translucent scrim below API 31 by default.

Tier 3 — API 29–30 (Scrim Fallback)

· No blur, no refraction. Use a tinted, near-opaque bar with:
  · Color(0xE6F2F2F7) (light) / Color(0xE61C1C1E) (dark) background
  · 1px light top edge (Color(0x1AFFFFFF))
  · Soft shadow
· This mirrors Apple's Reduce Transparency mode and must look deliberate, not broken.

Library Decision Matrix

API Level Blur Refraction Recommended Library
33+ ✅ ✅ Kyant0 Backdrop
31–32 ✅ ❌ Haze (with workarounds)
29–30 ❌ ❌ Custom scrim composable

Do not use: RenderScript (deprecated since Android 12, CPU-only on newer devices).

---

4. Design Tokens (Agent Must Use These)

4.1 Color Palette (Flat, Neutral, iOS-Like)

```
GlassLightBase:    #F2F2F7 (92% opacity for scrim fallback)
GlassDarkBase:     #1C1C1E (92% opacity for scrim fallback)
GlassTintLight:    #FFFFFF at 12% (over light content)
GlassTintDark:     #000000 at 35% (over bright content)
AccentBlue:        #007AFF
AccentGreen:       #34C759
AccentRed:         #FF3B30
LabelPrimary:      #000000 / #FFFFFF (auto-switch)
LabelSecondary:    #3C3C43 at 60% / #EBEBF5 at 60%
```

4.2 Typography

· Font: Inter (do not ship SF Pro for legal reasons).
· Scale: iOS Dynamic Type ramps (LargeTitle 34, Title1 28, Title2 22, Headline 17 semibold, Body 17, Callout 16, Subhead 15, Footnote 13, Caption 12).
· Line height: 1.2× for titles, 1.4× for body.

4.3 Shapes (Squircles, Not Rounded Rects)

Use continuous corner curves, not standard RoundedCornerShape. On Compose, use RoundedCornerShape with CornerBasedShape and a high smoothing value. On Views, use ShapeAppearanceModel with setCornerSize and a continuous curve interpolator.

Element Corner Radius
Bottom nav pill 28dp
Sheets 24dp (top corners)
Buttons 14dp
Cards (non-glass, content) 12dp

4.4 Spacing

Base unit = 4dp. Use 8, 12, 16, 20, 24, 32. Never use arbitrary values like 13dp or 27dp.

4.5 Motion

· All animations use spring physics. No linear, no tween().
· Compose: spring(dampingRatio = 0.75f, stiffness = 300f).
· Views: SpringAnimation from androidx.dynamicanimation.
· Haptics: Trigger on tab switches, toggles, and sheet dismiss. Use HapticFeedbackConstants.CONTEXT_CLICK and CONFIRM.

---

5. Component Specifications

5.1 Floating Pill Bottom Nav

The single biggest iOS tell. Must float with margin, not dock to the screen edge.

```
- Container: Glass surface, corner radius 28dp
- Margin: 16dp horizontal, 8dp bottom (plus system nav bar inset)
- Content scrolls UNDER the bar (this makes the blur visible)
- Icons: 24dp, label 10dp below
- Active state: tinted with accent color
- Tier 3 fallback: solid tinted bar, same shape and margin
```

5.2 Collapsing Large Title Top Bar

```
- Expanded: 34pt LargeTitle, left-aligned, no glass
- Collapsed: 17pt inline title, glass background appears
- Transition: spring-based, tied to scroll position
- Glass appears only when content scrolls under the bar
```

5.3 Bottom Sheets

```
- Corner radius: 24dp top
- Grabber: 36dp wide, 5dp tall, centered, 8dp from top
- Background: opaque (no glass — rule 1.2)
- Dimming: 35% black scrim behind sheet
```

5.4 Reader Overlay Controls

· Glass only on the overlay chrome (top bar, bottom controls).
· Never blur over full-bleed manga pages — it destroys readability and frames.
· Fade in/out with opacity, not slide.

---

6. Vibecoding Workflow for This Project

6.1 Docs = Agent Constraints

This file is not documentation for humans. It is prompt engineering for future agent interactions. Every rule must be actionable and testable. If an agent generates code that violates a rule, the rule was not specific enough.

6.2 Phased Execution

Do not ask the agent to "redesign the app." Execute in this order:

1. Theme layer — colors, typography, shapes, spacing. Pure XML/Material theme. Lands app-wide. Works on API 29.
2. Bottom nav — build the floating pill with tier logic. Test on one screen.
3. Top bar — collapsing large title with glass.
4. Sheets and dialogs — big radii, grabber, dimming.
5. Motion pass — springs everywhere, haptics.
6. Reader — overlay controls only. Do not touch page rendering.

Each phase is a separate agent session. Feed the agent only this file plus the relevant source files for that phase.

6.3 The Mapping Pass (Do This First)

Before any code generation, have the agent map Yokai's architecture:

· Count Controller classes vs Composable functions.
· Find the bottom nav host (likely Conductor-based, possibly migrating to Voyager).
· Identify which screens are XML and which are Compose.
· Locate the theme resources (colors.xml, themes.xml, Type.kt).

Yokai descends from TachiyomiJ2K: Conductor navigation, RxJava presenters, now mid-migration to Compose + Voyager. The agent must not assume a pure Compose architecture.

6.4 Build & Test Loop

· Build on a VPS or GitHub Actions. Gradle in Termux is impractical.
· Sideload debug APK after Phase 1. Judge on real hardware, not previews.
· Test on an API 29 device/emulator and an API 33+ device. The tier logic is where most bugs will appear.

---

7. Agent Guardrails

7.1 Never Do These

· Do not apply blur to RecyclerView items or manga cover images.
· Do not use RoundedCornerShape(50) for pills — use the correct radii.
· Do not animate with tween() or LinearInterpolator.
· Do not ship SF Pro or any Apple font.
· Do not stack glass on glass.
· Do not use RenderScript.

7.2 Always Do These

· Provide tier-aware fallbacks for every glass component.
· Test backdrop sampling on XML screens via AndroidView wrapping.
· Sample backdrop luminance and switch label color automatically.
· Add haptics to interactive glass controls.
· Cap glass surfaces at 3 per screen.

---

8. Reference Libraries (Exact Coordinates)

```kotlin
// Full Liquid Glass (API 33+)
implementation("io.github.kyant0:backdrop:2.0.1")

// Blur fallback (API 31+)
implementation("dev.chrisbanes.haze:haze:1.6.10")

// Spring animations (Views)
implementation("androidx.dynamicanimation:dynamicanimation:1.1.0-alpha03")

// Compose BOM (Yokai already has this)
implementation(platform("androidx.compose:compose-bom:2026.05.00"))
```


---

9. Fork Configuration

This is a fork of `null2264/yokai` working as `madsykle/yokai`.

- **App ID**: `eu.kanade.tachiyomi.madsykle` (installs alongside upstream)
- **Git remotes**: `upstream` → `github.com/null2264/yokai`, `origin` → `github.com/madsykle/yokai`
- **CI**: GitHub Actions at `.github/workflows/ci.yml` builds debug APKs for every PR/push
- **Build flavors**: `standard`, `dev` (dev is English-only, smaller)
- **Build types**: `debug` (suffix `.debugYokai`), `release` (suffix `.yokai`), `beta`, `nightly`

---

10. Execution Plan

## Phase 0: Architecture Mapping + Foundation ✅ DONE

**Status:** Complete

- [x] App ID changed to `eu.kanade.tachiyomi.madsykle` in `app/build.gradle.kts`
- [x] Tier libraries declared in `libs.versions.toml`: `backdrop:1.0.6`, `haze:1.6.10`, `dynamicanimation:1.1.0-alpha03`. `dynamicanimation` is an `app` dependency; `backdrop`/`haze` are dependencies of `presentation/theme` only (§12), so no other module is coupled to a glass library.
- [x] Inter font (400/500/600/700) added to `app/src/main/res/font/`
- [x] iOS 27 design tokens in `colors.xml` + `values-night/colors.xml`: glass base, tint, accent, label, border, specular, scrim colors
- [x] Shape tokens in `dimens`: corner radii (28/24/14/12dp pill/card), spacing (4dp unit)
- [x] Typography system in `Typography.kt`: Inter font, iOS Dynamic Type scale (34/28/22/17/17/16/15/13/12sp)
- [x] `Theme.kt`: glass tokens, `GlassTier` enum, `glassTier()` composable, `glassShapes` (squircular)
- [x] `GlassSurface.kt`: tier-aware composable + View extension for XML interop
- [x] CI workflow at `.github/workflows/ci.yml`: builds debug APKs + ktlint + lint + unit tests

**Architecture mapping findings:**

| Component | File | Tech | Glass Status |
|-----------|------|------|-------------|
| MainActivity (host) | `ui/main/MainActivity.kt` | Conductor + XML | Phase 1-2 target |
| Bottom Nav | `layout/main_activity.xml` | XML `BottomNavigationView` | Phase 1 |
| Top Bar (large title) | `ExpandedAppBarLayout` in XML | XML | Phase 2 |
| Top Bar (Compose) | `JayExpandedTopAppBar` | Compose | Phase 2 |
| Scaffold (Compose) | `Scaffold.kt` → `YokaiScaffold` | Compose | Phase 2 |
| Library screen | `LibraryController` / `LibraryComposeController` | XML / Compose | Phase 3 |
| Recents screen | `RecentsController` | XML | Phase 3 |
| Browse screen | `BrowseController` | XML | Phase 3 |
| Manga Details | `MangaDetailsController` | XML | Phase 4 |
| Sheets | `BottomSheetDialog` | XML + Material | Phase 4 |
| Reader overlay | `ReaderNavigationOverlayView` | XML | Phase 5 |
| Settings | `SettingsLegacyController` | XML | Phase 4 |

**Phase 0.5: Sprint 1 (Next 5 tasks)**

Priority order:

1. **Floating Pill Bottom Nav** (Phase 1 in execution)
   - Float `BottomNavigationView` above content (16dp h, 8dp bottom + insets)
   - Replace dock with glass pill using `GlassSurface` (tier-aware)
   - Make content scroll UNDER the nav (add bottom padding to router container)
   - XML `Widget.Tachiyomi.BottomNavigationView` → glass variant

2. **Collapsing Top Bar Glass** (Phase 2)
   - XML `ExpandedAppBarLayout` → add `GlassSurface` on collapse
   - Compose `JayExpandedTopAppBar` → add `GlassSurface` on collapse
   - Both use shared `GlassAppBarScrollBehavior` (NEW: unify the two systems)

3. **Sheet/Dialog Redesign** (Phase 4)
   - `BottomSheetDialogTheme` → 24dp top corners, grabder, opaque bg
   - Tier-aware glass on dialog scrim

4. **Theme: Inter font + glass colors** (Phase 1)
   - Wire `YokaiTypography` into XML via `fontFamily`
   - Add glass-aware color overlays

5. **Reader Overlay Controls** (Phase 5)
   - `ReaderNavigationOverlayView` → glass controls only, never on manga page
   - Fade in/out via opacity, not slide

---

## 11. Implementation Audit (2026-09-24)

Verification pass against HIG Materials/Navigation + Apple Books ref (`ref/Apple Books iOS 7.png`). Fixes applied in one pass:

**Correctness fixes (were violating §1.2/§5.3/§5.4 at runtime):**
- Fonts: all 8 `inter_*.ttf` were corrupted (GitHub HTML saved as TTF, app would crash on first inflate). Replaced with Inter 4.1 static TTFs (Regular/Medium/SemiBold/Bold × app + theme modules).
- `View.applyGlass` used `setRenderEffect(blur)` which blurs the view's OWN content (nav icons, sheet contents) — never the backdrop. Replaced with tier-aware translucent material tint (white/black, §4.1) as background drawable; §1.4 luminance adaptation comes from the base tint + dim scrim.
- Full-screen `window.decorView.applyGlass(...)` on sheets/dialogs/reader painted tint over the ENTIRE app. Sheets (§5.3) are opaque surfaces over the dim scrim; dialogs now use a rounded (28dp) opaque `MaterialShapeDrawable` window background with the §2.2 darkened rim.
- Reader overlay: glass removed entirely (full-screen over manga page = §1.1 violation); fades converted from 1s tween to springs (§4.5).
- Decorators (`applyGlassDecorators`) now idempotent `foreground` drawable (specular + 1px §2.2 rim); scrim tier draws exact §3 colors `#F2F2F7`/`#1C1C1E` @ 90%.
- `GlassMotion.addEndListener` lambda param fixed (4th param of `OnAnimationEndListener` is velocity, not canceled) and bad `androidx.core.view.performHapticFeedback` import removed (platform `View.performHapticFeedback` used).
- §2.2 transparency slider added to Settings → Appearance (6 stops, 30–95%, default 70 ≈ 0.72), read by all glass surfaces via `glassTintAlpha(context)`.

**Known follow-ups (both since resolved — see §12):**
- ~~Tier 1 (API 33+) renders the same translucent tint as Tier 2.~~ Backdrop now supplies real
  refraction on tier 1.
- ~~Haze (§3 Tier 2 for Compose) is a dependency but unused on Compose glass surfaces.~~ Haze now
  supplies the tier 2 blur, driven from `YokaiScaffold`.

---

## 12. Reference-Fidelity Pass (2026-09-25)

Second pass, targeting the three §11 follow-ups plus the `build_push` red. Verified green on
`edea58000c`: CI Build (incl. Android Lint) success with artifact `yokai-madsykle-debug`
(252.6 MB), and `build_push` success as well — it builds, runs unit tests and uploads the R8
APK/mapping, then *skips* sign/cleanup/publish because the fork has no signing secrets.

**Done:**
- **Concentric active-tab pill (§5.1).** The selected tab is now a lighter capsule around the
  icon, per `ref/Apple Books iOS 7.png`, instead of a tint over the whole item. Configured via
  `itemActiveIndicatorStyle` — the flat `itemActiveIndicatorWidth`/`…Color` attributes **do not
  exist** in Material 1.14, so the nested style is the only supported route;
  `NavigationBarActiveIndicator` reads `android:width`/`android:height`/`marginHorizontal`/
  `android:color`/`shapeAppearance`.
- **Separate circular glass button.** `bottom_nav_search` + `bottom_nav_search_button` in
  `main_activity.xml`, styled with the oval-clipping glass path (`applyGlass(…, circle = true)`,
  so the ripple stays round; later superseded by the §16 blur pane). Opens
  `GlobalSearchController`, which was otherwise reachable only through the Browse long-press.
- **Content scrolls under the pill (§5.1).** `controller_container` is no longer padded; the nav
  inset is pushed to each screen's scrollables via `MainActivity.insetScrollables`, which sets
  `clipToPadding = false` and only ever *increases* bottom padding (so screens that already reserve
  space for a download bar / FAB keep their own value). Applied on every controller change.
- **Nightly publish** repointed from upstream `null2264/yokai-nightly` to the fork's own
  `madsykle/yokai-nightly` (`build_push.yml` `Create Nightly`, plus the commit/release links).
  Signing is still *required* to publish, but the release steps are now gated on the secrets
  actually existing: they cannot be read from a step `if:` (the `secrets` context is unavailable
  there), so a `Check release secrets` step surfaces them as step outputs and
  `Sign APK` / cleanup / release / nightly skip while they are unset. Adding
  `SIGNING_KEY` / `ALIAS` / `KEY_STORE_PASSWORD` / `KEY_PASSWORD` (and `NIGHTLY_PAT`) re-enables
  the whole path with no further change.
- **Tier 1 / tier 2 material wired up** — see below.

**Tier 1 / Tier 2 backdrop — implemented, with one prerequisite that was not obvious:**

Both `Backdrop` (refraction, API 33+) and `Haze` (blur, API 31–32) need a *source* that records
the content the chrome sits over. In the Compose path that source did not exist:

- M3 `Scaffold` places the body content first and the top bar on top of it (verified in
  material3 1.5.0-alpha14 `ScaffoldLayout`), so a glass top bar *is* drawn above the content.
- But `Scaffold` passes the top bar's **current** height as the content's top inset, and
  `JayAppBarScrollBehavior.appBarScrollBehavior()` collapses by *reporting a smaller height*
  (`.layout { layout(placeable.width, placeable.height + scrollOffset) { placeable.placeWithLayer(0, scrollOffset) } }`),
  not by translating inside a fixed box.
- Net effect: the content's top edge always tracked the bar's bottom edge, so nothing was ever
  behind the glass and both backdrops would have sampled empty space.

Fix: the content's top inset is now **sticky at the expanded app-bar height** while the bar
collapses over it — what iOS large titles do (the scroll inset is the large-title height and
never shrinks). Applied in `YokaiScaffold`; this changes scrolling behaviour on every Compose
screen, which is the intended §5.1 behaviour but is the one part of this work that CI cannot
confirm visually.

**Do not bump `backdrop` to 2.x without bumping `compileSdk`.** 2.0.1 is a Compose
Multiplatform build that depends on Compose 1.12.0, and Compose ≥ 1.12 declares
`minCompileSdk 37`; this project compiles against android-36 and AGP 8.12.2 caps out at 36, so
`:app:checkStandardDebugAarMetadata` fails with 22 issues (all of the Compose 1.12.0 artifacts
plus `kyant0:backdrop-android:2.0.1` and `kyant0:shapes-android:1.2.1`). 1.0.6 is the right
pin: it has the full API §3 needs (`rememberLayerBackdrop`, `Modifier.layerBackdrop`,
`drawBackdrop`, `blur`, and the `lens` refraction effect) and asks for `minCompileSdk 36` with
Compose 1.10.3 — exactly this project's Compose BOM. `haze` is unconstrained by this
(`minCompileSdk 1`).

The XML chrome was already correct in this respect: the container is unpadded and the chrome is
a later sibling, so content genuinely passes underneath.

---

## 13. Transparency Preview, Tablet Parity and Tier-3 Compliance (2026-09-25)

Third pass: the §2.2 slider became a live preview, the tablet layout caught up with the phone
one, and tier 3 was brought in line with §3.

**Live transparency preview (§1.5, §2.2).** The six-stop list preference is replaced by a
`SeekBar` row (`GlassTransparencyPreference`) that writes `glass_transparency_alpha` on every
progress change. Nothing recreates the Activity any more, so the chrome re-tints *under the
finger*:

- The preference file is the single source of truth. The slider writes it, `MainActivity`
  observes it with a `SharedPreferences` listener and re-runs the View-side material, and
  Compose reads it through the new `glassTintAlphaState()`, which is now `GlassSurface`'s default
  `tintAlpha`.
- `glassTintAlpha(context)` (non-composable) is kept for `View.applyGlass`; both paths clamp to
  the same 30–95 bounds via `glassTransparencyPercent()`.
- The slider stays continuous during a drag and names the nearest §2.2 stop in its summary, so
  the spec's vocabulary survives without quantising the interaction.

**Tablet parity.** The `w720dp` layout had no search button and an opaque rail, and its style
was reachable only through `navigationRailStyle` in `themes.xml`:

- The rail now floats like the pill — transparent background plus
  `@dimen/bottom_nav_horizontal_margin` / `…bottom_margin` outside it — and gets the same
  tier-aware material and `28dp` radius, applied from `MainActivity.refreshGlassChrome()`
  (which is also what the transparency listener calls, so the rail previews too).
- `Widget.Tachiyomi.NavigationRail` gained `itemActiveIndicatorStyle` →
  `Widget.Tachiyomi.NavigationRail.ActiveIndicator` (64×56dp, same
  `ShapeAppearance.Tachiyomi.GlassActiveIndicator`), because a labelled rail item wraps the icon
  *and* its label, so the pill's 56×32dp capsule is the wrong shape there.
- The circular glass search button (`bottom_nav_search`) is added to the tablet layout, anchored
  to the rail's bottom. The rail carries the navigation bar inset as *padding* and the button
  sits outside it, so `MainActivity` completes the button's margin with
  `systemInsets.bottom` — only when `bottomNav == null`, since the portrait button is anchored to
  the pill, which already carries it.

**Tier 3 has no real blur, on purpose (§3, §7.1).** §3's tier 3 is the scrim fallback, not a
blur tier: refraction needs AGSL (API 33) and `RenderEffect` needs API 31, and RenderScript —
the only blur available below 31 — is banned outright by §7.1. A blur is also not what "Reduce
Transparency" means. What *was* non-compliant is that the scrim tier drew a bare flat fill:
`applyGlassDecorators` returned early for `GlassTier.Scrim`, so §3's "1px light top edge" and
the §2.1 darkened rim were missing. Every tier now gets both.

**Rim follows the surface, not its bounding box.** The darkened edge is a `GradientDrawable`
stroke, and it was drawn with no corner radius — a square outline over a rounded pill. It now
receives the surface's radius, and an oval for the round search button, from the nav-chrome
styling call sites. `applyGlassDecorators` no longer takes a tier (the treatment is
the same on all three) and takes `cornerRadiusDp` / `circle` instead; the app-side call sites
pass the same radius they pass to `applyGlass`.

**Testable geometry.** Two pieces of logic that used to be inline and unverifiable are now pure
functions with unit tests (`FloatingNavInsetsTest`, `GlassTierTest`): `glassTierFor(sdkInt)`
(§3's tier boundaries) and `FloatingNavInsets` (the §5.1 floating-nav inset arithmetic, including
"only ever increase a screen's bottom padding").

Still unverified visually: the sticky-inset scroll behaviour from §12, the slider's live repaint
on a real frame budget, and the tier-1/tier-2 effects. CI proves compilation and the unit tests
only.

---

## 14. Device Contrast Audit (2026-09-25)

First on-device verification pass (`currentappss/` screenshots, dark mode, 1080×2400). Pixel
analysis of the three main screens found the redesign was running but rendering as *nothing*,
and it traced to one wrong assumption carried through the whole material:

**Dark glass was tinted BLACK, which can only be invisible.** §4.1's `GlassTintDark: #000000 @
35%` over the `#1C1C1C` page lands on `#141414` — *darker than the page*. The floating pill, the
round search button and every glass surface rendered as a void; the tab capsule floated in empty
space. HIG dark materials do the opposite: they LIFT above their backdrop. Both material paths
now tint white in both modes, with dark mode at half the user's transparency value
(`GLASS_DARK_BASE_ALPHA = 0.50`, so the default 70% reads as a 35% white veil — a clearly visible
frosted surface). The §3 scrim is lifted the same way (`#2C2C2E`, not `#1C1C1E`).

**The dark rim was black-on-black.** §2.1's darkened edge `#33000000` cannot separate a dark
surface from a dark page. The rim is now mode-aware: `#59FFFFFF` (light) in dark mode,
`#33000000` in light mode — `DarkenedEdgeDark` in `GlassColors`, and the dialog rim follows.

**The specular ramp only covered 24dp** of a 76dp-tall pill, so the surface read as a flat wash.
The sheen now spans the full height (25% white at the top edge in dark mode, 12% in light).

**Sub-contrast tokens raised:** unselected nav labels `#99EBEBF5` → `#C9E1E4E8` (~79%), and the
dark active indicator `#24FFFFFF` (7% white, invisible) → `#42FFFFFF`.

Light mode is structurally unchanged: white frost on a light page carries through the rim,
indicator and accent icons, which is what iOS light glass does.

New open question for the next device pass: whether the lifted dark veil is *too* present at the
"Fully tinted" stop (95% × 0.50 ≈ 48% white).

## 15. Phase 1: Layout Overlap Pass (2026-09-26)

Three-phase UI fix, phase 1 only (layout bugs, flat colours, no blur yet). On-device screenshots
(`currentappss/`, 9 shots, 1080×2400, dark mode, Realme RMX2001 / API 30) confirmed the concrete
overlaps; two were fixed blind from code + screenshots.

**AllManga catalogue: the Popular/Latest/Filter bar floated ~76dp too high, over the cover grid.**
The screenshots proved the bottom nav is *not* on screen here: every measured nav bar in the set
sits at the screen bottom, and the catalogue has none. A `BrowseSourceController` is always pushed
onto the back stack, and `MainActivity` hides the pill for pushed controllers
(`nav.isVisible = !hideBottomNav`), so the `bottom_nav_total_height` clearance added in §14 was
pure extra offset — the bar's bottom margin was ~255px, putting its top edge in the middle of the
grid. `BrowseSourceController` now reserves only the 8dp + system-inset margin the bar genuinely
needs; `insetScrollables` already gives the grid the bottom padding that keeps the last row clear.

**Library filter sheet: the read-progress segmented control ("Not started"/"In progress") sat under
the pill's top edge.** The sheet reserved exactly the pill's *height* (`bottomBar.height`), but the
floating pill also carries an 8dp bottom margin (`bottom_nav_bottom_margin`), so its top edge lands
8dp *below* the reserved line. `LibraryController.updateFilterSheetY()` now reserves height +
margin.

**Reader: the page-seekbar pill overlaid the chapter-transition page text.** The transition page is
centred in the full page while the reader's bottom chrome (seekbar / chapter nav) floats over it, so
a tall transition card landed underneath. `PagerTransitionHolder` and `WebtoonTransitionHolder` now
reserve the chrome's height as bottom padding, so the transition lays out above it instead of under
it (`readerNav.root.height`).

**Top bar gap normalised.** Screens had drifted: some added an ad-hoc margin (Library's category row
+12dp, Recents +48dp), the `scrollViewWith` content padding had none. Everything now goes through
one pure helper, `FloatingNavInsets.topInsetFor(systemTop, appBarHeight, margin)`, with a single
`@dimen/content_top_margin` (12dp). The *bar height* still varies with the layout a screen needs
(large title + search pill vs toolbar-only); the *margin* no longer does.

Deliberately untouched in this phase: no blur, no BlurView dependency, no morphing indicator, no
translucent flat-black fills. Phases 2 (real backdrop blur on the top bar + bottom nav only) and 3
(morphing spring tab indicator) are next and are each gated on on-device confirmation.

## 16. Phase 2: Real Backdrop Blur on the Floating Chrome (2026-09-26)

The floating top card, the bottom nav pill, the round search button and the w720dp rail now sit on
a real backdrop blur (Dimezis BlurView) instead of a flat translucent fill. Only these four
*floating* surfaces get it: list items, cards, sheets and the reader page are content, not
material, and are deliberately left untouched.

**BlurView pinned at `version-2.0.6`.** 3.x moved to a `BlurTarget` wrapper (breaking API);
2.0.6 is the last line with the non-breaking `setupWith(ViewGroup)` that auto-picks
`RenderEffectBlur` on API 31+ and `RenderScriptBlur` below. Committed through the existing
Jitpack repository.

**The blur source is `controller_container`, not the decor view.** The chrome must never be
sampled into its own backdrop — the pill's icons and the top card's text are drawn on top of the
BlurView, never inside its snapshot — and a smaller snapshot is cheaper on the Snapdragon 720G.
Each frame is cleared with the window background (`setFrameClearDrawable`) because the container
is mostly transparent and a transparent snapshot blurs to a washed-out veil.

**Panes and shapes.** Four BlurViews, all transparent rounded panes (`applyGlassBackdropPane`:
transparent `GradientDrawable` + outline provider + `clipToOutline`) carrying only the §2.1 rim
and sheen on top (`applyGlassDecorators`): `card_blur` (24dp), `bottom_nav_blur` (28dp),
`bottom_nav_search_blur` (24dp, circle) and `side_nav_blur` (28dp, w720dp only). Each BlurView is
a **sibling drawn behind** its surface, not its parent — nesting would sample the surface's own
drawing. Radius is `@dimen/glass_blur_radius` (14dp); the downsample factor stays at the library
default (6) per the phase brief.

**Legibility veil, not a tint fill.** `setOverlayColor` paints a black veil over the blur,
mapped from the §2.2 transparency slider into a fixed 40–55% band
(`glassBlurTintColor`: more transparent ⇒ stronger veil). This is the only "tint" the material
has; the slider's live preview re-tints via `refreshGlassChrome()` instead of rebuilding
anything.

**The pill moves; its blur must follow.** Nav hide-on-scroll animates `bottom_nav.translationY`,
and a sibling BlurView does not track that. A `ViewTreeObserver.OnPreDrawListener`
(`GlassBlurChrome`) copies `translationY`/`alpha`/`isVisible` from the pill onto its blur every
frame, and `alpha`/`isVisible` for the search button (which never translates). Detached in
`onDestroy`.

**Flat fills removed so the blur is what you see.** `ExpandedAppBarLayout` no longer paints
`applyGlass` on `card_frame`; `setAppBarBG` no longer fills `card_view` (both branches — the
scroll-blend had no card colour left to animate); `card_view` is transparent in both layouts.
`FloatingGlassNavController` is deleted: its `applyGlass` fill was the "fake glass" the brief
bans. The *collapsed main toolbar's* own glass (not floating chrome) is untouched.

**Tier note.** On the target device (API 30) this blur is RenderScript — §7.1 bans RenderScript,
and the Phase 2 brief explicitly overrides that for the floating chrome. The perf lever on this
device is the downsample factor, never a lower radius.

Status: CI green (compile + unit tests + lint). Not yet device-verified; Phase 3 (morphing spring
tab indicator) is gated on that confirmation.
