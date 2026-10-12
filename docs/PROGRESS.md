# Yokai Modernization Progress
Last updated: 2026-10-04T03:39:07Z
Current phase: Phase 2 — DI swap (Koin → Hilt) at binder/toolchain stage
Current module: :app (builder stage)

## Legend
- [ ] not started
- [~] in progress
- [x] done and verified (build + lint pass)
- [!] blocked (see notes)
- [-] skipped (reason in notes)

## Phases
### Phase 0 — Reconnaissance
- [x] Scan repo tree, list all modules
- [x] Read every build.gradle.kts and version catalog
- [x] Read buildSrc convention plugins
- [x] Produce inventory of RxJava usage (file:line)
- [x] Produce inventory of XML layouts + Fragments + Conductor usage
- [x] Produce inventory of Koin modules
- [x] Produce inventory of Voyager usage
- [x] Produce inventory of Material3 / Material Components usage
- [x] VERIFICATION CHECKPOINT — present inventory to user, wait for approval

### Phase 1 — Async unification (RxJava → Flow)
- [x] :data — no RxJava present (grep verified); no-op
- [x] :domain — no RxJava present (grep verified); no-op
- [x] :source:api — DONE, CI GREEN. Option C (suspend-primary + deprecated Rx shim). Commit
      `8334c3f820` on branch `refactor/phase1-async-source-api`, PR #1. CI Build run 37130861457:
      `Build Debug APK` success (assembleStandardDebug + testStandardDebugUnitTest) and
      `Lint & Type Check` success. Changed: `Source.kt`, `CatalogueSource.kt`, `online/HttpSource.kt`,
      `util/RxExtension.kt` (common+android).
- [x] :core:main — no edit needed; `awaitSingle` and `runAsObservable` are now the android actuals
      backing :source:api's expect declarations (both retained).
- [x] :core:archive — no RxJava present; no-op
- [x] :app — DONE, CI GREEN. Commit `b0f6ac877a`, CI run 37132345760 (`Build Debug APK` success,
      `Lint & Type Check` success). Deleted `util/lang/RetryWithDelay.kt`, `util/lang/RxExtensions.kt`,
      `util/system/RxUtil.kt` (0 callers repo-wide, user-confirmed); removed the never-assigned
      `rx.Subscription requestSubscription` from `LoginDialogPreference`; dropped
      `rxjava`/`rxandroid`/`rxrelay` from `app/build.gradle.kts`. `:app` has zero `import rx.`.
      `rxjava` still reaches `:app` transitively via `:source:api`'s `api(libs.rxjava)`.
- [x] VERIFICATION CHECKPOINT — Phase 1 CI green: run 37130861457 (`:source:api`) and run
      37132345760 (`:app`), both `Build Debug APK` + `Lint & Type Check` success on PR #1.

### Phase 2 — DI swap (Koin → Hilt)
- [x] Steps 1-2 landed (`4834192b5a`): catalog coordinates + Hilt/KSP plugins on `:app` only.
      **Step 5 build RED — see Notes.** Steps 3-7 not started.
- [x] Steps 3-4 (toolchain + bridge) landed (`d0a48771e6`, `5e70ef2e57`): `javapoet:1.13.0` added
      to `buildSrc`, `@HiltAndroidApp` on `App`, `HiltInjektBridgeModule` + deprecated `@EntryPoint`
      created. **CI GREEN 2026-10-04** (commit `210f5aded3`, see Decisions log) — binder/toolchain
      stage COMPLETE and VERIFIED.
- [x] VERIFICATION CHECKPOINT — Phase 2 toolchain/binder: CI run 37173833695/37173833701
      (`Build Debug APK` + `Lint & Type Check` + `Build app` all success) on `210f5aded3`.
      Steps 5-7 (@Inject sweep) NOT started — deferred to a dedicated phase.

### Phase 3 — Navigation (Voyager + Conductor → Navigation 3)
- [x] RECONNAISSANCE COMPLETE 2026-10-04 — 8 artifacts produced (below).
- [x] Gate: Nav3 **1.1.7** PASSES on current toolchain (compileSdk 36 / AGP 8.12.2). Nav3 1.2.0+ is
      BLOCKED (minCompileSdk 37 + AGP 9.1.0). Nav3 stays pinned at 1.1.7.
- [x] Step 1: About island pilot — **VERIFIED (build)** on `338d822001`. NOT device-verified.
      Commits: `b1f2c4d5b2` (migration), `338d822001` (NavDisplay import fix).
      Voyager removed from the island: the 3 screens became plain `@Composable`s taking callbacks
      (no LocalNavigator / LocalBackPress); `AboutController`'s Voyager `Navigator` +
      `CrossfadeTransition` host became a Nav3 `NavDisplay` over one `rememberNavBackStack`.
      `AboutController` itself STAYS a Conductor shell (Ruling 1) — `MainActivity` and
      `SettingsMainController` still push it. New file `AboutRoutes.kt` = 3 `@Serializable`
      `NavKey` routes. NO DI changes. NO shell / tab-model changes. NO WebView changes.
      Step 1 was RED first: run `37176997508`, 2 errors, both `Unresolved reference 'NavDisplay'`
      (imported from `androidx.navigation3.runtime`; the class lives in `androidx.navigation3.ui`).
      CI GREEN on `338d822001`: `CI Build` 37177467806 (Build Debug APK + Lint & Type Check both
      success) and `Build PR` 37177467804 (Build app success).
- [x] Compose BOM skew RESOLVED on `df1d11ac21` — Compose BOM 2026.02.00 -> 2026.05.01,
      aligning runtime/ui/animation/foundation at 1.11.2. CI green (37179472492 + 37179472552).
      See Decisions log "Compose BOM skew".
- [x] Step 2 + Step 3 merged into one checkpoint — **Island A (Advanced + Data settings) MIGRATED,
      CI GREEN on `5a4d257cc4`.** 4 screens: `SettingsAdvancedScreen`, `SettingsDataScreen`,
      `StoryBookScreen`, plus the `ComposableSettings` base. NOT device-verified.
      `SettingsComposeController`'s `Navigator` + `CrossfadeTransition` host became a Nav3
      `NavDisplay` over one `rememberNavBackStack(getComposableSettings().route)`, mirroring
      `AboutController`. New file `SettingsRoutes.kt` = 3 `@Serializable` object routes
      (`SettingsAdvancedRoute` / `SettingsDataRoute` / `StorybookRoute`) — all payload-free, so the
      default `clazzContentKey` is safe. `ComposableSettings` **dropped its `: Screen()` supertype**
      entirely (no longer a Voyager type; the old `Content()` override is replaced by a concrete
      `Content(onOpenStorybook, onNavigateUp)`). `StoryBookScreen` is now a plain
      `@Composable fun StoryBookScreen(onNavigateUp)`. **BEHAVIOR CHANGE (device-verify, see
      Ruling 3 below).** CI: Build Debug APK 112283447340 success, Build app 112283447347 success,
      Lint & Type Check 112283447179 success.
- [x] Step 4: ExtensionRepoScreen — **MIGRATED, CI GREEN on `115223f487`. NOT device-verified.**
      The LAST live Voyager island. `ExtensionRepoController`'s `Navigator` + `CrossfadeTransition`
      host became a Nav3 `NavDisplay` over one `rememberNavBackStack(ExtensionRepoRoute(repoUrl))`,
      same shape as `AboutController` / `SettingsComposeController`. New file
      `ExtensionRepoRoutes.kt` = `@Serializable data class ExtensionRepoRoute(val repoUrl: String?)`,
      with an EXPLICIT `clazzContentKey = { it.repoUrl ?: "" }` per the standing rule (the default
      would bake the deep-linked URL into the saveable-state registry key). `ExtensionRepoScreen` is
      now `@Composable fun ExtensionRepoScreen(title, repoUrl, onBackPress)`; its two
      `DialogHostState` helpers became top-level private extensions; `ExtensionRepoScreenModel` is a
      plain class over an injected `CoroutineScope` with Compose snapshot state, no longer a Voyager
      `StateScreenModel`. The 6 `injectLazy` interactors, the `Channel`/`event` flow, and the
      `State` / `ExtensionRepoEvent` / `RepoDialog` sealed hierarchies are unchanged.
      **Three CI runs — the churn is on file deliberately, see "three red runs" below.**
      CI: Build Debug APK 112319854484 success, Build app 112319854533 success,
      Lint & Type Check 112319854920 success.
      **WART (pre-existing, deliberately NOT fixed):** the title is the hardcoded English literal
      `"Extension Repos"` in `ExtensionRepoController`, not a string resource. Flagged in Island B
      recon, left alone by the migration. Fix during the Phase 4 Compose rewrite, with `highlightKey`.
- [x] Step 7: Voyager dependency removal — **DONE, CI GREEN on `4672e46fb4`. PHASE 3 CLOSED.**
      Two commits, two CI runs, as ruled:
      **(a) `b1c85312f2`** `WebViewScreenContent.kt` only — Voyager `mutableStateStackOf` ->
      `mutableStateListOf`, 6 lines, dependency still present. Green: 112333452074 / 112333451887 /
      112333452532.
      **(b) `4672e46fb4`** the removal itself. Green: 112356917583 / 112356914245 / 112356917171.
      Deleted: `yokai/util/Navigation.kt` (10 lines, **dead — 0 importers of `yokai.util.Screen`
      once Island A and Island B dropped the base class**); `gradle/libs.versions.toml:18,127,128,
      129,158`; `app/build.gradle.kts:254`. Reworded `:131` Nav3 comment to
      "Phase 3 — Voyager replacement, Phase 3 exit".
      **`Locals.kt` — the `@Deprecated` is on `LocalRouter`, NOT on a Voyager marker** (Ruling 2
      was corrected pre-flight). `LocalRouter` is LIVE: provided at `BaseComposeController.kt:41`,
      read at `SettingsAdvancedScreen.kt:125` (DebugController) and `:207` (ClearDatabaseController)
      — both Island A code. Only the annotation was fixed: `message` now says "Navigation 3"
      instead of the stale "Voyager", and the `replaceWith` was dropped because it pointed at
      `cafe.adriel.voyager.navigator.LocalNavigator`, a class that no longer exists. The
      deprecation marker itself is KEPT — Conductor removal is still Phase 4.
      `StateCoroutinePresenter.kt:8` — KDoc `[cafe.adriel.voyager.core.model.StateScreenModel]`
      de-linked to plain prose ``Voyager's `StateScreenModel` `` (the class is live, 2 files use
      it; only the dangling KDoc link was removed).
      **Grep gates all pass:** `cafe.adriel.voyager` -> **0** repo-wide (all file types);
      `voyager` in any `.kts`/`.gradle`/`.toml` -> **0**; `libs.bundles.voyager` / `libs.voyager*`
      -> **0**; `NavigatorBackHandler` -> **0**; `LocalNavigator` -> **0**; `CrossfadeTransition`
      -> 3, all migration comments; `rememberScreenModel` -> 2, both migration KDoc lines in
      `ExtensionRepoScreenModel.kt:29,32`. Intentional historical prose, preserved on purpose.
- [x] Step 5: WebView — **RECLASSIFIED 2026-10-04: CLEANUP, not a migration.** The Voyager
      `WebViewScreen` was dead code. Deleted on `ad62f742f4` (CI green: 37188482435 + 37188482442).
      **`WebViewActivity` is OUT OF SCOPE for Phase 3 entirely** — it uses no navigation library.
      See "WebView pre-flight" below.
- [~] Step 6: TAB SHELL / setRoot -> Nav3 — **PRE-FLIGHT COMPLETE 2026-10-04. NOT STARTED.**
      Two premises from earlier recon were WRONG and are corrected below:
      **(1) the tabs contain ZERO Voyager** — all three tab roots are View-based Conductor Controllers
      (`BaseCoroutineController` / `BaseLegacyController`); **(2) there is no per-tab backstack today
      at all** — `Router.setRoot` replaces the ENTIRE backstack with one controller, so tabs are not
      stateful and back never crosses tabs. The tab shell is therefore a *simpler* migration than the
      Voyager islands, but requires embedding View-based Controllers inside Compose NavEntries.
      Full artifact summary below; rulings needed.
- [x] VERIFICATION CHECKPOINT — **PHASE 3 CLOSED 2026-10-06.** Phase 3 exit condition met:
      Voyager fully removed from the repo (dead WebView deleted `ad62f742f4`; 2 islands migrated to
      Nav3 `5a4d257cc4` + `115223f487`; dependency removed `b1c85312f2` + `4672e46fb4`).
      Conductor still present and Nav3 coexists with it, as designed.
      **Three removal gates, restated:**
      | library | gate | status |
      |---|---|---|
      | Voyager | end of Phase 3 | **DONE `4672e46fb4`** |
      | Conductor | end of Phase 4 (all Controllers rewritten to Compose) | pending — Phase 4 |
      | Injekt | end of Phase 5 | pending — 197 files / 755 call sites remaining |
      **OUTSTANDING: device verification has NOT been run.** Build-verified only. See the device
      checklist in Phase 4 entry notes — About back hand-off + crossfade timing + tap latency;
      Island A Storybook up/back symmetry (a deliberate behaviour change) + scroll-on-rotation;
      Island B deep-link one-shot (fires once, survives rotation, does not re-fire);
      **WebView popups/close-tab (the `mutableStateListOf` migration)**.
      Phase 4 must not begin until that reports.
- [ ] VERIFICATION CHECKPOINT

**WebView stack migration (`b1c85312f2`) — Voyager `mutableStateStackOf` -> Compose
  `mutableStateListOf`. BEHAVIOR-PRESERVING, but it is a LIVE WebView path — device-verify.**
  Voyager's `mutableStateStackOf` returns `SnapshotStateStack<Item> : Stack<Item>` (verified from
  voyager-core 2.2.21 bytecode), NOT a `MutableList`. Five call sites rewritten:
  `:101` `lastItemOrNull!!` -> `last()`; `:178` `push(x)` -> `add(x)`; `:228` `pop()` ->
  `removeAt(lastIndex)` (`removeLast()` deliberately avoided — the deprecated stdlib
  `List.removeLast()` can shadow `MutableList.removeLast()`); `:372` `.items.find {}` -> `.find {}`;
  `:225/:233/:297` `.size` unchanged. The **seed window was preserved** — the ruling's literal
  `mutableStateListOf<WebViewWindow>()` would have dropped the initial
  `WebViewWindow(WebContent.Url(url, headers), …)` and the WebView would never load; the vararg
  overload `mutableStateListOf(<seed>)` is what was actually used. `:225`'s
  `if (windowStack.size == 1) onNavigateUp()` remains the known-dead branch recorded in the WebView
  pre-flight — still dead, unchanged.

  **PATTERN filed from this session — two reconnaissance lessons:**
  1. **Substring-adjacent reconnaissance misses type-level dependency.** Shallow recon called
     `mutableStateStackOf` "Voyager's wrapper used as a plain list, not a Navigator" — *correct but
     incomplete*. It is not a Navigator, and it is not a list either: it is a Voyager **stack type**
     with `push`/`pop`/`lastItemOrNull`/`canPop`/`popUntil`/`replace*`. Seven of the eleven expected
     Voyager files had already been cleared by Island A + Island B, so a recon keyed on the old
     file list would have shipped a broken removal. **Re-run the grep against the CURRENT tree
     before the final removal commit, never against a pre-migration inventory.**
  2. **`@Deprecated` annotation targets are not always the API you think they are.** `Locals.kt:18`
     read as a dead Voyager `LocalNavigator` marker; it was actually a deprecation on the app's own
     live `LocalRouter`, with 3 live call sites. **Read the actual declaration below the annotation
     before ruling on it.** The same session, the same PR, two blocked-by-stop rulings — both from
     reading the file instead of the plan.

