# Data Protection Impact Assessment (DPIA)

> **Project**: ElSuarKu — Cloud-Based Secure E-Voting Platform
> **Tanggal**: 24 Juni 2026
> **DPO**: (TBD)
> **Referensi**: UU No. 27 Tahun 2022 (UU PDP), GDPR Art. 35

---

## 1. Ringkasan Eksekutif

ElSuarKu memproses data pribadi untuk keperluan e-voting internal organisasi. Data yang diproses mencakup email, nama, encrypted vote choices, dan device metadata. DPIA ini mengidentifikasi risiko privasi dan mitigasi yang diterapkan.

**Kesimpulan**: Dengan mitigasi yang ada, risiko residual berada pada level **Low-Medium** yang dapat diterima.

---

## 2. Data Flow Mapping

```
┌──────────┐     ┌───────────────┐     ┌──────────────┐     ┌──────────────┐
│  User    │────▶│ Android App   │────▶│ Firebase     │────▶│ Admin        │
│  Device  │     │ (Client)      │     │ (Server)     │     │ Dashboard    │
└──────────┘     └───────────────┘     └──────────────┘     └──────────────┘
     │                  │                      │                    │
     │ Device data      │ Encrypted vote       │ Aggregated         │ Can NOT see
     │ (hash only)      │ (AES-256-GCM)        │ results            │ individual votes
     ▼                  ▼                      ▼                    ▼
  Fingerprint      Ciphertext only      Counter + audit        Role-based
  (30min TTL)      (never readable      log (immutable)        access (admin
                   by server)                                  ≠ monitor)
```

**Data at rest**: Firestore (Google-managed encryption)
**Data in transit**: TLS 1.3 + Certificate Pinning
**Data in use**: Android Keystore (TEE/StrongBox) for encryption keys

---

## 3. Data Inventory

| Data Element | Category | Sensitivity | Legal Basis | Retention |
|-------------|----------|-------------|-------------|-----------|
| Email address | Personal | Medium | Contract + Consent | Until account deletion |
| Full name | Personal | Medium | Contract | Until account deletion |
| Encrypted vote | Special (political opinion) | **High** | Contract + Legitimate Interest | 90 days post-election |
| Device fingerprint | Personal (hash) | Low | Legitimate Interest | 30 min idle |
| FCM token | Technical | Low | Legitimate Interest | Until token refresh |
| Crash reports | Technical (anonymized) | Low | Legitimate Interest | 90 days |
| Audit logs | Personal (userId) | Medium | Legal Obligation | 1 year |

---

## 4. Risk Assessment

### Risk Matrix

| Risk | Likelihood | Impact | Inherent | Mitigation | Residual |
|------|-----------|--------|----------|------------|----------|
| Unauthorized access to vote data | Low | Critical | **High** | Firestore Security Rules + Custom Claims + App Check | **Low** |
| Data breach via MITM | Low | Critical | **High** | TLS 1.3 + Certificate Pinning + SSL intercept detection | **Low** |
| Admin abuse (view individual votes) | Medium | Critical | **High** | AES-256-GCM client-side encryption — admin CANNOT decrypt | **Low** |
| Device compromise | Low | High | **Medium** | Key Attestation + boot state check + biometric gate | **Low** |
| Double voting | Low | High | **Medium** | Voter hash deduplication + Firestore rules | **Low** |
| Data loss | Low | High | **Medium** | Firestore automatic backup + reconciliation engine | **Low** |
| Unauthorized FCM token access | Low | Low | **Low** | Token stored per-user, refresh on logout | **Low** |
| Re-identification from audit logs | Medium | Medium | **Medium** | Audit logs use userId not voter hash — admin visible but role-gated | **Medium** |

---

## 5. Privacy-by-Design Measures

| Principle | Implementation |
|-----------|---------------|
| **Data Minimization** | Hanya mengumpulkan data yang diperlukan untuk voting. Tidak ada location, contacts, atau browsing history. |
| **Purpose Limitation** | Data hanya digunakan untuk tujuan yang dinyatakan (voting, audit, monitoring) |
| **Storage Limitation** | Retention schedule ketat — vote dihapus 90 hari pasca pemilihan |
| **Integrity & Confidentiality** | AES-256-GCM + Keystore + certificate pinning + Firestore rules |
| **Transparency** | Privacy Policy tersedia di aplikasi dan repository |
| **User Rights** | User dapat mengakses, mengoreksi, menghapus data melalui admin organisasi |
| **Accountability** | Audit trail append-only mencatat semua operasi data |

---

## 6. Third-Party Processors

| Processor | Data | Purpose | DPA |
|-----------|------|---------|-----|
| Google (Firebase) | Semua data | Backend infrastructure | Firebase DPA — covered by GCP Terms |
| Google (Firebase Auth) | Email, password hash | Authentication | Firebase DPA |
| Google (FCM) | FCM token | Push notifications | Firebase DPA |
| Google (Crashlytics) | Anonymized crash data | Error monitoring | Firebase DPA |

Semua processor mematuhi GDPR dan memiliki Data Processing Agreement (DPA) dengan Google Cloud Platform.

---

## 7. Data Subject Rights Implementation

| Right | Mechanism | Timeline |
|-------|-----------|----------|
| Access | Admin dashboard → User management → Export | < 72 jam |
| Rectification | User profile edit (self-service) | Real-time |
| Erasure | Admin dashboard → Delete user | < 30 hari |
| Restriction | Settings → Disable optional features | Real-time |
| Portability | Contact DPO | < 30 hari |
| Objection | Uninstall + account deletion | Real-time |

---

## 8. Breach Notification

Jika terjadi data breach yang melibatkan data pribadi:
1. **Deteksi**: Crashlytics + Firestore audit monitoring
2. **Assessment**: Tim security evaluasi dalam 24 jam
3. **Notification**: 
   - Otoritas (DPO/BPKN) — dalam 72 jam (sesuai UU PDP)
   - Data subjects — tanpa delay yang tidak perlu
4. **Remediation**: Patch + root cause analysis

---

## 9. Sign-Off

| Role | Name | Date |
|------|------|------|
| Developer | Tim ElSuarKu | 2026-06-24 |
| DPO | (TBD) | |
| Legal Review | (TBD) | |

---

*DPIA ini ditinjau setiap 12 bulan atau setelah perubahan signifikan pada pemrosesan data.*
