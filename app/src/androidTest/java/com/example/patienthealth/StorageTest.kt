package com.example.patienthealth

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.patienthealth.data.EncryptedPatientRecord
import com.example.patienthealth.data.LocalPatientRepository
import com.example.patienthealth.data.PatientCipher
import com.example.patienthealth.data.PatientDatabase
import com.example.patienthealth.domain.Patient
import com.example.patienthealth.domain.VitalMetric
import com.example.patienthealth.domain.VitalReading
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageTest {
    private lateinit var database: PatientDatabase
    private lateinit var repository: LocalPatientRepository
    private val cipher = PatientCipher()
    private fun patient() = Patient(firstName = "Synthetic", lastName = "Patient", medicalId = "TEST-001", birthDate = "1990-01-01", consentGranted = true)

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PatientDatabase::class.java).build()
        repository = LocalPatientRepository(database, cipher)
    }
    @After fun close() { database.close() }

    @Test fun encryptionRoundTripAndKeyPersistence() {
        val id = UUID.randomUUID().toString()
        val plain = "synthetic patient record".toByteArray()
        val sealed = cipher.encrypt(id, plain)
        assertArrayEquals(plain, PatientCipher().decrypt(id, sealed))
        assertFalse(sealed.toString(Charsets.ISO_8859_1).contains("synthetic"))
        assertFalse(sealed.contentEquals(cipher.encrypt(id, plain)))
    }
    @Test fun tamperingRejected() {
        val sealed = cipher.encrypt("test", "synthetic".toByteArray())
        sealed[sealed.lastIndex] = (sealed.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { cipher.decrypt("test", sealed) }
    }
    @Test fun substitutedRecordRejected() {
        assertThrows(Exception::class.java) { cipher.decrypt("other-id", cipher.encrypt("id", "synthetic".toByteArray())) }
    }
    @Test fun truncatedPayloadRejected() { assertThrows(Exception::class.java) { cipher.decrypt("id", byteArrayOf(1)) } }

    @Test fun saveReloadAndEdit() = runBlocking {
        repository.load()
        val value = patient()
        repository.save(value)
        repository.addReading(value.id, VitalReading(values = mapOf(VitalMetric.HEART_RATE to 70.0)))
        val second = LocalPatientRepository(database, PatientCipher())
        second.load()
        assertEquals(70.0, second.patients.value.single().vitals.single().values[VitalMetric.HEART_RATE])
        second.save(second.patients.value.single().copy(firstName = "Updated"))
        repository.load()
        assertEquals("Updated", repository.patients.value.single().firstName)
        val record = database.patients().all().single()
        assertFalse(record.payload.toString(Charsets.ISO_8859_1).contains(value.medicalId))
    }
    @Test fun duplicateIdRejected() = runBlocking {
        repository.load()
        repository.save(patient())
        val result = runCatching { repository.save(patient().copy(medicalId = " test-001 ")) }
        assertTrue(result.isFailure)
        assertEquals(1, database.patients().all().size)
    }
    @Test fun invalidDataRejectedWithoutChangingRecords() = runBlocking {
        repository.load()
        assertTrue(runCatching { repository.save(Patient()) }.isFailure)
        val value = patient()
        repository.save(value)
        assertTrue(runCatching { repository.addReading(value.id, VitalReading(values = mapOf(VitalMetric.PAIN to 11.0))) }.isFailure)
        assertTrue(repository.patients.value.single().vitals.isEmpty())
    }
    @Test fun unreadableRecordFailsClosed() = runBlocking {
        database.patients().upsert(EncryptedPatientRecord("test", byteArrayOf(1, 2, 3)))
        assertTrue(runCatching { repository.load() }.isFailure)
        assertTrue(repository.patients.value.isEmpty())
        assertTrue(runCatching { repository.save(patient()) }.isFailure)
        assertEquals(1, database.patients().all().size)
    }
    @Test fun concurrentReadingsAreNotLost() = runBlocking {
        repository.load()
        val value = patient()
        repository.save(value)
        (1..5).map { async { repository.addReading(value.id, VitalReading(values = mapOf(VitalMetric.HEART_RATE to 70.0))) } }.awaitAll()
        assertEquals(5, repository.patients.value.single().vitals.size)
    }
}
