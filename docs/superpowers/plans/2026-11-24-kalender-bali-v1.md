# Kalender Bali v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ad-free Android Kalender Bali app (calendar, Otonan, Odalan, Tri Sandya/Rainan alarms, Play Billing donations) on a tested pure-Kotlin calendar engine.

**Architecture:** Gradle multi-module: `:calendar-core` (pure Kotlin/JVM, `java.time` only) holds all calendar logic, validated against golden data; `:app` (Compose, AlarmManager, Play Billing) only renders and schedules. Pawukon is exact arithmetic; lunar data is computed astronomically then fixed by a generated `corrections.json`.

**Tech Stack:** Kotlin, Gradle (Kotlin DSL), JUnit 5, Jetpack Compose, DataStore, AlarmManager, Google Play Billing, Node + Playwright (golden-data scripts only).

**Spec:** `docs/superpowers/specs/2026-11-24-kalender-bali-design.md`

## Global Constraints

- Package: `id.kalen3r` (placeholder until owner picks; one constant to change). minSdk 26.
- `:calendar-core` imports only the Kotlin stdlib and `java.time`. No Android, no network.
- All user-visible text is Indonesian, stored in string resources.
- No ads, no analytics, no tracking, no network calls other than Play Billing.
- Theme: teal `#0F5257`, gold `#E0A21B`, cream `#FBF6EC`, ink `#2B2118`. Touch targets ≥ 44dp. Mockup: https://claude.ai/artifact/EbjKwUZKj9UK357VZhcv1H
- Pawukon anchor: 2026-11-01 = index 0 = Wuku Sinta, Redite, Paing. Cycle 210 days. Pasaran order Paing, Pon, Wage, Kliwon, Umanis. Triwara Pasah, Beteng, Kajeng.
- Çaka year = Gregorian − 78 after Nyepi, − 79 before. Time zone WITA (UTC+8).
- Golden range 2024–2035. Outside it: `verified = false` and UI warning. Pawukon tolerance is zero.
- Do not copy text, tables, icons or names from other apps or sites. Golden data stores date facts only.
- Donation product IDs fixed: `donasi_10k`, `donasi_25k`, `donasi_50k`, `donasi_100k` (one-time, consumable).
- Alarm permission: `USE_EXACT_ALARM`, fallback `SCHEDULE_EXACT_ALARM`. Rolling window 60 days.
- Tri Sandya default times 06:00, 12:00, 18:00. Sound file `app/src/main/res/raw/tri_sandya.mp3` is supplied by the owner later; default notification sound until then.

## Review Focus

- Dates at the golden range edges (2024-01-01, 2035-12-31) and the far navigation edges (2000, 2100): no crash, `verified` correct.
- Leap day 2028-02-29 and year-end rollovers: wuku and Sasih stay continuous.
- Days with ngunalatri (skipped tithi) or nampih (repeated tithi): month grid shows one tithi label per day, never blank or duplicated wrongly.
- Device time zone not WITA: calendar dates still follow WITA (pure `LocalDate`), alarms fire at local clock time chosen by user.
- Device reboot, app update, or user clearing recent apps: alarms are rescheduled and nothing fires twice.
- Billing unavailable (no Play Services, offline): Donasi screen shows a clear message, rest of app unaffected.
- Invalid search input (pancawara that does not match wuku+saptawara, birth date before 1900): clear error, no crash.

---

### Task 1: Gradle scaffold and `:calendar-core` skeleton

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `.gitignore`, `calendar-core/build.gradle.kts`
- Create: `calendar-core/src/main/kotlin/id/kalen3r/core/Wewaran.kt` (enums), `calendar-core/src/test/kotlin/id/kalen3r/core/SmokeTest.kt`

**Interfaces:**
- Produces: `enum class Saptawara { REDITE, SOMA, ANGGARA, BUDA, WRASPATI, SUKRA, SANISCARA }`, `enum class Pancawara { PAING, PON, WAGE, KLIWON, UMANIS }`, `enum class Triwara { PASAH, BETENG, KAJENG }`. Gradle project path `:calendar-core`.

