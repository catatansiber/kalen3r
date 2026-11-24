# Kalender Bali (kalen3r)

*English: an ad-free, offline Android Balinese calendar (Pawukon, Sasih, Purnama/Tilem, hari raya, Otonan/Odalan search, Tri Sandya and Rainan reminders, QRIS donation screen). Kotlin, minSdk 26, distributed as a signed APK from the owner's website. Status: code complete, not yet tested on a real device; see "Status & batas yang diketahui".*

Aplikasi Android kalender Bali tanpa iklan, tanpa pelacak, dan berjalan penuh offline (tanpa izin INTERNET). Paket: `id.kalen3r` (sementara, lihat `docs/OWNER-CHECKLIST.md`).

## Fitur

- Kalender bulanan: Wuku, Wewaran (Eka sampai Dasa), tahun Çaka, Sasih, Purnama, Tilem, Kajeng Kliwon, dan hari raya.
- Cari Otonan dari tanggal lahir, cari tanggal dari kombinasi wewaran, Odalan dari Pawukon dan dari Sasih.
- Alarm dan notifikasi: Tri Sandya (06:00, 12:00, 18:00, jam bisa diubah) dan Rainan terpilih, dijadwal ulang saat boot, ganti zona waktu, dan pembaruan aplikasi. Ada layar bantuan "Agar alarm tidak mati" per merek HP.
- Donasi lewat QRIS statis (gambar disediakan pemilik; tanpa Play Billing).
- Bahasa Indonesia.

## Susunan modul

| Modul | Isi |
|---|---|
| `:calendar-core` | Mesin kalender, Kotlin/JVM murni (hanya stdlib dan `java.time`), dites tanpa Android. |
| `:app` | Aplikasi Android: Jetpack Compose, AlarmManager, DataStore, layar Donasi. Hanya menampilkan dan menjadwalkan. |

Dokumen lain: `docs/superpowers/specs/2026-11-24-kalender-bali-design.md` (desain), `docs/superpowers/plans/` (rencana), `calendar-core/docs/lunar-report.md` (laporan hitungan bulan), `docs/OWNER-CHECKLIST.md` (tugas pemilik), `PRIVACY.md`, `THIRD_PARTY_NOTICES.md`.

## Build dan tes

Butuh JDK 17 dan Gradle. Untuk `:app` butuh Android SDK (platform 34): set `ANDROID_HOME`, atau tulis `sdk.dir=/jalur/ke/android-sdk` di `local.properties` (berkas itu tidak ikut git).

    gradle :calendar-core:test                                  # tes mesin kalender (tanpa SDK)
    ANDROID_HOME=~/android-sdk gradle :app:testDebugUnitTest :app:assembleDebug
    ANDROID_HOME=~/android-sdk gradle :app:assembleRelease      # tanpa kunci: APK unsigned

APK keluar di `app/build/outputs/apk/`. Cara membuat APK bertanda tangan ada di `docs/OWNER-CHECKLIST.md`.

## Cara kerja mesin kalender (ringkas)

1. Semua logika ada di `:calendar-core`; fasad `BaliCalendar` memberi `BaliDay` per tanggal.
2. Pawukon adalah aritmetika eksak: siklus 210 hari dari patokan 2026-11-01 = indeks 0 (Wuku Sinta, Redite, Paing). Tanpa koreksi, toleransi nol.
3. Wuku, saptawara, pancawara, triwara, sadwara dihitung dari indeks; caturwara, astawara, sangawara punya tabel aturan khusus; dasawara dari urip.
4. Otonan = tanggal lahir + 210 x k hari; pencarian balik memakai indeks yang sama.
5. Bagian bulan: tithi per hari ditetapkan dengan aturan *pengalantaka* (lompat tithi tiap 63 hari), dengan aturan elongasi rata-rata sebagai cadangan (`Lunar.TITHI_RULE`).
6. Fase bulan dihitung astronomis (algoritma Meeus) ke WITA dan dipakai sebagai pemeriksa, dites ke data USNO.
7. Sasih dan tahun Çaka diturunkan dari urutan bulan (termasuk nampih dan ngunalatri); Nyepi = penanggal 1 Kadasa.
8. `corrections.json` (resource `:calendar-core`) memuat koreksi per tanggal bila hasil hitungan beda dari data resmi; dihasilkan skrip `gradle :calendar-core:generateCorrections`, tidak ditulis tangan.
9. Rerainan (Galungan, Kuningan, Tumpek, Kajeng Kliwon, dan lain-lain) berbasis Pawukon (eksak) atau bulan (Purnama, Tilem, Nyepi, Siwaratri).
10. `BaliDay.verified = true` hanya untuk tanggal di dalam rentang data emas (`GOLDEN_COVERAGE_RANGES`, sama dengan `coverage.json`: 2024-01-01..2024-06-30 dan 2026-11-01..2026-11-30). Di luar itu `verified = false` dan UI menampilkan peringatan bahwa Purnama, Tilem, dan hari raya berbasis bulan belum dicek dengan kalender resmi. Nilai berbasis Pawukon (wewaran, urip, wuku, dan hari raya Pawukon) eksak di mana pun dan tidak ikut diperingatkan.

