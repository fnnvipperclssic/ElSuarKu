# UPLOAD ElSuarKu KE GITHUB — Tutorial Lengkap

## Keamanan Data — File yang DILINDUNGI

File-file berikut **TIDAK AKAN** di-upload ke GitHub (sudah di `.gitignore`):

| File | Berisi Data Sensitif |
|------|---------------------|
| `app/google-services.json` | API Key, OAuth Client ID, Project Number |
| `local.properties` | Path lokal SDK Android |
| `*.keystore`, `*.jks` | Signing keys |
| `.claude/` | Claude Code session data |
| `*.logcat` | Log file dengan data device |
| `app/build/` | Build artifacts |

## Prasyarat

- [x] Akun GitHub (daftar di https://github.com)
- [x] Git terinstall di komputer
- [x] Project ElSuarKu siap

---

## STEP 1: Install Git (Jika Belum)

Cek apakah Git sudah terinstall:
```bash
git --version
```

Jika belum, download dari: https://git-scm.com/download/win

---

## STEP 2: Buka Terminal di Folder Project

```bash
cd C:\Users\ikmal\AndroidStudioProjects\ElSuarKu
```

---

## STEP 3: Inisialisasi Git Repository

```bash
git init
```

Output:
```
Initialized empty Git repository in C:/Users/ikmal/AndroidStudioProjects/ElSuarKu/.git/
```

---

## STEP 4: Tambahkan Semua File ke Staging

```bash
git add .
```

---

## STEP 5: Cek File yang Akan Di-upload

```bash
git status
```

**PASTIKAN** file `app/google-services.json` **TIDAK** muncul di daftar!

Jika muncul, artinya `.gitignore` belum berfungsi. Jalankan:
```bash
git rm --cached app/google-services.json
```

---

## STEP 6: Commit Pertama

```bash
git commit -m "Initial commit: ElSuarKu - Cloud-Based Secure E-Voting Platform"
```

---

## STEP 7: Buat Repository di GitHub

1. Buka https://github.com
2. Login ke akun Anda
3. Klik tombol **"+"** di pojok kanan atas → **"New repository"**
4. Isi form:
   - **Repository name:** `ElSuarKu` (atau nama lain)
   - **Description:** `Cloud-Based Secure E-Voting Platform — Kotlin + Jetpack Compose + Firebase`
   - **Public** atau **Private** (terserah Anda)
   - **JANGAN** centang "Add a README file" (kita sudah punya)
   - **JANGAN** centang ".gitignore" (kita sudah punya)
   - **JANGAN** centang "Choose a license" (opsional, bisa ditambah nanti)
5. Klik **"Create repository"**
6. GitHub akan menampilkan perintah untuk push — **COPY** 3 baris perintah tersebut

---

## STEP 8: Hubungkan & Push ke GitHub

Jalankan 3 perintah yang disalin dari GitHub (contoh):
```bash
git remote add origin https://github.com/USERNAME_ANDA/ElSuarKu.git
git branch -M main
git push -u origin main
```

Ganti `USERNAME_ANDA` dengan username GitHub Anda.

---

## STEP 9: Verifikasi

1. Buka `https://github.com/USERNAME_ANDA/ElSuarKu`
2. Pastikan semua file muncul
3. Pastikan `app/google-services.json` **TIDAK** muncul di repository

---

## SETUP UNTUK DEVELOPER LAIN

Developer yang men-clone project ini perlu:

### 1. Clone repository
```bash
git clone https://github.com/USERNAME_ANDA/ElSuarKu.git
cd ElSuarKu
```

### 2. Buat Firebase Project Sendiri
- Buka https://console.firebase.google.com
- Buat project baru
- Download `google-services.json`
- Letakkan di folder `app/`

### 3. Update Web Client ID
- Buka `app/src/main/res/values/strings.xml`
- Cari `default_web_client_id`
- Ganti `YOUR_WEB_CLIENT_ID.apps.googleusercontent.com` dengan Web Client ID dari Firebase Console

### 4. Setup Firebase Console
- Ikuti tutorial di `setup_firebase.md`

### 5. Build & Run
- Buka project di Android Studio
- Sync Gradle
- Run

---

## RINGKASAN PERINTAH CEPAT

```bash
# 1. Masuk ke folder project
cd C:\Users\ikmal\AndroidStudioProjects\ElSuarKu

# 2. Init git
git init

# 3. Add semua file
git add .

# 4. Cek tidak ada file sensitif
git status

# 5. Commit
git commit -m "Initial commit: ElSuarKu - Secure E-Voting Platform"

# 6. Tambah remote (GANTI USERNAME)
git remote add origin https://github.com/USERNAME_ANDA/ElSuarKu.git

# 7. Push
git branch -M main
git push -u origin main
```

---

## FILE YANG DI-UPLOAD KE GITHUB

```
✅ app/src/                    # Semua source code Kotlin
✅ app/build.gradle.kts        # Build configuration
✅ build.gradle.kts            # Root build config
✅ settings.gradle.kts         # Project settings
✅ gradle/                     # Gradle wrapper
✅ gradle.properties           # Gradle properties
✅ gradlew / gradlew.bat       # Gradle scripts
✅ firestore.rules             # Firestore security rules
✅ google-services.json.template # Template Firebase config
✅ proguard-rules.pro          # ProGuard rules
✅ .gitignore                  # Git ignore rules
✅ setup_firebase.md           # Firebase setup tutorial
✅ GITHUB_UPLOAD.md            # Tutorial ini
✅ ElSuarKu_Project.md         # Project documentation

❌ app/google-services.json    # DATA SENSITIF — TIDAK di-upload
❌ local.properties            # Path lokal — TIDAK di-upload
❌ .claude/                    # Claude session — TIDAK di-upload
❌ app/build/                  # Build output — TIDAK di-upload
```
