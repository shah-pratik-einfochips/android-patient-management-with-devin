package com.example.patienthealth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class StartupTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun realAppOpensSecureStorageAndNavigatesToPatientForm() {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Patient Health").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Add patient").assertIsDisplayed().performClick()
        compose.onNodeWithTag("First name").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Patient Health").assertIsDisplayed()
    }
}
