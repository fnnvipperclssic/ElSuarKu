# ADR-005: AES-256-GCM + Android Keystore untuk Enkripsi

**Status**: Accepted
**Tanggal**: 2026-06-24
**Deciders**: Tim ElSuarKu

## Context

Data vote harus dienkripsi end-to-end untuk memenuhi persyaratan keamanan e-voting:
- **Confidentiality**: Isi suara tidak boleh terbaca oleh siapa pun termasuk admin sistem
- **Integrity**: Suara tidak boleh dimodifikasi setelah dikirim
- **Authenticity**: Suara harus berasal dari pemilih yang sah

Android Keystore menyediakan hardware-backed key storage (TEE/StrongBox) yang merupakan fondasi keamanan mobile.

## Decision

**Menggunakan AES-256-GCM dengan kunci yang disimpan di Android Keystore.**

**Key Generation:**
- Kunci AES-256 dibuat di Android Keystore dengan `KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT`
- `setIsStrongBoxBacked(true)` diutamakan, fallback ke TEE
- `setUserAuthenticationRequired(true)` untuk operasi sensitif (biometric gate)
- `setRandomizedEncryptionRequired(true)` — setiap enkripsi menghasilkan IV unik

**Encryption Flow:**
1. Voter memilih kandidat → `PlainVote(candidateId, timestamp, electionId)`
2. `EncryptionManager.encrypt(plainVote)` → menghasilkan `EncryptedVote(ciphertext, iv, keyAlias)`
3. `EncryptedVote` dikirim ke Firestore melalui VoteRepository
4. Hanya cloud function dengan akses ke key material yang dapat mendekripsi (opsional, untuk verifikasi)

**Key Attestation:**
- X.509 certificate chain diverifikasi untuk memastikan kunci benar-benar hardware-backed
- Boot state diverifikasi (`ro.boot.verifiedbootstate`)
- Device lock status diperiksa

## Consequences

**Positif:**
- Kunci tidak pernah meninggalkan secure hardware (TEE/StrongBox)
- AES-256-GCM menyediakan authenticated encryption (confidentiality + integrity)
- IV random mencegah pattern analysis bahkan untuk vote ke kandidat yang sama
- Biometric gate mencegah akses tidak sah ke kunci enkripsi

**Negatif:**
- StrongBox tidak tersedia di semua device (terutama device low-end)
- User authentication requirement berarti user harus unlock device sebelum voting
- Key rotation membutuhkan migrasi semua vote terenkripsi
- Tidak bisa mendekripsi di server tanpa mengekspor kunci (by design — privacy positif)

## Alternatives Considered

| Alternatif | Kelebihan | Kekurangan | Alasan Ditolak |
|------------|-----------|------------|----------------|
| **RSA + AES Hybrid** | Standar industri | Overhead komputasi lebih tinggi untuk mobile | AES-256-GCM cukup untuk use case |
| **ChaCha20-Poly1305** | Lebih cepat di CPU tanpa AES-NI | Dukungan Keystore lebih terbatas | AES memiliki dukungan hardware acceleration di sebagian besar device Android modern |
| **Plain RSA** | Simple | Tidak cocok untuk payload besar, tidak ada forward secrecy | Tidak memenuhi kebutuhan enkripsi vote |
| **No client-side encryption** | Simple, performan | Melanggar prinsip zero-knowledge, admin bisa melihat isi vote | Tidak dapat diterima untuk sistem voting |