**Island B — the one-shot `repoUrl` mechanism: VERIFIED CORRECT, unrelated to the three red runs.**
  Implemented as `var repoUrlConsumed by rememberSaveable { mutableStateOf(false) }` in
  `ExtensionRepoScreen.kt`, replacing the old `LaunchedEffect(repoUrl)` that self-nulled a
  `private var repoUrl` on the Voyager Screen. Requirements satisfied by
  `SaveableStateHolderNavEntryDecorator.kt:57` (`SaveableStateProvider(entry.contentKey)`) and `:55`
  (state discarded on pop), with the decorator applied BY DEFAULT at `NavDisplay.kt:260-261`:
  (1) cold deep link fires once; (2) rotation does NOT re-fire — `BaseComposeController` uses
  `DisposeOnViewTreeLifecycleDestroyed`, which destroys and rebuilds the composition, exactly the
  case a plain `remember` gets wrong; (3) a new Controller instance is a new deep link and fires
  again, matching the old behavior. Genuine improvement: after process death the route is restored
  from the serialized back stack, whereas the Voyager screen lost `repoUrl` (a constructor arg on a
  non-parcelable Screen) entirely. **STILL NEEDS DEVICE VERIFICATION.**

**Island B — three red runs, root causes, on file (do not prune this history):**

  1. `de26d74405` RED (112292281726 / 112292281694 / 112292281474) — 7 errors, 2 causes:
     **RC1** the nested `sealed interface State` shadows `androidx.compose.runtime.State` inside the
     class body, so `mutableStateOf(State.Loading)` resolved the wrong receiver and cascaded into a
     type mismatch plus a missing `.value`. **RC2** `import kotlinx.coroutines.launch` was dropped in
     the rewrite, killing `scope.launch { }`.
  2. `e95e0d424e` RED (112297271653 / 112297271208 / 112297272040) — RC2 fixed, RC1 only HALF
     fixed: **RC3** the alias was misapplied — `val state: SnapshotState<...> get() = mutableState.value`
     returns one level too shallow (the domain value, not a wrapper), breaking the caller too.
  3. `115223f487` GREEN — Option A (Compose-native) shipped: `var state: ExtensionRepoScreenModel.State
     by mutableStateOf(State.Loading) private set` in the model, `val state = screenModel.state` at
     the caller, no Flow, no `collectAsState`.

  **PATTERN NOTE for Phase 4 (this will recur on every `StateScreenModel` → plain-class rewrite):**
  nested type names shadowing `androidx.compose.runtime` types. `State` inside a class that declares
  its own `State` NEVER resolves to `androidx.compose.runtime.State` — always write the type
  argument out: `mutableStateOf<Owner.State>(State.Loading)`. Related trap found the same session:
  `androidx.compose.runtime.State` has its OWN `collectAsState()` overload, so a model exposing
  `State<T>` lets callers keep `by ... .collectAsState()` unchanged; but exposing the plain value is
  cleaner when the caller does no writes — **check the caller's read/write shape BEFORE choosing, and
  do not guess** (attempt 2 shipped a wrong fix proposed from memory; attempt 3 only landed after
  reading the caller). Also: when pruning imports after a rewrite, check which `getValue`/`setValue`
  are still needed by surviving `by` delegates — eyeballing removed a live one before commit.

**KNOWN BUILD NOISE — do not chase.** Build output contains
`Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is
2.4.0, expected version is 2.2.0` against kotlin-stdlib 2.4.10. **CORRECTED BASELINE (measured
2026-10-06; the earlier guess of a flat 25 was WRONG):** the RAW COUNT SCALES WITH HOW FAR THE BUILD
GETS — 25 occurrences when compilation aborts at the first failing task, **75 on a full green build**.
The stable invariant is **13 unique `.kotlin_module` paths**: `kotlin-stdlib{,-jdk7,-jdk8}` plus
`Yokai.core_main_debug`, `Yokai_data_debug`, `Yokai_domain_debug`, `Yokai_i18n_debug`,
`Yokai.source_api_debug` (each of the latter five appearing twice — once from the Gradle transforms
cache, once from `build/tmp/kotlin-classes`). Not caused by any recent change. **If the set of unique
paths grows beyond these 13, THAT is signal — a growing raw count is not.**

**PHASE 3 / PHASE 4 RESCOPE RULING — 2026-10-04 (supersedes the earlier "migrate Voyager islands,
then the shell" framing, which was malformed):**

- **Phase 3 = Voyager removal only.** 2 secondary screens (`ExtensionRepoController` +
  `ExtensionRepoScreen`), then the dependency. NO shell work. NO tab work. NO View rewrites.
- **Phase 4 = rewrite tab roots + tab shell + remaining View screens to Compose, migrate to Nav3,
  remove Conductor.** These are the same phase now; splitting them was only possible if the shell
  could be hosted while its tab roots stayed on View — and it can't.
- **Ruling 1 — shell design (Phase 4 target, NOT implemented):** (ii) N `NavDisplay`s, one per tab,
  only the active composed. (i) single `NavDisplay` with tabs as NavKeys was rejected — it introduces
  cross-tab back leakage that does not exist today (regression, not fix). (iii) N always composed
  rejected — memory cost the app does not need on a 3-tab bottom nav.
- **Ruling 2 — migration order:** islands first, shell last. Phase 3 checkpoints are
  (1) Island A, (2) Island B, (3) Voyager dependency removal — one commit + one CI run each, never
  combined, never proceeding past a red.
- **Ruling 3 — View-in-NavEntry blocker is a Phase 4 problem, not Phase 3. Resolution recorded:
  "rewrite tab roots to Compose first".** Do NOT plan to host Conductor Controllers inside Nav3
  entries via `AndroidView`/`ComposeView` — the Conductor Router<->Activity lifecycle binding means
  the wrapper fights the framework. Delete the Conductor lifetime; do not tunnel through it.
- **Ruling 4 — device gates:** Phase 3 = build-verified + light device pass at the end (same gate as
  About). Phase 4 shell = FULL device gate; build-verification would miss R1-R6 of the tab-shell
  risk register (three interacting layers + tab-state semantics that do not exist today).

**Island A (Advanced settings) — behavior change, INTENTIONALLY NOT SILENT, device-verify item:**

  Previously the up button and device back behaved differently inside this island. The up button
  went through `LocalBackPress` -> `router.handleBack()`, popping the entire Conductor Controller
  *including* the nested Storybook screen; device back went through Voyager's
  `NavigatorBackHandler`, which popped only Storybook. Both now behave like the About pilot: pop one
  Nav3 entry, falling through to `router.handleBack()` at depth 1. This FIXES a pre-existing
  up/back asymmetry. Implementation: `SettingsScaffold` gained an optional `onNavigateUp` param that
  defaults to `LocalBackPress.currentOrThrow`, so un-migrated callers are unaffected; Island A's
  entries pass the island's Nav3 `onBack`.

  **Device-verify (build cannot catch this):** Settings -> Advanced -> Danger Zone -> Storybook, then
  (a) up button pops only Storybook, (b) device back pops only Storybook, (c) at depth 1 both hand
  off to Conductor and return to the settings list, (d) list scroll position survives rotation on
  the Storybook screen.

**Phase 4 note — wart, do not fix now:** `ComposableSettings.highlightKey`
(`ComposableSettings.kt` companion) is a plain `var`, not a state holder, yet it drives the
background-blip highlight that `SettingsCommonWidget.kt` reads and resets. Revisit when
`ComposableSettings` is rewritten for Cupertino. Out of scope for Phase 3.

**Island B (ExtensionRepo) — one-shot mechanism decided, NOT yet implemented:**

  `ExtensionRepoScreen`'s `LaunchedEffect(repoUrl)` self-nulls a `private var` so a deep-link repo
  fires once per Controller instance. Under Nav3 the route is immutable, so the replacement is
  `rememberSaveable { mutableStateOf(false) }`. Verified against Nav3 1.1.7 sources:
  `SaveableStateHolderNavEntryDecorator.kt:57` wraps each entry in
  `SaveableStateProvider(entry.contentKey)`, `:55` discards state on pop, and `NavDisplay.kt:260-261`
  applies that decorator BY DEFAULT (no explicit decorator needed). So:
  (1) cold deep link fires once; (2) rotation does NOT re-fire (`BaseComposeController` uses
  `DisposeOnViewTreeLifecycleDestroyed`, which destroys and rebuilds the composition on config
  change — exactly the case a plain `remember` gets wrong); (3) a new Controller instance is a new
  deep link and fires again, matching today's behavior. One genuine improvement: after process death
  the route is restored from the serialized back stack, whereas today `repoUrl` (a constructor arg
  on a non-parcelable Screen) was silently lost.

**Tab-shell / setRoot pre-flight (2026-10-04) — 8 artifacts, read-only, no edits:**

- **`setRoot` has exactly ONE call site.** `MainActivity.kt:1407-1409`:
  `router.setRoot(controller.withFadeInTransaction().tag(id.toString()))`. Triggered from
  `nav.setOnItemSelectedListener` (`MainActivity.kt:616`) via `:635-641`, guarded by
  `currentRoot?.tag()?.toIntOrNull() != id`. Verified from the 4.0.0-preview-4 bytecode:
  `Router.setRoot` does `Collections.singletonList(transaction)` -> `setBackstack(...)`, i.e. it
  **replaces the whole backstack** and destroys the outgoing tab's Controller instance.
- **Bottom nav is a View, not Compose.** `res/menu/bottom_navigation.xml` = 3 items
  (`nav_library` / `nav_recents` / `nav_browse`); host is `MainActivity.kt:1310-1311`
  (`NavigationBarView`, `binding.bottomNav ?: binding.sideNav!!`), mounted into
  `binding.controllerContainer` by `Conductor.attachRouter` at `MainActivity.kt:602`.
- **Tab roots — ALL View-based, ZERO Voyager inside them:**
  | tab | root Controller | Voyager islands inside |
  |---|---|---|
  | library | `LibraryComposeController` / `LibraryController` (Compose+View variants, chosen by `basePreferences.composeLibrary()`, `MainActivity.kt:637`) | none |
  | recents | `RecentsController` (`RecentsController.kt:108`) | none |
  | browse | `BrowseController` (`BrowseController.kt:85`) | none |
  The only live Voyager islands repo-wide are **`SettingsComposeController.kt:19`** (abstract;
  subclassed by `SettingsAdvancedController`, `SettingsDataController`) and
  **`ExtensionRepoController.kt:12`**. Both are pushed onto the **Conductor Router** (`MainActivity:1227`
  deep link, `BrowseController:373`, `SettingsBrowseController:65`, `SettingsMainController:92,110`) —
  i.e. *above* a tab root, never *inside* one. So a tab switch destroys them.
- **Back-stack shape today:** island screen -> Voyager pops it (`NavigatorBackHandler`, enabled while
  `navigator.canPop`, falls back to `parent?.pop() ?: pop()`) -> Conductor `router.handleBack()`
  (`MainActivity:1299-1308`) -> root Controller's `handleBack()` consumes UI (Recents:523 dismisses
  downloads sheet / view-type jump; Library:1900 collapses categories or filter sheet; Browse:514
  collapses extensions bottom sheet) -> else `goToStartingTab()`. **Back never crosses tabs** because
  each tab is a fresh single-controller backstack. `Router.canStillGoBack()` (`ControllerExtensions.kt:859`)
  re-arms the Activity callback (`MainActivity.kt:862-867`). Re-tapping the current tab is NOT back —
  it is `getControllerWithTag(...).toggleSheet()` (`MainActivity:642-647`).
- **BLOCKER CANDIDATE (Artifact 6):** hosting a View-based Conductor Controller as a Nav3 entry means
  embedding its view in Compose (`AndroidView`/`ComposeView`). Conductor binds each Controller's
  lifecycle to `Router`, which in turn is bound to the Activity — so a Controller placed inside a
  `NavEntry` is not independently lifecycle-managed. Needs an explicit ruling.
- **RULINGS NEEDED:** (a) shell design (i)/(ii)/(iii); (b) migration order (A)/(B)/(C);
  (c) whether View-based Controllers may be embedded in Nav3 entries or must be Compose-rewritten first;
  (d) device gate.

**WebView pre-flight (2026-10-04) — 7 artifacts, read-only, no edits:**

- **RESOLVED 2026-10-04 (user ruling): cleanup, not a migration.** Deleted `WebViewScreen.kt`,
  `WebViewScreenModel.kt` and `AssistContentScreen` on `ad62f742f4` (108 deletions, CI green
  37188482435 + 37188482442). Pre-deletion verification: repo-wide grep across all source sets and
  file types showed only intra-cluster references; `AndroidManifest.xml`, all `*.kts`/`*.gradle`/
  `*.toml`/`*.xml`/proguard had zero hits. `yokai/util/Navigation.kt` keeps `Screen` (3 live users:
  `ComposableSettings.kt`, `StoryBookScreen.kt`, `ExtensionRepoScreen.kt`) and loses only the interface.
  **`WebViewActivity` is explicitly OUT OF SCOPE for Phase 3** — it uses no navigation library, so it
  is required by neither Voyager removal nor Conductor removal nor the Phase 4 UI rewrite. If it is
  ever brought into the Nav3 shell, that is a Phase 5 / post-migration task with its own reconnaissance.
- **HEADLINE: the Voyager `WebViewScreen` is DEAD CODE.** `ui/webview/WebViewScreen.kt:12` has **zero
  constructors repo-wide** — verified by grep across all source sets, all file types, and the manifest.
  It was ADDED by `5e84586ff5` (2024-11-26, "Replace WebView with its Compose counterpart") and that same
  commit removed 0 lines constructing it; it was never wired up. `WebViewScreenModel.kt:20` is referenced
  only by that dead screen. `AssistContentScreen` / `onProvideAssistUrl` (`yokai/util/Navigation.kt:12-13`)
  are **never consumed anywhere**. Phase 3 "Step 5: WebViewScreen" as originally written is a deletion,
  not a migration.
- **The live WebView is `WebViewActivity`** (manifest `AndroidManifest.xml:198`), launched from 5 sites
  (MangaDetailsController x2, ReaderActivity, ReaderErrorView, BrowseSourceController). It calls the
  shared `yokai/presentation/webview/WebViewScreenContent.kt` via `setComposeContent` (`:52-64`) with
  `onNavigateUp = { finish() }`. **Zero Conductor involvement** — no Router, no Controller anywhere in
  `WebViewActivity` / `BaseWebViewActivity`.
- **Custom backstack location:** `WebViewScreenContent.kt:92-98`, a `remember { mutableStateStackOf(...) }`
  inside the composable. Owned by the composable. NOT on the Conductor Router, NOT on a ScreenModel.
  It is the ONLY `mutableStateStackOf` in the repo. Entries are `WebViewWindow` (`:64-70`), a plain class
  holding `WebViewState`, `popupMessage: Message?`, `webView: WebView?`. Plain `remember` => survives
  recomposition + config change; **does NOT survive process death**.
- **Today's arbitration is only 2 deep, and WebView history is NEVER consulted on back.**
  `BackHandler(windowStack.size > 1, popState)` at `:233` is enabled only when a popup window exists.
  Size 1 (normal) => no Compose handler => `OnBackPressedDispatcher` => `ComponentActivity` default
  => `finish()`, regardless of how deep in-page history is. `navigator.canGoBack/navigateBack` drive
  only the toolbar back/forward buttons (`:263-276`) and the "open in app" visibility (`:297`).
  Latent dead branch: the `if (windowStack.size == 1) onNavigateUp()` arm inside `popState` (`:220-222`)
  is unreachable — `BackHandler` only enables when size > 1, and the close-tab action (`:310`) is only
  added when size > 1.
