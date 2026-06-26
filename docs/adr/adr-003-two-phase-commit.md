# ADR-003: Two-Phase Commit untuk Vote Submission

**Status**: Accepted
**Tanggal**: 2026-06-24
**Deciders**: Tim ElSuarKu

## Context

Submit vote melibatkan beberapa operasi atomik yang harus konsisten:
1. Menulis dokumen vote ke Firestore
2. Menginkremen counter candidate.voteCount
3. Menginkremen counter election.votedCount
4. Menulis audit log

Jika salah satu operasi 2-4 gagal setelah operasi 1 berhasil, sistem akan memiliki inkonsistensi: vote tercatat tetapi counter tidak terupdate, menyebabkan hasil pemilihan salah.

Ini adalah **[CRITICAL] data integrity issue** — core invariant dari sistem e-voting.

## Decision

**Mengimplementasikan Two-Phase Commit pattern dengan Reconciliation Status.**

**Phase 1 — Write:**
- Vote ditulis dengan status `PENDING_RECONCILIATION`
- Ini adalah operasi atomik Firestore tunggal

**Phase 2 — Counters:**
- Setelah Phase 1 sukses, increment candidate.voteCount dan election.votedCount secara sequential
- Jika keduanya sukses → update vote status menjadi `CONFIRMED`
- Jika salah satu gagal → vote tetap `PENDING_RECONCILIATION`

**Reconciliation Job:**
- Background process (Cloud Function atau client-side) mendeteksi vote dengan status `PENDING_RECONCILIATION`
- Menghitung ulang counter dari data vote aktual
- Memperbaiki counter mismatch dan menandai vote sebagai `RECONCILED`

## Consequences

**Positif:**
- Tidak ada vote yang hilang — setiap vote tercatat meskipun terjadi partial failure
- Counter mismatch dapat dideteksi dan diperbaiki secara otomatis
- Audit trail lengkap: setiap vote memiliki riwayat status reconciliation

**Negatif:**
- Kompleksitas tambahan pada flow submit vote
- Reconciliation job perlu dijadwalkan dan dimonitor
- Ada window waktu singkat di mana counter bisa tidak akurat (sampai reconciliation berjalan)
- Tidak sepenuhnya atomik karena Firestore tidak mendukung multi-document transactions dengan increment

## Alternatives Considered

| Alternatif | Kelebihan | Kekurangan | Alasan Ditolak |
|------------|-----------|------------|----------------|
| **Firestore Transactions** | Atomic multi-document writes | Maks 500 operasi per transaksi, tidak cocok untuk counter global dengan concurrency tinggi | Transaction limits + contention |
| **Firestore Batched Writes** | Atomic batch up to 500 writes | Tidak ada atomicity guarantee antara batch dan operasi increment | Masih mungkin partial failure |
| **Single Document Model** | Atomic by design (one doc = one write) | Vote + counter + audit dalam satu dokumen — melanggar normalisasi, dokumen bisa melebihi 1 MiB limit | Desain data tidak scalable |
| **Event Sourcing** | Full audit trail, eventual consistency | Overkill untuk use case, kompleksitas tinggi, butuh message broker | Kompleksitas tidak sebanding |
