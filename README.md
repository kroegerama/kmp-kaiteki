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

| Module                              | Description                                                                      |
|-------------------------------------|----------------------------------------------------------------------------------|
| **core**                            | Coroutine/Flow utilities, DataStore helpers, serialization, lifecycle extensions |
| **compose**                         | Compose Multiplatform UI utilities, Material 3 helpers, navigation support       |
| **paging**                          | Paging integration with Arrow and ViewModel support                              |
| **camera**<br>_(experimental)_      | Camera integration with barcode scanning and text recognition                    |
| **permissions**<br>_(experimental)_ | Multiplatform runtime permission handling for Compose                            |

### Core

- `LoadState<E, T>` - sealed class with `Idle`, `Loading`, `Success`, `Error` states, stale data support, and Arrow `Either` integration
- `LoadStateFlow` - reactive wrapper around `LoadState` with refresh, parameter flows, and ViewModel scope support
- `EventFlow` - fire-and-forget event bus backed by `SharedFlow`
- `ConsumableEventFlow` - single-consumption event flow for one-shot events
- `SavedStateHandle.field()` - property delegate for ViewModel saved state
- Lifecycle-aware flow collection: `observeFlow`, `observeMultipleFlows`
- DataStore extensions: `get`, `set`, `flow` operators for `DataStore<Preferences>`
- Localized formatting: `DecimalFormatter`, `LocalizedDateTimeFormatter`, `HumanReadableBytes`
- _... and more_

### Compose

- Expressive Material 3 button versions with sizes (Small / Medium / Large)
- `ConsumableState<T>` - channel-backed one-shot event consumption in Compose (e.g. snackbar, navigation)
- `TextFieldValidation` - declarative text field validation
- `DecimalInputTransformation` - locale-aware decimal input
- `DashedBorder`, `PressAndHold` modifiers
- `TintedVectorPainter` - recolor vector drawables
- `Scrollbars` - scrollbar indicators for lazy lists
- `CustomTabsUriHandler` - platform URI handler (Chrome Custom Tabs on Android)
- `lazyPagingItemsOfData` - preview helper for `LazyPagingItems`
- `pagingHeaders`, `pagingFooters` - helpers for `LazyList`, `LazyGrid`, `LazyStaggeredGrid` to support loading / error states with headers / footers
- `Modifier.keepScreenOn` - multiplatform version, supports Android (delegate to official implementation) and iOS (uses `idleTimerDisabled`)
- _... and more_

### Paging

- `PagerHolder` - wrapper around `Pager` with `refresh`, `retry`, `cachedIn`, and reactive parameter support
- Ready-made `PagingSource` implementations based on Arrow `Either` error handling and duplicate detection:
    - `PageSizePagingSource` - page/size pagination
    - `ItemKeyedPagingSource` - cursor/item-keyed pagination
    - `ContinuationTokenPagingSource` - token-based pagination
    - `SinglePagePagingSource` - non-paginated single-page data
- _... and more_

### Camera (experimental)

- `CameraView` - Compose camera preview (Android: CameraX, iOS: AVFoundation)
- `rememberCameraController()` - zoom, torch, focus control
- `rememberBarcodeExtension()` - real-time barcode scanning with configurable formats
- `rememberOcrExtension()` - live text recognition (OCR)
- Multiplatform API with platform-specific implementations (Android + iOS)

### Permissions (experimental)

- `rememberPermissionState(permission)` - Composable for requesting and observing runtime permissions
- `PermissionStatus` - sealed interface (`Granted` / `Denied` with rationale info)
- `openSystemPreferences()` - deep-link to app settings
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
