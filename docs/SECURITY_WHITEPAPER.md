# ElSuarKu Security Whitepaper

> **Versi**: 1.0 — 24 Juni 2026
> **Klasifikasi**: Public
> **Cakupan**: Arsitektur keamanan ElSuarKu v1.0

---

## 1. Executive Summary

ElSuarKu adalah platform e-voting berbasis cloud untuk pemilihan internal organisasi. Dokumen ini menjelaskan arsitektur keamanan yang melindungi integritas, kerahasiaan, dan ketersediaan sistem pemilihan.

**Security posture**: Defense-in-depth dengan 7 lapisan keamanan.

---

## 2. Arsitektur Keamanan

### 2.1 Defense-in-Depth Layers

```
┌─────────────────────────────────────────┐
│ L7: Audit Logging + Monitoring          │  Crashlytics, Firestore audit trail
├─────────────────────────────────────────┤
│ L6: Application Security                │  Input validation, biometric gate
├─────────────────────────────────────────┤
│ L5: Encryption                          │  AES-256-GCM, Android Keystore
├─────────────────────────────────────────┤
│ L4: Network Security                    │  Certificate pinning, SSL detection
├─────────────────────────────────────────┤
│ L3: Authentication & Authorization      │  Firebase Auth, Custom Claims, App Check
├─────────────────────────────────────────┤
│ L2: Device Security                     │  Key Attestation, boot state check
├─────────────────────────────────────────┤
│ L1: Data Security                       │  Firestore Security Rules, encryption at rest
└─────────────────────────────────────────┘
```

### 2.2 Threat Model

| Ancaman | Mitigasi | Severity |
|---------|----------|----------|
| Vote tampering in transit | AES-256-GCM encryption + certificate pinning | Critical |
| Unauthorized vote access | Firestore Security Rules + Custom Claims | Critical |
| Man-in-the-Middle | SPKI certificate pinning + SSL interception detection | High |
| Replay attacks | Unique voter hash + timestamp validation | High |
| Double voting | Voter hash deduplication in Firestore rules | High |
| Compromised device | Key Attestation verification + boot state check | Medium |
| Admin abuse | Audit trail + separation of duties (admin ≠ monitor) | Medium |
| DDoS / resource exhaustion | App Check rate limiting | Low |

---

## 3. Authentication & Authorization

### 3.1 Firebase Authentication

- **Email/Password**: Dengan validasi kekuatan password (min 8 karakter, mixed case, angka)
- **Google Sign-In**: OAuth 2.0 dengan token validation server-side
- **Session Management**: Timeout 5 menit inactivity, step-up auth 2 menit untuk voting

### 3.2 Role-Based Access Control

| Role | Custom Claim | Akses |
|------|-------------|-------|
| Voter | `role: "voter"` | Vote, lihat hasil sendiri |
| Admin | `role: "admin"` | Kelola pemilihan, kandidat, user role |
| Monitor | `role: "monitor"` | Lihat hasil real-time, audit log (read-only) |

Custom claims disimpan di Firebase Auth JWT token — tidak memerlukan database read untuk otorisasi, mencegah timing attacks.

### 3.3 Firebase App Check

- **Play Integrity** untuk perangkat Android
- Mencegah akses Firestore dari client yang tidak sah (non-app traffic)
- Cloud Functions mengecek App Check token di setiap pemanggilan `setUserRole`

---

## 4. Enkripsi

### 4.1 Enkripsi Vote (Client-Side)

- **Algoritma**: AES-256-GCM (authenticated encryption)
- **Key Storage**: Android Keystore (hardware-backed via TEE/StrongBox)
- **IV**: Random 12-byte per enkripsi (mencegah pattern analysis)
- **Key Isolation**: Kunci tidak pernah meninggalkan secure hardware
- **Biometric Gate**: `setUserAuthenticationRequired(true)` untuk akses kunci

### 4.2 Enkripsi Data (Server-Side)

- **At rest**: Semua data Firestore dienkripsi secara default (Google-managed keys)
- **In transit**: TLS 1.3 dengan certificate pinning (SPKI hash)
- **End-to-end**: Vote dienkripsi di client, ciphertext yang dikirim ke server

