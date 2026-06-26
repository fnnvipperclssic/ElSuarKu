package com.example.elsuarku.utils

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.print.*
import android.print.pdf.PrintedPdfDocument
import android.util.DisplayMetrics
import com.example.elsuarku.data.model.Candidate
import com.example.elsuarku.data.model.Election
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates PDF reports for election results.
 *
 * Uses Android's built-in PdfDocument API — no external dependency.
 * Produces an A4-sized PDF file in the Downloads directory.
 *
 * @param context Android context for display metrics
 */
class PdfExportUtil(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics

    /**
     * Generate an election results PDF and return the file path.
     *
     * @param election The election to report
     * @param candidates Candidate list with vote counts (sorted descending)
     * @param totalVoters Total registered voters
     * @param totalVotes Total votes cast
     * @return Generated PDF file, or null on failure
     */
    fun exportElectionResults(
        election: Election,
        candidates: List<Candidate>,
        totalVoters: Int,
        totalVotes: Int
    ): File? {
        val document = PrintedPdfDocument(context, createPageAttributes())
        val page = document.startPage(0)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var y = 50f

        // Header
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.parseColor("#1A237E")
        canvas.drawText("LAPORAN HASIL PEMILIHAN", 40f, y, paint)
        y += 30f

        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#333333")
        canvas.drawText(election.title, 40f, y, paint)
        y += 22f
        canvas.drawText("Periode: ${dateFormat.format(Date(election.startDate))} - ${dateFormat.format(Date(election.endDate))}", 40f, y, paint)
        y += 22f
        canvas.drawText("Status: ${election.status}", 40f, y, paint)
        y += 32f

        // Divider
        paint.color = Color.parseColor("#E0E0E0")
        paint.strokeWidth = 1f
        canvas.drawLine(40f, y, canvas.width - 40f, y, paint)
        y += 24f

        // Participation summary
        paint.textSize = 16f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.parseColor("#1A237E")
        canvas.drawText("Ringkasan Partisipasi", 40f, y, paint)
        y += 26f

        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#555555")
        val participationPct = if (totalVoters > 0) (totalVotes.toFloat() / totalVoters * 100f) else 0f
        canvas.drawText("Total Pemilih Terdaftar: $totalVoters", 40f, y, paint)
        y += 18f
        canvas.drawText("Total Suara Masuk: $totalVotes (%.1f%%)".format(participationPct), 40f, y, paint)
        y += 28f

        // Divider
        paint.color = Color.parseColor("#E0E0E0")
        paint.strokeWidth = 1f
        canvas.drawLine(40f, y, canvas.width - 40f, y, paint)
        y += 24f

        // Results table header
        paint.textSize = 16f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.parseColor("#1A237E")
        canvas.drawText("Hasil Pemilihan", 40f, y, paint)
        y += 28f

        // Table header
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.parseColor("#FFFFFF")
        drawTableRow(canvas, listOf("No", "Nama Kandidat", "Jumlah Suara", "Persentase"), false, y, paint)
        y += 22f

        // Table rows
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#333333")
        candidates.forEachIndexed { index, candidate ->
            val pct = if (totalVotes > 0) (candidate.voteCount.toFloat() / totalVotes * 100f) else 0f
            drawTableRow(canvas, listOf(
                "${index + 1}",
                candidate.name.take(30),
                "${candidate.voteCount}",
                "%.1f%%".format(pct)
            ), index % 2 == 0, y, paint)
            y += 20f
        }

        // Footer
        y = canvas.height - 60f
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#999999")
        canvas.drawText("Dicetak pada: ${dateFormat.format(Date())}", 40f, y, paint)
        y += 14f
        canvas.drawText("ElSuarKu — Platform E-Voting Aman Berbasis Cloud", 40f, y, paint)

        document.finishPage(page)

        val downloadsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        } else {
            @Suppress("DEPRECATION")
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } ?: context.filesDir

        val fileName = "Hasil_${election.title.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val file = File(downloadsDir, fileName)

        return try {
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            file
        } catch (e: Exception) {
            null
        } finally {
            document.close()
        }
    }

    private fun createPageAttributes(): PrintAttributes {
        val density = displayMetrics.densityDpi
        val pageWidth = (8.27f * density).toInt()  // A4 width in points
        val pageHeight = (11.69f * density).toInt() // A4 height
        return PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", density, density))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
    }

    private fun drawTableRow(
        canvas: Canvas,
        values: List<String>,
        isEven: Boolean,
        y: Float,
        paint: Paint
    ) {
        val colWidths = listOf(28f, 200f, 80f, 64f)
        val columns = listOf(values[0], values[1], values[2], values[3])
        var x = 40f

        // Row background
        paint.style = Paint.Style.FILL
        paint.color = if (isEven) Color.parseColor("#F5F5F5") else Color.WHITE
        canvas.drawRect(x, y - 16f, x + colWidths.sum() + 4f, y + 6f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#333333")
        columns.forEachIndexed { index, value ->
            canvas.drawText(value, x + 4f, y, paint)
            x += colWidths[index]
        }
    }
}
