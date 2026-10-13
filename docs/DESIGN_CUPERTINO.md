# Cupertino Design Brief — Yokai

**Status:** CONTRACT. Binding on all Phase 4 work.
**Ruling date:** 2026-10-07
**Supersedes for Phase 4:** nothing. `DESIGN.md` remains the Liquid Glass constraint system; this document is the Cupertino counterpart. Where they conflict, this document wins on Cupertino concerns and `DESIGN.md` wins on glass.

**Read this before touching any Phase 4 screen.**

Companion to `DESIGN.md` (Liquid Glass, chrome only) and `.freebuff/PROGRESS.md` (execution state).

---

## 0. Preconditions

This document assumes, and does not re-litigate:

- **Cupertino is primary. Liquid Glass is secondary/accent.** Glass is architecture, not ornament.
- **Compose HIG is unavailable.** Every published version (`zone.ien.hig`) forces androidx Compose 1.12.x → compileSdk 37 → AGP 9. Blocked. **Hand-written Cupertino on `backdrop 1.0.6`.**
- **`io.github.kyant0:android-liquid-glass` does not exist** (Maven Central 404; the group directory contains only `backdrop/`).
- Nav3 pinned at 1.1.7, Compose BOM `2026.05.01` (1.11.2), Hilt 2.58, AGP 8.12.2, compileSdk/targetSdk 36, **minSdk 26**.

---

## 1. Aesthetic direction

### 1.1 The position

**A shelf, and one lamp, at 2am.**

Not a reading nook. Not a cosy corner. The register is cold, dark and quiet. YOKAI is a night-reading app; the reference is a private library after hours — covers catching a single warm light against a dark room, everything else receding to near-nothing.

Three commitments:

**a) Chrome is architecture, not ornament.**
The nav bar, top bar and sheets are structural: they hold position, show depth, and get out of the way. `DESIGN.md` §1.1 already forbids glass on content, and §1.3 caps glass at 2–3 surfaces per screen. Those two rules *are* the aesthetic. A cover grid is never glass. A chapter row is never glass. Nothing is glass that a reader would want to touch.

**b) iOS chrome, Android input.**
Cupertino *visuals* on Android *input semantics*. A Material ripple is not a spring. A long-press is a long-press. Where iOS gesture vocabulary and Android conflict (predictive back, edge swipe), **Android wins** and the animation adapts.

**c) Density is a feature.**
A manga library holds hundreds of entries. Airy card grids are a luxury-app move and wrong here. **Compact grouped lists, tight leading, small radii on rows; generous space only at the screen's outer frame.** Cupertino at iPhone density is dense, and that is the correct target.

### 1.2 The signature — one place where boldness is spent

> **The spring-damped large-title collapse.**
> Every screen opens with a 34pt left-aligned title on an opaque bar. As you scroll, the title physically condenses into a 17pt inline title and the bar becomes translucent, with a 1px hairline — the *shelf edge* — appearing at the top only when content is passing beneath it.

This is the one memorable moment and it earns its place by encoding something true: **how deep you have scrolled, and whether the chrome is currently transparent.** It is the only place in the app where two elements physically negotiate. Everything else stays disciplined. The hairline appears *because content is under the bar*, not because a designer added a divider.

**Restraint rule (Chanel test):** before shipping, take one thing out. If the signature and everything else are competing, the signature wins.

---

## 2. Typography ramp

**Current state is the anti-pattern this section fixes.** `presentation/theme/.../Typography.kt:23-141` takes real iOS sizes and assigns them to Material3 slots (`displayLarge` = LargeTitle, `labelMedium` = Caption). Any screen reaching for `MaterialTheme.typography.*` reads a Material vocabulary with Cupertino numbers painted on.

**Fix:** role-named tokens are the primary API. The Material3 mapping survives **only** as a bridge for existing consumers (see Ruling 5).

### 2.1 The ramp