- **Nav3 1.1.7 multi-backstack / nested-back API: VERIFIED from primary sources (gate PASSED).**
  No dedicated tab or multi-stack API exists in `navigation3-runtime`/`-ui` 1.1.7 (full class dump;
  zero hits for "tab" outside LICENSE/"stable"). Multiple back stacks = N independent
  `rememberNavBackStack(...)` instances. Nested arbitration needs NO special API: `NavDisplay` registers
  `NavigationBackHandler(isBackEnabled = scene.previousEntries.isNotEmpty())`
  (navigation3-ui `NavDisplay.kt:556-566`), and on Android `ComponentActivity` implements
  `NavigationEventDispatcherOwner` (activity 1.12.0 `ComponentActivity.kt:119`, view-tree wiring at
  `:431`). `BackHandlerDispatcherCompat.addHandler` routes EVERY handler to that ONE dispatcher when
  present (activity-compose 1.12.0 `BackHandlerDispatcherCompat.kt:50-58`), so a Compose `BackHandler`
  inside a NavEntry and NavDisplay's own handler share one LIFO stack
  (`NavigationEventProcessor.kt:270`: "Add to the front of the appropriate queue to achieve LIFO
  ordering"). An entry's handler registers after NavDisplay's, so it wins; not consuming falls through
  to NavDisplay; at root NavDisplay is disabled and back propagates outward.
  Also: `rememberNavBackStack(vararg)` DOES survive process death (reflection-backed
  `NavBackStackSerializer`/`NavKeySerializer`), and `rememberSaveableStateHolderNavEntryDecorator()`
  gives each entry a `SavedStateRegistryOwner` + `rememberSaveable` scope.
- **ARBITRATION — DEFERRED, DO NOT IMPLEMENT.** Nothing to arbitrate this session.
  **FUTURE (post-Phase 4): if `WebViewActivity` is migrated into the Nav3 shell, adopt arbitration
  (a) WebView-history-first** — back should walk in-page history before exiting, matching
  Chrome/Safari/iOS norms. The current "back always exits" behaviour diverges from those norms and is
  treated as a pre-existing bug, **not a behaviour to preserve.**
- **DEVICE GATE — none for this deletion.** Dead-code removal is build-verified only; if the code were
  not actually dead, CI fails on an unresolved reference. The device gate stays OPEN for (i) the
  Phase 4 pre-flight (About island back hand-off, crossfade timing, tap responsiveness) and (ii) any
  future `WebViewActivity` migration (three interacting back layers).
**Phase 3 recon summary (2026-10-04):**
- Voyager: 15 files (all :app), 0 in :presentation. 3 ScreenModels. No TabNavigator anywhere.
  Self-contained `Navigator(` islands hosted by Conductor controllers (AboutController,
  SettingsComposeController, ExtensionRepoController).
- Conductor: 28 files (all :app). Single Router per Activity (MainActivity + SearchActivity).
  Bottom-nav = `setRoot` tab swap (Library/Browse/Recents). ~24 legacy View/preference controllers
  + Compose controllers. No nested Conductor routers.
- No `nav_*.xml` graphs, no `registerDeepLink` — all deep links are manifest intent-filters → Activity
  → router.pushController. Relevant deep link: `tachiyomi://add-repo?url=…` → ExtensionRepoController.
- Predictive back: already hand-rolled (BackEventCompat / handleOnBackStarted) over the Conductor backstack;
  `MainActivity` OnBackPressedCallback + BackHandlerControllerInterface are rewrite sites for Nav3.
- ~~Highest risk: WebViewScreen nested `mutableStateStackOf` window stack (two backstacks).~~
  **SUPERSEDED 2026-10-04:** `WebViewScreen` was dead code and has been deleted (`ad62f742f4`);
  `WebViewActivity` is out of Phase 3 scope. **The tab shell / `setRoot` is now the highest risk.**
- Voyager/Conductor coexist through migration; both deleted only when Injekt ledger hits 0 (Phase 5).
  **CORRECTED 2026-10-04 by user ruling — three separate gates, not one:** Voyager removed at the
  END OF PHASE 3; Conductor removed at the END OF PHASE 4; Injekt removed in PHASE 5.

- [ ] VERIFICATION CHECKPOINT — Phase 4
- [ ] per-screen list to be filled after Phase 0
- [ ] VERIFICATION CHECKPOINT

### Phase 5 — Cleanup
- [ ] Remove dead deps
- [ ] kotlinter + Android Lint clean
- [ ] VERIFICATION CHECKPOINT

## Notes / Blockers
<append only, never delete entries>

- 2026-10-04 **Crossfade timing (About island pilot) — record, do not fix.** The pilot used
  `fadeIn()` / `fadeOut()` defaults in place of Voyager's `CrossfadeTransition` default spec.
  Voyager's spec was `inline`, so the default lived in Kotlin `@Metadata` and was not extractable
  from the bytecode — no verified value to copy. Device-verify after the Phase 4 UI rewrite; may
  need tuning against the Compose HIG's spring specs then. Not to be revisited before Phase 4.
- 2026-10-04 **PHASE 4 PRE-FLIGHT (device verification gate):** before any Cupertino rewrite,
  device-verify on hardware:
  (a) About island back hand-off (Nav3 root -> Conductor `router.handleBack`),
  (b) crossfade timing vs original Voyager behavior,
  (c) tap responsiveness on the migrated island.
  The Nav3 pilot (`338d822001`) and the Compose BOM bump (`df1d11ac21`) are **build-verified only**.
  No hardware verification has been attempted. This is a gate at Phase 4 entry, not before.

- 2026-10-04 Nav3 pilot API note: `NavDisplay`'s `entryProvider` returns `NavEntry<T>`, so it must be
  built with the `entryProvider { entry<K> { … } }` DSL from `androidx.navigation3.runtime` —
  `NavDisplay` itself is in `androidx.navigation3.ui`. `defaultContentKey(key)` is
  `Pair("$key", "$key::class")`, which for a route carrying an HTML blob bakes the whole blob into
  the saveable-state registry key; pass `clazzContentKey` explicitly for such routes.

- KMP modules still on Koin — migrate in Phase 5 or later, after evaluating whether they need DI at all
  or can use constructor injection manually.
- **BLOCKER for Phase 4:** `DESIGN.md` says Liquid Glass, `RESTYLE_PROGRESS.md` says Cupertino. Need user
  ruling on which is the source of truth before any Phase 4 code is written.
- **Injekt call sites remaining: 740** (target 0 by end of Phase 4). Baseline measured 2026-10-03:
  196 files using `uy.kohesive.injekt`, 192 of them in `:app`.
- The `:presentation:widget` module exists on disk (Glance widget, 15 files) but is commented out of
  `settings.gradle.kts` (line 46). Status unknown — dead or pending re-enable?
- No Gradle KSP plugin, Dagger, Hilt, or androidx.navigation reference exists anywhere in the repo
  (verified by grep). Phase 2/3 will introduce a new annotation processor toolchain (KSP).
- 2026-10-03 BUILD ORACLE: local Gradle is not usable on this Termux/proot setup
  (`RESTYLE_PROGRESS.md`); verification of any change is via GitHub Actions `CI Build`
  (`.github/workflows/ci.yml`).
- 2026-10-04 **Injekt ledger reconciled (rule defined):** repo-wide (excluding `.gradle`/`build`)
  = **197 files, 755 call sites** (312 `Injekt.get` + 441 `injectLazy`); `:app`-only = 193 files,
  747 sites. The old "740" figure predates the rule definition; use the above rule + numbers as
  the baseline going forward.
- 2026-10-04 **AGP 9 migration is a prerequisite for Hilt 2.59+.** Track as its own phase (before
  Phase 3 or after Phase 5). Requires: Gradle 9.1+, built-in Kotlin, buildSrc DSL migration,
  removal of kotlin-android plugin, compileSdk implications for KMP modules.
- 2026-10-04 **Phase 3 Nav3 version pin: 1.1.7.** Nav3 1.2.0+ requires compileSdk 37 + AGP 9.1.0
  (repo is 36 + 8.12.2) — do not upgrade Nav3 past 1.1.7 without an AGP 9 migration. Nav3 1.1.7 also
  pulls lifecycle-runtime 2.10.0 (repo has 2.9.4; Gradle resolves to 2.10.0, minCompileSdk 34 = safe).
  Verified from aar-metadata.properties. Coordinates (do NOT add until approved):
  `androidx.navigation3:navigation3-runtime:1.1.7`, `androidx.navigation3:navigation3-ui:1.1.7`.
- 2026-10-04 **BLOCKER for Phase 3 — NEEDS USER RULING:** boundary decision. Which screens migrate to
  Nav3 backstack vs stay as Conductor-host? Specifically: (a) the ~24 legacy View/preference
  Controllers (own RecyclerView/View state, no Nav3 equivalent) — separate stream? (b) the WebView
  nested `mutableStateStackOf` window stack — needs a two-backstack design. Also: bottom-nav tab
  model (`setRoot`) has no Nav3 mapping yet. Recon did NOT resolve these; they need a human call.

## Decisions log
<each decision with rationale + date>

- 2026-10-04 **Compose BOM skew introduced by Nav3 — OPEN, needs a ruling.**
  Adding `androidx.navigation3:navigation3-ui:1.1.7` (`:app` only) transitively bumped two things:
    - `androidx.activity:activity-compose` / `androidx.activity:activity` **1.11.0 -> 1.12.0**.
      Forced by `navigation3-ui:1.1.7`'s POM, which declares `activity-compose:1.12.0`;
      `activity-compose:1.12.0` then hard-ranges `androidx.activity:activity = [1.12.0]`, so the
      bump propagates. `activity-compose:1.12.0` aar-metadata = minCompileSdk 36 / minAGP 8.9.1. OK.
    - `androidx.compose.runtime:runtime-android` and `runtime-saveable-android` **1.10.3 -> 1.11.2**.
      Forced by `navigation3-ui:1.1.7`'s POM, which declares both at 1.11.2. The BOM only sets a
      *preferred* version, so the higher transitive wins.
  NOT bumped: `compose.ui`, `compose.animation`, `compose.foundation` stay at the BOM's **1.10.3**
  (Nav3 asks 1.10.0, which is lower, so the BOM wins). => **runtime 1.11.2 alongside ui 1.10.3.**
  Nothing is *resolution-broken*: `ui-android:1.10.3` declares `runtime-android` soft (`1.10.3`),
  and `runtime-android:1.11.2` internally hard-ranges only its own `runtime-annotation-android =
  [1.11.2]`, so the runtime family self-aligns at 1.11.2. CI is green.
  But it is **off Google's own invariant**: every Compose BOM checked pins runtime == ui == animation
  == foundation (2026.02.00=1.10.3, 2026.03.00=1.10.5, 2026.04.01=1.11.0, 2026.05.00=1.11.1,
  2026.05.01=1.11.2, 2026.06.01=1.11.4, 2026.09.00=1.12.1). Google never ships a mixed BOM.
  **Clean fix available at zero compatibility cost:** BOM `2026.05.01` pins *all* Compose artifacts
  at exactly **1.11.2** — the version Nav3 asks for. `ui-android:1.11.2` aar-metadata =
  minCompileSdk **35** / minAGP 8.6.0, so it fits compileSdk 36 + AGP 8.12.2. Compose **1.12.x**
  is the same compileSdk-37 / AGP-9.1.0 wall as Nav3 1.2.0, so 1.11.2 is our ceiling either way.
  `material3` is pinned explicitly to `1.5.0-alpha14` (not BOM-managed); its POM only soft-requires
  ui 1.8.2 / runtime 1.9.0 / foundation 1.8.1 and its aar-metadata is minCompileSdk 35, so it
  survives the bump.
  **RESOLVED 2026-10-04, user-ruled.** Commit `df1d11ac21` — Compose BOM **2026.02.00 -> 2026.05.01**
  (`gradle/compose.versions.toml`, one line, no manual artifact overrides). Now in force:
    - `compose.runtime:runtime-android` + `runtime-saveable-android` = **1.11.2**
    - `compose.ui`, `compose.animation`, `compose.foundation` = **1.11.2**
    - `material3` = **1.5.0-alpha14** (explicit repo pin, unchanged)
    - `activity` / `activity-compose` = **1.12.0** (forced by Nav3, not a Compose artifact)
  Repo-wide audit before the bump: the ONLY explicit Compose version pin anywhere is material3
  1.5.0-alpha14. Third-party Compose consumers checked, all with **zero hard version ranges**:
  `material3-android:1.5.0-alpha14` (soft ui 1.8.2 / runtime 1.9.0 / foundation 1.8.1, minCompileSdk 35),
  `backdrop:1.0.6` (soft 1.10.3), `compose-webview:0.33.6` (imports compose-bom 2024.01.00),
  `accompanist-themeadapter-material3:0.36.0` (soft material3 1.0.1),
  `material-motion-compose-core:2.0.1` (no Compose deps). Only `:app` applies the BOM.
  CI GREEN on `df1d11ac21`: `CI Build` 37179472492 (Build Debug APK + Lint & Type Check both
  success) and `Build PR` 37179472552 (Build app success).

  **CEILING — three upgrades now blocked by the same wall.** Compose **1.12.x**, Nav3 **1.2.0+**
  and Hilt **2.59+** all require compileSdk 37 + AGP 9.1.0. We sit at 36 / 8.12.2. These are ONE
  problem, not three: bundle them into a single future **"AGP 9 migration"** phase. Until then the
  ceilings are Compose 1.11.2 / Nav3 1.1.7 / Hilt 2.58. Source: `aar-metadata.properties`
  `minCompileSdk`/`minAndroidGradlePluginVersion` in each artifact (ui-android 1.12.0/1.12.1 =
  37 / 9.1.0; navigation3 1.2.0 = 37 / 9.1.0; `HiltPluginEnvironment` throws below AGP 9.0.0).

- 2026-10-04 **PHASE 3 RULING (user, approved with boundaries).**
  - **Ruling 1 — legacy Controller boundary:** the ~24 legacy View/preference Controllers stay on
    Conductor through Phase 3 and are NOT migrated there. They are rewritten to Compose in Phase 4
    and land on Nav3 in that same pass (avoid 2-3 passes per screen). Phase 3 scope = Voyager islands
    only (15 files, 3 ScreenModels, 0 tabs) + any enclosing Conductor shell needed to host Nav3
    during the transitional period. **Phase 3 exit = Voyager fully removed from the repo**
    (Conductor still present, Nav3 coexists with it). **Conductor removal = Phase 4 exit.**
  - **Ruling 2 — WebView two-backstack:** handled at the screen, not the navigator. Nav3 entry provides
    a Compose `BackHandler` that does `webView.goBack()` when `canGoBack()`, else returns false so
    Nav3 pops. Confirmed 2026-10-04 by reading the code: the `mutableStateStackOf` window stack is
    ALREADY composable-scoped (`WebViewScreenContent.kt:92-98`, `remember { mutableStateStackOf(...) }`),
    NOT on the Conductor Router — no relocation needed. **SUPERSEDED 2026-10-04:** the Voyager
    `WebViewScreen` was dead code (zero constructors) and is deleted; the live host is `WebViewActivity`,
    an Activity with no navigation library, now OUT OF SCOPE for Phase 3. The Nav3 mechanism described
    above remains valid and is verified: `ComponentActivity` implements `NavigationEventDispatcherOwner`
    and all handlers share one LIFO dispatcher, so an entry-level `BackHandler` outranks `NavDisplay`
    and falls through when it declines. Deferred ruling: if `WebViewActivity` ever migrates, adopt
    (a) WebView-history-first.
  - **Ruling 3 — tab/`setRoot` model:** Nav3 has no `setRoot` equivalent; tabs = multiple back stacks.
    Verified from the 1.1.7 binary: no dedicated tab API exists; the pattern is N independent
    `rememberNavBackStack` instances each rendered by its own `NavDisplay`. `NavBackStack` implements
    `List<T>` + `StateObject` + `KMutableList` (independent mutable snapshot list) which is what makes
    per-tab isolation work. The documented per-tab *recipe* is NEEDS VERIFICATION (docs page is
    JS-rendered) — **blocks shell migration only, not the pilot.** Shell untouched in the pilot.
  - **CORRECTION — three separate removal gates (supersedes the earlier "both removed when Injekt
    ledger hits 0"):**
    | Removed | Gate |
    |---|---|
    | Voyager | end of Phase 3 — **ACHIEVED 2026-10-06, `4672e46fb4`** |
    | Conductor | end of Phase 4 (all Controllers rewritten to Compose) |
    | Injekt | Phase 5 (ledger hits 0) |

- 2026-10-04 **Hilt version lock — 2.58.** Chosen over 2.59+/2.60.x because 2.58 is the **last AGP
  8-compatible release**; 2.59+ hard-requires AGP 9 (the Hilt Gradle plugin's `HiltPluginEnvironment`
  throws at plugin-apply time when AGP < 9.0.0). An attempted bump to 2.60.1 failed on that gate
  (runs 37142981936/37142981937, RED). AGP 9 migration is deferred to a dedicated phase; Phase 2
  proceeds on AGP 8.12.2.
- 2026-10-04 **kotlin-metadata-jvm 2.4.0 KSP override.** Added `ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.0")`
  to `:app` (commit `210f5aded3`, THIS commit) — temporary override so the highest version wins on
  the KSP processor classpath and can read the Kotlin 2.4.0 metadata emitted by Kotlin 2.4.10. Hilt
  2.58 bundles kotlin-metadata-jvm 2.2.20 (metadata 2.3 max). Remove when Hilt ships a release
  bundling kotlin-metadata-jvm 2.4.x. Source: google/dagger#5177 + #5059, arkitekt.futured.app
  Android migration guide.
- 2026-10-04 **javapoet 1.13.0 pin in buildSrc: PERMANENT.** Root cause of Phase 2 FAILED CI #2 was
  `com.squareup.javapoet.ClassName.canonicalName()` `NoSuchMethodError` inside the Hilt Gradle plugin
  worker; javapoet 1.13.0 (which has `public String canonicalName()`) is what both Hilt 2.56.2 and
  2.58 declare, and the buildSrc pin forces that version onto the plugin classpath. Keep it.
- 2026-10-04 **qa/hilt-2.60.1 branch: ABANDONED.** Its Hilt 2.60.1 bump is blocked by the AGP 9 gate
  (HiltPluginEnvironment throws at plugin-apply time). Delete locally only if instructed; do NOT push
  a delete.

- 2026-10-03 Phase 1 `:source:api`: user chose **Option C — suspend-primary, deprecated Rx shim**.
  `getPopularManga/getSearchManga/getLatestUpdates/getMangaDetails/getChapterList/getPageList/
  getImageUrl` are now the real implementations (OkHttp `awaitSuccess()`); the deprecated
  `fetch*(...): Observable` methods are now defaults = `runAsObservable { getX(...) }`. `runAsObservable`
  gained an expect/actual in `:source:api` (`eu.kanade.tachiyomi.util.RxExtension`).
  **Behaviour change:** an extension that overrides only `fetchX` (Rx) is no longer consulted by the
  app, because `getX` no longer calls `fetchX`. RxJava stays on the :source:api classpath for the
  shim. Accepted by the user as a partial break.
- 2026-10-03 Verification: user chose **push a branch + open a PR**; CI Build on the PR is the oracle.

- 2026-10-03 **PHASE 2 RESOLUTION (user, verified).** Deps: `hilt = "2.56.2"`, `androidx-hilt = "1.4.0"`,
  `ksp = "2.3.10"` (user-confirmed compatible with Kotlin 2.4.10); libraries `com.google.dagger:hilt-android`,
  `com.google.dagger:hilt-compiler`, `androidx.hilt:hilt-navigation-compose`; plugins
  `com.google.dagger.hilt.android` + `com.google.devtools.ksp`. No FROM MEMORY items remain for Phase 2 deps.
- 2026-10-03 **DI ruling = (c) SPLIT.** `@Inject`/`@HiltViewModel` for all NEW code and new screens;
  EntryPoint shim for legacy `Injekt` sites; every new file in Phase 3+ must use `@Inject`; sub-ledger
  "Injekt call sites remaining" starts at 740, target 0 by end of Phase 4; Injekt deleted only in Phase 5
  after the ledger hits 0.
- 2026-10-03 **KMP ruling = `:app` only.** Hilt is Android/JVM-only; KSP on KMP `androidTarget` is not a
  supported combination. Do NOT apply Hilt to `:data`, `:domain`, `:source:api`, `:core:main`, `:i18n` in
  Phase 2. Library modules stay on Koin/Injekt.
- 2026-10-03 **Phase 4 library coordinates (verified, not acted on):** `zone.ien.hig:hig:1.3.1`
  (github ienground/compose-hig) — Cupertino components + spring physics; `ir.farsroidx:compose-overscroll`
  (github farsroidx/ComposeElasticOverscroll); `io.github.kyant0:backdrop` already used (glass effect only,
  no components). **\"Compose Symphony\" is NOT published** (github jay3-yy/Compose-Symphony, Maven pending) —
  DO NOT add; vendor `SpringAnimations.kt` or skip (Compose HIG already has spring physics).

- 2026-10-03 Phase 1 finding: `:source:api` RxJava is the **extension API**, not removable
  dead weight. `awaitSingle` is `expect` in commonMain / `actual` in androidMain delegating to
  `core:main` `RxCoroutineBridge.awaitSingle`. Options presented to user (A keep shim / B remove +
  break extensions / C replace with suspend-only). Awaiting choice.

- 2026-10-03 Phase 2 **WORKING (pre-fire):** catalog coordinates + Hilt/KSP plugins on :app only; `:app`
  `@HiltAndroidApp`; `HiltInjektBridgeModule` + deprecated `@EntryPoint`. KSP/javapoet check is the blocker
  (build never gets past AAR metadata).

- 2026-10-03 **Phase 2 FAILED CI #1** — commit `4834192b5a` (run 37133779065, `Build Debug APK` +
  `Lint & Type Check` red): 8 issues from `androidx.hilt:hilt-navigation-compose:1.4.0` + transitive
  `hilt-lifecycle-viewmodel-compose:1.4.0`, `androidx.lifecycle:lifecycle-runtime-compose-android:2.11.0` —
  all require compileSdk 37 (`:app` is 36) and AGP 9.1.0+ (repo has 8.12.2). `com.google.dagger:hilt-android`
  + `hilt-compiler:2.56.2` are NOT implicated. `hilt-navigation-compose` subsequently dropped.

- 2026-10-03 **Phase 2 FAILED CI #2** — commit `5e70ef2e57` (run 37134553400, after re-adding
  `@HiltAndroidApp` + `HiltInjektBridgeModule` and `javapoet:1.13.0` to buildSrc): `:app:hiltAggregateDepsStandardDebug`
  FAILED → `dagger.hilt.android.plugin.task.AggregateDepsTask$WorkerAction`
  `NoSuchMethodError: java.lang.String com.squareup.javapoet.ClassName.canonicalName()`.
  Root cause = `com.squareup:javapoet` ABI mismatch inside the Hilt Gradle plugin worker (the installed
  javapoet on the buildscript classpath is too old for Hilt 2.56.2). KSP 2.3.10 + Kotlin 2.4.10 still
  **NOT verified** — the build dies at AAR metadata/hiltAggregateDeps, before Kotlin compilation.

