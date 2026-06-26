# Firestore Schema Reference

> ElSuarKu — Database Schema v1.0

## Collections Overview

```
elSuarKu (project)
├── users/{userId}
├── elections/{electionId}
├── candidates/{candidateId}
├── votes/{voteId}
├── auditLogs/{logId}
├── announcements/{announcementId}
├── electionTemplates/{templateId}
└── deviceFingerprints/{fingerprint}
```

---

## `users/{userId}`

Menyimpan data pengguna terdaftar.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `string` | ✅ | User UID dari Firebase Auth |
| `email` | `string` | ✅ | Email pengguna |
| `name` | `string` | ✅ | Nama lengkap |
| `role` | `string` | ✅ | `voter` / `admin` / `monitor` |
| `photoUrl` | `string` | ❌ | URL foto profil di Firebase Storage |
| `isActive` | `boolean` | ✅ | Status keaktifan akun |
| `createdAt` | `timestamp` | ✅ | Server timestamp |
| `lastLoginAt` | `timestamp` | ❌ | Timestamp login terakhir |
| `deviceFingerprint` | `string` | ❌ | SHA-256 fingerprint perangkat |

**Security Rules**:
- Admin dapat membaca/menulis semua
- User hanya dapat membaca/menulis dokumennya sendiri
- Role ditulis oleh Cloud Function, bukan client

---

## `elections/{electionId}`

Menyimpan data pemilihan.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `string` | ✅ | UUID |
| `name` | `string` | ✅ | Nama pemilihan |
| `description` | `string` | ❌ | Deskripsi |
| `status` | `string` | ✅ | `NOT_STARTED` / `ACTIVE` / `ENDED` |
| `startDate` | `timestamp` | ✅ | Waktu mulai |
| `endDate` | `timestamp` | ✅ | Waktu selesai |
| `votedCount` | `number` | ✅ | Total suara masuk |
| `totalEligibleVoters` | `number` | ❌ | Total pemilih terdaftar |
| `maxCandidates` | `number` | ❌ | Maks kandidat |
| `isAnonymous` | `boolean` | ✅ | Anonimitas pemilih |
| `requireBiometric` | `boolean` | ❌ | Gate biometrik |
| `createdBy` | `string` | ✅ | Admin UID |
| `createdAt` | `timestamp` | ✅ | Server timestamp |

**Security Rules**:
- Admin dapat CRUD penuh
- Monitor dapat membaca semua
- Voter hanya membaca ACTIVE elections
- Semua user dapat membaca results ENDED elections

---

## `candidates/{candidateId}`

Menyimpan kandidat per pemilihan.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `string` | ✅ | UUID |
| `name` | `string` | ✅ | Nama kandidat |
| `electionId` | `string` | ✅ | Foreign key ke elections |
| `description` | `string` | ❌ | Deskripsi/visi-misi |
| `photoUrl` | `string` | ❌ | Foto kandidat (Firebase Storage) |
| `voteCount` | `number` | ✅ | Counter suara (incremented via FieldValue.increment) |
| `order` | `number` | ❌ | Urutan tampilan |
| `createdAt` | `timestamp` | ✅ | Server timestamp |

**Security Rules**:
- Admin CRUD penuh
- Monitor + Voter read-only pada candidate milik election yang relevan
- `voteCount` hanya dapat diupdate oleh Cloud Function atau VoteRepository (FieldValue.increment)

---

## `votes/{voteId}`

Menyimpan suara terenkripsi.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `string` | ✅ | UUID |
| `electionId` | `string` | ✅ | Foreign key ke elections |
| `candidateId` | `string` | ✅ | Foreign key ke candidates |
| `voterHash` | `string` | ✅ | SHA-256 hash identitas pemilih (anonim) |
| `encryptedChoice` | `string` | ✅ | AES-256-GCM ciphertext |
| `encryptionIV` | `string` | ✅ | Initialization vector untuk dekripsi |
| `timestamp` | `timestamp` | ✅ | Server timestamp |
| `reconciliationStatus` | `string` | ✅ | `CONFIRMED` / `PENDING_RECONCILIATION` / `RECONCILED` |
| `signature` | `string` | ❌ | HMAC-SHA256 signature untuk verifikasi integritas |

