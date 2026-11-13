package com.example.patienthealth

import com.example.patienthealth.data.PatientRepository
import com.example.patienthealth.data.PatientValidationException
import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.VitalReading
import com.example.patienthealth.ui.PatientViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatientViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    private class FakeRepository : PatientRepository {
        override val patients = MutableStateFlow<List<Patient>>(emptyList())
        var loadFailure = false
        var saveError: Exception? = null
        override suspend fun load() { if (loadFailure) error("Internal storage details") }
        override suspend fun save(patient: Patient) {
            saveError?.let { throw it }
            patients.value = listOf(patient)
        }
        override suspend fun addReading(patientId: String, reading: VitalReading) {}
    }

    @Test fun successfulLoadAndSave() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = PatientViewModel(repository)
        assertTrue(model.state.value.loading)
        advanceUntilIdle()
        assertFalse(model.state.value.loading)
        var saved = false
        model.save(Patient()) { saved = true }
        assertTrue(model.state.value.saving)
        advanceUntilIdle()
        assertTrue(saved)
        assertFalse(model.state.value.saving)
    }
    @Test fun failedLoadBlocksWritesAndCanBeRetried() = runTest(dispatcher) {
        val repository = FakeRepository().apply { loadFailure = true }
        val model = PatientViewModel(repository)
        advanceUntilIdle()
        assertTrue(model.state.value.failed)
        var saved = false
        model.save(Patient()) { saved = true }
        advanceUntilIdle()
        assertFalse(saved)
        repository.loadFailure = false
        model.reload()
        advanceUntilIdle()
        assertFalse(model.state.value.failed)
    }
    @Test fun validationErrorIsShownWithoutCompletingSave() = runTest(dispatcher) {
        val model = PatientViewModel(FakeRepository().apply { saveError = PatientValidationException("Duplicate ID") })
        advanceUntilIdle()
        var saved = false
        model.save(Patient()) { saved = true }
        advanceUntilIdle()
        assertFalse(saved)
        assertEquals("Duplicate ID", model.state.value.error)
        model.dismissError()
        assertNull(model.state.value.error)
    }
    @Test fun unexpectedErrorDoesNotExposeInternalDetails() = runTest(dispatcher) {
        val model = PatientViewModel(FakeRepository().apply { saveError = IllegalStateException("Sensitive internal details") })
        advanceUntilIdle()
        model.save(Patient()) {}
        advanceUntilIdle()
        assertFalse(model.state.value.error.orEmpty().contains("Sensitive"))
        assertFalse(model.state.value.saving)
    }
    @Test fun repeatedSaveWhileBusyIsIgnored() = runTest(dispatcher) {
        val model = PatientViewModel(FakeRepository())
        advanceUntilIdle()
        var callbacks = 0
        model.save(Patient()) { callbacks++ }
        model.save(Patient()) { callbacks++ }
        advanceUntilIdle()
        assertEquals(1, callbacks)
    }
}
