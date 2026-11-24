# Kalender Bali (kalen3r) — Desain Versi 1

Tanggal: 2026-11-24. Status: menunggu persetujuan pemilik (desain lengkap).

## Maksud dan alasan

Pemilik ingin aplikasi Android kalender Bali dengan fitur yang sama seperti
aplikasi "Kalender Bali" (com.alitmd.kalenderbali) di Play Store, tetapi:

- **tanpa iklan**, dan
- **ada fitur donasi** lewat Google Play Billing.

Pengguna: umat Hindu Bali yang memakai kalender untuk hari raya, Otonan,
Odalan, dan pengingat sembahyang. Aplikasi asli punya masalah alarm mati di
HP Oppo/Xiaomi/Huawei/Asus; itu kelemahan yang harus diperbaiki, bukan ditiru.

Referensi tampilan dan istilah: kalenderbali.com (terbuka dibaca).
m.kalenderbali.org memblokir akses otomatis dan tidak dipakai.

## Tujuan

1. Kalender bulanan dengan: Wuku, Wewaran (Eka sampai Dasa, termasuk Urip),
   tahun Çaka, Sasih, Purnama, Tilem, Kajeng Kliwon, dan hari raya
   (Galungan, Kuningan, Pagerwesi, Saraswati, Nyepi, Siwaratri, Tumpek,
   Anggara Kasih, Buda Cemeng, dan sejenisnya).
2. Cari **Otonan dari tanggal lahir**, dan **tanggal lahir/Otonan berikutnya
   dari kombinasi wewaran** (arah sebaliknya).
3. Cari **Odalan dari Pawukon** (wuku + saptawara + pancawara) dan **dari
   Sasih** (sasih + Purnama/Tilem/tanggal penanggal-pangelong).
4. **Alarm dan notifikasi**: Rainan terpilih dan Tri Sandya (06:00, 12:00,
   18:00, jam bisa diubah), andal walau layar mati dan HP di-reboot.
5. **Donasi** lewat Google Play Billing: beberapa nominal sekali bayar.
6. Berjalan **offline penuh**. Bahasa Indonesia.

## Bukan tujuan (versi 1)

- Dewasa Ayu, Ala Ayuning Dewasa, Menyusun Dewasa, Dauh Ayu, ramalan
  karakter. Itu versi 2 (butuh data sumber yang jelas izinnya).
- Piodalan per pura, iOS, akun pengguna, sinkron cloud, widget.

## Anti-tujuan (dianggap gagal walau jalan)

- Tanggal hari raya salah. Satu hari meleset = aplikasi tidak bisa dipercaya.
- Alarm Tri Sandya tidak bunyi di HP Oppo/Xiaomi tanpa penjelasan ke pengguna.
- Iklan, pelacak, atau pengumpulan data pribadi apa pun.
- Menyalin teks, tabel, ikon, nama, atau gambar dari aplikasi/situs lain.

## Batasan

- Platform: Android, Kotlin, minSdk 26. Alasan: alarm dan notifikasi paling
  andal di native. (Pilihan pemilik.)
- Donasi: Google Play Billing. Hanya bisa diuji penuh setelah aplikasi ada di
  Play Console (track internal). Itu dikerjakan pemilik.
- Hak cipta: rumus ditulis sendiri; data tes hanya berupa fakta tanggal.
- Sandbox kerja ini punya Java dan Gradle tetapi belum tentu Android SDK.
  Karena itu mesin kalender dipisah sebagai modul Kotlin/JVM murni yang bisa
  dites tanpa Android.

## Pendekatan yang dipertimbangkan

| | Untung | Rugi |
|---|---|---|
| **A. Kotlin native (dipilih pemilik)** | Alarm andal, ukuran kecil, Billing resmi | Hanya Android |
| B. Flutter | Bisa iOS | Alarm background lebih rewel |
| C. PWA | Cepat | Alarm tidak andal; gagal di fitur inti |

Dipilih A atas permintaan pemilik.

Untuk mesin kalender: (1) **Pawukon** dihitung aritmetika murni siklus 210
hari dari satu tanggal acuan yang diverifikasi; (2) **Purnama/Tilem** dihitung
astronomis (fase bulan, waktu WITA) lalu dicek dengan tanggal resmi;
(3) **Sasih/Çaka** diturunkan dari hitungan Tilem, termasuk sasih tambahan
(nampih) dan hari yang dihapus (ngunalatri). Ini bagian paling berisiko.

## Keputusan teknis (pilihan builder, bukan kesepakatan pemilik)

- UI: Jetpack Compose, satu Activity, tema sendiri (bukan warna/ikon
  aplikasi lain).
- Struktur: modul `:calendar-core` (Kotlin/JVM, tanpa Android) + modul
  `:app` (UI, alarm, billing).
- Alarm: `AlarmManager.setExactAndAllowWhileIdle`, jadwal ulang saat
  `BOOT_COMPLETED`, izin exact alarm, dan layar bantuan "Agar alarm tidak
  mati" per merek HP (teks ditulis sendiri).
