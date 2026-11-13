package com.example.patienthealth

import com.example.patienthealth.domain.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PatientTest {
    private val now = Instant.parse("2026-10-28T12:00:00Z").toEpochMilli()
    private fun patient() = Patient(firstName = "Synthetic", lastName = "Patient", medicalId = "TEST-001", birthDate = "1990-01-01", consentGranted = true)

    @Test fun validPatient() { assertTrue(patient().validationErrors(LocalDate.of(2026, 10, 28)).isEmpty()) }
    @Test fun missingFields() { assertEquals(5, Patient().validationErrors().size) }
    @Test fun whitespaceFields() { assertEquals(3, patient().copy(firstName = " ", lastName = "\n", medicalId = "\t").validationErrors().size) }
    @Test fun futureBirthDate() { assertFalse(patient().copy(birthDate = "2999-01-01").validationErrors().isEmpty()) }
    @Test fun impossibleBirthDate() { assertFalse(patient().copy(birthDate = "2026-02-30").validationErrors().isEmpty()) }
    @Test fun invalidEmail() { assertFalse(patient().copy(email = "bad@").validationErrors().isEmpty()) }
    @Test fun validEmail() { assertTrue(patient().copy(email = "test@example.com").validationErrors().isEmpty()) }
    @Test fun consentRequired() { assertFalse(patient().copy(consentGranted = false).validationErrors().isEmpty()) }
    @Test fun examinationOptions() {
        assertEquals(listOf("Routine checkup", "Emergency", "Follow-up", "Consultation", "Diagnostic", "Preventive"), examinationTypes)
        examinationTypes.forEach { assertTrue(patient().copy(examinationType = it).validationErrors().isEmpty()) }
    }
    @Test fun invalidExamination() { assertFalse(patient().copy(examinationType = "Invalid").validationErrors().isEmpty()) }
    @Test fun medicalIdNormalization() { assertEquals("test-001", patient().copy(medicalId = " TEST-001 ").normalizedMedicalId) }
    @Test fun conversions() {
        assertEquals(45.359237, UnitConversion.kilograms(100.0), 0.000001)
        assertEquals(177.8, UnitConversion.centimeters(5.0, 10.0), 0.000001)
        assertEquals(37.0, UnitConversion.celsius(98.6), 0.000001)
        assertEquals(0.0, UnitConversion.celsius(32.0), 0.000001)
    }
    @Test fun strictDecimalInput() {
        listOf("70abc", "NaN", "Infinity", "1,000", "1 2", "", "1.2.3", "1e3").forEach { assertNull(it, decimalInput(it, Locale.US)) }
        assertEquals(70.5, decimalInput(" 70.5 ", Locale.US)!!, 0.000001)
        assertEquals(37.5, decimalInput("37,5", Locale.FRANCE)!!, 0.000001)
    }
    @Test fun validVitals() {
        val reading = VitalReading(timestamp = now, values = mapOf(VitalMetric.SYSTOLIC to 120.0, VitalMetric.DIASTOLIC to 80.0, VitalMetric.PAIN to 0.0))
        assertTrue(reading.validationErrors("1990-01-01", now).isEmpty())
    }
    @Test fun mapCalculation() {
        assertEquals(93.333333, VitalReading(values = mapOf(VitalMetric.SYSTOLIC to 120.0, VitalMetric.DIASTOLIC to 80.0)).meanArterialPressure!!, 0.00001)
        assertNull(VitalReading(values = mapOf(VitalMetric.SYSTOLIC to 120.0)).meanArterialPressure)
    }
    @Test fun emptyVitalsRejected() { assertFalse(VitalReading(timestamp = now).validationErrors("1990-01-01", now).isEmpty()) }
    @Test fun partialBloodPressureRejected() {
        listOf(VitalMetric.SYSTOLIC, VitalMetric.DIASTOLIC).forEach {
            assertFalse(VitalReading(timestamp = now, values = mapOf(it to 100.0)).validationErrors("1990-01-01", now).isEmpty())
        }
    }
    @Test fun invertedBloodPressureRejected() {
        assertFalse(VitalReading(timestamp = now, values = mapOf(VitalMetric.SYSTOLIC to 80.0, VitalMetric.DIASTOLIC to 120.0)).validationErrors("1990-01-01", now).isEmpty())
    }
    @Test fun invalidVitalsRejected() {
        VitalMetric.entries.forEach { metric ->
            listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, metric.minimum - 1, metric.maximum + 1).forEach {
                assertFalse("$metric: $it", VitalReading(timestamp = now, values = mapOf(metric to it)).validationErrors("1990-01-01", now).isEmpty())
            }
        }
    }
    @Test fun futureReadingRejected() {
        assertFalse(VitalReading(timestamp = now + 1, values = mapOf(VitalMetric.HEART_RATE to 70.0)).validationErrors("1990-01-01", now).isEmpty())
    }
    @Test fun readingBeforeBirthRejected() {
        assertFalse(VitalReading(timestamp = 0, values = mapOf(VitalMetric.HEART_RATE to 70.0)).validationErrors("1990-01-01", now).isEmpty())
    }
    @Test fun referenceRanges() {
        assertTrue(VitalMetric.HEART_RATE.outsideReference(130.0))
        assertFalse(VitalMetric.HEART_RATE.outsideReference(70.0))
        assertFalse(VitalMetric.HEIGHT.outsideReference(180.0))
    }
    @Test fun serializationRoundTrip() {
        val value = patient().copy(examinationType = "Follow-up", vitals = listOf(VitalReading(values = mapOf(VitalMetric.HEART_RATE to 70.0))))
        assertEquals(value, Json.decodeFromString<Patient>(Json.encodeToString(value)))
    }
    @Test fun draftConvertsSelectedUnits() {
        val draft = VitalDraft(timestamp = now, inputs = mapOf(VitalMetric.HEIGHT to "5", VitalMetric.WEIGHT to "100", VitalMetric.TEMPERATURE to "98.6"), heightUnit = "ft/in", inches = "10", weightUnit = "lbs", temperatureUnit = "F")
        assertTrue(draft.errors("1990-01-01", now, Locale.US).isEmpty())
        assertEquals(177.8, draft.reading(Locale.US).values.getValue(VitalMetric.HEIGHT), 0.000001)
        assertEquals(45.359237, draft.reading(Locale.US).values.getValue(VitalMetric.WEIGHT), 0.000001)
        assertEquals(37.0, draft.reading(Locale.US).values.getValue(VitalMetric.TEMPERATURE), 0.000001)
    }
    @Test fun draftRejectsInvalidNumberAlongsideValidNumber() {
        val draft = VitalDraft(timestamp = now, inputs = mapOf(VitalMetric.HEART_RATE to "70", VitalMetric.PAIN to "bad"))
        assertFalse(draft.errors("1990-01-01", now, Locale.US).isEmpty())
    }
    @Test fun draftRejectsInvalidInches() {
        listOf("12", "-1", "bad").forEach {
            assertFalse(VitalDraft(timestamp = now, inputs = mapOf(VitalMetric.HEIGHT to "5"), heightUnit = "ft/in", inches = it).errors("1990-01-01", now).isEmpty())
        }
    }
    @Test fun draftRejectsInchesWithoutFeet() {
        assertFalse(VitalDraft(timestamp = now, inputs = mapOf(VitalMetric.HEART_RATE to "70"), heightUnit = "ft/in", inches = "6").errors("1990-01-01", now).isEmpty())
    }
    @Test fun sevenDayWindowIncludesBoundaryAndExcludesFuture() {
        val zone = ZoneId.of("UTC")
        val start = Instant.parse("2026-10-22T00:00:00Z").toEpochMilli()
        val records = listOf(VitalReading(timestamp = now + 1), VitalReading(timestamp = start - 1), VitalReading(timestamp = now), VitalReading(timestamp = start))
        assertEquals(listOf(start, now), recentReadings(records, now, zone).map { it.timestamp })
    }
}
