package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseSeeder
import com.example.data.local.SessionStore
import com.example.data.repository.AuthSettingsRepository
import com.example.data.repository.FinanceRepository

class AppContainer(application: Application) {
    val database: AppDatabase = AppDatabase.getInstance(application)
    val sessionStore: SessionStore = SessionStore(application)
    val seeder: DatabaseSeeder = DatabaseSeeder(database)
    val authRepository: AuthSettingsRepository = AuthSettingsRepository(application, database, sessionStore)
    val financeRepository: FinanceRepository = FinanceRepository(application, database)
}

class IFintechApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
