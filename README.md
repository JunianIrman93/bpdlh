# LaporLH

Aplikasi pelaporan lapangan offline-first untuk laporan kegiatan Dana Lingkungan. Petugas lapangan menyusun laporan berisi teks, foto, gambar, dokumen, berkas, dan lokasi tanpa sinyal; aplikasi mengirimnya otomatis saat jaringan kembali. Verifikator memeriksa laporan lewat panel admin web, lalu menetapkan Diverifikasi atau Ditolak beserta catatan.

Repositori ini berisi aplikasi Android (Kotlin, Jetpack Compose) di `android/`, backend Node.js (TypeScript, Express) beserta panel admin di `backend/`, dan berkas contoh untuk menguji lampiran di `docs/sample/`. Studi kasus dan seluruh data contoh bersifat fiktif.

## Pemetaan persyaratan

| Persyaratan | Cara dipenuhi |
| --- | --- |
| Aplikasi offline-first | Semua alur tulis berjalan tanpa jaringan. Room menjadi sumber data utama, antarmuka hanya membaca dari Room, dan WorkManager mengirim antrean begitu jaringan tersedia. |
| Mobile native dengan Kotlin | Kotlin, Jetpack Compose, Material 3, Hilt, Room, WorkManager, Retrofit. Min API 26. |
| Autentikasi JWT, exp 5 menit | Access token JWT HS256 dengan `exp = iat + 300`. Server menolak token yang sudah lewat exp tanpa masa tenggang. |
| Setelah 5 menit token masih bisa dipakai | Aplikasi memperbarui token sendiri dengan tanda tangan kunci perangkat di Android Keystore, sehingga pengguna tidak perlu login ulang, juga setelah offline. Rinciannya di bagian Autentikasi. |
| Studi kasus bebas | Laporan lapangan kegiatan dana lingkungan, misalnya penanaman bibit mangrove. |
| Form teks, berkas, dokumen, foto, gambar, lokasi | Satu form laporan: judul dan deskripsi, foto dari kamera sistem, gambar dari Photo Picker, dokumen (PDF, DOCX, XLSX), berkas (TXT, CSV), dan lokasi GPS dengan akurasi dan waktu pengambilan. |
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

Akun demo:

| Peran | Surel | Kata sandi | Dipakai di |
| --- | --- | --- | --- |
| Petugas lapangan | `petugas@laporlh.test` | `P@ssw0rd123` | Aplikasi Android |
| Verifikator | `verifikator@laporlh.test` | `P@ssw0rd456` | Panel admin |

Layanan gratis Render tidur setelah sekitar 15 menit tanpa permintaan. Permintaan pertama setelahnya bisa memakan waktu sekitar satu menit; aplikasi menunggu sampai 120 detik dan mencoba ulang otomatis, jadi cukup tunggu. Membuka `/health` di peramban sebelum mencoba akan membangunkan server lebih dulu.

APK rilis sudah mengarah ke server di atas. Pasang APK, masuk dengan akun petugas, lalu buat laporan.

## Menjalankan backend sendiri dengan docker compose

Butuh Docker dengan Compose v2. Dari root repositori:

```
cp .env.example .env
```

Isi empat nilai yang masih kosong di `.env`:

- `JWT_SECRET` dan `ADMIN_COOKIE_SECRET`: teks acak minimal 32 karakter, misalnya hasil `openssl rand -base64 48`. Keduanya harus berbeda.
- `SEED_OFFICER_PASSWORD` dan `SEED_REVIEWER_PASSWORD`: kata sandi akun demo yang dibuat saat server pertama kali berjalan.

Lalu jalankan:

```
docker compose up -d --build
```

Compose menyalakan PostgreSQL 16 dan API. Saat start, container menjalankan migrasi Prisma, membuat akun demo, lalu mendengarkan di port 8080. Berkas lampiran disimpan di volume Docker. Setelah `http://localhost:8080/ready` menjawab 200, panel admin dapat dibuka di `http://localhost:8080/admin` dengan akun verifikator.

Variabel lingkungan lain di `.env.example` sudah berisi nilai bawaan yang sesuai. Beberapa yang penting:

