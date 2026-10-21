# Yokai UI map

App shell: Conductor controllers hosted by `MainActivity` (`app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt`, layout `app/src/main/res/layout/main_activity.xml`). Legacy screens are XML + ViewBinding/RecyclerView; Compose is embedded via `ComposeView` / `AbstractComposeView`. Shared Compose code lives in `app/src/main/java/yokai/presentation/` and `presentation/core/`.

## 1. UI toolkit per screen

| Screen | Toolkit | Entry points |
|---|---|---|
| Library | Mixed | Legacy XML default: `eu.kanade.tachiyomi.ui.library.LibraryController` + `res/layout/library_controller.xml`. Compose alternative: `eu.kanade.tachiyomi.ui.library.compose.LibraryComposeController` + `LibraryComposePresenter`, gated by `BasePreferences.composeLibrary()` (default `false`). Its content, `yokai/presentation/library/LibraryContent.kt`, is still a placeholder ("Blank"/"Manga" text). |
| Recents | Mostly XML | `eu.kanade.tachiyomi.ui.recents.RecentsController` + `res/layout/recents_controller.xml` and `recents_*` layouts. Compose only in `eu.kanade.tachiyomi.widget.EmptyView` (empty state) and some icons. |
| Browse | Mixed | `eu.kanade.tachiyomi.ui.source.BrowseController` + `res/layout/browse_controller.xml`; source grid cells are Compose (`BrowseSourceItem`, `BrowseSourceGridHolder`, `yokai/presentation/manga/components/`). Extension list is the XML `ExtensionBottomSheet` embedded in the same layout. |
| Extensions | Mixed | Rendered as Browse's bottom sheet: `eu.kanade.tachiyomi.ui.extension.ExtensionBottomSheet` + `res/layout/extensions_bottom_sheet.xml`, `ExtensionBottomPresenter`; tab switch in `BrowseController.updateSheetMenu()`. Extension details are XML (`eu.kanade.tachiyomi.ui.extension.details.ExtensionDetailsController`); the Compose `yokai/presentation/extension/ExtensionDetailsController.kt` is unused. Repo screen is Compose (`yokai/presentation/extension/repo/ExtensionRepoScreen.kt`). |
| Settings | Mixed | Main/General/Appearance/Library/Reader/Downloads/Browse/Sources/Security/Tracking are XML preferences (`eu.kanade.tachiyomi.ui.setting.SettingsLegacyController`, `ui/setting/controllers/Settings*Controller`). Advanced and Data are Compose (`SettingsComposeController` + Voyager → `yokai/presentation/settings/screen/SettingsAdvancedScreen.kt`, `SettingsDataScreen.kt`). About is Compose (`eu.kanade.tachiyomi.ui.more.AboutController`, `.../settings/screen/about/AboutScreen.kt`). |

Compose host base: `eu.kanade.tachiyomi.ui.base.controller.BaseComposeController` (wraps content in `YokaiTheme`). Legacy app bar: `main_activity.xml` + `MainActivity`; Compose bars: `yokai/presentation/Scaffold.kt` + `presentation/core/JayAppBar.kt`.

## 2. Theme, colors, shapes, typography

- Root themes: `app/src/main/res/values/themes.xml` — `Theme.Base` / `Theme.Tachiyomi`, parent `Theme.Material3.DayNight.NoActionBar`; named variants (`Theme.Tachiyomi.MidnightDusk`, `SapphireDusk`, `Lavender`, `Tako`, `Doki`, etc.).
- Colors: `values/colors.xml`, `values-night/colors.xml`; theme-specific colors in the same files. Custom attrs in `values/attrs.xml` / `values-night/attrs.xml`.
- Material You / dynamic color: `Theme.Tachiyomi.Monet` uses `@android:color/system_accent*` / `system_neutral*` in `values-v31/themes.xml` and `values-night-v31/themes.xml`. No Compose `dynamicColorScheme`; selection via `eu.kanade.tachiyomi.util.system.Themes` (`util/system/Themes.kt`), gated by `DynamicColors.isDynamicColorAvailable()` in `ui/setting/ThemePreference.kt`.
- AMOLED/dark: `ThemeOverlay.Tachiyomi.Amoled` in `values/themes.xml`, applied at runtime by `getThemeWithExtras()` in `util/system/ThemeUtil.kt`; toggle key `preferences.themeDarkAmoled()`. Night selection also in `ThemeUtil.setThemeByPref` / `isDarkMode`; applied by `ui/base/activity/BaseActivity.kt` and `BaseThemedActivity.kt`.
- Compose theme: `app/src/main/java/yokai/presentation/theme/Theme.kt` — `YokaiTheme` wraps `MaterialTheme` using Accompanist `createMdc3Theme` (reads the XML theme; no Compose-native color/shape/typography overrides). Spacing/size tokens: `yokai/presentation/theme/Constants.kt` (`Size`).
- Typography: `yokai/presentation/theme/Typography.kt` (only `Typography.header`; otherwise Material 3 defaults). XML text styles: `values/styles.xml` (`TextAppearance*`, `TextAppearance.FloatingTitle`, etc.).
- Shapes: mostly Material 3 defaults plus `shapeAppearanceCornerSmall` in `values/styles.xml`; XML card radius `@dimen/rounded_radius` = 12dp (`values/dimens.xml`). Compose radii are hardcoded (`RoundedCornerShape(12.dp)` in `yokai/presentation/manga/components/MangaCover.kt`, `CommonMangaItem.kt`); no central Compose `Shapes`.

