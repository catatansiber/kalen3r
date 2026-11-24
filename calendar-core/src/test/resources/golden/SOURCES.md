# Golden data sources

Collection date: 2026-11-24. Status: INCOMPLETE. See "Blocked" below.

## Source 1: kalenderbali.com (primary, owner-permitted for date facts only)

- Pages: `https://kalenderbali.com/?bl=<month>&th=<year>`.
- `robots.txt` allows crawling (`Allow: /`, `Crawl-delay: 10`). No terms notice that forbids access was found. Only a footer copyright line.
- Politeness: 10 s between requests, one at a time.
- Collected per day (date facts only): wuku, saptawara, pancawara, triwara, caturwara, astawara, sangawara, dasawara, urip (`sapta+panca`), penanggal/pangelong type and number, sasih, Caka year, Purnama/Tilem and hari raya names. No descriptive text, wewukon, ingkel, dewasa or other tables were read or stored.
- Site spelling is kept verbatim: `Keliwon`, sasih `Kelima`, `Kenam`, `Kesanga`, `Kedasa`. `tithiNo` can be `2/3` on days where the site shows two numbers (seen on 2024-03-12).
- The page exposes tithi type and number and sasih per day. Caka year is printed once per month in the header. The collector uses the header and switches to the year named in the Nyepi entry ("Tahun Baru Saka 1946") on Nyepi day.

### Blocked: bot verification

After the first one or two page loads in a browser session, the site answers with a "One moment, please... Please wait while your request is being verified" interstitial. Early loads cleared by themselves. From 2024-07 on, the grid never appeared within 60 s in three attempts. I did not try to get around it (no fresh identities, stealth or challenge solving) and stopped.

Collected so far: raw month data for 2024-01 .. 2024-06 only (182 days). These days were checked against the 210-day cycle anchored at 2026-11-01 with zero mismatches (wuku, saptawara, pancawara, triwara). They are not committed as CSV because `verify.mjs` needs the full 2024-2035 range. Months 2024-07 .. 2035-12 (138 months) are missing, and so are `days-<year>.csv` for all years.

To finish: the owner can allow-list the collector or provide an export, then run `node scripts/golden/collect.mjs --cache <dir>` and `node scripts/golden/verify.mjs`.

## Source 2: independent sources (`golden2/`)

No single published Bali calendar covers 2024-2035. What was found:

| File | Source | Coverage |
|---|---|---|
| `golden2/moon-phases.csv` | US Naval Observatory moon phase API (`aa.usno.navy.mil/api/moon/phases/year`). Astronomical full/new moon instants (UTC) and WITA (UTC+8) date. Not a Bali calendar. | Full and new moon, 2024-2035 |
| `golden2/hari-raya.csv` | Wikipedia "Galungan" table | Galungan, Kuningan 2024-2030 |
| | Wikipedia "Nyepi" table | Nyepi 2024-2030 |
| | timeanddate.com Nyepi list | Nyepi 2024-2027 |
| | detik.com Bali "jadwal lengkap hari raya hindu 2026" | 2026: Purnama, Tilem, Nyepi, Ngembak Geni, Galungan, Kuningan, Saraswati, Pagerwesi, Siwaratri |
| | detikEdu article citing SE Gubernur Bali 16/2024 | 2025: Siwaratri, Saraswati, Pagerwesi, Nyepi, Galungan, Kuningan |
| | web-search summaries of SE Gubernur Bali / news (not fetched in full) | 2024: Siwaratri, Nyepi, Galungan, Kuningan, Saraswati, Pagerwesi. Medium confidence. |
| | bali.com and officeholidays.com via search summary (not fetched in full) | Nyepi 2027-2030. Medium confidence. |

### Coverage gaps

- Purnama/Tilem as published Bali dates: 2026 only. For other years only the astronomical USNO check exists.
- Nyepi: 2031-2035 not found.
- Galungan, Kuningan: 2031-2035 not found.
- Saraswati, Pagerwesi, Siwaratri: only 2024-2026 found. 2027-2035 not found.
- Official Parisada (PHDI) / Kemenag calendars could not be located as machine-readable multi-year documents.

## Disagreements

### Between the independent sources (kalenderbali.com not involved)

| Item | Value A | Value B |
|---|---|---|
| Nyepi 2027 | 2027-03-09 (Wikipedia, officeholidays via search summary) | 2027-03-08 (timeanddate, bali.com via search summary) |
| Nyepi 2028 | 2028-03-26 (Wikipedia, bali.com) | none |
| Galungan 2029 / 2030 | 2029-05-02, 2029-11-28, 2030-06-26 (Wikipedia) | 2029-05-01, 2029-11-07, 2030-06-25 (hindu-blog.com, seen only in a search summary; likely outdated) |
| Galungan 2028 | 2028-03-08, 2028-10-04 (Wikipedia) | 2028-03-06, 2028-10-02 (an unnamed travel guide in a search summary) |
| Galungan 2027 second cycle | 2027-08-11 (Wikipedia, Honeycombers) | "9 to 21 August" in one travel guide |
| Pagerwesi 2027 | 2027-06-02 | 2027-06-28 (one guide; cannot fit the 210-day cycle with Saraswati 2027-05-29) |

None of these are resolved. The Galungan 2028-2030 differences look like errors in the weaker sources, but that is a judgement, not a check.

### Between kalenderbali.com and the independent sources

Only 2024-01 .. 2024-06 can be compared:

- Galungan 2024-02-28, Kuningan 2024-03-09, Nyepi 2024-03-11, Siwaratri 2024-01-09: all match the published lists.
- Purnama/Tilem vs USNO astronomical instants (WITA date), 12 pairs in 2024-01 .. 2024-06. The site day equals the astronomical day in 4 pairs (Purnama 02-24, Tilem 03-10, Tilem 05-08, Tilem 06-06). It is one day earlier in 8 pairs: Tilem 01-10 (astro 01-11), Purnama 01-25 (01-26), Tilem 02-09 (02-10), Purnama 03-24 (03-25), Tilem 04-08 (04-09), Purnama 04-23 (04-24), Purnama 05-22 (05-23), Purnama 06-21 (06-22). Not resolved. One-day offsets are plausible for a tithi rule that is evaluated at a fixed time of day, but that is not verified. `compare.mjs` lists every offset once the full data exists.
- Full comparison for 2024-07 .. 2035 is not possible until the data is collected.

Gaps in the site data itself: none found so far for tithi or sasih (both are on the page).
