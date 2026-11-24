# Daftar tugas pemilik sebelum rilis

Dokumen ini untuk pemilik aplikasi (tidak perlu bisa pemrograman). Bagian bertanda "minta pengembang" memerlukan bantuan orang yang bisa membuka proyek ini. Aplikasi dibagikan sebagai berkas APK dari website Anda sendiri, bukan lewat Play Store.

Urutan yang disarankan: 1, 2, 3, lalu 9 (cek tanggal), 4, 5, 6, 7, 8.

## 1. Pilih nama paket final

Nama paket adalah identitas aplikasi di HP (contoh `id.namaanda.kalenderbali`). Sekarang sementara `id.kalen3r`. Setelah dirilis, nama paket tidak boleh diganti: HP menganggap aplikasi dengan nama paket lain sebagai aplikasi berbeda, sehingga pengguna tidak bisa memperbarui.

Letaknya: berkas `app/build.gradle.kts`, satu baris `applicationId = "id.kalen3r"` di dalam `defaultConfig`. Langkah ganti: ubah teks di antara tanda kutip pada baris itu (huruf kecil, titik sebagai pemisah, tanpa spasi), simpan, lalu bangun ulang aplikasi. Alamat FileProvider dan pencarian gambar/suara memakai nama paket ini secara otomatis. Baris `namespace = "id.kalen3r"` dan nama folder kode boleh dibiarkan; itu hanya nama internal kode. Jika ingin menyamakan semuanya, minta pengembang.

## 2. Gambar QRIS dan nama penerima

Letakkan gambar QRIS di `app/src/main/res/drawable-nodpi/qris_donasi.png` (buat foldernya jika belum ada; persegi, disarankan minimal 1000 x 1000 px). Tanpa file ini layar Donasi menampilkan "QRIS segera hadir" dan tombol bagikan disembunyikan. Isi nama penerima (opsional) pada string `donasi_penerima` di `app/src/main/res/values/strings.xml`; kosong berarti baris penerima tidak tampil.

Ukuran gambar QRIS: persegi, 1000 sampai 2000 px (maksimal sekitar 2000 px, karena layar memuat gambar pada ukuran aslinya). Berkas `app/src/main/res/raw/keep.xml` mencegah shrinkResources membuang gambar `qris_donasi` yang dicari saat aplikasi berjalan; jangan dihapus.

## 3. Suara Tri Sandya (mp3)

Letakkan berkas di `app/src/main/res/raw/tri_sandya.mp3` (buat folder `raw` bila perlu; folder itu sudah berisi `keep.xml`, jangan dihapus). Nama berkas harus persis `tri_sandya.mp3` (huruf kecil semua).

- Format mp3, sebaiknya 20 sampai 60 detik, mono atau stereo 128 kbps cukup (ukuran di bawah 1 MB). Hindari berkas panjang karena notifikasi akan terus berbunyi.
- Hak cipta rekaman adalah tanggung jawab Anda. Pakai rekaman milik sendiri atau yang jelas izinnya.
- Pilihan "Suara Tri Sandya" di layar Pengaturan Alarm hanya muncul bila berkas ini ada. Tanpa berkas, aplikasi memakai nada notifikasi bawaan HP.
- Setelah menaruh berkas, bangun ulang aplikasi (langkah 5).

## 4. Membuat kunci tanda tangan (sekali saja)

Kunci ini membuktikan bahwa APK berasal dari Anda. Buat dengan perintah berikut (di komputer yang punya Java; ganti jalur dan nama):

    keytool -genkeypair -v -keystore ~/kunci-kalender/kalender-bali.jks -alias kalender -keyalg RSA -keysize 2048 -validity 10000

Anda akan diminta membuat kata sandi dan mengisi nama. Catat kata sandi di tempat aman.

**PERINGATAN KERAS:**
- Simpan berkas `.jks` DI LUAR folder proyek (contoh di atas: `~/kunci-kalender/`). Jangan pernah dimasukkan ke git, email publik, atau website.
- Buat minimal dua cadangan (flashdisk dan penyimpanan terenkripsi) beserta kata sandinya.
- Jika kunci hilang, pengguna TIDAK BISA memperbarui aplikasi: mereka harus menghapus aplikasi lama dan memasang yang baru, dan data pengaturan mereka hilang. Jika kunci bocor, orang lain bisa membuat APK palsu atas nama Anda.

