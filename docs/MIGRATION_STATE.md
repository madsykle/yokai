# Migration State

Last updated: 2026-10-13

## Phase
Phase 4 — Cupertino rewrite.

## Current work
Recents tab. Sub-step B1c (restore regressions from B1b) — DONE, pending
device verify.
- B1a: position-free holder interface (555559f4fe) — DONE, device-verified.
- B1b: Compose LazyColumn + sticky headers + empty state (a514b9b3fa,
  3e1e8707d9) — DONE, device-verified. Swipe-to-mark-read + pull-to-refresh
  + manual refresh trigger temporarily regressed.
- B1c: regressions restored (45d2a652d8, fea76da64d) — DONE, CI green on all
  three checks. Device verify pending.
- B2 (next): type-1 row rewritten in Compose; Cupertino row styling.

## Regressions closed in B1c
1. Swipe-to-mark-read — restored as a View-side drag driver
   (SwipeToMarkReadLayout, custom FrameLayout with onInterceptTouchEvent),
   NOT Compose pointerInput; wraps the inflated holder row.
2. Pull-to-refresh — Compose nestedScroll cage, threshold 150dp, indicator
   in the gap below the segmented control.
3. Manual refresh trigger — restored via the pull gesture; startLibraryUpdate
   reproduces the pre-B1b listener body exactly (snack + Cancel + startNow).
4. RecentMangaHeaderItem.equals — fixed to data-class-style
   (recentsType == other.recentsType).

## Regressions still open
None. The type-1 row Compose rewrite is B2 scope, not a regression.

## Ledger counts (canonical rule: call-site expressions)
- Injekt: 755 call sites / 197 files (target 0 by end of Phase 4)
- MaterialTheme.typography bridge: 22 files / 35 sites
- Constants.Size alias: 11 files / 40 sites

## Open blockers
- Phase 4 design conflict resolved: Cupertino primary, Liquid Glass accent.
- Compose HIG blocked on AGP 8 → Compose 1.12. Deferred.
- android-liquid-glass does not exist on Maven Central. Strike from plan.
- Phase 4 device-verify gate: applies to any user-visible change.

## Module gate / placement decisions
- Design tokens + components live in :presentation:theme (not a new module).
- Nav3 multiple-backstack = N independent rememberNavBackStack instances,
  one per tab, only active composed (Phase 4 shell work).
