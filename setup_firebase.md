# FIREBASE CONSOLE SETUP — ElSuarKu

## Tutorial Lengkap Step-by-Step Setup Firebase untuk ElSuarKu

**Estimasi Waktu:** 30-45 menit
**Biaya:** GRATIS (Firebase Spark Plan / Free Tier)
**Prasyarat:** Akun Google, Project ElSuarKu sudah di-build

---

## CHECKPOINT 0: Sebelum Mulai

Yang Anda butuhkan:
- Akun Google/Gmail
- Project ElSuarKu sudah di-build (APK tersedia)
- File `google-services.json` sudah ada di folder `app/`
- File `firestore.rules` sudah ada di root project

Informasi Project Firebase:
- Project Name: ElSuarKu
- Project ID: elsuarku-sistem-vote
- Project Number: 915492781440

---

## STEP 1: Buka Firebase Console

1. Buka browser, masuk ke: https://console.firebase.google.com
2. Login dengan akun Google Anda
3. Cari project "ElSuarKu" atau "elsuarku-sistem-vote"
4. Klik project tersebut

Jika project belum ada: Klik "Add project" -> masukkan nama "ElSuarKu" -> enable Analytics (opsional) -> Create project -> tunggu 2 menit -> Continue

### CHECKPOINT 1:
Anda sekarang berada di dashboard Firebase Console project ElSuarKu.

---

## STEP 2: Firebase Authentication Setup

### 2.1 Buka Menu Authentication
Sidebar kiri -> "Build" -> "Authentication" -> klik "Get started"

### 2.2 Enable Email/Password Sign-In
1. Klik tab "Sign-in method"
2. Cari "Email/Password" -> klik barisnya
3. Toggle "Enable" ke ON
4. Biarkan "Email link (passwordless sign-in)" OFF
5. Klik Save

### 2.3 Enable Google Sign-In
1. Masih di tab "Sign-in method"
2. Cari "Google" -> klik barisnya
3. Toggle "Enable" ke ON
4. Masukkan email support project (email Anda)
5. Klik Save

### 2.4 DAPATKAN WEB CLIENT ID (PENTING!)
1. Setelah Google provider di-enable, klik tombol expand di bagian "Web SDK configuration"
2. Anda akan melihat:
   - Web client ID: XXXXX.apps.googleusercontent.com
   - Web client secret: XXXXX
3. COPY Web client ID tersebut
4. Simpan di Notepad — akan digunakan di Step 7

### CHECKPOINT 2:
- [x] Email/Password sign-in: ENABLED
- [x] Google sign-in: ENABLED
- [x] Web Client ID sudah dicatat

---

## STEP 3: Cloud Firestore Setup

### 3.1 Buka Firestore Database
Sidebar kiri -> "Build" -> "Firestore Database" -> klik "Create database"

### 3.2 Pilih Security Rules Mode
- Pilih "Start in test mode" (rules akan diganti di Step 4)
- Klik Next

### 3.3 Pilih Lokasi Server
- Pilih `asia-southeast2` (Jakarta) untuk latency terendah di Indonesia
- Klik Enable
- Tunggu 1-2 menit sampai database siap

### CHECKPOINT 3:
- [x] Firestore Database: CREATED
- [x] Lokasi: asia-southeast2 (Jakarta)

---

## STEP 4: Firestore Security Rules

### 4.1 Buka Rules Editor
Di Firestore Database, klik tab "Rules". HAPUS semua rules yang ada (test mode rules).

