package com.example.patienthealth.domain

import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class VitalDraft(
    val timestamp: Long = System.currentTimeMillis(),
    val inputs: Map<VitalMetric, String> = emptyMap(),
    val heightUnit: String = "cm",
    val weightUnit: String = "kg",
    val temperatureUnit: String = "C",
    val inches: String = "",
    val symptoms: String = "",
) {
    fun reading(locale: Locale = Locale.getDefault()): VitalReading {
        val values = inputs.mapNotNull { (metric, raw) ->
            val value = decimalInput(raw, locale) ?: return@mapNotNull null
            metric to when {
                metric == VitalMetric.HEIGHT && heightUnit == "ft/in" -> UnitConversion.centimeters(value, decimalInput(inches, locale) ?: 0.0)
                metric == VitalMetric.WEIGHT && weightUnit == "lbs" -> UnitConversion.kilograms(value)
                metric == VitalMetric.TEMPERATURE && temperatureUnit == "F" -> UnitConversion.celsius(value)
                else -> value
            }
        }.toMap()
        return VitalReading(timestamp = timestamp, values = values, symptoms = symptoms)
    }

    fun errors(birthDate: String, now: Long = System.currentTimeMillis(), locale: Locale = Locale.getDefault()): List<String> = buildList {
        addAll(reading(locale).validationErrors(birthDate, now))
        for ((metric, raw) in inputs) {
            if (raw.isNotEmpty() && decimalInput(raw, locale) == null) add("Enter a valid number for ${metric.title}.")
        }
        if (heightUnit == "ft/in" && inches.isNotEmpty()) {
            val value = decimalInput(inches, locale)
            if (value == null || value < 0 || value >= 12 || decimalInput(inputs[VitalMetric.HEIGHT].orEmpty(), locale) == null) {
                add("Enter feet with inches from 0 to less than 12.")
            }
        }
    }
}