### 4.3 Key Attestation

```kotlin
// Memverifikasi bahwa kunci benar-benar hardware-backed
KeyAttestationVerifier.verifyKeyAttestation() → AttestationReport(
    isHardwareBacked: Boolean,
    isStrongBox: Boolean,
    isTeeEnforced: Boolean,
    verifiedBootState: BootState,  // VERIFIED / SELF_SIGNED / UNVERIFIED / FAILED
    isDeviceLocked: Boolean
)
```

---

## 5. Network Security

### 5.1 Certificate Pinning

- **Metode**: SPKI (Subject Public Key Info) pinning
- **Pin**: SHA-256 hash dari Firebase public key
- **Fallback**: Tidak ada — koneksi ditolak jika pin tidak cocok
- **File**: `res/xml/network_security_config.xml`

### 5.2 SSL Interception Detection

- Mendeteksi user-installed CA certificates (indikasi MITM proxy seperti Burp Suite, Charles)
- Memeriksa port proxy umum (8080, 8888, 27042)
- Hasil: `CLEAN` / `USER_CA_FOUND` / `SUSPICIOUS` / `ERROR`

### 5.3 Cleartext Traffic

- `android:usesCleartextTraffic="false"` di AndroidManifest
- Semua komunikasi melalui HTTPS

---

## 6. Data Integrity

### 6.1 Two-Phase Commit Voting

Flow submit vote menggunakan Two-Phase Commit pattern:
1. **Phase 1**: Vote ditulis dengan status `PENDING_RECONCILIATION`
2. **Phase 2**: Counter candidate + election diincrement
3. **Success**: Vote status diupdate ke `CONFIRMED`
4. **Failure**: Vote tetap `PENDING_RECONCILIATION`, background reconciliation job memperbaiki counter

### 6.2 Audit Trail

Setiap operasi sensitif dicatat di collection `auditLogs`:
- Vote submission (dengan reconciliation status)
- Election status changes
- User role changes
- Login/logout events

Audit log bersifat **append-only** — tidak dapat diubah atau dihapus (ditegakkan oleh Firestore Security Rules).

### 6.3 Integrity Verification

- SHA-256 hashing untuk verifikasi integritas data
- HMAC-SHA256 untuk verifikasi autentikasi pesan
- Vote signature generation + verification (mencegah vote forgery)

---

## 7. Device Security

### 7.1 Session Hardening

- **Timeout**: 5 menit inactivity (auto-logout)
- **Step-up Auth**: 2 menit window untuk operasi sensitif (voting)
- **Device Fingerprint**: SHA-256 dari Android ID + Build.FINGERPRINT + random UUID
- **Concurrent Session Detection**: Mendeteksi login dari device lain

### 7.2 Anti-Tampering Detection

- Deteksi rooted device melalui `ro.build.tags` (test-keys)
- Deteksi debugging aktif (`FLAG_DEBUGGABLE`, `FLAG_ALLOW_CLEARTEXT`)
- Deteksi emulator runtime

---

## 8. Incident Response

### 8.1 Security Contacts

- **Email**: security@elsuarku.example.com
- **Response Time**: 24 jam untuk Critical, 72 jam untuk High
- **PGP Key**: Tersedia di SECURITY.md

### 8.2 Vulnerability Disclosure

Lihat [SECURITY.md](../SECURITY.md) untuk kebijakan lengkap.

---

## 9. Compliance

| Framework | Status |
|-----------|--------|
| OWASP Mobile Top 10 (2024) | ✅ Covered |
| OWASP MASVS L1 | ✅ Covered |
| OWASP MASVS L2 | 🔄 In Progress |
| GDPR (Data Protection) | ✅ Covered (lihat DPIA) |
| Indonesia UU ITE | ✅ Covered |
| Indonesia UU PDP | ✅ Covered (lihat Privacy Policy) |

---

*Dokumen ini ditinjau setiap 6 bulan atau setelah perubahan arsitektur signifikan.*
