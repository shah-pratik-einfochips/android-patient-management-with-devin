package com.example.patienthealth.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.Serializable

val examinationTypes = listOf("Routine checkup", "Emergency", "Follow-up", "Consultation", "Diagnostic", "Preventive")

@Serializable
data class Patient(
    val id: String = UUID.randomUUID().toString(),
    val firstName: String = "",
    val lastName: String = "",
    val medicalId: String = "",
    val birthDate: String = "",
    val gender: String = "unknown",
    val ethnicity: String = "",
    val phone: String = "",
    val email: String = "",
    val bloodType: String = "Unknown",
    val allergies: String = "",
    val medications: String = "",
    val conditions: String = "",
    val examinationPurpose: String = "",
    val examinationType: String = "",
    val performedBy: String = "",
    val referredBy: String = "",
    val consentGranted: Boolean = false,
    val vitals: List<VitalReading> = emptyList(),
) {
    val name: String get() = "$firstName $lastName".trim()
    val normalizedMedicalId: String get() = medicalId.trim().lowercase(Locale.ROOT)

    fun validationErrors(today: LocalDate = LocalDate.now()): List<String> = buildList {
        if (firstName.isBlank()) add("First name is required.")
        if (lastName.isBlank()) add("Last name is required.")
        if (medicalId.isBlank()) add("Medical ID is required.")
        val birth = parseDate(birthDate)
        if (birth == null || birth.isAfter(today)) add("Select a valid date of birth, not in the future.")
        if (email.isNotBlank() && !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)) add("Enter a valid email address.")
        if (!consentGranted) add("Consent is required for local profile storage in this prototype.")
        if (examinationType.isNotBlank() && examinationType !in examinationTypes) add("Select a listed examination type.")
    }
}

fun parseDate(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()

@Serializable
enum class VitalMetric(val title: String, val unit: String, val minimum: Double, val maximum: Double, val reference: ClosedFloatingPointRange<Double>?) {
    SYSTOLIC("Systolic blood pressure", "mmHg", 20.0, 350.0, 90.0..119.0),
    DIASTOLIC("Diastolic blood pressure", "mmHg", 10.0, 250.0, 60.0..79.0),
    HEART_RATE("Heart rate", "bpm", 10.0, 350.0, 60.0..100.0),
    HEIGHT("Height", "cm", 20.0, 300.0, null),
    WEIGHT("Weight", "kg", 0.1, 700.0, null),
    TEMPERATURE("Temperature", "°C", 25.0, 45.0, 36.1..37.2),
    RESPIRATORY_RATE("Respiratory rate", "breaths/min", 1.0, 100.0, 12.0..20.0),
    OXYGEN("SpO2", "%", 1.0, 100.0, 95.0..100.0),
    PAIN("Pain level", "/10", 0.0, 10.0, 0.0..3.0);

    fun outsideReference(value: Double): Boolean = reference?.let { value !in it } ?: false
}

@Serializable
data class VitalReading(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val values: Map<VitalMetric, Double> = emptyMap(),
    val symptoms: String = "",
) {
    val meanArterialPressure: Double? get() {
        val systolic = values[VitalMetric.SYSTOLIC] ?: return null
        val diastolic = values[VitalMetric.DIASTOLIC] ?: return null
        return (systolic + 2 * diastolic) / 3
    }

    fun validationErrors(birthDate: String, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): List<String> = buildList {
        if (timestamp > now) add("Reading date cannot be in the future.")
        val birth = parseDate(birthDate)
        if (birth == null || Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate().isBefore(birth)) add("Reading date cannot precede date of birth.")
        if (values.isEmpty()) add("Enter at least one vital sign.")
        for ((metric, value) in values) {
            if (!value.isFinite() || value !in metric.minimum..metric.maximum) add("${metric.title} must be within ${metric.minimum}–${metric.maximum} ${metric.unit}.")
        }
        val systolic = values[VitalMetric.SYSTOLIC]
        val diastolic = values[VitalMetric.DIASTOLIC]
        if ((systolic == null) != (diastolic == null)) add("Enter both systolic and diastolic pressure.")
        if (systolic != null && diastolic != null && systolic <= diastolic) add("Systolic pressure must exceed diastolic pressure.")
    }
}

object UnitConversion {
    fun kilograms(pounds: Double) = pounds * 0.45359237
    fun centimeters(feet: Double, inches: Double) = (feet * 12 + inches) * 2.54
    fun celsius(fahrenheit: Double) = (fahrenheit - 32) * 5 / 9
}

fun decimalInput(text: String, locale: Locale = Locale.getDefault()): Double? {
    val separator = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val normalized = text.trim().replace(separator, '.')
    if (!Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)").matches(normalized)) return null
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
}

fun recentReadings(readings: List<VitalReading>, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): List<VitalReading> {
    val start = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
    return readings.filter { it.timestamp in start..now }.sortedBy { it.timestamp }
}
