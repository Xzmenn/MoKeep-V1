# MoKeep — Android

Aplikasi keuangan pribadi **MoKeep** (*Keuangan lebih teratur, hidup lebih tenang.*) yang dibangun secara native untuk Android menggunakan **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Firebase Authentication (Google Sign-In)**, dan **Room Database**.

## Fitur Utama
- **Autentikasi Akun Google**: Masuk secara eksklusif menggunakan Akun Google melalui Jetpack Credential Manager dan Firebase Authentication, dengan pemisahan data per pengguna serta opsi memuat data contoh.
- **Dasbor Keuangan**: Ringkasan kekayaan bersih, pemasukan, pengeluaran, arus kas bersih, perbandingan periode sebelumnya, grafik tren arus kas, donat pengeluaran per kategori, grafik batang 6 bulan, peringatan anggaran, progres target, dan daftar akun aktif.
- **Transaksi & Transaksi Berulang**: Pencarian, filter lanjutan (periode, jenis, akun, kategori, rentang nominal, urutan), tambah/ubah/hapus dengan kalkulasi ulang saldo dan validasi limit kartu kredit, tag, lampiran dokumen/gambar (`OpenDocument`), serta aturan transaksi berulang otomatis.
- **Akun & Kategori**: Pengelolaan akun per jenis (Kas, Bank, E-Wallet, Kartu Kredit dengan pelacakan limit, Investasi), arsip/pulihkan akun, transfer cepat, serta kategori dan subkategori kustom dengan pemilih warna dan ikon.
- **Anggaran & Target Tabungan**: Anggaran bulanan dengan navigasi bulan, salin anggaran bulan sebelumnya, grafik riwayat 6 bulan, serta target tabungan dengan estimasi pencapaian otomatis dan riwayat setoran/penarikan.
- **Laporan & Ekspor**: Ringkasan, arus kas dengan saldo berjalan, distribusi kategori, tren, perkembangan kekayaan bersih 12 bulan, serta ekspor CSV dan PDF.
- **Pengaturan & Cadangan**: Profil & foto profil (Android Photo Picker), preferensi mata uang/locale/tema, impor/ekspor CSV dengan pratinjau validasi, serta cadangan & pemulihan JSON lengkap.

## Teknologi
- Kotlin 2.2 · Jetpack Compose & Material 3 · Navigation Compose (Type-Safe)
- Firebase Auth + Jetpack Credential Manager (Google Sign-In)
- Room Database + KSP · DataStore Preferences · Kotlinx Serialization · Coil