- [ ] **Step 1:** Write `SmokeTest.pancawaraHasFiveValues` asserting `Pancawara.values().size == 5` and `Saptawara.SANISCARA.ordinal == 6`.
- [ ] **Step 2:** Run `gradle :calendar-core:test`. Expected: FAIL (build or symbols missing).
- [ ] **Step 3:** Create the Gradle Kotlin/JVM module with JUnit 5 and the three enums.
- [ ] **Step 4:** Run `gradle :calendar-core:test`. Expected: PASS. If plugin downloads are blocked, read `/root/.ccr/README.md` and fix proxy settings; never disable TLS verification.
- [ ] **Step 5:** Commit `chore: scaffold gradle and calendar-core`.

### Task 2: Golden data collector

**Files:**
- Create: `scripts/golden/collect.mjs`, `scripts/golden/README.md`, `calendar-core/src/test/resources/golden/days-<year>.csv` (2024–2035), `calendar-core/src/test/resources/golden/SOURCES.md`

**Interfaces:**
- Produces: CSV columns `date,wuku,saptawara,pancawara,triwara,tithiType,tithiNo,sasih,caka,flags` with `date` as `YYYY-MM-DD`; `flags` is a `;`-separated list (`purnama`, `tilem`, `kajeng-kliwon`, `anggara-kasih`, hari raya names). A second file set `golden2/` holds the cross-check source in the same format.

- [ ] **Step 1:** Write `collect.mjs` that loads `https://kalenderbali.com/?bl=<m>&th=<y>` with Playwright, waits for the page, and extracts the facts above per day. Store dates and wewaran names only, no descriptive text. Check `robots`/terms notice on the site; stop and report to the owner if scraping is disallowed.
- [ ] **Step 2:** Run for 2026-11 and compare by eye with the site: 2026-11-01 must be Sinta / Redite / Paing / Pasah. Expected: matches.
- [ ] **Step 3:** Run for 2024–2035 with a 2 s delay between requests. Commit CSVs.
- [ ] **Step 4:** Find a second source (official Parisada/Kemenag calendar PDF or another public listing) for Purnama, Tilem, Nyepi, Galungan 2024–2035 and write it into `golden2/`. Write `SOURCES.md` listing both sources, collection date, and every disagreement between them. Report disagreements to the owner; do not resolve silently.
- [ ] **Step 5:** Commit `test: add golden calendar data`.

### Task 3: Pawukon engine

**Files:**
- Create: `calendar-core/src/main/kotlin/id/kalen3r/core/Pawukon.kt`, `calendar-core/src/test/kotlin/id/kalen3r/core/PawukonTest.kt`, `calendar-core/src/test/kotlin/id/kalen3r/core/GoldenLoader.kt`

**Interfaces:**
- Consumes: Task 1 enums; Task 2 CSVs.
- Produces: `data class PawukonDay(val index: Int, val wuku: Int, val saptawara: Saptawara, val pancawara: Pancawara, val triwara: Triwara, val caturwara: Int, val sadwara: Int, val astawara: Int, val sangawara: Int, val dasawara: Int, val urip: Int)` and `object Pawukon { val ANCHOR: LocalDate; fun of(date: LocalDate): PawukonDay; val WUKU_NAMES: List<String> }`.

- [ ] **Step 1:** Write `PawukonTest.anchorIsSintaRediteBPaing` (`of(2026-11-01)` is index 0, wuku 1, REDITE, PAING, PASAH) and `matchesGoldenEveryDay2024To2035` (for every golden row, wuku, saptawara, pancawara, triwara equal; failures list the first 20 dates).
- [ ] **Step 2:** Run `gradle :calendar-core:test --tests '*PawukonTest*'`. Expected: FAIL.
- [ ] **Step 3:** Implement with `index = Math.floorMod(ChronoUnit.DAYS.between(ANCHOR, date), 210)`. wuku = index/7+1, saptawara = index%7, pancawara = index%5, triwara = index%3, sadwara = index%6. Caturwara, astawara, sangawara have the traditional repeated-day rule near the start of the cycle; derive tables from golden rows where the golden CSV carries them, otherwise from published rules, and add golden columns in Task 2 if needed. Dasawara from urip sum.
- [ ] **Step 4:** Run the tests. Expected: PASS with zero mismatches.
- [ ] **Step 5:** Commit `feat: pawukon engine`.