- Penyimpanan: DataStore untuk pengaturan; tidak perlu database.
- Donasi: produk sekali bayar (konsumsi) `donasi_10k`, `donasi_25k`,
  `donasi_50k`, `donasi_100k`; tombol "terima kasih" setelah berhasil.
- Tes: tes unit pada `:calendar-core` dengan data emas.

## Keputusan tambahan dari builder check (pilihan builder)

- Jangkauan: tes emas 2024–2035. Navigasi 2000–2100; di luar tahun
  terverifikasi tampil peringatan kecil "hitungan belum diverifikasi".
- Aturan Sasih/Çaka/nampih/ngunalatri: ikut kalender resmi Parisada;
  ujian lulus = cocok dengan tanggal terbit resmi pada data emas.
- Jangkar Pawukon: diturunkan dari data emas lalu dicek silang.
- Rainan yang bisa dialarm: Purnama, Tilem, Kajeng Kliwon, Anggara Kasih,
  Buda Cemeng, dan hari raya besar. Alarm Rainan jam 06:00 (bisa diubah).
  Jadwal bergulir 60 hari ke depan, dijadwal ulang tiap app dibuka dan saat boot.
- "Andal di Oppo/Xiaomi" = layar bantuan di app. Uji di HP sungguhan
  dikerjakan pemilik sebelum rilis.
- Android SDK: builder coba pasang di sandbox. Jika gagal, kode ditulis dan
  modul `:calendar-core` dites penuh; build `:app` dan uji HP oleh pemilik.
- Izin alarm: `USE_EXACT_ALARM` (aplikasi kalender), cadangan
  `SCHEDULE_EXACT_ALARM`. ID produk donasi tetap; harga bisa diubah di Play Console.
- Bahasa: Indonesia saja, string disimpan di resource agar mudah ditambah.

## Tampilan (disetujui pemilik)

Contoh tampilan 5 layar disetujui: https://claude.ai/artifact/EbjKwUZKj9UK357VZhcv1H
(privat, milik pemilik). Tema teal `#0F5257` + emas `#E0A21B` di atas krem.
Layar: Kalender (grid bulan + 4 tab), Detail hari, Cari (Otonan / Odalan
Pawukon / Odalan Sasih), Alarm (Tri Sandya, Rainan, panduan per merek HP),
Donasi. Data di mockup hanya contoh.

## Mesin kalender (`:calendar-core`)

Kotlin/JVM murni, hanya `java.time`. Tanpa Android, tanpa jaringan.

### Antarmuka

```kotlin
interface BaliCalendar {
    fun day(date: LocalDate): BaliDay
    fun month(year: Int, month: Int): List<BaliDay>
    fun otonan(birth: LocalDate, count: Int, from: LocalDate): List<LocalDate>
    fun findByWewaran(wuku: Int, saptawara: Saptawara, pancawara: Pancawara,
                      from: LocalDate, count: Int): List<LocalDate>
    fun odalanByPawukon(wuku: Int, sapta: Saptawara, panca: Pancawara,
                        from: LocalDate, to: LocalDate): List<LocalDate>
    fun odalanBySasih(sasih: Sasih, tithi: Tithi, year: Int): List<LocalDate>
    fun rerainan(from: LocalDate, to: LocalDate, kinds: Set<RerainanKind>): List<Rerainan>
}
```

`BaliDay` memuat: tanggal, `wuku` (1–30), indeks 0–209, saptawara, pancawara,
triwara, caturwara, sadwara, astawara, sangawara, dasawara, urip, tahun Çaka,
sasih, tithi (penanggal 1–15 / pangelong 1–15), penanda Purnama/Tilem,
daftar rerainan, dan flag `verified` (di dalam rentang data emas).

### Bagian 1 — Pawukon (eksak, tanpa koreksi)

Siklus 210 hari. Patokan: **1 Nov 2026 = hari ke-0 = Wuku Sinta, Redite
(Minggu), Paing** (terbaca dari kalenderbali.com, dicek silang pada data
emas). `indeks = floorMod(hariSejakPatokan, 210)`.

- wuku = `indeks / 7 + 1`; saptawara = `indeks % 7`; pancawara = `indeks % 5`
  (Paing, Pon, Wage, Kliwon, Umanis); triwara = `indeks % 3`
  (Pasah, Beteng, Kajeng); sadwara = `indeks % 6`.
- Caturwara, astawara, sangawara punya aturan khusus (ada hari yang
  diulang di awal siklus); dasawara dari jumlah urip saptawara + pancawara.
  Tabel aturan ditulis builder dan harus lulus data emas 100%.
- Otonan = tanggal lahir + 210·k hari. Cari balik dan Odalan Pawukon: indeks
  dari (wuku, saptawara), pancawara dipakai untuk memeriksa kecocokan, lalu
  daftar tanggal dengan langkah 210 hari. Kombinasi yang tidak ada
  (pancawara tidak cocok) ditolak dengan pesan jelas.

