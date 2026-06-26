# C4 Architecture Diagram: ElSuarKu

## Level 1: System Context

```
┌─────────────┐        ┌──────────────────────┐        ┌─────────────┐
│             │        │                      │        │             │
│   Voter     │───────▶│    ElSuarKu Platform  │◀───────│   Admin     │
│  (Android)  │  Vote  │  (Firebase GCP)      │ Config │  (Android)  │
│             │◀───────│                      │───────▶│             │
└─────────────┘ Results└──────────────────────┘ Results└─────────────┘
                               │
                               │
                        ┌──────┴──────┐
                        │             │
                        │   Monitor   │
                        │  (Android)  │
                        │  Read-only  │
                        └─────────────┘
```

## Level 2: Container Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                        ElSuarKu Platform                         │
│                                                                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────┐ │
│  │                 │  │                 │  │                  │ │
│  │  Android App    │  │  Cloud Functions │  │  Firebase       │ │
│  │  (Kotlin/       │  │  (Node.js/TS)   │  │  Console        │ │
│  │   Jetpack       │  │                  │  │                  │ │
│  │   Compose)      │  │  • setUserRole   │  │  • Config        │ │
│  │                 │  │  • onUserRoleChg │  │  • Monitoring    │ │
│  │  • MVVM         │  │  • checkExpired  │  │  • Analytics     │ │
│  │  • Manual DI    │  │  • audit hooks   │  │                  │ │
│  └────────┬────────┘  └────────┬─────────┘  └──────────────────┘ │
│           │                    │                                   │
│  ┌────────┴────────────────────┴────────────────────────────┐    │
│  │                      Firebase Services                     │    │
│  │                                                             │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │    │
│  │  │  Firestore   │  │  Auth        │  │  Storage         │ │    │
│  │  │  (NoSQL DB)  │  │  (JWT/Auth)  │  │  (Photos/PDF)    │ │    │
│  │  └──────────────┘  └──────────────┘  └──────────────────┘ │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │    │
│  │  │  App Check   │  │  FCM         │  │  Crashlytics +   │ │    │
│  │  │  (Integrity) │  │  (Push)      │  │  Performance     │ │    │
│  │  └──────────────┘  └──────────────┘  └──────────────────┘ │    │
│  └─────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

## Level 3: Component Diagram (Android App)

```
┌─────────────────────────────────────────────────────────────────┐
│                      Android Application                         │
│                                                                   │
│  ┌───────────────────┐  ┌───────────────────┐                   │
│  │ Presentation Layer│  │  Domain Layer      │                   │
│  │                   │  │                    │                   │
│  │ • Activities(1)   │  │ • UseCases         │                   │
│  │ • ViewModels(4)   │──│ • Repository       │                   │
│  │ • Screens(15+)    │  │   Interfaces (6)   │                   │
│  │ • Components(15+) │  │ • Domain Entities  │                   │
│  └─────────┬─────────┘  └─────────┬──────────┘                   │
│            │                      │                               │
│  ┌─────────┴──────────────────────┴──────────┐                   │
│  │               Data Layer                    │                   │
│  │                                              │                   │
│  │ • Repository Implementations (6)            │                   │
│  │ • Firebase Mappers                          │                   │
│  │ • Data Models (5 entities)                  │                   │
│  └─────────┬────────────────────────────────────┘                   │
│            │                                                        │
│  ┌─────────┴────────────────────────────────────┐                   │
│  │           Security Layer                       │                   │
│  │                                                │                   │
│  │ • EncryptionManager (AES-256-GCM)              │                   │
│  │ • KeyAttestationVerifier                       │                   │
│  │ • SslInterceptionDetector                      │                   │
│  │ • SessionManager (timeout + device fingerprint)│                   │
│  │ • AntiTampering                                │                   │
│  │ • ReconciliationHelper                         │                   │
│  │ • CertificatePinner                            │                   │
│  └────────────────────────────────────────────────┘                   │
│                                                                      │
│  ┌────────────────────────────────────────────────┐                   │
│  │            Infrastructure                       │                   │
│  │                                                │                   │
│  │ • DI (AppModule — manual DI)                   │                   │
│  │ • RetryPolicy + CircuitBreaker                 │                   │
│  │ • ConnectivityBanner (NetworkCallback)         │                   │
│  │ • FCM Service                                  │                   │
│  │ • ImageStorage (Firebase Storage + resize)     │                   │
│  └────────────────────────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────────┘
```

## Level 4: Data Model

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│    User      │     │    Election       │     │  Candidate   │
│              │     │                   │     │              │
│ id: String   │     │ id: String        │     │ id: String   │
│ email: Str   │     │ name: String      │     │ name: String │
│ role: String │     │ status: Status    │◀────│ electionId   │
│ name: String │     │ startDate: Long   │     │ voteCount    │
│ photoUrl?    │     │ endDate: Long     │     │ photoUrl?    │
│ fingerprint? │     │ votedCount: Int   │     │ description  │
└──────┬───────┘     │ maxCandidates     │     └──────┬───────┘
       │             └────────┬─────────┘            │
       │ votes                 │ votes                 │ votes
       ▼                      ▼                      ▼
┌──────────────────────────────────────────────────────────┐
│                        Vote                               │
│                                                           │
│ id: String          voterHash: String (SHA-256)          │
│ electionId: String  candidateId: String                   │
│ encryptedChoice: String (AES-256-GCM ciphertext)         │
│ timestamp: Long     reconciliationStatus: Status          │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│                      AuditLog                             │
│                                                           │
│ id: String          action: String                       │
│ userId: String      electionId: String?                   │
│ timestamp: Long     metadata: Map<String, Any>?           │
│ reconciliationStatus?: String                            │
└──────────────────────────────────────────────────────────┘

Additional collections:
┌──────────────────┐    ┌───────────────────────┐
│ Announcement     │    │ ElectionTemplate      │
│                  │    │                       │
│ id: String       │    │ id: String            │
│ title: String    │    │ name: String          │
│ message: String  │    │ defaultDurationDays   │
│ priority: String │    │ maxCandidates         │
│ isActive: Bool   │    │ isAnonymous           │
│ expiresAt: Long  │    │ requireBiometric      │
│ createdAt: Long  │    │ customFields: List    │
└──────────────────┘    └───────────────────────┘
```