### Task 4: Otonan and wewaran search

**Files:**
- Create: `calendar-core/src/main/kotlin/id/kalen3r/core/PawukonSearch.kt`, `.../PawukonSearchTest.kt`

**Interfaces:**
- Consumes: `Pawukon.of`.
- Produces: `fun otonan(birth: LocalDate, count: Int, from: LocalDate): List<LocalDate>`; `fun findByWewaran(wuku: Int, saptawara: Saptawara, pancawara: Pancawara, from: LocalDate, count: Int): List<LocalDate>` (throws `InvalidWewaranException` if pancawara does not match); `fun odalanByPawukon(wuku: Int, saptawara: Saptawara, pancawara: Pancawara, from: LocalDate, to: LocalDate): List<LocalDate>`.

- [ ] **Step 1:** Tests: `otonanStepsAre210Days`, `otonanHasSamePawukonIndexAsBirth`, `otonanIncludesBirthDateWhenFromIsBirth`, `findByWewaranRejectsMismatchedPancawara` (e.g. Sinta+Redite+Pon throws), `findByWewaranRoundTripsOtonan`, `odalanByPawukonBoundsInclusive`.
- [ ] **Step 2:** Run tests. Expected: FAIL.
- [ ] **Step 3:** Implement via the index of (wuku, saptawara) = `(wuku-1)*7 + saptawara.ordinal`; step 210 days.
- [ ] **Step 4:** Run tests. Expected: PASS. Birth before 1900 must still work (pure arithmetic).
- [ ] **Step 5:** Commit `feat: otonan and pawukon search`.

### Task 5: Lunar phases and tithi (raw)

**Files:**
- Create: `calendar-core/src/main/kotlin/id/kalen3r/core/Lunar.kt`, `.../LunarTest.kt`, `.../LunarGoldenReportTest.kt`

**Interfaces:**
- Produces: `object Lunar { fun newMoonUtc(k: Int): Instant; fun fullMoonUtc(k: Int): Instant; fun tithiOn(date: LocalDate): Tithi }`, `data class Tithi(val type: TithiType, val number: Int)`, `enum class TithiType { PENANGGAL, PANGELONG }`. Reference instant for a day is a tunable constant `TITHI_REFERENCE = LocalTime.of(6, 0)` in WITA.

- [ ] **Step 1:** Tests: `knownNewMoon2000Jan6` (Meeus: new moon 2000-01-06 18:14 UTC ±5 min), `fullMoonIsHalfCycleAfterNewMoon` (29.53 ± 0.5 days), and `LunarGoldenReportTest` which prints, per year, how many golden Purnama/Tilem days differ from raw output (report, not assertion).
- [ ] **Step 2:** Run tests. Expected: FAIL.
- [ ] **Step 3:** Implement Meeus ch. 49 phases with the main periodic terms; tithi = floor(elongation at day reference / 12°) + 1 mapped to penanggal 1–15 / pangelong 1–15.
- [ ] **Step 4:** Run tests, then tune `TITHI_REFERENCE` to minimise mismatches in the report. Record the final count per year in `calendar-core/docs/lunar-report.md`. Large mismatch counts mean the algorithm is wrong; fix it, do not patch.
- [ ] **Step 5:** Commit `feat: lunar phases and tithi`.

### Task 6: Sasih, Çaka, Nyepi

**Files:**
- Create: `calendar-core/src/main/kotlin/id/kalen3r/core/Sasih.kt`, `.../SasihTest.kt`

**Interfaces:**
- Consumes: `Lunar`.
- Produces: `enum class Sasih { KASA, KARO, KATIGA, KAPAT, KALIMA, KANEM, KAPITU, KAWOLU, KANAWA, KADASA, DESTA, SADA }`, `fun sasihOn(date: LocalDate): SasihInfo` (`data class SasihInfo(val sasih: Sasih, val isNampih: Boolean)`), `fun cakaYear(date: LocalDate): Int`, `fun nyepi(gregorianYear: Int): LocalDate`.