| Variabel | Fungsi |
| --- | --- |
| `ACCESS_TTL_SECONDS` | Umur access token, bawaan 300 detik. Dapat diperkecil saat menguji pembaruan token. |
| `DEVICE_KEY_TTL_SECONDS` | Masa berlaku kunci perangkat sejak pemakaian terakhir, bawaan 30 hari. |
| `STORAGE_DRIVER` | `disk` untuk volume lokal, `s3` untuk penyimpanan S3-compatible (perlu `S3_ENDPOINT`, `S3_REGION`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`). |
| `TRUST_PROXY` | Jumlah proxy di depan API: 0 untuk docker compose, 1 untuk Render. |
| `BCRYPT_COST` | Cost hashing kata sandi, bawaan 12. |

Kontrak API (13 endpoint di bawah `/api/v1`) ada di [backend/openapi/openapi.yaml](backend/openapi/openapi.yaml).

## Membangun APK

Butuh JDK 17 dan Android SDK dengan platform 37. Dari folder `android/`:

```
./gradlew assembleDebug
```

Alamat server ditentukan per build type. Build debug mengarah ke `http://10.0.2.2:8080/api/v1/`, yaitu backend docker compose di komputer yang sama bila aplikasi dijalankan di emulator. Build rilis mengarah ke server Render. Untuk ponsel fisik yang terhubung ke docker compose di jaringan lokal, ganti alamatnya dengan IP komputer:

```
./gradlew assembleDebug -Plaporlh.apiBaseUrl=http://192.168.1.10:8080/api/v1/
```

Build debug mengizinkan HTTP biasa agar dapat terhubung ke server lokal. Build rilis hanya menerima HTTPS, memakai R8, dan berukuran sekitar 2,5 MB.

APK rilis dibuat oleh workflow `release` setiap ada tag `v*`. Workflow itu menandatangani APK dengan keystore yang disimpan sebagai secrets GitHub, memastikan APK tidak bertanda tangan debug, lalu melampirkannya ke GitHub Release.

## Alur uji singkat

Berkas di `docs/sample/` dapat dipakai sebagai lampiran. Salin ke folder Download atau Pictures di ponsel atau emulator. `foto-kegiatan.jpg` berukuran 4000x3000 untuk melihat kompresi ke 1600 piksel, sedangkan `foto-orientasi-exif.jpg` menyimpan piksel terputar dengan tag EXIF Orientation, untuk memeriksa bahwa orientasi dikoreksi dan EXIF dibuang.

**Laporan tanpa sinyal.** Aktifkan mode pesawat, buat laporan baru, lalu isi judul, deskripsi, kategori, dan tanggal. Ambil lokasi; pada sebagian besar ponsel GPS tetap berfungsi dalam mode pesawat. Tambahkan foto dari kamera atau galeri dan sebuah PDF atau XLSX, lalu tekan Kirim. Laporan berstatus Menunggu jaringan, tanpa dialog galat, dan spanduk di daftar menyebut laporan akan dikirim otomatis.

**Jaringan kembali.** Matikan mode pesawat. Spanduk berubah menjadi Menyinkronkan, kartu menampilkan progres unggah lampiran, lalu laporan menjadi Terkirim. Di panel admin, laporan berstatus SUBMITTED dengan semua lampirannya. Setelah verifikator menekan Verifikasi atau Tolak, tarik daftar ke bawah di aplikasi untuk melihat status dan catatannya.

**Token lewat 5 menit.** Masuk, biarkan aplikasi lebih dari 5 menit, lalu tarik daftar ke bawah. Penarikan berhasil tanpa login ulang. Di log server, `POST /auth/challenge` dan `POST /auth/refresh` muncul sebelum `GET /reports`, karena aplikasi memperbarui token lebih dulu bila sisa umurnya kurang dari 60 detik. Bila token habis di tengah permintaan, server menjawab 401 `TOKEN_EXPIRED`, aplikasi memperbarui token, lalu mengulang permintaan itu satu kali.

## Arsitektur

Aplikasi Android memakai MVVM dengan Repository dan pembagian ui, domain, dan data di setiap fitur (`feature/auth`, `feature/report`, `feature/sync`), ditambah `core/` untuk basis data, jaringan, keamanan, berkas, dan tema. Antarmuka hanya membaca dari Room; hanya lapisan data yang berbicara dengan server.

Backend dibagi per modul (`auth`, `report`, `review`). Setiap modul punya lapisan domain, application (use case), ports, dan adapters. Modul hanya boleh memakai `ports/` modul lain, dan aturan ini dipaksa ESLint. REST API dan panel admin memanggil use case review yang sama, sehingga aturan status tidak dapat dilewati dari panel.

Keputusan penting:

- **ID dari klien dan PUT idempoten.** Laporan dan lampiran mendapat UUID di perangkat. `PUT /reports/{id}` dan `PUT /reports/{id}/attachments/{attachmentId}` aman diulang: isi yang sama dijawab 200 tanpa duplikat, isi berbeda pada laporan yang sudah dikirim dijawab 409.
- **Antrean berbasis status.** Status sinkronisasi disimpan pada baris laporan di Room (Draf, Menunggu jaringan, Menyinkronkan, Terkirim, Gagal), tanpa tabel outbox terpisah.
- **Metadata dulu, lampiran satu per satu.** SyncWorker mengirim metadata beserta manifes lampiran, lalu mengunggah lampiran yang belum ada di server. Bila jaringan putus di tengah, putaran berikutnya melanjutkan dari lampiran yang tersisa. Setiap lampiran membawa SHA-256 yang diperiksa server.
- **Galat dibedakan.** Galat sementara (jaringan, server sibuk) diulang dengan backoff eksponensial mulai 30 detik; setelah 8 percobaan laporan menjadi Gagal dan menampilkan alasan serta tombol Coba ulang. Galat permanen (isi tidak valid, berkas ditolak) langsung menjadi Gagal.
- **Server menentukan status review.** Penarikan memakai `updated_since` dari waktu server, bukan jam perangkat.
- **Berkas diperiksa dari isinya.** Server menentukan tipe berkas dari byte awal, bukan dari nama atau header, dan menyimpannya dengan kunci UUID.

## Autentikasi tanpa token di perangkat

Persyaratan "token exp 5 menit, setelah 5 menit token masih bisa dipakai" dipenuhi tanpa melemahkan server. Server tetap menolak access token yang sudah kedaluwarsa; yang berubah adalah cara aplikasi mendapatkan token baru.

Saat login, aplikasi membuat pasangan kunci EC P-256 di Android Keystore dengan alias `laporlh_device_key`. Kunci privatnya tidak dapat diekspor dan tidak pernah masuk ke proses aplikasi. Kunci publik dikirim bersama `device_id` dan disimpan server. Access token hanya disimpan di memori. Di disk hanya ada profil, `device_id`, dan penanda masa berlaku sesi; tidak ada token apa pun.

Untuk memperpanjang sesi, aplikasi meminta nonce sekali pakai lewat `POST /auth/challenge`. Nonce itu berukuran 256 bit, berlaku 60 detik, dan hanya disimpan di memori server. Aplikasi lalu menandatangani tiga baris `laporlh-auth-v1`, `device_id`, dan nonce dengan SHA256withECDSA, dan menukar tanda tangan itu dengan access token baru lewat `POST /auth/refresh`. Pembaruan dijaga Mutex agar hanya satu yang berjalan pada satu waktu. Pembaruan terjadi secara proaktif sebelum sinkronisasi bila sisa umur token kurang dari 60 detik (dihitung dengan jam monotonik), dan secara reaktif lewat OkHttp Authenticator saat server menjawab 401.

Alasannya: refresh token yang disimpan di perangkat, terenkripsi atau tidak, dapat dicuri dari disk atau memori dan dipakai di tempat lain. Kunci di Keystore tidak dapat dibawa keluar dari perangkat, dan pembongkaran APK tidak membuka rahasia apa pun karena memang tidak ada rahasia di dalamnya. Server cukup menyimpan kunci publik dan dapat mencabutnya kapan saja.

Kunci perangkat berlaku 30 hari sejak pemakaian terakhir. Selama masa itu aplikasi dapat dibuka dan dipakai offline. Bila server menolak kunci perangkat, misalnya karena dicabut atau kedaluwarsa, sinkronisasi berhenti, layar Masuk menampilkan pesan sesi berakhir, dan data lokal tetap utuh sampai pengguna masuk lagi. Keluar akun mencabut kunci di server bila online, lalu menghapus kunci Keystore dan seluruh data lokal; aplikasi memperingatkan lebih dulu bila masih ada laporan yang belum terkirim.

## Keterbatasan yang diketahui

- Laporan yang dibuat di perangkat lain hanya muncul sebagai ringkasan (judul, kategori, tanggal, dan status). Deskripsi, lokasi, dan lampirannya belum diambil dari server, sehingga kartunya menampilkan 0 lampiran.
- Basis data lokal tidak dienkripsi dan hanya dilindungi sandbox aplikasi. Tidak ada certificate pinning.
- Nonce autentikasi, batas laju, dan sesi panel admin disimpan di memori satu instance. Verifikator perlu masuk ulang ke panel setelah server restart atau tidur.
- Kunci perangkat dapat disalahgunakan lewat hooking di perangkat root. Key attestation dan Play Integrity di luar cakupan versi ini.
- Uji unit backend (Vitest) dijalankan lokal dan tidak disertakan di repositori. Aplikasi Android belum memiliki uji otomatis; perilakunya diuji manual di emulator.
- Server hosting gratis tidur saat tidak dipakai, sehingga permintaan pertama lambat.

## Uji memori

Hosting gratis Render memberi 512 MB RAM dan 0,1 CPU. Sebelum memilihnya, image produksi diuji dengan batas yang sama (`docker run -m 512m --memory-swap 512m --cpus 0.1`) terhadap PostgreSQL 16 lokal dan penyimpanan disk, memakai `BCRYPT_COST=12` dan `NODE_OPTIONS=--max-old-space-size=384`. Alur yang dijalankan: login petugas, PUT laporan, unggah lampiran 1 MB dan 10 MB, membuka panel admin beserta asetnya, daftar laporan, dan menolak laporan lewat panel.

| Ukuran | Uji 1 | Uji 2 |
| --- | --- | --- |
| Start sampai /ready, termasuk `prisma migrate deploy` | 32,3 detik | 31,6 detik |
| Memori puncak (memory.peak cgroup, termasuk cache berkas) | 184 MiB | 146 MiB |
| Memori tertinggi menurut docker stats | 99 MiB | 85 MiB |
| Login (bcrypt cost 12) | 2,1 detik | |
| Unggah lampiran 10 MB | 1,3 detik | |
| p95 endpoint selain login dan unggah | 95 ms | 95 ms |

Tidak ada kehabisan memori. Hasil ini memenuhi batas memori puncak di bawah sekitar 400 MB dan /ready dalam 60 detik, sehingga backend memakai Render gratis dengan Neon gratis. Median endpoint ringan 2 sampai 5 ms; p95 sekitar 95 ms berasal dari pembatasan CPU 0,1, bukan dari kode. Login sekitar 2 detik karena bcrypt dibatasi CPU yang sama; bila terasa lambat di hosting, `BCRYPT_COST` dapat diturunkan sampai 10.

Alur yang sama dijalankan ulang terhadap server Render setelah deploy, dengan berkas tersimpan di Neon Object Storage: login 1,7 detik, unggah lampiran 10 MB 3,0 detik, dan endpoint ringan sekitar 45 sampai 60 ms.

## Pengembangan

Backend butuh Node.js 22 (lihat `.node-version`). Pemeriksaan yang juga dijalankan CI:

```
cd backend
npm ci
npm run lint
npm run typecheck
```

Untuk menjalankan backend di luar Docker, nyalakan PostgreSQL dengan `docker compose up -d postgres`, buat `backend/.env` dengan variabel dari `.env.example` dan `DATABASE_URL` yang mengarah ke `localhost:5432`, lalu jalankan `npm run build`, `npm run migrate`, dan `npm start`.

Aplikasi Android diperiksa dengan `./gradlew ktlintCheck assembleDebug`, sama seperti di CI.