## 3. Bottom navigation

- View: `res/layout/main_activity.xml`, `com.google.android.material.bottomnavigation.BottomNavigationView` `@id/bottom_nav`, `app:menu="@menu/bottom_navigation"`; `@id/bottom_view` handles the system nav inset.
- Items: `res/menu/bottom_navigation.xml` — only `nav_library`, `nav_recents`, `nav_browse`. Extensions and Settings are not tabs; Settings/About/More are reached from the overflow dialog.
- Wiring: `MainActivity.kt` `setOnItemSelectedListener` / tab switching (~lines 510–560), choosing `LibraryComposeController` vs `LibraryController`; inset handling and show/hide (~lines 490–640); animation in `ui/main/TabsAnimator.kt`.
- Styling: `Widget.Tachiyomi.BottomNavigationView` in `values/styles.xml`; selectors `res/color/bottom_nav_item_selector.xml`, `bottom_nav_text_selector.xml`, `ripple_nav.xml`.
- Overflow menu: `eu.kanade.tachiyomi.ui.more.OverflowDialog` + `res/layout/tachi_overflow_layout.xml`, opens `MainActivity.showSettings()` / `showAbout()`.

## 4. Bottom sheets, dialogs, popup menus

- Sheet base classes: `eu.kanade.tachiyomi.widget.E2EBottomSheetDialog` (edge-to-edge `BottomSheetDialog`, `widget/E2EBottomSheetDialog.kt`) and `eu.kanade.tachiyomi.widget.TabbedBottomSheet` (`TabbedBottomSheetDialog`).
- Sheets: `ui/base/MaterialMenuSheet.kt` (`bottom_menu_sheet.xml`), `ui/extension/ExtensionBottomSheet.kt`, `ui/download/DownloadBottomSheet.kt`, `ui/library/filter/FilterBottomSheet.kt`, `ui/library/filter/ExpandedFilterSheet.kt`, `ui/library/display/TabbedLibraryDisplaySheet.kt`, `ui/source/browse/SourceFilterSheet.kt`, `ui/category/addtolibrary/SetCategoriesSheet.kt`, `ui/recents/options/TabbedRecentsOptionsSheet.kt`, `ui/manga/track/TrackingBottomSheet.kt`, `ui/manga/chapter/ChaptersSortBottomSheet.kt`, `ui/migration/manga/design/MigrationBottomSheetDialog.kt`, `ui/reader/settings/TabbedReaderSettingsSheet.kt`.
- Dialogs: Conductor base `ui/base/controller/DialogController.kt`; helpers in `util/system/MaterialAlertDialogExtensions.kt`; `ui/category/ManageCategoryDialog.kt`, `ui/manga/EditMangaDialog.kt`, `ui/manga/FullCoverDialog.kt`, `widget/preference/TrackLogoutDialog.kt`. Compose dialogs: `yokai/presentation/settings/screen/about/AboutDialogs.kt`, `.../advanced/AdvancedSettingsDialogs.kt`, `.../data/AlertDialogs.kt`, `yokai/presentation/component/preference/widget/TriStateListDialog.kt`.
- Popup menus: `widget/MaterialSpinnerView.kt`, `ui/manga/chapter/BaseChapterHolder.kt`, `ui/manga/MangaDetailsController.kt`, `ui/download/DownloadHolder.kt`, `ui/migration/manga/process/MigrationProcessHolder.kt`, `ui/manga/track/TrackingBottomSheet.kt`. Compose `DropdownMenu` in `yokai/presentation/component/AppBar.kt`.

