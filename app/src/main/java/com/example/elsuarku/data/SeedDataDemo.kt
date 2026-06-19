package com.example.elsuarku.data

import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.data.model.Election
import com.example.elsuarku.data.model.ElectionStatus
import com.example.elsuarku.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * One-click demo data seeder — Admin pakai untuk testing.
 * Membuat 1 pemilihan + 3 kandidat langsung ke Firestore.
 */
object SeedDataDemo {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun seed(adminUid: String): String {
        return try {
            // 1. Buat Election
            val now = System.currentTimeMillis()
            val election = Election(
                id = firestore.collection(Constants.COLLECTION_ELECTIONS).document().id,
                title = "Pemilihan Ketua OSIS 2026",
                description = "Pilih ketua OSIS terbaik untuk periode 2026/2027. Gunakan hak suara Anda dengan bijak!",
                startDate = now,
                endDate = now + 7 * 24 * 60 * 60 * 1000L, // 7 hari
                status = ElectionStatus.ACTIVE,
                createdBy = adminUid,
                totalVoters = 500,
                votedCount = 0
            )
            firestore.collection(Constants.COLLECTION_ELECTIONS)
                .document(election.id).set(election).await()

            // 2. Buat 3 Candidates
            val candidates = listOf(
                Candidate(
                    id = firestore.collection(Constants.COLLECTION_CANDIDATES).document().id,
                    electionId = election.id,
                    name = "Andi Pratama",
                    photoBase64 = "",
                    description = "Calon dengan visi modern dan inovatif",
                    visi = "Mewujudkan OSIS yang transparan, modern, dan melayani seluruh siswa",
                    misi = "1. Digitalisasi program kerja OSIS\n2. Meningkatkan komunikasi antar kelas\n3. Program beasiswa internal",
                    nomorUrut = 1,
                    voteCount = 0
                ),
                Candidate(
                    id = firestore.collection(Constants.COLLECTION_CANDIDATES).document().id,
                    electionId = election.id,
                    name = "Siti Nurhaliza",
                    photoBase64 = "",
                    description = "Pemimpin perempuan yang peduli dan berdedikasi",
                    visi = "OSIS inklusif yang menjadi rumah bagi seluruh siswa tanpa terkecuali",
                    misi = "1. Program mentoring akademik\n2. Kegiatan seni dan budaya rutin\n3. Layanan pengaduan siswa 24/7",
                    nomorUrut = 2,
                    voteCount = 0
                ),
                Candidate(
                    id = firestore.collection(Constants.COLLECTION_CANDIDATES).document().id,
                    electionId = election.id,
                    name = "Budi Santoso",
                    photoBase64 = "",
                    description = "Aktivis muda dengan segudang prestasi",
                    visi = "Membawa OSIS ke tingkat nasional dengan prestasi dan inovasi",
                    misi = "1. Club debat dan olimpiade sains\n2. Kerjasama dengan sekolah internasional\n3. Program entrepreneurship siswa",
                    nomorUrut = 3,
                    voteCount = 0
                )
            )
            for (c in candidates) {
                firestore.collection(Constants.COLLECTION_CANDIDATES)
                    .document(c.id).set(c).await()
            }

            "✅ Berhasil! 1 pemilihan + 3 kandidat dibuat. Refresh dashboard."
        } catch (e: Exception) {
            "❌ Gagal: ${e.localizedMessage ?: "unknown error"}. Pastikan Firestore rules allow write."
        }
    }
}
