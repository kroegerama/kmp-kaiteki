# KMP Kaiteki

A set of helper classes for modern Kotlin multiplatform projects.

![KMP Targets](https://img.shields.io/badge/kmp%20targets-JVM%20Android%20iOS-blue?style=flat)
![API Level](https://img.shields.io/badge/min%20sdk-API%2024-blue?style=flat)

![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/kroegerama/kmp-kaiteki/gradle.yml?style=flat)
[![Maven Central](https://img.shields.io/maven-central/v/com.kroegerama.kmp.kaiteki/kaiteki-core?style=flat)](https://central.sonatype.com/search?namespace=com.kroegerama.kmp.kaiteki)

## Usage

```toml
[versions]
kaiteki = "x.y.z"

[libraries]
kaiteki-core = { module = "com.kroegerama.kmp.kaiteki:kaiteki-core", version.ref = "kaiteki" }
kaiteki-compose = { module = "com.kroegerama.kmp.kaiteki:kaiteki-compose", version.ref = "kaiteki" }
kaiteki-paging = { module = "com.kroegerama.kmp.kaiteki:kaiteki-paging", version.ref = "kaiteki" }
kaiteki-camera = { module = "com.kroegerama.kmp.kaiteki:kaiteki-camera", version.ref = "kaiteki" }
kaiteki-permissions = { module = "com.kroegerama.kmp.kaiteki:kaiteki-permissions", version.ref = "kaiteki" }
```

```kotlin
dependencies {
  implementation(libs.kaiteki.core)
  implementation(libs.kaiteki.compose)
  implementation(libs.kaiteki.paging)
  implementation(libs.kaiteki.camera)
  implementation(libs.kaiteki.permissions)
}
```

## Modules

| Module                              | Description                                                                                                  |
|-------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **core**                            | Load-state & event flows, lifecycle collection, DataStore, locale-aware formatting, date/time, serialization |
| **compose**                         | Compose Multiplatform UI utilities, Material 3 helpers, navigation support                                   |
| **paging**                          | Paging integration with Arrow and ViewModel support                                                          |
| **camera**<br>_(experimental)_      | Camera integration with barcode scanning and text recognition                                                |
| **permissions**<br>_(experimental)_ | Multiplatform runtime permission handling for Compose                                                        |

### Core

**State & events**

- `LoadState<E, T>` - sealed class with `Idle`, `Loading`, `Success`, `Error` states, stale-data support, and Arrow `Either` integration
- `LoadStateFlow` - reactive wrapper around `LoadState` with refresh, parameter flows, and ViewModel scope support
- `EventFlow` - fire-and-forget event bus backed by `SharedFlow`
- `ConsumableEventFlow` - single-consumption event flow for one-shot effects (snackbar, navigation)

**Lifecycle & ViewModel**

- Lifecycle-aware flow collection: `observeFlow`, `observeWithLifecycle`, `observeMultipleFlows`
- `launchWithCreated` / `launchWithStateAtLeast` - run a coroutine once a lifecycle state is reached
- `SavedStateHandle.field()` - property delegate for ViewModel saved state (natively supported types; use AndroidX `saved()` for `@Serializable`
  types)
- `SavedStateHandle.stateField()` - like `field()`, but exposes a `MutableStateFlow` for observable saved state

**Platform & storage**

- `Platform.name`, `platformContext` - platform info and the shared application context (JVM: id detected from `kaiteki.applicationId`, jpackage
  launcher, main class or jar name; override via `PlatformContext.initialize(applicationId)`)
- `platformContext.dataDirectory` / `cacheDirectory` - okio `Path`s to the app's persistent and cache directories, resolved per OS on the JVM
- `Initializer` - marker interface for DI-driven startup logic
- DataStore extensions for `DataStore<Preferences>`: `get` / `set` operators, `flow`, `remove`

**Formatting, locale & date/time**

- `DecimalFormatter`, `LocalizedDateTimeFormatter` (incl. relative/"fancy" formatting), `Long.asHumanReadableBytes()`
- `PlatformLocale` with `Locales` constants, display names, `isoCountryCodeToFlag`, `unicodeFlag`
- `Month` / `DayOfWeek` / `YearMonth` `displayName`, `LocalDate/Time.now()`, `toMinuteOfDay()`, Julian days and `dayDistanceTo`
- `BlurHash.decode()` - decode BlurHash image placeholders
- Enum serial-name helpers: `enumValueOfSerialName`, `serialName()`

### Compose

**Modifiers**

- `Modifier.dashedBorder()` - dashed border with an optional animated "marching ants" effect
- `Modifier.checkerboard()` - checkerboard background to visualize transparency
- `Modifier.pressAndHold()` - repeats a click while pressed, accelerating over time
- `Modifier.keepScreenOn()` - keeps the screen awake (Android flag / iOS `idleTimerDisabled`)
- `Modifier.cacheSize()` - reuses a previously measured size, e.g. for moveable content
- `Modifier.blurHash()` - draws a BlurHash placeholder behind the content

**Components**

- Expressive Material 3 buttons in five sizes: `ButtonExtraSmall` to `ButtonExtraLarge`, likewise for `Outlined`, `Elevated`, `FilledTonal` and `Text`
  buttons, with optional start/end icons
- Icon buttons in the same sizes: `IconButton*`, `FilledIconButton*`, `FilledTonalIconButton*`, `OutlinedIconButton*` and the matching
  `*IconToggleButton*` variants
- `SegmentedListItemColumn` - column of `SegmentedListItem`s and `Card`s shaped for their position in the group
- `ExpressivePullToRefreshBox` - pull-to-refresh with the Expressive loading indicator
- `ResideLayout` + `rememberResideLayoutState()` - side-menu layout where the content pane slides aside with a 3D scale/tilt effect to reveal a menu
- `VerticalScrollbar` / `HorizontalScrollbar` - lightweight scrollbar thumbs for a `ScrollState`
- `BracketsOverlay` / `BracketsShape` - rounded corner brackets like a QR or document scanner frame
- `StyledSnackbarHost` + `SnackbarController` - send `SnackbarEvent`s from anywhere, e.g. a ViewModel, rendered with custom shape, colors and layout

**Graphics**

- `rememberBlurHashPainter()` / `rememberBlurHash()` - BlurHash placeholders as a `Painter`
- `rememberAnimatedShape()` - morphs between corner-based shapes; `ListItemShapes.rememberShapeForInteraction()` follows press, drag, focus and hover
- `Path.strokeToFill()` - converts a stroke centerline into its filled outline

**Text fields**

- `rememberValidatingTextFieldState()` / `rememberSimpleValidatingState()` - declarative validation with an error DSL (`raise`, `require`);
  `validate(...)` aggregates multiple `Validator`s
- `InputTransformation.decimalInput()` / `DecimalInputTransformation` - locale-aware decimal input
- `TextFieldState.string`, `textAsFlow()`, `trim()`

**Navigation 3**

- `rememberScaffoldSceneDecorator()` - wraps every scene in a `Scaffold` and shares one top app bar across destinations
- `rememberBottomSheetSceneStrategy()` / `rememberAlertDialogSceneStrategy()` - show entries in a modal bottom sheet or alert dialog

**Paging**

- `pagingHeaders` / `pagingFooters` - render refresh / prepend / append / empty states in `LazyList`, `LazyGrid` and `LazyStaggeredGrid`
- `lazyPagingItemsOfData()` - preview/mock `LazyPagingItems`

**Formatting & locale**

- `rememberDecimalFormatter()`, `LocalDecimalFormatter`
- `rememberLocalizedDateTimeFormatter()`, `LocalLocalizedDateTimeFormatter`, `formatFancyAsState()` for self-updating relative times
- `Locale.asPlatformLocale()`, `LocalPlatformLocale`

**Misc**

- `ConsumableState<T>` + `Consume { }` - channel-backed one-shot event consumption (e.g. snackbar, navigation)
- `rememberTintedVectorPainter()` - recolor a vector as a `Painter`
- `rememberCustomTabsUriHandler()` - open links in-app (Chrome Custom Tabs on Android, `SFSafariViewController` on iOS)
- `KaitekiIcon` - the Kaiteki logo as an `ImageVector`

### Paging

- `PagerHolder` - wrapper around `Pager` with `refresh`, `retry`, `cachedIn`, and reactive parameter support
- Ready-made `PagingSource` implementations based on Arrow `Either` error handling and duplicate detection
  (configurable `DuplicateStrategy`: invalidate on shifted data, or filter duplicates from non-deterministic backends):
    - `PageSizePagingSource` - page/size pagination
  - `OffsetLimitPagingSource` - offset/limit pagination
    - `ItemKeyedPagingSource` - cursor/item-keyed pagination
    - `ContinuationTokenPagingSource` - token-based pagination
    - `SinglePagePagingSource` - non-paginated single-page data
- `pagingDataOf()`, `emptyPagingSource()`, `emptyItemSnapshotList()` - static data for previews and tests

### Camera (experimental)

- `CameraView` - Compose camera preview (Android: CameraX, iOS: AVFoundation)
- `rememberCameraController()` - zoom, torch, focus control
- `CameraController.bindBarcodeAnalyzerFlow(formats)` - real-time barcode scanning with configurable formats
- `CameraController.bindTextAnalyzerFlow(minConfidence)` - live text recognition (OCR)
- `Flow<OCRResult>.stabilized()` - suppresses OCR flicker by merging results across frames
- `AnalysisRegion` - restricts barcode/OCR results to the part of the frame visible in the viewfinder, via
  `CameraView(analysisRegion = AnalysisRegion.VisibleArea(inset))`; default `FullFrame`
- `BarcodeResult` - includes the normalized bounding box of the detected code (`relativeX/Y/Width/Height`)
- `CameraController.analysisRegionRect` - the active analysis region in viewfinder coordinates, e.g. for drawing scan brackets
- Multiplatform API with platform-specific implementations (Android + iOS)

### Permissions (experimental)

- `rememberPermissionState(permission)` - returns a `PermissionState` for observing and requesting a runtime permission
- `PermissionStatus` - sealed interface (`Granted` / `Denied(shouldShowRationale)`)
- `PermissionState.launchPermissionRequest()` / `openSystemPreferences()` - request the permission or deep-link to app settings
- Multiplatform API with platform-specific implementations (Android + iOS)

## License

```
Copyright 2026 kroegerama

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