## 5. Loading indicator

- XML Material 3 `com.google.android.material.loadingindicator.LoadingIndicator`: `library_controller.xml`, `recents_controller.xml`, `browse_source_controller.xml`, `reader_activity.xml`, `source_progress_item.xml`. Bound in `LibraryController`, `RecentsController`, `ui/source/browse/ProgressItem.kt`, `ReaderActivity`.
- Compose: `yokai/presentation/component/CircularProgressIndicator.kt` (`CombinedCircularProgressIndicator`, used by `ui/reader/viewer/ReaderProgressIndicator.kt`); `yokai/presentation/component/LoadingButton.kt`; `androidx.compose.material3.LoadingIndicator` in `yokai/presentation/manga/components/CommonMangaItem.kt`; `CircularProgressIndicator` in `WebtoonTransitionHolder`, `PagerTransitionHolder`, `ExtensionRepoItem`.

## 6. SDK levels

`buildSrc/src/main/kotlin/AndroidConfig.kt`:

- `minSdk = 26`
- `targetSdk = 36`
- `compileSdk = 36`

Applied through `buildSrc/src/main/kotlin/yokai.android.application.gradle.kts` and `yokai/build/ProjectExtensions.kt`.

## Rules

- No new dependencies.
- No Gradle, AGP or Kotlin version changes.
- No refactors outside the files the task needs.
- Every change must compile.
- The app must run on API 30. Any API 31+ call needs a `Build.VERSION` check and a fallback.
- No live blur over scrolling lists.

## 7. Settings grouped-inset plan (report only)

Settings are rendered in two different ways:

- Legacy screens: `eu.kanade.tachiyomi.ui.setting.SettingsLegacyController` extends `PreferenceController`, builds a `PreferenceScreen` in `setupPreferenceScreen`, and renders it through `listView` (`RecyclerView`) with AndroidX Preference's `PreferenceGroupAdapter`. Rows use `res/layout/preference_material.xml`; category headers use `preference_category_material.xml`. `Tachiyomi.PreferenceTheme` (parent `PreferenceThemeOverlay`) currently only overrides `switchPreferenceCompatStyle`.
- Compose screens: Advanced/Data use `SettingsComposeController` -> `yokai/presentation/settings/screen/SettingsAdvancedScreen.kt` / `SettingsDataScreen.kt`; About uses `.../settings/screen/about/AboutScreen.kt`. These do not use preference XML and are outside an XML-only restyle.

Target look:

- Page background `ios_black`; each `PreferenceCategory` is a rounded 16dp `#1C1C1E` card.
- 16dp side insets; 17sp title (Inter regular/medium); 13sp summary in `ios_label_secondary`.
- 1dp `#38383A` separator inset to the title start (16dp inside the card); no separator after the last row in a category.
- Navigation rows get a small trailing chevron (`ic_chevron_right_24dp`, tinted `ios_label_tertiary`); switch/checkbox rows keep their widget and no chevron.

XML-only changes:

- `app/src/main/res/values/styles.xml`: extend `Tachiyomi.PreferenceTheme` with `preferenceStyle`, `preferenceCategoryStyle`, `switchPreferenceCompatStyle`, `checkBoxPreferenceStyle`, `dialogPreferenceStyle`, `editTextPreferenceStyle`, `dropdownPreferenceStyle`, `preferenceFragmentCompatStyle`, and `preferenceFragmentListStyle`. Add `Widget.Yokai.Preference`, `Widget.Yokai.PreferenceCategory`, `Widget.Yokai.PreferenceFragment`, `TextAppearance.Yokai.PreferenceTitle`, `TextAppearance.Yokai.PreferenceSummary`, and `TextAppearance.Yokai.PreferenceCategory`.
- `app/src/main/res/layout/preference_yokai.xml` + `preference_category_yokai.xml` (or app overrides of the library `preference_material.xml` / `preference_category_material.xml`): 44dp minimum row height, 16dp horizontal padding, 17sp/13sp text, transparent row background, category header style. Reference each from the matching style via `android:layout`.
- `app/src/main/res/drawable/preference_list_divider_ios.xml`: 1dp `#38383A` with a start inset; set as `android:divider` in `Widget.Yokai.PreferenceFragment`.
- `app/src/main/res/drawable/preference_card_bg.xml`: 16dp radius, `#1C1C1E` (used by the card decoration described below).
- `app/src/main/res/values-night/colors.xml` already contains the palette; no new color resource is needed.

