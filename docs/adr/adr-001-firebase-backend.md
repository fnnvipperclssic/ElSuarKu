# ADR-001: Firebase sebagai Backend Utama

**Status**: Accepted
**Tanggal**: 2026-06-24
**Deciders**: Tim ElSuarKu

## Context

Platform e-voting membutuhkan backend yang dapat menangani autentikasi pengguna, penyimpanan data real-time, keamanan tingkat tinggi, dan skalabilitas untuk pemilihan dengan ribuan pemilih. Kami perlu memilih antara membangun backend kustom (Express.js/FastAPI + PostgreSQL) atau menggunakan BaaS (Backend-as-a-Service).

## Decision

**Menggunakan Firebase (Authentication + Firestore + Cloud Functions + App Check + Storage) sebagai backend utama.**

Alasan:
1. **Time-to-market** — Firebase menyediakan autentikasi, database, dan fungsi serverless tanpa perlu mengelola infrastruktur
2. **Real-time sync** — Firestore mendukung snapshot listeners untuk pembaruan hasil pemilihan secara real-time
3. **Keamanan berlapis** — App Check (Play Integrity) + Firestore Security Rules + Custom Claims memberikan model keamanan defense-in-depth
4. **Skalabilitas otomatis** — Firestore menangani scaling tanpa intervensi manual untuk beban puncak pemilihan
5. **Cloud Functions** — Memungkinkan logika server-side (custom claims, reconciliation, scheduled tasks) tanpa mengelola server

## Consequences

**Positif:**
- Setup cepat, fokus pada fitur bisnis bukan infrastruktur
- Keamanan terintegrasi (App Check, Firebase Auth, Security Rules)
- Tidak perlu mengelola server, patching, atau scaling manual
- Observability melalui Firebase Crashlytics + Performance Monitoring

**Negatif:**
- Vendor lock-in ke Google Cloud Platform
- Biaya dapat meningkat pada skala besar (meskipun masih di bawah biaya operasional server kustom)
- Batasan query Firestore (tidak ada JOIN, aggregasi terbatas, composite index maks 200)
- Cold start Cloud Functions (~2-5 detik untuk first invocation)

## Alternatives Considered

| Alternatif | Kelebihan | Kekurangan | Alasan Ditolak |
|------------|-----------|------------|----------------|
| **Supabase** | Open source, PostgreSQL, real-time | Self-host complexity, lebih baru dari Firebase | Maturity risk untuk sistem voting |
| **Custom (FastAPI + PostgreSQL + Redis)** | Kontrol penuh, tidak ada vendor lock-in | Waktu development 3-5x lebih lama, perlu tim DevOps | Tidak feasible untuk MVP awal |
| **AWS Amplify + DynamoDB** | Ekosistem AWS luas, skalabilitas tinggi | Kurva belajar curam, DX kurang baik untuk mobile | Developer experience lebih rendah dari Firebase |