## 5. Membangun APK bertanda tangan

Pastikan nama paket (1), QRIS (2), dan mp3 (3) sudah final. Lalu jalankan di folder proyek (satu baris per perintah; ganti jalur dan kata sandi):

    export ANDROID_HOME=~/android-sdk
    export KALEN3R_KEYSTORE=~/kunci-kalender/kalender-bali.jks
    export KALEN3R_KEYSTORE_PASSWORD='kata-sandi-anda'
    export KALEN3R_KEY_ALIAS=kalender
    export KALEN3R_KEY_PASSWORD='kata-sandi-kunci'   # boleh dihilangkan jika sama dengan kata sandi keystore
    gradle :app:assembleRelease

Hasilnya: `app/build/outputs/apk/release/app-release.apk`. Jika keempat variabel tidak diisi, hasilnya `app-release-unsigned.apk` yang TIDAK bisa dipasang; itu bukan berkas untuk dibagikan. Nilai yang sama bisa diberikan sebagai `-PKALEN3R_KEYSTORE=...` dan seterusnya. Jangan menulis kata sandi ke berkas di dalam proyek.

Memeriksa tanda tangan: `$ANDROID_HOME/build-tools/<versi>/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`.

Setiap rilis baru: naikkan `versionCode` (angka bulat, +1) dan `versionName` di `app/build.gradle.kts`. Tanpa menaikkan `versionCode`, HP menolak memperbarui. Rilis pertama: `versionCode = 1`, `versionName = "1.0.0"`.

Catatan: penyusutan kode (R8/minify) sengaja dimatikan pada v1, jadi APK lebih besar dari biasanya (sekitar 11 MB), tetapi lebih aman.

## 6. Menaruh APK di website

1. Unggah `app-release.apk` ke website Anda (misalnya `kalender-bali-1.0.0.apk`).
2. Hitung dan tampilkan checksum SHA-256 di halaman unduhan supaya pengguna bisa memeriksa: `sha256sum app-release.apk` (Linux/Mac) atau `certutil -hashfile app-release.apk SHA256` (Windows).
3. Tulis petunjuk bagi pengguna: setelah mengunduh, Android akan meminta izin "Instal dari sumber tidak dikenal" untuk peramban atau aplikasi File yang dipakai; izinkan lalu pasang. Izin itu bisa dimatikan lagi setelahnya.
4. Tidak ada pembaruan otomatis. Untuk versi baru, unggah APK baru dan beri tahu pengguna agar mengunduh dan memasangnya di atas versi lama (bisa, asalkan nama paket dan kunci yang sama dan `versionCode` lebih besar).
5. Unggah juga `PRIVACY.md` sebagai halaman Kebijakan Privasi dan tautkan dari halaman unduhan.
6. Untuk rilis, sebaiknya cantumkan "Aplikasi belum diverifikasi penuh untuk tahun 2024 Juli sampai 2035" bila data belum dilengkapi (lihat bagian 9 dan README).

## 7. Uji alarm di HP sungguhan (Xiaomi, Oppo, Huawei, Asus)

Aplikasi ini belum pernah dijalankan di HP; bagian ini wajib sebelum rilis. Pinjam atau gunakan minimal satu HP Xiaomi, satu Oppo, dan bila bisa Huawei dan Asus. Pasang APK bertanda tangan, buka layar Alarm, aktifkan Tri Sandya dan satu Rainan, lalu atur jam 2-3 menit dari sekarang. Uji:

- [ ] Izinkan notifikasi saat diminta (Android 13 ke atas).
- [ ] Android 12: aktifkan izin "Alarm dan pengingat" (alarm tepat) bila diminta.
- [ ] Alarm bunyi dengan layar mati dan HP terkunci.
- [ ] Alarm tetap bunyi setelah HP di-restart (reboot) tanpa membuka aplikasi.
- [ ] Alarm tetap bunyi setelah aplikasi di-swipe dari daftar aplikasi terbaru (recents).
- [ ] Alarm tetap sesuai jam setelah zona waktu HP diganti (misalnya WITA ke WIB) dan setelah jam diatur ulang.
- [ ] Alarm tidak bunyi dua kali, dan tidak bunyi setelah dimatikan di aplikasi.
- [ ] Ikuti layar "Agar alarm tidak mati" untuk merek itu (autostart, hemat baterai, kunci aplikasi di recents). Catat langkah yang salah atau menu yang berbeda di versi MIUI/ColorOS/EMUI/ZenUI Anda, dan kirim ke pengembang.
- [ ] Suara Tri Sandya terdengar (bila mp3 sudah ada) dan nada bawaan terdengar bila dipilih.
- [ ] Layar Donasi: QRIS tampil jelas, bisa dipindai dari aplikasi pembayaran, tombol Bagikan bekerja.
- [ ] Tampilan kalender, Detail, dan Cari tidak terpotong di layar kecil dan besar.

