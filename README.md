# LaporLH

Aplikasi pelaporan lapangan offline-first untuk laporan kegiatan Dana Lingkungan. Petugas lapangan menyusun laporan berisi teks, foto, gambar, dokumen, berkas, dan lokasi tanpa sinyal; aplikasi mengirimnya otomatis saat jaringan kembali. Verifikator memeriksa laporan lewat panel admin web, lalu menetapkan Diverifikasi atau Ditolak beserta catatan.

Repositori ini berisi aplikasi Android (Kotlin, Jetpack Compose) di `android/`, backend Node.js (TypeScript, Express) beserta panel admin di `backend/`, dan berkas contoh lampiran di `docs/sample/`. Studi kasus dan seluruh data contoh bersifat fiktif.

## Pemetaan persyaratan

| Persyaratan | Cara dipenuhi |
| --- | --- |
| Aplikasi offline-first | Semua alur tulis berjalan tanpa jaringan. Room menjadi sumber data utama, antarmuka hanya membaca dari Room, dan WorkManager mengirim antrean begitu jaringan tersedia. |
| Mobile native dengan Kotlin | Kotlin, Jetpack Compose, Material 3, Hilt, Room, WorkManager, Retrofit. Min API 26. |
| Autentikasi JWT, exp 5 menit | Access token JWT HS256 dengan `exp = iat + 300`. Server menolak token yang sudah lewat exp tanpa masa tenggang. |
| Setelah 5 menit token masih bisa dipakai | Aplikasi memperbarui token sendiri dengan tanda tangan kunci perangkat di Android Keystore, tanpa login ulang, juga setelah offline. |
| Studi kasus bebas | Laporan lapangan kegiatan dana lingkungan, misalnya penanaman bibit mangrove. |
| Form teks, berkas, dokumen, foto, gambar, lokasi | Satu form: judul dan deskripsi, foto dari kamera, gambar dari galeri, dokumen (PDF, DOCX, XLSX) dan berkas (TXT, CSV) lewat pilihan Dokumen, serta lokasi GPS dengan akurasi, waktu, dan alamat bila tersedia. |
| Backend Node.js dan Express | Express 5 dengan TypeScript, Prisma, PostgreSQL, dan panel admin AdminJS. |
| Repositori GitHub | Monorepo dengan CI untuk Android dan backend, serta APK rilis di GitHub Releases. |

## Mencoba versi yang sudah di-hosting

Backend berjalan di Render (paket gratis, region Singapore) dengan PostgreSQL dan penyimpanan berkas di Neon.

