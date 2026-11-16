package com.example.patienthealth.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.VitalDraft

@Composable
fun PatientApp(viewModel: PatientViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val patientDraft by viewModel.patientDraft.collectAsStateWithLifecycle()
    val vitalDraft by viewModel.vitalDraft.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    if (state.loading) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text("Opening secure storage")
        }
        return
    }
    if (state.failed) {
        Page("Storage unavailable") {
            Text("Patient data could not be opened securely. No records have been reset.")
            Button(onClick = viewModel::reload) { Text("Retry") }
        }
        return
    }
    NavHost(nav, startDestination = "patients") {
        composable("patients") {
            PatientList(patients, onAdd = {
                viewModel.editPatient(Patient())
                nav.navigate("new")
            }, onOpen = { nav.navigate("patient/$it") })
        }
        composable("new") {
            PatientForm(patientDraft, viewModel::editPatient, state.saving, onBack = { nav.popBackStack() }, onSave = {
                viewModel.save(patientDraft) { nav.popBackStack() }
            })
        }
        composable("patient/{id}") { entry ->
            val id = entry.arguments?.getString("id")
            val patient = patients.firstOrNull { it.id == id }
            if (patient != null) {
                val context = androidx.compose.ui.platform.LocalContext.current
                PatientDetail(patient, onBack = { nav.popBackStack() }, onEdit = {
                    viewModel.editPatient(patient)
                    nav.navigate("edit/${patient.id}")
                }, onAddVitals = {
                    viewModel.editVitals(VitalDraft())
                    nav.navigate("vitals/${patient.id}")
                }, onExport = { pdf -> shareExport(context, patient, pdf, viewModel::reportFailure) },
                    onDeletePatient = {
                        viewModel.deletePatient(patient.id) { nav.popBackStack() }
                    }, onDeleteReading = { readingId -> viewModel.deleteReading(patient.id, readingId) })
            } else Page("Patient unavailable", { nav.popBackStack() }) { Text("Return to the patient list.") }
        }
        composable("edit/{id}") { entry ->
            val existing = patients.firstOrNull { it.id == entry.arguments?.getString("id") }
            if (existing != null) {
                val draft = patientDraft.takeIf { it.id == existing.id } ?: existing
                PatientForm(draft, viewModel::editPatient, state.saving, onBack = { nav.popBackStack() }, onSave = {
                    viewModel.save(draft) { nav.popBackStack() }
                })
            }
        }
        composable("vitals/{id}") { entry ->
            val patient = patients.firstOrNull { it.id == entry.arguments?.getString("id") }
            if (patient != null) {
                VitalsForm(patient, vitalDraft, viewModel::editVitals, state.saving, onBack = { nav.popBackStack() }, onSave = {
                    viewModel.addReading(patient.id, vitalDraft.reading()) { nav.popBackStack() }
                })
            }
        }
    }
    state.error?.let { error ->
        AlertDialog(onDismissRequest = viewModel::dismissError, title = { Text("Unable to save") }, text = { Text(error) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } })
    }
}

private fun shareExport(context: android.content.Context, patient: Patient, pdf: Boolean, onError: () -> Unit) {
    try {
        val file = if (pdf) com.example.patienthealth.data.PatientExport.writePdf(patient, context.cacheDir)
            else com.example.patienthealth.data.PatientExport.writeCsv(patient, context.cacheDir)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
            .setType(if (pdf) "application/pdf" else "text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share export"))
    } catch (_: Exception) {
        onError()
    }
}

@Composable
private fun PatientList(patients: List<Patient>, onAdd: () -> Unit, onOpen: (String) -> Unit) {
    var search by androidx.compose.runtime.remember { mutableStateOf("") }
    Page("Patient Health", actions = { TextButton(onClick = onAdd) { Text("Add patient") } }) {
        PrototypeNotice()
        Field("Search name or medical ID", search, { search = it })
        val filtered = patients.filter { search.isBlank() || it.name.contains(search, ignoreCase = true) || it.medicalId.contains(search, ignoreCase = true) }
        if (filtered.isEmpty()) {
            SectionTitle(if (patients.isEmpty()) "No patients yet" else "No matching patients")
            Text(if (patients.isEmpty()) "Add a synthetic patient profile to get started." else "Try a different name or medical ID.")
        }
        filtered.forEach { patient ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(patient.id) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(patient.name, style = MaterialTheme.typography.titleMedium)
                    Text("ID: ${patient.medicalId}")
                    Text("${patient.vitals.size} readings", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
