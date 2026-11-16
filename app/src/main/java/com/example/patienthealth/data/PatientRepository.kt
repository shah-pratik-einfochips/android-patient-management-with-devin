package com.example.patienthealth.data

import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.VitalReading
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface PatientRepository {
    val patients: StateFlow<List<Patient>>
    suspend fun load()
    suspend fun save(patient: Patient)
    suspend fun addReading(patientId: String, reading: VitalReading)
    suspend fun removeReading(patientId: String, readingId: String)
    suspend fun remove(patientId: String)
}

class PatientValidationException(message: String) : IllegalArgumentException(message)

@Singleton
class LocalPatientRepository @Inject constructor(
    private val database: PatientDatabase,
    private val cipher: PatientCipher,
) : PatientRepository {
    private val mutablePatients = MutableStateFlow<List<Patient>>(emptyList())
    override val patients = mutablePatients.asStateFlow()
    private val mutex = Mutex()
    private var loaded = false
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val decoded = database.patients().all().map { record ->
                json.decodeFromString<Patient>(cipher.decrypt(record.id, record.payload).toString(Charsets.UTF_8)).also {
                    check(it.id == record.id)
                }
            }
            mutablePatients.value = decoded.sortedBy { it.name.lowercase() }
            loaded = true
        }
    }

    override suspend fun save(patient: Patient) = withContext(Dispatchers.IO) {
        mutex.withLock { persist(patient) }
    }

    override suspend fun addReading(patientId: String, reading: VitalReading) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val patient = mutablePatients.value.firstOrNull { it.id == patientId }
                ?: throw PatientValidationException("Patient is unavailable. Reopen the profile.")
            persist(patient.copy(vitals = patient.vitals + reading))
        }
    }

    override suspend fun removeReading(patientId: String, readingId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val patient = mutablePatients.value.firstOrNull { it.id == patientId }
                ?: throw PatientValidationException("Patient is unavailable. Reopen the profile.")
            persist(patient.copy(vitals = patient.vitals.filterNot { it.id == readingId }))
        }
    }

    override suspend fun remove(patientId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (mutablePatients.value.none { it.id == patientId }) return@withLock
            database.patients().delete(patientId)
            mutablePatients.value = mutablePatients.value.filterNot { it.id == patientId }
        }
    }

    private suspend fun persist(patient: Patient) {
        check(loaded) { "Storage has not loaded" }
        val errors = patient.validationErrors() + patient.vitals.flatMap { it.validationErrors(patient.birthDate) }
        if (errors.isNotEmpty()) throw PatientValidationException(errors.first())
        if (mutablePatients.value.any { it.id != patient.id && it.normalizedMedicalId == patient.normalizedMedicalId }) {
            throw PatientValidationException("A patient with this medical ID already exists.")
        }
        val payload = cipher.encrypt(patient.id, json.encodeToString(patient).toByteArray(Charsets.UTF_8))
        database.patients().upsert(EncryptedPatientRecord(patient.id, payload))
        mutablePatients.value = (mutablePatients.value.filterNot { it.id == patient.id } + patient).sortedBy { it.name.lowercase() }
    }
}