| Role | Size / line | Weight | Tracking | Where it appears |
|---|---|---|---|---|
| `largeTitle` | 34 / 41 | Regular | +0.37 | Collapsing screen title. The signature. |
| `title1` | 28 / 34 | Regular | +0.36 | Manga detail title, rarely |
| `title2` | 22 / 26 | Regular | +0.35 | Sheet headers |
| `title3` † | 20 / 25 | Regular | +0.34 | Group titles in sheets |
| `headline` | 17 / 24 | **Semibold** | −0.41 | Collapsed inline title, row emphasis |
| `body` | 17 / 24 | Regular | −0.41 | Manga titles, chapter names |
| `callout` | 16 / 22 | Regular | −0.32 | Dense row secondary |
| `subhead` | 15 / 21 | Regular | −0.24 | Settings row labels at density |
| `footnote` | 13 / 18 | Regular | −0.08 | Metadata, timestamps, unread counts |
| `caption1` | 12 / 16 | Regular | 0 | Cover badges |
| `caption2` | 11 / 14 | Regular | +0.06 | Timestamps, over-counts |
| `sectionHeader` | 13 / 16 | **Semibold** | +0.06 | Uppercase grouped-list headers |

† `title3` is **interpolated, not sourced** — it is not in `DESIGN.md` §4.2 or Apple's published ramp. It is our addition, the lowest-confidence token in the system, and the first to cut if it reads wrong on device.

All values above except `title3` are from `DESIGN.md` §4.2, which already matches this table in the existing `Typography.kt`.

### 2.2 Rules

- **Family: Inter only.** Already shipped — `inter_regular/medium/semibold/bold.ttf` in both `app` and `presentation/theme`. `DESIGN.md` §7.1 forbids SF Pro for legal reasons. **No second family.** This is a single-family system.
- **Positive tracking on large sizes, negative on small.** That inversion is the SF signature and Inter reproduces it. Getting it backwards reads as fake immediately.
- **Line height 1.2× titles / 1.4× body.** Preserve; already correct in the current file.
- **Section headers are a role, not a style.** `sectionHeader` gets its own token so it cannot drift per call site. Uppercase, semibold, `caption1` size, `labelSecondary` color.

---

## 3. Color tokens

`GlassColors` (`Theme.kt:27-59`) already carries the iOS system palette and is correct. It is **glass vocabulary**. What is missing is the layer underneath: what colour is a *surface*, a *row*, a *separator*, a *label* — independent of whether glass sits on top.

### 3.1 Naming rule

**Semantic names. No Material roles.** No `primary`, `onPrimary`, `surfaceVariant`, `surfaceTint`, `outlineVariant`. Those encode Material's elevation model, which we are discarding. Ours encode **what the thing is**.

### 3.2 Surfaces — a strict three-step ladder

| Token | Light | Dark | Use |
|---|---|---|---|
| `surface` | `#F2F2F7` | `#1C1C1E` | Screen background |
| `surfaceRaised` | `#FFFFFF` | `#2C2C2E` | Grouped list cards, rows, opaque sheets |
| `surfaceChrome` | `#F2F2F7` @92% | `#1C1C1E` @92% | Nav bar, top bar when opaque, segmented track |

**The grouped-list rule:** on `surface`, a list is **never** drawn edge to edge. Rows sit on `surfaceRaised` with a **16dp horizontal inset** and **10–12dp vertical radius**, grouped under one rounded container. That inset is the strongest Cupertino tell in a settings screen — stronger than colour, stronger than radius.

**The two insets STACK — they do not collapse.** This was ambiguous in the first draft of this section, which said both that they "stack visually" and that row content has "0 additional". Resolved, and recorded here so it is not re-litigated:

```
  screen edge
    |<- 16dp ->|<------- surfaceRaised, 12dp radius -------->|
                 |<- 16dp ->|  label             value  >  |
                 |          |------------------------------|  separator, inset 16dp
                 |          |  next row                     |
```

- **16dp** screen edge → card edge (`sectionInset`)
- **+16dp** card edge → row content (`rowContentInset`)
- ⇒ a row label sits **32dp** from the screen edge
- ⇒ the separator starts at **16dp**, aligned to row content, **never full-bleed**