- [ ] **Step 1:** Tests: `cakaYear2026Nov1Is1948`, `cakaYearChangesAfterNyepi` (day before and day after `nyepi(2026)`), `nyepiMatchesGolden2024To2035`, `sasihMatchesGolden` (report mismatches as in Task 5).
- [ ] **Step 2:** Run tests. Expected: FAIL.
- [ ] **Step 3:** Derive Sasih from the ordered Tilem sequence; choose and document the nampih-sasih rule in a comment with its source; Nyepi = day after Tilem Kesanga.
- [ ] **Step 4:** Run tests. Expected: Çaka and Nyepi tests PASS; Sasih mismatches are reported and later fixed by Task 7.
- [ ] **Step 5:** Commit `feat: sasih, caka year, nyepi`.

### Task 7: Corrections

**Files:**
- Create: `calendar-core/src/main/resources/corrections.json`, `calendar-core/src/main/kotlin/id/kalen3r/core/Corrections.kt`, `scripts/golden/make-corrections.mjs` (or Kotlin test-scope tool), `.../CorrectionsTest.kt`

**Interfaces:**
- Produces: JSON rows `{"date":"YYYY-MM-DD","field":"tithi|sasih|purnama|tilem|rerainanAdd|rerainanRemove","value":"…","source":"…"}`; `object Corrections { fun apply(day: RawLunarDay): RawLunarDay }`. No `pawukon` field is allowed; loading such a row throws.

- [ ] **Step 1:** Tests: `loaderRejectsPawukonField`, `appliedEqualsGoldenForAllDays2024To2035` (tithi, sasih, purnama, tilem flags), `correctionCountPerYearIsBelowLimit` (limit 10 per year; if exceeded the test fails and says the engine needs fixing).
- [ ] **Step 2:** Run tests. Expected: FAIL.
- [ ] **Step 3:** Write the generator that diffs raw output vs golden and emits `corrections.json`, one row per differing day and field, with the source from `SOURCES.md`. Implement `Corrections.apply`.
- [ ] **Step 4:** Run tests. Expected: PASS.
- [ ] **Step 5:** Commit `feat: corrections table`.

### Task 8: Rerainan

**Files:**
- Create: `calendar-core/src/main/kotlin/id/kalen3r/core/Rerainan.kt`, `.../RerainanTest.kt`

**Interfaces:**
- Produces: `enum class RerainanKind { PURNAMA, TILEM, KAJENG_KLIWON, ANGGARA_KASIH, BUDA_CEMENG, TUMPEK, GALUNGAN_KUNINGAN, SARASWATI_PAGERWESI, NYEPI, SIWARATRI }`, `data class Rerainan(val date: LocalDate, val name: String, val kind: RerainanKind)`, `fun rerainanOn(date: LocalDate): List<Rerainan>`.

- [ ] **Step 1:** Tests: `galunganIsBudaKliwonDungulan`, `kuninganIs10DaysAfterGalungan`, `kajengKliwonHas15DayStep`, `pagerwesiIsBudaKliwonSinta`, `allGoldenFlagsMatch2024To2035` (every golden `flags` entry appears for that date and nothing extra).
- [ ] **Step 2:** Run tests. Expected: FAIL.
- [ ] **Step 3:** Implement Pawukon-based rules from the spec table, lunar-based rules from Tasks 5–7. Exact names and any extra rerainan come from golden flags.
- [ ] **Step 4:** Run tests. Expected: PASS.
- [ ] **Step 5:** Commit `feat: rerainan`.

### Task 9: `BaliCalendar` facade

**Files:**
- Create: `calendar-core/src/main/kotlin/id/kalen3r/core/BaliCalendar.kt`, `.../BaliCalendarTest.kt`

**Interfaces:**
- Consumes: Tasks 3–8.
- Produces: the `BaliCalendar` interface and `BaliDay` from the spec (including `verified: Boolean`), `fun month(year: Int, month: Int): List<BaliDay>`, `fun odalanBySasih(sasih: Sasih, tithi: Tithi, year: Int): List<LocalDate>`, `fun rerainan(from: LocalDate, to: LocalDate, kinds: Set<RerainanKind>): List<Rerainan>`.