- 2026-10-07 **PHASE 4 PRE-FLIGHT: ReaderViewModel transient retention (~15s window, ~115KB per
  instance, held by a `withTimeout` in a transitive call path, likely extension `getPages`).** Options:
  (a) accept as transient, (b) restructure `getPages` invocation to release the VM reference,
  (c) chase the extension's timeout handling. **Do not fix in Phase 3.** Evidence: LeakCanary trace
  `ReaderViewModel$loadChapter$1`; `watchDurationMillis = 6494`, `retainedDurationMillis = 1493`; object
  self-releases when the timeout fires. Trace chain out-in: `TimeoutCoroutine` (scheduled on Main) →
  `DispatchedCoroutine` (`withIOContext`, `ReaderViewModel.kt:430`) → `UndispatchedCoroutine`
  (`withContext(NonCancellable)`, `CoroutinesExtensions.kt:31`) → `loadChapter$1` → `ReaderViewModel`.
  **Verified by source grep: the `withTimeout` is NOT ours.** Zero `withTimeout` in the whole
  `ui/reader/**` package (only `ViewConfiguration` timeout constants in
  `GestureDetectorWithLongTap.kt:23-24`); repo-wide the only one is
  `MangaDetailsController.kt:759` `withTimeoutOrNull(1000)`, a MangaDetails path. Also disproven:
  none of the 11 `launchNonCancellableIO` sites (`:448 :462 :524 :542 :593 :684 :894 :950 :980 :1043
  :1053`) contains a `withTimeout`. Naming the owning library **NEEDS VERIFICATION** — requires the
  trace frames between `TimeoutCoroutine` and `DispatchedCoroutine`. Bounded window; contributes GC
  pressure and jank (20 chapter opens ≈ 2.3MB retained), not a permanent leak.

- 2026-10-07 **PHASE 4 PRE-FLIGHT: LRU cache hit rate 25%** (from the same LeakCanary trace metadata,
  `LruCache[maxSize=3000, hits=86416, misses=257563, hitRate=25%]`). Cache too small for the workload,
  or images requested with cache-busting keys (different URLs / headers / request keys). Coil config
  or request keys suspected. Likely contributes more to jank than the leaks above. **Do not fix now.**

- 2026-10-07 **URGENT — `composeLibrary` flag IS user-reachable; the Library tab is a LIVE BROKEN TAB
  when on.** `yokai/domain/base/BasePreferences.kt:54`
  `composeLibrary() = preferenceStore.getBoolean("pref_use_compose_library", false)`. Gate at
  `MainActivity.kt:637` `R.id.nav_library -> if (basePreferences.composeLibrary().get())
  LibraryComposeController() else LibraryController()`. **Two settings entry points, both gated only
  by `if (BuildConfig.FLAVOR == "dev" || BuildConfig.DEBUG)`** — i.e. compiled IN for every debug
  build:
    - Compose: `yokai/presentation/settings/screen/SettingsAdvancedScreen.kt:398-404`
    - View: `eu/kanade/tachiyomi/ui/setting/controllers/legacy/SettingsAdvancedLegacyController.kt:396-401`
  Label "Use experimental compose library" (`i18n/src/commonMain/moko-resources/base/strings.xml:169`).
  **CI ships `assembleStandardDebug`** (`.github/workflows/ci.yml:32`) → `BuildConfig.DEBUG` is TRUE →
  **the switch is visible in the installed artifact.** `.get()` is read inside `setOnItemSelectedListener`,
  so **no restart is needed** — toggling it and switching tabs takes effect immediately.
  **When ON:** `LibraryComposeController.kt:30` renders `LibraryContent.kt:12` with hardcoded
  `(0..50).map { LibraryItem.Blank(it) }` and `columns = 3` (`:80-81`), and `LibraryContent.kt:31-42`
  prints three `Text(...)` placeholders. `:63-64` hides `swipeRefresh` + `fastScroller`.
  `showSheet/hideSheet/toggleSheet` are **empty** (`:85-92`) → **re-tap on the Library tab does nothing**,
  breaking the `toggleSheet` contract at `MainActivity.kt:643-648`. No action mode, no covers, no search.
  Default is `false`, so a fresh install is unaffected. **Fix is Step 2b, NOT part of the Cupertino
  rewrite.** Options to rule on: (a) hide the switch, (b) default it off AND suppress the tab when a
  stub build is active, (c) finish the Library rewrite and let it become real.

- 2026-10-07 **AGP 9 MIGRATION PHASE (deferred): unlocks Compose HIG, Compose BOM 1.12.x,
  Nav3 1.2.0+, backdrop 2.0.1+, ComposeElasticOverscroll 1.0.0 (pending activity-compose 1.13.0
  compileSdk check). Prerequisites: Gradle 9.1+, built-in Kotlin, buildSrc DSL migration, compileSdk 37,
  removal of kotlin-android plugin. Requires separate reconnaissance session.** Evidence gathered
  2026-10-07: `zone.ien.hig:hig` **every published version** (1.3.1 → 27.0.1-alpha01) pulls
  `org.jetbrains.compose.foundation:foundation:1.12.0-rc01/1.12.1`, whose `available-at` chain resolves to
  `androidx.compose.foundation:foundation:1.12.0-beta01` — the Compose 1.12 line, hence compileSdk 37.
  Also `org.jetbrains.compose.material3:material3:1.12.0-alpha03` → `androidx.compose.material3:1.5.0-alpha22`.
  Same wall as backdrop 2.0.1 (wants `foundation 1.12.0`). `ir.farsroidx:compose-overscroll:1.0.0` is the
  ONLY near-compatible library: it wants `androidx.compose:compose-bom 2026.05.01` (exactly our BOM) with
  version-less compose deps; open item is `androidx.activity:activity-compose 1.13.0` (repo on 1.12.0) and
  its compileSdk impact — NEEDS VERIFICATION, settle with a one-line CI probe.
  **NOTE: `io.github.kyant0:android-liquid-glass` DOES NOT EXIST** — `https://repo1.maven.org/maven2/io/github/kyant0/android-liquid-glass/`
  returns HTTP 404, and the group directory `https://repo1.maven.org/maven2/io/github/kyant0/` contains
  exactly one entry, `backdrop/`. Phase 4 ruling: hand-written Cupertino on backdrop 1.0.6, no HIG.

- 2026-10-07 **PHASE 4 RULING — migration order (Option A approved):**
  1. smallest first step — `LibraryContent.kt:31-42` real `LibraryItem.Manga` arm via the existing Coil
     path (`yokai/presentation/manga/components/MangaCover.kt:15-16`). No shell/controller/dependency change.
  2. Library tab full rewrite (covers, click, long-press action mode, hopper, filter sheet). No shell change.
  3. Recents tab rewrite. No shell change.
  4. Browse tab rewrite. No shell change.
  5. Shell swap: `MainActivity.setRoot` → N `NavDisplay`s, one per tab, only active composed.
  6. Conductor removed at Phase 4 exit.
  **Cupertino design system lands as its OWN commit BEFORE any component rewrite** — type ramp
  (largeTitle/title1-3/headline/body/callout/subhead/footnote/caption1-2), spacing 4/8/12/16/20/24/32,
  continuous radii ~10/12/16/22, spring-for-taps / ease-for-content motion, semantic (non-Material) colour
  tokens, haptic wrappers light/medium/heavy/selection/success/warning/error. It adds tokens only and must
  change no screen.
  **Skill availability 2026-10-07:** `frontend-design`, `agentic-visual-design`, `agentic-typography`,
  `agentic-animation` ARE installed. `/animate`, `/delight`, `/typeset`, `/polish` (jimiryquai/pi-agent)
  are NOT installed in this session — their four passes (animation / micro-interaction / typeset / polish)
  fold into the four hypnotox decomposition skills. Do not pretend to use skills that are absent.

