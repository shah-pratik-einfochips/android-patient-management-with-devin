package com.example.patienthealth

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.VitalMetric
import com.example.patienthealth.domain.VitalReading
import com.example.patienthealth.ui.PatientDetail
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DetailUiTest {
    @get:Rule val compose = createComposeRule()
    private fun patient() = Patient(
        firstName = "Synthetic", lastName = "Patient", medicalId = "TEST-001",
        birthDate = "1990-01-01", consentGranted = true,
        vitals = listOf(VitalReading(values = mapOf(VitalMetric.HEART_RATE to 70.0))),
    )

    private fun detail(
        patient: Patient,
        onExport: (Boolean) -> Unit = {},
        onDeletePatient: () -> Unit = {},
        onDeleteReading: (String) -> Unit = {},
    ) = compose.setContent {
        MaterialTheme { PatientDetail(patient, {}, {}, {}, onExport, onDeletePatient, onDeleteReading) }
    }

    @Test fun exportButtonsInvokeCallbacks() {
        val calls = mutableListOf<Boolean>()
        detail(patient(), onExport = { calls += it })
        compose.onNodeWithText("Export CSV").performClick()
        compose.onNodeWithText("Export PDF").performClick()
        compose.runOnIdle { assertEquals(listOf(false, true), calls) }
    }

    @Test fun deletePatientRequiresConfirmation() {
        var deleted = false
        detail(patient(), onDeletePatient = { deleted = true })
        compose.onNodeWithText("Delete patient").performClick()
        compose.onNodeWithText("This cannot be undone.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertFalse(deleted) }
        compose.onNodeWithText("Delete patient").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.runOnIdle { assertTrue(deleted) }
    }

    @Test fun deleteReadingRequiresConfirmation() {
        var deletedId: String? = null
        val value = patient()
        detail(value, onDeleteReading = { deletedId = it })
        compose.onNodeWithText("Show reading:", substring = true).performClick()
        compose.onNodeWithText("Delete reading").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.runOnIdle { assertEquals(value.vitals.single().id, deletedId) }
    }
}
