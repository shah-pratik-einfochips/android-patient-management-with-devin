package com.example.patienthealth.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.examinationTypes

@Composable
fun PatientForm(patient: Patient, onChange: (Patient) -> Unit, saving: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    val errors = patient.validationErrors()
    Page("Patient profile", onBack = if (saving) null else onBack, actions = {
        TextButton(onClick = onSave, enabled = errors.isEmpty() && !saving, modifier = Modifier.testTag("savePatient")) { Text(if (saving) "Saving…" else "Save") }
    }) {
        SectionTitle("Demographics")
        Field("First name", patient.firstName, { onChange(patient.copy(firstName = it)) })
        Field("Last name", patient.lastName, { onChange(patient.copy(lastName = it)) })
        Field("Medical ID", patient.medicalId, { onChange(patient.copy(medicalId = it)) })
        DateField("Date of birth", patient.birthDate) { onChange(patient.copy(birthDate = it)) }
        Choice("Administrative gender", patient.gender, listOf("unknown", "female", "male", "other")) { onChange(patient.copy(gender = it)) }
        Field("Ethnicity (optional)", patient.ethnicity, { onChange(patient.copy(ethnicity = it)) })
        SectionTitle("Contact")
        Field("Phone", patient.phone, { onChange(patient.copy(phone = it)) }, KeyboardType.Phone)
        Field("Email", patient.email, { onChange(patient.copy(email = it)) }, KeyboardType.Email)
        SectionTitle("Clinical details")
        Choice("Blood type", patient.bloodType, listOf("Unknown", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")) { onChange(patient.copy(bloodType = it)) }
        Field("Allergies", patient.allergies, { onChange(patient.copy(allergies = it)) })
        Field("Medications", patient.medications, { onChange(patient.copy(medications = it)) })
        Field("Health conditions", patient.conditions, { onChange(patient.copy(conditions = it)) })
        SectionTitle("Examination")
        Field("Purpose of examination", patient.examinationPurpose, { onChange(patient.copy(examinationPurpose = it)) })
        Choice("Type of examination", patient.examinationType, examinationTypes) { onChange(patient.copy(examinationType = it)) }
        Field("Performed by", patient.performedBy, { onChange(patient.copy(performedBy = it)) })
        Field("Referred by", patient.referredBy, { onChange(patient.copy(referredBy = it)) })
        SectionTitle("Consent")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(patient.consentGranted, { onChange(patient.copy(consentGranted = it)) }, modifier = Modifier.testTag("consent"))
            Text("Consent to local profile storage")
        }
        Text("Prototype consent flag only. Formal consent records, revocation workflows, and clinical authorization are not implemented.")
        ValidationErrors(errors)
    }
}
