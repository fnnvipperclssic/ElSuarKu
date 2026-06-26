# Data Retention Schedule

> **Berlaku**: 24 Juni 2026
> **Referensi**: UU PDP Pasal 51, GDPR Art. 5(1)(e)

---

## 1. Prinsip Umum

Data disimpan **hanya selama diperlukan** untuk tujuan yang dinyatakan, kemudian dihapus atau dianonimkan secara otomatis. Prinsip storage limitation diterapkan di semua collection.

---

## 2. Retention Matrix

### 2.1 User Data (`users/`)

| Data | Retention | Trigger | Action |
|------|-----------|---------|--------|
| Active account | Indefinite | — | — |
| Inactive account (> 1 tahun) | 30 hari setelah warning | Scheduled Cloud Function | Anonymize + notify |
| Deleted account | 30 hari | User/admin request | Soft delete → hard delete after 30d |
| Device fingerprint | 30 menit | Session timeout / logout | Auto-cleanup |
| FCM token | Until refresh | Token rotation | Overwrite |

### 2.2 Election Data (`elections/`, `candidates/`)

| Data | Retention | Trigger | Action |
|------|-----------|---------|--------|
| Election metadata | Indefinite | — | Archive after ENDED |
| Candidate data | Indefinite | — | Archived with election |
| Vote count (aggregate) | Indefinite | — | Results preserved permanently |

### 2.3 Vote Data (`votes/`)

| Data | Retention | Trigger | Action |
|------|-----------|---------|--------|
| Encrypted votes | **90 hari** | Election ENDED date + 90d | Batch delete by Cloud Function |
| Reconciliation status | Same as vote | — | Deleted with vote |
| Vote signatures | Same as vote | — | Deleted with vote |

**Rasional**: UU Pemilu di Indonesia mensyaratkan penyimpanan surat suara fisik selama 1 tahun. Untuk e-voting, encrypted digital ballot disimpan 90 hari untuk keperluan audit, kemudian dihapus. Hasil agregat disimpan permanen.

### 2.4 Audit Logs (`auditLogs/`)

| Data | Retention | Trigger | Action |
|------|-----------|---------|--------|
| Audit logs | **1 tahun** | Creation date + 365d | Archive to Cold Storage, then delete |
| Security events | **3 tahun** | Creation date + 1095d | Archive permanently |

### 2.5 System Data

| Data | Retention | Trigger | Action |
|------|-----------|---------|--------|
| Crash reports (Crashlytics) | 90 hari | Crash date + 90d | Auto-deleted by Firebase |
| Performance traces | 90 hari | Trace date + 90d | Auto-deleted by Firebase |
| FCM tokens (stale) | 30 hari | Last used + 30d | Batch cleanup |
| Announcements | Until expiry | expiresAt field | Soft-delete (isActive = false) |
| Election templates | Indefinite | — | — |

---

## 3. Deletion Mechanisms

| Mekanisme | Trigger | Scope |
|-----------|---------|-------|
| **Cloud Function (scheduled)** | Harian, UTC 00:00 | Hapus expired votes + archive old audit logs |
| **Cloud Function (onDelete)** | User document deleted | Cascade delete user-related data |
| **Firestore TTL (future)** | Document-level TTL | Auto-delete setelah timestamp |
| **Manual (admin)** | Admin action | Delete specific election/user |

---

## 4. Cloud Function: Data Retention Job

```typescript
// functions/src/retention.ts — Scheduled daily
export const runDataRetention = onSchedule("every 24 hours", async () => {
  const now = Date.now();

  // 1. Delete votes older than 90 days from ENDED elections
  const endedElections = await db.collection("elections")
    .where("status", "==", "ENDED")
    .where("endDate", "<", now - 90 * 86400000)
    .get();

  // 2. Archive audit logs older than 1 year
  const oldLogs = await db.collection("auditLogs")
    .where("timestamp", "<", now - 365 * 86400000)
    .get();

  // 3. Cleanup stale device fingerprints (> 30 min)
  const staleFingerprints = await db.collection("deviceFingerprints")
    .where("lastSeen", "<", now - 30 * 60000)
    .get();

  // Batch delete
  // ...
});
```

---

## 5. Data Subject Deletion Request

1. User mengirim permintaan ke admin organisasi
2. Admin memverifikasi identitas pemohon
3. Admin menjalankan "Delete Account" dari dashboard
4. Sistem:
   - Soft-delete user (isActive = false) — 30 hari grace period
   - Setelah 30 hari: hard delete user document
   - Vote tetap dipertahankan sampai retention period (anonim via voter hash)
   - Audit log tetap dipertahankan (kewajiban hukum)
5. Konfirmasi dikirim ke email pemohon

---

## 6. Backup Retention

| Backup Type | Retention | Frequency |
|-------------|-----------|-----------|
| Firestore automatic | 24 jam | Continuous |
| Firestore export | 30 hari | Daily |
| Cloud Functions source | Indefinite (git) | Per deploy |

Semua backup dienkripsi at rest (Google-managed keys).

---

*Jadwal ini ditinjau setiap 12 bulan atau saat ada perubahan regulasi.*