- 2026-10-07 **CORRECTION to an earlier filing — `ReaderViewModel.kt:162` `runBlocking` is NOT main-thread
  jank.** Full chain verified: `:518 fun onPageSelected` (not suspend) → `:524 viewModelScope.launchNonCancellableIO`
  → `CoroutineExtensions.kt:31 launchIO { withContext(NonCancellable) }` on **Dispatchers.IO** →
  `:624 saveChapterProgress` (suspend) → `:639 onChapterReadComplete` (suspend) → `:663`, the only call site.
  The `runBlocking` therefore blocks an **IO-pool thread that is already off the main thread**. Residual
  costs, both small: pins one IO worker for the DB query, and because the scope is `NonCancellable` that
  thread is held past `onCleared()`. The "cannot be moved off the main thread" framing is moot — it is
  already off it. No fix proposed; the `by lazy` + non-suspend initialiser means it cannot simply become a
  suspend call without changing the declaration.

- 2026-10-07 **CORRECTION — the 25% LRU hit rate is NOT Coil. Do not "fix Coil" on this evidence.**
  `App.kt:309-313` sets `memoryCache(MemoryCache.Builder().maxSizePercent(context).build())` with no
  explicit maxSize, but Coil 3.4.0's `RealMemoryCache` is not an `android.util.LruCache` and its limit is
  in **bytes**; LeakCanary's `LruCacheTracker` only instruments `android.util.LruCache`.
  `grep -rn LruCache app/src core data domain` finds only `DiskLruCache` in `ChapterCache.kt:6,94,95,154,214`
  — **zero `android.util.LruCache` in the repo**, and zero hits for a `3000` literal. So
  `LruCache[maxSize=3000, hits=86416, misses=257563]` is a **dependency's** entry-counted cache.
  **Owning class NEEDS VERIFICATION** — same evidence gap as Leak 1: requires the LeakCanary trace's
  owning-class header. Not being chased.

- 2026-10-07 **ALSO FALSE-POSITIVE PROOF — `MaterialFastScroll.kt:22` is not a Controller.** It is an
  `open class MaterialFastScroll : FastScroller` with a `var controller: BaseLegacyController<*>?` field
  at `:28`; a naive "extends Base*Controller" grep over a 9-line class header counts it. The corrected
  Conductor census is 29 real declarations (6 shared bases + 23 leaf `Base*Controller`) plus 13
  `SettingsLegacyController` subclasses and 2 `SettingsComposeController` subclasses = 38 leaf controllers.
  `ExtensionDetailsController` name collision resolved 2026-10-07 (`c529216b9d`, dead stub deleted).

---

## PHASE 4 — DESIGN SYSTEM LANDED (`24f0c4150f`, CI GREEN 2026-10-07)

Jobs: 112891911055 Build Debug APK · 112891911838 Build app · 112891910790 Lint & Type Check.
23/23 new tests pass, 0 fail. **Warning count 1052 -> 1052, delta 0.**

### Module placement: :presentation:theme (option a), NOT a new :presentation:design

Decisive argument: `YokaiTheme` (`Theme.kt:214`) is already the single wrapper every screen
enters. A separate module would need a second wrapper and every consumer would have to remember
both. Corroborating: `presentation/theme/build.gradle.kts:36-38` already states the module's
boundary — libraries are referenced *only* from there so consumers never depend on `backdrop`/
`haze` directly. Cupertino tokens serve that same boundary. All six deliverables need only
dependencies the module already has (`:31-40`). One consumer only (`:app`, `app/build.gradle.kts:158`),
so there is no fan-out to protect. Option (b) would add a Gradle module and a project dependency
edge for zero new capability. Glass and Cupertino are one design system here, not two.

### Files

| file | lines | what |
|---|---|---|
| `Typography.kt` | 249 | `CupertinoType` (12 roles) + `@Deprecated` `YokaiTypography` bridge |
| `Spacing.kt` | 83 | `space4/8/12/16/20/24/32` + `legacySizesAgree()` |
| `Constants.kt` | 73 | `Size` retained, values frozen, deprecation in KDoc only |
| `CornerRadii.kt` | 89 | 10/12/16/22 + shapes |
| `Motion.kt` | 133 | 4 springs + no-tween marker |
| `Haptics.kt` | 149 | 4 functions, version-verified |
| `SemanticColors.kt` | 145 | 12 semantic tokens x 2 schemes |
| `app/.../CupertinoTokensTest.kt` | 296 | 23 tests pinning every table to the doc |

Plus one line in `Theme.kt` (`@Suppress("DEPRECATION")` on `YokaiTheme`).

### Ledgers — UNCHANGED, nothing migrated

- Typography bridge: **22 files / 35 call sites** (unchanged)
- Size deprecation: **11 files / 40 call sites** (unchanged)
- Consumers of the new tokens outside `presentation/theme`: **0**. Lands inert, as designed.

### Haptic scope: four shipped, three DEFERRED

Shipped: `hapticLight` (CONTEXT_CLICK), `hapticMedium` (LONG_PRESS), `hapticSelection` (CLOCK_TICK),
`hapticWarning` (CONFIRM, guarded).

**Deferred: Heavy, Success, Error. Add when a screen needs one. Do not invent mappings.**
Heavy has no constant distinct from LONG_PRESS at the `View.performHapticFeedback` level, so a
mapping now would make it a synonym for Medium. Success — CONFIRM is plausible but hapticWarning
already owns it. Error — REJECT is API 30+ and AOSP maps it to `EFFECT_DOUBLE_CLICK`
(android-10.0.0_r1:5333), which is not obviously an error to a user who cannot see the screen.

### Haptic version checks — VERIFIED, cited in KDoc

Public API level (AOSP `frameworks/base/api/current.txt`, android-8.0.0_r1 … android-11.0.0_r1):

| constant | A26 | A27 | A28 | A29 | A30 | first public |
|---|---|---|---|---|---|---|
| LONG_PRESS | yes | yes | yes | yes | yes | <=26, no guard |
| CLOCK_TICK | yes | yes | yes | yes | yes | <=26, no guard |
| CONTEXT_CLICK | yes | yes | yes | yes | yes | <=26, no guard |
| CONFIRM | **no** | **no** | **no** | **no** | yes | **30, guard required** |

Runtime: `View.performHapticFeedback(int,int)` does **no validation** — forwards the raw int
verbatim (android-9.0.0_r1 `core/java/android/view/View.java:23668`). The decision is
server-side in `PhoneWindowManager.getVibrationEffect(int)`:

| API | source | CONFIRM | effect |
|---|---|---|---|
| 26 | android-8.0.0_r1:7710 | absent -> `default: return null` | **silent no-op** |
| 27 | android-8.1.0_r1 | absent -> `default: return null` | **silent no-op** |
| 28 | android-9.0.0_r1:8085 | `EFFECT_CLICK` | works |
| 29 | android-10.0.0_r1:5327 | `EFFECT_CLICK` | works |

`performHapticFeedbackLw` returns false when effect is null, so on Android 8.0/8.1 a destructive
confirm would be **completely silent**. Guard is at `SDK_INT >= 30`, NOT 28: AOSP 28/29 handle it
but that is an unpublicised internal that OEM forks need not share. Side benefit — 28/29 get
`LONG_PRESS` -> `EFFECT_HEAVY_CLICK`, more emphatic, which suits the one haptic reserved for
destructive confirm.

Also verified: `performHapticFeedbackLw` reads `Settings.System.HAPTIC_FEEDBACK_ENABLED`
(android-9.0.0_r1:8045-8048), so `LocalView` satisfies §5 "respects the system setting" by
construction. `Vibrator`/`VibratorManager` bypass it.

### Design decisions that are NOT what they look like

1. **Bridge line heights frozen at caption1 17 / caption2 15**, while `CupertinoType` uses the
   doc's 16 / 14. §2.1 disagrees with the shipped file here. Aliasing would restyle 35 call
   sites in 22 files — i.e. touch screens this commit must not touch. The delta lands at migration.
2. **`Size` is NOT `@Deprecated`-annotated.** Annotating it warns at all 40 call sites in 11 files
   I may not touch, turning "no new warnings" into 40 new warnings. The deprecation is recorded in
   KDoc with the migration ledger instead. `Typography` IS annotated because it has exactly one
   internal reference (`Theme.kt:224`), suppressed there.
3. **Motion damping values are literals, not Compose constants.** Verified from
   `animation-core-android-1.10.3` `Spring.class`: `DampingRatioMediumBouncy` is **0.5**, not the
   0.60 `bouncy` needs. Reaching for the named constant would have silently restyled all four
   springs.
4. **`CornerRadii` are `RoundedCornerShape`, not squircles.** A squircle needs Superellipse's
   point array through a `GenericShape`; that belongs to the first component that wants it.
   Superellipse (built, `CORNER_EXPONENT = 4.0`) untouched.
5. **`AUTHORED_ANIMATIONS_MUST_NOT_TWEEN` is a greppable marker, not a Lint check.** A real one
   needs a custom `LintCheck` in buildSrc — out of scope for a token commit.

### LANDMINES FOUND (do not re-step)

- **`spring()` parameter order is `(dampingRatio, stiffness, visibilityThreshold)`** — damping
  FIRST (verified from the published sources jar, `AnimationSpec.kt:808-812`:
  `fun <T> spring(dampingRatio, stiffness, visibilityThreshold)`). Named args saved this commit;
  positional args would have transposed every spring in the system and no test would have caught
  it, because the assertions would have been written against the same wrong object.
- **Kotlin binds infix `shouldBe` TIGHTER than `>`**, so `abs(x) > 0.01f shouldBe true` parses as
  `abs(x) > (0.01f shouldBe true)`. Needs parens.
- All four springs are underdamped (`dampingRatio < 1`), so "only bouncy overshoots" is a *usage*
  rule, not a physics claim and is not testable as `dampingRatio < 1`. Tested instead as "bouncy
  has the minimum damping ratio".

### OPEN — PROVISIONAL, must be sourced before Phase 4 exit

`docs/DESIGN_CUPERTINO.md` §3 introduces four tokens with no hex values, and Apple's HIG carries
none (the page is JS-rendered; a direct fetch of developer.apple.com/design/human-interface-guidelines/color
returned 17,807 bytes and zero hex literals). Shipped marked PROVISIONAL and enumerated in
`PROVISIONAL_TOKENS` so a grep finds them:

- `labelTertiary` · `labelOnAccent` · `separatorOpaque` · `warning`

### OPEN — doc contradiction, resolve before the sheet component

`DESIGN_CUPERTINO.md` §5 says sheet dismiss is "heavier than present, matching §4.2 rule 4", but
§4.2 rule 4 states dismiss is **gentler** than present. Flagged in `Haptics.kt` KDoc on
`hapticLight`. NEEDS VERIFICATION.

### Deferred perf items (unchanged from the brief commit)

LRU owner (`LruCache[maxSize=3000]`) NEEDS VERIFICATION · Leak 1 transient, Phase 4 pre-flight ·
`ReaderViewModel.kt:162 runBlocking` residual (pins one IO worker; not main-thread jank) · Leak 3
`SystemForegroundService` — do not fix, known framework bug.

### COMPONENT 1 LANDED — grouped section list

`066a2e4ba7` (red) then `122a0aae9d` (fix). CI GREEN, 44/44 tests, warnings 1052 -> 1052.
Inert: zero consumers outside its own file + test.

Three new files under `presentation/theme/.../theme/components/`:
`GroupedSection.kt` 336, `GroupedSectionMetrics.kt` 116, `CupertinoChevron.kt` 92,
test 200. Also one addition to `SemanticColors.kt` — `CupertinoColors.current/light/dark`,
filling a gap the token commit left (`LocalCupertinoColors` existed with no accessor).

**Module placement (ruled).** `presentation/theme/components`, NOT `presentation/core`.
Decisive evidence: `presentation/core/build.gradle.kts:32` declares `api(libs.material)` — it
re-exports Material to everything downstream, and all three of its existing components
(`LazyGrid.kt`, `LinkIcon.kt`, `VerticalFastScroller.kt`) import `androidx.compose.material`.
A Cupertino component in that package interleaves two design languages inside the one module
whose public contract is Material. `theme` already carries every dependency needed.

**Zero Material imports.** Text is `BasicText`, chevron is a hand-drawn `ImageVector`.
Checkable: `grep -c "^import androidx.compose.material3" GroupedSection.kt` -> 0.
(KDoc prose mentioning material3 will trip a naive `grep -c "androidx.compose.material3"` —
the `^import` anchor is what matters.)

#### SPEC CORRECTION — the two insets STACK

§3.2 said both that insets "stack visually" and that content has "0 additional". Those
conflict. Resolved, and `docs/DESIGN_CUPERTINO.md` §3.2 now states it with a diagram so it is
not re-litigated:

- 16dp screen edge -> card edge (`sectionInset`)
- +16dp card edge -> row content (`rowContentInset`)
- => row label sits **32dp** from the screen edge
- => separator starts at **16dp**, aligned to content, **never full-bleed**

A collapsed reading would put the label at 16dp and lose the tell entirely. Both are named
constants in `GroupedSectionMetrics`, so the alternate reading stays a one-line change.

**Inset does not scale with configuration.** iOS uses 16pt regular / 20pt Plus-Max. Not
implemented, deliberately: repo has **zero `values-sw*` resource dirs**, so a qualifier split has
no infrastructure, and a code-based split would introduce a second layout mode with no
precedent. `Spacing.space20` already exists — a tablet layout is a one-token change.

**Row radius** `CornerRadii.medium` = 12dp (looser end of the 10-12dp range; iOS tightens as
a list grows taller, `CornerRadii.small` = 10dp is the tighter end).

#### CHEVRON — hand-drawn, marked tune-on-device

Not Material (`Icons.AutoMirrored.Filled.KeyboardArrowRight` is a *filled* 24dp path with
miter joins tuned for 48dp rows; too heavy and too square beside a 44dp row at 15pt).
Not SF Symbols — Apple-licensed, same constraint as SF Pro (`DESIGN.md` §7.1).

**NEEDS DEVICE VERIFY — file, do not block.** Stroke weight (1.5dp), cap style (Round), and
aspect ratio (7x12dp) were shipped as stated intent, not measurements. SF Symbols was not
inspectable and Apple's HIG is JS-rendered (curl returned 17,807 bytes, zero hex literals).
Verify on hardware against settings-list density.

**Separator is one PHYSICAL pixel** (`drawBehind` inside a 1dp box). A 1dp rule at 3x is a 3px
band and reads as a gap, not a hairline.

#### TWO COMPILE FAILURES, both mine, both from unverified inference

First CI run red, 11 errors, two causes. Fixed in `122a0aae9d`.

1. **`autoMirrored()` does not exist.** `ImageVector.kt:119` declares
   `private val autoMirror: Boolean = false` as the last ctor arg of `ImageVector.Builder`;
   the old signature survives only as a `DeprecationLevel.HIDDEN` secondary constructor.
   I inferred a builder method from the `getAutoMirror()` property name without checking.
   -> `autoMirror = true` on the Builder call.
2. **`BasicText` has no `color: Color`.** `BasicText.kt:101` is `color: ColorProducer? = null`,
   a fun-interface SAM, so a call site wants `color = { Color }`. Ten errors from five sites
   written against Material3 `Text`'s signature. Colour now rides on the `TextStyle`.
   **Verified equivalent rather than assumed:** the producer threads to `overrideColor`
   (`TextStringSimpleElement.kt:57`), which stays null when the param is omitted, so
   `style.color` is what paints.

