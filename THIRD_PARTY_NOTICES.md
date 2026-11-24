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
