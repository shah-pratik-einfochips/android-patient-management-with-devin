package com.example.patienthealth.data

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.VitalMetric
import com.example.patienthealth.domain.VitalReading
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PatientExport {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun formatTimestamp(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(dateFormat)

    fun csv(patient: Patient): String = buildString {
        appendLine("name,medical_id,date_of_birth,gender,examination_type")
        appendLine(
            listOf(patient.name, patient.medicalId, patient.birthDate, patient.gender, patient.examinationType)
                .joinToString(",") { quote(it) }
        )
        appendLine()
        appendLine("reading_date,metric,value,unit,outside_illustrative_range,map,symptoms")
        patient.vitals.sortedBy { it.timestamp }.forEach { reading ->
            val rows = VitalMetric.entries.filter { reading.values.containsKey(it) }
                .ifEmpty { listOf(null) }
            rows.forEachIndexed { index, metric ->
                val value = metric?.let { reading.values[it] }
                appendLine(
                    listOf(
                        formatTimestamp(reading.timestamp),
                        metric?.title ?: "",
                        value?.let { formatValue(it) } ?: "",
                        metric?.unit ?: "",
                        if (metric != null && value != null && metric.outsideReference(value)) "yes" else "",
                        if (index == 0) reading.meanArterialPressure?.let(::formatValue) ?: "" else "",
                        if (index == 0) quote(reading.symptoms) else "",
                    ).joinToString(",") { it }
                )
            }
        }
    }

    private fun quote(text: String): String =
        "\"${text.replace("\"", "\"\"")}\""

    fun formatValue(value: Double): String =
        if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString() else "%.1f".format(value)

    fun writeCsv(patient: Patient, cacheDir: File): File {
        val file = File(exportDir(cacheDir), "patient-${patient.medicalId.ifBlank { patient.id }.take(24)}.csv")
        file.writeText(csv(patient))
        return file
    }

    fun writePdf(patient: Patient, cacheDir: File): File {
        val document = PdfDocument()
        val width = 612
        val height = 792
        val margin = 48f
        val lineHeight = 18f
        val title = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
        val section = Paint().apply { textSize = 14f; typeface = Typeface.DEFAULT_BOLD }
        val body = Paint().apply { textSize = 11f }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
        var y = margin

        fun advance(lines: Int = 1) {
            y += lineHeight * lines
            if (y > height - margin) {
                document.finishPage(page)
                page = document.startPage(PdfDocument.PageInfo.Builder(width, height, ++pageNumber).create())
                y = margin
            }
        }
        fun draw(text: String, paint: Paint = body) {
            page.canvas.drawText(text, margin, y, paint)
            advance()
        }

        draw("${patient.name} (${patient.medicalId})", title); advance()
        listOf(
            "Date of birth: ${patient.birthDate}",
            "Gender: ${patient.gender}",
            "Blood type: ${patient.bloodType}",
            "Examination: ${patient.examinationType}".takeIf { patient.examinationType.isNotBlank() },
            "Allergies: ${patient.allergies}".takeIf { patient.allergies.isNotBlank() },
            "Medications: ${patient.medications}".takeIf { patient.medications.isNotBlank() },
            "Conditions: ${patient.conditions}".takeIf { patient.conditions.isNotBlank() },
        ).forEach { it?.let(::draw) }
        advance()
        draw("Vitals readings (${patient.vitals.size})", section)
        draw("All entries are synthetic prototype data. Reference ranges are illustrative adult values.")
        patient.vitals.sortedBy { it.timestamp }.forEach { reading ->
            advance()
            draw(formatTimestamp(reading.timestamp), section)
            VitalMetric.entries.forEach { metric ->
                reading.values[metric]?.let { value ->
                    draw("  ${metric.title}: ${formatValue(value)} ${metric.unit}${if (metric.outsideReference(value)) " (outside illustrative range)" else ""}")
                }
            }
            reading.meanArterialPressure?.let { draw("  MAP: ${formatValue(it)} mmHg") }
            if (reading.symptoms.isNotBlank()) {
                wrap(reading.symptoms, 88).forEachIndexed { index, line ->
                    draw(if (index == 0) "  Symptoms: $line" else "  $line")
                }
            }
        }
        document.finishPage(page)
        val file = File(exportDir(cacheDir), "patient-${patient.medicalId.ifBlank { patient.id }.take(24)}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun wrap(text: String, width: Int): List<String> =
        text.split(' ').fold(mutableListOf<String>()) { lines, word ->
            val last = lines.lastOrNull()
            if (last == null || last.length + word.length + 1 > width) lines.add(word) else lines[lines.lastIndex] = "$last $word"
            lines
        }

    private fun exportDir(cacheDir: File): File =
        File(cacheDir, "exports").apply { mkdirs() }

    fun clearExports(cacheDir: File) {
        exportDir(cacheDir).deleteRecursively()
    }
}
