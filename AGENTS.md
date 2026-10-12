# Yokai — Agent Instructions

## Project
Fork of Mihon/Tachiyomi (manga reader). Mid-migration to 100% Jetpack
Compose + Navigation 3 + Hilt, with a Cupertino (iOS-style) design system
on top of backdrop 1.0.6. See docs/MIGRATION_STATE.md for live state and
docs/PROGRESS.md for full history.

## Stack (locked — do NOT bump without an explicit ruling)
- Kotlin 2.4.10, minSdk 26, target/compileSdk 36, JVM 17
- Gradle 8.14.4, AGP 8.12.2
- Compose BOM 2026.05.01 (resolves to 1.11.2)
- Hilt 2.58
- Navigation 3 1.1.7
- backdrop 1.0.6
- KSP 2.3.10, javapoet 1.13.0 pin (buildSrc), kotlin-metadata-jvm 2.4.0 KSP override
- SQLDelight 2.2.1, OkHttp 5.4.0, Coil 3.4.0

## Blocked versions (do NOT add — all need AGP 9 / compileSdk 37)
Hilt 2.59+, Nav3 1.2.0+, backdrop 2.x, Compose HIG, Compose BOM 1.12.x,
ComposeElasticOverscroll 1.0.0. Bundled into a future "AGP 9 migration"
phase. Do not attempt any of these mid-Phase-4.

## Build / verify
- CI is the oracle. Local Gradle is not usable (Termux). Do not attempt a
  local build.
- Branch: refactor/phase1-async-source-api
- Every commit pushes and waits for CI. Three checks must pass: Build
  Debug APK, Build app (unit tests), Lint & Type Check.
- Warning baseline: 1052. Delta must be 0.
- PR #1 is open, unmerged. Do not merge.

## Hard rules (non-negotiable)
1. Never invent library versions, APIs, or coordinates. Unknown →
   "NEEDS VERIFICATION" and stop.
2. Before adding a dependency: grep the repo, check all catalogs, cite the
   source URL.
3. Before deleting a file: list it, wait for confirmation.
4. Read files before assuming contents. Cite file:line.
5. Do not claim a build passed unless you ran it and read the output.
6. On CI failure: STOP, paste raw error, wait. No improvised workarounds.
7. One commit, one CI run, one checkpoint. Do not chain sessions.
8. Report-first only for architectural decisions (module placement, spec
   disagreements, dependency additions). Mechanical fixes go straight
   through — fix forward on API typos without asking.
9. No force-push. No `git add -A` (it once destroyed commit separability).
10. CI green is insufficient for user-visible changes. Device verification
   by the user is the gate. State the device checklist at end of session.

## Current phase
Phase 4 — Cupertino rewrite. Recents tab in progress (B1c).

## Dead ends (do NOT re-litigate — filed for a reason)
- Library discriminator for the 415ms cold-start warning was invalid
  (Library is empty; test proved nothing).
- Bundle/ClassCastException: no reproduction, pre-existing, deferred.
- ReaderViewModel Leak 1: transient ~15s window from an extension
  withTimeout, deferred.
- LRU 25% hit rate: not Coil, owner unknown, deferred.
- ReaderViewModel:162 runBlocking: off main thread, not jank.

## Local oracles (high-value, keep using)
- ~/android-sdk/platforms/android-36/android.jar → `javap -classpath` for
  framework API checks.
- ~/.gradle/caches/.../*.aar → unzip classes.jar, `javap -p -c` to verify
  library behavior (not just signatures).
