# LaporLH

Aplikasi pelaporan lapangan offline-first untuk laporan kegiatan Dana Lingkungan. Terdiri dari aplikasi Android (Kotlin, Jetpack Compose) dan backend Node.js (TypeScript, Express) dengan panel verifikator.

Proyek masih dalam pengerjaan. Petunjuk menjalankan, akun demo, dan ringkasan arsitektur akan ditambahkan setelah backend dan aplikasi dapat dijalankan.

## Pengembangan backend

Butuh Node.js 22 (lihat `.node-version`).

```
cd backend
npm ci
npm run lint
npm run typecheck
```

PostgreSQL untuk pengembangan lokal dijalankan dengan `docker compose up -d postgres`; buat berkas `.env` di root sesuai variabel yang dibaca `backend/src/shared/config/env.ts`.

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