**Pattern filed:** `javap` on a compiled class shows getters, not the builder API. The
property existed; the function I invented did not. Read the Kt source for anything
constructed, `javap` only for constants.

#### NOT COVERED BY TESTS — honest gap

This repo has **no Compose UI test infrastructure**: no `androidTest` source set, no
Robolectric, no `ui-test-junit4`, no Compose test bundle. CI runs only
`testStandardDebugUnitTest` (`.github/workflows/ci.yml:40`). A `createComposeRule()` assertion
would compile and then **never execute**.

So the component was shaped to make every decision a pure function in `GroupedSectionMetrics`,
and the 200-line JVM test pins the inset arithmetic, separator alignment, last-row rule, touch
floor, chevron inference and header casing.

**Still needing a render tree, and NOT claimed as tested:** that padding lands at 32dp on
screen; that the subtitle paints *below* rather than beside the label; chevron pixel weight;
click dispatch. Closing this needs `ui-test-junit4` + Robolectric in `app/build.gradle.kts`,
or an instrumented source set. **Deferred — not blocking Phase 4.**

### COMPONENT 2 LANDED — large-title collapsing bar

`d7ede2e087` then `721c74712e` (fix). Component 3 below, then `19f4a1f3ae`. CI GREEN on head,
87/87 tests, warnings 1052 -> 1052. Both **inert** — zero consumers outside own file + test.

Files: `LargeTitleBar.kt` 285, `LargeTitleBarMetrics.kt` 157, test 220.

#### API — a caller-fed state holder plus a bar, not a modifier and not a wrapper

```kotlin
@Composable fun rememberLargeTitleBarState(): LargeTitleBarState
LargeTitleBarState { fraction, barAlpha, showHairline, titleFontSize, onScroll, onSettle, velocityCapture }
@Composable fun LargeTitleBar(title, state, modifier, actions)
```

- **Not a modifier** — a modifier has nowhere to put the title's *layout space*. The title starts
  in the scroll content, so it needs a subtree holding two type sizes, not a decoration.
- **Not a wrapper** — would dictate every screen's Box structure. This app wraps screens in a
  bottom nav (`RecentsController.kt:544`) and a download bottom sheet (`:549`); shell work is
  later phases.
- **Not state-taking**, for two source-verified reasons:
  - `ScrollState` exposes **no velocity** — it is `value`/`maxValue`/`viewportSize`/
    `interactionSource` (`foundation/Scroll.kt:89-120`). Fling velocity exists only on
    `NestedScrollConnection.onPostFling` (`NestedScrollModifier.kt:86`).
  - Screen content will be `LazyColumn`; `LazyListState` reports `firstVisibleItemIndex` +
    `firstVisibleItemScrollOffset` — a different coordinate space.

So the state is fed a **collapse fraction**, the way Material3's `TopAppBarState` is fed
`heightFraction`/`collapsedFraction`. Same reason, same shape. The bar ships the missing half
itself: `velocityCapture()`, a `NestedScrollConnection` that records the fling for `onSettle()`.

#### The invariant, enforced structurally not by convention

While the finger is down, `onScroll` is a **plain field write** — no coroutine, no clock, no
animation — so the title tracks the finger exactly. Only `onSettle()` starts a spring, and it
cancels any in-flight one first. The old behaviour (title animating while the list sits still)
**is not expressible through this API**. That is the point. (`DESIGN_CUPERTINO.md` §4.1 rule 5.)

`onSettle()` converts px/s through `collapseVelocity()` into `initialVelocity` on
`Animatable.animateTo(target, animationSpec, initialVelocity)` (`Animatable.kt:227`);
`SpringSpec` is an `AnimationSpec`, so `CupertinoMotion.default` drops in unchanged.

Type interpolates **size** 34->17sp on one fraction rather than cross-fading two runs, so the bar
cannot reflow its own height mid-collapse. Weight lerps Normal->SemiBold on the same fraction.

Bar reaches opaque at **0.6** of the collapse — before the type settles, or the title shrinks
through a translucent background and washes out mid-run. Asserted, not assumed.

**NEEDS DEVICE VERIFY** (file, do not block): whether the 32dp collapse window feels right at 3x.
The window is a judgement call, not a spec number.

### COMPONENT 3 LANDED — segmented control

`139bd3abc0`, then `039914c4db` + `19f4a1f3ae` (fixes). CI GREEN. Inert.

**`RecentsController` is View-based** (`binding.recycler`, `RecentMangaAdapter`) — there is no
Compose Recents screen yet, so this lands unused until the Recents rewrite.

**Generic `<T>`, not an index.** `:925` builds tabs from `RecentsViewType.entries` and hands the
result to `setViewType(RecentsViewType)` at `:743`. An Int-indexed control makes that call site
`entries[index]` in / `entries[indexOf]` out, with a silent wrong answer if they ever disagree.
`RecentsViewType` is in `:app` and this is in `:presentation:theme`, so naming it is not on the
table — a type parameter carries it across free.

**One callback**, fired on tap *and* mid-drag. Not two: a control drawing the highlight on
segment 2 while reporting segment 1 is lying. Screens that must not persist mid-drag read
`state.isDragging` and commit on release. Component reports truth; screen decides what truth costs.

**Drag-to-select** — a 5-segment control is otherwise tiny tap targets, and tap-only is the thing
users of iOS segmented controls reach past. Thumb follows the finger continuously
(`indicatorPosition()` returns a *float* cell index); only release snaps.

**Haptic = one tick per boundary crossed**, not per frame: `boundariesCrossed()` returns a count
so the caller fires once per crossing. Sitting exactly on a boundary does not tick until past it
(strict inequalities). Returning a Boolean would force a tick every straddling frame.

Thumb 2dp inside the track with radius reduced to match — at zero inset thumb and track are the
same shape and the control reads as one pill. Height **32dp not 44**: toolbar control inside a row
that is already 44dp. Segment count **validated with `require`, not clamped** — past 5 iOS
switches to a menu, and clamping silently hides a segment.

### THREE RULES-OF-THUMB — cost 5 red CI runs across 3 components

**1. `BasicText` takes `color: ColorProducer?` (a SAM), not `Color`.** Hit twice (Component 1's
five call sites, then Component 3). Use the style-colour route: `style = X.copy(color = c)`.
Verified equivalent, not assumed: the producer threads down as `overrideColor`
(`TextStringSimpleElement.kt:57`), null when the param is omitted, so `style.color` is what paints.
**Check this before writing any `BasicText`.**

**2. State holders: pick the primitive that matches the field type.** `var dragging by
mutableFloatStateOf(false)` used as a Boolean made the field `Float` and cascaded into **seven**
errors across the file from one token. `mutableStateOf(Boolean)` for Booleans,
`mutableIntStateOf` for Ints, `mutableFloatStateOf` for Floats.

**3. `@Composable` cannot be called from non-composable lambda contexts** — `pointerInput`,
`detectHorizontalDragGestures` callbacks, and also `remember { }` and `animateTo { }`. Resolve the
composable value in the composable body and pass the resulting plain lambda in. `hapticSelection()`
became `performSelectionHaptic(view)` for exactly this reason.

**3b (same family, new): extension lambdas have their own receiver.** Inside `animateTo { }`,
`value` is `Animatable.value` — read-only — so assigning to it failed. Write through an explicit
`this@Owner.value = value`. `remember { }` and `animateTo { }` are *not* composable either, so
`LocalView.current` cannot be read inside `remember { }`; read it outside and capture.

### MORE LANDMINES THIS SESSION

- **`Modifier.width` takes Dp, not pixels.** Handing it raw px fails with a useless "None of the
  following candidates is applicable".
- **`Float.toDp()` is a `Density` *member*** (`Density.kt:81`), not a free function — not in scope
  in a `BoxWithConstraints` content lambda. Convert explicitly: `Dp(px / density.density)`.
- **`Size` is `androidx.compose.ui.geometry.Size`**, not `ui.unit.Size`. `drawRect(size=)` wants
  geometry.
- **`TextUnit` has no `plus`/`minus`** — only scalar multiply, because adding two type sizes is
  meaningless. Use the public `lerp(TextUnit, TextUnit, Float)` (`TextUnit.kt:367`).
- **`ImageVector.autoMirror` is a `Builder` ctor param** (`ImageVector.kt:119`), not a chainable
  `autoMirrored()`. `javap` shows getters, not the builder API — read the Kt source for anything
  constructed.
- **Degenerate density must be caught with `!(pxPerDp > 0f)`**, not `pxPerDp <= 0f` — NaN passes
  `<= 0f` and poisons every downstream interpolation.
- **A `Float` return typed `Int`** (`floor(x + 0.5f)`) needs `.toInt()`.
- **My own test expectation was wrong once**: I asserted `boundariesCrossed(1.1, 1.9) == 0`, but
  1.1 -> 1.9 genuinely crosses the 1.5 boundary. The code was right; the test was not. Verified
  every arithmetic expectation with a Python mirror before committing.

### COMPONENTS 4 + 5 LANDED — search bar + switch

`be68655757` then three fix commits to `71b8051d0d`. CI GREEN on head, **104/104** tests,
warnings 1052 -> 1052. Both **inert**.

Note the commit split did NOT come out as ruled — see "COMMIT HYGIENE" below. Content is correct;
only the separation was lost.

#### Component 4 — search bar. API: hoisted query, owned focus

Neither fully stateless nor stateful, because the three consumers do not agree on that split.

| Consumer | What it needs |
|---|---|
| `LibraryController.kt:1444` `search(query: String?)` | Query arrives from `SearchActivity`; heavy side effects — toggles `showAllCategoriesView`, `scrollToPosition(0)`, mutates adapter scrollable headers |
| `GlobalSearchController.kt:190,213` | Pushes query **into** the view via `setQuery(...)`; handles URL + extension-intent cases itself |
| `BrowseSourceController` | Captures a hint then navigates away — **no submit at all** |

- **`query` + `onQueryChange` hoisted.** All three own the query in a presenter with side effects
  welded to it. A bar that owned it would reconcile its own state on every keystroke — the
  two-way sync that forces rewrite-then-migrate.
- **Focus owned by the bar.** No consumer has an opinion about "am I focused"; hoisting it pushes
  IME bookkeeping onto three screens for nothing.
- **`contentScrolledUnder: Boolean` hoisted.** Caller already knows it; deriving it here would mean
  the bar owning a nested-scroll connection it has no business owning.
- **`onSubmit` / `onCancel` nullable, not paired with `show*` flags** — a flag and a callback can
  disagree. A Cancel that calls nothing is worse than no Cancel, so its visibility is gated on the
  handler existing at all.
- **`cancelLabel` is a parameter.** Hard-coding "Cancel" would be untranslatable.
- **No haptic on Cancel.** §5 gives the switch toggle none because its animation is the feedback;
  same argument here — the field collapsing back to unfocused is the acknowledgement.

`barAlpha(false) = 0f` exactly. "Flush, no elevation" means **zero**, not a faint tint; anything
else puts a visible grey rectangle on an empty screen.

Height 36dp sits deliberately between the segmented control's 32 and a 44dp row. Radius 10dp
**asserted against the Material pill shape** (`radius == height/2`) that §6 rules out.

The focus disc shares the bar's radius, not a rounder one — iOS separates them by fill, not shape.

#### Component 5 — switch. Stateless `(checked, onCheckedChange)`

Confirmed against both real consumers, not assumed: `ThemeStep.kt:99` and
`SwitchPreferenceWidget.kt:24` both own the value in a preference and read it statelessly.

