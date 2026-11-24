
## QRIS donasi

Letakkan gambar QRIS di `app/src/main/res/drawable-nodpi/qris_donasi.png` (buat foldernya jika belum ada; persegi, disarankan minimal 1000 x 1000 px). Tanpa file ini layar Donasi menampilkan "QRIS segera hadir" dan tombol bagikan disembunyikan. Isi nama penerima (opsional) pada string `donasi_penerima` di `app/src/main/res/values/strings.xml`; kosong berarti baris penerima tidak tampil.

Ukuran gambar QRIS: persegi, 1000 sampai 2000 px (maksimal sekitar 2000 px, karena layar memuat gambar pada ukuran aslinya). Berkas `app/src/main/res/raw/keep.xml` mencegah shrinkResources membuang gambar `qris_donasi` yang dicari saat aplikasi berjalan; jangan dihapus.