### 4.2 COPY & PASTE Rules RBAC Berikut:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated() {
      return request.auth != null;
    }
    function isAdmin() {
      return isAuthenticated() &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
    }
    function isMonitor() {
      return isAuthenticated() &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'MONITOR';
    }
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    match /users/{userId} {
      allow read: if isOwner(userId) || isAdmin() || isMonitor();
      allow create: if isOwner(userId) || isAdmin();
      allow update: if isOwner(userId) || isAdmin();
      allow delete: if false;
    }

    match /elections/{electionId} {
      allow read: if isAuthenticated();
      allow create: if isAdmin();
      allow update: if isAdmin();
      allow delete: if isAdmin();
    }

    match /candidates/{candidateId} {
      allow read: if isAuthenticated();
      allow create: if isAdmin();
      allow update: if isAdmin();
      allow delete: if isAdmin();
    }

    match /votes/{voteId} {
      allow read: if isAuthenticated() &&
        (resource == null || resource.data.userId == request.auth.uid || isAdmin() || isMonitor());
      allow create: if isAuthenticated();
      allow update: if false;
      allow delete: if false;
    }

    match /audit_logs/{logId} {
      allow read: if isAdmin() || isMonitor();
      allow create: if isAuthenticated();
      allow update: if false;
      allow delete: if false;
    }
  }
}
```

3. Klik "Publish"
4. Tunggu beberapa detik sampai rules ter-deploy — pastikan tidak ada error merah

### CHECKPOINT 4:
- [x] Rules RBAC sudah dipublish
- [x] Tidak ada error di rules editor

---

## STEP 5: Firestore Composite Indexes

PENTING: Firestore memerlukan composite index untuk query dengan multiple filter + orderBy. Tanpa index ini, query akan GAGAL dengan error "FAILED_PRECONDITION: The query requires an index".

### 5.1 Buka Composite Indexes
Firestore Database -> tab "Indexes" -> tab "Composite"

### 5.2 Buat 4 Composite Indexes

**Index #1 — candidates query:**
Klik "Add Index", isi:
- Collection ID: `candidates`
- Field 1: `electionId` (Ascending)
- Field 2: `status` (Ascending)
- Field 3: `nomorUrut` (Ascending)
- Query scope: Collection
- Klik "Create Index"

**Index #2 — votes query:**
Klik "Add Index", isi:
- Collection ID: `votes`
- Field 1: `userId` (Ascending)
- Field 2: `electionId` (Ascending)
- Query scope: Collection
- Klik "Create Index"

**Index #3 — audit_logs severity query:**
Klik "Add Index", isi:
- Collection ID: `audit_logs`
- Field 1: `severity` (Ascending)
- Field 2: `timestamp` (Descending)
- Query scope: Collection
- Klik "Create Index"

**Index #4 — audit_logs actor query:**
Klik "Add Index", isi:
- Collection ID: `audit_logs`
- Field 1: `actorId` (Ascending)
- Field 2: `timestamp` (Descending)
- Query scope: Collection
- Klik "Create Index"

### 5.3 TUNGGU Sampai Semua Index "Enabled"
Setiap index butuh 2-5 menit untuk build. Status: "Building" -> "Enabled". JANGAN lanjut sebelum semua enabled!

### CHECKPOINT 5:
- [x] Index candidates (electionId + status + nomorUrut): ENABLED
- [x] Index votes (userId + electionId): ENABLED
- [x] Index audit_logs (severity + timestamp): ENABLED
- [x] Index audit_logs (actorId + timestamp): ENABLED

---

## STEP 6: Firebase App Check (Production)

### 6.1 Buka App Check
Sidebar kiri -> "Build" -> "App Check" -> klik "Get started"

### 6.2 Register Play Integrity
1. Pilih "Play Integrity" sebagai provider
2. Klik "Register"
3. Biarkan default token TTL (1 hour)
4. Klik "Save"

### 6.3 (Opsional) Debug Token untuk Development
1. Tab "Apps" di App Check
2. Cari app Android Anda (com.example.elsuarku)
3. Klik tiga titik (menu) -> "Manage debug token"
4. Klik "Generate debug token"
5. COPY debug token yang muncul
6. Buka file `app/src/main/java/com/example/elsuarku/utils/Constants.kt`
7. Update: `const val APP_CHECK_DEBUG_TOKEN = "TOKEN_YANG_DICOPY"`
8. Rebuild APK

### CHECKPOINT 6:
- [x] App Check: REGISTERED (Play Integrity)
- [x] Debug token disalin (opsional — untuk development)

---

## STEP 7: Update Web Client ID di strings.xml

### 7.1 Buka file
```
app/src/main/res/values/strings.xml
```

### 7.2 Cari dan update baris default_web_client_id
Cari:
```xml
<string name="default_web_client_id">915492781440-XXXXX.apps.googleusercontent.com</string>
```

Ganti dengan Web Client ID yang dicatat di Step 2.4:
```xml
<string name="default_web_client_id">[PASTE_WEB_CLIENT_ID_DISINI]</string>
```

### 7.3 Rebuild APK
```bash
./gradlew assembleDebug
```
Install ulang APK yang baru.

### CHECKPOINT 7:
- [x] default_web_client_id sudah diupdate dengan Web Client ID yang benar
- [x] APK sudah di-rebuild

---

## STEP 8: Verifikasi Final

### Checklist Verifikasi:

| No | Item | Status |
|----|------|--------|
| 1 | Authentication Email/Password enabled | [ ] |
| 2 | Authentication Google enabled | [ ] |
| 3 | Web Client ID di-copy dan diupdate | [ ] |
| 4 | Firestore Database created (asia-southeast2) | [ ] |
| 5 | Security Rules RBAC published | [ ] |
| 6 | Index #1 (candidates) enabled | [ ] |
| 7 | Index #2 (votes) enabled | [ ] |
| 8 | Index #3 (audit_logs severity) enabled | [ ] |
| 9 | Index #4 (audit_logs actorId) enabled | [ ] |
| 10 | App Check registered | [ ] |

### Uji Coba Aplikasi:

1. Install APK di device/emulator
2. Buka aplikasi -> klik tombol **[Dev] Seed Test Users** di layar login
3. Login sebagai Admin:
   - Email: `admin@elsuarku.id`
   - Password: `Admin123!`
   - Dashboard admin akan auto-create demo data (1 election + 3 candidates)
4. Logout -> Login sebagai Pemilih:
   - Email: `pemilih@elsuarku.id`
   - Password: `Pemilih123!`
   - Pilih pemilihan -> pilih kandidat -> verifikasi biometrik -> konfirmasi -> submit vote
5. Logout -> Login sebagai Monitor:
   - Email: `monitor@elsuarku.id`
   - Password: `Monitor123!`
   - Lihat live statistics -> lihat audit logs

### CHECKPOINT 8 (FINAL):
Semua checklist tercentang. Aplikasi berjalan normal.

---

## TROUBLESHOOTING

### Error: "PERMISSION_DENIED: Missing or insufficient permissions"
Penyebab: Firestore security rules memblokir request.

Solusi:
1. Cek Step 4 — pastikan rules RBAC sudah dipublish
2. Pastikan user sudah login (authenticated) sebelum mencoba baca/tulis data
3. Untuk admin operations, pastikan role di Firestore document adalah "ADMIN"

### Error: "FAILED_PRECONDITION: The query requires an index"
Penyebab: Composite index belum dibuat.

Solusi:
1. Baca error message lengkap — Firebase akan memberikan URL untuk membuat index
2. Buka URL tersebut (langsung mengarah ke Firebase Console)
3. Klik "Create Index"
4. Tunggu 2-5 menit sampai index selesai build
5. Atau: cek Step 5 — pastikan semua 4 index sudah dibuat

### Error: Google Sign-In "DEVELOPER_ERROR"
Penyebab: Web Client ID di strings.xml salah atau tidak sesuai.

Solusi:
1. Firebase Console -> Authentication -> Sign-in method -> Google
2. Expand "Web SDK configuration"
3. Copy Web Client ID
4. Update `default_web_client_id` di `strings.xml`
5. Rebuild APK

### Error: Aplikasi force close saat startup
Penyebab: Firebase tidak ter-initialize dengan benar.

Solusi:
1. Pastikan `google-services.json` ada di folder `app/`
2. Bersihkan dan rebuild:
   ./gradlew clean
   ./gradlew assembleDebug

### Error: Vote gagal (sudah memilih)
Penyebab: User sudah memberikan suara di pemilihan tersebut (one user = one vote).
Solusi: Gunakan akun test berbeda, atau buat pemilihan baru.

### Error: "Biometric: No hardware / Not enrolled"
Penyebab: Device/emulator tidak support biometrik.
Solusi: Aplikasi akan menampilkan warning dan melanjutkan tanpa verifikasi biometrik.

### Error: App Check token invalid
Penyebab: App Check memblokir request dari app yang tidak terverifikasi.
Solusi:
1. Development: generate debug token di Step 6.3, tambahkan ke Constants.kt
2. Production: pastikan app terdaftar di Google Play Console

---

## URL CEPAT FIREBASE CONSOLE

| Menu | URL |
|------|-----|
| Dashboard | https://console.firebase.google.com/project/elsuarku-sistem-vote |
| Authentication | https://console.firebase.google.com/project/elsuarku-sistem-vote/authentication |
| Firestore | https://console.firebase.google.com/project/elsuarku-sistem-vote/firestore |
| App Check | https://console.firebase.google.com/project/elsuarku-sistem-vote/appcheck |

---

Tutorial selesai.
Project: ElSuarKu — Cloud-Based Secure E-Voting Platform
Last Updated: 2026-06-17
 