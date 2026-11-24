# Kalender Bali (kalen3r) — Desain Versi 1

Tanggal: 2026-11-24. Status: menunggu persetujuan.

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

## Diserahkan ke builder

Nama paket final, ikon, warna, tata letak detail, daftar lengkap hari raya
selain yang disebut, nominal donasi akhir, format ekspor/bagikan.

## Pertanyaan terbuka untuk pemilik

1. **Data emas:** tanggal Purnama, Tilem, Nyepi, Galungan dari sumber resmi
   (mis. kalender Parisada/Kemenag) untuk 2024–2030 dipakai sebagai patokan
   tes. Boleh me pakai kalenderbali.com sebagai pembanding fakta tanggal?
2. Siapa pemilik akun Play Console dan nama paket yang diinginkan?
