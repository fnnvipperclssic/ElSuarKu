# ElSuarKu — Cloud-Based Secure E-Voting Platform

[![CI](https://github.com/username/elsuarku/workflows/CI/badge.svg)](https://github.com/username/elsuarku/actions)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-BOM%202024.12.01-green)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-BoM%2033.7.0-orange)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](./LICENSE)

**ElSuarKu** (El Suara Kamu — "Your Voice") adalah platform e-voting aman berbasis cloud untuk pemilihan internal organisasi. Dibangun dengan Kotlin Multiplatform-ready arsitektur dan Firebase backend, dirancang untuk memenuhi standar keamanan tertinggi.

---

## 🎯 Fitur Utama

| Kategori | Fitur |
|----------|-------|
| **Keamanan** | AES-256-GCM enkripsi vote, Android Keystore (TEE/StrongBox), certificate pinning, SSL interception detection, key attestation, biometric authentication |
| **Integritas** | Two-Phase Commit voting, reconciliation engine, hash-based integrity verification, HMAC signature |
| **Transparansi** | Real-time hasil, audit trail append-only, verifiable vote count |
| **Manajemen** | Dashboard admin, election template system, auto-close expired elections, voter turnout analytics |
| **Komunikasi** | FCM push notifications, in-app announcements, election reminders |
| **UX** | Material 3, onboarding flow, skeleton loading, haptic feedback, animated transitions, pull-to-refresh |
| **DevOps** | CI/CD (GitHub Actions), detekt + ktlint, Crashlytics, Performance Monitoring, StrictMode |

---

## 🏗️ Arsitektur

```
┌──────────────────────────────────────────────────────┐
│                   Presentation Layer                  │
│  Compose UI → ViewModel (MVVM) → Screen composables │
├──────────────────────────────────────────────────────┤
│                     Domain Layer                      │
│  Repository Interfaces → UseCases → Domain Entities  │
├──────────────────────────────────────────────────────┤
│                      Data Layer                       │
│  Firebase Firestore / Auth / Storage / Functions     │
│  Repository Implementations → Firebase Mappers       │
├──────────────────────────────────────────────────────┤
│                   Security Layer                      │
│  EncryptionManager → KeyAttestation → SessionManager │
│  CertificatePinner → SslDetector → AntiTampering     │
└──────────────────────────────────────────────────────┘
```

**Tech Stack**: Kotlin · Jetpack Compose · Firebase (Auth, Firestore, Storage, Functions, App Check, Crashlytics, Performance) · Vico Charts · Coroutines + Flow · Manual DI

Lihat [docs/adr/](docs/adr/) untuk Architecture Decision Records.

---

## 🚀 Quick Start

### Prasyarat

- Android Studio Hedgehog (2024.1) atau lebih baru
- JDK 17
- Firebase project dengan Blaze plan (untuk Cloud Functions)
- Google Play Console (untuk App Check — Play Integrity)

### Setup

```bash
# 1. Clone repository
git clone https://github.com/username/elsuarku.git
cd elsuarku

# 2. Tambahkan google-services.json
#    Download dari Firebase Console → Project Settings → Your apps
#    Letakkan di app/google-services.json

# 3. Deploy Cloud Functions
cd functions
npm install
firebase deploy --only functions

# 4. Deploy Firestore Rules
firebase deploy --only firestore:rules

# 5. Build & Run
cd ..
./gradlew assembleDebug
```

### Konfigurasi Build

```properties
# gradle.properties (local — tidak di-commit)
FIREBASE_API_KEY=your_api_key
APP_CHECK_DEBUG_TOKEN=your_debug_token
```

---

## 📁 Struktur Proyek

```
app/src/main/java/com/example/elsuarku/
├── data/
│   ├── model/           # Firestore entity models
│   └── repository/      # Repository implementations
├── di/                  # Manual dependency injection
├── domain/
│   └── repository/      # Repository interfaces
├── presentation/
│   ├── admin/           # Admin screens
│   ├── auth/            # Login/auth screens
│   ├── components/      # Reusable composables + charts/
│   ├── dashboard/       # User dashboard
│   ├── monitor/         # Real-time election monitor
│   ├── onboarding/      # First-run onboarding
│   ├── settings/        # Settings screen
│   └── voting/          # Vote flow
├── security/            # Encryption, attestation, session, etc.
├── service/             # FCM service
├── ui/theme/            # Material 3 theme
└── utils/               # Haptic, PDF, simulators, etc.

functions/src/           # Firebase Cloud Functions
docs/                    # ADR, diagrams, whitepapers
test/                    # Unit tests with MockK + Turbine
```

---

## 🔒 Keamanan

Lihat [docs/SECURITY_WHITEPAPER.md](docs/SECURITY_WHITEPAPER.md) dan [SECURITY.md](SECURITY.md).

**Highlights:**
- ✅ OWASP Mobile Top 10 (2024) covered
- ✅ Defense-in-depth: 7 layer keamanan
- ✅ Zero-knowledge vote encryption (admin tidak bisa melihat pilihan individual)
- ✅ Certificate pinning + SSL interception detection
- ✅ Rooted device detection + boot state verification

---

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Lint
./gradlew detekt ktlintCheck

# App Check (debug)
./gradlew assembleDebug
```

**Coverage**: ViewModel tests, repository tests, security utility tests, integrity verification tests.

---

## 📚 Dokumentasi

| Doc | Deskripsi |
|-----|-----------|
| [ADR Index](docs/adr/README.md) | Architecture Decision Records |
| [Security Whitepaper](docs/SECURITY_WHITEPAPER.md) | Arsitektur keamanan detail |
| [Firestore Schema](docs/FIRESTORE_SCHEMA.md) | Struktur database |
| [C4 Diagram](docs/C4_ARCHITECTURE.md) | Diagram arsitektur C4 |
| [Privacy Policy](docs/PRIVACY_POLICY.md) | Kebijakan privasi |
| [Terms of Service](docs/TERMS_OF_SERVICE.md) | Syarat penggunaan |
| [DPIA](docs/DPIA.md) | Data Protection Impact Assessment |
| [Contributing](CONTRIBUTING.md) | Panduan kontribusi |

---

## 🤝 Kontribusi

Lihat [CONTRIBUTING.md](CONTRIBUTING.md). PRs, issues, dan diskusi sangat diterima.

---

## 📄 Lisensi

MIT License — lihat [LICENSE](LICENSE).

---

*Dibangun dengan ❤️ untuk integritas pemilihan.*
