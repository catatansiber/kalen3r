# Golden data scripts

Collect and check the golden calendar facts used to validate `:calendar-core`.
Output goes to `calendar-core/src/test/resources/golden/`.

| Script | Purpose |
|---|---|
| `collect.mjs` | Loads `https://kalenderbali.com/?bl=<m>&th=<y>` with Playwright and writes `days-<year>.csv`. Date facts only (wewaran names, wuku, tithi, sasih, Caka year, Purnama/Tilem and hari raya names). No descriptive text is stored. |
| `collect2.mjs` | Second source: USNO astronomical full/new moon times, written to `golden2/moon-phases.csv`. |
| `compare.mjs` | Prints every disagreement between `golden/` and `golden2/` and writes `golden2/moon-diff.csv`. |
| `verify.mjs` | Sanity checks over all CSV rows (contiguous dates, 210-day cycle, anchors). Exit code 1 on failure. |

## Running

Playwright with Chromium must be resolvable by `require('playwright')`.

    node scripts/golden/collect.mjs [--from 2024 --to 2035] [--month 2026-11] [--delay 10] [--cache <dir>]
    node scripts/golden/collect2.mjs
    node scripts/golden/verify.mjs
    node scripts/golden/compare.mjs

`--cache <dir>` stores the raw per-month extraction (whitelisted fields) so a rerun resumes
without new requests. `--month` writes a `*.partial.csv` for a quick check.

## Politeness

`robots.txt` of kalenderbali.com allows crawling with `Crawl-delay: 10`; the collector waits
10 s between requests (never below 2 s), one request at a time. The site shows a passive "One moment, please..." verification interstitial, and after a few
requests in one browser session it stopped clearing. The collector only waits passively; if the
grid does not appear within 60 s it stops (resume later with `--cache`). It never interacts with
a challenge and never tries to evade it (no fresh identities, no stealth).

## CSV columns

`date,wuku,saptawara,pancawara,triwara,caturwara,astawara,sangawara,dasawara,urip,tithiType,tithiNo,sasih,caka,flags`

Names use the site's spelling (for example `Keliwon`, sasih `Kelima`, `Kenam`, `Kesanga`).
`tithiNo` is verbatim and can be `a/b` on days where the site shows two numbers.
See `SOURCES.md` for sources, collection date, gaps and disagreements.
