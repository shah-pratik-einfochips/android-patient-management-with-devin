package com.example.patienthealth.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.VitalDraft
import com.example.patienthealth.domain.VitalMetric
import com.example.patienthealth.domain.parseDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun VitalsForm(patient: Patient, draft: VitalDraft, onChange: (VitalDraft) -> Unit, saving: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    val errors = draft.errors(patient.birthDate)
    val reading = draft.reading()
    Page("Add vitals", onBack = if (saving) null else onBack, actions = {
        TextButton(onClick = onSave, enabled = errors.isEmpty() && !saving, modifier = Modifier.testTag("saveVitals")) { Text(if (saving) "Saving…" else "Save") }
    }) {
        Text(patient.name)
        DateField("Reading date", Instant.ofEpochMilli(draft.timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString(), minimum = parseDate(patient.birthDate)) {
            onChange(draft.copy(timestamp = LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()))
        }
        Text("Leave unmeasured vitals blank. Blood pressure requires both values.")
        VitalMetric.entries.forEach { metric ->
            val unit = when (metric) {
                VitalMetric.HEIGHT -> if (draft.heightUnit == "ft/in") "feet" else "cm"
                VitalMetric.WEIGHT -> draft.weightUnit
                VitalMetric.TEMPERATURE -> "°${draft.temperatureUnit}"
                else -> metric.unit
            }
            Field("${metric.title} ($unit)", draft.inputs[metric].orEmpty(), {
                onChange(draft.copy(inputs = draft.inputs + (metric to it)))
            }, KeyboardType.Decimal, metric.name)
            when (metric) {
                VitalMetric.HEIGHT -> {
                    Choice("Height unit", draft.heightUnit, listOf("cm", "ft/in")) {
                        onChange(draft.copy(heightUnit = it, inputs = draft.inputs - metric, inches = ""))
                    }
                    if (draft.heightUnit == "ft/in") Field("Additional inches (0–11.99)", draft.inches, { onChange(draft.copy(inches = it)) }, KeyboardType.Decimal)
                }
                VitalMetric.WEIGHT -> Choice("Weight unit", draft.weightUnit, listOf("kg", "lbs")) {
                    onChange(draft.copy(weightUnit = it, inputs = draft.inputs - metric))
                }
                VitalMetric.TEMPERATURE -> Choice("Temperature unit", draft.temperatureUnit, listOf("C", "F")) {
                    onChange(draft.copy(temperatureUnit = it, inputs = draft.inputs - metric))
                }
                else -> Unit
            }
            reading.values[metric]?.let { if (metric.outsideReference(it)) Text("Warning: outside illustrative adult reference range.") }
        }
        reading.meanArterialPressure?.let { Text("Calculated MAP: ${formatValue(it)} mmHg") }
        Field("Symptoms (optional)", draft.symptoms, { onChange(draft.copy(symptoms = it)) })
        Text("Reference ranges are illustrative only, not age-adjusted, and must not be used for diagnosis or treatment.")
        ValidationErrors(errors)
    }
}

fun formatValue(value: Double): String = String.format(java.util.Locale.getDefault(), "%.1f", value)