A collapsed single-inset reading would put the label at 16dp and lose the tell entirely. Implemented as two named constants in `GroupedSectionMetrics` so the alternate reading stays a one-line change.

**Row radius** is `CornerRadii.medium` (12dp), the looser end of the 10–12dp range; iOS tightens as a list grows taller and `CornerRadii.small` (10dp) is the tighter end.

**Insets do not scale with configuration.** iOS uses 16pt on regular devices and 20pt on Plus/Max. Not implemented, deliberately: this repo has **zero `values-sw*` resource directories**, so a qualifier-based split has no infrastructure to hang on, and a code-based split would introduce a second layout mode with no precedent on any Cupertino surface. `Spacing.space20` already exists, so revisiting this when a tablet layout lands is a one-token change.

### 3.3 Labels — four levels, not two

| Token | Light | Dark | Use |
|---|---|---|---|
| `labelPrimary` | `#000000` | `#FFFFFF` | The thing you read in order to act |
| `labelSecondary` | `#3C3C43` @60% | `#EBEBF5` @60% | Supporting text *(exists — `Theme.kt:47-48`)* |
| `labelTertiary` | — | — | **NEW** — timestamps, counts, disabled. Rows currently jump primary→secondary with nothing between |
| `labelOnAccent` | — | — | **NEW** — text on a filled accent. No token exists, which is why every current accent button picks white by hand |

### 3.4 Separators

`separator` `#3C3C43` @29% / `#EBEBF5` @29%, plus `separatorOpaque` for the shelf-edge hairline. **Hairlines start inset 16dp** to align with row content; never full-bleed.

### 3.5 Accents

`accent` `#007AFF`/`#0A84FF` · `success` `#34C759`/`#30D158` · `destructive` `#FF3B30`/`#FF453A` · **`warning` — NEW, currently absent.** Download-queue and migration screens need it and today reach for orange by hex.

### 3.6 Both schemes are first-class

Dark is **not** an inversion — it is the primary condition. **This is a design decision, not a claim about our users**: no analytics or research in the tree supports it. It is justified on the product's nature (long reading sessions, low ambient light) and is revisitable on device.

---

## 4. Motion vocabulary

### 4.1 Tokens

| Token | Spec | Applies to |
|---|---|---|
| `spring.snappy` | stiffness 700, damping 0.85 | Press-in, chip |
| `spring.default` | stiffness 300, damping 0.75 | **House default.** Sheets, large-title collapse, list push |
| `spring.gentle` | stiffness 180, damping 0.90 | Dismiss, fade-out, layout settle |
| `spring.bouncy` | stiffness 400, damping 0.60 | Segmented-control indicator **only** |
| `crossfade` | framework defaults | Tab switch only |

### 4.2 Rules

1. **`spring.default` = 300/0.75 because it is what already ships.** `GlassRecipe.SWEEP_DURATION_MS = 650L` and the morphing tab indicator (`DESIGN.md` §18) are in the installed APK. Continuity with motion users have already learned is the reason. It is not a preference.
2. **Stiffness carries weight.** 300 is the default; depart deliberately.
3. **Nothing overshoots except the segmented control.** Bounce is a one-verb vocabulary; used broadly it reads as cheap.
4. **Dismiss is always gentler than present.** Present 300/0.75, dismiss 180/0.90. The asymmetry is what makes a dismissal feel acknowledged rather than reversed.
5. **The large-title collapse is spring-solved against scroll**, not time-based — driven by scroll offset with velocity carry-over. **It must never run on a timer while the finger is down.**
6. **Tab switch is a crossfade on framework defaults — no authored spring.** See Ruling 1. Resolve cross-fade/dissolve ambiguity: the tab indicator morph is the only authored motion, and it already exists.

---

## 5. Haptic vocabulary

Haptics are scattered calls today. This is a **table**, so each interaction has exactly one answer.

