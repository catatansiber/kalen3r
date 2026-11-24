# Third-party notices

## peradnya/balinese-date-js-lib

- Upstream: https://github.com/peradnya/balinese-date-js-lib
- License: Apache License 2.0 (https://www.apache.org/licenses/LICENSE-2.0; upstream file `LICENSE.md`)

Used by `:calendar-core` (`Lunar.kt`, `Sasih.kt`) as a reference for these calendar facts and parameters only:

- The pengalantaka pivot: 2000-01-18 is penanggal 12 (upstream `src/internal/Pivot.ts`, `PIVOT_2000`).
- The 63-day ngunalatri skip: day count = pivot tithi + days since pivot + ceil(days / 63)
  (upstream `src/BalineseDate.ts`, `_F_CALC_SASIH_DAY` and `_F_CALC_SASIH_INFO`).
- The nampih sasih sets by Çaka year mod 19: 0, 6, 11 give nampih Desta, with no nampih Desta in Çaka 1925;
  3, 8, 14, 16 give nampih Sada; the nampih month follows the regular month
  (upstream `src/BalineseDate.ts`, `_F_CALC_SASIH_INFO`).

No upstream code or tables were copied. The Kotlin implementation was written independently and is checked
against this repository's own golden data.

## Libraries shipped inside the APK (Apache License 2.0)

The app is built with the following open-source libraries. Each is licensed under the Apache License 2.0
(https://www.apache.org/licenses/LICENSE-2.0) and is used unmodified as a dependency (not copied into this
repository). Versions are those declared in `app/build.gradle.kts`.

- AndroidX Core (`androidx.core`, pulled in transitively), AndroidX Activity Compose (`androidx.activity:activity-compose`),
  AndroidX Lifecycle (`lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`), AndroidX Navigation
  (`navigation-compose`) and AndroidX DataStore (`datastore-preferences`).
  Project: Android Open Source Project, https://developer.android.com/jetpack/androidx
  Licence: Apache License 2.0 (https://www.apache.org/licenses/LICENSE-2.0). Used unmodified as a dependency.
- Jetpack Compose (`androidx.compose.ui`, Compose BOM 2024.10.01), Material3 (`androidx.compose.material3`) and
  Material Icons Extended (`androidx.compose.material:material-icons-extended`).
  Project: Android Open Source Project, https://developer.android.com/jetpack/compose
  Licence: Apache License 2.0 (https://www.apache.org/licenses/LICENSE-2.0). Used unmodified as a dependency.
- Kotlin standard library and kotlinx.coroutines (pulled in by the Kotlin toolchain and the AndroidX libraries above).
  Project: JetBrains, https://kotlinlang.org and https://github.com/Kotlin/kotlinx.coroutines
  Licence: Apache License 2.0 (https://www.apache.org/licenses/LICENSE-2.0). Used unmodified as a dependency.

`:calendar-core` itself imports only the Kotlin standard library and `java.time`.

## Algorithms and test data (nothing from these is shipped as code or text)

- Jean Meeus, "Astronomical Algorithms" (2nd edition): the moon-phase and Moon/Sun longitude formulas
  (chapters 25, 47 and 49) are implemented independently in `Lunar.kt`. Only the published algorithms and
  formulas are used; no text or tables were copied from the book.
- U.S. Naval Observatory (USNO) moon-phase data: used only as reference data in test resources
  (`calendar-core/src/test/resources`) to check the astronomical computation. It is not part of the APK.
- Test-only dependencies (JUnit, kotlin-test) are used to run the unit tests and are not shipped in the APK.