**Superellipse needed NO small-surface variant — the ruling asked, and the answer is no.**
`Superellipse.path()` is parameterised by half-extents and segment count. `DEFAULT_SEGMENTS = 64`
kept (at 51dp that's ~2.4dp per segment); named as `TrackSegments` so a larger switch can raise it.
Superellipse was not rebuilt.

Travel is **derived** (`TrackWidth - 2*inset - ThumbSize = 20dp`), not hard-coded, so changing the
track or thumb in one place cannot slide the thumb off the end.

Thumb is a **circle, not a squircle** — inside a 31dp track the corner exponent's contribution is
under a pixel and iOS draws a circle. Asserted in the test so nobody "fixes" it to match.

Reversing mid-flight springs from **where the thumb currently is**, not the previous target, so a
fast double-tap does not snap the wrong way.

#### COMMIT HYGIENE — a mistake worth filing

I ran `git add -A` while fixing the search bar, which **swept the entire switch component into the
search-bar fix commit**. `b24132b73a`'s message describes only the search fix, so it
under-describes its own contents (`git show --stat b24132b73a` shows 4 files, 266 insertions, not
the 2 files the message implies). No force-push per standing rules, so it stands.

**Rule for next session: `git add` explicit paths when commits are meant to be separate.** `-A`
is fine for a fix, wrong when a component is sitting uncommitted in the same tree.

### NEW API LANDMINES (Components 4 + 5) — 4 red runs

- **`ImageVector.Builder.path` is a top-level EXTENSION** (`ImageVector.kt:640`), not a member, so
  it needs an explicit import. Ten of eleven errors in one run were `moveTo`/`lineTo` cascading from
  that one missing import. Second time a composable-extension bit this session — the fix was to copy
  the imports off the file that already worked.
- **`Superellipse.path()` takes and returns `android.graphics.Path`, NOT the Compose `Path`.**
  `GlassPane.kt:13` imports the Android Path for exactly this reason. `DrawScope.drawPath` needs the
  Compose one, so the two cannot meet. Fixed by building the track from `Superellipse.points()`
  (`:41`) — the same primitive `path()` assembles from, with a Compose Path.
  **I looked for `asComposePath()` as the tidier bridge and could not find it** in the published
  `ui-android` or `ui-unit` sources. Shipped against the primitive I had actually read instead of
  an API I could not confirm.
- **`kotlin.math.cos`/`sin` return `Double`; `PathBuilder.moveTo`/`lineTo` take `Float`.**
- **kotest's `shouldBe` is `infix`** — it cannot be called with a dot. `x.shouldBe(true)` does not
  parse; only `x shouldBe true`. This is the second kotest precedence trap this project has hit.

### NEXT SESSION — pick the first consumer (not started, by ruling)

**Agent's recommendation: `RecentsController`**, ahead of `SettingsMainController`.

Reasoning:
- Recents needs **2 of the 3 largest already-built components** (segmented control + collapsing
  bar) — the most design-system surface per line migrated of either candidate.
- SettingsMain is the bigger surface overall (13 controllers) but its grouped-list consumers are
  the 13 `SettingsLegacyController` subclasses, which are **View-based** — the same trap
  `RecentsController` is in. Migrating either means Compose-ifying a View controller first.
- Recents' segmented control maps **directly** onto `RecentsViewType` (4 entries) with no new
  mapping layer, and its large title replaces a Material `TabLayout` strip.
- Smaller blast radius, and it exercises the whole system at once — the best proof that the design
  system works before betting 13 controllers on it.

### HARD STOPS STILL IN FORCE

No screen consumed these. No tab roots, MainActivity, shell. No `MaterialTheme.typography` or
`Constants.Size` migration. No @Inject sweep. PR #1 unmerged.

### WORKING RULES — changed this session, apply from here

- **Mechanical API fixes: fix forward, no report-first.** Report-first is reserved for
  architectural decisions (module placement, spec disagreement, dependency additions).
- **Anti-hallucination scope:** version numbers and dependency coordinates stay verify-first.
  Kotlin API surfaces you can grep in the Kt source are read-and-apply.
- **Batch independent components** into one session to save a CI run.
- **Skip /tdd** unless the component has non-obvious invariants.
- **Warnings-delta check** only when the diff plausibly moves it.
- **Skip device-verify ceremony** — file the item, do not block on it.
- **NEW: `git add` explicit paths when commits must stay separate.**
- **NEW: Robolectric revisit trigger** — "add it if any Phase 4 component costs 3+ red CI runs on
  the same file; otherwise defer to the Phase 4 exit device pass." Deferred by ruling: the 9 red
  runs this session were all compile errors, which Robolectric would not have caught.

### STILL TRUE — deferred, not resolved

- **No Compose UI test infrastructure.** Five `*Metrics` objects are load-bearing; it is the only
  reason any of this is testable. Components 1, 2 and 4 have render-only behaviour that no test
  executes (padding landing on screen, subtitle placement, `onScroll` framelessness, glyph weight).
- Device pass at Phase 4 exit covers rendering.

---

## PHASE 4 — FIRST CONSUMER: RecentsController (Session A) — CI GREEN

Commits `6fdd7ed58d` (RED) → `d28d528738` (GREEN).
Branch `refactor/phase1-async-source-api`. 2 files, +204/−35 then a 30-line fix.

### Approach — (c), revised

Kept `BaseCoroutineController` and `binding.root` untouched; added a `ComposeView`
(`cupertino_chrome`) as a child of the existing `recents_controller.xml` CoordinatorLayout,
placed **before** the `download_bottom_sheet` include so `BottomSheetBehavior` stays last in
child order. A base-class swap to `BaseComposeController` was impossible this session: 75 of
the 108 `binding.` call sites are the out-of-scope download bottom sheet.

### What shipped

- `shouldHideLegacyAppBar = true`. **`main_tabs` is NESTED inside `ExpandedAppBarLayout`**
  (`main_activity.xml:217`, under the `app_bar` opened at line 32) — *not* a sibling as I first
  believed and wrote in a comment. Hiding the app bar therefore hides the tab strip with it.
- The tab-adding block in `onChangeStarted` is deleted; tabs are still cleared and
  `showTabBar(false)` because `tabs_frame_layout` is `gone` by default but
  `MainActivity.showTabBar(true)` can reveal it.
- `SegmentedControl` maps straight onto `RecentsViewType.entries` — no mapping layer, as predicted.
- `chromeViewType by mutableStateOf` shadows `presenter.viewType` (a plain `var` ⇒ would never
  recompose). `syncChromeFromPresenter()` in `tempJumpTo` and `onChangeStarted` covers
  back-handler jumps and item taps.
- `LargeTitleBar` fed by a new `RecyclerView.OnScrollListener`. State holder published via
  `SideEffect`, not by assigning a field in the composable body.

### Three corrections I made mid-session (from wrong first reads)

1. **`tabs_frame_layout` is nested in the app bar, not a sibling.** Verified by reading
   `main_activity.xml:210-237`; the stale claim was already in a code comment and is now fixed.
2. **`controller_container` is `match_parent` constrained to the parent top** (`main_activity.xml:11-19`),
   and the app bar merely **overlays** it. So `isVisible = false` (INVISIBLE, `BaseController.kt:147`)
   leaves **no dead layout band** — no need to GONE it. The chrome therefore owns the top inset:
   `cupertinoChrome.updatePaddingRelative(top = systemInsets.top)`.
3. **`headerHeight` is write-only** — assigned at one site, read nowhere, before and after my
   change. Kept the assignment (with a comment saying so) rather than deleting pre-existing
   state inside a chrome commit.

### GENUINE DESIGN GAP — Component 2 velocity cannot be driven from a View list

`LargeTitleBarState.onSettle(v)` wants fling velocity in px/s. Its source,
`velocityCapture()`, is a `NestedScrollConnection` — Compose scrollables only.
`android.view.VelocityTracker` cannot substitute: **its only `addMovement` overload takes a
`MotionEvent`** (verified with `javap` on `android.jar` API 36 — there is no
`addMovement(float, float, long)`) and `RecyclerView.onScrolled` supplies no event.
Shipped: `onSettle()` with no argument ⇒ `pendingFlingVelocity` stays 0f ⇒ the settle spring
still runs (overscroll, short list) but without a flick-proportional start.
**Not a component change** — a caller adaptation plus a filed gap. Impact: every View-based
consumer (Library, Settings) hits this wall; Compose consumers do not, and session B gets
`velocityCapture()` free via `Modifier.nestedScroll` on the LazyColumn.

### API landmine #8

- **`android.view.VelocityTracker`'s constructor is package-private** — the factory is
  `VelocityTracker.obtain()`. Already used correctly in `MainActivity.kt:238` and
  `FullCoverDialog.kt:63`; **I did not check and got it wrong.** And there is no
  float-triple `addMovement`, so the tracker is unusable from a scroll listener at all.
- Local oracle found: `/data/data/com.termux/files/home/android-sdk/platforms/android-36/android.jar`.
  **Use `javap -classpath` against it for framework API checks** — it is fast, local, and exact.

### CI arc

`6fdd7ed58d` RED, **2 errors, both mine**:
`RecentsController.kt:224` package-private `VelocityTracker()`; `:296` `addMovement(dy.toFloat())`
type-mismatched to the `MotionEvent` overload. Fixed forward per the mechanical-error rule.

### Gates on final head `d28d528738`

| gate | value |
|---|---|
| Build Debug APK / Build app / Lint | 113933835841 / 113933836004 / 113933836195 — all success |
| `w:` warnings | **1052 → 1052, delta 0** |
| `e:` errors / FAILED | 0 / 0 |
| diff scope | 2 files only; **neither in either ledger** (0 matches) ⇒ bridge 22/35 and Size 11/40 provably unmoved |
| material3 imports in components | 0 (untouched) |

### Not done — filed for device pass / session B

- **Content does NOT scroll beneath the bar.** The recycler is padded by the full chrome height
  so nothing hides under the segmented control — the safe choice — which means the bar's opacity
  and hairline animate over an empty band rather than over content. Expressing the iOS overlap
  needs the list to be a Compose `LazyColumn`.
- **Segmented-control drag-to-select and the `CLOCK_TICK` haptic are untested** (needs a device).
- Large title is `MR.strings.recents`, matching the old `getTitle()` at `:163` exactly.
- Bottom sheet collapsed height now tracks the chrome (44+8+32 = 84dp) rather than the app bar.
- **Device verify is the gate. CI green is insufficient.** Do not start session B without it.

---

## DEVICE VERIFY 1 (post-session-A) — FAILED. 2 bugs + 1 scope question. NOTHING FIXED.

### Bug 1 — Recents renders title + search bar, then nothing. Diagnosis INCOMPLETE.

**DISPROVEN (each checked against source):**
1. SegmentedControl is NOT missing — `RecentsChrome` emits `Column { LargeTitleBar; SegmentedControl }`.
2. There is NO `when` on RecentsViewType anywhere in the chrome.
3. There is NO `AndroidView` to drop — by design the RecyclerView stayed a View sibling in
   `recents_controller.xml`. Still wired at `onViewCreated` (`binding.recycler.adapter = adapter`).
4. ComposeView is `wrap_content` + `layout_gravity="top"` — not 0dp, not match_parent.
5. Layout variants: only ONE `recents_controller.xml`, no `layout-*` variant ⇒
   `binding.cupertinoChrome` is non-null, not a nullable ViewBinding field.
6. Neither new call can throw: `LocalCupertinoColors = staticCompositionLocalOf { LightCupertinoColors }`
   has a default; `DisposeOnViewTreeLifecycleDestroyed` DEFERS via `OnAttachStateChangeListener`
   when unattached (`ViewCompositionStrategy.android.kt:152-172`) — it does NOT `require` attachment
   as I assumed. Sheet is not screen-covering (`sheet_layout` is `wrap_content`).

**PROVEN BUG (mine): `shouldHideLegacyAppBar` is defeated by the controller's own code.**
`RecentsController.updateTitleAndMenu()` does `activityBinding?.appBar?.isInvisible = showingDownloads`
(`:625`); with `showingDownloads == false` that RE-SHOWS the app bar, overriding
`shouldHideLegacyAppBar = true`. Called from `onChangeEnded` on every entry and from `setViewType`.
Also `:406-408` and `:485` manipulate/alpha the app bar on scroll.
**⇒ What the user saw as "large title Recents" + "Search updates & history" was the LEGACY app
bar, not my chrome.** Z-order: `controller_container` at `main_activity.xml:11`, `app_bar` at
`:32` ⇒ the app bar draws OVER everything the controller renders, chrome included.

**NOT PROVEN: why the list is empty.** Nothing in my diff explains it statically. Needs device
evidence (logcat, or: does the empty-state text show? is the list scrollable?).
Also noted: `viewScope.launchUI` at `:416` calls `recycler.updatePaddingRelative(bottom=…)`
AFTER my insets block, resetting top padding to 0 — pre-existing, now a real interaction.

### Bug 2 — MangaButton "Start reading" crash. PRE-EXISTING, not Phase 4.

- `start_reading_button` is a `com.google.android.material.button.MaterialButton` in all 3 layout
  variants. `MangaHeaderHolder.kt:93` sets its `transitionName`;
  `ReaderActivity.kt:300-301` installs `MaterialContainerTransformSharedElementCallback` +
  `sharedElementEnterTransition`. Both commits are **2022-04-24** (`c3275e63c4`, `2f16e01513`).
  **Zero commits since Phase 3 touched MangaHeaderHolder / styles.xml / themes.xml.**
- NO `android:background` on the button in any variant; none in `MangaHeaderHolder`;
  `themes.xml:56` `materialButtonStyle` → `Widget.Tachiyomi.Button.Primary` sets
  `backgroundTint`, NOT `android:background`. `values-v31/themes.xml:13` `background` is the
  window attr, unrelated.
- **Bytecode-verified** against `material-1.14.0-alpha09.aar`: `getShapeAppearanceModel()` throws
  gated SOLELY on `isUsingOriginalBackground()`, BEFORE reading any shape.
  ⇒ **Fix option (d) `app:shapeAppearance` CANNOT work** — the getter never reaches the shape.
  Same gate on `setShapeAppearance`.
- `setBackgroundOverwritten()` has exactly ONE caller: `MaterialButtonHelper.loadFromAttributes`.
  So the flag is set at INFLATION from XML attrs, never by `MangaHeaderHolder.kt:614`'s runtime
  `backgroundTintList`. ⇒ **(a) "remove the custom background" has nothing to remove in our code.**
- `isUsingOriginalBackground()` also returns false when `materialButtonHelper == null` — same
  misleading message. So "we never set a background" does NOT exonerate the app.

### Scope — NO VIOLATION

Both session A commits touch exactly 2 files: `RecentsController.kt`, `recents_controller.xml`.
`MangaDetailsController.kt` untouched. The only `Theme.kt` change in the token commit
(`24f0c4150f`) was a comment + `@Suppress("DEPRECATION")` — no visual effect. Typography bridge
frozen at caption1 17 / caption2 15. ⇒ MangaDetails' rounded top bar is PRE-EXISTING
(`ExpandedAppBarLayout` big-title), not a Cupertino component.

### Tooling win — local oracle found

- `~/android-sdk/platforms/android-36/android.jar` → `javap -classpath` for framework API.
- `~/.gradle/caches/.../material-1.14.0-alpha09.aar` → unzip `classes.jar`, `javap -p -c` to
  prove library *behaviour*, not just signatures. Found the real trigger this way.

---

## DARK MODE FIX — CI GREEN `75fb5c6a9d`

Files: `Theme.kt`, `SemanticColors.kt` (KDoc), `RecentsController.kt`. +37/−16.
CI 114132343277 / 114132343254 / 114132343432 all success. **w: 1052 → 1052, delta 0.**

### The bug was TWO layers, not one

Ruling assumed a missing `YokaiTheme` wrapper in `RecentsController.setContent`. That was true —
but **A alone would not have fixed it**. `LocalCupertinoColors` was **provided by nothing**;
its own KDoc at `SemanticColors.kt:133-141` said so and deferred the wiring. `YokaiTheme` never
provided it, so wrapping alone still resolved to `LightCupertinoColors`.

B (hardcoded light in components) was **disproven**: all 10 read sites use
`CupertinoColors.current` (GroupedSection ×6, LargeTitleBar, SegmentedControl, CupertinoSearchBar,
CupertinoSwitch), zero `MaterialTheme` reads, no hardcoded values.

**⇒ Lesson: when a CompositionLocal "isn't wired", check whether anything provides it before
assuming the provider is the only break. Two independent breaks can hide the same symptom.**

### Fix

`YokaiTheme` now provides `LocalCupertinoColors` from `isSystemInDarkTheme()` (already imported
at `Theme.kt:7`; only `CompositionLocalProvider` was added). Recents chrome wrapped in
`YokaiTheme` (`BaseComposeController.kt:30` is the standard). Accessors confirmed at
`SemanticColors.kt:157,160` — `CupertinoColors.light`/`.dark` exist; concrete objects at `:88,114`.
Chose the accessors to match the object components already read.

**Blast radius nil:** repo-wide, nothing outside `presentation/theme/components` reads the local
(only two KDoc mentions in `Typography.kt:39,179`). The "lands inert" constraint still holds.

### Dark mode mechanism — verified

**System only.** AppCompat DayNight via `values-night` + `values-night-v31`; **no
`AppCompatDelegate.setDefaultNightMode(...)` call exists anywhere**. `ContextExtensions.kt:355
appDelegateNightMode()` is a pure mapper that is never applied. `ThemeStep.kt:53` holds an
in-app nightMode preference but only feeds `AppCompatDelegate.MODE_NIGHT_*` constants for the
theme *picker*. So `isSystemInDarkTheme()` cannot desync — it is the correct signal.

### Other session findings (device, adb alias `emulator-5554` = the Realme 6)

- **415ms layout chain names `app:id/recycler` directly; `cupertino_chrome` appears nowhere.**
  Second cold start: 0 warnings ⇒ cold-start-only. Ruling accepted; (a)/(b)/(d) not pursued.
- **Library-tab discriminator is INVALID** — Library is empty, 0 rows inflated.
- **User has exactly 1 item in Recents** ⇒ the "inflating many rows" theory is dead; 415ms is
  first-inflate cost of `recent_manga_item`, pre-existing, unrelated to session A.
- **Bug 1 did not reproduce**: title, segmented control (`Grouped`/`All`/`History`/`Updates` at
  y=319) and list rows all present on `versionCode=162`. The original "renders empty" was a
  screenshot taken during the slow first layout.
- **Bug 2 (MaterialButton) did not change**; crash buffer confirms the stack. **Bug 2 CCE did not
  reproduce** — 1280 lines of cold-start logcat, zero `Bundle` lines. Still no key name.

---

## 2026-10-10 — Recents chrome: fixed height + Cupertino search + app-bar suppression

Commits `515b967772` (main) + `15b4ed9703` (JVM clash rename). **CI GREEN** on
`15b4ed9703`: Build Debug APK 114149743872 / Build app 114149743861 / Lint 114149743730,
all success. **w: 1052 → 1052, delta 0**, e: 0. Two files, +85/−19:
`RecentsController.kt`, `recents_controller.xml`. Awaiting device verify.

### Three of the ruling's premises were wrong; corrected before applying

1. **ComposeView is `wrap_content`, not `match_parent`** (`recents_controller.xml:46`).
2. **CoordinatorLayout does not arbitrate sibling heights.** No such mechanism — it lays each
   child out independently against parent constraints. The "whichever is measured first wins"
   theory was fictional.
3. **"View history" is not the legacy empty state.** It is
   `RecentMangaFooterHolder.kt:21` → `MR.strings.view_history`, bound when
   `recentsType == RecentMangaHeaderItem.CONTINUE_READING`. The class is misnamed "Footer";
   the constant lives on `RecentMangaHeaderItem`. **It is a row inside the RecyclerView** —
   the "continue reading" section header. So screenshots 1/2 showed a list that *was*
   laying out; `isEmpty()` was false so `EmptyView` correctly stayed hidden.

**Q2 answer from user: "only after tap."** Chapter 197 / Solo Leveling appears only after
interacting — the list holds a header row first and items arrive later. So part of the
"screens look different" impression was data-arrival, not layout.

### Real mechanism

`RecentsController.kt:377` (old) — `binding.cupertinoChrome.updatePaddingRelative(top = systemInsets.top)`.
**Padding on a `wrap_content` view changes its measured height**, and the inset callback is
async, so the chrome grew after first layout. That is the genuine instability.

Fix: pin the height in XML (**136dp**) so the view can no longer resize itself, and move the
inset with `updateLayoutParams { topMargin = inset }` instead. On a fixed-height view padding
would have been *wrong* — it shrinks the content area rather than positioning the block.

### Chrome height arithmetic (from source, not assumed)

```
LargeTitleBar          44   LargeTitleBarMetrics.kt:58
  + gap                 8   new — matches the existing segmented inset
CupertinoSearchBar     36   SearchBarMetrics.kt:24
  + gap                 8   SegmentedControl's own padding(vertical = 8.dp)
SegmentedControl       32   SegmentedControlMetrics.kt:20
  + gap                 8   SegmentedControl's own padding(vertical = 8.dp)
  = 136dp
```

Ruling's guess was 148dp — assumed a 56dp search bar and no top gap. **No component supplies
its own vertical rhythm**; all three are `Modifier.height(...)` with horizontal padding only.
Also: `SearchBarMetrics.HorizontalInset` (16dp) is applied *inside* the bar's own background
(`CupertinoSearchBar.kt:100-101`), so it is **not** a screen margin — the caller must add
`padding(horizontal = 16.dp)` or the capsule bleeds to the edges.

### Padding wipe — structural fix

`updatePaddingRelative` **replaces both axes**, so every bottom-only call silently reset the
list's top padding to 0. Wipe sites were **`:421` (`setBottomPadding`) and `:719`
(`setPadding`)** — *not* the `:415`/`:713` in my earlier report; those line numbers were stale.
All three writes now route through one helper:

```kotlin
private fun applyListPadding(bottom: Int) {
    binding.recycler.updatePaddingRelative(top = headerHeight, bottom = bottom)
}
```

`headerHeight` was pre-existing **write-only dead state** (assigned `:374`, never read). It is
now live and is the single source of truth for the list's top padding — no new field.
`:387` (`recentsEmptyView.updatePadding`) and `:648` (`dlRecycler`) untouched: different views.

### Legacy app bar

`shouldHideLegacyAppBar = true` at `:195` **was** honored (`BaseLegacyController.kt:29` →
`BaseController.kt:64` → `hideLegacyAppBar()`). It was defeated at **`:631`** (I first said
`:625` — wrong line) where `updateTitleAndMenu()` sets
`appBar.isInvisible = showingDownloads`, and `showingDownloads` is false in normal use.
Now `showingDownloads || shouldHideLegacyAppBar`.

**Only `:631` needed the guard.** `:412-414`, `:491`, `:770` touch `y`/`alpha`/`lockYPos`
on an `INVISIBLE` view, which does not draw — harmless.

### Search port — one pathway, not two

`query` → `by mutableStateOf("")` so the bar recomposes; a single `applyQuery(value)` writes
both the snapshot state and `presenter.query`. Call sites: the Cupertino bar's
`onQueryChange`/`onCancel`, and the legacy `SearchView` listener — all three do the same
`resetProgressItem(); refresh()`. Placeholder is `getSearchTitle()` (`:167-175`), the same
per-view-type accessor the toolbar used. `cancelLabel = stringResource(MR.strings.cancel)`
(verified present, used in Compose at `DialogHostState.kt:88`).

**LANDMINE:** a blanket `sed` for the rename also rewrote `searchView?.setQuery(query, true)`
at `:1151` — `SearchView`'s own two-arg framework API. Caught and reverted. Always re-grep
after a global rename.

**LANDMINE:** `var query by mutableStateOf("")` generates a JVM setter named `setQuery(String)`,
so a hand-written `fun setQuery(String)` is a **platform declaration clash**. First CI run
was RED on exactly that (`:159:13` / `:166:13`). Renamed to `applyQuery`.

### `alwaysExpanded()` — my report was WRONG, change reverted

I claimed `:1075 alwaysExpanded()` opened the legacy `SearchView` and needed a
`shouldHideLegacyAppBar` guard. **It has nothing to do with search** — it is
`RecentMangaAdapter.kt:111`, about **grouped-section expansion**, called at `:961` and `:970`.
The guard would have made Recents sections collapsible — a list behaviour change I invented.
Reverted before commit. Read the callers first.

### `contentScrolledUnder = true` at the call site

`SearchBarMetrics.kt:56` — `barAlpha(false) == 0f`. With no scroll-under yet the capsule would
be **permanently invisible**; iOS always draws it. Passing `true` is an argument, not an API
change. **Flip to `false` in session B** when the LazyColumn can actually pass content beneath.

### Residual

`expandSearch()` (`:1223`) still expands the now-hidden `SearchView` if MainActivity calls it
(search intent / keyboard shortcut). Visually a no-op. Not fixed — out of scope.

### Gates

- w: **1052 → 1052, delta 0**; e: 0.
- Bridge / `Constants.Size` ledgers: unchanged **by construction** — the diff touches only
  `RecentsController.kt` (which references neither symbol) and an XML file.
- No `material3` imports in components (the two `material3` hits are prose inside a
  `GroupedSection.kt` KDoc about exactly that).

---

## 2026-10-10 — B1a: item-keyed holder, position coupling removed

`555559f4fe` — **CI GREEN** (APK 114168573301 / app 114168573307 / lint 114168573414).
**w: 1052 → 1052, delta 0**, e: 0, BUILD SUCCESSFUL. 3 files, +~110/−~40.
No Compose, layout, presenter or chrome changes. Awaiting device verify.

### Why this session existed

B1b (RecyclerView → LazyColumn) was blocked by a finding, not by effort:
**`AndroidView` does not decouple `RecentMangaHolder`.** It had 14 references to
`flexibleAdapterPosition` / `bindingAdapterPosition`, and every `RecentsInterface` callback it
called was position-keyed. Those come from the **RecyclerView**, not the data — `NO_POSITION` the
moment the holder leaves one. Hosting the holder in an `AndroidView` would have wrapped dead
callbacks: right-string/no-op or "opens the wrong item". Hence B1a.

### What changed

`RecentMangaHolder` keeps `boundItem` (set in `bind`) and speaks in items. Seven
`RecentsInterface` callbacks take a `RecentMangaItem`; each controller impl lost its
`adapter.getItem(position)` lookup. **No controller state was position-keyed** —
`expandedSectionsMap` was already keyed `"${item.mch.manga} - $date"`.

**transitionName** `:104` was `"recents chapter $bindingAdapterPosition transition"` — changed on
every sort and every page. Now `"recents chapter ${manga.id ?: 0L}-${chapter.id ?: 0L} transition"`.
**chapter id is required, not optional**: `RecentMangaItem.equals` compares `chapter.id`, so the
same manga appears twice in Recents with different chapters, and two attached rows sharing a
shared-element name is undefined. `:425`'s sub-chapter name was already stable.

**The transitionName's consumer was not a string lookup.** Grepping for the literal found only
producers and nearly produced a false "dead code" conclusion. Real chain:
`RecentsController.kt:1017 openChapter(view.findViewById(R.id.main_view), …)` →
`ReaderActivity.kt:276-280 makeSceneTransitionAnimation(activity, sharedElement, sharedElement.transitionName)`
→ `ReaderActivity.kt:297`. **The View is passed by identity**; the string rides along in the
bundle. So "temporarily disable the transitionName" would have *broken* shared-element
transitions rather than silenced them.

**markAsRead** used `findViewHolderForAdapterPosition(position).chapterId` — reaching into the
view tree to ask a question about the data. The holder now publishes to
`RecentMangaAdapter.setActiveChapterId(mangaId, chapterId)` at touch time; the controller reads
`activeChapterIdFor`. `chapterId` is private as a result.

### LANDMINE — `onItemLongClick(position: Int)` is a LIBRARY interface

It is `override` in `RecentsController`, `LibraryController`, `MangaDetailsController`,
`BrowseSourceController` and `ExtensionBottomSheet`, with **no declaration anywhere in
`app/src/main`** — it comes from FlexibleAdapter's listener. It cannot be re-signatured. Kept as an
override delegating to a new `private fun onItemLongClick(item: RecentMangaItem)`.

### STILL POSITION-COUPLED — B1b's seam (deliberately not touched)

- `onItemClick(view, position)` `:1000` — **this is the shared-element entry point**;
  under Compose there is no such callback, so B1b must capture the view from the `AndroidView`
  factory.
- `downloadChapter(position)` `:912`, `downloadChapter(position, chapter)` `:933`,
  `startDownloadNow(position)` `:927` — all `GroupedDownloadInterface`/`DownloadInterface`.
- **`BaseChapterHolder.downloadOrRemoveMenu` (`ui/manga/chapter/BaseChapterHolder.kt:21-65`) has
  5 more `flexibleAdapterPosition` references and is inherited by `RecentMangaHolder`**
  (`RecentMangaHolder.kt:447` calls it). It is `internal` and shared with `DownloadHolder`,
  `ChapterHolder`, `RecentMangaFooterHolder` — fixing it touches **other tabs**, which B1a is
  forbidden to do. **Found during B1a, deferred to B1b.**

### B2 PRE-FLIGHT (carried forward)

- **`contentScrolledUnder = true` hardcoded** at the `RecentsController` call site. **Flip to
  `false` in B1b**, once chrome and list share a composition and content can actually pass
  beneath the bar.
- **Is selection live on Recents?** Nothing in `RecentsController` calls
  `select`/`deselect`/`clearSelection`, so `isSelected` looks dead — but that is an inference about
  FlexibleAdapter's own auto-select behaviour that has **not** been checked. B1a ships a real
  `isSelected(item)` lookup rather than deleting the branch on a guess. If selection is confirmed
  dead, delete the `Download.State.CHECKED` branch in the holder and pass `item.status` /
  `downloadInfo.status` directly. B2 rewrites the row anyway.
- `recents_empty_view` padding still uses `fullAppBarHeight` (`:449`) — dead space now the app bar
  is hidden. Cosmetic.
- `expandSearch()` (`:1223`) expands the hidden `SearchView`. Visually a no-op.

### Split agreed

B1a (this) · **B1b** Compose list, RecyclerView + SwipeRefreshLayout removed, sticky headers,
refresh state ported, gesture deferred · **B2** type-1 row in Compose, AndroidView must not outlive
B2. Full Recents rewrite is 4–6 sessions, not 2–3.

### Gates

- w: **1052 → 1052, delta 0**; e: 0; BUILD SUCCESSFUL.
- **No recents unit tests exist** (`find app/src/test -ipath '*recents*'` empty), so nothing
  asserted position-based behaviour. Verification is the device pass.
- No `material3` imports touched. `RecentMangaHolder` is same-package for its sibling imports, so
  removing the controller's last `RecentMangaHolder` reference created **no unused import**.

---

## B1b — CI GREEN 2026-10-10 · `a514b9b3fa` + fix `3e1e8707d9` · AWAITING DEVICE VERIFY

Jobs 114218929514 / 114218929309 / 114218929350 all success. **w: 1052 -> 1052 delta 0**, e: 0.
One red round (3 API-shape errors, all mechanical): `stickyHeader` is a `LazyListScope` member not an
import; `Modifier.nestedScroll` has no vararg overload; `CupertinoColors.current` is a `@Composable`
getter and `DrawScope` is not a composable scope.

### Shipped
- `recents_controller.xml` = `CoordinatorLayout` -> match_parent ComposeView + `dl_bottom_sheet` (last).
  RecyclerView, SwipeRefreshLayout, FrameLayout, EmptyView, LoadingIndicator all deleted.
- `RecentsScreen` in `RecentsController`: chrome Column + `Box{ LazyColumn; empty overlay; loading overlay }`,
  one composition. **`contentScrolledUnder = false` for real** — chrome is a sibling, not an overlay.
- `RecentsRow` sealed model + `buildRecentsRows`: `Section` / `DateSection` / `Footer` / `Manga`.
  Identity is a **string** (`recentsType` / `date.time` / manga+chapter id), never `equals()`.
- `CupertinoEmptyState.kt` + `CupertinoEmptyStateMetrics` (new component, `presentation/theme`).
- `CupertinoLoadingIndicator` — hand-rolled `Canvas` arc. **No material3 on this screen.**
- `stickyHeader` for both header kinds.

### The six ruled items
1. `scrollViewWith` gone -> `setUpInsets(view)`: one `ViewCompat.setOnApplyWindowInsetsListener` on
   `binding.root`. Bottom value is `bottomNav.height` (a View, not an inset) so it goes to the screen
   explicitly as `contentPadding`, never `Modifier.windowInsetsPadding`.
2. Section model derived from `item.header`. **BUG LOGGED:** `RecentMangaHeaderItem.equals:47-52`
   compares `recentsType == recentsType` in the `other is LibraryHeaderItem` branch and has **no
   `RecentMangaHeaderItem` branch** — every header is unequal to itself. B1c rewrites the class.
3. `setPreferenceFlows()` has exactly one call site, `RecentsController.kt:407`. The two RecyclerView
   calls are now `adapter.onDataInvalidated` (bumps `preferencesVersion`, re-read by
   `RecentMangaHolder.boundRebindToken` in the row's `update`) and `adapter.onOutlineChanged`
   (pushes `updateCards()` into `holderRegistry`).
4. Pagination: `LaunchedEffect` + `snapshotFlow` near-end, `PREFETCH_DISTANCE = 3` rows, old guard
   ported verbatim into `requestNextPage()`, latch `pagingRequestInFlight` cleared in `showLists`.
5. `holderRegistry: MutableMap<String, RecentMangaHolder>` on the controller, filled by the
   `AndroidView` factory, emptied by `DisposableEffect(key) { onDispose { remove } }`.
   Keyed by row key, **not** `item.id` — `item.id` is `chapter.id` via `Chapter by chapter`
   (`BaseChapterItem.kt:14`), nullable, collides on the footer row's blank chapter.
6. `mainRecycler` override deleted, comment added.

### REGRESSED DELIBERATELY — B1c restores
- **swipe-to-mark-read** (ItemTouchHelper died with the adapter)
- **pull-to-refresh**, and with it Recents' trigger for a library update
- `RecyclerView` item divider (`RecentMangaDivider`)
- `onActionStateChanged` now `= Unit` (existed only to pause refresh during a swipe)
- `expandSearch()` still expands the hidden legacy SearchView (pre-existing)

### Dead code B1c should reclaim
- `RecentMangaAdapter.isSelected` / `isSwipeEnabled` / `setEndlessScrollListener` consumers
- `loadNoMore()` / `resetProgressItem()` are now one-line latch flips
- `RecentMangaHolder.onLongClick` override vs the `AndroidView` long-press listener

### New API landmines
- `stickyHeader` is `LazyListScope.stickyHeader(key, contentType, content)` — **not importable**
- `Modifier.nestedScroll` has **no vararg** overload; chain two calls
- `CupertinoColors.current` is `@Composable get()` — unusable inside `Canvas {}` / `DrawScope`
- `Int.toDp()` is a `Density` member — `Modifier.padding(x = int.toDp())` does **not** compile