## 8. Pemeliharaan tahunan: data emas dan koreksi (sebelum Desember)

Letak berkas:
- Data emas (fakta tanggal resmi): `calendar-core/src/test/resources/golden/days-<tahun>.csv`, daftar rentang yang ada di `coverage.json` dan `COVERAGE.md`, sumber dan perbedaan di `SOURCES.md`, sumber kedua di `golden2/`.
- Tabel koreksi: `calendar-core/src/main/resources/corrections.json` (sekarang kosong). Satu baris = satu tanggal + satu bidang + nilai + catatan sumber. Jangan diedit tangan.
- Skrip: `scripts/golden/` (lihat `scripts/golden/README.md`).

**Jujur soal pengumpulan otomatis:** skrip `collect.mjs` mengambil data dari kalenderbali.com, tetapi situs itu memakai verifikasi bot yang berhenti lolos setelah beberapa permintaan, sehingga hanya 2024-01..06 dan 2026-11 (212 hari) yang berhasil dikumpulkan. Verifikasi itu sengaja tidak dilewati dan tidak boleh dilewati. Jadi pengumpulan otomatis tidak bisa diandalkan.

Yang sebaiknya Anda lakukan setiap tahun sebelum Desember (dan sekali sekarang untuk menutup celah):
1. Cari tanggal resmi terbitan lembaga yang berwenang, misalnya kalender Parisada Hindu Dharma Indonesia (PHDI) atau Kemenag, untuk tahun berikutnya: tanggal Purnama, Tilem, hari raya (Galungan, Kuningan, Nyepi, Saraswati, Pagerwesi, Siwaratri, dan lain-lain).
2. Berikan datanya dalam salah satu bentuk: (a) berkas CSV dengan format golden: `date,wuku,saptawara,pancawara,triwara,caturwara,astawara,sangawara,dasawara,urip,tithiType,tithiNo,sasih,caka,flags` (contoh baris ada di `days-2024.csv`; hanya fakta tanggal, jangan salin teks atau tabel dari situs lain), atau (b) daftar tanggal resmi sederhana dan minta pengembang menambah baris koreksi.
3. Pengembang menaruh CSV di folder golden, memperbarui `coverage.json` dan `COVERAGE.md`, lalu menjalankan `gradle :calendar-core:test` dan `gradle :calendar-core:generateCorrections`.

Cara kerja `generateCorrections`: skrip membandingkan hasil mesin dengan data emas pada rentang di `coverage.json` dan menulis ulang `corrections.json` hanya untuk tanggal yang berbeda. Laporan jumlah koreksi per tahun harus kecil; jumlah besar berarti aturan hitungan salah dan harus dibenahi, bukan ditambal. Setelah itu bangun ulang APK dan naikkan versi (langkah 5).

Tanpa langkah ini, tahun di luar data terverifikasi tetap tampil dengan peringatan "hitungan belum diverifikasi".

## 9. Cek dua area yang belum pasti (sebelum rilis)

Verifikasi keduanya dengan kalender resmi (PHDI/Kemenag) dan kabari pengembang bila beda:
1. **Nyepi 2027**: sumber-sumber independen tidak sepakat, 8 Maret atau 9 Maret 2027 (lihat `SOURCES.md`). Mesin belum dicocokkan dengan kalender resmi. Pastikan juga Ngembak Geni dan Çaka 1949 mengikuti.
2. **Nampih Desta (sasih tambahan) pertama pada 2027** (Çaka 1949): tidak ada data di repo yang membuktikan aturan ini. Periksa semua tanggal Purnama, Tilem, dan hari raya berbasis bulan dari Mei sampai Juli 2027 (dan sesudahnya) terhadap kalender resmi.

Bila ada perbedaan, tambahkan sebagai data emas (langkah 8), bukan dibiarkan.