| Interaction | Feedback | Constant | Why |
|---|---|---|---|
| Tab switch | Light | `CONTEXT_CLICK` | Confirms arrival, not a choice you can feel wrong about |
| Segment change | Selection | `CLOCK_TICK` | Continuous, tiny, fires while dragging |
| Switch toggle | — | **none** | The animation *is* the feedback. iOS does not buzz switches. |
| Long-press to select | Medium | `LONG_PRESS` | Arms selection mode |
| Drag reorder pickup | Medium | `LONG_PRESS` | One gesture, one buzz |
| Sheet present | Selection | `CLOCK_TICK` | Something is arriving |
| Sheet dismiss | Light | `CONTEXT_CLICK` | Acknowledged, gone |
| Destructive confirm | Warning | `CONFIRM` | Reserved — only on delete/clear/wipe |
| Search committed | Selection | `CLOCK_TICK` | Keyboard dismissal |

**`destructive confirm` is the only heavy haptic in the app. Reserve it.** If it fires routinely it stops meaning anything, and a delete you cannot feel is a delete people get wrong.

**Haptics and motion are independent.** This table cites no motion rule and is not derived from
one. §4.2 rule 4 ("dismiss is always gentler than present") governs *springs* — the dismiss
animation is 180/0.90 against the present's 300/0.75 — and says nothing about which haptic
constant fires. An earlier draft of this table justified sheet-dismiss's `CONTEXT_CLICK` as
"matching §4.2 rule 4", which was simply wrong: it cross-referenced a motion rule that states the
opposite relationship. The values stand on their own — present is a "something is arriving" tick,
dismiss is an "acknowledged, gone" click. **Do not reintroduce a cross-reference between this
table and §4.**

**Accessibility:** all haptics respect the system touch-feedback setting. Never haptic-gate a destructive action without an on-screen consequence too.

---

## 6. Component inventory

Ordered by what Phase 4 needs first. **Each is its own commit, own CI run, independently revertable.**

| # | Component | Status | Notes |
|---|---|---|---|
| — | Superellipse | ✅ **BUILT** | `Superellipse.kt`, `CORNER_EXPONENT = 4.0`, 64 segments. **Must not be rebuilt.** |
| 1 | **Grouped section list** | NEW | `sectionHeader` + inset `surfaceRaised` container + 16dp inset + separators inset to content. 13 legacy settings controllers need it. **Highest leverage in the system.** |
| 2 | **Large-title collapsing bar** | NEW | The signature. Springs against scroll; becomes translucent + hairline below threshold. |
| 3 | **Segmented control** | NEW | Recents view-type switch (`RecentsController.kt:528`) needs it today. Glass track, morphing `spring.bouncy` indicator, selection haptics while dragging. |
| 4 | **Search bar** | NEW | The loudest anti-pattern in the app. Flush, no elevation, no rounded-rectangle pill, 17pt, cancellable, translucent only when scrolled. **Explicitly not a Material search view.** |
| 5 | **Switch** | NEW | iOS proportions — 51×31dp track, 27dp thumb — not Material's. Squircle track, `spring.snappy`. No haptic (§5). |
| 6 | Sheet refinements | PARTIAL | 24dp top radius + grabber exist. Needs opaque background (rule 1.2, no glass on glass), 35% scrim, spring present / gentle dismiss. |
| 7 | Alert refinements | PARTIAL | `GlassDialog.kt` exists. Cupertino action-sheet ordering, destructive action last and red. |

**Picker: CUT.** See Ruling 6.

---

## Rulings

### Ruling 1 — NO TWEEN, with carve-out
Authored animations are **spring-only**. No `tween()`, no linear.
**Carve-out:** framework-provided defaults are grandfathered — Nav3 transitions, `AnimatedVisibility`, `AnimatedContent`, `Crossfade`. Phase 3 already committed `fadeIn() togetherWith fadeOut()` in all three islands, CI-green. Without the carve-out, Phase 4 fights itself.
**Action:** update `DESIGN.md` §4.5.

### Ruling 2 — minSdk is 26, not 29
Actual `MIN_SDK = 26` (`buildSrc/src/main/kotlin/AndroidConfig.kt:5`). `DESIGN.md` claims "Android 10+ (API 29+)" and defines Tier 3 as API 29–30.
**Tier 3 must cover API 26–30** or ~4.7% of devices get no fallback path at all.
**Action:** update `DESIGN.md`.