Detail: desain di `docs/superpowers/specs/`, laporan hitungan bulan di `calendar-core/docs/lunar-report.md`.

## Status & batas yang diketahui

Jujur: kode lengkap, tetapi belum siap diklaim "terverifikasi penuh".

- **Data emas sebagian (212 hari).** Hanya 2024-01..2024-06 dan 2026-11 yang ada (`calendar-core/src/test/resources/golden/COVERAGE.md`). Sisanya (2024-07 sampai 2035) tidak terkumpul karena verifikasi bot di kalenderbali.com tidak lolos dan sengaja tidak dilewati.
  - Pawukon: cocok pada setiap hari yang tercakup (212 hari) dan lolos tes sifat (siklus 210 hari). Kepercayaan tinggi.
  - Bagian bulan (tithi, sasih, Purnama/Tilem, hari raya berbasis bulan): terverifikasi hanya pada 2024-01..06 dan 2026-11, ditambah cek astronomi USNO dan tanggal di `golden2/hari-raya.csv`. Selebihnya adalah hitungan yang belum dicocokkan dengan kalender resmi.
- **Pilihan aturan tithi** (pengalantaka) berkepercayaan rendah-sedang sampai ada data emas 2024-07 ke atas; aturan cadangan beda sampai puluhan hari pada 2035 (lihat `lunar-report.md`).
- **`corrections.json` saat ini kosong** (`[]`): tidak ada baris koreksi yang dibutuhkan pada data yang ada. Tahun lain tanpa koreksi.
- **Label Purnama/Tilem saat tithi 15/30 terlewat** (sekitar 1-2 kali setahun) bersifat sementara: ditandai pada hari sebelum tithi yang terlewat. Belum ada data emas yang menguji.
- **`nyepi(2001)` melempar exception** di `Sasih.kt` (di luar jangkauan model). Fasad tidak memanggilnya; perlu ditindaklanjuti di mesin.
- **Nyepi 2027**: sumber independen tidak sepakat (8 atau 9 Maret); hitungan mesin belum dicocokkan dengan kalender resmi.
- **Nampih Desta** (pertama pada 2027, Çaka 1949) tidak diverifikasi oleh data apa pun di repo.
- **UI dan alarm belum pernah dijalankan di perangkat nyata** (sandbox tanpa emulator). Hanya tes unit dan build yang lolos. Perlu uji di HP:
  - tampilan semua layar (Kalender, Detail, Cari, Alarm, Donasi) di beberapa ukuran layar, mode gelap dan terang;
  - alarm Tri Sandya/Rainan bunyi saat layar mati, setelah reboot, setelah aplikasi di-swipe dari recents, setelah ganti zona waktu, dan tidak bunyi dua kali;
  - izin notifikasi (Android 13+) dan izin alarm tepat (Android 12);
  - layar bantuan per merek (Xiaomi, Oppo, Huawei, Asus) membuka pengaturan yang benar;
  - suara Tri Sandya (setelah mp3 ada) dan nada bawaan;
  - layar Donasi dengan gambar QRIS asli, tombol bagikan, dan pemindaian dari aplikasi pembayaran;
  - pemasangan APK bertanda tangan dan pembaruan di atas versi lama.
- R8/minify sengaja **mati** pada rilis v1 (aplikasi mencari resource berdasarkan nama; belum diuji di perangkat dengan penyusutan).
