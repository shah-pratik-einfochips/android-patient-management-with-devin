package com.example.patienthealth

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.VitalDraft
import com.example.patienthealth.domain.VitalMetric
import com.example.patienthealth.ui.PatientForm
import com.example.patienthealth.ui.VitalsForm
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PatientUiTest {
    @get:Rule val compose = createComposeRule()
    private fun patient() = Patient(firstName = "Synthetic", lastName = "Patient", medicalId = "TEST-001", birthDate = "1990-01-01", consentGranted = true)

    @Test fun examinationDropdownSelectsAndSaves() {
        var draft by mutableStateOf(patient())
        var saved: Patient? = null
        compose.setContent { MaterialTheme { PatientForm(draft, { draft = it }, false, {}, { saved = draft }) } }
        compose.onNodeWithTag("Type of examination").performScrollTo().performClick()
        compose.onNodeWithText("Emergency").performClick()
        compose.onNodeWithTag("Type of examination").assertTextContains("Emergency")
        compose.onNodeWithTag("savePatient").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("Emergency", saved?.examinationType) }
    }
    @Test fun invalidProfileCannotBeSaved() {
        compose.setContent { MaterialTheme { PatientForm(Patient(), {}, false, {}, {}) } }
        compose.onNodeWithTag("savePatient").assertIsNotEnabled()
    }
    @Test fun validVitalCanBeEnteredAndSaved() {
        var draft by mutableStateOf(VitalDraft())
        var saved = false
        compose.setContent { MaterialTheme { VitalsForm(patient(), draft, { draft = it }, false, {}, { saved = true }) } }
        compose.onNodeWithTag("saveVitals").assertIsNotEnabled()
        compose.onNodeWithTag(VitalMetric.HEART_RATE.name).performScrollTo().performTextInput("72")
        compose.onNodeWithTag("saveVitals").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(true, saved); assertEquals(72.0, draft.reading().values[VitalMetric.HEART_RATE]) }
    }
}