Not XML-only in this codebase:

- True per-category card bounds (first/last rounded corners and 16dp gaps between categories) need a small `RecyclerView.ItemDecoration` in `SettingsLegacyController.onViewCreated` that draws a rounded rect behind each contiguous category range, or a custom `PreferenceGroupAdapter`.
- Suppressing the last divider in each category and suppressing dividers around category headers also needs that decoration/adapter.
- Chevrons only on navigation rows need a small `NavigationPreference` subclass or a custom `PreferenceViewHolder` binding; a generic layout would also put chevrons on switches.
- Main Settings has no `PreferenceCategory`; it should either be wrapped in one synthetic category or treated as a single card by the decoration.
- Compose Advanced/Data/About need separate Compose card/list work, not preference XML.

Bottom line: XML-only covers colors, type, row height, side padding, header styling, and the inset divider drawable. A fully faithful grouped-card look is not XML-only; budget a small Kotlin item decoration plus a navigation-row binding.

## Design decisions

- Palette (`app/src/main/res/values-night/colors.xml`): `ios_black #000000`, `ios_grouped #1C1C1E`, `ios_raised #2C2C2E`, `ios_raised2 #3A3A3C`, `ios_separator #38383A`, `ios_label #FFFFFF`, `ios_label_secondary #99EBEBF5`, `ios_label_tertiary #4DEBEBF5`, `ios_blue #0A84FF`, `ios_red #FF453A`.
- Type: Inter (`res/font/inter.xml`) with 400/500/600/700 weights. Use `@font/inter` plus `android:textFontWeight`, or `@font/inter_medium` / `inter_semibold`. Body 17sp, secondary 15/13sp, labels 13sp; no all-caps.
- Radii: modal sheets 32dp top only; popup/dialog/card 14dp (planned settings cards 16dp); chips, segmented control, and nav capsule are pill-shaped (radius = height / 2); drag handle 5dp tall with 2.5dp radius.
- Nav pill (`nav_glass_bg.xml`): keep the layer-list, no `<inset>`, 8dp vertical / 16dp horizontal item offsets. Outer rim gradient `#30FFFFFF -> #08FFFFFF`; inner gradient `#2A2A2D -> #1C1C1E`.
- Nav capsule (`NavGlassIndicator.kt`): width = selected item width - 8dp, minimum 76dp; height = nav height - 22dp (8dp pill inset + 3dp clearance each side). Fill vertical `#38FFFFFF -> #14FFFFFF`, 1dp `#1FFFFFFF` stroke. Spring damping 0.8 / stiffness 400; stretch width by `abs(velocity) / 80dp-per-sec`, clamped 0..14dp around the center. Snap on first layout, size changes, and `onResume`; `sync(animated)` reads `nav.selectedItemId` as the single source of truth, and all programmatic selections go through `MainActivity.selectTab()`. Press feedback: scale 0.92 over 120ms, back to 1.0 over 200ms with overshoot, listener returns false. Haptics: `CONFIRM` on API 30+, `VIRTUAL_KEY` below.
- Sheets: `sheet_ios_bg.xml` is `#1C1C1E` with 32dp top corners only, no elevation/tint; `Widget.Yokai.Sheet`; drag handle 36dp x 5dp, fully rounded, `ios_label_tertiary`.
- Chips: 34dp high, fully rounded; unchecked `#2C2C2E`, checked `#0A84FF`, white 15sp medium text, no stroke.
- Menus: `popup_menu_ios_bg.xml` is `#2C2C2E`, 14dp corners, 1dp `#14FFFFFF` border, 0 elevation; 44dp rows, 17sp regular white text, no dividers. Alert dialogs: `#2C2C2E`, 14dp corners, 17sp semibold title, `#0A84FF` 17sp buttons, 0.4 dim.
- Segmented control (Recents): 36dp high, `#1C1C1E` container, fully rounded; selected `#3A3A3C` pill with 2dp inset; white 13sp semibold selected label and `#99EBEBF5` 13sp medium unselected label; no ripple. Implemented with `tabTextAppearance` + `tabSelectedTextAppearance` (Material 1.14 exposes the selected appearance).

## Blocked

- Compile/device verification for this UI pass was not run. The workspace Rules forbid local Gradle/APK builds, and the task forbids pushing, so GitHub Actions could not be used. Resource XML was checked with `aapt2 compile`; Kotlin changes were reviewed statically but not compiled. No emulator or device testing is claimed.