### Bagian 2 — Bulan, Sasih, Çaka (hitungan + tabel koreksi)

- Fase bulan dihitung astronomis (algoritma Meeus, galat menit), diubah ke
  WITA (UTC+8). Tithi per hari dihitung pada patokan jam pagi (parameter
  yang disetel sampai paling banyak cocok dengan data emas).
- Purnama = tithi 15 penanggal; Tilem = tithi 15 pangelong. Tithi yang
  terlewat (ngunalatri) dan hari ganda (nampih) dihasilkan dari hitungan
  ini lalu dikoreksi tabel jika beda dari kalender resmi.
- Sasih (Kasa … Sada) diturunkan dari urutan Tilem; sasih tambahan
  (nampih sasih) ditandai dengan aturan yang dipilih builder dan diuji.
- Tahun Çaka = tahun Masehi − 78 setelah Nyepi, − 79 sebelum Nyepi
  (Nov 2026 → 1948, cocok dengan referensi).
- Nyepi = penanggal 1 Sasih Kadasa (sehari setelah Tilem Kesanga).
  Siwaratri = pangelong 14 Sasih Kapitu.

### Tabel koreksi (disetujui pemilik)

- Berkas data `corrections.json` di resource `:calendar-core`. Satu baris =
  satu tanggal + satu bidang (tithi, sasih, penanda Purnama/Tilem, rerainan
  tambah/hapus) + nilai + catatan sumber.
- Urutan: hitung → terapkan koreksi → hasil. Koreksi hanya untuk tanggal
  yang beda dari tanggal resmi. Tidak ada koreksi untuk Pawukon.
- Dihasilkan oleh skrip pembanding dari data emas (bukan ditulis tangan).
  Skrip melaporkan jumlah koreksi per tahun; jumlah besar = tanda hitungan
  salah dan harus dibenahi, bukan ditambal.
- Pemilik memperbarui data emas tiap tahun sebelum Desember. Tahun di luar
  data emas: tidak ada koreksi, `verified = false`, UI tampil peringatan.

### Bagian 3 — Rerainan

Berbasis Pawukon (eksak): Pagerwesi (Buda Kliwon Sinta), Banyu Pinaruh
(hari setelah Saraswati), Saraswati (Saniscara Umanis Watugunung), Soma
Ribek, Sabuh Mas, Galungan (Buda Kliwon Dungulan), Kuningan (Saniscara
Kliwon Kuningan), Tumpek (Landep, Wariga, Krulut, Uye, Wayang; Saniscara
Kliwon), Kajeng Kliwon (triwara Kajeng + pancawara Kliwon), Anggara Kasih
(Anggara Kliwon), Buda Cemeng.
Berbasis bulan: Purnama, Tilem, Nyepi, Ngembak Geni, Siwaratri.
Daftar lengkap dan nama persis dicocokkan dengan data emas.

### Data emas

- Tanggal fakta (Purnama, Tilem, wewaran, sasih, hari raya) 2024–2035,
  dikumpulkan lewat skrip dari kalenderbali.com (diizinkan pemilik; hanya
  fakta tanggal, bukan teks/tabel mereka) dan dicek dengan sumber kedua
  (kalender resmi Parisada/Kemenag bila ditemukan). Beda antar sumber
  dilaporkan ke pemilik, tidak diputuskan diam-diam.
- Disimpan di `calendar-core/src/test/resources/golden/`.

### Pengujian

1. Pawukon: setiap hari 2024–2035 harus cocok 100% (toleransi nol).
2. Bulan/Sasih: hasil mentah dibandingkan data emas, hasil akhir (hitungan
   + koreksi) harus cocok 100%; jumlah koreksi per tahun dicatat.
3. Cari Otonan/Odalan: tes sifat (hasil selalu indeks sama, selisih 210 hari)
   dan contoh tangan.
4. Rerainan: semua hari raya 2024–2035 cocok data emas.

## Suara Tri Sandya

Pemilik akan mengunggah mp3 setelah pembuatan selesai. Sampai itu, nada
notifikasi bawaan dipakai. Berkas ditaruh di `app/src/main/res/raw/`
(nama `tri_sandya.mp3`); builder menyiapkan tempat dan kode pemutar.
Hak cipta rekaman menjadi tanggung jawab pemilik. Pengguna bisa pilih
"suara Tri Sandya" atau "nada bawaan" di Pengaturan.

## Diserahkan ke builder

Nama paket final, ikon, warna, tata letak detail, daftar lengkap hari raya
selain yang disebut, nominal donasi akhir, format ekspor/bagikan.

## Pertanyaan terbuka untuk pemilik

Sudah dijawab pemilik: kalenderbali.com boleh dipakai untuk fakta tanggal;
pakai tabel koreksi; tampilan disetujui; mp3 Tri Sandya menyusul.

Masih terbuka (tidak menghalangi): nama paket dan akun Play Console.