**Security Rules**:
- Create: voterHash + electionId + candidateId unique (mencegah double voting)
- Read: Admin + Monitor (tanpa encryptedChoice); Voter hanya suaranya sendiri
- Update: Hanya `reconciliationStatus` field, hanya oleh Cloud Function/admin
- Delete: Tidak diizinkan (immutable)

---

## `auditLogs/{logId}`

Menyimpan audit trail.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `string` | ✅ | UUID |
| `userId` | `string` | ✅ | User yang melakukan aksi |
| `action` | `string` | ✅ | `VOTE_SUBMIT` / `ELECTION_CREATE` / `STATUS_CHANGE` / `ROLE_CHANGE` / `LOGIN` |
| `electionId` | `string` | ❌ | Foreign key ke elections |
| `metadata` | `map` | ❌ | Data tambahan (key-value) |
| `reconciliationStatus` | `string` | ❌ | Status reconciliation (untuk VOTE_SUBMIT) |
| `timestamp` | `timestamp` | ✅ | Server timestamp |

**Security Rules**:
- Create: Semua authenticated users
- Read: Admin + Monitor
- Update/Delete: Tidak diizinkan (append-only, immutable)

---

## `announcements/{announcementId}`

Menyimpan pengumuman in-app.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `string` | ✅ | UUID |
| `title` | `string` | ✅ | Judul pengumuman |
| `message` | `string` | ✅ | Isi pengumuman |
| `priority` | `string` | ✅ | `HIGH` / `NORMAL` / `LOW` |
| `isActive` | `boolean` | ✅ | Status aktif |
| `expiresAt` | `timestamp` | ❌ | Waktu kadaluarsa |
| `createdBy` | `string` | ✅ | Admin UID |
| `createdAt` | `timestamp` | ✅ | Server timestamp |

**Security Rules**:
- Admin CRUD penuh
- Semua user bisa membaca yang aktif

---

## `electionTemplates/{templateId}`

Menyimpan template konfigurasi pemilihan.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `string` | ✅ | UUID |
| `name` | `string` | ✅ | Nama template |
| `description` | `string` | ❌ | Deskripsi |
| `defaultDurationDays` | `number` | ✅ | Durasi default (hari) |
| `maxCandidates` | `number` | ✅ | Maks kandidat |
| `isAnonymous` | `boolean` | ✅ | Anonimitas |
| `requireBiometric` | `boolean` | ❌ | Biometrik gate |
| `allowMonitorAccess` | `boolean` | ✅ | Akses monitor |
| `customFields` | `array<map>` | ❌ | Additional fields |
| `createdBy` | `string` | ✅ | Admin UID |
| `createdAt` | `timestamp` | ✅ | Server timestamp |
| `updatedAt` | `timestamp` | ✅ | Last modified |

---

## `deviceFingerprints/{fingerprint}`

Menyimpan fingerprint perangkat untuk concurrent session detection.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `fingerprint` | `string` | ✅ | SHA-256 hash (document ID) |
| `userId` | `string` | ✅ | User UID |
| `lastSeen` | `timestamp` | ✅ | Timestamp aktivitas terakhir |

---

## Indexes (Composite)

| Collection | Fields | Purpose |
|------------|--------|---------|
| `votes` | `electionId` ASC, `timestamp` ASC | Vote timeline queries |
| `votes` | `voterHash` ASC, `electionId` ASC | Deduplication check |
| `votes` | `electionId` ASC, `reconciliationStatus` ASC | Reconciliation queries |
| `candidates` | `electionId` ASC, `voteCount` DESC | Results display |
| `auditLogs` | `electionId` ASC, `timestamp` DESC | Audit trail queries |
| `auditLogs` | `userId` ASC, `timestamp` DESC | User activity queries |
| `announcements` | `isActive` ASC, `createdAt` DESC | Active announcements |

---

## Data Retention

| Collection | Retention | Notes |
|------------|-----------|-------|
| `votes` | 90 hari setelah election ENDED | Dihapus otomatis oleh Cloud Function |
| `auditLogs` | 1 tahun | Diarsipkan ke Cold Storage setelah 90 hari |
| `deviceFingerprints` | 30 menit idle | Auto-cleanup |
| Semua lainnya | Indefinite | Kecuali user request deletion |
