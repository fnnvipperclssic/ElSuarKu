# Privacy Policy / Kebijakan Privasi

**Terakhir diperbarui**: 24 Juni 2026

---

## 1. Pengantar

ElSuarKu ("kami", "aplikasi") berkomitmen untuk melindungi privasi pengguna ("Anda"). Kebijakan ini menjelaskan data apa yang kami kumpulkan, bagaimana kami menggunakannya, dan hak Anda terkait data tersebut.

Kebijakan ini disusun sesuai dengan:
- **UU No. 27 Tahun 2022** tentang Pelindungan Data Pribadi (UU PDP) — Indonesia
- **General Data Protection Regulation (GDPR)** — EU
- **OWASP MASVS** privacy requirements

---

## 2. Data yang Dikumpulkan

### 2.1 Data yang Anda Berikan

| Data | Tujuan | Dasar Hukum |
|------|--------|-------------|
| Email | Autentikasi, komunikasi | Consent + Contract |
| Nama lengkap | Identifikasi akun | Contract |
| Foto profil (opsional) | Personalisasi | Consent |
| Pilihan suara (terenkripsi) | Penghitungan suara | Contract + Legitimate Interest |

### 2.2 Data yang Dikumpulkan Otomatis

| Data | Tujuan | Retention |
|------|--------|-----------|
| Device fingerprint (hash) | Concurrent session detection | 30 menit idle |
| App Check token | Anti-abuse | Session duration |
| Crash reports (anonim) | Debugging | 90 hari |
| Performance metrics (anonim) | Monitoring | 90 hari |

### 2.3 Data yang TIDAK Dikumpulkan

- **Lokasi GPS** — tidak digunakan
- **Kontak** — tidak diakses
- **Riwayat browsing** — tidak dilacak
- **Pilihan suara individual** — admin hanya melihat agregat, bukan pilihan per individu

---

## 3. Enkripsi dan Keamanan Data

### 3.1 Enkripsi Vote
- Pilihan suara dienkripsi dengan **AES-256-GCM** di perangkat Anda
- Kunci enkripsi disimpan di **Android Keystore** (hardware-backed TEE/StrongBox)
- Kunci **tidak pernah** meninggalkan perangkat Anda
- Hanya ciphertext yang dikirim ke server — kami tidak dapat mendekripsi pilihan Anda

### 3.2 Data in Transit
- Semua komunikasi menggunakan **TLS 1.3** dengan certificate pinning
- SSL interception detection aktif untuk mendeteksi MITM

### 3.3 Data at Rest
- Data Firestore dienkripsi secara default oleh Google Cloud
- Backup dienkripsi

---

## 4. Penggunaan Data

| Tujuan | Data | Pihak Ketiga |
|--------|------|-------------|
| Autentikasi | Email, nama | Firebase Auth (Google) |
| Voting | Encrypted choice (ciphertext only) | Firestore (Google Cloud) |
| Notifikasi | FCM token | Firebase Cloud Messaging (Google) |
| Monitoring performa | Anonymized metrics | Firebase Performance (Google) |
| Crash reporting | Anonymized stack traces | Firebase Crashlytics (Google) |

---

## 5. Hak Anda

Berdasarkan UU PDP dan GDPR, Anda memiliki hak:

| Hak | Cara Menggunakan |
|-----|-----------------|
| **Akses** data pribadi | Hubungi admin organisasi |
| **Koreksi** data tidak akurat | Edit profil di aplikasi |
| **Hapus** data pribadi | Hubungi admin + hapus akun |
| **Batasi** pemrosesan | Nonaktifkan fitur opsional di Settings |
| **Portabilitas** data | Ekspor data (tersedia dalam rilis mendatang) |
| **Keberatan** pemrosesan | Uninstall aplikasi + hapus akun |

Untuk menggunakan hak-hak ini, hubungi: **privacy@elsuarku.example.com**

---

## 6. Retensi Data

| Data | Retensi |
|------|---------|
| Data akun | Sampai akun dihapus |
| Suara (terenkripsi) | 90 hari setelah pemilihan berakhir |
| Audit log | 1 tahun |
| Device fingerprint | 30 menit setelah logout/session timeout |
| Crash reports | 90 hari |

Lihat [Data Retention Schedule](DATA_RETENTION.md) untuk detail lengkap.

---

## 7. Anak-Anak

ElSuarKu tidak ditujukan untuk pengguna di bawah 13 tahun. Kami tidak secara sadar mengumpulkan data dari anak di bawah 13 tahun.

---

## 8. Perubahan Kebijakan

Perubahan kebijakan akan diberitahukan melalui:
1. In-app announcement
2. Email (untuk perubahan signifikan)
3. Update dokumen ini

---

## 9. Kontak

**Data Protection Officer**: dpo@elsuarku.example.com
**Privacy inquiries**: privacy@elsuarku.example.com
**Alamat**: (TBD — sesuai lokasi organisasi)
