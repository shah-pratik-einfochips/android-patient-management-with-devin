package com.example.patienthealth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.patienthealth.data.PatientExport
import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.VitalMetric
import com.example.patienthealth.domain.VitalReading
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun patient(medicalId: String = "TEST-001") = Patient(
        firstName = "Synthetic", lastName = "Patient", medicalId = medicalId,
        birthDate = "1990-01-01", consentGranted = true,
        vitals = listOf(
            VitalReading(
                timestamp = 1_700_000_000_000,
                values = mapOf(VitalMetric.HEART_RATE to 70.0, VitalMetric.OXYGEN to 90.0),
                symptoms = "note, with comma",
            ),
        ),
    )

    @Test fun csvContainsEscapesAndRows() {
        val file = PatientExport.writeCsv(patient(), context.cacheDir)
        assertTrue(file.isFile && file.length() > 0)
        val text = file.readText()
        assertTrue(text.contains("reading_date,metric,value,unit"))
        assertTrue(text.lines().any { it.contains("Heart rate") && it.contains("70") })
        assertTrue(text.contains("yes"))
        assertTrue(text.contains("\"note, with comma\""))
    }

    @Test fun pdfIsCreatedWithHeader() {
        val file = PatientExport.writePdf(patient(), context.cacheDir)
        assertTrue(file.isFile && file.length() > 0)
        file.inputStream().use { stream ->
            val header = ByteArray(4).also { stream.read(it) }
            assertEquals("%PDF", header.toString(Charsets.US_ASCII))
        }
    }

    @Test fun medicalIdWithPathSeparatorsProducesValidFile() {
        val csv = PatientExport.writeCsv(patient("PT/2024/001"), context.cacheDir)
        val pdf = PatientExport.writePdf(patient("PT/2024/001"), context.cacheDir)
        assertFalse(csv.name.contains('/'))
        assertFalse(pdf.name.contains('/'))
        assertTrue(csv.isFile && pdf.isFile)
    }
}
