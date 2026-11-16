package com.example.patienthealth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.patienthealth.data.PatientRepository
import com.example.patienthealth.data.PatientValidationException
import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.VitalReading
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StorageState(val loading: Boolean = true, val failed: Boolean = false, val saving: Boolean = false, val error: String? = null)

@HiltViewModel
class PatientViewModel @Inject constructor(private val repository: PatientRepository) : ViewModel() {
    private val mutablePatientDraft = MutableStateFlow(Patient())
    val patientDraft = mutablePatientDraft.asStateFlow()
    private val mutableVitalDraft = MutableStateFlow(com.example.patienthealth.domain.VitalDraft())
    val vitalDraft = mutableVitalDraft.asStateFlow()
    fun editPatient(patient: Patient) { mutablePatientDraft.value = patient }
    fun editVitals(draft: com.example.patienthealth.domain.VitalDraft) { mutableVitalDraft.value = draft }

    val patients = repository.patients
    private val mutableState = MutableStateFlow(StorageState())
    val state = mutableState.asStateFlow()

    init { reload() }

    fun reload() {
        if (mutableState.value.saving) return
        mutableState.value = StorageState()
        viewModelScope.launch {
            try {
                repository.load()
                mutableState.value = StorageState(loading = false)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.value = StorageState(loading = false, failed = true)
            }
        }
    }

    fun save(patient: Patient, onSaved: () -> Unit) = write(onSaved) { repository.save(patient) }
    fun addReading(patientId: String, reading: VitalReading, onSaved: () -> Unit) = write(onSaved) { repository.addReading(patientId, reading) }
    fun deleteReading(patientId: String, readingId: String) = write({}) { repository.removeReading(patientId, readingId) }
    fun deletePatient(patientId: String, onDeleted: () -> Unit) = write(onDeleted) { repository.remove(patientId) }
    fun dismissError() { mutableState.value = mutableState.value.copy(error = null) }
    fun reportFailure() {
        mutableState.value = mutableState.value.copy(error = "The export could not be created. Please retry.")
    }

    private fun write(onSaved: () -> Unit, operation: suspend () -> Unit) {
        if (mutableState.value.loading || mutableState.value.failed || mutableState.value.saving) return
        mutableState.value = mutableState.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                operation()
                mutableState.value = mutableState.value.copy(saving = false)
                onSaved()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(saving = false, error =
                    if (error is PatientValidationException) error.message else "Unable to save securely. Existing records have not been reset. Please retry.")
            }
        }
    }
}
