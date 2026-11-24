# Golden coverage

Machine-readable list: `coverage.json`. Only these date ranges exist in `days-<year>.csv`:

| From | To | File |
|---|---|---|
| 2024-01-01 | 2024-06-30 | `days-2024.csv` |
| 2026-11-01 | 2026-11-30 | `days-2026.csv` |

`BaliDay.verified` is true only inside these ranges (`GOLDEN_COVERAGE_RANGES` in `BaliCalendar.kt`, kept equal to
`coverage.json` by a test). Adding a CSV means updating `coverage.json`, `GoldenLoader.DAY_FILES` and
`GOLDEN_COVERAGE_RANGES` together.

Everything else in 2024-2035 (138 months) is missing because kalenderbali.com's bot verification
stopped clearing during collection. See `SOURCES.md`. Tests must only compare dates inside these ranges.