- [ ] **Step 1:** Tests: `november2026Has30DaysAndStartsSunday`, `day2026Nov24IsPurnamaKajengKliwonAnggaraKasih`, `verifiedFalseOutsideGoldenRange` (2036-01-01, 2023-12-31), `verifiedTrueAtEdges` (2024-01-01, 2035-12-31), `extremeYearsDoNotThrow` (2000-01-01, 2100-12-31), `odalanBySasihReturnsOneOrTwoDatesPerYear` (two when nampih).
- [ ] **Step 2:** Run tests. Expected: FAIL.
- [ ] **Step 3:** Compose the pieces; apply corrections before building `BaliDay`.
- [ ] **Step 4:** Run `gradle :calendar-core:test`. Expected: all PASS.
- [ ] **Step 5:** Commit `feat: bali calendar facade`. Tag `engine-v1`.

### Task 10: Android scaffold, theme, navigation

**Files:**
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/id/kalen3r/MainActivity.kt`, `app/src/main/kotlin/id/kalen3r/ui/Theme.kt`, `app/src/main/kotlin/id/kalen3r/ui/AppNav.kt`, `app/src/main/res/values/strings.xml`
- Modify: `settings.gradle.kts` (include `:app`)

**Interfaces:**
- Consumes: `:calendar-core` as a project dependency.
- Produces: routes `kalender`, `detail/{date}`, `cari`, `alarm`, `donasi`; `KalenTheme { }` composable; bottom bar with 4 tabs (Kalender, Cari, Alarm, Donasi).

- [ ] **Step 1:** Check for an Android SDK (`$ANDROID_HOME`, `sdkmanager`). Try to install command-line tools and platform 34 through the proxy. If it cannot be installed, write all `:app` code anyway, mark every `:app` verification step "owner builds", and list the exact build commands in `README.md`.
- [ ] **Step 2:** Write a Compose UI test (or JVM test of the nav graph) `appNavHasFourTabs`. Expected: FAIL.
- [ ] **Step 3:** Create the module, theme from Global Constraints, nav host, placeholder screens.
- [ ] **Step 4:** If an SDK exists: `gradle :app:assembleDebug` and the test; Expected PASS. Otherwise commit and flag for owner.
- [ ] **Step 5:** Commit `feat: android app scaffold`.

### Task 11: Month and Detail screens

**Files:**
- Create: `app/src/main/kotlin/id/kalen3r/ui/month/MonthScreen.kt`, `MonthViewModel.kt`, `ui/detail/DetailScreen.kt`, tests beside them.

**Interfaces:**
- Consumes: `BaliCalendar.month`, `BaliDay`.
- Produces: `MonthViewModel(calendar: BaliCalendar)` with `state: StateFlow<MonthUiState>`, `next()`, `previous()`. Year range clamped 2000–2100. `verified == false` shows a small notice on the screen.

- [ ] **Step 1:** Tests on the ViewModel: `startsOnCurrentMonth`, `previousAtJan2000IsNoOp`, `nextAtDec2100IsNoOp`, `unverifiedMonthSetsWarning`. Expected FAIL.
- [ ] **Step 2:** Implement the ViewModel, then the grid per mockup (day number, pasaran, marker label, Purnama gold, Tilem dark, Kajeng Kliwon teal) and the legend list "Bulan ini".
- [ ] **Step 3:** Implement Detail screen (wewaran grid, rerainan list, "Ingatkan saya pada hari ini" button leading to Task 13 for that date).
- [ ] **Step 4:** Run tests. Expected PASS (or owner builds).
- [ ] **Step 5:** Commit `feat: month and detail screens`.

### Task 12: Cari screen

**Files:**
- Create: `app/src/main/kotlin/id/kalen3r/ui/cari/CariScreen.kt`, `CariViewModel.kt`, tests.

**Interfaces:**
- Consumes: `otonan`, `findByWewaran`, `odalanByPawukon`, `odalanBySasih`.
- Produces: `CariViewModel` with three tabs (Otonan, Odalan Pawukon, Odalan Sasih) and `sealed interface CariResult { data class Dates(...); data class Error(val message: String) }`.

- [ ] **Step 1:** Tests: `otonanFromBirthReturnsNextFiveDates`, `mismatchedWewaranShowsError`, `birthBefore1900StillWorks`, `emptyInputShowsError`. Expected FAIL.
- [ ] **Step 2:** Implement ViewModel and screen per mockup; results list tappable to Detail.
- [ ] **Step 3:** Run tests. Expected PASS.
- [ ] **Step 4:** Commit `feat: cari screen`.

### Task 13: Alarms (Tri Sandya, Rainan) and help screen

**Files:**
- Create: `app/src/main/kotlin/id/kalen3r/alarm/AlarmScheduler.kt`, `AlarmReceiver.kt`, `BootReceiver.kt`, `AlarmSettings.kt` (DataStore), `ui/alarm/AlarmScreen.kt`, `ui/alarm/BatteryHelpScreen.kt`, tests.
- Modify: `AndroidManifest.xml` (permissions, receivers, notification channel).

**Interfaces:**
- Consumes: `BaliCalendar.rerainan`.
- Produces: `class AlarmPlanner(calendar: BaliCalendar) { fun plan(now: LocalDateTime, settings: AlarmSettingsData): List<PlannedAlarm> }`, `data class PlannedAlarm(val id: Int, val at: LocalDateTime, val title: String)`. Ids are stable (hash of kind + date) so rescheduling never duplicates. `AlarmScheduler.rescheduleAll()` runs on app open, boot, and after settings change.

- [ ] **Step 1:** Tests for `AlarmPlanner` (pure JVM): `planCovers60Days`, `triSandyaGivesThreePerDay`, `disabledKindProducesNothing`, `idsAreStableAcrossCalls`, `pastTimesAreSkipped`, `changingTimeChangesOnlyThatSlot`. Expected FAIL.
- [ ] **Step 2:** Implement `AlarmPlanner`. Run tests. Expected PASS.
- [ ] **Step 3:** Implement scheduler (`setExactAndAllowWhileIdle`), receivers, channel, permission flow (`canScheduleExactAlarms()` fallback), the Alarm screen per mockup, and the help screen with Indonesian text for Xiaomi, Oppo, Huawei, Asus written from scratch (autostart, lock in recents, battery whitelist).
- [ ] **Step 4:** Add sound choice setting: "Suara Tri Sandya" if `R.raw.tri_sandya` exists else default tone; the Tri Sandya option is hidden until the file exists.
- [ ] **Step 5:** Commit `feat: alarms and help`. Owner device test required before release.

### Task 14: Donasi (Play Billing)

**Files:**
- Create: `app/src/main/kotlin/id/kalen3r/billing/DonationBilling.kt`, `ui/donasi/DonasiScreen.kt`, tests.

**Interfaces:**
- Produces: `interface DonationBilling { suspend fun products(): List<DonationProduct>; suspend fun donate(activity: Activity, productId: String): DonationResult }`; `sealed interface DonationResult { Success, Cancelled, Unavailable, Error }`. A `FakeDonationBilling` for tests and previews.

- [ ] **Step 1:** Tests with the fake: `unavailableShowsMessageAndHidesButtons`, `successShowsThanks`, `purchaseIsConsumedSoItCanRepeat`, `cancelledShowsNothing`. Expected FAIL.
- [ ] **Step 2:** Implement with Play Billing Library (one-time products, consume after purchase). Screen per mockup; copy states donations are voluntary and all features stay open.
- [ ] **Step 3:** Run tests. Expected PASS.
- [ ] **Step 4:** Commit `feat: donations`. Real purchases can only be tested after upload to Play Console internal track.

### Task 15: Release prep and owner handoff

**Files:**
- Create/Modify: `README.md`, `docs/OWNER-CHECKLIST.md`, `app/src/main/res/` icons (own design), privacy text.

- [ ] **Step 1:** Write `OWNER-CHECKLIST.md`: pick package name, create Play Console app, add 4 donation products, upload `tri_sandya.mp3`, sign and upload to internal track, test alarms on a Xiaomi/Oppo phone, update golden data each year before December (`scripts/golden/README.md`).
- [ ] **Step 2:** Run the whole suite: `gradle :calendar-core:test` (and `:app:test` if SDK). Expected: PASS.
- [ ] **Step 3:** Grep the repo for banned items (ads SDKs, analytics, other apps' names or text): `grep -ri 'admob\|firebase\|analytics' .`. Expected: no hits.
- [ ] **Step 4:** Commit `docs: owner checklist and release notes`.
