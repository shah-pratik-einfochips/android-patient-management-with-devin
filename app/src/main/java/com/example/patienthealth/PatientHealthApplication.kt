package com.example.patienthealth

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.patienthealth.data.LocalPatientRepository
import com.example.patienthealth.data.PatientDatabase
import com.example.patienthealth.data.PatientRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@HiltAndroidApp
class PatientHealthApplication : Application()

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): PatientDatabase =
        Room.databaseBuilder(context, PatientDatabase::class.java, "patient-health.db").build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun repository(local: LocalPatientRepository): PatientRepository
}
