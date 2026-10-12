# Migration State

Last updated: 2026-10-12

## Phase
Phase 4 — Cupertino rewrite.

## Current work
Recents tab. Sub-step B1c (restore regressions from B1b).
- B1a: position-free holder interface (555559f4fe) — DONE, device-verified.
- B1b: Compose LazyColumn + sticky headers + empty state (a514b9b3fa,
  3e1e8707d9) — DONE, device-verified. Swipe-to-mark-read + pull-to-refresh
  + manual refresh trigger temporarily regressed.
- B1c (in progress): restore swipe, pull-to-refresh, refresh trigger; fix
  RecentMangaHeaderItem.equals.
- B2 (next): type-1 row rewritten in Compose; Cupertino row styling.

## Regressions to close in B1c
1. Swipe-to-mark-read — View-side drag driver (custom FrameLayout with
   onInterceptTouchEvent), NOT Compose pointerInput.
2. Pull-to-refresh — Compose nestedScroll cage, threshold 150dp, indicator
   in the gap below segmented control.
3. Manual refresh trigger — check the agent's B1b commit body for the
   recommended path (menu item vs pull gesture).
4. RecentMangaHeaderItem.equals (:40-46) — broken; fix to data-class-style.

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