### Ruling 3 — backdrop is 1.0.6, not 2.0.1
`DESIGN.md` §8 recommends `2.0.1`. It is blocked: forces Compose 1.12 → compileSdk 37 → AGP 9.
**Use `io.github.kyant0:backdrop:1.0.6`** (Compose 1.10.3, minCompileSdk 36 — exactly our BOM). Revisit in the AGP 9 migration phase.
**Action:** update `DESIGN.md` §8.

### Ruling 4 — `Constants.kt` `Size` is replaced, not deleted
New scale **4 / 8 / 12 / 16 / 20 / 24 / 32**. Current `Size` **omits 20dp entirely**, which `DESIGN.md` §4.4 requires, and carries a typo (`smedium`) that has clearly been load-bearing.
`Size` is **deprecated, not deleted**. All 11 consuming files keep compiling.
**Ledger:** "Size deprecation ledger: N files" — tracked.

### Ruling 5 — `Typography.kt` becomes a bridge, with a kill condition
Role-named tokens primary; the Material3 mapping is **deprecated**.
**Kill condition: zero `MaterialTheme.typography` consumers before Phase 4 exit.**
**Ledger:** "Typography bridge ledger: N files" — tracked.

### Ruling 6 — Picker is cut
Largest single component, and nothing in the three tab roots requires one. Defer until a screen demands it. Smaller system, smaller surface area, same visual result.

### Additional rulings

**A — Design system scope:** type ramp, spacing scale, corner radii, motion tokens, haptic wrappers, color tokens. **No components.** Components are separate commits. Lands **inert** — no screen consumes it in the same commit.

**B — Component build order:** grouped section list → large-title collapsing bar → segmented control → search bar → switch → sheet refinements → alert refinements.

**C — Perf items deferred.** LRU cache owner (NEEDS VERIFICATION — not Coil, owner unidentified), ReaderViewModel Leak 1 (transient, ~15s window, extension `getPages`), and the `runBlocking` residual at `ReaderViewModel.kt:162` (holds an IO worker inside a `NonCancellable` window; already off the main thread, so not jank). Filed, not addressed.

**D — Expected benefit, not a claim.** The originating complaint was lag, popup-menu lag and intermittent breakage. Leaks were a partial cause. The UI rewrite **should** reduce perceived lag. This is an expectation to be tested on device, **not a promised result.**

---

## Appendix — ledgers

Both must reach zero. Re-count at each Phase 4 checkpoint.

- **Typography bridge ledger: 22 files / 35 call sites** as of `ed31d565d4`. Slots in use: `bodySmall` 8, `bodyMedium` 8, `titleLarge` 7, `titleSmall` 3, `titleMedium` 3, `labelMedium` 2, `bodyLarge` 2, `headlineMedium` 1, `headlineLarge` 1.
  Heaviest consumers: `reader/ChapterTransition.kt` (6), `presentation/core/JayAppBar.kt` (3), `settings/screen/data/StorageInfo.kt` (2), `onboarding/InfoScreen.kt` (2), `manga/components/CommonMangaItem.kt` (2), `extension/repo/ExtensionRepoScreen.kt` (2), `domain/DialogHostState.kt` (2), `component/EmptyScreen.kt` (2).

- **Size deprecation ledger: 11 files / 40 call sites** as of `ed31d565d4`. Members in use: `small` 16, `medium` 13, `tiny` 6, `huge` 2, `extraTiny` 2, `extraExtraTiny` 1. Unused: `none`, `smedium`, `large`, `extraLarge`, `extraHuge`, `navBarSize`.
  Heaviest consumers: `component/ThemeItem.kt` (10), `onboarding/InfoScreen.kt` (8), `onboarding/steps/ThemeStep.kt` (4), `onboarding/steps/StorageStep.kt` (3), `onboarding/steps/PermissionStep.kt` (3).

Verified by grep on `app/src`, `presentation`, `core` at `ed31d565d4`. Excludes View/XML theming entirely — the Material3 XML theme is untouched by Phase 4.