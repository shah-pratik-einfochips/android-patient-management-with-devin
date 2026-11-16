package com.example.patienthealth.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "patients")
data class EncryptedPatientRecord(@PrimaryKey val id: String, val payload: ByteArray)

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients")
    suspend fun all(): List<EncryptedPatientRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: EncryptedPatientRecord)

    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [EncryptedPatientRecord::class], version = 1, exportSchema = true)
abstract class PatientDatabase : RoomDatabase() {
    abstract fun patients(): PatientDao
}
