# Architecture Decision Records (ADR)

> ElSuarKu — Cloud-Based Secure E-Voting Platform

Catatan keputusan arsitektur yang dibuat selama pengembangan. Format mengikuti [Michael Nygard's ADR template](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions).

## Indeks

| # | Judul | Status | Tanggal |
|---|-------|--------|---------|
| 1 | [Firebase sebagai Backend Utama](adr-001-firebase-backend.md) | ✅ Accepted | 2026-06-24 |
| 2 | [MVVM + Manual DI](adr-002-mvvm-manual-di.md) | ✅ Accepted | 2026-06-24 |
| 3 | [Two-Phase Commit untuk Voting](adr-003-two-phase-commit.md) | ✅ Accepted | 2026-06-24 |
| 4 | [Custom Claims untuk Role-Based Access](adr-004-custom-claims.md) | ✅ Accepted | 2026-06-24 |
| 5 | [AES-256-GCM + Android Keystore untuk Enkripsi](adr-005-encryption-strategy.md) | ✅ Accepted | 2026-06-24 |
| 6 | [Certificate Pinning + SSL Interception Detection](adr-006-certificate-pinning.md) | ✅ Accepted | 2026-06-24 |
| 7 | [Firestore sebagai Primary Database](adr-007-firestore-database.md) | ✅ Accepted | 2026-06-24 |
| 8 | [Circuit Breaker + Retry Policy](adr-008-circuit-breaker.md) | ✅ Accepted | 2026-06-24 |

## Template

```markdown
# ADR-NNN: Judul Singkat

**Status**: Proposed | Accepted | Deprecated | Superseded by ADR-XXX
**Tanggal**: YYYY-MM-DD
**Deciders**: [nama]

## Context
Deskripsi masalah dan batasan yang mempengaruhi keputusan.

## Decision
Keputusan yang diambil dan justifikasinya.

## Consequences
Dampak positif, negatif, dan netral dari keputusan ini.

## Alternatives Considered
Alternatif yang dipertimbangkan dan alasan tidak dipilih.
```
