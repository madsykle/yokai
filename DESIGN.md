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
- [x] Tier libraries added: `backdrop:2.0.1`, `haze:1.6.10`, `dynamicanimation:1.1.0-alpha03` in `libs.versions.toml` + `app/build.gradle.kts`
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