| | Alamat |
| --- | --- |
| API | `https://laporlh-api.onrender.com/api/v1` |
| Health check | `https://laporlh-api.onrender.com/health` dan `/ready` |
| Panel admin | `https://laporlh-api.onrender.com/admin` |
| APK rilis | [GitHub Releases](https://github.com/JunianIrman93/bpdlh/releases/latest) |

| Peran | Surel | Kata sandi | Dipakai di |
| --- | --- | --- | --- |
| Petugas lapangan | `petugas@laporlh.test` | `P@ssw0rd123` | Aplikasi Android |
| Verifikator | `verifikator@laporlh.test` | `P@ssw0rd456` | Panel admin |

APK rilis sudah mengarah ke server di atas: pasang, masuk dengan akun petugas, lalu buat laporan. Layanan gratis Render tidur setelah sekitar 15 menit tanpa permintaan, sehingga permintaan pertama bisa memakan waktu sekitar satu menit. Aplikasi menunggu sampai 120 detik dan mencoba ulang otomatis; membuka `/health` di peramban lebih dulu akan membangunkan server.

## Menjalankan backend dengan docker compose

Butuh Docker dengan Compose v2. Dari root repositori:

```
cp .env.example .env
```

Isi empat nilai yang masih kosong di `.env`: `JWT_SECRET` dan `ADMIN_COOKIE_SECRET` (teks acak berbeda, minimal 32 karakter, misalnya hasil `openssl rand -base64 48`), serta `SEED_OFFICER_PASSWORD` dan `SEED_REVIEWER_PASSWORD` untuk akun demo. Lalu jalankan:

```
docker compose up -d --build
```

Compose menyalakan PostgreSQL 16 dan API. Saat start, container menjalankan migrasi Prisma, membuat akun demo, lalu mendengarkan di port 8080. Setelah `http://localhost:8080/ready` menjawab 200, panel admin dapat dibuka di `http://localhost:8080/admin` dengan akun verifikator. Variabel lain di `.env.example` sudah berisi nilai bawaan; `ACCESS_TTL_SECONDS` (bawaan 300) dapat diperkecil untuk menguji pembaruan token. Kontrak API (13 endpoint di bawah `/api/v1`) ada di [backend/openapi/openapi.yaml](backend/openapi/openapi.yaml).

## Membangun APK

Butuh JDK 17 dan Android SDK dengan platform 37. Dari folder `android/`:

```
./gradlew assembleDebug
```

Build debug mengarah ke `http://10.0.2.2:8080/api/v1/`, yaitu backend docker compose di komputer yang sama bila aplikasi dijalankan di emulator. Build rilis mengarah ke server Render, hanya menerima HTTPS, dan memakai R8. Untuk ponsel fisik yang tersambung lewat kabel USB, teruskan port lalu bangun dengan alamat `localhost`:

```
adb reverse tcp:8080 tcp:8080
./gradlew assembleDebug -Plaporlh.apiBaseUrl=http://localhost:8080/api/v1/
```

APK rilis dibuat oleh workflow `release` setiap ada tag `v*`. Workflow itu menandatangani APK dengan keystore dari secrets GitHub, menolak APK bertanda tangan debug, lalu melampirkannya ke GitHub Release.

## Alur uji singkat

Berkas di `docs/sample/` dapat disalin ke folder Download atau Pictures di ponsel. `foto-kegiatan.jpg` (4000x3000) memperlihatkan kompresi ke 1600 piksel, dan `foto-orientasi-exif.jpg` memperlihatkan koreksi orientasi EXIF.

**Laporan tanpa sinyal.** Aktifkan mode pesawat, buat laporan baru, lalu isi judul, deskripsi, dan kategori. Tanggal hari ini dan lokasi terisi otomatis; pada sebagian besar ponsel GPS tetap berfungsi dalam mode pesawat. Tambahkan foto dan sebuah dokumen lewat Tambah lampiran, lalu tekan Kirim. Laporan berstatus Menunggu jaringan dan spanduk di daftar menyebut laporan akan dikirim otomatis.

**Jaringan kembali.** Matikan mode pesawat. Tanpa aksi apa pun, laporan terkirim beserta lampirannya dan statusnya menjadi Terkirim. Di panel admin, laporan berstatus SUBMITTED. Setelah verifikator menekan Verifikasi atau Tolak, tarik daftar ke bawah di aplikasi untuk melihat status dan catatannya.

**Token lewat 5 menit.** Masuk, biarkan aplikasi lebih dari 5 menit, lalu tarik daftar ke bawah. Penarikan berhasil tanpa login ulang; di log server, `POST /auth/challenge` dan `POST /auth/refresh` muncul sebelum `GET /reports`.

**Validasi.** Judul 5 sampai 100 karakter, deskripsi 20 sampai 500 karakter, tanggal kegiatan tidak di masa depan dan paling lama 90 hari ke belakang, minimal satu foto atau gambar, maksimal 10 lampiran, 2 MB per berkas, dan 10 MB per laporan. Pelanggaran ditampilkan per isian di aplikasi; bila dikirim langsung ke API, server menjawab 400, 413, atau 415.

## Arsitektur

Aplikasi Android memakai MVVM dengan Repository dan pembagian ui, domain, dan data di setiap fitur (`feature/auth`, `feature/report`, `feature/sync`), ditambah `core/` untuk basis data, jaringan, keamanan, berkas, dan tema. Antarmuka hanya membaca dari Room; hanya lapisan data yang berbicara dengan server.

Backend dibagi per modul (`auth`, `report`, `review`), masing-masing dengan lapisan domain, application, ports, dan adapters. Modul hanya boleh memakai `ports/` modul lain, dipaksa lewat ESLint. REST API dan panel admin memanggil use case review yang sama.

Laporan dan lampiran mendapat UUID di perangkat, sehingga `PUT /reports/{id}` dan `PUT /reports/{id}/attachments/{attachmentId}` aman diulang tanpa duplikat. Status sinkronisasi disimpan pada baris laporan di Room. SyncWorker mengirim metadata beserta manifes lampiran, lalu mengunggah lampiran satu per satu dengan SHA-256 yang diperiksa server; bila jaringan putus, putaran berikutnya melanjutkan dari lampiran yang tersisa. Galat sementara diulang dengan backoff eksponensial mulai 30 detik, dan setelah 8 percobaan laporan menjadi Gagal dengan tombol Coba ulang. Server menentukan tipe berkas dari byte awal, bukan dari nama berkas.

## Autentikasi tanpa token di perangkat

Server tetap menolak access token yang sudah kedaluwarsa; yang berubah adalah cara aplikasi mendapatkan token baru. Saat login, aplikasi membuat pasangan kunci EC P-256 di Android Keystore (alias `laporlh_device_key`) yang kunci privatnya tidak dapat diekspor, lalu mengirim kunci publik bersama `device_id`. Access token hanya disimpan di memori; di disk tidak ada token apa pun.

Untuk memperpanjang sesi, aplikasi meminta nonce sekali pakai (256 bit, berlaku 60 detik) lewat `POST /auth/challenge`, menandatangani `laporlh-auth-v1`, `device_id`, dan nonce dengan SHA256withECDSA, lalu menukarnya dengan access token baru lewat `POST /auth/refresh`. Pembaruan berjalan proaktif bila sisa umur token kurang dari 60 detik, dan reaktif lewat OkHttp Authenticator saat server menjawab 401. Tidak ada refresh token yang bisa dicuri dari perangkat, dan server dapat mencabut kunci kapan saja.

Kunci perangkat berlaku 30 hari sejak pemakaian terakhir. Bila server menolaknya, layar Masuk menampilkan pesan sesi berakhir dan data lokal tetap utuh sampai pengguna masuk lagi. Keluar akun mencabut kunci di server, lalu menghapus kunci Keystore dan seluruh data lokal; aplikasi memperingatkan lebih dulu bila masih ada laporan yang belum terkirim.

## Keterbatasan yang diketahui

- Laporan dari perangkat lain awalnya muncul sebagai ringkasan; isinya diunduh saat laporan dibuka dan membutuhkan internet. Dokumen dari server hanya ditampilkan nama dan ukurannya.
- Alamat lokasi dicari dengan Geocoder bawaan Android dan membutuhkan internet. Laporan yang dikirim sebelum alamat sempat dicari, termasuk laporan yang dibuat sebelum fitur ini ada, hanya memiliki koordinat.
- Aplikasi dikunci pada orientasi potret. Pada tablet dan ponsel lipat yang terbuka dengan Android 16 ke atas, sistem mengabaikan kunci ini untuk aplikasi yang menargetkan API 37.
- Basis data lokal tidak dienkripsi dan hanya dilindungi sandbox aplikasi. Tidak ada certificate pinning, key attestation, atau Play Integrity.
- Nonce autentikasi, batas laju, dan sesi panel admin disimpan di memori satu instance, sehingga verifikator perlu masuk ulang ke panel setelah server tidur.
- Uji unit backend (Vitest) dijalankan lokal dan tidak disertakan di repositori. Aplikasi Android diuji manual di emulator dan ponsel.

## Uji memori hosting

Sebelum memilih Render gratis (512 MB RAM, 0,1 CPU), image produksi diuji pada 10 Oktober dengan batas yang sama terhadap PostgreSQL 16 lokal: login, PUT laporan, unggah lampiran 1 MB dan 10 MB (sebelum batas per berkas diturunkan ke 2 MB), panel admin, dan review. Memori puncak 146 sampai 184 MiB, `/ready` sekitar 32 detik setelah start termasuk migrasi, login sekitar 2 detik karena bcrypt cost 12, dan p95 endpoint ringan 95 ms. Tidak ada kehabisan memori, sehingga backend memakai Render gratis dengan Neon gratis.

## Pengembangan

Backend butuh Node.js 22 (lihat `.node-version`). CI menjalankan `npm ci`, `npm run lint`, dan `npm run typecheck` di `backend/`, serta `./gradlew ktlintCheck assembleDebug` di `android/`. Untuk menjalankan backend di luar Docker, nyalakan PostgreSQL dengan `docker compose up -d postgres`, buat `backend/.env` dengan `DATABASE_URL` ke `localhost:5432`, lalu jalankan `npm run build`, `npm run migrate`, dan `npm start`.
