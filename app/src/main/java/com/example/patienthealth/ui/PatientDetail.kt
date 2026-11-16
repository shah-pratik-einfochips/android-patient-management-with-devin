package com.example.patienthealth.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.VitalMetric
import com.example.patienthealth.domain.VitalReading
import com.example.patienthealth.domain.recentReadings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun PatientDetail(patient: Patient, onBack: () -> Unit, onEdit: () -> Unit, onAddVitals: () -> Unit,
                  onExport: (Boolean) -> Unit, onDeletePatient: () -> Unit, onDeleteReading: (String) -> Unit) {
    var metricName by rememberSaveable { mutableStateOf(VitalMetric.HEART_RATE.name) }
    val metric = VitalMetric.valueOf(metricName)
    var chart by rememberSaveable { mutableStateOf(true) }
    var deletePatient by remember { mutableStateOf(false) }
    var deleteReadingId by remember { mutableStateOf<String?>(null) }
    val recent = recentReadings(patient.vitals)
    val measured = recent.filter { it.values[metric] != null }
    val values = measured.mapNotNull { it.values[metric] }
    Page(patient.name, onBack, actions = { TextButton(onClick = onEdit) { Text("Edit") } }) {
        SectionTitle("Patient profile")
        Text("Medical ID: ${patient.medicalId}")
        Text("Date of birth: ${patient.birthDate}")
        Text("Blood type: ${patient.bloodType}")
        if (patient.allergies.isNotBlank()) Text("Allergies: ${patient.allergies}")
        if (patient.medications.isNotBlank()) Text("Medications: ${patient.medications}")
        if (patient.conditions.isNotBlank()) Text("Conditions: ${patient.conditions}")
        if (patient.examinationType.isNotBlank()) Text("Examination: ${patient.examinationType}")
        if (patient.examinationPurpose.isNotBlank()) Text("Purpose: ${patient.examinationPurpose}")
        if (patient.performedBy.isNotBlank()) Text("Performed by: ${patient.performedBy}")
        if (patient.referredBy.isNotBlank()) Text("Referred by: ${patient.referredBy}")
        Button(onClick = onAddVitals) { Text("Add vitals") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onExport(false) }) { Text("Export CSV") }
            TextButton(onClick = { onExport(true) }) { Text("Export PDF") }
            TextButton(onClick = { deletePatient = true }) { Text("Delete patient", color = MaterialTheme.colorScheme.error) }
        }
        SectionTitle("Last seven days")
        Choice("Vital type", metric.title, VitalMetric.entries.map { it.title }) {
            metricName = VitalMetric.entries.first { metric -> metric.title == it }.name
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(chart, { chart = it })
            Text("Chart view")
        }
        if (values.isEmpty()) {
            Text("No ${metric.title.lowercase()} readings in the last seven days.")
        } else {
            if (chart) VitalsChart(measured, metric)
            Text("Average: ${formatValue(values.average())} ${metric.unit}")
            Text("Minimum / maximum: ${formatValue(values.min())} / ${formatValue(values.max())} ${metric.unit}")
            measured.reversed().forEach { reading ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(formatDate(reading.timestamp))
                        Text("${formatValue(reading.values.getValue(metric))} ${metric.unit}")
                        if (metric.outsideReference(reading.values.getValue(metric))) Text("Warning: outside illustrative adult range", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        Text("Illustrative adult reference ranges only. Not clinical decision support.", style = MaterialTheme.typography.bodySmall)
        SectionTitle("All readings")
        if (patient.vitals.isEmpty()) Text("No vitals recorded yet.")
        patient.vitals.sortedByDescending { it.timestamp }.forEach { reading -> ReadingCard(reading) { deleteReadingId = it } }
    }
    if (deletePatient) {
        androidx.compose.material3.AlertDialog(onDismissRequest = { deletePatient = false },
            title = { Text("Delete patient") },
            text = { Text("Delete ${patient.name} and all recorded vitals? This cannot be undone.") },
            confirmButton = { TextButton(onClick = { deletePatient = false; onDeletePatient() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deletePatient = false }) { Text("Cancel") } })
    }
    deleteReadingId?.let { id ->
        androidx.compose.material3.AlertDialog(onDismissRequest = { deleteReadingId = null },
            title = { Text("Delete reading") },
            text = { Text("Delete this vitals reading? This cannot be undone.") },
            confirmButton = { TextButton(onClick = { deleteReadingId = null; onDeleteReading(id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteReadingId = null }) { Text("Cancel") } })
    }
}

@Composable
private fun ReadingCard(reading: VitalReading, onDelete: (String) -> Unit) {
    var expanded by remember(reading.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { expanded = !expanded }) { Text("${if (expanded) "Hide" else "Show"} reading: ${formatDate(reading.timestamp)}") }
            if (expanded) {
                VitalMetric.entries.forEach { metric ->
                    reading.values[metric]?.let { value ->
                        Text("${metric.title}: ${formatValue(value)} ${metric.unit}${if (metric.outsideReference(value)) " — outside illustrative range" else ""}")
                    }
                }
                reading.meanArterialPressure?.let { Text("MAP: ${formatValue(it)} mmHg") }
                if (reading.symptoms.isNotBlank()) Text("Symptoms: ${reading.symptoms}")
                TextButton(onClick = { onDelete(reading.id) }) { Text("Delete reading", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun VitalsChart(readings: List<VitalReading>, metric: VitalMetric) {
    var selected by remember(readings, metric) { mutableIntStateOf(-1) }
    val values = readings.map { it.values.getValue(metric) }
    val low = values.min() - 1
    val high = values.max() + 1
    val start = readings.first().timestamp
    val duration = (readings.last().timestamp - start).coerceAtLeast(1)
    val fractions = readings.map { if (readings.size == 1) 0.5f else (it.timestamp - start).toFloat() / duration }
    val primary = MaterialTheme.colorScheme.primary
    val warning = MaterialTheme.colorScheme.error
    Canvas(Modifier.fillMaxWidth().height(200.dp).semantics {
        contentDescription = "${metric.title} trend, ${values.size} readings in ${metric.unit}. Individual values are listed below."
    }.pointerInput(readings, metric) {
        detectTapGestures { tap ->
            selected = fractions.indices.minByOrNull { abs((16.dp.toPx() + fractions[it] * (size.width - 32.dp.toPx())) - tap.x) } ?: -1
        }
    }) {
        val inset = 16.dp.toPx()
        val points = fractions.mapIndexed { index, fraction ->
            Offset(inset + fraction * (size.width - inset * 2), size.height - inset - ((values[index] - low) / (high - low)).toFloat() * (size.height - inset * 2))
        }
        drawLine(Color.Gray, Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset))
        points.zipWithNext().forEach { (a, b) -> drawLine(primary, a, b, strokeWidth = 2.dp.toPx()) }
        points.forEachIndexed { index, point ->
            val size = if (index == selected) 7.dp.toPx() else 4.dp.toPx()
            if (metric.outsideReference(values[index])) {
                withTransform({ translate(point.x, point.y); rotate(45f, Offset.Zero) }) {
                    drawRect(warning, Offset(-size, -size), androidx.compose.ui.geometry.Size(size * 2, size * 2))
                }
            } else {
                drawCircle(primary, size, point)
            }
        }
    }
    Text("${formatDate(readings.first().timestamp)} — ${formatDate(readings.last().timestamp)}", style = MaterialTheme.typography.bodySmall)
    if (selected in readings.indices) Text("Selected: ${formatDate(readings[selected].timestamp)} · ${formatValue(values[selected])} ${metric.unit}")
    Text("Tap a point to inspect it. Warning values are also labeled in the table.", style = MaterialTheme.typography.bodySmall)
}

fun formatDate(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))
